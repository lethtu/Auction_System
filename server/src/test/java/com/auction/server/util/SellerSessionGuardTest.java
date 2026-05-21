package com.auction.server.util;

import com.auction.server.model.AuctionSession;
import com.auction.server.model.AuctionStatus;
import com.auction.server.model.Seller;
import com.auction.server.model.User;
import com.auction.server.repository.AuctionSessionRepository;
import com.auction.server.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SellerSessionGuardTest {

    private final FakeUserRepository userRepository = new FakeUserRepository();
    private final FakeAuctionSessionRepository sessionRepository = new FakeAuctionSessionRepository();
    private final SellerSessionGuard guard = new SellerSessionGuard(
            userRepository.proxy(),
            sessionRepository.proxy()
    );

    @Test
    void getSellerById_existingSeller_returnsSeller() {
        Seller seller = seller(1);
        userRepository.users.put(1, seller);

        Seller result = guard.getSellerById(1);

        assertSame(seller, result);
    }

    @Test
    void getSellerById_missingUser_throwsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> guard.getSellerById(99));

        assertEquals("Không tìm thấy người bán", ex.getMessage());
    }

    @Test
    void getSellerById_nullId_throwsCleanException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> guard.getSellerById(null));

        assertEquals("Không tìm thấy người bán", ex.getMessage());
    }

    @Test
    void getSellerById_userIsNotSeller_throwsException() {
        User user = new User();
        user.setId(2);
        userRepository.users.put(2, user);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> guard.getSellerById(2));

        assertEquals("Người dùng này không phải seller", ex.getMessage());
    }

    @Test
    void getSessionById_missingSession_throwsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> guard.getSessionById(10));

        assertEquals("Phiên đấu giá không tồn tại", ex.getMessage());
    }

    @Test
    void getSessionById_nullId_throwsCleanException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> guard.getSessionById(null));

        assertEquals("Phiên đấu giá không tồn tại", ex.getMessage());
    }

    private Seller seller(Integer id) {
        Seller seller = new Seller();
        seller.setId(id);
        seller.setUsername("seller" + id);
        return seller;
    }

    private AuctionSession session(Integer id, Seller seller, AuctionStatus status) {
        AuctionSession session = new AuctionSession();
        session.setId(id);
        session.setSeller(seller);
        session.setStatus(status);
        return session;
    }

    private static class FakeUserRepository {
        private final Map<Integer, User> users = new HashMap<>();

        UserRepository proxy() {
            return (UserRepository) Proxy.newProxyInstance(
                    UserRepository.class.getClassLoader(),
                    new Class[]{UserRepository.class},
                    (proxy, method, args) -> {
                        if ("findById".equals(method.getName())) {
                            return Optional.ofNullable(users.get((Integer) args[0]));
                        }

                        if ("toString".equals(method.getName())) {
                            return "FakeUserRepository";
                        }

                        throw new UnsupportedOperationException(method.getName());
                    }
            );
        }
    }

    private static class FakeAuctionSessionRepository {
        private final Map<Integer, AuctionSession> sessions = new HashMap<>();

        AuctionSessionRepository proxy() {
            return (AuctionSessionRepository) Proxy.newProxyInstance(
                    AuctionSessionRepository.class.getClassLoader(),
                    new Class[]{AuctionSessionRepository.class},
                    (proxy, method, args) -> {
                        if ("findById".equals(method.getName())) {
                            return Optional.ofNullable(sessions.get((Integer) args[0]));
                        }

                        if ("toString".equals(method.getName())) {
                            return "FakeAuctionSessionRepository";
                        }

                        throw new UnsupportedOperationException(method.getName());
                    }
            );
        }
    }
}
