from pathlib import Path
from datetime import datetime
import re
import shutil

ROOT = Path.cwd()
login_fxml = ROOT / "client/src/main/java/com/auction/client/view/Login.fxml"
login_controller = ROOT / "client/src/main/java/com/auction/client/controller/LoginController.java"

if not login_fxml.exists():
    raise SystemExit(f"Không tìm thấy {login_fxml}")

backup_dir = ROOT / f"backup_fix_login_checkbox_fxml_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
backup_dir.mkdir(exist_ok=True)
shutil.copy2(login_fxml, backup_dir / "Login.fxml")

def read_text(path: Path) -> str:
    try:
        return path.read_text(encoding="utf-8-sig")
    except UnicodeDecodeError:
        return path.read_text(encoding="utf-8", errors="replace")

fxml_text = read_text(login_fxml)
controller_text = read_text(login_controller) if login_controller.exists() else ""

# Prefer the CheckBox field already declared in LoginController so fx:id keeps matching controller code.
checkbox_ids = re.findall(r"(?:@FXML\s+)?(?:private|public|protected)?\s*CheckBox\s+([A-Za-z_][A-Za-z0-9_]*)\s*;", controller_text)
preferred = [x for x in checkbox_ids if re.search(r"remember|keep|sign|signed|login", x, re.I)]
fx_id = preferred[0] if preferred else (checkbox_ids[0] if checkbox_ids else "rememberMeCheckBox")

lines = fxml_text.splitlines()
changed = False
new_lines = []

for line in lines:
    if "<CheckBox" in line and re.search(r"Keep me signed|Remember me|Ghi nhớ", line, re.I):
        indent = re.match(r"\s*", line).group(0)
        style_class_match = re.search(r'styleClass="([^"]+)"', line)
        style_class = style_class_match.group(1) if style_class_match else "remember-checkbox"
        new_line = (
            f'{indent}<CheckBox fx:id="{fx_id}" mnemonicParsing="false" '
            f'text="Keep me signed in on this device" styleClass="{style_class}" />'
        )
        new_lines.append(new_line)
        changed = True
    else:
        new_lines.append(line)

if not changed:
    # Fallback: find any CheckBox line near the original broken area and repair the first likely one.
    for i, line in enumerate(new_lines):
        if "<CheckBox" in line:
            indent = re.match(r"\s*", line).group(0)
            new_lines[i] = (
                f'{indent}<CheckBox fx:id="{fx_id}" mnemonicParsing="false" '
                f'text="Keep me signed in on this device" styleClass="remember-checkbox" />'
            )
            changed = True
            break

if not changed:
    raise SystemExit("Không tìm thấy dòng CheckBox trong Login.fxml để sửa.")

fixed = "\n".join(new_lines) + "\n"

# Remove BOM and save UTF-8 without BOM.
fixed = fixed.lstrip("\ufeff")
login_fxml.write_text(fixed, encoding="utf-8")

# Remove stale compiled resources.
for target in [ROOT / "client/target"]:
    if target.exists():
        shutil.rmtree(target, ignore_errors=True)

print(f"Project root: {ROOT}")
print(f"Backup folder: {backup_dir}")
print(f"Fixed Login.fxml CheckBox using fx:id={fx_id}")
print("DONE: Đã sửa lỗi FXML CheckBox malformed và xóa client/target.")
