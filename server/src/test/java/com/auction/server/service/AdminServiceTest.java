package com.auction.server.service;

import com.auction.server.dto.SessionResponseDTO;
import com.auction.server.dto.UserResponseDTO;
import com.auction.server.model.Admin;
import com.auction.server.model.AuctionSession;
import com.auction.server.model.AuctionStatus;
import com.auction.server.model.Item;
import com.auction.server.model.Seller;
import com.auction.server.model.User;
import com.auction.server.repository.AuctionSessionRepository;
import com.auction.server.repository.ItemRepository;
import com.auction.server.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AdminServiceTest {

    private final FakeAuctionSessionRepository sessionRepository = new FakeAuctionSessionRepository();
    private final FakeUserRepository userRepository = new FakeUserRepository();

    private final AdminService adminService = new AdminService(
            sessionRepository.proxy(),
            userRepository.proxy()
    );

    @Test
    void getPendingSessions_returnsDraftSessionsAsDtos() {
        Seller seller = seller(2, "seller01");
        AuctionSession draft = session(10, seller, AuctionStatus.DRAFT, "Laptop");
        AuctionSession active = session(11, seller, AuctionStatus.ACTIVE, "Phone");

        sessionRepository.allSessions = List.of(draft, active);

        List<SessionResponseDTO> result = adminService.getPendingSessions();

        assertEquals(1, result.size());
        assertEquals(10, result.get(0).getId());
        assertEquals("Laptop", result.get(0).getProductName());
        assertEquals("DRAFT", result.get(0).getStatus());
    }

    @Test
    void getAllSessions_blankStatus_returnsAllSessions() {
        Seller seller = seller(2, "seller01");
        sessionRepository.allSessions = List.of(
                session(10, seller, AuctionStatus.DRAFT, "Laptop"),
                session(11, seller, AuctionStatus.ACTIVE, "Phone")
        );

        List<SessionResponseDTO> result = adminService.getAllSessions("   ");

        assertEquals(2, result.size());
        assertTrue(sessionRepository.findAllCalled);
    }

    @Test
    void getAllSessions_validStatus_returnsMatchingSessions() {
        Seller seller = seller(2, "seller01");
        sessionRepository.allSessions = List.of(
                session(10, seller, AuctionStatus.DRAFT, "Laptop"),
                session(11, seller, AuctionStatus.ACTIVE, "Phone")
        );

        List<SessionResponseDTO> result = adminService.getAllSessions(" active ");

        assertEquals(1, result.size());
        assertEquals(11, result.get(0).getId());
        assertEquals("ACTIVE", result.get(0).getStatus());
    }

    @Test
    void getAllSessions_invalidStatus_returnsEmptyList() {
        List<SessionResponseDTO> result = adminService.getAllSessions("abc");

        assertTrue(result.isEmpty());
    }

    @Test
    void getSessionDetail_existingSession_returnsDto() {
        Seller seller = seller(2, "seller01");
        AuctionSession session = session(10, seller, AuctionStatus.ACTIVE, "Laptop");
        sessionRepository.sessionsById.put(10, session);

        SessionResponseDTO result = adminService.getSessionDetail(10);

        assertEquals(10, result.getId());
        assertEquals("Laptop", result.getProductName());
        assertEquals("seller01", result.getSellerUsername());
    }

    @Test
    void getSessionDetail_nullId_throwsException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> adminService.getSessionDetail(null)
        );

        assertEquals("Auction session not found", ex.getMessage());
    }

    @Test
    void getSessionDetail_missingSession_throwsException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> adminService.getSessionDetail(404)
        );

        assertEquals("Auction session not found", ex.getMessage());
    }

    @Test
    void approveSession_nullStartTime_setsActiveAndClearsRejectionInfo() {
        Admin admin = admin(1, "admin01");
        Seller seller = seller(2, "seller01");
        AuctionSession session = session(10, seller, AuctionStatus.DRAFT, "Laptop");
        session.setStartTime(null);
        session.setRejectedAt(LocalDateTime.now().minusDays(1));
        session.setRejectedByAdminId(99);
        session.setRejectReason("old reason");

        userRepository.usersById.put(1, admin);
        sessionRepository.sessionsById.put(10, session);

        adminService.approveSession(10, 1);

        assertEquals(AuctionStatus.ACTIVE, session.getStatus());
        assertNotNull(session.getStartTime());
        assertNotNull(session.getApprovedAt());
        assertEquals(1, session.getApprovedByAdminId());
        assertNull(session.getRejectedAt());
        assertNull(session.getRejectedByAdminId());
        assertNull(session.getRejectReason());
        assertSame(session, sessionRepository.savedSession);
    }

    @Test
    void approveSession_futureStartTime_setsComing() {
        Admin admin = admin(1, "admin01");
        Seller seller = seller(2, "seller01");
        AuctionSession session = session(10, seller, AuctionStatus.DRAFT, "Laptop");
        LocalDateTime future = LocalDateTime.now().plusDays(1);
        session.setStartTime(future);

        userRepository.usersById.put(1, admin);
        sessionRepository.sessionsById.put(10, session);

        adminService.approveSession(10, 1);

        assertEquals(AuctionStatus.COMING, session.getStatus());
        assertEquals(future, session.getStartTime());
        assertEquals(1, session.getApprovedByAdminId());
    }

    @Test
    void approveSession_nonDraft_throwsException() {
        Admin admin = admin(1, "admin01");
        Seller seller = seller(2, "seller01");
        AuctionSession session = session(10, seller, AuctionStatus.ACTIVE, "Laptop");

        userRepository.usersById.put(1, admin);
        sessionRepository.sessionsById.put(10, session);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> adminService.approveSession(10, 1)
        );

        assertEquals("This session has already been processed or is not in pending status", ex.getMessage());
    }

    @Test
    void approveSession_missingAdmin_throwsException() {
        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> adminService.approveSession(10, 999)
        );

        assertEquals("Admin not found", ex.getMessage());
    }

    @Test
    void approveSession_nonAdmin_throwsException() {
        Seller notAdmin = seller(2, "seller01");

        userRepository.usersById.put(2, notAdmin);

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> adminService.approveSession(10, 2)
        );

        assertEquals("This user is not an Administrator", ex.getMessage());
    }

    @Test
    void rejectSession_validDraft_trimsReasonAndClearsApprovalInfo() {
        Admin admin = admin(1, "admin01");
        Seller seller = seller(2, "seller01");
        AuctionSession session = session(10, seller, AuctionStatus.DRAFT, "Laptop");
        session.setStartTime(LocalDateTime.now().plusDays(1));
        session.setApprovedAt(LocalDateTime.now().minusDays(1));
        session.setApprovedByAdminId(99);

        userRepository.usersById.put(1, admin);
        sessionRepository.sessionsById.put(10, session);

        adminService.rejectSession(10, 1, "  duplicate product  ");

        assertEquals(AuctionStatus.CANCELED, session.getStatus());
        assertEquals("duplicate product", session.getRejectReason());
        assertNotNull(session.getRejectedAt());
        assertEquals(1, session.getRejectedByAdminId());
        assertNull(session.getApprovedAt());
        assertNull(session.getApprovedByAdminId());
        assertNull(session.getStartTime());
        assertSame(session, sessionRepository.savedSession);
    }

    @Test
    void rejectSession_emptyReason_throwsException() {
        Admin admin = admin(1, "admin01");

        userRepository.usersById.put(1, admin);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> adminService.rejectSession(10, 1, "   ")
        );

        assertEquals("Please enter a rejection reason", ex.getMessage());
    }

    @Test
    void rejectSession_nonDraft_throwsException() {
        Admin admin = admin(1, "admin01");
        Seller seller = seller(2, "seller01");
        AuctionSession session = session(10, seller, AuctionStatus.ACTIVE, "Laptop");

        userRepository.usersById.put(1, admin);
        sessionRepository.sessionsById.put(10, session);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> adminService.rejectSession(10, 1, "bad data")
        );

        assertEquals("Can only reject sessions in pending status", ex.getMessage());
    }

    @Test
    void banUser_validSeller_setsBannedTrue() {
        Admin admin = admin(1, "admin01");
        Seller seller = seller(2, "seller01");

        userRepository.usersById.put(1, admin);
        userRepository.usersById.put(2, seller);

        adminService.banUser(2, 1);

        assertTrue(seller.isBanned());
        assertSame(seller, userRepository.savedUser);
    }

    @Test
    void banUser_nullTarget_throwsSelfBanException() {
        Admin admin = admin(1, "admin01");
        userRepository.usersById.put(1, admin);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> adminService.banUser(null, 1)
        );

        assertEquals("Cannot ban your own admin account", ex.getMessage());
    }

    @Test
    void banUser_selfAdmin_throwsException() {
        Admin admin = admin(1, "admin01");

        userRepository.usersById.put(1, admin);

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> adminService.banUser(1, 1)
        );

        assertEquals("Cannot ban your own admin account", ex.getMessage());
    }

    @Test
    void banUser_targetMissing_throwsException() {
        Admin admin = admin(1, "admin01");
        userRepository.usersById.put(1, admin);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> adminService.banUser(404, 1)
        );

        assertEquals("Target user not found", ex.getMessage());
    }

    @Test
    void banUser_targetAdmin_throwsException() {
        Admin admin = admin(1, "admin01");
        Admin targetAdmin = admin(2, "admin02");

        userRepository.usersById.put(1, admin);
        userRepository.usersById.put(2, targetAdmin);

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> adminService.banUser(2, 1)
        );

        assertEquals("Cannot ban another Admin account", ex.getMessage());
    }

    @Test
    void restoreUser_validTarget_setsBannedFalse() {
        Admin admin = admin(1, "admin01");
        Seller seller = seller(2, "seller01");
        seller.setBanned(true);

        userRepository.usersById.put(1, admin);
        userRepository.usersById.put(2, seller);

        adminService.restoreUser(2, 1);

        assertFalse(seller.isBanned());
        assertSame(seller, userRepository.savedUser);
    }

    @Test
    void restoreUser_missingTarget_throwsException() {
        Admin admin = admin(1, "admin01");
        userRepository.usersById.put(1, admin);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> adminService.restoreUser(404, 1)
        );

        assertEquals("User to restore not found", ex.getMessage());
    }

    @Test
    void cancelAuction_activeSession_setsCanceled() {
        Admin admin = admin(1, "admin01");
        Seller seller = seller(2, "seller01");
        AuctionSession session = session(10, seller, AuctionStatus.ACTIVE, "Laptop");

        userRepository.usersById.put(1, admin);
        sessionRepository.sessionsById.put(10, session);

        adminService.cancelAuction(10, 1);

        assertEquals(AuctionStatus.CANCELED, session.getStatus());
        assertSame(session, sessionRepository.savedSession);
    }

    @Test
    void cancelAuction_endedSession_throwsException() {
        Admin admin = admin(1, "admin01");
        Seller seller = seller(2, "seller01");
        AuctionSession session = session(10, seller, AuctionStatus.ENDED, "Laptop");

        userRepository.usersById.put(1, admin);
        sessionRepository.sessionsById.put(10, session);

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> adminService.cancelAuction(10, 1)
        );

        assertEquals("This session has already ended or been canceled", ex.getMessage());
    }

    @Test
    void cancelAuction_canceledSession_throwsException() {
        Admin admin = admin(1, "admin01");
        Seller seller = seller(2, "seller01");
        AuctionSession session = session(10, seller, AuctionStatus.CANCELED, "Laptop");

        userRepository.usersById.put(1, admin);
        sessionRepository.sessionsById.put(10, session);

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> adminService.cancelAuction(10, 1)
        );

        assertEquals("This session has already ended or been canceled", ex.getMessage());
    }

    @Test
    void hideProduct_validItem_setsHiddenTrue() {
        Admin admin = admin(1, "admin01");
        TestItem item = item(20, "Laptop");
        FakeItemRepository itemRepository = new FakeItemRepository();
        AdminService service = serviceWithItemRepository(itemRepository);

        userRepository.usersById.put(1, admin);
        itemRepository.itemsById.put(20, item);

        service.hideProduct(20, 1);

        assertTrue(item.isHidden());
        assertSame(item, itemRepository.savedItem);
    }

    @Test
    void showProduct_validItem_setsHiddenFalse() {
        Admin admin = admin(1, "admin01");
        TestItem item = item(20, "Laptop");
        item.setHidden(true);
        FakeItemRepository itemRepository = new FakeItemRepository();
        AdminService service = serviceWithItemRepository(itemRepository);

        userRepository.usersById.put(1, admin);
        itemRepository.itemsById.put(20, item);

        service.showProduct(20, 1);

        assertFalse(item.isHidden());
        assertSame(item, itemRepository.savedItem);
    }

    @Test
    void hideProduct_withoutItemRepository_throwsException() {
        Admin admin = admin(1, "admin01");
        userRepository.usersById.put(1, admin);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> adminService.hideProduct(20, 1)
        );

        assertEquals("Product data repository not configured", ex.getMessage());
    }

    @Test
    void hideProduct_nullProductId_throwsException() {
        Admin admin = admin(1, "admin01");
        FakeItemRepository itemRepository = new FakeItemRepository();
        AdminService service = serviceWithItemRepository(itemRepository);

        userRepository.usersById.put(1, admin);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.hideProduct(null, 1)
        );

        assertEquals("Product not found", ex.getMessage());
    }

    @Test
    void hideProduct_missingProduct_throwsException() {
        Admin admin = admin(1, "admin01");
        FakeItemRepository itemRepository = new FakeItemRepository();
        AdminService service = serviceWithItemRepository(itemRepository);

        userRepository.usersById.put(1, admin);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.hideProduct(404, 1)
        );

        assertEquals("Product not found", ex.getMessage());
    }

    @Test
    void getAllUsers_withoutRole_returnsAllUsers() {
        Admin admin = admin(1, "admin01");
        Seller seller = seller(2, "seller01");

        userRepository.allUsers = List.of(admin, seller);

        List<UserResponseDTO> result = adminService.getAllUsers(null);

        assertEquals(2, result.size());
        assertEquals("admin01", result.get(0).getUsername());
        assertEquals("seller01", result.get(1).getUsername());
    }

    @Test
    void getAllUsers_blankRole_returnsAllUsers() {
        Admin admin = admin(1, "admin01");
        Seller seller = seller(2, "seller01");

        userRepository.allUsers = List.of(admin, seller);

        List<UserResponseDTO> result = adminService.getAllUsers("  ");

        assertEquals(2, result.size());
    }

    @Test
    void getAllUsers_withRole_returnsFilteredUsers() {
        Admin admin = admin(1, "admin01");
        Seller seller = seller(2, "seller01");

        setAccountType(admin, "admin");
        setAccountType(seller, "seller");

        userRepository.allUsers = List.of(admin, seller);

        List<UserResponseDTO> result = adminService.getAllUsers("seller");

        assertEquals(1, result.size());
        assertEquals("seller01", result.get(0).getUsername());
        assertEquals("seller", result.get(0).getAccountType());
    }

    @Test
    void getAllUsers_roleFilteringIsTrimmedAndCaseInsensitive() {
        Admin admin = admin(1, "admin01");
        Seller seller = seller(2, "seller01");

        setAccountType(admin, "ADMIN");
        setAccountType(seller, " Seller ");

        userRepository.allUsers = List.of(admin, seller);

        List<UserResponseDTO> result = adminService.getAllUsers(" SELLER ");

        assertEquals(1, result.size());
        assertEquals("seller01", result.get(0).getUsername());
        assertEquals("seller", result.get(0).getAccountType());
    }

    @Test
    void getAllUsers_nullFields_mapsSafeDefaults() {
        User user = new User();
        user.setId(3);
        user.setUsername(null);
        user.setFullname(null);
        user.setEmail(null);
        user.setBalance(null);
        setAccountType(user, null);

        userRepository.allUsers = List.of(user);

        List<UserResponseDTO> result = adminService.getAllUsers(null);

        assertEquals(1, result.size());
        assertEquals("", result.get(0).getUsername());
        assertEquals("", result.get(0).getFullname());
        assertEquals("", result.get(0).getEmail());
        assertEquals("user", result.get(0).getAccountType());
        assertEquals(BigDecimal.ZERO, result.get(0).getBalance());
        assertFalse(result.get(0).isBanned());
    }

    private AdminService serviceWithItemRepository(FakeItemRepository itemRepository) {
        return new AdminService(
                sessionRepository.proxy(),
                userRepository.proxy(),
                itemRepository.proxy()
        );
    }

    private Admin admin(Integer id, String username) {
        Admin admin = new Admin();
        admin.setId(id);
        admin.setUsername(username);
        admin.setFullname("Admin Fullname");
        admin.setEmail(username + "@gmail.com");
        return admin;
    }

    private Seller seller(Integer id, String username) {
        Seller seller = new Seller();
        seller.setId(id);
        seller.setUsername(username);
        seller.setFullname("Seller Fullname");
        seller.setEmail(username + "@gmail.com");
        return seller;
    }

    private AuctionSession session(
            Integer id,
            Seller seller,
            AuctionStatus status,
            String productName
    ) {
        TestItem item = item(id + 100, productName);

        AuctionSession session = new AuctionSession();
        session.setId(id);
        session.setSeller(seller);
        session.setItem(item);
        session.setStatus(status);
        session.setStartingPrice(new BigDecimal("1000"));
        session.setCurrentPrice(new BigDecimal("1000"));
        session.setStepPrice(new BigDecimal("100"));
        session.setReservePrice(new BigDecimal("1500"));
        session.setHighestBidderId(9);
        return session;
    }

    private TestItem item(Integer id, String productName) {
        TestItem item = new TestItem();
        item.setId(id);
        item.setName(productName);
        item.setType("TEST");
        item.setDescription("Description");
        return item;
    }

    private void setAccountType(User user, String accountType) {
        try {
            Field field = User.class.getDeclaredField("accountType");
            field.setAccessible(true);
            field.set(user, accountType);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static class TestItem extends Item {
        @Override
        public String getCategoryInfo() {
            return "TEST";
        }
    }

    private static class FakeAuctionSessionRepository {
        private List<AuctionSession> allSessions = new ArrayList<>();
        private final Map<Integer, AuctionSession> sessionsById = new HashMap<>();

        private boolean findAllCalled;
        private AuctionSession savedSession;

        AuctionSessionRepository proxy() {
            return (AuctionSessionRepository) Proxy.newProxyInstance(
                    AuctionSessionRepository.class.getClassLoader(),
                    new Class[]{AuctionSessionRepository.class},
                    (proxy, method, args) -> handle(method.getName(), args)
            );
        }

        private Object handle(String methodName, Object[] args) {
            if ("findByStatus".equals(methodName)) {
                AuctionStatus status = (AuctionStatus) args[0];
                return allSessions.stream()
                        .filter(session -> session.getStatus() == status)
                        .toList();
            }

            if ("findAll".equals(methodName)) {
                findAllCalled = true;
                return allSessions;
            }

            if ("findById".equals(methodName)) {
                return Optional.ofNullable(sessionsById.get((Integer) args[0]));
            }

            if ("save".equals(methodName)) {
                savedSession = (AuctionSession) args[0];
                return savedSession;
            }

            if ("toString".equals(methodName)) {
                return "FakeAuctionSessionRepository";
            }

            throw new UnsupportedOperationException(methodName);
        }
    }

    private static class FakeUserRepository {
        private List<User> allUsers = new ArrayList<>();
        private final Map<Integer, User> usersById = new HashMap<>();

        private User savedUser;

        UserRepository proxy() {
            return (UserRepository) Proxy.newProxyInstance(
                    UserRepository.class.getClassLoader(),
                    new Class[]{UserRepository.class},
                    (proxy, method, args) -> handle(method.getName(), args)
            );
        }

        private Object handle(String methodName, Object[] args) {
            if ("findById".equals(methodName)) {
                return Optional.ofNullable(usersById.get((Integer) args[0]));
            }

            if ("findAll".equals(methodName)) {
                return allUsers;
            }

            if ("save".equals(methodName)) {
                savedUser = (User) args[0];
                return savedUser;
            }

            if ("toString".equals(methodName)) {
                return "FakeUserRepository";
            }

            throw new UnsupportedOperationException(methodName);
        }
    }

    private static class FakeItemRepository {
        private final Map<Integer, Item> itemsById = new HashMap<>();
        private Item savedItem;

        ItemRepository proxy() {
            return (ItemRepository) Proxy.newProxyInstance(
                    ItemRepository.class.getClassLoader(),
                    new Class[]{ItemRepository.class},
                    (proxy, method, args) -> handle(method.getName(), args)
            );
        }

        private Object handle(String methodName, Object[] args) {
            if ("findById".equals(methodName)) {
                return Optional.ofNullable(itemsById.get((Integer) args[0]));
            }

            if ("save".equals(methodName)) {
                savedItem = (Item) args[0];
                return savedItem;
            }

            if ("toString".equals(methodName)) {
                return "FakeItemRepository";
            }

            throw new UnsupportedOperationException(methodName);
        }
    }
}
