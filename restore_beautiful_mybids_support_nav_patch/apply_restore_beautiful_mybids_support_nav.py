from pathlib import Path
from datetime import datetime
import shutil

ROOT = Path.cwd()
file_path = ROOT / "client/src/main/java/com/auction/client/controller/SidebarController.java"
if not file_path.exists():
    raise SystemExit(f"Không tìm thấy file: {file_path}")

backup_dir = ROOT / f"backup_restore_beautiful_mybids_support_nav_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
backup_dir.mkdir(parents=True, exist_ok=True)
shutil.copy2(file_path, backup_dir / "SidebarController.java")

text = file_path.read_text(encoding="utf-8")

def replace_method(src: str, signature: str, new_method: str) -> str:
    start = src.find(signature)
    if start == -1:
        raise SystemExit(f"Không tìm thấy method: {signature}")
    brace_start = src.find("{", start)
    if brace_start == -1:
        raise SystemExit(f"Không tìm thấy '{{' cho method: {signature}")
    depth = 0
    i = brace_start
    while i < len(src):
        ch = src[i]
        if ch == "{":
            depth += 1
        elif ch == "}":
            depth -= 1
            if depth == 0:
                end = i + 1
                break
        i += 1
    else:
        raise SystemExit(f"Không tìm thấy '}}' kết thúc method: {signature}")
    return src[:start] + new_method.rstrip() + src[end:]

new_handle_mybids = '''public void handleMyBids(ActionEvent event) {
        autoCollapse();
        setActiveMyBids();
        try {
            Stage stage = resolveStage(event);
            boolean wasMaximized = stage != null && stage.isMaximized();
            int currentWidth = stage == null ? 1280 : Math.max(1280, (int) Math.round(stage.getWidth()));
            int currentHeight = stage == null ? 800 : Math.max(800, (int) Math.round(stage.getHeight()));

            if (onBeforeNavigate != null) onBeforeNavigate.run();
            SceneSwitcher.switchScene(event, "MyBids.fxml", currentWidth, currentHeight);

            if (stage != null && wasMaximized) {
                Platform.runLater(() -> stage.setMaximized(true));
            }
        } catch (IOException e) {
            logger.error("Lỗi chuyển cảnh sang MyBids.fxml: ", e);
            showInfo("My Bids", "Không thể mở màn My Bids. Vui lòng thử lại.");
        }
    }'''

new_handle_support = '''public void handleSupport(ActionEvent event) {
        autoCollapse();
        setActiveSupport();
        try {
            Stage stage = resolveStage(event);
            boolean wasMaximized = stage != null && stage.isMaximized();
            int currentWidth = stage == null ? 1280 : Math.max(1280, (int) Math.round(stage.getWidth()));
            int currentHeight = stage == null ? 800 : Math.max(800, (int) Math.round(stage.getHeight()));

            if (onBeforeNavigate != null) onBeforeNavigate.run();
            SceneSwitcher.switchScene(event, "Support.fxml", currentWidth, currentHeight);

            if (stage != null && wasMaximized) {
                Platform.runLater(() -> stage.setMaximized(true));
            }
        } catch (IOException e) {
            logger.error("Lỗi chuyển cảnh sang Support.fxml: ", e);
            showInfo("Support", "Không thể mở màn hỗ trợ. Vui lòng thử lại.");
        }
    }'''

text = replace_method(text, "public void handleMyBids(ActionEvent event)", new_handle_mybids)
text = replace_method(text, "public void handleSupport(ActionEvent event)", new_handle_support)

file_path.write_text(text, encoding="utf-8")

target = ROOT / "client/target"
if target.exists():
    shutil.rmtree(target)
    print("Removed stale target: client/target")

print("Patched: client/src/main/java/com/auction/client/controller/SidebarController.java")
print(f"Backup folder: {backup_dir}")
print("DONE: Support và My Bids giờ điều hướng trực tiếp sang Support.fxml/MyBids.fxml đẹp.")
