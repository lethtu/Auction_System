package com.auction.server.service;

import com.auction.server.model.Admin;
import com.auction.server.model.Bidder;
import com.auction.server.model.Seller;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        mockBidder = new Bidder();
        mockBidder.setId(10);
        mockBidder.setUsername("chuvanan");
        mockBidder.setBalance(BigDecimal.valueOf(500));

        mockSeller = new Seller();
        mockSeller.setId(14);
        mockSeller.setUsername("nguoiban");
    }

    @Test
    @DisplayName("upToSeller: bidder hợp lệ -> nâng cấp thành seller thành công")
    public void testUpToSeller_ThanhCong() {
        when(userRepository.findById(10)).thenReturn(Optional.of(mockBidder));
        when(userRepository.updateRoleById(10, "seller")).thenReturn(1);

        Map<String, Object> result = bidderService.upToSeller(10);

        assertTrue((Boolean) result.get("success"), "Phải trả về success = true");
        assertEquals("Account upgraded successfully", result.get("message"));

        verify(userRepository, times(1)).updateRoleById(10, "seller");
    }

    @Test
    @DisplayName("upToSeller: không tìm thấy userId -> trả về lỗi")
    public void testUpToSeller_KhongTimThayUser() {
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        Map<String, Object> result = bidderService.upToSeller(999);

        assertFalse((Boolean) result.get("success"), "Phải trả về success = false");
        assertEquals("User does not exist", result.get("message"));

        verify(userRepository, never()).updateRoleById(anyInt(), anyString());
    }

    @Test
    @DisplayName("upToSeller: user đã là seller -> từ chối nâng cấp")
    public void testUpToSeller_DaLaSeller() {
        when(userRepository.findById(14)).thenReturn(Optional.of(mockSeller));

        Map<String, Object> result = bidderService.upToSeller(14);

        assertFalse((Boolean) result.get("success"), "Phải trả về success = false");
        assertEquals("Account is not a BIDDER or is already a SELLER", result.get("message"));

        verify(userRepository, never()).updateRoleById(anyInt(), anyString());
    }

    @Test
    @DisplayName("upToSeller: DB update thất bại -> trả về lỗi")
    public void testUpToSeller_UpdateThatBai() {
        when(userRepository.findById(10)).thenReturn(Optional.of(mockBidder));
        when(userRepository.updateRoleById(10, "seller")).thenReturn(0);

        Map<String, Object> result = bidderService.upToSeller(10);

        assertFalse((Boolean) result.get("success"), "Phải trả về success = false khi UPDATE thất bại");
        assertEquals("Upgrade failed", result.get("message"));

        verify(userRepository, times(1)).updateRoleById(10, "seller");
    }

    @Test
    @DisplayName("upToSeller: user là admin -> từ chối nâng cấp")
    public void testUpToSeller_LaAdmin() {
        Admin mockAdmin = new Admin();
        mockAdmin.setId(15);
        mockAdmin.setUsername("phanbuom");

        when(userRepository.findById(15)).thenReturn(Optional.of(mockAdmin));

        Map<String, Object> result = bidderService.upToSeller(15);

        assertFalse((Boolean) result.get("success"));
        assertEquals("Account is not a BIDDER or is already a SELLER", result.get("message"));

        verify(userRepository, never()).updateRoleById(anyInt(), anyString());
    }
}