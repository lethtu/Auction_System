from pathlib import Path
import re
import shutil
from datetime import datetime

ROOT = Path.cwd()
BACKUP = ROOT / f"backup_fix_bidder_layout_plus_deposit_popup_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
BACKUP.mkdir(exist_ok=True)

def p(rel):
    return ROOT / rel

def backup(path: Path):
    if path.exists():
        shutil.copy2(path, BACKUP / path.name)

def read_text(path: Path) -> str:
    raw = path.read_bytes()
    for enc in ("utf-8-sig", "utf-8", "utf-16"):
        try:
            return raw.decode(enc)
        except UnicodeDecodeError:
            pass
    return raw.decode("utf-8", errors="ignore")

def write_text(path: Path, text: str):
    path.write_text(text.lstrip('\ufeff'), encoding="utf-8")

def replace_method(text: str, signature_pattern: str, new_method: str, label: str, required: bool = True) -> str:
    m = re.search(signature_pattern, text, flags=re.MULTILINE)
    if not m:
        if required:
            raise SystemExit(f"Không tìm thấy method để sửa: {label}")
        return text
    start = m.start()
    brace = text.find('{', m.end() - 1)
    if brace == -1:
        raise SystemExit(f"Không tìm thấy dấu mở method: {label}")
    depth = 0
    in_str = False
    esc = False
    end = None
    for i in range(brace, len(text)):
        ch = text[i]
        if in_str:
            if esc:
                esc = False
            elif ch == '\\':
                esc = True
            elif ch == '"':
                in_str = False
            continue
        if ch == '"':
            in_str = True
        elif ch == '{':
            depth += 1
        elif ch == '}':
            depth -= 1
            if depth == 0:
                end = i + 1
                break
    if end is None:
        raise SystemExit(f"Không tìm thấy điểm kết thúc method: {label}")
    return text[:start] + new_method.strip() + "\n\n" + text[end:]

def ensure_import(text: str, import_line: str) -> str:
    if import_line in text:
        return text
    if "import javafx.scene.control.*;" in text and import_line.startswith("import javafx.scene.control."):
        return text
    m = re.search(r'(^import\s+[^;]+;\s*)+', text, flags=re.MULTILINE)
    if m:
        return text[:m.end()] + import_line + "\n" + text[m.end():]
    return text

def patch_main_controller():
    path = p('client/src/main/java/com/auction/client/controller/MainController.java')
    if not path.exists():
        print('Skip MainController.java: not found')
        return
    backup(path)
    text = read_text(path)
    text = ensure_import(text, 'import javafx.scene.control.ScrollPane;')

    update_grid = '''
    private void updateGridLayout() {
        if (scrollPane == null || productContainer == null) return;

        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        productContainer.setAlignment(Pos.TOP_LEFT);
        productContainer.setHgap(44.0);
        productContainer.setVgap(36.0);
        productContainer.setPadding(new Insets(10.0, 18.0, 32.0, 18.0));

        double viewportWidth = scrollPane.getViewportBounds().getWidth();
        if (viewportWidth <= 0 && scrollPane.getWidth() > 0) {
            viewportWidth = scrollPane.getWidth();
        }
        if (viewportWidth <= 0) return;

        double stableWidth = Math.max(0, Math.floor(viewportWidth) - 36.0);
        productContainer.setPrefWrapLength(stableWidth);
        productContainer.setMinWidth(stableWidth);
        productContainer.setPrefWidth(stableWidth);
        productContainer.setMaxWidth(stableWidth);
    }
'''
    text = replace_method(text, r'\s*private\s+void\s+updateGridLayout\s*\(\s*\)', update_grid, 'updateGridLayout', required=False)

    text = text.replace('actionBox.setAlignment(Pos.CENTER);', 'actionBox.setAlignment(Pos.CENTER_RIGHT);')
    text = re.sub(r'actionBox\.setMinWidth\(102\.0\);', 'actionBox.setMinWidth(52.0);', text)
    text = re.sub(r'actionBox\.setPrefWidth\(102\.0\);', 'actionBox.setPrefWidth(52.0);', text)
    text = re.sub(r'actionBox\.setMaxWidth\(102\.0\);', 'actionBox.setMaxWidth(52.0);', text)

    text = re.sub(
        r'mainBtn\.setStyle\("[^"]*-fx-background-color:\s*#ffd6ee;[^"]*"\);',
        'mainBtn.setStyle("-fx-background-color: #ffd6ee; -fx-background-radius: 24px; -fx-padding: 0; -fx-alignment: center; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(224, 64, 160, 0.16), 10, 0, 0, 3);");',
        text,
        count=1
    )
    for old, new in [
        ('mainBtn.setMinSize(44.0, 44.0);', 'mainBtn.setMinSize(48.0, 48.0);'),
        ('mainBtn.setPrefSize(44.0, 44.0);', 'mainBtn.setPrefSize(48.0, 48.0);'),
        ('mainBtn.setMaxSize(44.0, 44.0);', 'mainBtn.setMaxSize(48.0, 48.0);'),
        ('mainPlusIcon.setMinSize(44.0, 44.0);', 'mainPlusIcon.setMinSize(48.0, 48.0);'),
        ('mainPlusIcon.setPrefSize(44.0, 44.0);', 'mainPlusIcon.setPrefSize(48.0, 48.0);'),
        ('mainPlusIcon.setMaxSize(44.0, 44.0);', 'mainPlusIcon.setMaxSize(48.0, 48.0);'),
    ]:
        text = text.replace(old, new)

    update_toggle = '''
    private void updateViewToggleButton(boolean compactMode) {
        if (btnToggleProductView == null) {
            return;
        }

        btnToggleProductView.getStyleClass().remove("view-toggle-button-active");
        if (compactMode) {
            btnToggleProductView.getStyleClass().add("view-toggle-button-active");
            btnToggleProductView.setTooltip(new Tooltip("Đang xem dạng danh sách. Bấm để về dạng thẻ."));
        } else {
            btnToggleProductView.setTooltip(new Tooltip("Xem dạng danh sách rút gọn"));
        }

        btnToggleProductView.setText("▦");
        btnToggleProductView.setAlignment(Pos.CENTER);
        btnToggleProductView.setContentDisplay(ContentDisplay.CENTER);
    }
'''
    text = replace_method(text, r'\s*private\s+void\s+updateViewToggleButton\s*\(\s*boolean\s+compactMode\s*\)', update_toggle, 'updateViewToggleButton', required=False)

    write_text(path, text)
    print('Patched MainController')

def patch_main_template():
    path = p('client/src/main/java/com/auction/client/view/MainTemplate.fxml')
    if not path.exists():
        print('Skip MainTemplate.fxml: not found')
        return
    backup(path)
    text = read_text(path)

    def normalize_button(m):
        block = m.group(1)
        block = re.sub(r'\s+alignment="[^"]*"', '', block)
        block = re.sub(r'\s+contentDisplay="[^"]*"', '', block)
        block = re.sub(r'\s+textAlignment="[^"]*"', '', block)
        block = re.sub(r'\s+text="[^"]*"', '', block)
        return block + ' text="▦" alignment="CENTER" contentDisplay="CENTER" textAlignment="CENTER">'

    text = re.sub(r'(<Button\s+fx:id="btnToggleProductView"[^>]*?)>', normalize_button, text, count=1, flags=re.DOTALL)
    write_text(path, text)
    print('Patched MainTemplate')

def patch_css():
    path = p('client/src/main/java/com/auction/client/view/styles.css')
    if not path.exists():
        print('Skip styles.css: not found')
        return
    backup(path)
    text = read_text(path)
    addition = r'''

/* Bidder dashboard final alignment polish */
.view-toggle-button {
    -fx-min-width: 52px;
    -fx-pref-width: 52px;
    -fx-max-width: 52px;
    -fx-min-height: 52px;
    -fx-pref-height: 52px;
    -fx-max-height: 52px;
    -fx-background-color: #ffffff;
    -fx-border-color: #f1ddea;
    -fx-border-width: 1.5px;
    -fx-background-radius: 26px;
    -fx-border-radius: 26px;
    -fx-text-fill: #907898;
    -fx-font-family: "DM Sans", "Segoe UI Symbol", "Arial";
    -fx-font-size: 21px;
    -fx-font-weight: 900;
    -fx-padding: 0;
    -fx-alignment: center;
    -fx-content-display: center;
    -fx-text-alignment: center;
    -fx-cursor: hand;
}

.view-toggle-button:hover,
.view-toggle-button-active {
    -fx-background-color: #ffeaf7;
    -fx-border-color: #ff9bd5;
    -fx-text-fill: #e040a0;
    -fx-effect: dropshadow(three-pass-box, rgba(224, 64, 160, 0.18), 14, 0, 0, 4);
}

.combo-box-popup .list-view {
    -fx-background-color: #ffffff;
    -fx-background-radius: 14px;
    -fx-border-radius: 14px;
    -fx-border-color: #ffdceb;
    -fx-border-width: 1.2px;
    -fx-padding: 6px;
    -fx-effect: dropshadow(three-pass-box, rgba(224, 64, 160, 0.16), 18, 0, 0, 6);
}

.combo-box-popup .list-cell {
    -fx-background-radius: 10px;
    -fx-padding: 8px 12px;
    -fx-font-family: "DM Sans";
    -fx-font-size: 13px;
    -fx-text-fill: #2e1a28;
}

.combo-box-popup .list-cell:hover,
.combo-box-popup .list-cell:selected {
    -fx-background-color: #ffeaf7;
    -fx-text-fill: #e040a0;
}
'''
    marker = '/* Bidder dashboard final alignment polish */'
    text = re.sub(r'\n/\* Bidder dashboard final alignment polish \*/[\s\S]*$', '', text).rstrip()
    text += addition
    write_text(path, text)
    print('Patched styles.css')

def patch_alert_util_style_hook():
    path = p('client/src/main/java/com/auction/client/util/AlertUtil.java')
    if not path.exists():
        return
    backup(path)
    text = read_text(path)
    if 'styleAndShow(Alert alert)' in text:
        write_text(path, text)
        return
    method = '''

    public static void styleAndShow(Alert alert) {
        if (alert == null) return;
        try {
            if (alert.getAlertType() == Alert.AlertType.ERROR) {
                showError(alert.getTitle() == null ? "Lỗi" : alert.getTitle(), alert.getContentText());
            } else if (alert.getAlertType() == Alert.AlertType.WARNING) {
                showWarning(alert.getTitle() == null ? "Cảnh báo" : alert.getTitle(), alert.getContentText());
            } else {
                showInfo(alert.getTitle() == null ? "Thông báo" : alert.getTitle(), alert.getContentText());
            }
        } catch (Exception ex) {
            alert.showAndWait();
        }
    }
'''
    idx = text.rfind('}')
    if idx != -1:
        text = text[:idx] + method + '\n' + text[idx:]
        write_text(path, text)
        print('Patched AlertUtil styleAndShow')

def patch_deposit_controller():
    candidates = [
        p('client/src/main/java/com/auction/client/controller/DepositController.java'),
        p('client/src/main/java/com/auction/client/controller/WalletController.java'),
        p('client/src/main/java/com/auction/client/controller/PaymentController.java'),
    ]
    touched = False
    for path in candidates:
        if not path.exists():
            continue
        text = read_text(path)
        if 'Alert.AlertType.INFORMATION' not in text:
            continue
        if 'Nạp tiền' not in text and 'nạp thành công' not in text.lower() and 'deposit' not in path.name.lower():
            continue
        backup(path)
        original = text
        text = ensure_import(text, 'import com.auction.client.util.AlertUtil;')
        pattern = re.compile(
            r'Alert\s+(\w+)\s*=\s*new\s+Alert\s*\(\s*Alert\.AlertType\.INFORMATION\s*\)\s*;\s*'
            r'\1\.setTitle\("Nạp tiền thành công"\)\s*;\s*'
            r'\1\.setHeaderText\(null\)\s*;\s*'
            r'\1\.setContentText\(([^;]+)\)\s*;\s*'
            r'\1\.(?:showAndWait|show)\(\)\s*;',
            flags=re.DOTALL
        )
        text = pattern.sub(r'AlertUtil.showInfo("Nạp tiền thành công", \2);', text)
        text = re.sub(
            r'(Alert\s+\w+\s*=\s*new\s+Alert\s*\(\s*Alert\.AlertType\.INFORMATION\s*\)[\s\S]{0,500}?\.setContentText\([\s\S]{0,300}?\);\s*)\w+\.(showAndWait|show)\(\)\s*;',
            lambda m: m.group(1) + 'AlertUtil.styleAndShow(alert);' if 'alert' in m.group(1) else m.group(0),
            text,
            count=1
        )
        if text != original:
            write_text(path, text)
            touched = True
            print(f'Patched {path.name} deposit/info popup')
    if not touched:
        print('Deposit popup patch skipped: no matching direct Alert block found')

def main():
    print(f'Project root: {ROOT}')
    print(f'Backup folder: {BACKUP}')
    patch_main_controller()
    patch_main_template()
    patch_css()
    patch_alert_util_style_hook()
    patch_deposit_controller()
    target = ROOT / 'client' / 'target'
    if target.exists():
        shutil.rmtree(target, ignore_errors=True)
        print('Removed client/target')
    print('DONE: layout stable + plus button aligned + toggle/dropdown polished + deposit popup patched when found')

if __name__ == '__main__':
    main()
