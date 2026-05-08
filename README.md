# Auction System

Hệ thống đấu giá trực tuyến (Auction System) được phát triển bằng Java, sử dụng kiến trúc Client-Server.

## Cấu trúc dự án

Dự án bao gồm hai thành phần chính:
- **Server**: REST API phát triển bằng Spring Boot 3.2.4, Java 17.
- **Client**: Ứng dụng desktop phát triển bằng JavaFX 21, Java 21.

## Yêu cầu hệ thống

- **Java JDK**: 21 (khuyến nghị để tương thích cả hai thành phần).
- **Maven**: 3.8+
- **MySQL Server**: 8.0+

## Hướng dẫn cài đặt và chạy local

### 1. Thiết lập cơ sở dữ liệu

1. Mở MySQL và tạo cơ sở dữ liệu mới:
   ```sql
   CREATE DATABASE auction_system;
   ```
2. Import dữ liệu từ file [Data.sql](Data.sql):
   ```bash
   mysql -u root -p auction_system < Data.sql
   ```
3. Kiểm tra cấu hình kết nối trong file `server/src/main/resources/application.properties` và cập nhật username/password nếu cần.

### 2. Chạy Server

Mở terminal tại thư mục gốc của dự án:
```bash
cd server
mvn clean install
mvn spring-boot:run
```
Server sẽ chạy tại `http://localhost:8080`.

### 3. Chạy Client

Mở một terminal mới tại thư mục gốc của dự án:
```bash
cd client
mvn clean install
mvn javafx:run
```

## CI/CD

Dự án đã được tích hợp GitHub Actions để tự động build và test. File cấu hình nằm tại `.github/workflows/ci.yml`.
Mỗi khi có thay đổi được push lên GitHub, hệ thống sẽ tự động chạy quy trình kiểm thử và tạo ra các bản build (Artifacts).

## Tác giả
- BTL Nhóm 30
