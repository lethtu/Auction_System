package com.auction.client;

import java.io.ObjectOutputStream;
import java.net.Socket;

public class Test {
    public static void main(String[] args) {
        // Địa chỉ IP của Server (chạy trên cùng một máy nên dùng localhost)
        String serverAddress = "localhost";

        // Cổng mà SocketServer của bạn đang lắng nghe
        int port = 8081;

        System.out.println("Đang thử gõ cửa Server tại " + serverAddress + ":" + port + "...");

        // Thử mở kết nối
        try (Socket socket = new Socket(serverAddress, port);
             // Mở luồng gửi dữ liệu (Bắt buộc dùng ObjectOutputStream vì Server đang dùng ObjectInputStream)
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {

            System.out.println("✅ THÀNH CÔNG: Đã bước vào cửa Server!");

            // Đóng gói một lời chào và gửi sang Server
            String testMessage = "Xin chào Server, tôi là Client thử nghiệm đây! Tôi đã kết nối thành công!";
            out.writeObject(testMessage);

            // flush() giống như thao tác bấm nút "Gửi" để ép dữ liệu bay qua mạng ngay lập tức
            out.flush();

            System.out.println("✅ THÀNH CÔNG: Đã gửi gói hàng cho Server!");

        } catch (Exception e) {
            // Nếu Server chưa bật, nó sẽ báo lỗi Connection Refused ở đây
            System.err.println("❌ THẤT BẠI: Không thể kết nối tới Server. Lỗi: " + e.getMessage());
        }
    }
}