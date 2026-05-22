import os
import re

TEST_DIR = r"C:\Users\Khanh\Documents\GitHub\Auction_System\server\src\test\java"

translations = {
    "Phê duyệt thành công! Phiên đấu giá đã bắt đầu.": "Approved successfully! The auction session has started.",
    "Đã hủy phiên đấu giá.": "Auction session has been canceled.",
    "Lấy danh sách phiên thành công": "Session list retrieved successfully",
    "Lấy danh sách người dùng thành công": "User list retrieved successfully",
    "Lấy danh sách phiên chờ duyệt thành công": "Pending sessions retrieved successfully",
    "Lấy chi tiết phiên thành công": "Session details retrieved successfully",
    "Đã từ chối phiên đấu giá.": "Auction session rejected.",
    "Không có tài khoản nào liên kết với Email này": "No account is associated with this email",
    "Code sai hoặc đã hết hạn, vui lòng kiểm tra lại": "Invalid or expired code, please try again",
    "Đã gửi mã xác nhận": "Verification code sent",
    "Đăng nhập thất bại": "Login failed",
    "Đăng nhập thành công": "Login successful",
    "Email hoặc Username đã tồn tại": "Email or Username already exists",
    "Đăng ký thành công": "Registration successful",
    "Đã hủy phiên thành công": "Session canceled successfully",
    "đã hủy": "canceled",
    "kết thúc": "ended"
}

def translate_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    new_content = content
    for vn, en in translations.items():
        new_content = new_content.replace(vn, en)
        
    if new_content != content:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f"Translated: {filepath}")

for root, _, files in os.walk(TEST_DIR):
    for file in files:
        if file.endswith(".java"):
            translate_file(os.path.join(root, file))

print("Translation script 2 finished.")
