package com.auction.server.service;

import com.auction.server.model.*;
import com.auction.server.repository.AuctionSessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuctionSchedulerServiceTest {

    @Mock
    private AuctionSessionRepository auctionSessionRepository;

    @Mock
    private AuctionService auctionService;

    @InjectMocks
    private AuctionSchedulerService auctionSchedulerService;

    @Test
    public void testScanAndUpdateAuctionStatus_NoChanges() {
        when(auctionSessionRepository.findByStatusAndStartTimeLessThanEqual(eq(AuctionStatus.COMING), any()))
                .thenReturn(Collections.emptyList());
        when(auctionSessionRepository.findByStatusAndEndTimeLessThanEqual(eq(AuctionStatus.ACTIVE), any()))
                .thenReturn(Collections.emptyList());

        auctionSchedulerService.scanAndUpdateAuctionStatus();

        verify(auctionSessionRepository, never()).saveAll(anyList());
        verify(auctionService, never()).endSession(anyInt());
    }

    @Test
    public void testScanAndUpdateAuctionStatus_WithChanges() {
        AuctionSession comingSession = new AuctionSession();
        comingSession.setId(1);
        comingSession.setStatus(AuctionStatus.COMING);

        AuctionSession activeSession = new AuctionSession();
        activeSession.setId(2);
        activeSession.setStatus(AuctionStatus.ACTIVE);
        activeSession.setHighestBidderId(10);
        activeSession.setTotalBids(5);
        Item item = new Art();
        item.setName("Masterpiece");
        activeSession.setItem(item);

        when(auctionSessionRepository.findByStatusAndStartTimeLessThanEqual(eq(AuctionStatus.COMING), any()))
                .thenReturn(Collections.singletonList(comingSession));
        when(auctionSessionRepository.findByStatusAndEndTimeLessThanEqual(eq(AuctionStatus.ACTIVE), any()))
                .thenReturn(Collections.singletonList(activeSession));

        AuctionSession endedSession = new AuctionSession();
        endedSession.setId(2);
        endedSession.setStatus(AuctionStatus.ENDED);
        endedSession.setHighestBidderId(10);
        endedSession.setItem(item);
        when(auctionSessionRepository.findById(2)).thenReturn(Optional.of(endedSession));

        auctionSchedulerService.scanAndUpdateAuctionStatus();

        verify(auctionSessionRepository, times(1)).saveAll(Collections.singletonList(comingSession));
        verify(auctionService, times(1)).endSession(2);
    }

    @Test
    void scanAndUpdateAuctionStatus_endSessionThrows_stillContinuesBroadcastLoopSafely() {
        AuctionSession activeSession = new AuctionSession();
        activeSession.setId(3);
        activeSession.setStatus(AuctionStatus.ACTIVE);

        when(auctionSessionRepository.findByStatusAndStartTimeLessThanEqual(eq(AuctionStatus.COMING), any()))
                .thenReturn(Collections.emptyList());
        when(auctionSessionRepository.findByStatusAndEndTimeLessThanEqual(eq(AuctionStatus.ACTIVE), any()))
                .thenReturn(Collections.singletonList(activeSession));
        doThrow(new RuntimeException("boom")).when(auctionService).endSession(3);
        when(auctionSessionRepository.findById(3)).thenReturn(Optional.empty());

        auctionSchedulerService.scanAndUpdateAuctionStatus();

        verify(auctionService).endSession(3);
        verify(auctionSessionRepository).findById(3);
        verify(auctionSessionRepository, never()).saveAll(anyList());
    }

    @Test
    void scanAndUpdateAuctionStatus_refetchedSessionWithNullId_isSkippedSafely() {
        AuctionSession activeSession = new AuctionSession();
        activeSession.setId(4);
        activeSession.setStatus(AuctionStatus.ACTIVE);

        AuctionSession updatedWithoutId = new AuctionSession();
        updatedWithoutId.setStatus(AuctionStatus.ENDED);

        when(auctionSessionRepository.findByStatusAndStartTimeLessThanEqual(eq(AuctionStatus.COMING), any()))
                .thenReturn(Collections.emptyList());
        when(auctionSessionRepository.findByStatusAndEndTimeLessThanEqual(eq(AuctionStatus.ACTIVE), any()))
                .thenReturn(Collections.singletonList(activeSession));
        when(auctionSessionRepository.findById(4)).thenReturn(Optional.of(updatedWithoutId));

        auctionSchedulerService.scanAndUpdateAuctionStatus();

        verify(auctionService).endSession(4);
        verify(auctionSessionRepository).findById(4);
    }

    @Test
    void scanAndUpdateAuctionStatus_endedWithoutWinnerAndWithFinalPrice_isHandledSafely() {
        AuctionSession activeSession = new AuctionSession();
        activeSession.setId(5);
        activeSession.setStatus(AuctionStatus.ACTIVE);

        AuctionSession endedWithoutWinner = new AuctionSession();
        endedWithoutWinner.setId(5);
        endedWithoutWinner.setStatus(AuctionStatus.ENDED);
        endedWithoutWinner.setHighestBidderId(null);
        endedWithoutWinner.setCurrentPrice(new BigDecimal("125000"));

        when(auctionSessionRepository.findByStatusAndStartTimeLessThanEqual(eq(AuctionStatus.COMING), any()))
                .thenReturn(Collections.emptyList());
        when(auctionSessionRepository.findByStatusAndEndTimeLessThanEqual(eq(AuctionStatus.ACTIVE), any()))
                .thenReturn(Collections.singletonList(activeSession));
        when(auctionSessionRepository.findById(5)).thenReturn(Optional.of(endedWithoutWinner));

        auctionSchedulerService.scanAndUpdateAuctionStatus();

        verify(auctionService).endSession(5);
        verify(auctionSessionRepository).findById(5);
    }
}
