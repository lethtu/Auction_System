
from pathlib import Path
from datetime import datetime
import re
import shutil

ROOT = Path.cwd()
FXML = ROOT / "client/src/main/java/com/auction/client/view/MainTemplate.fxml"
CSS = ROOT / "client/src/main/java/com/auction/client/view/styles.css"

def backup_file(path: Path, backup_dir: Path):
    if path.exists():
        dest = backup_dir / path.relative_to(ROOT)
        dest.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(path, dest)

def write_text_no_bom(path: Path, text: str):
    path.write_text(text.lstrip("\ufeff"), encoding="utf-8")

def ensure_attr(tag: str, attr: str, value: str) -> str:
    pattern = rf'\s{re.escape(attr)}="[^"]*"'
    repl = f' {attr}="{value}"'
    if re.search(pattern, tag):
        return re.sub(pattern, repl, tag, count=1)
    if tag.rstrip().endswith("/>"):
        return re.sub(r'\s*/>\s*$', repl + " />", tag, count=1)
    return re.sub(r'\s*>\s*$', repl + ">", tag, count=1)

def ensure_style_class(tag: str, cls: str) -> str:
    m = re.search(r'styleClass="([^"]*)"', tag)
    if m:
        classes = m.group(1).split()
        if cls not in classes:
            classes.append(cls)
        return tag[:m.start(1)] + " ".join(classes) + tag[m.end(1):]
    return ensure_attr(tag, "styleClass", cls)

def patch_fxml():
    if not FXML.exists():
        raise SystemExit(f"Không tìm thấy {FXML}")
    text = FXML.read_text(encoding="utf-8-sig")

    pattern = re.compile(r'<Button\b(?=[^>]*fx:id="btnViewToggle")[^>]*>', re.DOTALL)
    m = pattern.search(text)
    if not m:
        raise SystemExit("Không tìm thấy Button fx:id=\"btnViewToggle\" trong MainTemplate.fxml")

    tag = m.group(0)
    new_tag = tag

    new_tag = ensure_attr(new_tag, "alignment", "CENTER")
    new_tag = ensure_attr(new_tag, "contentDisplay", "CENTER")
    new_tag = ensure_attr(new_tag, "textAlignment", "CENTER")
    new_tag = ensure_attr(new_tag, "mnemonicParsing", "false")
    new_tag = ensure_style_class(new_tag, "view-toggle-button")
    new_tag = ensure_style_class(new_tag, "icon-only-toggle-button")

    if re.search(r'\stext="[^"]*"', new_tag):
        new_tag = re.sub(r'\stext="[^"]*"', ' text="▦"', new_tag, count=1)
    else:
        new_tag = ensure_attr(new_tag, "text", "▦")

    text = text[:m.start()] + new_tag + text[m.end():]
    write_text_no_bom(FXML, text)
    print("Patched MainTemplate.fxml: centered btnViewToggle")

def patch_css():
    if not CSS.exists():
        raise SystemExit(f"Không tìm thấy {CSS}")
    css = CSS.read_text(encoding="utf-8-sig")

    block = """

/* Final fix: center the single Live Auctions card/list toggle button */
.view-toggle-button,
.icon-only-toggle-button {
    -fx-min-width: 54px;
    -fx-pref-width: 54px;
    -fx-max-width: 54px;
    -fx-min-height: 54px;
    -fx-pref-height: 54px;
    -fx-max-height: 54px;
    -fx-padding: 0;
    -fx-alignment: center;
    -fx-content-display: center;
    -fx-text-alignment: center;
    -fx-background-radius: 999px;
    -fx-border-radius: 999px;
    -fx-background-color: #ffffff;
    -fx-border-color: #f2d9ec;
    -fx-border-width: 1.2px;
    -fx-text-fill: #8c6d97;
    -fx-font-size: 22px;
    -fx-font-weight: 800;
}

.view-toggle-button:hover,
.icon-only-toggle-button:hover {
    -fx-background-color: #fff6fc;
    -fx-border-color: #f3a8d8;
    -fx-effect: dropshadow(gaussian, rgba(224, 64, 160, 0.18), 14, 0, 0, 5);
}

.view-toggle-button:pressed,
.icon-only-toggle-button:pressed {
    -fx-scale-x: 0.96;
    -fx-scale-y: 0.96;
}

.view-toggle-button-active,
.icon-only-toggle-button-active {
    -fx-background-color: #e040a0;
    -fx-border-color: #e040a0;
    -fx-text-fill: white;
    -fx-effect: dropshadow(gaussian, rgba(224, 64, 160, 0.30), 16, 0, 0, 6);
}

.view-toggle-button .text,
.icon-only-toggle-button .text {
    -fx-translate-x: 0;
    -fx-translate-y: -1px;
    -fx-text-alignment: center;
}
"""
    marker = "/* Final fix: center the single Live Auctions card/list toggle button */"
    if marker in css:
        css = css[:css.index(marker)].rstrip() + "\n" + block
    else:
        css = css.rstrip() + "\n" + block

    write_text_no_bom(CSS, css)
    print("Patched styles.css: centered toggle button CSS")

def main():
    if not FXML.exists() or not CSS.exists():
        raise SystemExit("Hãy chạy script ở root project D:\\CodeJava\\Acution_System")

    backup_dir = ROOT / f"backup_fix_view_toggle_center_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
    backup_dir.mkdir(parents=True, exist_ok=True)
    backup_file(FXML, backup_dir)
    backup_file(CSS, backup_dir)

    patch_fxml()
    patch_css()

    target = ROOT / "client/target"
    if target.exists():
        shutil.rmtree(target, ignore_errors=True)
        print("Removed client/target")

    print("DONE: Đã căn giữa nút toggle card/list.")

if __name__ == "__main__":
    main()
