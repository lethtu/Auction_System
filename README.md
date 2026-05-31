# Auction System - Hệ thống đấu giá trực tuyến

## 1. Mô tả bài toán và phạm vi hệ thống

Auction System là hệ thống đấu giá trực tuyến theo kiến trúc Client-Server. Người dùng có thể đăng ký, đăng nhập, xem các phiên đấu giá, đặt giá theo thời gian thực, theo dõi lịch sử bid và nhận thông báo khi giá thay đổi hoặc phiên đấu giá kết thúc.

Hệ thống gồm ba nhóm vai trò chính:

- Bidder: xem phiên đấu giá, đặt giá, dùng auto-bid, theo dõi các phiên đã tham gia.
- Seller: tạo và quản lý sản phẩm/phiên đấu giá.
- Admin: quản lý người dùng, sản phẩm và phiên đấu giá trong hệ thống.

## 2. Thành viên nhóm

| STT | Họ và tên | Mã sinh viên | GitHub |
| --- | --- | --- | --- |
| 1 | Lê Thanh Tùng | 25022003 | `lethtu` |
| 2 | Lê Đình Quốc Khánh | 25021824 | `Ledinhquockhanh2007` |
| 3 | Nguyễn Lê Quang Minh | 25021877 | `nguyenlequangminh2409-png` |
| 4 | Nguyên Hà Phan | 25021927 | `nhphan0505` |

## 3. Công nghệ sử dụng và yêu cầu cài đặt

### Công nghệ chính

- Client: Java 21, JavaFX 21.0.2, FXML, Maven.
- Server: Java 17+, Spring Boot 3.2.4, Spring Web, Spring Data JPA, WebSocket.
- Database: MySQL hoặc TiDB Cloud.
- Kiểm thử và chất lượng mã: JUnit 5, Mockito, JaCoCo, Checkstyle.
- Công cụ hỗ trợ: Git, GitHub Actions, Maven.

### Yêu cầu cài đặt

- JDK 21.
- Maven 3.8 trở lên.
- MySQL 8.0 trở lên nếu chạy database local.
- Kết nối internet nếu dùng database/cloud service đã cấu hình sẵn.

## 4. Cấu hình database

Server đọc cấu hình database tại:

```text
server/src/main/resources/application.properties
```

Hiện tại project đã cấu hình sẵn database TiDB Cloud trong file này. Nếu dùng cấu hình cloud sẵn, chỉ cần chạy server.

Nếu muốn chạy database local bằng MySQL, thực hiện các bước sau.

### Bước 1: Tạo database

Đăng nhập MySQL:

```bash
mysql -u root -p
```

Tạo database:

```sql
CREATE DATABASE auction_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
EXIT;
```

### Bước 2: Import dữ liệu mẫu

Từ thư mục gốc project, chạy:

```bash
mysql -u root -p auction_system < Data.sql
```

Nếu dùng PowerShell trên Windows, lệnh tương tự:

```powershell
mysql -u root -p auction_system < .\Data.sql
```

### Bước 3: Sửa cấu hình kết nối database

Mở `server/src/main/resources/application.properties` và đổi các dòng database thành thông tin MySQL local:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/auction_system?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.hibernate.ddl-auto=update
```

Sau đó build/chạy lại server.

## 5. Cấu trúc thư mục chính

```text
.
|-- client/      # Ứng dụng desktop JavaFX
|-- server/      # Backend Spring Boot
|-- dist/        # File JAR nộp kèm repository
|-- Data.sql     # Script database mẫu
|-- README.md    # Hướng dẫn build, chạy và mô tả hệ thống
`-- LICENSE
```

Các package quan trọng:

- `client/src/main/java/com/auction/client/controller`: controller JavaFX.
- `client/src/main/java/com/auction/client/service`: service phía client, notification, settings, socket.
- `client/src/main/java/com/auction/client/model`: model phía client.
- `server/src/main/java/com/auction/server/controller`: REST controller.
- `server/src/main/java/com/auction/server/service`: xử lý nghiệp vụ.
- `server/src/main/java/com/auction/server/repository`: truy cập dữ liệu.
- `server/src/main/java/com/auction/server/socket`: WebSocket realtime.

## 6. Vị trí file JAR và cách build

File JAR nộp kèm repository nằm tại:

- `dist/server.jar`
- `dist/client.jar`: client chạy với server local tại `http://localhost:8080`.
- `dist/client-online.jar`: client chạy trực tiếp với server online, không cần bật server local.

Nếu cần build lại từ source, chạy từ thư mục gốc project:

```bash
mvn -f server/pom.xml clean package -Dcheckstyle.skip=true -Dmaven.test.skip=true
mvn -f client/pom.xml clean package -Dcheckstyle.skip=true -Dmaven.test.skip=true
```

Sau khi build lại, Maven tạo JAR tại:

- `server/target/server.jar`
- `client/target/client.jar`

Trong repository này, các file JAR đã được copy sẵn sang `dist/` để phục vụ nộp bài và chạy demo.
Riêng `client-online.jar` là bản client đã trỏ sẵn tới server online.

## 7. Hướng dẫn chạy Server và Client

Chạy server trước:

```bash
java -jar dist/server.jar
```

Server mặc định chạy tại:

```text
http://localhost:8080
```

Mở terminal khác và chạy client:

```bash
java -jar dist/client.jar
```

`dist/client.jar` giao tiếp với server local tại:

```text
http://localhost:8080
```

Để demo nhiều người tham gia đấu giá, mở nhiều terminal và chạy nhiều client:

```bash
java -jar dist/client.jar
java -jar dist/client.jar
```

Nếu muốn chạy client với server online mà không bật server local, dùng:

```bash
java -jar dist/client-online.jar
```

## 8. Cấu hình địa chỉ server cho client

Client lấy địa chỉ backend tại:

```text
client/src/main/java/com/auction/client/Config.java
```

Dòng cấu hình chính:

```java
public static final String API_URL = "http://localhost:8080";
```

Nếu muốn build bản client chạy với server online, đổi thành:

```java
public static final String API_URL = "https://server-bai-tap-lon.shin25112007.workers.dev";
```

Sau khi đổi `API_URL`, build lại client:

```bash
mvn -f client/pom.xml clean package -Dcheckstyle.skip=true -Dmaven.test.skip=true
```

WebSocket realtime được suy ra tự động từ `API_URL`:

- `https://...` -> `wss://.../ws/notification`
- `http://localhost:8080` -> `ws://localhost:8080/ws/notification`

## 9. Chức năng hoàn thành theo barem yêu cầu

### 9.1 Quản lý người dùng

- Đăng ký, đăng nhập, đăng xuất.
- Quên mật khẩu qua email.
- Đăng nhập Google.
- Phân vai trò Bidder, Seller, Admin.
- Quản lý hồ sơ người dùng, số dư và trạng thái tài khoản.

### 9.2 Quản lý sản phẩm và phiên đấu giá

- Seller tạo sản phẩm và phiên đấu giá.
- Cấu hình tên, mô tả, ảnh, loại sản phẩm, giá khởi điểm, bước giá, thời gian bắt đầu/kết thúc.
- Seller xem, sửa, hủy và theo dõi các phiên của mình.
- Admin kiểm duyệt và quản lý phiên đấu giá.

### 9.3 Tham gia đấu giá

- Bidder xem danh sách phiên đấu giá đang hoạt động.
- Xem chi tiết sản phẩm, giá hiện tại, người dẫn đầu, số lượt bid và thời gian còn lại.
- Đặt giá trực tiếp trên màn đấu giá.
- Kiểm tra giá hợp lệ trước khi gửi bid.
- Không cho seller tự bid sản phẩm của mình.
- Cập nhật kết quả bid theo thời gian thực qua WebSocket.

### 9.4 Kết thúc phiên đấu giá

- Server tự động kiểm tra và kết thúc phiên khi hết thời gian.
- Xác định người thắng dựa trên bid hợp lệ cao nhất.
- Gửi thông báo thắng/thua và cập nhật số dư liên quan.
- Hỗ trợ nhập thông tin giao hàng sau khi thắng phiên.

### 9.5 Xử lý lỗi và ngoại lệ

- Chặn bid thấp hơn giá hiện tại hoặc sai bước giá.
- Chặn bid khi phiên đã đóng.
- Chặn bid khi số dư không đủ.
- Xử lý lỗi kết nối WebSocket bằng heartbeat và reconnect.
- Trả thông báo lỗi rõ ràng cho client thay vì làm treo giao diện.

### 9.6 Giao diện người dùng

- Giao diện desktop JavaFX/FXML.
- Màn hình đăng nhập/đăng ký/quên mật khẩu.
- Màn hình danh sách phiên đấu giá.
- Màn hình chi tiết đấu giá realtime.
- Dashboard cho Seller.
- Dashboard cho Admin.
- Notification center, topbar, sidebar, theme sáng/tối và màu nhấn.

### 9.7 OOP, MVC và Design Pattern

- OOP: dùng các model như `User`, `Bidder`, `Seller`, `Admin`, `Item`, `Art`, `Electronics`, `Vehicle`, `AuctionSession`, `Bid`.
- Encapsulation: các entity/model có trường dữ liệu và getter/setter rõ ràng.
- Inheritance/Abstraction: phân loại người dùng và sản phẩm theo vai trò/loại.
- MVC: client tách FXML, controller, service; server tách controller, service, repository, model.
- Factory Method: `ItemFactory` tạo các loại sản phẩm.
- Observer/Event-based: WebSocket broadcast sự kiện bid và kết thúc phiên.
- Singleton: các service client dùng chung như notification, settings, cache.

### 9.8 Concurrency, test và CI

- Server xử lý bid và auto-bid tập trung để tránh lost update/race condition.
- Có test JUnit/Mockito cho nhiều phần logic quan trọng.
- Có cấu hình Maven, Checkstyle, JaCoCo.
- Có GitHub Actions phục vụ kiểm thử tự động.

## 10. Chức năng nâng cao và điểm nổi bật

- Auto-bidding: người dùng đặt `maxBid` và `increment`, hệ thống tự trả giá khi có đối thủ bid.
- Anti-sniping/gia hạn phiên: hỗ trợ cập nhật thời gian kết thúc khi có bid sát giờ.
- Realtime update: giá, số lượt bid, người dẫn đầu, lịch sử bid và số người đang xem được cập nhật không cần refresh.
- Bid history visualization: hiển thị lịch sử/đường biến động giá trong phiên.
- Notification realtime: thông báo khi bị vượt giá, thắng/thua phiên, phiên kết thúc.
- Heartbeat/reconnect WebSocket: giảm lỗi mất kết nối khi app để lâu hoặc mạng chập chờn.
- Upload ảnh và model 3D cho sản phẩm.
- UI có theme, âm thanh thông báo và trải nghiệm giống ứng dụng desktop hoàn chỉnh.

## 11. Link báo cáo và video demo

- Báo cáo PDF: https://drive.google.com/file/d/1Laf1aX2HR_uxoy012ON7dtIsHZIkfMfK/view?usp=sharing
- Video demo: https://drive.google.com/drive/folders/1lGXy85Uv6c3gUsLks1Bsg_k5NVa3DG8x?usp=drive_link
