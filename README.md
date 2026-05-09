<h1 align="center">Hệ thống Đấu giá Trực tuyến (Auction System)</h1>

<p align="center">
  <i>Hệ thống quản lý và tham gia đấu giá trực tuyến được phát triển bằng Java, ứng dụng kiến trúc Client-Server với khả năng cập nhật thời gian thực.</i>
</p>

---

## 1. Thành viên Nhóm

| STT | Họ và Tên | Mã Sinh Viên | Username (GitHub) |
| :---: | :--- | :---: | :--- |
| 1 | Lê Thanh Tùng | 25022003 | `lethtu` |
| 2 | Lê Đình Quốc Khánh | 25021824 | `Ledinhquockhanh2007` |
| 3 | Nguyễn Lê Quang Minh | 25021877 | `nguyenlequangminh2409-png` |
| 4 | Nguyên Hà Phan | 25021927 | `nhphan0505` |

---

## 2. Mô tả bài toán và Phạm vi hệ thống

**Mô tả bài toán:**  
Dự án số hóa quy trình đấu giá truyền thống thành một hệ sinh thái thương mại trực tuyến minh bạch và có tính tương tác cao. Bài toán giải quyết bao phủ toàn bộ luồng nghiệp vụ với sự tham gia của nhiều vai trò:
- **Quản lý Định danh & Tài khoản:** Hỗ trợ đăng nhập, đăng ký, khôi phục mật khẩu an toàn và quản lý hồ sơ cá nhân.
- **Dành cho Người bán (Seller):** Cho phép người dùng tạo mới sản phẩm, tự do thiết lập thông tin, giá khởi điểm, bước giá và khoảng thời gian diễn ra đấu giá.
- **Dành cho Người mua (Bidder):** Duyệt danh mục sản phẩm công khai. Khi tham gia một phiên, người dùng có thể "đặt giá (bid)" liên tục. Lịch sử đặt giá và đồng hồ đếm ngược được cập nhật theo thời gian thực (real-time) để đảm bảo tính công bằng.
- **Dành cho Quản trị viên (Admin):** Cung cấp các công cụ đặc quyền để giám sát hệ thống, quản lý người dùng và trực tiếp kiểm duyệt các phiên đấu giá trước khi chúng được hiển thị công khai.

**Phạm vi hệ thống:**  
Hệ thống được thiết kế theo kiến trúc Client-Server, phân chia trách nhiệm rõ ràng:
- **Máy chủ (Server - Spring Boot):** Kiểm soát toàn bộ logic nghiệp vụ, giao tiếp với cơ sở dữ liệu, phân quyền bảo mật qua REST API. Đặc biệt, Server chịu trách nhiệm quản lý kết nối Socket để phát sóng (broadcast) trạng thái đấu giá đồng bộ tới toàn bộ Client ngay tức thì.
- **Máy trạm (Client - JavaFX):** Cung cấp giao diện đồ họa (GUI) trực quan. Client tiếp nhận thao tác của người dùng cuối, hiển thị dữ liệu và duy trì kết nối liên tục (Socket) để cập nhật các biến động giá mới nhất không có độ trễ.

---

## 3. Công nghệ sử dụng và Yêu cầu cài đặt

### Công nghệ cốt lõi
- **Server:** Java 17, Spring Boot 3.2.4.
- **Client:** Java 21, JavaFX 21.
- **Giao tiếp:** Kết hợp REST API (cho các thao tác CRUD) và **Socket** (đảm bảo cập nhật thời gian thực cho phiên đấu giá).
- **Cơ sở dữ liệu:** MySQL 8.0+.

### Yêu cầu môi trường
- **Java Development Kit (JDK):** Phiên bản 21 (được khuyến nghị để tương thích tốt nhất với cả Server và Client).
- **Build Tool:** Maven 3.8 trở lên.
- **Database:** MySQL Server bản 8.0 trở lên.

---

## 4. Cấu trúc thư mục chính

Hệ thống được chia làm hai module độc lập để dễ quản lý và triển khai:

- `server/` : Chứa mã nguồn của backend (Spring Boot), cung cấp RESTful API và xử lý các kết nối qua Socket.
- `client/` : Chứa mã nguồn của giao diện ứng dụng desktop (JavaFX).
- `Data.sql` : File kịch bản SQL dùng để khởi tạo cơ sở dữ liệu và dữ liệu mẫu.
- `.github/workflows/` : Chứa các file cấu hình tự động hóa (CI/CD Pipeline) bằng GitHub Actions.

---

## 5. Vị trí các file thực thi (.jar)

Sau khi quá trình build hoàn tất, các file thực thi sẽ được đặt tại:
- **Server:** `server/target/`
- **Client:** `client/target/`

---

## 6. Hướng dẫn cài đặt và Khởi chạy hệ thống

Vui lòng thực hiện tuần tự các bước dưới đây để khởi chạy hệ thống trên máy cá nhân (Localhost).

### Bước 6.1: Khởi tạo Cơ sở dữ liệu

1. Truy cập vào MySQL và tạo cơ sở dữ liệu:
   ```sql
   CREATE DATABASE auction_system;
   ```
2. Nạp dữ liệu cấu trúc vào hệ thống từ file `Data.sql`:
   ```bash
   mysql -u root -p auction_system < Data.sql
   ```
3. Mở file cấu hình `server/src/main/resources/application.properties` và điều chỉnh `spring.datasource.username` / `spring.datasource.password` cho phù hợp với môi trường của bạn.

### Bước 6.2: Khởi chạy Server (Bắt buộc chạy trước)

Server cần được khởi động trước để có thể lắng nghe kết nối Socket và các truy xuất API từ Client. Mở terminal tại thư mục gốc dự án:

```bash
cd server
mvn clean install
mvn spring-boot:run
```
> Server sẽ được khởi chạy tại địa chỉ: `http://localhost:8080`

### Bước 6.3: Khởi chạy Client

Sau khi Server đã báo khởi chạy thành công, hãy mở một cửa sổ terminal mới từ thư mục gốc của dự án:

```bash
cd client
mvn clean install
mvn javafx:run
```

---

## 7. Danh sách Chức năng đã hoàn thành

- Quản lý phiên đấu giá và danh mục sản phẩm.
- Quản lý định danh: Đăng ký, Đăng nhập và Xác thực tài khoản người dùng an toàn.
- Tham gia đấu giá thời gian thực với độ trễ thấp thông qua công nghệ **Socket**.
- Tự động hóa quy trình kiểm thử và tích hợp liên tục (CI/CD) qua GitHub Actions.

---

## 8. Tài nguyên đính kèm

- **Báo cáo PDF:** [*(Nhấn vào đây để cập nhật link báo cáo)*](#)
- **Video Demo:** [*(Nhấn vào đây để cập nhật link video)*](#)

