from pathlib import Path
from datetime import datetime
import shutil
import re

ROOT = Path.cwd()
file = ROOT / "client/src/test/java/com/auction/client/controller/ForgotPasswordControllerTest.java"
if not file.exists():
    raise SystemExit(f"Không tìm thấy file: {file}")

backup = ROOT / f"backup_fix_forgot_alert_test_v2_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
backup.mkdir(parents=True, exist_ok=True)
shutil.copy2(file, backup / file.name)

text = file.read_text(encoding="utf-8")

# Add Window import for robust lookup through JavaFX windows.
if "import javafx.stage.Window;" not in text:
    text = text.replace("import javafx.stage.Stage;\n", "import javafx.stage.Stage;\nimport javafx.stage.Window;\n")

old_method = re.compile(
    r"    private DialogPane waitForDialogPane\(FxRobot robot\) \{.*?\n    \}\n\n    private void sleepBriefly\(\)",
    re.DOTALL,
)
new_method = '''    private DialogPane waitForDialogPane(FxRobot robot) {
        long deadline = System.currentTimeMillis() + 12000;

        while (System.currentTimeMillis() < deadline) {
            WaitForAsyncUtils.waitForFxEvents();

            var directDialog = robot.lookup(".dialog-pane").tryQuery();
            if (directDialog.isPresent() && directDialog.get() instanceof DialogPane dialogPane) {
                return dialogPane;
            }

            for (Window window : Window.getWindows()) {
                if (window != null && window.isShowing() && window.getScene() != null) {
                    var root = window.getScene().getRoot();
                    if (root != null) {
                        var node = root.lookup(".dialog-pane");
                        if (node instanceof DialogPane dialogPane) {
                            return dialogPane;
                        }
                    }
                }
            }

            sleepBriefly();
        }

        throw new AssertionError("Không tìm thấy Alert trong 12 giây.");
    }

    private void sleepBriefly()'''

text2, count = old_method.subn(new_method, text, count=1)
if count != 1:
    raise SystemExit("Không thay được waitForDialogPane. File test có thể đã khác cấu trúc.")
text = text2

file.write_text(text, encoding="utf-8")

# Clear stale build output.
target = ROOT / "client/target"
if target.exists():
    shutil.rmtree(target)

print("Patched: ForgotPasswordControllerTest.java")
print(f"Backup folder: {backup}")
print("DONE. Chạy lại: mvn -Dtest=ForgotPasswordControllerTest test")
