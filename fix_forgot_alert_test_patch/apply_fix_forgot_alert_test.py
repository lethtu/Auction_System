from pathlib import Path
from datetime import datetime
import shutil
import re

ROOT = Path.cwd()
STAMP = datetime.now().strftime('%Y%m%d_%H%M%S')
BACKUP = ROOT / f"backup_fix_forgot_alert_test_{STAMP}"

TEST = ROOT / "client/src/test/java/com/auction/client/controller/ForgotPasswordControllerTest.java"
if not TEST.exists():
    raise SystemExit(f"Không tìm thấy file test: {TEST}")

BACKUP.mkdir(parents=True, exist_ok=True)
backup_file = BACKUP / TEST.relative_to(ROOT)
backup_file.parent.mkdir(parents=True, exist_ok=True)
shutil.copy2(TEST, backup_file)

text = TEST.read_text(encoding="utf-8")

if "import javafx.scene.Node;" not in text:
    text = text.replace("import javafx.scene.Parent;\n", "import javafx.scene.Parent;\nimport javafx.scene.Node;\n")
if "import javafx.stage.Window;" not in text:
    text = text.replace("import javafx.stage.Stage;\n", "import javafx.stage.Stage;\nimport javafx.stage.Window;\n")

new_method = """    private DialogPane waitForDialogPane(FxRobot robot) {
        long deadline = System.currentTimeMillis() + 12000;

        while (System.currentTimeMillis() < deadline) {
            WaitForAsyncUtils.waitForFxEvents();

            var node = robot.lookup(".dialog-pane").tryQuery();
            if (node.isPresent() && node.get() instanceof DialogPane dialogPane) {
                return dialogPane;
            }

            for (Window window : Window.getWindows()) {
                if (window != null && window.isShowing() && window.getScene() != null) {
                    Node root = window.getScene().getRoot();
                    if (root != null) {
                        Node dialogNode = root.lookup(".dialog-pane");
                        if (dialogNode instanceof DialogPane dialogPane) {
                            return dialogPane;
                        }
                    }
                }
            }

            sleepBriefly();
        }

        throw new AssertionError("Không tìm thấy Alert trong 12 giây.");
    }
"""

pattern = re.compile(r"    private DialogPane waitForDialogPane\(FxRobot robot\) \{.*?
    \}

    private void sleepBriefly\(\)", re.DOTALL)
replacement = new_method + "
    private void sleepBriefly()"
text2 = pattern.sub(replacement, text, count=1)
if text2 == text:
    raise SystemExit("Không tìm thấy method waitForDialogPane để vá")

TEST.write_text(text2, encoding="utf-8")

target = ROOT / "client/target"
if target.exists():
    shutil.rmtree(target)
    print("Removed stale target: client/target")

print(f"Patched: {TEST.relative_to(ROOT)}")
print(f"Backup folder: {BACKUP}")
print("DONE. Chạy lại client test.")
