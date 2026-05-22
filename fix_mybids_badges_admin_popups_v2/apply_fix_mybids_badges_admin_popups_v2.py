from pathlib import Path
from datetime import datetime
import re
import shutil

ROOT = Path.cwd()
print(f"Project root: {ROOT}")

backup = ROOT / f"backup_mybids_badges_admin_popups_v2_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
backup.mkdir(exist_ok=True)
print(f"Backup folder: {backup}")

files = [
    Path("client/src/main/java/com/auction/client/controller/MyBidsController.java"),
    Path("client/src/main/java/com/auction/client/view/MyBids.fxml"),
    Path("client/src/main/java/com/auction/client/util/AlertUtil.java"),
    Path("client/src/main/java/com/auction/client/controller/AdminDashboardController.java"),
]

for rel in files:
    src = ROOT / rel
    if not src.exists():
        raise SystemExit(f"Missing file: {rel}")
    dst = backup / rel
    dst.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(src, dst)

def read(path):
    return path.read_text(encoding="utf-8")

def write(path, text):
    path.write_text(text, encoding="utf-8", newline="\n")

# 1) Fix MyBids status badges being removed when image loading fails + robust status filters.
mybids = ROOT / "client/src/main/java/com/auction/client/controller/MyBidsController.java"
text = read(mybids)

# Add robust status helper methods if missing.
if "private boolean isActiveStatus(String status)" not in text:
    helper = '''
    private boolean isActiveStatus(String status) {
        if (status == null) {
            return false;
        }
        String normalized = status.trim();
        return "ACTIVE".equalsIgnoreCase(normalized)
                || "ONGOING".equalsIgnoreCase(normalized)
                || "LIVE".equalsIgnoreCase(normalized)
                || "OPEN".equalsIgnoreCase(normalized);
    }

    private boolean isEndedStatus(String status) {
        if (status == null) {
            return false;
        }
        String normalized = status.trim();
        return "ENDED".equalsIgnoreCase(normalized)
                || "CLOSED".equalsIgnoreCase(normalized)
                || "COMPLETED".equalsIgnoreCase(normalized)
                || "FINISHED".equalsIgnoreCase(normalized)
                || "CANCELLED".equalsIgnoreCase(normalized)
                || "CANCELED".equalsIgnoreCase(normalized);
    }

'''
    marker = "    private VBox createProductCard(JSONObject sessionObj, JSONObject itemObj) {"
    if marker not in text:
        raise SystemExit("Cannot find createProductCard marker")
    text = text.replace(marker, helper + marker, 1)

text = text.replace('"ACTIVE".equalsIgnoreCase(status)', 'isActiveStatus(status)')
text = text.replace('"ENDED".equalsIgnoreCase(status)', 'isEndedStatus(status)')

old_image_block = '''        String imageUrl = buildImageUrl(imagePath);
        if (!imageUrl.isBlank()) {
            Image cached = imageCache.get(imageUrl);
            if (cached == null || cached.isError()) {
                cached = new Image(imageUrl, true);
                imageCache.put(imageUrl, cached);
            }
            imageView.setImage(cached);
            imageWrapper.getChildren().add(imageView);
            cached.errorProperty().addListener((obs, oldValue, isError) -> {
                if (isError && !imageWrapper.getChildren().contains(imageStatusLabel)) {
                    imageWrapper.getChildren().setAll(imageStatusLabel);
                }
            });
        } else {
            imageWrapper.getChildren().add(imageStatusLabel);
        }
'''
new_image_block = '''        String imageUrl = buildImageUrl(imagePath);
        imageWrapper.getChildren().add(imageStatusLabel);
        if (!imageUrl.isBlank()) {
            Image cached = imageCache.get(imageUrl);
            if (cached == null || cached.isError()) {
                cached = new Image(imageUrl, true);
                imageCache.put(imageUrl, cached);
            }
            imageView.setImage(cached);
            imageWrapper.getChildren().add(imageView);
            cached.errorProperty().addListener((obs, oldValue, isError) -> {
                if (isError) {
                    imageWrapper.getChildren().remove(imageView);
                    if (!imageWrapper.getChildren().contains(imageStatusLabel)) {
                        imageWrapper.getChildren().add(0, imageStatusLabel);
                    }
                }
            });
        }
'''
if old_image_block in text:
    text = text.replace(old_image_block, new_image_block, 1)
else:
    print("WARNING: image block already changed or not found; skipping exact image block replacement")

# Make the two status badges simpler and consistent: every active outbid/winning card keeps labels even if image fails.
text = text.replace('badgeLabel.setText("Highest Bidder");', 'badgeLabel.setText("Winning");')

write(mybids, text)
print("Patched MyBidsController.java")

# 2) Align MyBids top-right header icons.
fxml = ROOT / "client/src/main/java/com/auction/client/view/MyBids.fxml"
fx = read(fxml)
fx = fx.replace('<HBox spacing="15.0" alignment="CENTER_RIGHT">', '<HBox spacing="10.0" alignment="CENTER" prefHeight="48.0" minHeight="48.0" maxHeight="48.0">')
fx = fx.replace('<Button style="-fx-background-color: transparent; -fx-cursor: hand;">', '<Button minWidth="44.0" prefWidth="44.0" maxWidth="44.0" minHeight="44.0" prefHeight="44.0" maxHeight="44.0" alignment="CENTER" style="-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0;">')
fx = fx.replace('<MenuButton fx:id="userMenuButton" id="profile-menu" style="-fx-background-color: transparent; -fx-cursor: hand;">', '<MenuButton fx:id="userMenuButton" id="profile-menu" minWidth="54.0" prefWidth="54.0" maxWidth="54.0" minHeight="48.0" prefHeight="48.0" maxHeight="48.0" alignment="CENTER" style="-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0;">')
write(fxml, fx)
print("Patched MyBids.fxml")

# 3) Replace AlertUtil with compact modern custom dialog used by Admin actions.
alert_util = ROOT / "client/src/main/java/com/auction/client/util/AlertUtil.java"
alert_code = r'''package com.auction.client.util;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public final class AlertUtil {
    private static final String DEFAULT_INFO_TITLE = "Thành công";
    private static final String DEFAULT_ERROR_TITLE = "Lỗi";
    private static final String DEFAULT_WARNING_TITLE = "Cảnh báo";
    private static final String DEFAULT_ERROR_MESSAGE = "Đã xảy ra lỗi. Vui lòng thử lại.";

    private AlertUtil() {
    }

    public static void show(Alert.AlertType type, String title, String message) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(normalizeTitle(title, type));
        dialog.setHeaderText(null);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        dialog.getDialogPane().setContent(buildContent(type, normalizeMessage(message)));
        styleDialog(dialog);
        stylePrimaryButton(dialog.getDialogPane().lookupButton(ButtonType.OK));
        dialog.showAndWait();
    }

    public static void showInfo(String message) {
        show(Alert.AlertType.INFORMATION, DEFAULT_INFO_TITLE, message);
    }

    public static void showWarning(String title, String message) {
        show(Alert.AlertType.WARNING, title, message);
    }

    public static void showError(String message) {
        show(Alert.AlertType.ERROR, DEFAULT_ERROR_TITLE, message);
    }

    public static void showError(Exception e, String defaultMessage) {
        String errorMessage = extractErrorMessage(e, defaultMessage);
        showError(errorMessage);
    }

    public static void styleDialog(Dialog<?> dialog) {
        if (dialog == null || dialog.getDialogPane() == null) {
            return;
        }

        DialogPane pane = dialog.getDialogPane();
        pane.setMinWidth(430);
        pane.setPrefWidth(500);
        pane.setStyle("-fx-background-color: #fff9fd;"
                + " -fx-border-color: #f6a6d7;"
                + " -fx-border-width: 1.5px;"
                + " -fx-border-radius: 18px;"
                + " -fx-background-radius: 18px;"
                + " -fx-padding: 18px;"
                + " -fx-font-family: 'DM Sans';");

        pane.lookupAll(".button").forEach(AlertUtil::styleSecondaryButton);
    }

    private static Node buildContent(Alert.AlertType type, String message) {
        HBox root = new HBox(18);
        root.setAlignment(Pos.CENTER_LEFT);
        root.setPadding(new Insets(8, 8, 4, 8));

        StackPane iconBox = new StackPane();
        iconBox.setMinSize(58, 58);
        iconBox.setPrefSize(58, 58);
        iconBox.setMaxSize(58, 58);
        iconBox.setStyle("-fx-background-color: " + iconBackground(type) + ";"
                + " -fx-background-radius: 18px;"
                + " -fx-effect: dropshadow(three-pass-box, rgba(224, 64, 160, 0.22), 12, 0, 0, 4);");

        Label icon = new Label(iconText(type));
        icon.setStyle("-fx-text-fill: white; -fx-font-weight: 900; -fx-font-size: 28px;");
        iconBox.getChildren().add(icon);

        Label text = new Label(message);
        text.setWrapText(true);
        text.setMaxWidth(360);
        text.setStyle("-fx-text-fill: #211427; -fx-font-size: 16px; -fx-font-weight: 700; -fx-line-spacing: 2px;");

        root.getChildren().addAll(iconBox, text);
        HBox.setHgrow(text, Priority.ALWAYS);
        return root;
    }

    private static void stylePrimaryButton(Node node) {
        if (node instanceof Button button) {
            button.setMinWidth(96);
            button.setMinHeight(42);
            button.setStyle("-fx-background-color: #e040a0;"
                    + " -fx-text-fill: white;"
                    + " -fx-font-weight: 800;"
                    + " -fx-background-radius: 18px;"
                    + " -fx-padding: 10px 26px;"
                    + " -fx-cursor: hand;"
                    + " -fx-effect: dropshadow(three-pass-box, rgba(224, 64, 160, 0.25), 12, 0, 0, 4);");
        }
    }

    private static void styleSecondaryButton(Node node) {
        if (node instanceof Button button) {
            button.setStyle("-fx-background-color: #fff1fa;"
                    + " -fx-text-fill: #8a2b66;"
                    + " -fx-font-weight: 700;"
                    + " -fx-background-radius: 16px;"
                    + " -fx-border-color: #f6a6d7;"
                    + " -fx-border-radius: 16px;"
                    + " -fx-padding: 8px 18px;"
                    + " -fx-cursor: hand;");
        }
    }

    private static String iconText(Alert.AlertType type) {
        return switch (type) {
            case INFORMATION -> "✓";
            case ERROR -> "×";
            case WARNING -> "!";
            default -> "i";
        };
    }

    private static String iconBackground(Alert.AlertType type) {
        return switch (type) {
            case INFORMATION -> "#22c55e";
            case ERROR -> "#ef4444";
            case WARNING -> "#f59e0b";
            default -> "#e040a0";
        };
    }

    private static String extractErrorMessage(Exception e, String defaultMessage) {
        if (e != null && !isBlank(e.getMessage())) {
            return e.getMessage();
        }

        if (!isBlank(defaultMessage)) {
            return defaultMessage;
        }

        return DEFAULT_ERROR_MESSAGE;
    }

    private static String normalizeTitle(String title, Alert.AlertType type) {
        if (!isBlank(title)) {
            return title;
        }

        return switch (type) {
            case INFORMATION -> DEFAULT_INFO_TITLE;
            case WARNING -> DEFAULT_WARNING_TITLE;
            case ERROR -> DEFAULT_ERROR_TITLE;
            default -> "";
        };
    }

    private static String normalizeMessage(String message) {
        return isBlank(message) ? DEFAULT_ERROR_MESSAGE : message;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
'''
write(alert_util, alert_code)
print("Patched AlertUtil.java")

# 4) Style Admin reject-reason TextInputDialog too.
admin = ROOT / "client/src/main/java/com/auction/client/controller/AdminDashboardController.java"
ad = read(admin)
if 'AlertUtil.styleDialog(dialog);' not in ad:
    old = '''        dialog.setTitle("Từ chối phiên");
        dialog.setHeaderText(null);
        dialog.setContentText("Nhập lý do từ chối:");

        String reason = dialog.showAndWait().orElse("").trim();
'''
    new = '''        dialog.setTitle("Từ chối phiên");
        dialog.setHeaderText(null);
        dialog.setContentText("Nhập lý do từ chối:");
        AlertUtil.styleDialog(dialog);

        String reason = dialog.showAndWait().orElse("").trim();
'''
    if old in ad:
        ad = ad.replace(old, new, 1)
    else:
        print("WARNING: reject reason dialog block not found; skipping AdminDashboardController dialog style injection")
write(admin, ad)
print("Patched AdminDashboardController.java")

# Clear JavaFX stale resources/classes.
for target in [ROOT / "client/target"]:
    if target.exists():
        shutil.rmtree(target)
        print(f"Removed stale target: {target.relative_to(ROOT)}")

print("DONE: Fixed My Bids badges/header and compact Admin popups.")
print("Run: cd client && mvn test")
