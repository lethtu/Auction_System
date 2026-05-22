from pathlib import Path
from datetime import datetime
import shutil
import re

ROOT = Path.cwd()
file = ROOT / "client/src/main/java/com/auction/client/controller/SidebarController.java"
target = ROOT / "client/target"

if not file.exists():
    raise SystemExit(f"ERROR: Không tìm thấy {file}")

backup = ROOT / f"backup_support_scene_size_compile_fix_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
backup.mkdir(parents=True, exist_ok=True)
shutil.copy2(file, backup / "SidebarController.java")

text = file.read_text(encoding="utf-8")
old = text

# Fix compile error: SceneSwitcher.switchScene expects Integer/int sizes, not double.
text = text.replace(
    "double currentWidth = stage != null ? stage.getWidth() : 1280;",
    "int currentWidth = stage != null ? (int) Math.round(stage.getWidth()) : 1280;"
)
text = text.replace(
    "double currentHeight = stage != null ? stage.getHeight() : 800;",
    "int currentHeight = stage != null ? (int) Math.round(stage.getHeight()) : 800;"
)

# If a previous patch left doubles and direct call unchanged, harden the call too.
text = text.replace(
    'SceneSwitcher.switchScene(event, "Support.fxml", currentWidth, currentHeight);',
    'SceneSwitcher.switchScene(event, "Support.fxml", currentWidth, currentHeight);'
)

# Last-resort regex for variants with double declarations inside handleSupport.
text = re.sub(
    r"double\s+currentWidth\s*=\s*stage\s*!=\s*null\s*\?\s*stage\.getWidth\(\)\s*:\s*1280\s*;",
    "int currentWidth = stage != null ? (int) Math.round(stage.getWidth()) : 1280;",
    text
)
text = re.sub(
    r"double\s+currentHeight\s*=\s*stage\s*!=\s*null\s*\?\s*stage\.getHeight\(\)\s*:\s*800\s*;",
    "int currentHeight = stage != null ? (int) Math.round(stage.getHeight()) : 800;",
    text
)

if text == old:
    raise SystemExit("ERROR: Không tìm thấy đoạn currentWidth/currentHeight cần sửa. Chưa ghi file.")

file.write_text(text, encoding="utf-8")

if target.exists():
    shutil.rmtree(target, ignore_errors=True)
    print("Removed stale target: client/target")

print("DONE: Đã sửa lỗi double -> Integer trong SidebarController.handleSupport.")
print(f"Backup: {backup}")
print("Next:")
print(r'  cd client')
print(r'  & "D:\IntelliJ IDEA 2025.3.3\plugins\maven\lib\maven3\bin\mvn.cmd" test')
