
from pathlib import Path
import re
import shutil
from datetime import datetime

ROOT = Path.cwd()
BACKUP = ROOT / f"backup_fix_bidder_toggle_filter_dropdown_v3_{datetime.now().strftime('%Y%m%d_%H%M%S')}"

MAIN_FXML = ROOT / "client/src/main/java/com/auction/client/view/MainTemplate.fxml"
MAIN_CONTROLLER = ROOT / "client/src/main/java/com/auction/client/controller/MainController.java"
STYLES = ROOT / "client/src/main/java/com/auction/client/view/styles.css"

def read(path):
    return path.read_text(encoding="utf-8-sig")

def write(path, text):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text.lstrip("\ufeff"), encoding="utf-8", newline="")

def backup(path):
    if path.exists():
        dest = BACKUP / path.relative_to(ROOT)
        dest.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(path, dest)

def remove_button_nodes(text, ids, actions):
    changed = True
    terms = [fr'fx:id="{re.escape(i)}"' for i in ids] + [fr'onAction="#{re.escape(a)}"' for a in actions]
    if not terms:
        return text
    look = "|".join(terms)
    while changed:
        changed = False
        pattern = re.compile(r'\s*<Button\b(?=[^>]*(?:' + look + r'))[^>]*/>', re.DOTALL)
        text2 = pattern.sub('', text)
        if text2 != text:
            changed = True
            text = text2
        pattern = re.compile(r'\s*<Button\b(?=[^>]*(?:' + look + r'))[^>]*>.*?</Button>', re.DOTALL)
        text2 = pattern.sub('', text)
        if text2 != text:
            changed = True
            text = text2
    return text

def patch_main_template():
    if not MAIN_FXML.exists():
        raise SystemExit(f"Không thấy {MAIN_FXML}")
    backup(MAIN_FXML)
    text = read(MAIN_FXML)

    mojibake_pairs = {
        "Lá»c:": "Lọc:",
        "Lá»?c:": "Lọc:",
        "Lá»‹c:": "Lọc:",
        "Lá»c:": "Lọc:",
        "Láº¡c:": "Lọc:",
        "Láº·c:": "Lọc:",
        "Lб»Ќc:": "Lọc:",
        "Lб»?c:": "Lọc:",
        "Äáº·t láº¡i": "Đặt lại",
        "Äặt lại": "Đặt lại",
    }
    for a,b in mojibake_pairs.items():
        text = text.replace(a,b)

    text = remove_button_nodes(
        text,
        ids=[
            "btnResetFilter", "btnGridView", "btnListView", "btnCardView",
            "btnToggleGridView", "btnToggleListView", "btnProductGridView", "btnProductListView"
        ],
        actions=[
            "handleResetFilter", "handleGridView", "handleListView", "handleCardView",
            "handleToggleGridView", "handleToggleListView"
        ]
    )

    m = re.search(r'(<HBox\b[^>]*fx:id="filterControlsBox"[^>]*>)(.*?)(</HBox>)', text, re.DOTALL)
    if m:
        open_tag, body, close_tag = m.group(1), m.group(2), m.group(3)

        body = re.sub(r'<Label\b([^>]*)text="[^"]*:"([^>]*)/>',
                      r'<Label\1text="Lọc:"\2/>', body, count=1)
        body = re.sub(r'(<Label\b(?=[^>]*text="Lọc:")(?![^>]*styleClass=)[^>]*)/>',
                      r'\1 styleClass="filter-label"/>', body, count=1)

        def add_combo_style(mm):
            tag = mm.group(0)
            if "styleClass=" in tag:
                if "filter-combo" not in tag:
                    tag = re.sub(r'styleClass="([^"]*)"', r'styleClass="\1 filter-combo"', tag, count=1)
                return tag
            return tag[:-2] + ' styleClass="filter-combo"/>'
        body = re.sub(r'<ComboBox\b[^>]*/>', add_combo_style, body)

        body = remove_button_nodes(
            body,
            ids=["btnToggleProductView", "btnResetFilter", "btnGridView", "btnListView", "btnCardView"],
            actions=["handleToggleProductView", "handleResetFilter", "handleListView", "handleGridView", "handleCardView"]
        )

        toggle = '''
                                <Button fx:id="btnToggleProductView"
                                        mnemonicParsing="false"
                                        onAction="#handleToggleProductView"
                                        styleClass="view-toggle-button"
                                        text="▦"/>'''
        new_body = body.rstrip() + toggle + "\n                            "
        text = text[:m.start()] + open_tag + new_body + close_tag + text[m.end():]
    else:
        if 'fx:id="btnToggleProductView"' not in text:
            print("WARN: Không tìm thấy filterControlsBox để chèn nút toggle; đã chỉ xóa nút thừa.")

    write(MAIN_FXML, text)
    print("Patched MainTemplate.fxml")

def find_class_insert_pos(text):
    m = re.search(r'public\s+class\s+MainController[^{]*\{', text)
    return m.end() if m else -1

def ensure_import(text, import_line):
    if import_line in text:
        return text
    package_m = re.search(r'(package\s+[^;]+;\s*)', text)
    imports = re.findall(r'import\s+[^;]+;', text)
    if imports:
        last = text.rfind(imports[-1]) + len(imports[-1])
        return text[:last] + "\n" + import_line + text[last:]
    elif package_m:
        pos = package_m.end()
        return text[:pos] + "\n\n" + import_line + text[pos:]
    return import_line + "\n" + text

def replace_method(text, method_name, new_method):
    m = re.search(r'(?m)^\s*(?:@FXML\s*)?(?:private|public|protected)\s+void\s+' + re.escape(method_name) + r'\s*\([^)]*\)\s*\{', text)
    if not m:
        return text, False
    start = m.start()
    brace = text.find("{", m.end()-1)
    depth = 0
    end = None
    for i in range(brace, len(text)):
        if text[i] == "{":
            depth += 1
        elif text[i] == "}":
            depth -= 1
            if depth == 0:
                end = i + 1
                break
    if end is None:
        return text, False
    return text[:start] + new_method.rstrip() + "\n" + text[end:], True

def patch_main_controller():
    if not MAIN_CONTROLLER.exists():
        raise SystemExit(f"Không thấy {MAIN_CONTROLLER}")
    backup(MAIN_CONTROLLER)
    text = read(MAIN_CONTROLLER)

    text = ensure_import(text, "import javafx.fxml.FXML;")
    text = ensure_import(text, "import javafx.event.ActionEvent;")
    text = ensure_import(text, "import javafx.scene.control.Button;")

    if "btnToggleProductView" not in text:
        class_pos = find_class_insert_pos(text)
        insert = "\n    @FXML\n    private Button btnToggleProductView;\n\n    private boolean compactProductListMode = false;\n"
        if class_pos != -1:
            text = text[:class_pos] + insert + text[class_pos:]
    else:
        if "compactProductListMode" not in text:
            class_pos = find_class_insert_pos(text)
            if class_pos != -1:
                text = text[:class_pos] + "\n    private boolean compactProductListMode = false;\n" + text[class_pos:]

    new_toggle = '''
    @FXML
    private void handleToggleProductView(ActionEvent event) {
        compactProductListMode = !compactProductListMode;

        if (compactProductListMode) {
            handleListView(event);
        } else {
            handleResetFilter(event);
        }

        updateViewToggleButton(compactProductListMode);
    }
'''
    text, replaced = replace_method(text, "handleToggleProductView", new_toggle)
    if not replaced:
        m = re.search(r'(?m)^\s*(?:@FXML\s*)?(?:private|public|protected)\s+void\s+handleListView\s*\(', text)
        pos = m.start() if m else text.rfind("}")
        text = text[:pos] + "\n" + new_toggle + "\n" + text[pos:]

    new_update = '''
    private void updateViewToggleButton(boolean compactMode) {
        if (btnToggleProductView == null) {
            return;
        }

        btnToggleProductView.getStyleClass().remove("view-toggle-button-active");
        if (compactMode) {
            btnToggleProductView.getStyleClass().add("view-toggle-button-active");
        }

        btnToggleProductView.setText("▦");
    }
'''
    text, replaced = replace_method(text, "updateViewToggleButton", new_update)
    if not replaced:
        pos = text.rfind("}")
        text = text[:pos] + "\n" + new_update + "\n" + text[pos:]

    text = text.replace("handleListView();", "handleListView(null);")
    text = text.replace("handleResetFilter();", "handleResetFilter(null);")
    text = text.replace("updateViewToggleButton(compactMode: true)", "updateViewToggleButton(true)")
    text = text.replace("updateViewToggleButton(compactMode: false)", "updateViewToggleButton(false)")

    write(MAIN_CONTROLLER, text)
    print("Patched MainController.java")

def patch_styles():
    if not STYLES.exists():
        raise SystemExit(f"Không thấy {STYLES}")
    backup(STYLES)
    text = read(STYLES)

    text = re.sub(r'/\* === Bidder dashboard filter/toggle polish v3 === \*/.*?/\* === End bidder dashboard filter/toggle polish v3 === \*/\s*',
                  '', text, flags=re.DOTALL)
    text = re.sub(r'/\* === Bidder dashboard filter/toggle polish.*?End bidder dashboard filter/toggle polish.*?\*/\s*',
                  '', text, flags=re.DOTALL)

    block = '''
/* === Bidder dashboard filter/toggle polish v3 === */
.filter-label {
    -fx-text-fill: #4b3154;
    -fx-font-size: 14px;
    -fx-font-weight: 800;
}

.filter-combo,
.combo-box.filter-combo {
    -fx-background-color: #fff8fd;
    -fx-border-color: #ffd8ea;
    -fx-border-width: 1.2px;
    -fx-background-radius: 18px;
    -fx-border-radius: 18px;
    -fx-padding: 6px 10px 6px 12px;
    -fx-font-size: 14px;
    -fx-text-fill: #25132a;
}

.filter-combo:hover,
.combo-box.filter-combo:hover {
    -fx-border-color: #ec77be;
    -fx-effect: dropshadow(gaussian, rgba(224, 64, 160, 0.16), 10, 0, 0, 3);
}

.filter-combo:focused,
.combo-box.filter-combo:focused {
    -fx-border-color: #e040a0;
    -fx-effect: dropshadow(gaussian, rgba(224, 64, 160, 0.20), 14, 0, 0, 4);
}

.filter-combo .arrow-button {
    -fx-background-color: transparent;
}

.filter-combo .arrow {
    -fx-background-color: #6f5875;
}

.combo-box-popup .list-view {
    -fx-background-color: white;
    -fx-border-color: #f4b7d9;
    -fx-border-width: 1.2px;
    -fx-background-radius: 14px;
    -fx-border-radius: 14px;
    -fx-padding: 6px;
    -fx-effect: dropshadow(gaussian, rgba(62, 24, 70, 0.18), 18, 0, 0, 8);
}

.combo-box-popup .list-view .list-cell {
    -fx-background-color: white;
    -fx-text-fill: #25132a;
    -fx-font-size: 14px;
    -fx-padding: 10px 14px;
    -fx-background-radius: 10px;
}

.combo-box-popup .list-view .list-cell:filled:hover {
    -fx-background-color: #ffeaf6;
    -fx-text-fill: #d92891;
}

.combo-box-popup .list-view .list-cell:filled:selected,
.combo-box-popup .list-view .list-cell:filled:selected:hover {
    -fx-background-color: #e040a0;
    -fx-text-fill: white;
}

.view-toggle-button {
    -fx-min-width: 48px;
    -fx-min-height: 48px;
    -fx-pref-width: 48px;
    -fx-pref-height: 48px;
    -fx-max-width: 48px;
    -fx-max-height: 48px;
    -fx-background-color: #ffffff;
    -fx-border-color: #f1dce9;
    -fx-border-width: 1.2px;
    -fx-background-radius: 999px;
    -fx-border-radius: 999px;
    -fx-text-fill: #8a6b94;
    -fx-font-size: 24px;
    -fx-font-weight: 900;
    -fx-cursor: hand;
    -fx-effect: dropshadow(gaussian, rgba(72, 35, 78, 0.07), 10, 0, 0, 3);
}

.view-toggle-button:hover {
    -fx-background-color: #fff3fb;
    -fx-border-color: #f4a6d4;
    -fx-text-fill: #e040a0;
    -fx-effect: dropshadow(gaussian, rgba(224, 64, 160, 0.18), 16, 0, 0, 5);
}

.view-toggle-button-active {
    -fx-background-color: linear-gradient(to bottom right, #e040a0, #f15bb5);
    -fx-border-color: #f9bde0;
    -fx-text-fill: white;
    -fx-effect: dropshadow(gaussian, rgba(224, 64, 160, 0.38), 18, 0, 0, 7);
}
/* === End bidder dashboard filter/toggle polish v3 === */
'''
    text = text.rstrip() + "\n\n" + block.strip() + "\n"
    write(STYLES, text)
    print("Patched styles.css")

def main():
    print(f"Project root: {ROOT}")
    BACKUP.mkdir(parents=True, exist_ok=True)
    print(f"Backup folder: {BACKUP}")
    patch_main_template()
    patch_main_controller()
    patch_styles()

    target = ROOT / "client/target"
    if target.exists():
        shutil.rmtree(target, ignore_errors=True)
        print("Removed stale client/target")

    print("\nDONE: Đã sửa Live Auctions chỉ còn 1 nút toggle + style combobox.")
    print("Chạy test:")
    print(r'  cd client')
    print(r'  & "D:\IntelliJ IDEA 2025.3.3\plugins\maven\lib\maven3\bin\mvn.cmd" test')

if __name__ == "__main__":
    main()
