package com.auction.client.service;

import com.auction.client.Config;
import com.auction.client.model.User;
import com.auction.client.model.notification.AppNotification;
import com.auction.client.model.notification.NotificationType;
import com.auction.client.model.notification.NotificationSeverity;
import com.auction.client.util.ShippingInfoDialog;
import com.auction.client.util.MoneyFormatUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NotificationSocketService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationSocketService.class);
    private static NotificationSocketService instance;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Thread listenerThread;
    private volatile boolean running = false;
    private Integer userId;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();
    private final ExecutorService notificationExecutor = Executors.newFixedThreadPool(
            Math.max(2, Math.min(4, Runtime.getRuntime().availableProcessors())),
            runnable -> {
                Thread thread = new Thread(runnable, "notification-worker");
                thread.setDaemon(true);
                return thread;
            });
    
    // Use WeakReference to prevent memory leaks from registered controllers
    private final List<java.lang.ref.WeakReference<SocketEventListener>> listeners = new CopyOnWriteArrayList<>();

    public interface SocketEventListener {
        void onEvent(JSONObject event);
        void onNotice(JSONObject notice);
    }

    private NotificationSocketService() {}

    public static synchronized NotificationSocketService getInstance() {
        if (instance == null) {
            instance = new NotificationSocketService();
        }
        return instance;
    }

    public synchronized void start(Integer userId) {
        if (running) {
            logger.info("NotificationSocketService is already running.");
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
        if (!running) return;
        running = false;
        disconnect();
        if (listenerThread != null) {
            listenerThread.interrupt();
            listenerThread = null;
        }
        logger.info("NotificationSocketService stopped.");
    }

    public void addListener(SocketEventListener listener) {
        if (listener == null) return;
        // Clean up GC'd listeners and prevent duplicates
        removeListener(listener);
        listeners.add(new java.lang.ref.WeakReference<>(listener));
    }

    public void removeListener(SocketEventListener listener) {
        if (listener == null) return;
        listeners.removeIf(ref -> {
            SocketEventListener l = ref.get();
            return l == null || l == listener;
        });
    }

    private void disconnect() {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (Exception e) {
            logger.warn("Error during socket disconnect: {}", e.getMessage());
        } finally {
            out = null;
            in = null;
            socket = null;
        }
    }

    private void listenSocketLoop() {
        int delay = 2000; // start with 2s reconnect delay
        while (running) {
            try {
                logger.info("Connecting to global socket server at {}:{}", Config.SOCKET_HOST, Config.PORT_SOCKET);
                socket = new Socket(Config.SOCKET_HOST, Config.PORT_SOCKET);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                if (User.getSessionToken() != null) {
                    JSONObject authJson = new JSONObject();
                    authJson.put("token", User.getSessionToken());
                    out.println("AUTH:" + authJson.toString());
                    out.flush();
                }

                // Register with user ID
                out.println("JOIN_HOME:" + userId);
                delay = 2000; // reset delay on success
                logger.info("Successfully connected and registered JOIN_HOME:{} on socket server.", userId);

                String line;
                while (running && (line = in.readLine()) != null) {
                    handleServerMessage(line);
                }
            } catch (Exception e) {
                if (running) {
                    logger.warn("Socket connection lost or failed. Reconnecting in {}ms... Error: {}", delay, e.getMessage());
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    // Simple backoff
                    delay = Math.min(delay * 2, 30000);
                }
            } finally {
                disconnect();
            }
        }
    }

    private void handleServerMessage(String line) {
        logger.info("Global socket received message: {}", line);
        try {
            if (line.startsWith("EVENT:")) {
                String jsonStr = line.substring(6);
                JSONObject eventObj = new JSONObject(jsonStr);
                
                // Propagate event to active weak listeners
                for (java.lang.ref.WeakReference<SocketEventListener> ref : listeners) {
                    SocketEventListener l = ref.get();
                    if (l != null) {
                        try {
                            l.onEvent(eventObj);
                        } catch (Exception ex) {
                            logger.error("Error in listener onEvent: {}", ex.getMessage());
                        }
                    } else {
                        listeners.remove(ref);
                    }
                }

                // Handle global events (e.g. AUCTION_ENDED)
                if ("AUCTION_ENDED".equals(eventObj.optString("type"))) {
                    int sessionId = eventObj.getInt("sessionId");
                    handleAuctionEndedEvent(sessionId, eventObj);
                }
            } else if (line.startsWith("NOTICE:")) {
                String jsonStr = line.substring(7);
                JSONObject noticeObj = new JSONObject(jsonStr);
                
                // Propagate notice to active weak listeners
                for (java.lang.ref.WeakReference<SocketEventListener> ref : listeners) {
                    SocketEventListener l = ref.get();
                    if (l != null) {
                        try {
                            l.onNotice(noticeObj);
                        } catch (Exception ex) {
                            logger.error("Error in listener onNotice: {}", ex.getMessage());
                        }
                    } else {
                        listeners.remove(ref);
                    }
                }

                processNoticeNotification(noticeObj);
            }
        } catch (Exception e) {
            logger.error("Error parsing global socket message: raw={}, error={}", line, e.getMessage());
        }
    }

    public void fetchLatestUserBalance() {
        if (userId == null) return;
        notificationExecutor.execute(() -> {
            try {
                HttpRequest.Builder builder = HttpRequest.newBuilder()
                        .uri(URI.create(Config.API_URL + "/api/users/" + userId))
                        .GET();
                if (User.getSessionToken() != null) {
                    builder.header("X-Auth-Token", User.getSessionToken());
                }
                HttpRequest request = builder.build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    JSONObject responseJson = new JSONObject(response.body());
                    if (responseJson.optInt("status", 500) == 200) {
                        JSONObject data = responseJson.optJSONObject("data");
                        if (data != null) {
                            BigDecimal balance = new BigDecimal(data.opt("balance").toString());
                            BigDecimal frozen = new BigDecimal(data.opt("frozenBalance").toString());
                            User.updateProfile(
                                    data.optString("username", User.getUsername()),
                                    data.optString("fullname", User.getFullname()),
                                    data.optString("email", User.getEmail()),
                                    data.optString("dob", User.getDob()),
                                    data.optString("placeOfBirth",
                                            data.optString("place_of_birth", User.getPlace_of_birth())),
                                    balance,
                                    frozen,
                                    data.optString("avatarUrl", data.optString("avatar_url", User.getAvatarUrl())));
                            
                            // Propagate UI update event
                            javafx.application.Platform.runLater(() -> {
                                try {
                                    JSONObject userUpdate = new JSONObject();
                                    userUpdate.put("type", "USER_PROFILE_UPDATED");
                                    userUpdate.put("userId", userId);
                                    userUpdate.put("balance", balance);
                                    userUpdate.put("frozenBalance", frozen);
                                    
                                    for (java.lang.ref.WeakReference<SocketEventListener> ref : listeners) {
                                        SocketEventListener l = ref.get();
                                        if (l != null) {
                                            try {
                                                l.onEvent(userUpdate);
                                            } catch (Exception ex) {
                                                logger.error("Error sending user update to listener: {}", ex.getMessage());
                                            }
                                        } else {
                                            listeners.remove(ref);
                                        }
                                    }
                                } catch (Exception e) {
                                    logger.error("Error creating/sending user update event: {}", e.getMessage());
                                }
                            });
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("Failed to fetch latest user balance: {}", e.getMessage());
            }
        });
    }

    private void processNoticeNotification(JSONObject noticeObj) {
        try {
            int auctionId = noticeObj.optInt("auctionId", -1);
            if (auctionId == -1) return;
            BigDecimal newPrice = noticeObj.getBigDecimal("newPrice");
            Integer highestBidderId = noticeObj.has("highestBidderId") && !noticeObj.isNull("highestBidderId") 
                ? noticeObj.getInt("highestBidderId") : null;
            Integer previousHighestBidderId = noticeObj.has("previousHighestBidderId") && !noticeObj.isNull("previousHighestBidderId")
                ? noticeObj.getInt("previousHighestBidderId") : null;
            String itemName = noticeObj.optString("itemName", "Unknown Item");

            Integer currentUserId = User.getId();
            if (currentUserId == null) return;
            logger.info("processNoticeNotification: auctionId={}, currentUserId={}, highestBidderId={}, previousHighestBidderId={}, newPrice={}",
                    auctionId, currentUserId, highestBidderId, previousHighestBidderId, newPrice);

            // 1. Target outbid user: previousHighestBidderId == currentUserId && highestBidderId != currentUserId
            if (previousHighestBidderId != null && previousHighestBidderId.equals(currentUserId) && !currentUserId.equals(highestBidderId)) {
                AppNotification notif = new AppNotification(
                    NotificationType.OUTBID, 
                    NotificationSeverity.WARNING, 
                    "You have been outbid", 
                    "Product " + itemName + " is now at ₫ " + formatPrice(newPrice)
                );
                notif.setAuctionId(auctionId);
                notif.setItemName(itemName);
                NotificationCenterService.getInstance().addNotification(notif);
                fetchLatestUserBalance();
            }
        } catch (Exception e) {
            logger.error("Error processing notice notification: {}", e.getMessage(), e);
        }
    }

    private void fetchAndNotifyAuctionEnd(int sessionId) {
        notificationExecutor.execute(() -> {
            try {
                // Fetch auction session details
                HttpRequest sessionReq = HttpRequest.newBuilder()
                        .uri(URI.create(Config.API_URL + "/api/auctions/" + sessionId))
                        .GET()
                        .build();
                HttpResponse<String> sessionRes = httpClient.send(sessionReq, HttpResponse.BodyHandlers.ofString());
                if (sessionRes.statusCode() != 200 || sessionRes.body() == null || sessionRes.body().isBlank()) {
                    return;
                }
                
                JSONObject session = new JSONObject(sessionRes.body());
                Integer highestBidderId = session.has("highestBidderId") && !session.isNull("highestBidderId") 
                    ? session.getInt("highestBidderId") : null;
                BigDecimal currentPrice = new BigDecimal(session.opt("currentPrice").toString());
                
                JSONObject item = session.optJSONObject("item");
                String itemName = item != null ? item.optString("name", "Unknown Item") : session.optString("productName", "Unknown Item");

                // Fetch bid history to check if user participated
                HttpRequest historyReq = HttpRequest.newBuilder()
                        .uri(URI.create(Config.API_URL + "/api/auctions/" + sessionId + "/bid-history"))
                        .GET()
                        .build();
                HttpResponse<String> historyRes = httpClient.send(historyReq, HttpResponse.BodyHandlers.ofString());
                
                boolean participated = false;
                Integer currentUserId = User.getId();
                if (currentUserId != null && historyRes.statusCode() == 200 && historyRes.body() != null) {
                    JSONArray historyArr = new JSONArray(historyRes.body());
                    for (int i = 0; i < historyArr.length(); i++) {
                        JSONObject bid = historyArr.getJSONObject(i);
                        if (bid.optInt("bidderId", -1) == currentUserId) {
                            participated = true;
                            break;
                        }
                    }
                }

                if (participated && currentUserId != null) {
                    if (currentUserId.equals(highestBidderId)) {
                        AppNotification notif = new AppNotification(
                            NotificationType.AUCTION_WON, 
                            NotificationSeverity.SUCCESS, 
                            "You won!", 
                            "You are the highest bidder for " + itemName + " at ₫ " + formatPrice(currentPrice)
                        );
                        notif.setAuctionId(sessionId);
                        notif.setItemName(itemName);
                        notif.setActionLabel("Enter delivery info");
                        notif.setAction(() -> ShippingInfoDialog.show(sessionId, itemName));
                        NotificationCenterService.getInstance().addNotification(notif);
                    } else {
                        AppNotification notif = new AppNotification(
                            NotificationType.AUCTION_LOST, 
                            NotificationSeverity.INFO, 
                            "Auction ended", 
                            "Unfortunately, you did not win the auction for " + itemName
                        );
                        notif.setAuctionId(sessionId);
                        notif.setItemName(itemName);
                        NotificationCenterService.getInstance().addNotification(notif);
                    }
                    fetchLatestUserBalance();
                }
            } catch (Exception e) {
                logger.warn("Failed to fetch/notify auction end details for session {}: {}", sessionId, e.getMessage());
            }
        });
    }

    private void handleAuctionEndedEvent(int sessionId, JSONObject eventObj) {
        Integer currentUserId = User.getId();
        if (currentUserId == null) {
            return;
        }

        Integer winnerId = eventObj.has("winnerId") && !eventObj.isNull("winnerId")
                ? eventObj.optInt("winnerId")
                : (eventObj.has("highestBidderId") && !eventObj.isNull("highestBidderId")
                    ? eventObj.optInt("highestBidderId")
                    : null);

        if (winnerId != null) {
            if (!currentUserId.equals(winnerId)) {
                // If it's not us, we still might have participated and lost, so check and update
                fetchAndNotifyAuctionEnd(sessionId);
                return;
            }

            BigDecimal finalPrice = parsePrice(eventObj.opt("finalPrice"));
            String itemName = eventObj.optString("itemName", "Unknown Item");
            AppNotification notif = new AppNotification(
                    NotificationType.AUCTION_WON,
                    NotificationSeverity.SUCCESS,
                    "You won!",
                    "Please enter delivery information for " + itemName + " at VND " + formatPrice(finalPrice));
            notif.setAuctionId(sessionId);
            notif.setItemName(itemName);
            notif.setActionLabel("Enter delivery info");
            notif.setAction(() -> ShippingInfoDialog.show(sessionId, itemName));
            NotificationCenterService.getInstance().addNotification(notif);
            fetchLatestUserBalance();
            return;
        }

        if (eventObj.has("hasWinner") && !eventObj.optBoolean("hasWinner")) {
            return;
        }

        fetchAndNotifyAuctionEnd(sessionId);
    }

    private BigDecimal parsePrice(Object value) {
        if (value == null || JSONObject.NULL.equals(value)) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value.toString());
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private String formatPrice(BigDecimal price) {
        return MoneyFormatUtil.formatGrouped(price);
    }
}
