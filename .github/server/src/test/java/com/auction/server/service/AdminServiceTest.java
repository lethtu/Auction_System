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
import com.auction.server.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
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
    void getAllSessions_invalidStatus_returnsEmptyList() {
        List<SessionResponseDTO> result = adminService.getAllSessions("abc");

        assertTrue(result.isEmpty());
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
        TestItem item = new TestItem();
        item.setId(id + 100);
        item.setName(productName);
        item.setType("TEST");
        item.setDescription("Description");

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
}