package com.auction.client.util;

import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class GoogleOAuthService {
    private static final Logger logger = LoggerFactory.getLogger(GoogleOAuthService.class);
    private HttpServer server;

    public interface AuthorizationCallback {
        void onSuccess(String code, String redirectUri);
        void onFailure(String error);
    }

    public void startAuthorizationFlow(String clientId, AuthorizationCallback callback) {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            int port = server.getAddress().getPort();
            String redirectUri = "http://127.0.0.1:" + port + "/callback";

            server.createContext("/callback", exchange -> {
                String query = exchange.getRequestURI().getQuery();
                String code = getQueryValue(query, "code");
                String error = getQueryValue(query, "error");

                if (code != null && !code.isBlank()) {
                    writeHtml(exchange, 200, successPageHtml());
                    final String finalCode = code;
                    new Thread(() -> callback.onSuccess(finalCode, redirectUri)).start();
                } else {
                    String errorText = error == null || error.isBlank() ? "Unknown error" : error;
                    writeHtml(exchange, 400, failurePageHtml(errorText));
                    new Thread(() -> callback.onFailure(errorText)).start();
                }

                new Thread(this::stopServer).start();
            });

            server.start();
            logger.info("Loopback server started on {}", redirectUri);

            String authUrl = "https://accounts.google.com/o/oauth2/v2/auth"
                    + "?client_id=" + encode(clientId)
                    + "&redirect_uri=" + encode(redirectUri)
                    + "&response_type=code"
                    + "&scope=openid%20email%20profile";

            openBrowser(authUrl);
        } catch (Exception e) {
            logger.error("Failed to start authorization flow", e);
            callback.onFailure(e.getMessage());
            stopServer();
        }
    }

    public synchronized void stopServer() {
        if (server != null) {
            try {
                server.stop(0);
                logger.info("Loopback server stopped.");
            } catch (Exception e) {
                logger.error("Error stopping loopback server", e);
            }
            server = null;
        }
    }

    private static String getQueryValue(String query, String key) {
        if (query == null || query.isBlank()) {
            return null;
        }
        for (String param : query.split("&")) {
            String[] pair = param.split("=", 2);
            if (pair.length > 0 && key.equals(pair[0])) {
                return pair.length > 1 ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : "";
            }
        }
        return null;
    }

    private static void writeHtml(com.sun.net.httpserver.HttpExchange exchange, int status, String html) throws java.io.IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        byte[] responseBytes = html.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    private static String encode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String successPageHtml() {
        return basePage("Google login completed!",
                "Google account verification is complete.<br>You can now return to <strong>BidPop</strong>.",
                "success");
    }

    private static String failurePageHtml(String errorText) {
        String safeError = escapeHtml(errorText);
        return basePage("Google login failed",
                "Google account verification could not be completed.<br>Details: <strong>" + safeError + "</strong>",
                "error");
    }

    private static String basePage(String title, String message, String state) {
        boolean success = "success".equals(state);
        String accent = success ? "#1f8a3b" : "#c62828";
        String icon = success ? "&#10003;" : "&#10005;";
        String bg = success ? "#eef6ff" : "#fff1f1";

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>BidPop Google Login</title>
                    <style>
                        * { box-sizing: border-box; margin: 0; padding: 0; }
                        body {
                            min-height: 100vh;
                            display: flex;
                            justify-content: center;
                            align-items: center;
                            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Arial, sans-serif;
                            background: linear-gradient(135deg, #eef6ff 0%, #dbe8f8 100%);
                            color: #1d2433;
                        }
                        .card {
                            width: min(88vw, 760px);
                            padding: 64px 72px;
                            background: rgba(255, 255, 255, 0.96);
                            border-radius: 24px;
                            box-shadow: 0 28px 70px rgba(53, 80, 120, 0.18);
                            text-align: center;
                        }
                        .icon {
                            width: 120px;
                            height: 120px;
                            margin: 0 auto 36px;
                            border-radius: 999px;
                            display: flex;
                            align-items: center;
                            justify-content: center;
                            font-size: 52px;
                            font-weight: 800;
                            background: #f1fff5;
                            color: %ACCENT%;
                            box-shadow: 0 0 36px rgba(31, 138, 59, 0.16);
                        }
                        h1 {
                            color: %ACCENT%;
                            font-size: 38px;
                            line-height: 1.2;
                            margin-bottom: 24px;
                            font-weight: 800;
                        }
                        p {
                            color: #59657a;
                            font-size: 22px;
                            line-height: 1.55;
                            margin-bottom: 34px;
                        }
                        .action-box {
                            border: 1px solid #e4e9f2;
                            border-radius: 16px;
                            padding: 24px;
                            background: #fbfdff;
                        }
                        .return-button {
                            border: none;
                            border-radius: 14px;
                            padding: 15px 34px;
                            background: %ACCENT%;
                            color: white;
                            font-size: 16px;
                            font-weight: 800;
                            cursor: pointer;
                            box-shadow: 0 14px 30px rgba(31, 138, 59, 0.22);
                        }
                        .return-button:hover { filter: brightness(0.96); }
                        .hint {
                            margin-top: 22px;
                            color: #98a4b7;
                            font-size: 18px;
                        }
                    </style>
                </head>
                <body>
                    <main class="card">
                        <div class="icon">%ICON%</div>
                        <h1>%TITLE%</h1>
                        <p>%MESSAGE%</p>
                        <div class="action-box">
                            <button class="return-button" onclick="closeThisTab()">Close this tab</button>
                            <div id="manualHint" class="hint">If the button does not close this tab, please close it manually and return to BidPop.</div>
                        </div>
                    </main>
                    <script>
                        function closeThisTab() {
                            try {
                                window.open('', '_self', '');
                                window.close();
                            } catch (e) {
                                console.error(e);
                            }
                            document.getElementById('manualHint').style.display = 'block';
                        }
                    </script>
                </body>
                </html>
                """
                .replace("%ACCENT%", accent)
                .replace("%ICON%", icon)
                .replace("%TITLE%", title)
                .replace("%MESSAGE%", message)
                .replace("#eef6ff", bg);
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static void openBrowser(String url) {
        try {
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                if (desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
                    desktop.browse(new java.net.URI(url));
                    return;
                }
            }
        } catch (Exception e) {
            logger.warn("java.awt.Desktop browse failed, trying process fallback: {}", e.getMessage());
        }

        String os = System.getProperty("os.name").toLowerCase();
        try {
            if (os.contains("win")) {
                new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start();
            } else if (os.contains("mac")) {
                new ProcessBuilder("open", url).start();
            } else {
                new ProcessBuilder("xdg-open", url).start();
            }
        } catch (Exception e) {
            logger.error("Failed to open browser fallback: {}", e.getMessage());
        }
    }
}
