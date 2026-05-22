package com.auction.server.service;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AdminServiceExtraTest {

    private final FakeAuctionSessionRepository sessionRepository = new FakeAuctionSessionRepository();
    private final FakeUserRepository userRepository = new FakeUserRepository();
    private final AdminService adminService = new AdminService(sessionRepository.proxy(), userRepository.proxy());


    @Test
    void getSessionDetail_missingSession_throwsException() {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> adminService.getSessionDetail(99));

        assertEquals("Auction session not found", ex.getMessage());
    }

    @Test
    void approveSession_missingAdmin_throwsException() {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> adminService.approveSession(10, 99));

        assertEquals("Admin not found", ex.getMessage());
    }

    @Test
    void approveSession_notPending_throwsExceptionAndDoesNotSave() {
        Admin admin = admin(1, "admin01");
        Seller seller = seller(2, "seller01");
        AuctionSession session = session(10, seller, AuctionStatus.ACTIVE, "Laptop");

        userRepository.usersById.put(1, admin);
        sessionRepository.sessionsById.put(10, session);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> adminService.approveSession(10, 1));

        assertEquals("This session has already been processed or is not in pending status", ex.getMessage());
        assertNull(sessionRepository.savedSession);
    }

    @Test
    void rejectSession_notPending_throwsExceptionAndDoesNotSave() {
        Admin admin = admin(1, "admin01");
        Seller seller = seller(2, "seller01");
        AuctionSession session = session(10, seller, AuctionStatus.ACTIVE, "Laptop");

        userRepository.usersById.put(1, admin);
        sessionRepository.sessionsById.put(10, session);

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> adminService.rejectSession(10, 1, "Incorrect info")
        );

        assertEquals("Can only reject sessions in pending status", ex.getMessage());
        assertNull(sessionRepository.savedSession);
    }

    @Test
    void banUser_missingTarget_throwsException() {
        Admin admin = admin(1, "admin01");
        userRepository.usersById.put(1, admin);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> adminService.banUser(99, 1));

        assertEquals("Target user not found", ex.getMessage());
    }

    @Test
    void cancelAuction_canceledSession_throwsExceptionAndDoesNotSave() {
        Admin admin = admin(1, "admin01");
        Seller seller = seller(2, "seller01");
        AuctionSession session = session(10, seller, AuctionStatus.CANCELED, "Laptop");

        userRepository.usersById.put(1, admin);
        sessionRepository.sessionsById.put(10, session);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> adminService.cancelAuction(10, 1));

        assertEquals("This session has already ended or been canceled", ex.getMessage());
        assertNull(sessionRepository.savedSession);
    }

    private Admin admin(Integer id, String username) {
        Admin admin = new Admin();
        admin.setId(id);
        admin.setUsername(username);
        admin.setFullname("Admin Fullname");
        admin.setEmail(username + "@gmail.com");
        setAccountType(admin, "admin");
        return admin;
    }

    private Seller seller(Integer id, String username) {
        Seller seller = new Seller();
        seller.setId(id);
        seller.setUsername(username);
        seller.setFullname("Seller Fullname");
        seller.setEmail(username + "@gmail.com");
        setAccountType(seller, "seller");
        return seller;
    }

    private AuctionSession session(Integer id, Seller seller, AuctionStatus status, String productName) {
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
        private final List<AuctionSession> allSessions = new ArrayList<>();
        private final Map<Integer, AuctionSession> sessionsById = new HashMap<>();
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
        private final List<User> allUsers = new ArrayList<>();
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
            if ("findAll".equals(methodName)) {
                return allUsers;
            }

            if ("findById".equals(methodName)) {
                return Optional.ofNullable(usersById.get((Integer) args[0]));
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