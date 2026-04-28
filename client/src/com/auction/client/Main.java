package com.auction.client;

<<<<<<< HEAD
=======
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.util.Objects;

public class Main extends Application {
<<<<<<< HEAD

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader
                .load(Objects.requireNonNull(getClass().getResource("/com/auction/client/view/Login.fxml")));
        primaryStage.setTitle("Hệ thống Đấu giá");
        primaryStage.setScene(new Scene(root, 400, 500));
=======
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    @Override
    public void start(Stage primaryStage) throws Exception {
        logger.info("Đã khởi động Client thành công");
        Parent root = FXMLLoader
                .load(Objects.requireNonNull(getClass().getResource("/com/auction/client/view/Login.fxml")));
        primaryStage.setTitle(Config.Title);
        primaryStage.setScene(new Scene(root, Config.Width, Config.Height));
>>>>>>> 0e01b02 (Thêm log, lọc file, fix logic, kiểm tra và test toàn bộ, thêm checkstyle)
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}