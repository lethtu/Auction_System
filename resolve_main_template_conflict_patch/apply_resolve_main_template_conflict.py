from pathlib import Path
import shutil
import subprocess
from datetime import datetime

project = Path.cwd()
rel = Path("client/src/main/java/com/auction/client/view/MainTemplate.fxml")
target = project / rel
source = Path(__file__).resolve().parent / "MainTemplate.fxml"

if not target.exists():
    raise SystemExit(f"Không tìm thấy file cần sửa: {target}")

backup_dir = project / f"backup_resolve_main_template_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
backup_dir.mkdir(parents=True, exist_ok=True)
shutil.copy2(target, backup_dir / "MainTemplate.fxml.conflict_backup")

text = source.read_text(encoding="utf-8")
for marker in ("<<<<<<<", "=======", ">>>>>>>"):
    if marker in text:
        raise SystemExit("File resolved vẫn còn conflict marker, dừng lại.")

target.write_text(text, encoding="utf-8", newline="")

subprocess.run(["git", "add", str(rel).replace("\\", "/")], check=True)

print("DONE: Đã resolve MainTemplate.fxml và git add file này.")
print(f"Backup: {backup_dir}")
print("Tiếp theo chạy:")
print("  git status")
print("  cd client")
print("  & \"D:\\IntelliJ IDEA 2025.3.3\\plugins\\maven\\lib\\maven3\\bin\\mvn.cmd\" test")
print("Nếu test xanh, quay lại root rồi git commit để kết thúc merge.")
