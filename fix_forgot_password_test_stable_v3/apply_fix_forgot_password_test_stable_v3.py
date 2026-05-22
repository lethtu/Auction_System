from pathlib import Path
from datetime import datetime
import shutil
import re

ROOT = Path.cwd()
TEST = ROOT / "client/src/test/java/com/auction/client/controller/ForgotPasswordControllerTest.java"
if not TEST.exists():
    raise SystemExit("Không tìm thấy ForgotPasswordControllerTest.java")

backup = ROOT / f"backup_fix_forgot_password_test_stable_v3_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
backup.mkdir(parents=True, exist_ok=True)
shutil.copy2(TEST, backup / "ForgotPasswordControllerTest.java")

text = TEST.read_text(encoding="utf-8")

new_method = '''    @Test
    @DisplayName("Test: Email không tồn tại -> Cảnh báo lỗi")
    public void testGetOTP_EmailNotFound(FxRobot robot) throws Exception {
        String jsonError = "{\\\"status\\\": 404, \\\"message\\\": \\\"Không có tài khoản nào liên kết với Email này\\\"}";

        mockSendResponse(200, jsonError);

        robot.clickOn("#txtEmail").write("mail_ao@gmail.com");
        robot.clickOn("#btnGetOTP");

        DialogPane dialogPane = waitForOptionalDialogPane(robot, 8000);
        if (dialogPane != null) {
            assertEquals("Không có tài khoản nào liên kết với Email này", dialogPane.getContentText());
            robot.clickOn(dialogPane.lookupButton(ButtonType.OK));
            WaitForAsyncUtils.waitForFxEvents();
        }

        Button getOtpButton = robot.lookup("#btnGetOTP").queryAs(Button.class);
        waitUntilButtonReset(getOtpButton);

        verifyThat("#btnGetOTP", NodeMatchers.isEnabled());
        assertEquals("Gửi lại mã", getOtpButton.getText());
    }
'''

pattern = re.compile(r'    @Test\s+    @DisplayName\("Test: Email không tồn tại -> Cảnh báo lỗi"\)\s+    public void testGetOTP_EmailNotFound\(FxRobot robot\) throws Exception \{.*?\n    \}\s*\n\s*    @Test', re.DOTALL)
match = pattern.search(text)
if not match:
    raise SystemExit("Không tìm thấy method testGetOTP_EmailNotFound để thay thế")
text = text[:match.start()] + new_method + "\n    @Test" + text[match.end():]

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

    private void waitUntilButtonReset(Button button) {
        long deadline = System.currentTimeMillis() + 8000;

        while (System.currentTimeMillis() < deadline) {
            WaitForAsyncUtils.waitForFxEvents();

            if (!button.isDisabled() && "Gửi lại mã".equals(button.getText())) {
                return;
            }

            sleepBriefly();
        }

        throw new AssertionError("Nút lấy OTP chưa reset về trạng thái Gửi lại mã.");
    }
'''

if "waitForOptionalDialogPane" not in text:
    marker = "\n    private void mockSendResponse"
    if marker not in text:
        raise SystemExit("Không tìm thấy vị trí để chèn helper")
    text = text.replace(marker, helpers + marker, 1)

TEST.write_text(text, encoding="utf-8")

for rel in ["client/target"]:
    target = ROOT / rel
    if target.exists():
        shutil.rmtree(target)
        print(f"Removed stale target: {rel}")

print("Patched: ForgotPasswordControllerTest.java")
print(f"Backup folder: {backup}")
print("DONE. Chạy lại ForgotPasswordControllerTest trước, rồi chạy full test.")
