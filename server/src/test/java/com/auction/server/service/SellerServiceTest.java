package com.auction.server.service;

import com.auction.server.dto.CreateAuctionRequest;
import com.auction.server.dto.SellerStatsDTO;
import com.auction.server.dto.SessionResponseDTO;
import com.auction.server.exception.InvalidItemException;
import com.auction.server.model.AuctionSession;
import com.auction.server.model.AuctionStatus;
import com.auction.server.model.Item;
import com.auction.server.model.Seller;
import com.auction.server.repository.AuctionSessionRepository;
import com.auction.server.repository.ItemRepository;
import com.auction.server.util.SellerSessionGuard;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SellerServiceTest {

    private final FakeItemRepository itemRepository = new FakeItemRepository();
    private final FakeAuctionSessionRepository auctionSessionRepository = new FakeAuctionSessionRepository();
    private final FakeSellerSessionGuard sellerSessionGuard = new FakeSellerSessionGuard();

    private final SellerService sellerService = new SellerService(
            itemRepository.proxy(),
            auctionSessionRepository.proxy(),
            sellerSessionGuard
    );

    @Test
    void createAuctionSession_validRequest_savesItemAndPendingSession() {
        Seller seller = seller(1, "seller01");
        sellerSessionGuard.seller = seller;

        CreateAuctionRequest request = validRequest();
        request.setStartTime(null);

        SessionResponseDTO result = sellerService.createAuctionSession(request);

        assertSame(itemRepository.savedItem, auctionSessionRepository.savedSession.getItem());
        assertSame(seller, auctionSessionRepository.savedSession.getSeller());
        assertEquals(AuctionStatus.PENDING, auctionSessionRepository.savedSession.getStatus());
        assertNotNull(auctionSessionRepository.savedSession.getStartTime());
        assertEquals(new BigDecimal("1000000"), auctionSessionRepository.savedSession.getStartingPrice());
        assertEquals(new BigDecimal("1000000"), auctionSessionRepository.savedSession.getCurrentPrice());
        assertEquals(new BigDecimal("100000"), auctionSessionRepository.savedSession.getStepPrice());
        assertEquals(new BigDecimal("1000000"), auctionSessionRepository.savedSession.getReservePrice());
        assertNull(auctionSessionRepository.savedSession.getHighestBidderId());

        assertEquals("Laptop Gaming", itemRepository.savedItem.getName());
        assertEquals("electronics", itemRepository.savedItem.getType());
        assertEquals("Máy còn tốt", itemRepository.savedItem.getDescription());

        assertEquals("Laptop Gaming", result.getProductName());
        assertEquals("electronics", result.getProductType());
        assertEquals("PENDING", result.getStatus());
        assertEquals(new BigDecimal("1000000"), result.getReservePrice());
    }

    @Test
    void createAuctionSession_invalidRequest_throwsExceptionAndDoesNotSave() {
        CreateAuctionRequest request = validRequest();
        request.setName(" ");

        assertThrows(InvalidItemException.class, () -> sellerService.createAuctionSession(request));

        assertNull(itemRepository.savedItem);
        assertNull(auctionSessionRepository.savedSession);
    }

    @Test
    void updatePendingSession_validOwnerAndPending_updatesItemAndSession() {
        Seller seller = seller(1, "seller01");
        AuctionSession session = session(10, seller, AuctionStatus.PENDING, "Old Laptop", "500000");
        CreateAuctionRequest request = validRequest();
        request.setName("New Laptop");
        request.setType("art");
        request.setDescription("Mô tả mới");
        request.setStartingPrice(new BigDecimal("2000000"));
        request.setStepPrice(new BigDecimal("200000"));
        request.setReservePrice(new BigDecimal("2500000"));
        session.setHighestBidderId(99);

        sellerSessionGuard.seller = seller;
        sellerSessionGuard.session = session;

        SessionResponseDTO result = sellerService.updatePendingSession(10, 1, request);

        assertTrue(sellerSessionGuard.validateOwnerCalled);
        assertTrue(sellerSessionGuard.validatePendingCalled);
        assertSame(session.getItem(), itemRepository.savedItem);
        assertSame(session, auctionSessionRepository.savedSession);

        assertEquals("New Laptop", session.getItem().getName());
        assertEquals("art", session.getItem().getType());
        assertEquals("Mô tả mới", session.getItem().getDescription());
        assertEquals(new BigDecimal("2000000"), session.getStartingPrice());
        assertEquals(new BigDecimal("2000000"), session.getCurrentPrice());
        assertEquals(new BigDecimal("200000"), session.getStepPrice());
        assertEquals(new BigDecimal("2500000"), session.getReservePrice());
        assertNull(session.getHighestBidderId());

        assertEquals("New Laptop", result.getProductName());
        assertEquals("art", result.getProductType());
        assertEquals(new BigDecimal("2500000"), result.getReservePrice());
    }

    @Test
    void updatePendingSession_wrongOwner_throwsExceptionAndDoesNotSave() {
        Seller seller = seller(1, "seller01");
        AuctionSession session = session(10, seller, AuctionStatus.PENDING, "Laptop", "500000");

        sellerSessionGuard.seller = seller;
        sellerSessionGuard.session = session;
        sellerSessionGuard.throwOnValidateOwner = true;

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> sellerService.updatePendingSession(10, 2, validRequest())
        );

        assertEquals("Bạn không có quyền sửa phiên này", ex.getMessage());
        assertNull(itemRepository.savedItem);
        assertNull(auctionSessionRepository.savedSession);
    }

    @Test
    void updatePendingSession_notPending_throwsExceptionAndDoesNotSave() {
        Seller seller = seller(1, "seller01");
        AuctionSession session = session(10, seller, AuctionStatus.ACTIVE, "Laptop", "500000");

        sellerSessionGuard.seller = seller;
        sellerSessionGuard.session = session;
        sellerSessionGuard.throwOnValidatePending = true;

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> sellerService.updatePendingSession(10, 1, validRequest())
        );

        assertEquals("Chỉ được thao tác với phiên đang chờ duyệt", ex.getMessage());
        assertNull(itemRepository.savedItem);
        assertNull(auctionSessionRepository.savedSession);
    }

    @Test
    void getMySessions_withoutStatus_returnsAllSellerSessions() {
        Seller seller = seller(1, "seller01");
        AuctionSession session = session(10, seller, AuctionStatus.PENDING, "Laptop", "1000");

        sellerSessionGuard.seller = seller;
        auctionSessionRepository.sessionsBySeller = List.of(session);

        List<SessionResponseDTO> result = sellerService.getMySessions(1, null);

        assertEquals(1, result.size());
        assertEquals(10, result.get(0).getId());
        assertEquals("Laptop", result.get(0).getProductName());
        assertEquals("PENDING", result.get(0).getStatus());

        assertEquals(1, sellerSessionGuard.lastSellerId);
        assertTrue(auctionSessionRepository.findBySellerCalled);
        assertFalse(auctionSessionRepository.findBySellerAndStatusCalled);
    }

    @Test
    void getMySessions_withStatus_returnsFilteredSessions() {
        Seller seller = seller(1, "seller01");
        AuctionSession session = session(11, seller, AuctionStatus.ACTIVE, "Phone", "2000");

        sellerSessionGuard.seller = seller;
        auctionSessionRepository.sessionsBySellerAndStatus = List.of(session);

        List<SessionResponseDTO> result = sellerService.getMySessions(1, "active");

        assertEquals(1, result.size());
        assertEquals(11, result.get(0).getId());
        assertEquals("Phone", result.get(0).getProductName());
        assertEquals("ACTIVE", result.get(0).getStatus());

        assertEquals(1, auctionSessionRepository.lastSellerId);
        assertEquals(AuctionStatus.ACTIVE, auctionSessionRepository.lastStatus);
    }

    @Test
    void getMySessions_invalidStatus_returnsEmptyList() {
        Seller seller = seller(1, "seller01");

        sellerSessionGuard.seller = seller;

        List<SessionResponseDTO> result = sellerService.getMySessions(1, "abc");

        assertTrue(result.isEmpty());
        assertEquals(1, sellerSessionGuard.lastSellerId);
        assertFalse(auctionSessionRepository.findBySellerCalled);
        assertFalse(auctionSessionRepository.findBySellerAndStatusCalled);
    }

    @Test
    void getSessionDetail_validOwner_returnsSessionDTO() {
        Seller seller = seller(1, "seller01");
        AuctionSession session = session(20, seller, AuctionStatus.PENDING, "Watch", "3000");

        sellerSessionGuard.seller = seller;
        sellerSessionGuard.session = session;

        SessionResponseDTO result = sellerService.getSessionDetail(20, 1);

        assertEquals(20, result.getId());
        assertEquals("Watch", result.getProductName());
        assertEquals("PENDING", result.getStatus());

        assertEquals(1, sellerSessionGuard.lastSellerId);
        assertEquals(20, sellerSessionGuard.lastSessionId);
        assertTrue(sellerSessionGuard.validateOwnerCalled);
    }

    @Test
    void cancelSession_pendingSession_setsStatusCanceledAndSaves() {
        Seller seller = seller(1, "seller01");
        AuctionSession session = session(30, seller, AuctionStatus.PENDING, "Camera", "4000");

        sellerSessionGuard.seller = seller;
        sellerSessionGuard.session = session;

        sellerService.cancelSession(30, 1);

        assertEquals(AuctionStatus.CANCELED, session.getStatus());
        assertTrue(sellerSessionGuard.validateOwnerCalled);
        assertTrue(sellerSessionGuard.validatePendingCalled);
        assertSame(session, auctionSessionRepository.savedSession);
    }

    @Test
    void cancelSession_notPending_throwsExceptionAndDoesNotSave() {
        Seller seller = seller(1, "seller01");
        AuctionSession session = session(30, seller, AuctionStatus.ACTIVE, "Camera", "4000");

        sellerSessionGuard.seller = seller;
        sellerSessionGuard.session = session;
        sellerSessionGuard.throwOnValidatePending = true;

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> sellerService.cancelSession(30, 1)
        );

        assertEquals("Chỉ được thao tác với phiên đang chờ duyệt", ex.getMessage());
        assertNull(auctionSessionRepository.savedSession);
    }

    @Test
    void getSellerStats_sumsEndedSessionRevenueIgnoringNullPrice() {
        Seller seller = seller(1, "seller01");

        AuctionSession session1 = session(40, seller, AuctionStatus.ENDED, "A", "1000");
        session1.setCurrentPrice(new BigDecimal("1500"));

        AuctionSession session2 = session(41, seller, AuctionStatus.ENDED, "B", "2000");
        session2.setCurrentPrice(new BigDecimal("2500"));

        AuctionSession session3 = session(42, seller, AuctionStatus.ENDED, "C", "3000");
        session3.setCurrentPrice(null);

        sellerSessionGuard.seller = seller;
        auctionSessionRepository.sessionsBySellerAndStatus = List.of(session1, session2, session3);

        SellerStatsDTO result = sellerService.getSellerStats(1);

        assertEquals(3, result.getTotalSoldItems());
        assertEquals(new BigDecimal("4000"), result.getTotalRevenue());
        assertEquals(AuctionStatus.ENDED, auctionSessionRepository.lastStatus);
    }

    private CreateAuctionRequest validRequest() {
        CreateAuctionRequest request = new CreateAuctionRequest();
        request.setName("Laptop Gaming");
        request.setType("electronics");
        request.setDescription("Máy còn tốt");
        request.setSellerId(1);
        request.setStartingPrice(new BigDecimal("1000000"));
        request.setStepPrice(new BigDecimal("100000"));
        request.setStartTime(LocalDateTime.now().plusHours(1));
        request.setEndTime(LocalDateTime.now().plusDays(1));
        return request;
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
            String productName,
            String price
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
        session.setStartingPrice(new BigDecimal(price));
        session.setCurrentPrice(new BigDecimal(price));
        session.setStepPrice(new BigDecimal("100"));
        session.setEndTime(LocalDateTime.now().plusDays(1));
        return session;
    }

    private static class TestItem extends Item {
        @Override
        public String getCategoryInfo() {
            return "TEST";
        }
    }

    private static class FakeItemRepository {
        private Item savedItem;

        ItemRepository proxy() {
            return (ItemRepository) Proxy.newProxyInstance(
                    ItemRepository.class.getClassLoader(),
                    new Class[]{ItemRepository.class},
                    (proxy, method, args) -> {
                        if ("save".equals(method.getName())) {
                            savedItem = (Item) args[0];
                            return savedItem;
                        }

                        if ("toString".equals(method.getName())) {
                            return "FakeItemRepository";
                        }

                        throw new UnsupportedOperationException(method.getName());
                    }
            );
        }
    }

    private static class FakeSellerSessionGuard extends SellerSessionGuard {
        private Seller seller;
        private AuctionSession session;

        private Integer lastSellerId;
        private Integer lastSessionId;

        private boolean validateOwnerCalled;
        private boolean validatePendingCalled;
        private boolean throwOnValidateOwner;
        private boolean throwOnValidatePending;

        FakeSellerSessionGuard() {
            super(null, null);
        }

        @Override
        public Seller getSellerById(Integer sellerId) {
            this.lastSellerId = sellerId;
            return seller;
        }

        @Override
        public AuctionSession getSessionById(Integer sessionId) {
            this.lastSessionId = sessionId;
            return session;
        }

        @Override
        public void validateSessionOwner(AuctionSession session, Integer sellerId, String errorMessage) {
            this.validateOwnerCalled = true;
            if (throwOnValidateOwner) {
                throw new RuntimeException(errorMessage);
            }
        }

        @Override
        public void validatePendingSession(AuctionSession session) {
            this.validatePendingCalled = true;
            if (throwOnValidatePending) {
                throw new RuntimeException("Chỉ được thao tác với phiên đang chờ duyệt");
            }
        }
    }

    private static class FakeAuctionSessionRepository {
        private List<AuctionSession> sessionsBySeller = new ArrayList<>();
        private List<AuctionSession> sessionsBySellerAndStatus = new ArrayList<>();

        private Integer lastSellerId;
        private AuctionStatus lastStatus;

        private boolean findBySellerCalled;
        private boolean findBySellerAndStatusCalled;

        private AuctionSession savedSession;

        AuctionSessionRepository proxy() {
            return (AuctionSessionRepository) Proxy.newProxyInstance(
                    AuctionSessionRepository.class.getClassLoader(),
                    new Class[]{AuctionSessionRepository.class},
                    (proxy, method, args) -> handle(method.getName(), args)
            );
        }

        private Object handle(String methodName, Object[] args) {
            if ("findBySeller_Id".equals(methodName)) {
                findBySellerCalled = true;
                lastSellerId = (Integer) args[0];
                return sessionsBySeller;
            }

            if ("findBySeller_IdAndStatus".equals(methodName)) {
                findBySellerAndStatusCalled = true;
                lastSellerId = (Integer) args[0];
                lastStatus = (AuctionStatus) args[1];
                return sessionsBySellerAndStatus;
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
}
