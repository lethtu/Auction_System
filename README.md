# Auction System

## Thành viên nhóm
- Lê Thanh Tùng username: lethtu MSV: 25022003
- Lê Đình Quốc Khánh username: Ledinhquockhanh2007 MSV: 25021824
- Nguyễn Lê Quang Minh username: nguyenlequangminh2409-png MSV: 25021877
- Nguyên Hà Phan username: nhphan0505 MSV: 25021927

## Mô tả bài toán và phạm vi hệ thống
Hệ thống đấu giá trực tuyến (Auction System) được phát triển bằng Java, sử dụng kiến trúc Client-Server. Hệ thống cho phép người dùng tham gia đấu giá các sản phẩm trong thời gian thực, quản lý phiên đấu giá và đặt giá. Phạm vi bao gồm một máy chủ trung tâm quản lý dữ liệu và kết nối, cùng với nhiều client cho người dùng cuối thao tác.

## Công nghệ sử dụng, môi trường chạy và yêu cầu cài đặt
**Công nghệ sử dụng:**
- **Server**: Spring Boot 3.2.4, Java 17.
- **Client**: JavaFX 21, Java 21.
- **Giao tiếp**: Kết hợp REST API và Socket (cho các tính năng thời gian thực).
- **Cơ sở dữ liệu**: MySQL.

**Môi trường chạy và yêu cầu cài đặt:**
- Java JDK 21 (khuyến nghị để tương thích cả hai thành phần).
- Maven 3.8+.
- MySQL Server 8.0+.

## Cấu trúc thư mục hoặc các module chính
Dự án bao gồm hai thành phần chính:
- `server/`: Mã nguồn của Server (Spring Boot) cung cấp REST API và xử lý Socket.
- `client/`: Mã nguồn của ứng dụng desktop Client (JavaFX).
- `Data.sql`: File SQL dùng để khởi tạo cơ sở dữ liệu.
- `.github/workflows/`: Cấu hình CI/CD tự động.

## Vị trí các file .jar
- Bản build .jar của Server sau khi đóng gói sẽ nằm trong thư mục `server/target/`.
- Bản build .jar của Client sau khi đóng gói sẽ nằm trong thư mục `client/target/`.

## Hướng dẫn chạy Server/Client theo thứ tự cụ thể

### Bước 1: Thiết lập cơ sở dữ liệu
1. Mở MySQL và tạo cơ sở dữ liệu mới:
   ```sql
   CREATE DATABASE auction_system;
   ```
2. Import dữ liệu từ file `Data.sql`:
   ```bash
   mysql -u root -p auction_system < Data.sql
   ```
3. Kiểm tra cấu hình kết nối trong file `server/src/main/resources/application.properties` và cập nhật username/password để kết nối.

### Bước 2: Chạy Server (Yêu cầu chạy trước)
Mở terminal tại thư mục gốc của dự án:
```bash
cd server
mvn clean install
mvn spring-boot:run
```
Server sẽ chạy tại `http://localhost:8080`.

### Bước 3: Chạy Client
Mở một terminal mới tại thư mục gốc của dự án (sau khi Server đã chạy thành công):
```bash
cd client
mvn clean install
mvn javafx:run
```

## Danh sách chức năng đã hoàn thành
- Quản lý phiên đấu giá và sản phẩm.
- Đăng nhập, đăng ký và xác thực tài khoản.
- Tham gia đấu giá thời gian thực sử dụng Socket.
- Tự động hóa kiểm thử và tích hợp CI/CD với GitHub Actions.

## Link báo cáo PDF và video demo
- **Báo cáo PDF**: *(Link pdf)*
- **Video demo**: *(Link demo)*
