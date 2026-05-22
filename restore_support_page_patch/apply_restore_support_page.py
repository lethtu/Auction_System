from pathlib import Path
from datetime import datetime
import re
import shutil
import sys

ROOT = Path.cwd()
sidebar = ROOT / "client/src/main/java/com/auction/client/controller/SidebarController.java"
support_fxml = ROOT / "client/src/main/java/com/auction/client/view/Support.fxml"
target = ROOT / "client/target"

if not sidebar.exists():
    raise SystemExit(f"ERROR: Không tìm thấy file: {sidebar}")
if not support_fxml.exists():
    raise SystemExit(f"ERROR: Không tìm thấy Support.fxml. Không apply để tránh làm mất tính năng support.")

backup = ROOT / f"backup_restore_support_page_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
backup.mkdir(parents=True, exist_ok=True)
shutil.copy2(sidebar, backup / "SidebarController.java")

text = sidebar.read_text(encoding="utf-8")

new_method = '''    @FXML
    public void handleSupport(ActionEvent event) {
        autoCollapse();
        setActiveButton(btnSupport);

        try {
            Stage stage = resolveStage(event);
            boolean wasMaximized = stage != null && stage.isMaximized();
            double currentWidth = stage != null ? stage.getWidth() : 1280;
            double currentHeight = stage != null ? stage.getHeight() : 800;

            if (onBeforeNavigate != null) {
                onBeforeNavigate.run();
            }

            SceneSwitcher.switchScene(event, "Support.fxml", currentWidth, currentHeight);

            if (stage != null && wasMaximized) {
                Platform.runLater(() -> stage.setMaximized(true));
            }
        } catch (IOException e) {
            logger.error("Lỗi chuyển cảnh sang Support: ", e);
            showInfo("Support", "Không thể mở trang hỗ trợ. Vui lòng thử lại.");
        }
    }
'''

pattern = re.compile(r"    @FXML\s+public void handleSupport\(ActionEvent event\) \{.*?\n    \}\s*\n\s*private Stage resolveStage", re.DOTALL)
match = pattern.search(text)
if not match:
    raise SystemExit("ERROR: Không tìm thấy đúng method handleSupport để thay. Chưa sửa gì.")

replacement = new_method + "\n    private Stage resolveStage"
new_text = text[:match.start()] + replacement + text[match.end():]

if new_text == text:
    raise SystemExit("ERROR: Nội dung không thay đổi. Chưa sửa gì.")

# Optional cleanup: remove imports that are no longer needed by SidebarController support popup.
# Java allows unused imports, but these two imports become unused after restoring Support.fxml navigation.
new_text = new_text.replace("import javafx.scene.control.TextInputDialog;\n", "")
new_text = new_text.replace("import java.util.Optional;\n", "")

sidebar.write_text(new_text, encoding="utf-8")

if target.exists():
    shutil.rmtree(target, ignore_errors=True)
    print("Removed stale target: client/target")

print("DONE: Đã khôi phục nút Support mở trang Support.fxml đẹp thay vì TextInputDialog cũ.")
print(f"Backup: {backup}")
print("File changed:")
print(" - client/src/main/java/com/auction/client/controller/SidebarController.java")
