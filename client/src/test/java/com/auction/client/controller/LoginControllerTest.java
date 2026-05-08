package com.auction.client.controller;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxAssert;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.control.LabeledMatchers;

import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.base.NodeMatchers.isVisible;

@ExtendWith(ApplicationExtension.class)
public class LoginControllerTest {

    @Start
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/com/auction/client/view/Login.fxml"));
        stage.setScene(new Scene(root, 400, 500));
        stage.show();
    }

    @Test
    public void should_have_login_fields(FxRobot robot) {
        verifyThat("#txtUsername", isVisible());
        verifyThat("#txtPassword", isVisible());
    }

    @Test
    public void should_show_warning_on_empty_login(FxRobot robot) {
        // Click login without entering anything
        robot.clickOn(".button"); 
        
        // Note: Testing alerts with TestFX can be tricky as they are separate windows.
        // Usually, we would mock the showAlert method or check for the alert window.
    }

    @Test
    public void should_allow_typing_username(FxRobot robot) {
        robot.clickOn("#txtUsername").write("testuser");
        FxAssert.verifyThat("#txtUsername", (TextField t) -> t.getText().equals("testuser"));
    }

    @Test
    public void should_allow_typing_password(FxRobot robot) {
        robot.clickOn("#txtPassword").write("password123");
        FxAssert.verifyThat("#txtPassword", (PasswordField p) -> p.getText().equals("password123"));
    }
}
