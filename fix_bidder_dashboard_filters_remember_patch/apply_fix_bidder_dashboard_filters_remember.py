from pathlib import Path
import re
import shutil
from datetime import datetime

ROOT = Path.cwd()
STAMP = datetime.now().strftime('%Y%m%d_%H%M%S')
BACKUP = ROOT / f"backup_fix_bidder_dashboard_filters_remember_{STAMP}"
BACKUP.mkdir(exist_ok=True)


def read_text(path: Path) -> str:
    data = path.read_bytes()
    # Handle files accidentally saved as UTF-16 by PowerShell, and UTF-8 BOM.
    for enc in ("utf-8-sig", "utf-16", "utf-16-le", "utf-16-be", "cp1258", "latin1"):
        try:
            text = data.decode(enc)
            # Prefer decodings that actually look like XML/Java.
            if "<" in text[:200] or "package " in text[:200] or "import " in text[:500]:
                return text.lstrip('\ufeff')
        except Exception:
            pass
    return data.decode("utf-8", errors="replace").lstrip('\ufeff')


def write_text(path: Path, text: str):
    # Always write UTF-8 without BOM to avoid mojibake in JavaFX/FXML.
    path.write_text(text.lstrip('\ufeff'), encoding="utf-8")


def backup(path: Path):
    if not path.exists():
        return
    target = BACKUP / path.relative_to(ROOT)
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(path, target)


def ensure_import(text: str, imp: str) -> str:
    if imp in text:
        return text
    m = re.search(r'(?m)^import\s+[^;]+;\s*$', text)
    if not m:
        return text
    insert_at = m.end()
    return text[:insert_at] + "\n" + imp + text[insert_at:]


def patch_main_template():
    path = ROOT / "client/src/main/java/com/auction/client/view/MainTemplate.fxml"
    if not path.exists():
        print("SKIP MainTemplate.fxml: not found")
        return
    backup(path)
    text = read_text(path)
    original = text

    # Fix visible Vietnamese text that was corrupted by earlier encoding saves.
    text = re.sub(r'<Label\s+text="[^"]*"\s+textFill="#604868"\s+style="-fx-font-weight:\s*bold;"\s*/>',
                  '<Label text="Lọc:" textFill="#604868" style="-fx-font-weight: bold;" />', text, count=1)
    text = re.sub(r'(<ComboBox\s+fx:id="cbCategory"[^>]*?)promptText="[^"]*"', r'\1promptText="Thể loại"', text, count=1)
    text = re.sub(r'(<ComboBox\s+fx:id="cbStatus"[^>]*?)promptText="[^"]*"', r'\1promptText="Trạng thái"', text, count=1)

    # Remove the old reset pill button so only one compact/full toggle button remains.
    text, removed = re.subn(
        r'\s*<Button\s+fx:id="btnResetFilter"[\s\S]*?</Button>\s*',
        '\n',
        text,
        count=1
    )

    # Make the right-side list/grid button the single toggle button.
    def repl_toggle_btn(match: re.Match) -> str:
        block = match.group(0)
        block = re.sub(r'onAction="#[^"]+"', 'onAction="#handleToggleProductView"', block, count=1)
        if 'fx:id="btnViewToggle"' not in block:
            block = block.replace('<Button ', '<Button fx:id="btnViewToggle" ', 1)
        # Compact circular button, no garbled text.
        block = re.sub(r'style="[^"]*"',
                       'style="-fx-background-color: #ffffff; -fx-border-color: #f2e8f2; -fx-border-radius: 999; -fx-background-radius: 999; -fx-cursor: hand; -fx-padding: 8px 10px; -fx-min-width: 44px; -fx-min-height: 44px;"',
                       block, count=1)
        # Use list icon by default.
        block = re.sub(r'text="&#x[a-fA-F0-9]+;"', 'text="&#xe8ef;"', block)
        return block

    text, changed_toggle = re.subn(
        r'<Button\s+(?=[^>]*onAction="#handleListView")[\s\S]*?</Button>',
        repl_toggle_btn,
        text,
        count=1
    )
    if changed_toggle == 0:
        # If already partially patched, normalize the fx:id button.
        text = text.replace('onAction="#handleListView"', 'onAction="#handleToggleProductView"')
        text = re.sub(r'<Button(?![^>]*fx:id="btnViewToggle")([^>]*onAction="#handleToggleProductView")',
                      r'<Button fx:id="btnViewToggle"\1', text, count=1)

    # Cleaner filter HBox spacing.
    text = re.sub(r'<HBox\s+spacing="10\.0"\s+alignment="CENTER">',
                  '<HBox spacing="12.0" alignment="CENTER" style="-fx-padding: 0 4px;">', text, count=1)

    # Keep product cards left-aligned even when filter returns only one item.
    text = text.replace('alignment="TOP_CENTER"', 'alignment="TOP_LEFT"')
    text = re.sub(r'(<FlowPane\s+fx:id="productContainer"[^>]*?)alignment="[^"]*"', r'\1alignment="TOP_LEFT"', text, count=1)
    text = re.sub(r'(<FlowPane\s+fx:id="productContainer"[^>]*?)style="[^"]*"',
                  r'\1style="-fx-padding: 10px 28px 24px 28px;"', text, count=1)

    if text != original:
        write_text(path, text)
        print(f"Patched MainTemplate.fxml (removed reset button: {bool(removed)}, toggle button: {bool(changed_toggle)})")
    else:
        print("No MainTemplate.fxml changes needed")


def patch_main_controller():
    path = ROOT / "client/src/main/java/com/auction/client/controller/MainController.java"
    if not path.exists():
        print("SKIP MainController.java: not found")
        return
    backup(path)
    text = read_text(path)
    original = text

    # Add Button import only if needed and not wildcard imported.
    if "btnViewToggle" not in text:
        if "javafx.scene.control.*" not in text and "javafx.scene.control.Button;" not in text:
            text = ensure_import(text, "import javafx.scene.control.Button;")
        # Insert field near other @FXML fields.
        m = re.search(r'(?m)(\s*@FXML\s+private\s+[^;]+;)', text)
        if m:
            insert_at = m.end()
            text = text[:insert_at] + "\n    @FXML\n    private Button btnViewToggle;" + text[insert_at:]
        else:
            # Fallback after class declaration.
            text = re.sub(r'(public\s+class\s+MainController\s*\{)', r'\1\n    @FXML\n    private Button btnViewToggle;', text, count=1)

    if "compactProductListMode" not in text:
        # Insert state field after class declaration or after btnViewToggle field.
        if "private Button btnViewToggle;" in text:
            text = text.replace("private Button btnViewToggle;", "private Button btnViewToggle;\n    private boolean compactProductListMode = false;", 1)
        else:
            text = re.sub(r'(public\s+class\s+MainController\s*\{)', r'\1\n    private boolean compactProductListMode = false;', text, count=1)

    has_handle_list = re.search(r'void\s+handleListView\s*\(', text) is not None
    has_reset_filter = re.search(r'void\s+handleResetFilter\s*\(', text) is not None
    has_reset_dashboard = re.search(r'void\s+handleResetDashboard\s*\(', text) is not None

    if not has_handle_list:
        print("WARN: Không thấy handleListView() trong MainController.java, không thể tự thêm toggle view đầy đủ.")

    if "handleToggleProductView" not in text and has_handle_list:
        reset_call = "handleResetFilter();" if has_reset_filter else ("handleResetDashboard();" if has_reset_dashboard else "fetchProductsData();")
        method = f'''

    @FXML
    private void handleToggleProductView() {{
        compactProductListMode = !compactProductListMode;
        if (compactProductListMode) {{
            handleListView();
            updateViewToggleButton(true);
        }} else {{
            {reset_call}
            updateViewToggleButton(false);
        }}
    }}

    private void updateViewToggleButton(boolean compactMode) {{
        if (btnViewToggle == null) {{
            return;
        }}
        btnViewToggle.setText(compactMode ? "▦" : "☰");
        btnViewToggle.setStyle("-fx-background-color: #ffffff;"
                + " -fx-border-color: #f2e8f2;"
                + " -fx-border-radius: 999;"
                + " -fx-background-radius: 999;"
                + " -fx-cursor: hand;"
                + " -fx-padding: 8px 12px;"
                + " -fx-font-size: 18px;"
                + " -fx-font-weight: 900;"
                + " -fx-text-fill: #604868;");
    }}
'''
        # Insert before final class closing brace.
        idx = text.rfind("}")
        if idx != -1:
            text = text[:idx] + method + text[idx:]

    # If code is setting productContainer alignment to center, force left.
    text = text.replace("productContainer.setAlignment(Pos.CENTER);", "productContainer.setAlignment(Pos.TOP_LEFT);")
    text = text.replace("productContainer.setAlignment(Pos.TOP_CENTER);", "productContainer.setAlignment(Pos.TOP_LEFT);")

    # Nudge plus buttons to the visual bottom-right when the builder uses an HBox bottom row.
    # This is intentionally conservative: it only normalizes obvious '+' button style blocks.
    text = re.sub(
        r'(new\s+Button\s*\(\s*"\+"\s*\)\s*;)',
        r'\1',
        text
    )
    # Prefer a compact, anchored-looking plus button if a + button style is present.
    text = re.sub(
        r'((?:Button\s+\w+\s*=\s*new\s+Button\("\+"\);[\s\S]{0,500}?\.setStyle\()"[^"]*"(\);)',
        r'\1"-fx-background-color: #ffc8ec; -fx-text-fill: white; -fx-font-size: 26px; -fx-font-weight: 400; -fx-background-radius: 999; -fx-min-width: 56px; -fx-min-height: 56px; -fx-pref-width: 56px; -fx-pref-height: 56px; -fx-cursor: hand;"\2',
        text,
        count=3
    )

    if text != original:
        write_text(path, text)
        print("Patched MainController.java (toggle view + left alignment helpers)")
    else:
        print("No MainController.java changes needed")


def patch_login_remember():
    path = ROOT / "client/src/main/java/com/auction/client/controller/LoginController.java"
    if not path.exists():
        print("SKIP LoginController.java: not found")
        return
    backup(path)
    text = read_text(path)
    original = text

    # Find fields dynamically.
    checkboxes = re.findall(r'@FXML\s*(?:private\s+)?CheckBox\s+(\w+)\s*;', text)
    textfields = re.findall(r'@FXML\s*(?:private\s+)?TextField\s+(\w+)\s*;', text)
    passfields = re.findall(r'@FXML\s*(?:private\s+)?PasswordField\s+(\w+)\s*;', text)

    remember = next((x for x in checkboxes if re.search(r'remember|keep|sign', x, re.I)), checkboxes[0] if checkboxes else None)
    email = next((x for x in textfields if re.search(r'email|mail', x, re.I)), textfields[0] if textfields else None)
    password = next((x for x in passfields if re.search(r'pass', x, re.I)), passfields[0] if passfields else None)

    if not (remember and email and password):
        print("WARN: Không nhận diện được đủ CheckBox/TextField/PasswordField trong LoginController.java, bỏ qua remember-me logic")
    else:
        text = ensure_import(text, "import java.util.prefs.Preferences;")
        if "REMEMBER_LOGIN_PREFS" not in text:
            # Add constants after class open.
            text = re.sub(
                r'(public\s+class\s+LoginController\s*\{)',
                r'\1\n    private static final Preferences REMEMBER_LOGIN_PREFS = Preferences.userNodeForPackage(LoginController.class);\n    private static final String PREF_REMEMBER = "rememberLogin";\n    private static final String PREF_EMAIL = "rememberEmail";\n    private static final String PREF_PASSWORD = "rememberPassword";',
                text,
                count=1
            )

        restore_method = f'''

    private void restoreRememberedLogin() {{
        if ({remember} == null || {email} == null || {password} == null) {{
            return;
        }}
        boolean rememberLogin = REMEMBER_LOGIN_PREFS.getBoolean(PREF_REMEMBER, false);
        {remember}.setSelected(rememberLogin);
        if (rememberLogin) {{
            {email}.setText(REMEMBER_LOGIN_PREFS.get(PREF_EMAIL, ""));
            {password}.setText(REMEMBER_LOGIN_PREFS.get(PREF_PASSWORD, ""));
        }}
    }}

    private void saveRememberedLoginChoice() {{
        if ({remember} == null || {email} == null || {password} == null) {{
            return;
        }}
        if ({remember}.isSelected()) {{
            REMEMBER_LOGIN_PREFS.putBoolean(PREF_REMEMBER, true);
            REMEMBER_LOGIN_PREFS.put(PREF_EMAIL, {email}.getText() == null ? "" : {email}.getText());
            REMEMBER_LOGIN_PREFS.put(PREF_PASSWORD, {password}.getText() == null ? "" : {password}.getText());
        }} else {{
            REMEMBER_LOGIN_PREFS.putBoolean(PREF_REMEMBER, false);
            REMEMBER_LOGIN_PREFS.remove(PREF_EMAIL);
            REMEMBER_LOGIN_PREFS.remove(PREF_PASSWORD);
        }}
    }}
'''
        if "restoreRememberedLogin()" not in text:
            idx = text.rfind("}")
            text = text[:idx] + restore_method + text[idx:]

        # Call restore in initialize if present.
        if re.search(r'void\s+initialize\s*\(', text) and "restoreRememberedLogin();" not in text:
            text = re.sub(
                r'(void\s+initialize\s*\([^)]*\)\s*\{)',
                r'\1\n        restoreRememberedLogin();',
                text,
                count=1
            )
        # If no initialize method exists, add one.
        elif "restoreRememberedLogin();" not in text:
            init = '''

    @FXML
    private void initialize() {
        restoreRememberedLogin();
    }
'''
            idx = text.rfind("}")
            text = text[:idx] + init + text[idx:]

        # Save the choice when login handler runs. This is intentionally light-touch.
        if "saveRememberedLoginChoice();" not in text:
            m = re.search(r'(private\s+void\s+handleLogin\s*\([^)]*\)\s*\{|public\s+void\s+handleLogin\s*\([^)]*\)\s*\{|@FXML\s*\n\s*private\s+void\s+handleLogin\s*\([^)]*\)\s*\{)', text)
            if m:
                text = text[:m.end()] + "\n        saveRememberedLoginChoice();" + text[m.end():]
            else:
                print("WARN: Không tìm thấy handleLogin(), remember-me chỉ tự load dữ liệu đã lưu")

    if text != original:
        write_text(path, text)
        print("Patched LoginController.java (remember checkbox now stores/restores login fields)")
    else:
        print("No LoginController.java changes needed")


def patch_login_fxml_spacing():
    path = ROOT / "client/src/main/java/com/auction/client/view/Login.fxml"
    if not path.exists():
        print("SKIP Login.fxml: not found")
        return
    backup(path)
    text = read_text(path)
    original = text
    # Keep the remember checkbox visually aligned with input fields.
    text = re.sub(
        r'(<CheckBox\b(?=[^>]*(?:Keep me signed|signed in|remember))[^>]*)(>)',
        lambda m: (m.group(1) if 'style=' in m.group(1) else m.group(1) + ' style="-fx-padding: 0 0 0 2; -fx-text-fill: #604868;"') + m.group(2),
        text,
        count=1,
        flags=re.IGNORECASE
    )
    if text != original:
        write_text(path, text)
        print("Patched Login.fxml (remember checkbox spacing)")


def remove_stale_target():
    target = ROOT / "client/target"
    if target.exists():
        shutil.rmtree(target, ignore_errors=True)
        print("Removed stale client/target")


def main():
    print(f"Project root: {ROOT}")
    print(f"Backup folder: {BACKUP}")
    patch_main_template()
    patch_main_controller()
    patch_login_remember()
    patch_login_fxml_spacing()
    remove_stale_target()
    print("DONE: Đã sửa toggle list/card, text filter, căn card, và remember login ở mức an toàn.")
    print("Chạy test: cd client && mvn test")


if __name__ == "__main__":
    main()
