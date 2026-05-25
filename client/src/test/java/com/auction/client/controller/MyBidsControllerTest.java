package com.auction.client.controller;

import com.auction.client.model.User;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.base.NodeMatchers.isVisible;

@ExtendWith(ApplicationExtension.class)
public class MyBidsControllerTest {

    @Start
    public void start(Stage stage) throws Exception {
        SidebarController.isSidebarCollapsed = false;
        User.setSession(1, "testuser", "Nguyen Van A", "test@example.com", "2000-01-01", "Hanoi", "USER", null);
        
        Parent root = FXMLLoader.load(getClass().getResource("/com/auction/client/view/MyBids.fxml"));
        stage.setScene(new Scene(root, 1024, 768));
        stage.show();
    }

    @Test
    @DisplayName("Test: Product Container is visible on My Bids")
    public void should_have_product_container(FxRobot robot) {
        verifyThat("#productContainer", isVisible());
    }

    @Test
    @DisplayName("Test: Search field is visible on My Bids")
    public void should_have_search_field(FxRobot robot) {
        verifyThat("#txtSearch", isVisible());
    }

    @Test
    @DisplayName("Test: Tab buttons are visible on My Bids")
    public void should_have_tab_buttons(FxRobot robot) {
        verifyThat("#btnTabActive", isVisible());
        verifyThat("#btnTabWinning", isVisible());
        verifyThat("#btnTabOutbid", isVisible());
        verifyThat("#btnTabEnded", isVisible());
    }

    @Test
    @DisplayName("Test: Toggle tabs updates styles correctly")
    public void testToggleTabs(FxRobot robot) {
        Button btnActive = robot.lookup("#btnTabActive").queryAs(Button.class);
        Button btnWinning = robot.lookup("#btnTabWinning").queryAs(Button.class);
        Button btnOutbid = robot.lookup("#btnTabOutbid").queryAs(Button.class);
        Button btnEnded = robot.lookup("#btnTabEnded").queryAs(Button.class);

        assertNotNull(btnActive);
        assertNotNull(btnWinning);
        assertNotNull(btnOutbid);
        assertNotNull(btnEnded);

        // Click Winning Tab
        robot.clickOn(btnWinning);
        robot.sleep(200);

        // Click Outbid Tab
        robot.clickOn(btnOutbid);
        robot.sleep(200);

        // Click Ended Tab
        robot.clickOn(btnEnded);
        robot.sleep(200);

        // Click Active Tab
        robot.clickOn(btnActive);
        robot.sleep(200);
    }

    @Test
    @DisplayName("Test: Ended products won by the bidder are ordered first")
    public void endedProductsPrioritizeWins() {
        MyBidsController controller = new MyBidsController();
        JSONObject lostFirst = new JSONObject().put("id", 11).put("status", "ENDED").put("highestBidderId", 7);
        JSONObject wonSecond = new JSONObject().put("id", 12).put("status", "ENDED").put("highestBidderId", 1);
        JSONObject wonThird = new JSONObject().put("id", 13).put("status", "CLOSED").put("highestBidderId", 1);

        List<JSONObject> ordered = controller.prioritizeWonProducts(
                List.of(lostFirst, wonSecond, wonThird), 1);

        assertEquals(List.of(12, 13, 11),
                ordered.stream().map(product -> product.getInt("id")).toList());
    }
}
