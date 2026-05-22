import os

files_to_clean = {
    r"client\src\main\java\com\auction\client\controller\MyBidsController.java": ["import javafx.scene.Cursor;", "import com.auction.client.HttpClientSingleton;", "import javafx.geometry.Bounds;", "import com.auction.client.service.ClientLogger;"],
    r"client\src\main\java\com\auction\client\controller\NotificationCenterController.java": ["import javafx.scene.layout.Region;"],
    r"client\src\main\java\com\auction\client\controller\SellerDashboardController.java": ["import java.time.LocalDate;", "import java.time.format.DateTimeFormatter;", "import javafx.util.StringConverter;"],
    r"client\src\main\java\com\auction\client\controller\SettingsController.java": ["import javafx.geometry.Pos;", "import javafx.stage.Stage;"],
    r"client\src\main\java\com\auction\client\controller\UpToSellerController.java": ["import javafx.geometry.Insets;", "import javafx.geometry.Bounds;", "import javafx.geometry.Pos;", "import javafx.scene.layout.VBox;", "import javafx.animation.PauseTransition;", "import javafx.util.Duration;", "import java.util.HashMap;", "import java.util.Map;"],
    r"client\src\main\java\com\auction\client\dto\BidRequest.java": ["import com.auction.client.model.Bidder;", "import com.auction.client.model.User;"],
    r"client\src\main\java\com\auction\client\util\AlertUtil.java": ["import javafx.scene.control.ButtonType;"],
    r"client\src\main\java\com\auction\client\util\SettingsDialog.java": ["import javafx.scene.paint.Color;", "import javafx.scene.text.Font;", "import javafx.scene.text.FontWeight;"],
    r"client\src\test\java\com\auction\client\controller\AuctionPageControllerTest.java": ["import javafx.scene.layout.VBox;"],
    r"client\src\test\java\com\auction\client\controller\MainControllerTest.java": ["import javafx.scene.control.Label;", "import org.testfx.api.FxAssert;"],
    r"client\src\test\java\com\auction\client\controller\MyBidsControllerTest.java": ["import org.testfx.matcher.base.NodeMatchers;", "import org.testfx.matcher.control.LabeledMatchers;"],
    r"client\src\test\java\com\auction\client\controller\SellerDashboardControllerTest.java": ["import org.json.JSONArray;", "import org.json.JSONObject;", "import org.testfx.api.FxAssert;", "import java.time.LocalDateTime;"],
    r"server\src\main\java\com\auction\server\controller\BidderController.java": ["import com.auction.server.model.*;", "import com.auction.server.model.User;", "import com.auction.server.model.Bidder;", "import java.time.LocalDateTime;"],
    r"server\src\main\java\com\auction\server\dto\BidRequest.java": ["import com.auction.server.model.AuctionSession;", "import com.auction.server.model.Bidder;", "import com.auction.server.model.User;"],
    r"server\src\main\java\com\auction\server\util\SellerSessionGuard.java": ["import com.auction.server.model.AuctionStatus;"],
    r"server\src\test\java\com\auction\server\controller\AuthForgotpassTest.java": ["import org.mockito.ArgumentMatchers.any;"],
    r"server\src\test\java\com\auction\server\mapper\SessionResponseMapperTest.java": ["import com.auction.server.model.AuctionStatus;", "import com.auction.server.model.Seller;", "import java.time.LocalDateTime;"],
    r"server\src\test\java\com\auction\server\service\AdminServiceExtraTest.java": ["import com.auction.server.dto.SessionResponseDTO;"],
    r"server\src\test\java\com\auction\server\util\SellerSessionGuardTest.java": ["import org.junit.jupiter.api.Assertions.assertDoesNotThrow;"],
    r"server\src\test\java\com\auction\server\view\EmailServerTest.java": ["import org.mockito.Mockito;"]
}

for rel_path, imports_to_remove in files_to_clean.items():
    filepath = os.path.join(r"c:\Users\Khanh\Documents\GitHub\Auction_System", rel_path)
    if os.path.exists(filepath):
        with open(filepath, 'r', encoding='utf-8') as f:
            lines = f.readlines()
        
        new_lines = []
        for line in lines:
            if not any(imp in line for imp in imports_to_remove):
                new_lines.append(line)
                
        if len(new_lines) != len(lines):
            with open(filepath, 'w', encoding='utf-8') as f:
                f.writelines(new_lines)
            print(f"Cleaned {filepath}")
