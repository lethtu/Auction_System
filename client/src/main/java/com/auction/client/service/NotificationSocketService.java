package com.auction.client.service;

import com.auction.client.Config;
import com.auction.client.model.User;
import com.auction.client.model.notification.AppNotification;
import com.auction.client.model.notification.NotificationSeverity;
import com.auction.client.model.notification.NotificationType;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class NotificationSocketService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationSocketService.class);
    private static NotificationSocketService instance;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Thread listenerThread;
    private volatile boolean running = false;
    private Integer userId;

    private final List<WeakReference<SocketEventListener>> listeners = new CopyOnWriteArrayList<>();

    public interface SocketEventListener {
        void onEvent(JSONObject event);
        void onNotice(JSONObject notice);
    }

    private NotificationSocketService() {
    }

    public static synchronized NotificationSocketService getInstance() {
        if (instance == null) {
            instance = new NotificationSocketService();
        }
        return instance;
    }

    public synchronized void start(Integer userId) {
        if (userId == null) {
            logger.warn("NotificationSocketService start skipped because userId is null.");
            return;
        }
        if (running) {
            this.userId = userId;
            logger.info("NotificationSocketService already running. Updated userId={}", userId);
            return;
        }
        this.userId = userId;
        this.running = true;
        listenerThread = new Thread(this::listenSocketLoop, "NotificationSocketServiceThread");
        listenerThread.setDaemon(true);
        listenerThread.start();
        logger.info("NotificationSocketService started for user {}", userId);
    }

    public synchronized void stop() {
        if (!running) {
            return;
        }
        running = false;
        disconnect();
        if (listenerThread != null) {
            listenerThread.interrupt();
            listenerThread = null;
        }
        logger.info("NotificationSocketService stopped.");
    }

    public void addListener(SocketEventListener listener) {
        if (listener == null) {
            return;
        }
        removeListener(listener);
        listeners.add(new WeakReference<>(listener));
    }

    public void removeListener(SocketEventListener listener) {
        if (listener == null) {
            return;
        }
        listeners.removeIf(ref -> {
            SocketEventListener current = ref.get();
            return current == null || current == listener;
        });
    }

    private void listenSocketLoop() {
        int delayMs = 2000;
        while (running) {
            try {
                logger.info("Connecting notification socket to {}:{}", Config.SOCKET_HOST, Config.PORT_SOCKET);
                socket = new Socket(Config.SOCKET_HOST, Config.PORT_SOCKET);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // Backward-compatible registration:
                // old server accepts JOIN_HOME; newer server can also map JOIN_HOME:<userId>.
                out.println("JOIN_HOME");
                out.println("JOIN_HOME:" + userId);

                delayMs = 2000;
                logger.info("Notification socket connected for user {}", userId);

                String line;
                while (running && (line = in.readLine()) != null) {
                    handleServerMessage(line);
                }
            } catch (Exception e) {
                if (running) {
                    logger.warn("Notification socket disconnected. Reconnect in {}ms. Error: {}", delayMs, e.getMessage());
                    sleepBeforeReconnect(delayMs);
                    delayMs = Math.min(delayMs * 2, 30000);
                }
            } finally {
                disconnect();
            }
        }
    }

    private void sleepBeforeReconnect(int delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void handleServerMessage(String line) {
        if (line == null || line.isBlank()) {
            return;
        }
        logger.info("Notification socket received: {}", line);
        try {
            if (line.startsWith("EVENT:")) {
                JSONObject event = new JSONObject(line.substring("EVENT:".length()));
                notifyEventListeners(event);
                if ("AUCTION_ENDED".equalsIgnoreCase(event.optString("type"))) {
                    int sessionId = event.optInt("sessionId", event.optInt("auctionId", -1));
                    if (sessionId > 0) {
                        fetchAndNotifyAuctionEnd(sessionId);
                    }
                }
            } else if (line.startsWith("NOTICE:")) {
                JSONObject notice = new JSONObject(line.substring("NOTICE:".length()));
                notifyNoticeListeners(notice);
                processBidNotice(notice);
            }
        } catch (Exception e) {
            logger.warn("Could not parse notification socket message. raw={}, error={}", line, e.getMessage());
        }
    }

    private void notifyEventListeners(JSONObject event) {
        listeners.removeIf(ref -> ref.get() == null);
        for (WeakReference<SocketEventListener> ref : listeners) {
            SocketEventListener listener = ref.get();
            if (listener != null) {
                try {
                    listener.onEvent(event);
                } catch (Exception e) {
                    logger.warn("Socket event listener failed: {}", e.getMessage());
                }
            }
        }
    }

    private void notifyNoticeListeners(JSONObject notice) {
        listeners.removeIf(ref -> ref.get() == null);
        for (WeakReference<SocketEventListener> ref : listeners) {
            SocketEventListener listener = ref.get();
            if (listener != null) {
                try {
                    listener.onNotice(notice);
                } catch (Exception e) {
                    logger.warn("Socket notice listener failed: {}", e.getMessage());
                }
            }
        }
    }

    private void processBidNotice(JSONObject notice) {
        try {
            Integer currentUserId = User.getId();
            if (currentUserId == null) {
                return;
            }

            Integer highestBidderId = readNullableInt(notice, "highestBidderId");
            Integer previousHighestBidderId = readNullableInt(notice, "previousHighestBidderId");
            if (previousHighestBidderId == null || previousHighestBidderId.equals(highestBidderId)) {
                return;
            }
            if (!previousHighestBidderId.equals(currentUserId)) {
                return;
            }
            if (highestBidderId != null && highestBidderId.equals(currentUserId)) {
                return;
            }

            int auctionId = notice.optInt("auctionId", notice.optInt("sessionId", -1));
            String itemName = notice.optString("itemName", notice.optString("productName", "this auction"));
            BigDecimal newPrice = readBigDecimal(notice, "newPrice", readBigDecimal(notice, "currentPrice", BigDecimal.ZERO));

            AppNotification notification = new AppNotification(
                    NotificationType.OUTBID,
                    NotificationSeverity.WARNING,
                    "You have been outbid",
                    "Product " + itemName + " is now at ₫ " + formatPrice(newPrice)
            );
            if (auctionId > 0) {
                notification.setAuctionId(auctionId);
            }
            notification.setItemName(itemName);
            NotificationCenterService.getInstance().addNotification(notification);
        } catch (Exception e) {
            logger.warn("Could not process bid notice notification: {}", e.getMessage());
        }
    }

    private void fetchAndNotifyAuctionEnd(int sessionId) {
        Thread worker = new Thread(() -> {
            try {
                JSONObject session = fetchSession(sessionId);
                if (session == null) {
                    return;
                }

                Integer currentUserId = User.getId();
                if (currentUserId == null) {
                    return;
                }

                Integer highestBidderId = readNullableInt(session, "highestBidderId");
                BigDecimal currentPrice = readBigDecimal(session, "currentPrice", BigDecimal.ZERO);
                String itemName = resolveItemName(session);

                if (!hasUserParticipated(sessionId, currentUserId)) {
                    return;
                }

                boolean won = highestBidderId != null && highestBidderId.equals(currentUserId);
                AppNotification notification = new AppNotification(
                        won ? NotificationType.AUCTION_END_WIN : NotificationType.AUCTION_END_LOSE,
                        won ? NotificationSeverity.SUCCESS : NotificationSeverity.INFO,
                        won ? "You won!" : "Auction ended",
                        won
                                ? "You are the highest bidder for " + itemName + " at ₫ " + formatPrice(currentPrice)
                                : "Unfortunately, you did not win the auction for " + itemName
                );
                notification.setAuctionId(sessionId);
                notification.setItemName(itemName);
                NotificationCenterService.getInstance().addNotification(notification);
            } catch (Exception e) {
                logger.warn("Could not create auction-end notification for session {}: {}", sessionId, e.getMessage());
            }
        }, "NotificationAuctionEndFetchThread");
        worker.setDaemon(true);
        worker.start();
    }

    private JSONObject fetchSession(int sessionId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(Config.API_URL + "/api/auctions/" + sessionId))
                .GET()
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200 || response.body() == null || response.body().isBlank()) {
            return null;
        }
        JSONObject body = new JSONObject(response.body());
        return unwrapObjectData(body);
    }

    private boolean hasUserParticipated(int sessionId, int userId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(Config.API_URL + "/api/auctions/" + sessionId + "/bid-history"))
                .GET()
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200 || response.body() == null || response.body().isBlank()) {
            return false;
        }

        JSONArray history = unwrapArrayData(response.body());
        for (int i = 0; i < history.length(); i++) {
            JSONObject bid = history.optJSONObject(i);
            if (bid != null && bid.optInt("bidderId", -1) == userId) {
                return true;
            }
        }
        return false;
    }

    private JSONObject unwrapObjectData(JSONObject body) {
        JSONObject data = body.optJSONObject("data");
        return data != null ? data : body;
    }

    private JSONArray unwrapArrayData(String rawBody) {
        String trimmed = rawBody == null ? "" : rawBody.trim();
        if (trimmed.startsWith("[")) {
            return new JSONArray(trimmed);
        }
        JSONObject body = new JSONObject(trimmed);
        JSONArray dataArray = body.optJSONArray("data");
        if (dataArray != null) {
            return dataArray;
        }
        JSONObject dataObj = body.optJSONObject("data");
        if (dataObj != null) {
            JSONArray content = dataObj.optJSONArray("content");
            if (content != null) {
                return content;
            }
        }
        return new JSONArray();
    }

    private String resolveItemName(JSONObject session) {
        JSONObject item = session.optJSONObject("item");
        if (item != null) {
            String itemName = item.optString("name", "");
            if (!itemName.isBlank()) {
                return itemName;
            }
        }
        String productName = session.optString("productName", "");
        return productName.isBlank() ? "this auction" : productName;
    }

    private Integer readNullableInt(JSONObject object, String key) {
        if (object == null || !object.has(key) || object.isNull(key)) {
            return null;
        }
        try {
            return object.getInt(key);
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal readBigDecimal(JSONObject object, String key, BigDecimal fallback) {
        if (object == null || !object.has(key) || object.isNull(key)) {
            return fallback;
        }
        try {
            Object value = object.get(key);
            return new BigDecimal(String.valueOf(value));
        } catch (Exception e) {
            return fallback;
        }
    }

    private String formatPrice(BigDecimal price) {
        if (price == null) {
            return "0";
        }
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        DecimalFormat decimalFormat = new DecimalFormat("###,###", symbols);
        return decimalFormat.format(price);
    }

    private void disconnect() {
        try {
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (Exception e) {
            logger.warn("Error during notification socket disconnect: {}", e.getMessage());
        } finally {
            out = null;
            in = null;
            socket = null;
        }
    }
}
