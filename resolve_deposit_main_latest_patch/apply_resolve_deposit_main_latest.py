from pathlib import Path
import shutil
import subprocess
from datetime import datetime

ROOT = Path.cwd()
STAMP = datetime.now().strftime("%Y%m%d_%H%M%S")
BACKUP = ROOT / f"backup_resolve_deposit_main_latest_{STAMP}"

FILES = {
    "client/src/main/java/com/auction/client/controller/DepositController.java": Path(__file__).parent / "resolved" / "DepositController.java",
    "client/src/main/java/com/auction/client/controller/MainController.java": Path(__file__).parent / "resolved" / "MainController.java",
}

def write_utf8_no_bom(path: Path, text: str):
    path.write_text(text.lstrip("\ufeff"), encoding="utf-8", newline="\n")

def backup_file(path: Path):
    source = ROOT / path
    dest = BACKUP / path
    dest.parent.mkdir(parents=True, exist_ok=True)
    if source.exists():
        shutil.copy2(source, dest)

def fix_main_template():
    path = ROOT / "client/src/main/java/com/auction/client/view/MainTemplate.fxml"
    if not path.exists():
        return False

    backup_file(Path("client/src/main/java/com/auction/client/view/MainTemplate.fxml"))
    text = path.read_text(encoding="utf-8-sig", errors="replace")
    lines = text.splitlines()
    new_lines = []
    fixed = False
    i = 0

    button_block = [
        '                    <Button fx:id="btnToggleProductView"',
        '                            mnemonicParsing="false"',
        '                            onAction="#handleToggleProductView"',
        '                            text="▦"',
        '                            alignment="CENTER"',
        '                            contentDisplay="CENTER"',
        '                            textAlignment="CENTER"',
        '                            styleClass="view-toggle-button" />'
    ]

    while i < len(lines):
        line = lines[i]
        if 'fx:id="btnToggleProductView"' in line:
            while i < len(lines):
                current = lines[i]
                if '</Button>' in current:
                    i += 1
                    break
                if '/>' in current:
                    i += 1
                    break
                if '</HBox>' in current:
                    break
                i += 1
            new_lines.extend(button_block)
            fixed = True
            continue
        new_lines.append(line)
        i += 1

    if not fixed:
        inserted = False
        new_lines = []
        for line in lines:
            new_lines.append(line)
            if 'fx:id="cbStatus"' in line and not inserted:
                new_lines.extend(button_block)
                inserted = True
                fixed = True

    if fixed:
        write_utf8_no_bom(path, "\n".join(new_lines) + "\n")
    return fixed

def ensure_styles():
    path = ROOT / "client/src/main/java/com/auction/client/view/styles.css"
    if not path.exists():
        return False

    backup_file(Path("client/src/main/java/com/auction/client/view/styles.css"))
    text = path.read_text(encoding="utf-8-sig", errors="replace")
    block = """

/* Resolve patch: one-button card/list toggle */
.view-toggle-button {
    -fx-min-width: 48px;
    -fx-pref-width: 48px;
    -fx-max-width: 48px;
    -fx-min-height: 48px;
    -fx-pref-height: 48px;
    -fx-max-height: 48px;
    -fx-background-color: #ffffff;
    -fx-background-radius: 24px;
    -fx-border-color: #f3c6e4;
    -fx-border-width: 1.2px;
    -fx-border-radius: 24px;
    -fx-text-fill: #6d4b75;
    -fx-font-size: 19px;
    -fx-font-weight: 900;
    -fx-alignment: center;
    -fx-content-display: center;
    -fx-text-alignment: center;
    -fx-padding: 0;
    -fx-cursor: hand;
    -fx-effect: dropshadow(three-pass-box, rgba(224, 64, 160, 0.10), 12, 0, 0, 3);
}

.view-toggle-button-active {
    -fx-background-color: #fde7f6;
    -fx-border-color: #e040a0;
    -fx-text-fill: #e040a0;
    -fx-effect: dropshadow(three-pass-box, rgba(224, 64, 160, 0.26), 18, 0, 0, 5);
}
"""
    if "Resolve patch: one-button card/list toggle" not in text:
        write_utf8_no_bom(path, text.rstrip() + block + "\n")
    return True

def main():
    print(f"Project root: {ROOT}")
    print(f"Backup folder: {BACKUP}")

    for rel, src in FILES.items():
        target = ROOT / rel
        if not target.exists():
            raise FileNotFoundError(f"Không tìm thấy file cần sửa: {target}")
        backup_file(Path(rel))
        content = src.read_text(encoding="utf-8")
        write_utf8_no_bom(target, content)
        print(f"Resolved: {rel}")

    fxml_fixed = fix_main_template()
    css_fixed = ensure_styles()
    print(f"Checked MainTemplate.fxml toggle button: {fxml_fixed}")
    print(f"Checked styles.css toggle style: {css_fixed}")

    target_dir = ROOT / "client" / "target"
    if target_dir.exists():
        shutil.rmtree(target_dir, ignore_errors=True)
        print(f"Removed stale target: {target_dir}")

    try:
        subprocess.run([
            "git", "add",
            "client/src/main/java/com/auction/client/controller/DepositController.java",
            "client/src/main/java/com/auction/client/controller/MainController.java",
            "client/src/main/java/com/auction/client/view/MainTemplate.fxml",
            "client/src/main/java/com/auction/client/view/styles.css",
        ], check=False)
        print("DONE: Đã resolve conflict và git add các file cần thiết.")
    except Exception as exc:
        print(f"WARNING: Không tự git add được: {exc}")

    print("\nBước tiếp theo:")
    print("  git status")
    print("  cd client")
    print('  & "D:\\IntelliJ IDEA 2025.3.3\\plugins\\maven\\lib\\maven3\\bin\\mvn.cmd" test')
    print("Nếu test xanh:")
    print("  cd D:\\CodeJava\\Acution_System")
    print('  git commit -m "Hòa cập nhật main với giao diện nạp tiền và bidder"')
    print("  git push origin develop/minh-seller-admin")

if __name__ == "__main__":
    main()
