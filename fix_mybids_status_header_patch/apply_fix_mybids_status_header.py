from pathlib import Path
import re
import shutil
from datetime import datetime

ROOT = Path.cwd()
backup = ROOT / f"backup_fix_mybids_status_header_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
backup.mkdir(parents=True, exist_ok=True)

controller = ROOT / "client/src/main/java/com/auction/client/controller/MyBidsController.java"
fxml = ROOT / "client/src/main/java/com/auction/client/view/MyBids.fxml"

for p in [controller, fxml]:
    if not p.exists():
        raise SystemExit(f"Không tìm thấy file: {p}")
    dest = backup / p.relative_to(ROOT)
    dest.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(p, dest)

text = controller.read_text(encoding="utf-8")
orig = text

# 1) Add helper methods for stable status handling. These only affect My Bids rendering/filtering.
helpers = r'''
    private String normalizeSessionStatus(JSONObject sessionObj) {
        if (sessionObj == null) {
            return "ACTIVE";
        }
        String status = sessionObj.optString("status", "ACTIVE");
        if (status == null || status.isBlank()) {
            return "ACTIVE";
        }
        return status.trim().toUpperCase();
    }

    private boolean isActiveSession(JSONObject sessionObj) {
        return "ACTIVE".equals(normalizeSessionStatus(sessionObj));
    }

    private boolean isEndedSession(JSONObject sessionObj) {
        String status = normalizeSessionStatus(sessionObj);
        if ("ACTIVE".equals(status)
                || "COMING".equals(status)
                || "UPCOMING".equals(status)
                || "PENDING".equals(status)
                || "DRAFT".equals(status)
                || "APPROVED".equals(status)
                || "WAITING".equals(status)) {
            return false;
        }
        return "ENDED".equals(status)
                || "CLOSED".equals(status)
                || "COMPLETED".equals(status)
                || "FINISHED".equals(status)
                || "EXPIRED".equals(status)
                || "CANCELED".equals(status)
                || "CANCELLED".equals(status)
                || !status.isBlank();
    }

    private boolean isWinningSession(JSONObject sessionObj, int currentUserId) {
        return isActiveSession(sessionObj) && sessionObj.optInt("highestBidderId", -1) == currentUserId;
    }

    private boolean isOutbidSession(JSONObject sessionObj, int currentUserId) {
        return isActiveSession(sessionObj) && sessionObj.optInt("highestBidderId", -1) != currentUserId;
    }

    private String getRenderedStateKey(JSONObject sessionObj, BigDecimal currentPrice, int highestBidderId) {
        return sessionObj.optInt("id") + "_" + currentPrice + "_" + highestBidderId + "_" + normalizeSessionStatus(sessionObj);
    }

'''
if "normalizeSessionStatus(JSONObject sessionObj)" not in text:
    marker = "    private VBox createProductCard(JSONObject sessionObj, JSONObject itemObj) {"
    if marker not in text:
        raise SystemExit("Không tìm thấy createProductCard để chèn helper methods")
    text = text.replace(marker, helpers + marker, 1)

# 2) Replace tab matching switches in filterAndRenderProducts with helper-based rules.
switch_pattern = re.compile(r'''\s*boolean matchTab = false;\s*\n\s*switch \(currentTab\) \{\s*\n\s*case ACTIVE:\s*\n\s*matchTab = "ACTIVE"\.equalsIgnoreCase\(status\);\s*\n\s*break;\s*\n\s*case WINNING:\s*\n\s*matchTab = "ACTIVE"\.equalsIgnoreCase\(status\) && \(highestBidderId == currentUserId\);\s*\n\s*break;\s*\n\s*case OUTBID:\s*\n\s*matchTab = "ACTIVE"\.equalsIgnoreCase\(status\) && \(highestBidderId != currentUserId\);\s*\n\s*break;\s*\n\s*case ENDED:\s*\n\s*matchTab = "ENDED"\.equalsIgnoreCase\(status\);\s*\n\s*break;\s*\n\s*}\s*''', re.MULTILINE)
new_switch = '''
                boolean matchTab = false;
                switch (currentTab) {
                    case ACTIVE:
                        matchTab = isActiveSession(sessionObj);
                        break;
                    case WINNING:
                        matchTab = isWinningSession(sessionObj, currentUserId);
                        break;
                    case OUTBID:
                        matchTab = isOutbidSession(sessionObj, currentUserId);
                        break;
                    case ENDED:
                        matchTab = isEndedSession(sessionObj);
                        break;
                }
'''
text, switch_count = switch_pattern.subn(new_switch, text)
if switch_count == 0:
    print("WARN: Không tìm thấy switch tab cũ để thay. Có thể file đã được sửa trước đó.")
else:
    print(f"Patched tab filter switches: {switch_count}")

# 3) Use normalized status and stable state key.
text = re.sub(r'String status = sessionObj\.optString\("status", "ACTIVE"\);',
              'String status = normalizeSessionStatus(sessionObj);', text)
# Keep the helper implementation itself non-recursive after the broad replacement above.
text = text.replace('String status = normalizeSessionStatus(sessionObj);\n        if (status == null || status.isBlank()) {',
                    'String status = sessionObj.optString("status", "ACTIVE");\n        if (status == null || status.isBlank()) {', 1)
text = re.sub(r'sessionObj\.optInt\("id"\) \+ "_" \+ currentPrice \+ "_" \+ highestBidderId \+ "_" \+ status',
              'getRenderedStateKey(sessionObj, currentPrice, highestBidderId)', text)

# 4) Add readable state booleans in createProductCard.
anchor = '''        int highestBidderId = sessionObj.optInt("highestBidderId", -1);
        int currentUserId = User.getId() != null ? User.getId() : -1;
'''
replacement = '''        int highestBidderId = sessionObj.optInt("highestBidderId", -1);
        int currentUserId = User.getId() != null ? User.getId() : -1;
        boolean activeSession = isActiveSession(sessionObj);
        boolean endedSession = isEndedSession(sessionObj);
        boolean winningSession = isWinningSession(sessionObj, currentUserId);
        boolean outbidSession = isOutbidSession(sessionObj, currentUserId);
'''
if anchor in text and "boolean activeSession = isActiveSession(sessionObj);" not in text:
    text = text.replace(anchor, replacement, 1)

# 5) Replace status badge block to force badges to be consistent across all My Bids cards.
status_start = text.find("        // Status Badge (Top-Left)")
status_end = text.find("        if (\"ACTIVE\".equalsIgnoreCase(status)) {", status_start)
if status_start != -1 and status_end != -1:
    new_status_block = r'''        // Status Badge (Top-Left)
        HBox statusBadge = new HBox(4.0);
        statusBadge.setAlignment(Pos.CENTER);
        statusBadge.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        StackPane.setAlignment(statusBadge, Pos.TOP_LEFT);
        StackPane.setMargin(statusBadge, new Insets(10, 0, 0, 10));

        Region dot = new Region();
        dot.setPrefSize(8, 8);
        dot.setMinSize(8, 8);
        dot.setMaxSize(8, 8);

        Label badgeLabel = new Label();
        badgeLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");

        if (endedSession) {
            if (highestBidderId == currentUserId) {
                statusBadge.setStyle("-fx-background-color: rgba(16, 185, 129, 0.15); -fx-background-radius: 12px; -fx-padding: 4px 10px; -fx-border-color: rgba(16, 185, 129, 0.3); -fx-border-radius: 12px;");
                badgeLabel.setText("Won");
                badgeLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #10b981;");
                dot.setStyle("-fx-background-color: #10b981; -fx-background-radius: 4px;");
                statusBadge.getChildren().setAll(dot, badgeLabel);
            } else {
                statusBadge.setStyle("-fx-background-color: rgba(108, 117, 125, 0.15); -fx-background-radius: 12px; -fx-padding: 4px 10px; -fx-border-color: rgba(108, 117, 125, 0.3); -fx-border-radius: 12px;");
                badgeLabel.setText("Ended");
                badgeLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #6c757d;");
                statusBadge.getChildren().setAll(badgeLabel);
            }
        } else if (winningSession) {
            statusBadge.setStyle("-fx-background-color: rgba(16, 185, 129, 0.15); -fx-background-radius: 12px; -fx-padding: 4px 10px; -fx-border-color: rgba(16, 185, 129, 0.3); -fx-border-radius: 12px;");
            badgeLabel.setText("Winning");
            badgeLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #10b981;");
            dot.setStyle("-fx-background-color: #10b981; -fx-background-radius: 4px;");
            statusBadge.getChildren().setAll(dot, badgeLabel);
        } else if (outbidSession) {
            statusBadge.setStyle("-fx-background-color: rgba(239, 68, 68, 0.15); -fx-background-radius: 12px; -fx-padding: 4px 10px; -fx-border-color: rgba(239, 68, 68, 0.3); -fx-border-radius: 12px;");
            badgeLabel.setText("Outbid");
            badgeLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #ef4444;");
            Label warningIcon = new Label("\uE002");
            warningIcon.setStyle("-fx-font-family: 'Material Symbols Outlined'; -fx-font-size: 14px; -fx-text-fill: #ef4444;");
            statusBadge.getChildren().setAll(warningIcon, badgeLabel);
        } else {
            statusBadge.setStyle("-fx-background-color: rgba(224, 64, 160, 0.12); -fx-background-radius: 12px; -fx-padding: 4px 10px; -fx-border-color: rgba(224, 64, 160, 0.25); -fx-border-radius: 12px;");
            badgeLabel.setText(status);
            badgeLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #e040a0;");
            statusBadge.getChildren().setAll(badgeLabel);
        }
        imageWrapper.getChildren().remove(statusBadge);
        imageWrapper.getChildren().add(statusBadge);

'''
    text = text[:status_start] + new_status_block + text[status_end:]
else:
    print("WARN: Không tìm thấy status badge block để thay.")

# 6) Replace direct ACTIVE checks inside card with helper boolean.
text = text.replace('if ("ACTIVE".equalsIgnoreCase(status)) {', 'if (activeSession) {')
text = text.replace('if ("ACTIVE".equalsIgnoreCase(status) && highestBidderId != currentUserId) {', 'if (outbidSession) {')
# If the above order missed the action button check due previous replacement, fix remaining condition.
text = text.replace('if (activeSession && highestBidderId != currentUserId) {', 'if (outbidSession) {')
text = text.replace('if ("ACTIVE".equalsIgnoreCase(status) && highestBidderId != currentUserId)', 'if (outbidSession)')
text = text.replace('if (activeSession && highestBidderId != currentUserId)', 'if (outbidSession)')

# 7) Make bid detail text consistent so Outbid/Ended are not misleading.
text = text.replace('Label lblYourBid = new Label(highestBidderId != currentUserId ? "YOUR MAX BID" : "YOUR BID");',
'''Label lblYourBid = new Label(outbidSession ? "YOUR MAX BID" : (endedSession ? "FINAL BID" : "YOUR BID"));''')
old_user_price = '''        if (highestBidderId == currentUserId) {
            userPriceLabel.setText("₫ " + formatPrice(currentPrice));
            userPriceLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #10b981;");
        } else {
            userPriceLabel.setText("Outbid");
            userPriceLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #ef4444;");
        }'''
new_user_price = '''        if (winningSession || (endedSession && highestBidderId == currentUserId)) {
            userPriceLabel.setText("₫ " + formatPrice(currentPrice));
            userPriceLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #10b981;");
        } else if (outbidSession) {
            userPriceLabel.setText("Outbid");
            userPriceLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #ef4444;");
        } else {
            userPriceLabel.setText("Ended");
            userPriceLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #6c757d;");
        }'''
if old_user_price in text:
    text = text.replace(old_user_price, new_user_price, 1)

if text == orig:
    print("WARN: MyBidsController.java không thay đổi. Có thể format file khác dự kiến.")
else:
    controller.write_text(text, encoding="utf-8")
    print("Patched MyBidsController.java")

# 8) Header icon alignment in MyBids.fxml: keep layout/function, only normalize top right buttons.
f = fxml.read_text(encoding="utf-8")
for old, new in [
    ('<HBox spacing="15.0" alignment="CENTER_RIGHT">', '<HBox spacing="12.0" alignment="CENTER" minHeight="52.0" prefHeight="52.0" maxHeight="52.0">'),
    ('<Button style="-fx-background-color: transparent; -fx-cursor: hand;">', '<Button minWidth="44.0" prefWidth="44.0" maxWidth="44.0" minHeight="44.0" prefHeight="44.0" maxHeight="44.0" style="-fx-background-color: transparent; -fx-background-radius: 22px; -fx-padding: 0; -fx-alignment: CENTER; -fx-cursor: hand;">'),
    ('<MenuButton fx:id="userMenuButton" id="profile-menu" style="-fx-background-color: transparent; -fx-cursor: hand;">', '<MenuButton fx:id="userMenuButton" id="profile-menu" minWidth="54.0" prefWidth="54.0" maxWidth="54.0" minHeight="44.0" prefHeight="44.0" maxHeight="44.0" style="-fx-background-color: transparent; -fx-padding: 0; -fx-alignment: CENTER; -fx-cursor: hand;">')
]:
    if old in f:
        f = f.replace(old, new, 1 if 'MenuButton' in old or 'HBox spacing' in old else 2)
# Ensure notification/settings icon labels have no vertical drift.
f = f.replace("-fx-font-size: 20px; -fx-font-weight: normal; -fx-text-fill: #604868;", "-fx-font-size: 22px; -fx-font-weight: normal; -fx-text-fill: #604868;")
# Do not make every material icon larger? This only changes exact pattern in top icons and possibly hamburger? Acceptable minimal.

if f != fxml.read_text(encoding="utf-8"):
    fxml.write_text(f, encoding="utf-8")
    print("Patched MyBids.fxml header alignment")
else:
    print("WARN: MyBids.fxml không thay đổi.")

# Remove stale client target so FXML/CSS/controller cache cannot hide the change.
target = ROOT / "client/target"
if target.exists():
    shutil.rmtree(target)
    print("Removed stale target: client/target")

print("DONE: Đã sửa My Bids badge/filter/header, có backup tại:", backup)
