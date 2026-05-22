from pathlib import Path
import re
import shutil
from datetime import datetime

ROOT = Path.cwd()
STAMP = datetime.now().strftime('%Y%m%d_%H%M%S')
BACKUP = ROOT / f"backup_fix_header_login_modal_{STAMP}"
BACKUP.mkdir(exist_ok=True)

UTF8_NO_BOM = "utf-8"


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8-sig")


def write_text(path: Path, text: str):
    path.write_text(text.lstrip('\ufeff'), encoding=UTF8_NO_BOM)


def backup(path: Path):
    if not path.exists():
        return
    target = BACKUP / path.relative_to(ROOT)
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(path, target)


def patch_login_controller():
    path = ROOT / "client/src/main/java/com/auction/client/controller/LoginController.java"
    if not path.exists():
        print("SKIP LoginController.java: not found")
        return
    backup(path)
    text = read_text(path)

    if "import com.auction.client.util.AlertUtil;" not in text:
        text = text.replace("import com.auction.client.model.User;", "import com.auction.client.model.User;\nimport com.auction.client.util.AlertUtil;")

    new_method = '''private void showAlert(Alert.AlertType alertType, String title, String message) {
        if (isTestEnvironment()) {
            Alert alert = new Alert(alertType);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
            return;
        }

        AlertUtil.show(alertType, title, message);
    }'''

    pattern = re.compile(
        r'private\s+void\s+showAlert\s*\(\s*Alert\.AlertType\s+alertType\s*,\s*String\s+title\s*,\s*String\s+message\s*\)\s*\{.*?\n\s*\}\s*\n\s*\n\s*private\s+record',
        re.DOTALL
    )
    replacement = new_method + "\n\n    private record"
    text2, count = pattern.subn(replacement, text, count=1)
    if count == 0:
        # If the method was already partly patched, replace the body more conservatively.
        start = text.find("private void showAlert(Alert.AlertType alertType, String title, String message)")
        marker = "\n\n    private record"
        end = text.find(marker, start)
        if start != -1 and end != -1:
            text2 = text[:start] + new_method + text[end:]
            count = 1
        else:
            raise SystemExit("Không tìm thấy method showAlert trong LoginController.java để vá")

    write_text(path, text2)
    print("Patched LoginController.java: login alerts use styled modal outside tests")


def normalize_action_header(text: str) -> str:
    # Normalize action HBox containers on bidder/my-bids/support/auction pages.
    text = text.replace(
        '<HBox spacing="15.0" alignment="CENTER_RIGHT">',
        '<HBox spacing="10.0" alignment="CENTER_RIGHT" minHeight="52.0" prefHeight="52.0" maxHeight="52.0" style="-fx-padding: 0;">'
    )
    text = text.replace(
        '<HBox alignment="CENTER" spacing="15.0">',
        '<HBox alignment="CENTER_RIGHT" spacing="10.0" minHeight="52.0" prefHeight="52.0" maxHeight="52.0" style="-fx-padding: 0;">'
    )

    # Bell button with fx:id
    text = re.sub(
        r'<Button\s+fx:id="btnNotificationBell"\s+style="[^"]*">',
        '<Button fx:id="btnNotificationBell" minWidth="44.0" prefWidth="44.0" maxWidth="44.0" minHeight="44.0" prefHeight="44.0" maxHeight="44.0" style="-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0; -fx-alignment: center;">',
        text
    )

    # Plain bell/settings buttons in MyBids/Support topbars.
    text = re.sub(
        r'<Button\s+style="-fx-background-color:\s*transparent;\s*-fx-cursor:\s*hand;">\s*\n\s*<graphic>\s*\n\s*<Label\s+text="&#xe7f4;"',
        '<Button minWidth="44.0" prefWidth="44.0" maxWidth="44.0" minHeight="44.0" prefHeight="44.0" maxHeight="44.0" style="-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0; -fx-alignment: center;">\n                    <graphic>\n                        <Label text="&#xe7f4;"',
        text
    )
    text = re.sub(
        r'<Button\s+style="-fx-background-color:\s*transparent;\s*-fx-cursor:\s*hand;">\s*\n\s*<graphic>\s*\n\s*<Label\s+text="&#xe8b8;"',
        '<Button minWidth="44.0" prefWidth="44.0" maxWidth="44.0" minHeight="44.0" prefHeight="44.0" maxHeight="44.0" style="-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0; -fx-alignment: center;">\n                    <graphic>\n                        <Label text="&#xe8b8;"',
        text
    )

    # Settings button with action if any.
    text = re.sub(
        r'<Button\s+onAction="#handleSettings"\s+style="[^"]*">',
        '<Button onAction="#handleSettings" minWidth="44.0" prefWidth="44.0" maxWidth="44.0" minHeight="44.0" prefHeight="44.0" maxHeight="44.0" style="-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0; -fx-alignment: center;">',
        text
    )

    # AuctionPage plain settings button with padding:8.
    text = re.sub(
        r'<Button\s+style="-fx-background-color:\s*transparent;\s*-fx-cursor:\s*hand;\s*-fx-padding:\s*8;">\s*\n\s*<graphic>\s*\n\s*<Label\s+text="&#xe8b8;"',
        '<Button minWidth="44.0" prefWidth="44.0" maxWidth="44.0" minHeight="44.0" prefHeight="44.0" maxHeight="44.0" style="-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0; -fx-alignment: center;">\n                            <graphic>\n                                <Label text="&#xe8b8;"',
        text
    )

    # StackPane around notification badge should align to center but keep badge top-right.
    text = text.replace(
        '<StackPane alignment="TOP_RIGHT">',
        '<StackPane alignment="CENTER" minWidth="44.0" prefWidth="44.0" maxWidth="44.0" minHeight="44.0" prefHeight="44.0" maxHeight="44.0">'
    )

    # Badge translation after making the bell container centered.
    text = re.sub(
        r'-fx-translate-x:\s*-5;\s*-fx-translate-y:\s*5;',
        '-fx-translate-x: 12; -fx-translate-y: -12;',
        text
    )

    # MenuButton fixed height/width and centered.
    text = re.sub(
        r'<MenuButton\s+fx:id="userMenuButton"\s+id="profile-menu"\s+style="[^"]*">',
        '<MenuButton fx:id="userMenuButton" id="profile-menu" minWidth="58.0" prefWidth="58.0" maxWidth="58.0" minHeight="44.0" prefHeight="44.0" maxHeight="44.0" style="-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0; -fx-alignment: center; -fx-background-insets: 0;">',
        text
    )

    # Profile avatar circle: slightly larger, aligns with 44px action buttons.
    text = re.sub(
        r'<StackPane\s+minWidth="36"\s+maxWidth="36"\s+minHeight="36"\s+maxHeight="36"\s+prefWidth="36"\s+prefHeight="36"\s+style="-fx-background-color:\s*#1a1a3a;\s*-fx-background-radius:\s*18px;">',
        '<StackPane minWidth="44" maxWidth="44" minHeight="44" maxHeight="44" prefWidth="44" prefHeight="44" style="-fx-background-color: #1a1a3a; -fx-background-radius: 22px;">',
        text
    )

    # Remove small icon vertical offsets in the topbar that make icons look uneven.
    text = text.replace('translateY="3.0" />', 'translateY="0.0" />')

    return text


def patch_fxml_headers():
    files = [
        "client/src/main/java/com/auction/client/view/MainTemplate.fxml",
        "client/src/main/java/com/auction/client/view/MyBids.fxml",
        "client/src/main/java/com/auction/client/view/Support.fxml",
        "client/src/main/java/com/auction/client/view/AuctionPage.fxml",
        "client/src/main/java/com/auction/client/view/UpToSeller.fxml",
    ]
    for rel in files:
        path = ROOT / rel
        if not path.exists():
            print(f"SKIP {rel}: not found")
            continue
        backup(path)
        text = read_text(path)
        text2 = normalize_action_header(text)
        if text2 != text:
            write_text(path, text2)
            print(f"Patched header alignment: {rel}")
        else:
            print(f"No header alignment changes needed: {rel}")


def remove_targets():
    for rel in ["client/target"]:
        p = ROOT / rel
        if p.exists():
            shutil.rmtree(p, ignore_errors=True)
            print(f"Removed stale target: {rel}")


def main():
    print(f"Project root: {ROOT}")
    print(f"Backup folder: {BACKUP}")
    patch_login_controller()
    patch_fxml_headers()
    remove_targets()
    print("DONE: Đã căn header icon và chuyển popup đăng nhập sang style mới.")
    print("Chạy: cd client && mvn test")


if __name__ == "__main__":
    main()
