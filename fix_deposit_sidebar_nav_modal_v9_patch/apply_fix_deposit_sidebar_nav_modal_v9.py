from pathlib import Path
from datetime import datetime
import re
import shutil

ROOT = Path.cwd()
STAMP = datetime.now().strftime('%Y%m%d_%H%M%S')
BACKUP = ROOT / f'backup_fix_deposit_sidebar_nav_modal_v9_{STAMP}'
BACKUP.mkdir(exist_ok=True)

def read(p: Path) -> str:
    return p.read_text(encoding='utf-8-sig')

def write(p: Path, text: str):
    p.write_text(text, encoding='utf-8', newline='')

def backup(p: Path):
    if not p.exists():
        raise FileNotFoundError(p)
    dest = BACKUP / p.relative_to(ROOT)
    dest.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(p, dest)


def ensure_import(text: str, import_line: str) -> str:
    if import_line in text:
        return text
    # insert after package and existing imports near top
    m = re.search(r'(package\s+[^;]+;\s*)', text)
    if not m:
        return import_line + '\n' + text
    insert_at = m.end()
    return text[:insert_at] + '\n' + import_line + text[insert_at:]


def replace_method(text: str, signature_regex: str, new_method: str) -> str:
    m = re.search(signature_regex, text)
    if not m:
        raise RuntimeError(f'Không tìm thấy method theo pattern: {signature_regex}')
    start = m.start()
    brace = text.find('{', m.end() - 1)
    if brace < 0:
        raise RuntimeError('Không tìm thấy dấu { của method')
    depth = 0
    end = None
    for i in range(brace, len(text)):
        ch = text[i]
        if ch == '{':
            depth += 1
        elif ch == '}':
            depth -= 1
            if depth == 0:
                end = i + 1
                break
    if end is None:
        raise RuntimeError('Không tìm thấy cuối method')
    return text[:start] + new_method.rstrip() + '\n' + text[end:]


def insert_before_last_class_brace(text: str, code: str) -> str:
    idx = text.rfind('}')
    if idx < 0:
        raise RuntimeError('Không tìm thấy dấu } cuối class')
    return text[:idx].rstrip() + '\n\n' + code.rstrip() + '\n' + text[idx:]


def patch_deposit_controller():
    p = ROOT / 'client/src/main/java/com/auction/client/controller/DepositController.java'
    backup(p)
    text = read(p)

    # imports for robust menu navigation
    for imp in [
        'import javafx.fxml.FXMLLoader;',
        'import javafx.scene.Parent;',
        'import javafx.scene.Scene;',
        'import javafx.stage.Stage;',
        'import javafx.stage.Window;',
        'import com.auction.client.util.AlertUtil;'
    ]:
        text = ensure_import(text, imp)

    # Replace local account menu action to avoid MenuItem event source issues.
    account_action = '''accountItem.setOnAction(event -> {
            try {
                MainController.initialShowAccount = true;
                MainController.initialShowWatchlist = false;
                MainController.initialHomeFilterMode = "ACCOUNT";
                openMainTemplateFromCurrentWindow();
            } catch (IOException e) {
                logger.error("Lỗi khi chuyển sang trang tài khoản: ", e);
                AlertUtil.showInfo("Tài khoản", "Không thể mở trang tài khoản. Vui lòng thử lại.");
            }
        });'''
    text = re.sub(r'accountItem\.setOnAction\(event\s*->\s*\{[\s\S]*?\n\s*\}\);', account_action, text, count=1)

    # Replace old native Alert wrapper with the shared custom modal.
    new_show_alert = '''private void showAlert(Alert.AlertType type, String title, String message) {
        AlertUtil.show(type, title, message);
    }'''
    text = replace_method(text, r'private\s+void\s+showAlert\s*\(\s*Alert\.AlertType\s+type\s*,\s*String\s+title\s*,\s*String\s+message\s*\)', new_show_alert)

    if 'private void openMainTemplateFromCurrentWindow() throws IOException' not in text:
        helper = '''
    private void openMainTemplateFromCurrentWindow() throws IOException {
        Window window = userMenuButton != null && userMenuButton.getScene() != null
                ? userMenuButton.getScene().getWindow()
                : null;

        if (window instanceof Stage stage) {
            boolean wasMaximized = stage.isMaximized();
            int width = Math.max(1280, (int) Math.round(stage.getWidth()));
            int height = Math.max(800, (int) Math.round(stage.getHeight()));

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/client/view/MainTemplate.fxml"));
            Parent root = loader.load();
            stage.setScene(new Scene(root, width, height));
            stage.show();

            if (wasMaximized) {
                Platform.runLater(() -> stage.setMaximized(true));
            }
            return;
        }

        SceneSwitcher.switchScene(new ActionEvent(userMenuButton, userMenuButton), "MainTemplate.fxml", 1280, 800);
    }
'''
        # insert before showInfo wrappers
        marker = re.search(r'\n\s*private\s+void\s+showInfo\s*\(', text)
        if marker:
            text = text[:marker.start()] + helper + text[marker.start():]
        else:
            text = insert_before_last_class_brace(text, helper)

    write(p, text)
    print('Patched DepositController.java')


def patch_sidebar_controller():
    p = ROOT / 'client/src/main/java/com/auction/client/controller/SidebarController.java'
    backup(p)
    text = read(p)

    for imp in [
        'import javafx.fxml.FXMLLoader;',
        'import javafx.scene.Parent;',
        'import javafx.scene.Scene;',
        'import com.auction.client.util.AlertUtil;'
    ]:
        text = ensure_import(text, imp)

    # Use direct stage navigation for sidebar buttons to avoid failed navigation from nested/sidebar contexts.
    text = text.replace('SceneSwitcher.switchScene(event, "MainTemplate.fxml", currentWidth, currentHeight);',
                        'switchSceneKeepingStage(event, "MainTemplate.fxml", currentWidth, currentHeight);')
    text = text.replace('SceneSwitcher.switchScene(event, "MyBids.fxml", currentWidth, currentHeight);',
                        'switchSceneKeepingStage(event, "MyBids.fxml", currentWidth, currentHeight);')
    text = text.replace('SceneSwitcher.switchScene(event, "SellerDashboard.fxml", currentWidth, currentHeight);',
                        'switchSceneKeepingStage(event, "SellerDashboard.fxml", currentWidth, currentHeight);')
    text = text.replace('SceneSwitcher.switchScene(event, "Support.fxml", currentWidth, currentHeight);',
                        'switchSceneKeepingStage(event, "Support.fxml", currentWidth, currentHeight);')
    text = text.replace('SceneSwitcher.switchScene(event, "Settings.fxml", currentWidth, currentHeight);',
                        'switchSceneKeepingStage(event, "Settings.fxml", currentWidth, currentHeight);')
    text = text.replace('SceneSwitcher.switchScene(event, "UpToSeller.fxml", 1280, 800);',
                        'switchSceneKeepingStage(event, "UpToSeller.fxml", 1280, 800);')

    new_show_info = '''private void showInfo(String title, String message) {
        AlertUtil.showInfo(title, message);
    }'''
    text = replace_method(text, r'private\s+void\s+showInfo\s*\(\s*String\s+title\s*,\s*String\s+message\s*\)', new_show_info)

    if 'private void switchSceneKeepingStage(ActionEvent event, String fxmlName, int width, int height) throws IOException' not in text:
        helper = '''
    private void switchSceneKeepingStage(ActionEvent event, String fxmlName, int width, int height) throws IOException {
        Stage stage = resolveStage(event);
        if (stage == null) {
            SceneSwitcher.switchScene(event, fxmlName, width, height);
            return;
        }

        boolean wasMaximized = stage.isMaximized();
        int targetWidth = Math.max(900, width);
        int targetHeight = Math.max(650, height);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/client/view/" + fxmlName));
        Parent root = loader.load();
        stage.setScene(new Scene(root, targetWidth, targetHeight));
        stage.show();

        if (wasMaximized) {
            Platform.runLater(() -> stage.setMaximized(true));
        }
    }
'''
        # Insert before resolveStage helper
        marker = re.search(r'\n\s*private\s+Stage\s+resolveStage\s*\(', text)
        if marker:
            text = text[:marker.start()] + helper + text[marker.start():]
        else:
            text = insert_before_last_class_brace(text, helper)

    write(p, text)
    print('Patched SidebarController.java')


def main():
    print(f'Project root: {ROOT}')
    print(f'Backup folder: {BACKUP}')
    patch_deposit_controller()
    patch_sidebar_controller()

    target = ROOT / 'client/target'
    if target.exists():
        shutil.rmtree(target, ignore_errors=True)
        print('Removed client/target')

    print('\nDONE: Đã sửa popup nạp tiền + điều hướng Dashboard/Watchlist/Tài khoản.')
    print('Bước tiếp theo: cd client; mvn test')

if __name__ == '__main__':
    main()
