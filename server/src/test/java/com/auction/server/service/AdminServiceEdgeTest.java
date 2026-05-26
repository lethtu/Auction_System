package com.auction.server.service;

import com.auction.server.dto.SessionResponseDTO;
import com.auction.server.dto.UserResponseDTO;
import com.auction.server.model.Admin;
import com.auction.server.model.AuctionSession;
import com.auction.server.model.AuctionStatus;
import com.auction.server.model.Seller;
import com.auction.server.model.User;
import com.auction.server.repository.AuctionSessionRepository;
import com.auction.server.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceEdgeTest {

    @Mock
    private AuctionSessionRepository sessionRepository;

    @Mock
    private UserRepository userRepository;

    private AdminService adminService;

    @BeforeEach
    void setUp() {
        adminService = new AdminService(sessionRepository, userRepository);
    }

    @Test
    void rejectSession_blankReason_throwsBeforeRepositoryLookup() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> adminService.rejectSession(10, 1, "   ")
        );

        assertEquals("Please enter a rejection reason", ex.getMessage());
        verifyNoInteractions(userRepository, sessionRepository);
    }

    @Test
    void getAllSessions_invalidStatus_returnsEmptyList() {
        List<SessionResponseDTO> result = adminService.getAllSessions("not-a-status");

        assertEquals(0, result.size());
        verifyNoInteractions(sessionRepository);
    }

    @Test
    void getAllSessions_blankStatus_returnsAllSessions() {
        AuctionSession draft = session(10, AuctionStatus.DRAFT);
        AuctionSession active = session(11, AuctionStatus.ACTIVE);
        when(sessionRepository.findAll()).thenReturn(List.of(draft, active));

        List<SessionResponseDTO> result = adminService.getAllSessions("  ");

        assertEquals(2, result.size());
        assertEquals(10, result.get(0).getId());
        assertEquals("DRAFT", result.get(0).getStatus());
        assertEquals(11, result.get(1).getId());
        assertEquals("ACTIVE", result.get(1).getStatus());
    }

    @Test
    void approveSession_pendingFutureStart_setsComingAndClearsRejectionInfo() {
        Admin admin = admin(1);
        AuctionSession session = session(20, AuctionStatus.DRAFT);
        session.setStartTime(LocalDateTime.now().plusDays(1));
        session.setRejectedAt(LocalDateTime.now().minusDays(1));
        session.setRejectedByAdminId(99);
        session.setRejectReason("old reason");

        when(userRepository.findById(1)).thenReturn(Optional.of(admin));
        when(sessionRepository.findById(20)).thenReturn(Optional.of(session));

        adminService.approveSession(20, 1);

        assertEquals(AuctionStatus.COMING, session.getStatus());
        assertEquals(1, session.getApprovedByAdminId());
        assertNull(session.getRejectedAt());
        assertNull(session.getRejectedByAdminId());
        assertNull(session.getRejectReason());
        verify(sessionRepository).save(session);
    }

    @Test
    void getAllUsers_filtersRoleCaseInsensitiveAndDefaultsNullFields() {
        Seller seller = new Seller();
        seller.setId(30);
        seller.setUsername("seller01");
        seller.setFullname(null);
        seller.setEmail(null);
        seller.setBalance(null);
        seller.setBanned(false);

        User user = new User();
        user.setId(31);
        user.setUsername("user01");
        user.setAccountType("USER");

        when(userRepository.findAll()).thenReturn(List.of(seller, user));

        List<UserResponseDTO> sellers = adminService.getAllUsers(" SELLER ");

        assertEquals(1, sellers.size());
        UserResponseDTO dto = sellers.get(0);
        assertEquals(30, dto.getId());
        assertEquals("seller01", dto.getUsername());
        assertEquals("", dto.getFullname());
        assertEquals("", dto.getEmail());
        assertEquals("seller", dto.getAccountType());
        assertEquals(BigDecimal.ZERO, dto.getBalance());
        assertFalse(dto.isBanned());
    }

    @Test
    void restoreUser_nullTarget_throwsAfterAdminPermissionCheck() {
        Admin admin = admin(1);
        when(userRepository.findById(1)).thenReturn(Optional.of(admin));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> adminService.restoreUser(null, 1)
        );

        assertEquals("User to restore not found", ex.getMessage());
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private Admin admin(Integer id) {
        Admin admin = new Admin();
        admin.setId(id);
        admin.setUsername("admin" + id);
        admin.setFullname("Admin " + id);
        admin.setEmail("admin" + id + "@gmail.com");
        return admin;
    }

    private AuctionSession session(Integer id, AuctionStatus status) {
        AuctionSession session = new AuctionSession();
        session.setId(id);
        session.setStatus(status);
        session.setStartingPrice(new BigDecimal("1000"));
        session.setCurrentPrice(new BigDecimal("1000"));
        session.setStepPrice(new BigDecimal("100"));
        return session;
    }
}