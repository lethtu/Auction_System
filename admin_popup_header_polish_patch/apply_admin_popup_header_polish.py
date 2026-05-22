from pathlib import Path
from datetime import datetime
import shutil
import re

ROOT = Path.cwd()
STAMP = datetime.now().strftime('%Y%m%d_%H%M%S')
BACKUP = ROOT / f"backup_admin_popup_header_polish_{STAMP}"

FILES = {
    "alert": ROOT / "client/src/main/java/com/auction/client/util/AlertUtil.java",
    "controller": ROOT / "client/src/main/java/com/auction/client/controller/AdminDashboardController.java",
    "fxml": ROOT / "client/src/main/java/com/auction/client/view/AdminDashboard.fxml",
    "css": ROOT / "client/src/main/java/com/auction/client/view/styles.css",
}

missing = [str(p) for p in FILES.values() if not p.exists()]
if missing:
    raise SystemExit("Không tìm thấy file cần sửa:\n" + "\n".join(missing))

BACKUP.mkdir(parents=True, exist_ok=True)
for name, path in FILES.items():
    target = BACKUP / path.relative_to(ROOT)
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(path, target)

ALERT_UTIL = r'''package com.auction.client.util;

import java.net.URL;

import javafx.scene.control.Alert;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;

public final class AlertUtil {
    private static final String DEFAULT_INFO_TITLE = "Thành công";
    private static final String DEFAULT_ERROR_TITLE = "Lỗi";
    private static final String DEFAULT_WARNING_TITLE = "Cảnh báo";
    private static final String DEFAULT_ERROR_MESSAGE = "Đã xảy ra lỗi. Vui lòng thử lại.";
    private static final String ADMIN_STYLESHEET_PATH = "/com/auction/client/view/styles.css";
    private static final double DEFAULT_DIALOG_WIDTH = 520;

    private AlertUtil() {
    }

    public static void show(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(normalizeTitle(title, type));
        alert.setHeaderText(null);
        alert.setContentText(normalizeMessage(message));
        alert.setGraphic(createStyledIcon(type));
        styleAlert(alert, type);
        alert.showAndWait();
    }

    public static void styleDialog(Dialog<?> dialog) {
        if (dialog == null) {
            return;
        }

        styleDialogPane(dialog.getDialogPane(), null);
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

    private static void styleAlert(Alert alert, Alert.AlertType type) {
        if (alert == null) {
            return;
        }

        String typeClass = type == null ? null : "admin-alert-" + type.name().toLowerCase();
        styleDialogPane(alert.getDialogPane(), typeClass);
    }

    private static void styleDialogPane(DialogPane dialogPane, String typeClass) {
        if (dialogPane == null) {
            return;
        }

        addStyleClass(dialogPane, "admin-alert-dialog");
        if (!isBlank(typeClass)) {
            addStyleClass(dialogPane, typeClass);
        }

        URL stylesheet = AlertUtil.class.getResource(ADMIN_STYLESHEET_PATH);
        if (stylesheet != null) {
            String stylesheetPath = stylesheet.toExternalForm();
            if (!dialogPane.getStylesheets().contains(stylesheetPath)) {
                dialogPane.getStylesheets().add(stylesheetPath);
            }
        }

        dialogPane.setMinWidth(DEFAULT_DIALOG_WIDTH);
        dialogPane.setPrefWidth(DEFAULT_DIALOG_WIDTH);
    }

    private static Label createStyledIcon(Alert.AlertType type) {
        Label icon = new Label(iconText(type));
        icon.getStyleClass().add("admin-alert-icon");
        icon.getStyleClass().add("admin-alert-icon-" + iconType(type));
        return icon;
    }

    private static String iconText(Alert.AlertType type) {
        if (type == Alert.AlertType.INFORMATION) {
            return "✓";
        }
        if (type == Alert.AlertType.ERROR) {
            return "×";
        }
        return "!";
    }

    private static String iconType(Alert.AlertType type) {
        if (type == Alert.AlertType.INFORMATION) {
            return "information";
        }
        if (type == Alert.AlertType.ERROR) {
            return "error";
        }
        return "warning";
    }

    private static void addStyleClass(DialogPane dialogPane, String styleClass) {
        if (!dialogPane.getStyleClass().contains(styleClass)) {
            dialogPane.getStyleClass().add(styleClass);
        }
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
FILES["alert"].write_text(ALERT_UTIL, encoding="utf-8")
print("Patched: AlertUtil.java")

# Style TextInputDialog in reject reason prompt only; logic unchanged.
controller = FILES["controller"].read_text(encoding="utf-8")
if "AlertUtil.styleDialog(dialog);" not in controller:
    controller_new = controller.replace(
        '        dialog.setContentText("Nhập lý do từ chối:");\n\n        String reason = dialog.showAndWait().orElse("").trim();',
        '        dialog.setContentText("Nhập lý do từ chối:");\n        AlertUtil.styleDialog(dialog);\n\n        String reason = dialog.showAndWait().orElse("").trim();'
    )
    if controller_new == controller:
        print("WARN: Không tìm thấy vị trí TextInputDialog để style; bỏ qua AdminDashboardController.java")
    else:
        FILES["controller"].write_text(controller_new, encoding="utf-8")
        print("Patched: AdminDashboardController.java")
else:
    print("Skip: AdminDashboardController.java already styled")

# Strengthen Admin Control Panel title.
fxml = FILES["fxml"].read_text(encoding="utf-8")
pattern = re.compile(
    r'\s*<Label\s+text="Admin Control Panel"\s+textFill="#2e1a28">\s*<font>\s*<Font\s+name="System Bold"\s+size="30"\s*/>\s*</font>\s*</Label>',
    re.DOTALL
)
fxml_new = pattern.sub('\n                        <Label text="Admin Control Panel" styleClass="admin-bidpop-page-title"/>', fxml, count=1)
if fxml_new == fxml and 'styleClass="admin-bidpop-page-title"' not in fxml:
    fxml_new = fxml.replace('<Label text="Admin Control Panel" textFill="#2e1a28">', '<Label text="Admin Control Panel" styleClass="admin-bidpop-page-title">', 1)
if fxml_new != fxml:
    FILES["fxml"].write_text(fxml_new, encoding="utf-8")
    print("Patched: AdminDashboard.fxml")
else:
    print("Skip: AdminDashboard.fxml already has page title style or no exact match")

css = FILES["css"].read_text(encoding="utf-8")
CSS_BLOCK = r'''

/* === ADMIN POPUP + TITLE POLISH START === */
.admin-bidpop-page-title {
    -fx-text-fill: #2e1a28;
    -fx-font-size: 34px;
    -fx-font-weight: 900;
    -fx-effect: dropshadow(gaussian, rgba(255, 255, 255, 0.85), 2, 0.25, 0, 1);
}

.admin-alert-dialog {
    -fx-font-family: "DM Sans", "Segoe UI", sans-serif;
    -fx-background-color: linear-gradient(to bottom right, #ffffff, #fff7fc);
    -fx-background-radius: 26;
    -fx-border-color: #f0a0cc;
    -fx-border-width: 1.4;
    -fx-border-radius: 26;
    -fx-padding: 16 18 14 18;
    -fx-effect: dropshadow(gaussian, rgba(224, 64, 160, 0.22), 24, 0.18, 0, 8);
}

.admin-alert-dialog > .header-panel {
    -fx-background-color: transparent;
    -fx-padding: 0;
}

.admin-alert-dialog > .content.label {
    -fx-text-fill: #2e1a28;
    -fx-font-size: 15px;
    -fx-font-weight: 700;
    -fx-padding: 18 18 18 8;
}

.admin-alert-dialog > .graphic-container {
    -fx-padding: 12 14 12 14;
}

.admin-alert-icon {
    -fx-min-width: 54;
    -fx-min-height: 54;
    -fx-pref-width: 54;
    -fx-pref-height: 54;
    -fx-max-width: 54;
    -fx-max-height: 54;
    -fx-alignment: center;
    -fx-background-radius: 18;
    -fx-text-fill: white;
    -fx-font-size: 28px;
    -fx-font-weight: 900;
    -fx-effect: dropshadow(gaussian, rgba(46, 26, 40, 0.18), 12, 0.20, 0, 4);
}

.admin-alert-icon-warning {
    -fx-background-color: linear-gradient(to bottom right, #fbbf24, #f97316);
}

.admin-alert-icon-error {
    -fx-background-color: linear-gradient(to bottom right, #fb7185, #ef4444);
}

.admin-alert-icon-information {
    -fx-background-color: linear-gradient(to bottom right, #34d399, #22c55e);
}

.admin-alert-dialog .button-bar {
    -fx-padding: 4 0 0 0;
}

.admin-alert-dialog .button-bar .container {
    -fx-padding: 0 2 0 0;
}

.admin-alert-dialog .button {
    -fx-background-color: #e040a0;
    -fx-background-radius: 999;
    -fx-border-color: transparent;
    -fx-text-fill: white;
    -fx-font-weight: 900;
    -fx-font-size: 13px;
    -fx-padding: 9 24 9 24;
    -fx-cursor: hand;
    -fx-effect: dropshadow(gaussian, rgba(224, 64, 160, 0.28), 12, 0.20, 0, 4);
}

.admin-alert-dialog .button:hover {
    -fx-background-color: #c92f8b;
    -fx-scale-x: 1.02;
    -fx-scale-y: 1.02;
}

.admin-alert-dialog .text-field,
.admin-alert-dialog .text-area {
    -fx-background-color: #fff7fc;
    -fx-background-radius: 16;
    -fx-border-color: #f4c7e0;
    -fx-border-radius: 16;
    -fx-padding: 10 14 10 14;
    -fx-text-fill: #2e1a28;
    -fx-font-weight: 600;
}

.admin-alert-dialog .text-field:focused,
.admin-alert-dialog .text-area:focused {
    -fx-border-color: #e040a0;
    -fx-effect: dropshadow(gaussian, rgba(224, 64, 160, 0.18), 10, 0.20, 0, 3);
}
/* === ADMIN POPUP + TITLE POLISH END === */
'''
if "ADMIN POPUP + TITLE POLISH START" not in css:
    FILES["css"].write_text(css + CSS_BLOCK, encoding="utf-8")
    print("Patched: styles.css")
else:
    print("Skip: styles.css already contains popup polish block")

# Remove stale build output so FXML/CSS/AlertUtil are rebuilt cleanly.
for rel in ["client/target", "server/target"]:
    target = ROOT / rel
    if target.exists():
        shutil.rmtree(target)
        print(f"Removed stale target: {rel}")

print(f"Backup folder: {BACKUP}")
print("DONE: Đã làm đẹp popup Admin và làm rõ tiêu đề Admin Control Panel.")
