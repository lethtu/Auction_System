package com.auction.client.util;

import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.net.InetSocketAddress;
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
            // Bind to dynamic free port on localhost (IPv4 loopback)
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            int port = server.getAddress().getPort();
            String redirectUri = "http://127.0.0.1:" + port + "/callback";

            server.createContext("/callback", exchange -> {
                String query = exchange.getRequestURI().getQuery();
                String code = null;
                String error = null;

                if (query != null) {
                    for (String param : query.split("&")) {
                        String[] pair = param.split("=");
                        if (pair.length > 0) {
                            String key = pair[0];
                            String val = pair.length > 1 ? pair[1] : "";
                            if ("code".equals(key)) {
                                code = val;
                            } else if ("error".equals(key)) {
                                error = val;
                            }
                        }
                    }
                }

                String responseHtml;
                if (code != null) {
                    responseHtml = "<!DOCTYPE html>"
                            + "<html lang='en'>"
                            + "<head>"
                            + "<meta charset='UTF-8'>"
                            + "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                            + "<title>Login Successful</title>"
                            + "<style>"
                            + "  * { box-sizing: border-box; margin: 0; padding: 0; }"
                            + "  body {"
                            + "    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;"
                            + "    background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);"
                            + "    display: flex; justify-content: center; align-items: center; min-height: 100vh; color: #333;"
                            + "  }"
                            + "  .card {"
                            + "    background: rgba(255, 255, 255, 0.95);"
                            + "    backdrop-filter: blur(10px);"
                            + "    border-radius: 20px; padding: 40px; width: 90%; max-width: 440px;"
                            + "    box-shadow: 0 15px 35px rgba(0, 0, 0, 0.1);"
                            + "    text-align: center; border: 1px solid rgba(255, 255, 255, 0.5);"
                            + "    animation: slideUp 0.6s ease-out;"
                            + "  }"
                            + "  @keyframes slideUp {"
                            + "    from { opacity: 0; transform: translateY(20px); }"
                            + "    to { opacity: 1; transform: translateY(0); }"
                            + "  }"
                            + "  .checkmark-wrapper {"
                            + "    width: 80px; height: 80px; margin: 0 auto 24px;"
                            + "    background: #e8f5e9; border-radius: 50%;"
                            + "    display: flex; align-items: center; justify-content: center;"
                            + "    box-shadow: 0 0 20px rgba(76, 175, 80, 0.2);"
                            + "  }"
                            + "  .checkmark {"
                            + "    width: 40px; height: 40px; stroke: #4CAF50; stroke-width: 4;"
                            + "    stroke-linecap: round; stroke-linejoin: round; fill: none;"
                            + "    stroke-dasharray: 100; stroke-dashoffset: 100;"
                            + "    animation: draw 0.8s cubic-bezier(0.65, 0, 0.45, 1) 0.2s forwards;"
                            + "  }"
                            + "  @keyframes draw {"
                            + "    to { stroke-dashoffset: 0; }"
                            + "  }"
                            + "  h2 { font-size: 22px; color: #2e7d32; margin-bottom: 12px; font-weight: 700; }"
                            + "  p { font-size: 15px; color: #555; line-height: 1.6; margin-bottom: 24px; }"
                            + "  .countdown-container {"
                            + "    font-size: 14px; color: #666; background: #f9f9f9;"
                            + "    padding: 16px; border-radius: 12px; border: 1px solid #eaeaea;"
                            + "  }"
                            + "  .progress-bar-container {"
                            + "    width: 100%; height: 5px; background: #e0e0e0; border-radius: 3px;"
                            + "    overflow: hidden; margin-top: 12px;"
                            + "  }"
                            + "  .progress-bar {"
                            + "    width: 100%; height: 100%;"
                            + "    background: linear-gradient(90deg, #4CAF50, #8BC34A);"
                            + "    transition: width 5s linear;"
                            + "  }"
                            + "  .close-hint { font-size: 12px; color: #aaa; margin-top: 16px; }"
                            + "</style>"
                            + "</head>"
                            + "<body>"
                            + "  <div class='card'>"
                            + "    <div class='checkmark-wrapper'>"
                            + "      <svg class='checkmark' viewBox='0 0 52 52'>"
                            + "        <path d='M14.1 27.2l7.1 7.2 16.7-16.8'/>"
                            + "      </svg>"
                            + "    </div>"
                            + "    <h2>Login Successful!</h2>"
                            + "    <p>Google authentication complete.<br>Please return to the <strong>BidPop</strong> application.</p>"
                            + "    <div class='countdown-container'>"
                            + "      This window will close automatically in <strong id='countdown'>5</strong> seconds."
                            + "      <div class='progress-bar-container'>"
                            + "        <div id='progress' class='progress-bar'></div>"
                            + "      </div>"
                            + "    </div>"
                            + "    <div class='close-hint'>If the window does not close, you can close it manually.</div>"
                            + "  </div>"
                            + "  <script>"
                            + "    setTimeout(() => { document.getElementById('progress').style.width = '0%'; }, 50);"
                            + "    let seconds = 5;"
                            + "    const countdownEl = document.getElementById('countdown');"
                            + "    const timer = setInterval(() => {"
                            + "      seconds--;"
                            + "      countdownEl.textContent = seconds;"
                            + "      if (seconds <= 0) {"
                            + "        clearInterval(timer);"
                            + "        try {"
                            + "          window.open('', '_self', '');"
                            + "          window.close();"
                            + "        } catch(e) {"
                            + "          console.error(e);"
                            + "        }"
                            + "      }"
                            + "    }, 1000);"
                            + "  </script>"
                            + "</body>"
                            + "</html>";
                    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                    byte[] responseBytes = responseHtml.getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(200, responseBytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(responseBytes);
                    }

                    final String finalCode = code;
                    new Thread(() -> callback.onSuccess(finalCode, redirectUri)).start();
                } else {
                    String errorText = error != null ? error : "Unknown error";
                    responseHtml = "<!DOCTYPE html>"
                            + "<html lang='en'>"
                            + "<head>"
                            + "<meta charset='UTF-8'>"
                            + "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                            + "<title>Login Failed</title>"
                            + "<style>"
                            + "  * { box-sizing: border-box; margin: 0; padding: 0; }"
                            + "  body {"
                            + "    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;"
                            + "    background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);"
                            + "    display: flex; justify-content: center; align-items: center; min-height: 100vh; color: #333;"
                            + "  }"
                            + "  .card {"
                            + "    background: rgba(255, 255, 255, 0.95);"
                            + "    backdrop-filter: blur(10px);"
                            + "    border-radius: 20px; padding: 40px; width: 90%; max-width: 440px;"
                            + "    box-shadow: 0 15px 35px rgba(0, 0, 0, 0.1);"
                            + "    text-align: center; border: 1px solid rgba(255, 255, 255, 0.5);"
                            + "    animation: slideUp 0.6s ease-out;"
                            + "  }"
                            + "  @keyframes slideUp {"
                            + "    from { opacity: 0; transform: translateY(20px); }"
                            + "    to { opacity: 1; transform: translateY(0); }"
                            + "  }"
                            + "  .error-wrapper {"
                            + "    width: 80px; height: 80px; margin: 0 auto 24px;"
                            + "    background: #ffebee; border-radius: 50%;"
                            + "    display: flex; align-items: center; justify-content: center;"
                            + "    box-shadow: 0 0 20px rgba(244, 67, 54, 0.2);"
                            + "  }"
                            + "  .cross {"
                            + "    width: 40px; height: 40px; stroke: #f44336; stroke-width: 4;"
                            + "    stroke-linecap: round; stroke-linejoin: round; fill: none;"
                            + "    stroke-dasharray: 100; stroke-dashoffset: 100;"
                            + "    animation: draw 0.8s cubic-bezier(0.65, 0, 0.45, 1) 0.2s forwards;"
                            + "  }"
                            + "  @keyframes draw {"
                            + "    to { stroke-dashoffset: 0; }"
                            + "  }"
                            + "  h2 { font-size: 22px; color: #c62828; margin-bottom: 12px; font-weight: 700; }"
                            + "  p { font-size: 15px; color: #555; line-height: 1.6; margin-bottom: 24px; }"
                            + "  .close-hint { font-size: 12px; color: #aaa; margin-top: 16px; }"
                            + "</style>"
                            + "</head>"
                            + "<body>"
                            + "  <div class='card'>"
                            + "    <div class='error-wrapper'>"
                            + "      <svg class='cross' viewBox='0 0 52 52'>"
                            + "        <path d='M16 16 L36 36 M36 16 L16 36'/>"
                            + "      </svg>"
                            + "    </div>"
                            + "    <h2>Login Failed</h2>"
                            + "    <p>An error occurred during authentication with Google.<br>Details: <strong>" + errorText + "</strong></p>"
                            + "    <div class='close-hint'>You can close this window and try again.</div>"
                            + "  </div>"
                            + "</body>"
                            + "</html>";
                    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                    byte[] responseBytes = responseHtml.getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(400, responseBytes.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(responseBytes);
                    }

                    final String finalError = error != null ? error : "Unknown error";
                    new Thread(() -> callback.onFailure(finalError)).start();
                }

                // Stop the server in a separate thread so it completes the response stream cleanly
                new Thread(this::stopServer).start();
            });

            server.start();
            logger.info("Loopback server started on {}", redirectUri);

            // Construct Google Authorization URL
            String authUrl = "https://accounts.google.com/o/oauth2/v2/auth"
                    + "?client_id=" + clientId
                    + "&redirect_uri=" + redirectUri
                    + "&response_type=code"
                    + "&scope=openid%20email%20profile";

            // Open the user's default browser
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
// Force compile check
