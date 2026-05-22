from pathlib import Path
import shutil
import subprocess
from datetime import datetime

ROOT = Path.cwd()
SRC = ROOT / 'client/src/main/java/com/auction/client/controller/MainController.java'
PATCH_DIR = ROOT / 'resolve_maincontroller_live_layout_patch'
RESOLVED = PATCH_DIR / 'MainController.java'

if not SRC.exists():
    raise SystemExit(f'Không tìm thấy file cần sửa: {SRC}')
if not RESOLVED.exists():
    raise SystemExit(f'Không tìm thấy file resolved trong patch: {RESOLVED}')

backup_dir = ROOT / f'backup_resolve_maincontroller_live_layout_{datetime.now().strftime("%Y%m%d_%H%M%S")}'
backup_dir.mkdir(parents=True, exist_ok=True)
shutil.copy2(SRC, backup_dir / 'MainController.java')

text = RESOLVED.read_text(encoding='utf-8')
if '<<<<<<<' in text or '>>>>>>>' in text or '\n=======' in text:
    raise SystemExit('File resolved vẫn còn conflict marker, dừng lại để tránh phá project.')

# Save UTF-8 no BOM with CRLF, because project is on Windows/JavaFX.
SRC.write_text(text, encoding='utf-8', newline='\r\n')

# Remove stale compiled classes/resources so JavaFX does not load cached FXML/classes.
for target in [ROOT / 'client/target']:
    if target.exists():
        shutil.rmtree(target)
        print(f'Removed stale target: {target.relative_to(ROOT)}')

try:
    subprocess.run(['git', 'add', str(SRC.relative_to(ROOT)).replace('\\', '/')], cwd=ROOT, check=False)
except Exception:
    pass

print('DONE: Đã resolve MainController.java, sửa chữ lỗi và ổn định layout Live Auctions.')
print(f'Backup: {backup_dir}')
print('Tiếp theo chạy:')
print('  git status')
print('  cd client')
print('  & "D:\\IntelliJ IDEA 2025.3.3\\plugins\\maven\\lib\\maven3\\bin\\mvn.cmd" test')
