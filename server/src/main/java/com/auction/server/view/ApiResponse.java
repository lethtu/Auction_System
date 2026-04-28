package com.auction.server.view;

<<<<<<< HEAD
=======
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
public class ApiResponse<T>{
    private int status;
    private String message;
    private T data;
<<<<<<< HEAD
=======
    private static final Logger logger = LoggerFactory.getLogger(ApiResponse.class);
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)

    public ApiResponse(int status, String message, T data){
        this.status = status;
        this.message = message;
        this.data = data;
<<<<<<< HEAD
=======
        logger.info("Trạng thái phản hồi: {}, tin nhắn: {}", status, message);
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
    }

    public int getStatus(){
        return status;
    }

    public String getMessage(){
        return message;
    }

    public T getData(){
        return data;
    }
}
