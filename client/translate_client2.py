import os

directory = r"c:\Users\Khanh\Documents\GitHub\Auction_System\client\src\main\java"

translations = {
    "Lưu bản nháp success.": "Draft saved successfully.",
    "Cập nhật bản nháp success.": "Draft updated successfully.",
    "Cập nhật phiên đấu giá success.": "Auction session updated successfully.",
    "Tạo phiên đấu giá success.": "Auction session created successfully.",
    "Đăng bán phiên đấu giá success.": "Auction session published successfully.",
    "Đã hủy phiên success.": "Session canceled successfully.",
    "Error không thể kết nối đến máy chủ: {}": "Cannot connect to server: {}",
    "Error mạng": "Network Error",
    "Error dữ liệu": "Data Error",
    "Giá khởi điểm phải là số.": "Starting price must be a number.",
    "Xác nhận": "Confirmation",
    "Bạn có chắc muốn hủy phiên #": "Are you sure you want to cancel session #",
    " không?": "?",
    "Error đăng bán": "Sale Error",
    "Tên sản phẩm của bản nháp không được để trống!": "Product name of the draft cannot be empty!",
    "Loại sản phẩm của bản nháp không được để trống!": "Product type of the draft cannot be empty!",
    "Giá khởi điểm phải lớn hơn 0!": "Starting price must be greater than 0!",
    "Error thời gian": "Time Error",
    "Định dạng thời gian bắt đầu của bản nháp không hợp lệ!": "Invalid start time format in the draft!",
    "Định dạng thời gian kết thúc của bản nháp không hợp lệ!": "Invalid end time format in the draft!",
    "Error không lấy được sellerId từ session": "Could not get sellerId from session",
    "Không thể tải dữ liệu seller từ server.": "Cannot load seller data from server.",
    "Không rõ": "Unknown",
    "Error khi chuyển sang màn hình Login!": "Error switching to Login screen!",
    "Có lỗi xảy ra từ server.": "An error occurred from the server.",
    "Không có dữ liệu từ server.": "No data from the server.",
    "Không đọc được dữ liệu từ server.": "Could not read data from the server.",
    "Thời gian bắt đầu đã đến!": "Start time has arrived!",
    "Phiên đấu giá cho vật phẩm của bạn đã sẵn sàng.\\nBạn có muốn bắt đầu ngay bây giờ không?": "The auction session for your item is ready.\\nDo you want to start it now?",
    "Chấp nhận": "Accept",
    "Bỏ qua": "Ignore",
    "Đã bắt đầu phiên thành công!": "Session started successfully!",
    "Đã bắt đầu phiên success!": "Session started successfully!",
    "Trang chủ": "Home",
    "Tạo phiên đấu giá mới": "Create New Auction",
    "Đăng xuất": "Logout",
    "Chỉ cần điền Tên và Loại Sản Phẩm để lưu bản nháp!": "Just fill in Name and Product Type to save a draft!",
    "Bắt buộc": "Required",
    "Không bắt buộc": "Optional",
    "Lưu bản nháp": "Save Draft",
    "Đăng bán": "Publish",
    "Chọn Loại": "Select Type"
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

print("Translation script 2 finished.")
