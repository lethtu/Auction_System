from pathlib import Path
import shutil
import subprocess
import sys
from datetime import datetime

PROJECT_ROOT = Path.cwd()
PATCH_DIR = Path(__file__).resolve().parent
RESOLVED_DIR = PATCH_DIR / "resolved"

FILES = [
    Path("client/src/main/java/com/auction/client/controller/MainController.java"),
    Path("client/src/main/java/com/auction/client/view/styles.css"),
]

def read_text(path: Path) -> str:
    raw = path.read_bytes()
    if raw.startswith(b"\xef\xbb\xbf"):
        return raw.decode("utf-8-sig")
    return raw.decode("utf-8", errors="replace")

def write_text_no_bom(path: Path, text: str) -> None:
    path.write_text(text.lstrip("\ufeff"), encoding="utf-8", newline="\n")

def ensure_project_root() -> None:
    if not (PROJECT_ROOT / "client").exists() or not (PROJECT_ROOT / "server").exists():
        raise SystemExit("Hãy chạy script ở thư mục root D:\\CodeJava\\Acution_System")

def backup_files() -> Path:
    backup_dir = PROJECT_ROOT / f"backup_resolve_ui_main_styles_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
    backup_dir.mkdir(parents=True, exist_ok=True)
    for rel in FILES:
        src = PROJECT_ROOT / rel
        if src.exists():
            dest = backup_dir / rel.name
            shutil.copy2(src, dest)
    return backup_dir

def copy_resolved_files() -> None:
    for rel in FILES:
        src = RESOLVED_DIR / rel
        dst = PROJECT_ROOT / rel
        if not src.exists():
            raise SystemExit(f"Thiếu file resolved trong patch: {src}")
        text = read_text(src)
        if "<<<<<<<" in text or "=======" in text and "origin/main" in text or ">>>>>>>" in text:
            raise SystemExit(f"File resolved vẫn còn conflict marker: {rel}")
        write_text_no_bom(dst, text)
        print(f"Resolved: {rel}")

def git_add_files() -> None:
    cmd = ["git", "add"] + [str(rel).replace("\\", "/") for rel in FILES]
    subprocess.run(cmd, cwd=PROJECT_ROOT, check=True)
    print("DONE: Đã git add 2 file conflict.")

def main() -> None:
    ensure_project_root()
    backup_dir = backup_files()
    print(f"Project root: {PROJECT_ROOT}")
    print(f"Backup folder: {backup_dir}")
    copy_resolved_files()
    git_add_files()
    print()
    print("Bước tiếp theo:")
    print("  git status")
    print("  cd client")
    print('  & "D:\\IntelliJ IDEA 2025.3.3\\plugins\\maven\\lib\\maven3\\bin\\mvn.cmd" test')
    print("Nếu test xanh:")
    print("  cd D:\\CodeJava\\Acution_System")
    print('  git commit -m "Hòa cập nhật main với giao diện bidder"')
    print("  git push origin develop/minh-seller-admin")

if __name__ == "__main__":
    main()
