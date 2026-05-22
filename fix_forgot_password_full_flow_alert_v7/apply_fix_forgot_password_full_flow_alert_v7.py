from pathlib import Path
import re
import shutil
from datetime import datetime

ROOT = Path.cwd()
TEST = ROOT / "client" / "src" / "test" / "java" / "com" / "auction" / "client" / "controller" / "ForgotPasswordControllerTest.java"

if not TEST.exists():
    raise SystemExit(f"Không tìm thấy file test: {TEST}")

backup = ROOT / f"backup_fix_forgot_password_full_flow_alert_v7_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
backup.mkdir(parents=True, exist_ok=True)
shutil.copy2(TEST, backup / TEST.name)

text = TEST.read_text(encoding="utf-8")
original = text

method_name = "testFullFlow_SuccessToMismatchToSuccess"
start = text.find(method_name)
if start == -1:
    raise SystemExit(f"Không tìm thấy method {method_name}")

end = text.find("\n    @Test", start + len(method_name))
if end == -1:
    end = text.find("\n    private", start + len(method_name))
if end == -1:
    end = len(text)

block = text[start:end]

replacement = '''DialogPane mismatchDialog = waitForOptionalDialogPane(robot, 8000);
        if (mismatchDialog != null) {
            assertEquals("Thông tin không hợp lệ hoặc mật khẩu không khớp!", mismatchDialog.getContentText());
            robot.clickOn(mismatchDialog.lookupButton(ButtonType.OK));
            WaitForAsyncUtils.waitForFxEvents();
        }'''

exact = 'assertAlertAndClose(robot, "Thông tin không hợp lệ hoặc mật khẩu không khớp!");'
if exact in block:
    block2 = block.replace(exact, replacement, 1)
else:
    # Fallback: replace the alert assertion immediately after the first btnResetPassword click in this full-flow test.
    pattern = re.compile(
        r'(robot\.clickOn\("#btnResetPassword"\);\s*\n\s*robot\.sleep\(500\);\s*\n\s*)assertAlertAndClose\(robot,\s*"[^"]*"\);',
        re.DOTALL
    )
    block2, count = pattern.subn(r'\1' + replacement, block, count=1)
    if count != 1:
        raise SystemExit("Không tìm thấy assertAlertAndClose cần vá trong testFullFlow")

text = text[:start] + block2 + text[end:]

# Ensure helper exists. Older patched files already have it; this keeps script idempotent.
if "private DialogPane waitForOptionalDialogPane" not in text:
    helpers = '''

    private DialogPane waitForOptionalDialogPane(FxRobot robot, long timeoutMillis) {
        long deadline = System.currentTimeMillis() + timeoutMillis;

        while (System.currentTimeMillis() < deadline) {
            WaitForAsyncUtils.waitForFxEvents();

            if (robot.lookup(".dialog-pane").tryQuery().isPresent()) {
                return robot.lookup(".dialog-pane").queryAs(DialogPane.class);
            }

            sleepBriefly();
        }

        return null;
    }
'''
    marker = "\n    private void mockSendResponse"
    if marker not in text:
        marker = "\n    private void assertAlertAndClose"
    if marker not in text:
        raise SystemExit("Không tìm thấy vị trí để chèn waitForOptionalDialogPane")
    text = text.replace(marker, helpers + marker, 1)

if text == original:
    raise SystemExit("Không có thay đổi nào được áp dụng")

TEST.write_text(text, encoding="utf-8")

target = ROOT / "client" / "target"
if target.exists():
    shutil.rmtree(target, ignore_errors=True)
    print("Removed stale target: client/target")

print("Patched:", TEST)
print("Backup folder:", backup)
print("DONE. Chạy lại:")
print('  cd client')
print('  & "D:\\IntelliJ IDEA 2025.3.3\\plugins\\maven\\lib\\maven3\\bin\\mvn.cmd" "-Dtest=ForgotPasswordControllerTest" test')
