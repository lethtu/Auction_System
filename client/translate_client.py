import os
import re

directory = r"c:\Users\Khanh\Documents\GitHub\Auction_System\client\src\main\java"

translations = {
    "Lỗi chuyển sang trang Settings.fxml: ": "Error switching to Settings.fxml: ",
    "Lỗi điều hướng:": "Navigation error:",
    "Lỗi điều hướng sang My Bids:": "Navigation error to My Bids:",
    "Tài Khoản Của Tôi": "My Account",
    "Nạp tiền": "Deposit",
    "Đăng Xuất": "Logout",
    "Lỗi khi chuyển sang trang tài khoản: ": "Error switching to account page: ",
    "Lỗi khi chuyển sang trang nạp tiền: ": "Error switching to deposit page: ",
    "Lỗi đăng xuất": "Logout error",
    "Lỗi tải sản phẩm: {}": "Error loading products: {}",
    "Lỗi chuyển cảnh vào phòng đấu giá": "Error switching to auction room",
    "Lỗi chuyển cảnh": "Error switching scene",
    "Không thể cập nhật avatar trên top bar: {}": "Cannot update avatar on top bar: {}",
    "Có thể navigate sang setting nếu cần": "Can navigate to settings if needed",
    "Lỗi quay lại trang chính: ": "Error going back to main page: ",
    "thành công": "success",
    "Hàm trung tâm xử lý Data-Driven UI: Lọc bộ đệm (RAM) và vẽ lại màn hình": "Core Data-Driven UI handler: Filter buffer (RAM) and redraw screen",
    "Bước 1: Tính toán danh sách ID sẽ hiển thị sau khi lọc": "Step 1: Calculate the list of IDs to display after filtering",
    "Chỉ chặn đứng các phiên bị hủy hoặc đã thanh toán": "Only block canceled or paid sessions",
    "Logic lọc 3 lớp": "3-layer filtering logic",
    "Bước 2: So sánh xem danh sách hiển thị có bị đổi không (thêm/bớt/đổi bộ lọc)": "Step 2: Check if the display list has changed (add/remove/change filters)",
    "Có sự thay đổi => Vẽ lại toàn bộ": "Changes detected => Redraw everything",
    "Chào, ": "Hello, ",
    "Đăng bán nhanh": "Quick Sale",
    "Không lấy được sellerId từ session": "Could not get sellerId from session",
    "Không lấy được sellerId từ session.": "Could not get sellerId from session.",
    "Lỗi": "Error",
    "Giá khởi điểm không được để trống": "Starting price cannot be empty",
    "Giá khởi điểm phải là một số dương": "Starting price must be a positive number",
    "Vui lòng nhập ngày giờ bắt đầu hợp lệ!": "Please enter a valid start date and time!",
    "Vui lòng nhập ngày giờ kết thúc hợp lệ!": "Please enter a valid end date and time!",
    "Thời gian kết thúc phải ở tương lai": "End time must be in the future",
    "Thời gian kết thúc phải sau thời gian bắt đầu": "End time must be after start time",
    "Lưu bản nháp thành công.": "Draft saved successfully.",
    "Thành công": "Success",
    "Lỗi api: {}": "API error: {}",
    "Lỗi không thể kết nối đến máy chủ: {}": "Server connection error: {}",
    "Lỗi mạng": "Network Error",
    "Không thể kết nối đến máy chủ!": "Cannot connect to the server!",
    "Bước ": "Step ",
    "Lưu thành công.": "Saved successfully.",
    "Vui lòng điền đầy đủ Tên SP, Loại SP, Giá khởi điểm và Bước giá.": "Please fill in Product Name, Type, Starting Price and Step Price.",
    "Giá khởi điểm, bước giá và giá sàn phải là số hợp lệ.": "Starting price, step price and reserve price must be valid numbers.",
    "Giá khởi điểm phải lớn hơn 0.": "Starting price must be greater than 0.",
    "Bước giá phải lớn hơn 0.": "Step price must be greater than 0.",
    "Giá sàn phải lớn hơn 0.": "Reserve price must be greater than 0.",
    "Giá sàn không được nhỏ hơn giá khởi điểm.": "Reserve price cannot be less than starting price.",
    "Vui lòng nhập mức giá tối thiểu (Min rate).": "Please enter minimum rate (Min rate).",
    "Mức giá tối thiểu phải là số lớn hơn 0 hợp lệ.": "Minimum rate must be a valid number greater than 0.",
    "Mức giá tối thiểu không được nhỏ hơn giá khởi điểm.": "Minimum rate cannot be less than starting price.",
    "Lỗi hệ thống khi tải sản phẩm: ": "System error when loading products: ",
    "Đã xảy ra lỗi khi tải danh sách sản phẩm.": "An error occurred while loading the product list.",
    "Tạo phiên đấu giá thành công!": "Auction session created successfully!",
    "Tạo thất bại: ": "Creation failed: ",
    "Cập nhật thành công!": "Update successful!",
    "Cập nhật thất bại: ": "Update failed: ",
    "Bạn chưa chọn sản phẩm nào để xoá!": "You haven't selected any product to delete!",
    "Xoá thành công!": "Deleted successfully!",
    "Xoá thất bại: ": "Deletion failed: ",
    "Bắt đầu phiên đấu giá thành công!": "Started auction session successfully!",
    "Bắt đầu thất bại: ": "Start failed: "
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

for root, _, files in os.walk(directory):
    for file in files:
        if file.endswith(".java") or file.endswith(".fxml"):
            translate_file(os.path.join(root, file))

print("Translation script finished.")
