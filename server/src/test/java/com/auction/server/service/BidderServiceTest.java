package com.auction.server.service;

import com.auction.server.model.Bidder;
import com.auction.server.model.Seller;
import com.auction.server.model.User;
import com.auction.server.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BidderServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BidderService bidderService;

    private Bidder mockBidder;
    private Seller mockSeller;

    @BeforeEach
    public void setUp() {
        // Chuẩn bị một Bidder giả
        mockBidder = new Bidder();
        mockBidder.setId(10);
        mockBidder.setUsername("chuvanan");
        mockBidder.setBalance(BigDecimal.valueOf(500));

        // Chuẩn bị một Seller giả (đã là SELLER rồi)
        mockSeller = new Seller();
        mockSeller.setId(14);
        mockSeller.setUsername("nguoiban");
    }

    // =====================================================================
    // TEST 1: Nâng cấp thành công (User là BIDDER hợp lệ)
    // =====================================================================
    @Test
    @DisplayName("upToSeller: BIDDER hợp lệ -> Nâng cấp thành SELLER thành công")
    public void testUpToSeller_ThanhCong() {
        // Giả lập DB trả về Bidder khi tìm theo ID
        when(userRepository.findById(10)).thenReturn(Optional.of(mockBidder));
        // Giả lập native query UPDATE thành công (trả về 1 row affected)
        when(userRepository.updateRoleById(10, "SELLER")).thenReturn(1);

        Map<String, Object> result = bidderService.upToSeller(10);

        // Kiểm tra kết quả trả về
        assertTrue((Boolean) result.get("success"), "Phải trả về success = true");
        assertEquals("Đã nâng cấp tài khoản thành công", result.get("message"));

        // Đảm bảo native UPDATE được gọi đúng 1 lần với đúng tham số
        verify(userRepository, times(1)).updateRoleById(10, "SELLER");
    }

    // =====================================================================
    // TEST 2: Không tìm thấy User
    // =====================================================================
    @Test
    @DisplayName("upToSeller: Không tìm thấy userId -> Trả về lỗi")
    public void testUpToSeller_KhongTimThayUser() {
        // Giả lập DB không tìm thấy user
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        Map<String, Object> result = bidderService.upToSeller(999);

        assertFalse((Boolean) result.get("success"), "Phải trả về success = false");
        assertEquals("Người dùng không tồn tại", result.get("message"));

        // Đảm bảo KHÔNG gọi UPDATE
        verify(userRepository, never()).updateRoleById(anyInt(), anyString());
    }

    // =====================================================================
    // TEST 3: User đã là SELLER - không được nâng cấp lần nữa
    // =====================================================================
    @Test
    @DisplayName("upToSeller: User đã là SELLER -> Từ chối nâng cấp")
    public void testUpToSeller_DaLaSeller() {
        when(userRepository.findById(14)).thenReturn(Optional.of(mockSeller));

        Map<String, Object> result = bidderService.upToSeller(14);

        assertFalse((Boolean) result.get("success"), "Phải trả về success = false");
        assertEquals("Tài khoản không phải là BIDDER hoặc đã là SELLER", result.get("message"));

        // Đảm bảo KHÔNG gọi UPDATE
        verify(userRepository, never()).updateRoleById(anyInt(), anyString());
    }

    // =====================================================================
    // TEST 4: Native UPDATE thất bại (trả về 0 row affected)
    // =====================================================================
    @Test
    @DisplayName("upToSeller: DB UPDATE thất bại (0 rows affected) -> Trả về lỗi")
    public void testUpToSeller_UpdateThatBai() {
        when(userRepository.findById(10)).thenReturn(Optional.of(mockBidder));
        // Giả lập UPDATE không ảnh hưởng đến row nào (lỗi DB)
        when(userRepository.updateRoleById(10, "SELLER")).thenReturn(0);

        Map<String, Object> result = bidderService.upToSeller(10);

        assertFalse((Boolean) result.get("success"), "Phải trả về success = false khi UPDATE thất bại");
        assertEquals("Cập nhật thất bại", result.get("message"));
    }

    // =====================================================================
    // TEST 5: User là ADMIN - không được nâng cấp thành SELLER
    // =====================================================================
    @Test
    @DisplayName("upToSeller: User là ADMIN -> Từ chối nâng cấp")
    public void testUpToSeller_LaAdmin() {
        com.auction.server.model.Admin mockAdmin = new com.auction.server.model.Admin();
        mockAdmin.setId(15);
        mockAdmin.setUsername("phanbuom");

        when(userRepository.findById(15)).thenReturn(Optional.of(mockAdmin));

        Map<String, Object> result = bidderService.upToSeller(15);

        assertFalse((Boolean) result.get("success"));
        assertEquals("Tài khoản không phải là BIDDER hoặc đã là SELLER", result.get("message"));
        verify(userRepository, never()).updateRoleById(anyInt(), anyString());
    }
}
