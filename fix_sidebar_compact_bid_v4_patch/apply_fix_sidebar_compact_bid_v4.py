from pathlib import Path
import re
import shutil
from datetime import datetime

ROOT = Path.cwd()
BACKUP = ROOT / f"backup_fix_sidebar_compact_bid_v4_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
BACKUP.mkdir(exist_ok=True)

def p(rel):
    return ROOT / rel

def backup(path: Path):
    if path.exists():
        shutil.copy2(path, BACKUP / path.name)

def read(path: Path):
    return path.read_text(encoding='utf-8-sig')

def write(path: Path, text: str):
    path.write_text(text.lstrip('\ufeff'), encoding='utf-8')

def replace_method(text: str, signature_pattern: str, new_method: str, label: str) -> str:
    m = re.search(signature_pattern, text, flags=re.MULTILINE)
    if not m:
        raise SystemExit(f"Không tìm thấy method để sửa: {label}")
    start = m.start()
    brace = text.find('{', m.end() - 1)
    if brace == -1:
        raise SystemExit(f"Không tìm thấy dấu mở method: {label}")
    depth = 0
    end = None
    for i in range(brace, len(text)):
        ch = text[i]
        if ch == '{':
            depth += 1
        elif ch == '}':
            depth -= 1
            if depth == 0:
                end = i + 1
                break
    if end is None:
        raise SystemExit(f"Không tìm thấy điểm kết thúc method: {label}")
    return text[:start] + new_method.strip() + "\n\n" + text[end:]


def patch_sidebar():
    path = p('client/src/main/java/com/auction/client/controller/SidebarController.java')
    backup(path)
    text = read(path)

    dashboard = '''
    @FXML
    public void handleDashboard(ActionEvent event) {
        autoCollapse();
        setActiveDashboard();
        try {
            Stage stage = resolveStage(event);
            boolean wasMaximized = stage != null && stage.isMaximized();
            int currentWidth = stage == null ? 1280 : Math.max(1280, (int) Math.round(stage.getWidth()));
            int currentHeight = stage == null ? 800 : Math.max(800, (int) Math.round(stage.getHeight()));

            if (onBeforeNavigate != null) onBeforeNavigate.run();
            MainController.initialShowWatchlist = false;
            MainController.initialHomeFilterMode = "ALL";
            SceneSwitcher.switchScene(event, "MainTemplate.fxml", currentWidth, currentHeight);

            if (stage != null && wasMaximized) {
                Platform.runLater(() -> stage.setMaximized(true));
            }
        } catch (IOException e) {
            logger.error("Lỗi chuyển cảnh về Dashboard: ", e);
            showInfo("Dashboard", "Không thể mở Dashboard. Vui lòng thử lại.");
        }
    }
'''
    watchlist = '''
    @FXML
    public void handleWatchlist(ActionEvent event) {
        autoCollapse();
        setActiveWatchlist();
        try {
            Stage stage = resolveStage(event);
            boolean wasMaximized = stage != null && stage.isMaximized();
            int currentWidth = stage == null ? 1280 : Math.max(1280, (int) Math.round(stage.getWidth()));
            int currentHeight = stage == null ? 800 : Math.max(800, (int) Math.round(stage.getHeight()));

            if (onBeforeNavigate != null) onBeforeNavigate.run();
            MainController.initialShowWatchlist = true;
            MainController.initialHomeFilterMode = "WATCHLIST";
            SceneSwitcher.switchScene(event, "MainTemplate.fxml", currentWidth, currentHeight);

            if (stage != null && wasMaximized) {
                Platform.runLater(() -> stage.setMaximized(true));
            }
        } catch (IOException e) {
            logger.error("Lỗi chuyển cảnh sang Watchlist: ", e);
            showInfo("Watchlist", "Không thể mở Watchlist. Vui lòng thử lại.");
        }
    }
'''
    text = replace_method(text, r'\s*@FXML\s+public\s+void\s+handleDashboard\s*\(\s*ActionEvent\s+event\s*\)', dashboard, 'handleDashboard')
    text = replace_method(text, r'\s*@FXML\s+public\s+void\s+handleWatchlist\s*\(\s*ActionEvent\s+event\s*\)', watchlist, 'handleWatchlist')
    write(path, text)
    print('Patched SidebarController navigation')


def patch_main_template():
    path = p('client/src/main/java/com/auction/client/view/MainTemplate.fxml')
    backup(path)
    text = read(path)
    start = text.find('<HBox fx:id="filterControlsBox"')
    if start == -1:
        raise SystemExit('Không tìm thấy filterControlsBox trong MainTemplate.fxml')
    end = text.find('</HBox>', start)
    if end == -1:
        raise SystemExit('Không tìm thấy </HBox> của filterControlsBox')
    end += len('</HBox>')
    block = '''<HBox fx:id="filterControlsBox" spacing="12.0" alignment="CENTER_RIGHT" styleClass="filter-controls-bar">
                    <Label text="Lọc:" textFill="#604868" styleClass="filter-label" />
                    <ComboBox fx:id="cbCategory" promptText="Thể loại" prefWidth="150.0" styleClass="filter-combo" />
                    <ComboBox fx:id="cbStatus" promptText="Trạng thái" prefWidth="150.0" styleClass="filter-combo" />
                    <Button fx:id="btnToggleProductView"
                            mnemonicParsing="false"
                            onAction="#handleToggleProductView"
                            styleClass="view-toggle-button"
                            text="▦"
                            alignment="CENTER"
                            contentDisplay="CENTER"
                            textAlignment="CENTER" />
                </HBox>'''
    text = text[:start] + block + text[end:]
    write(path, text)
    print('Patched MainTemplate filter/toggle area')


def patch_main_controller():
    path = p('client/src/main/java/com/auction/client/controller/MainController.java')
    backup(path)
    text = read(path)

    lines = text.splitlines()
    seen = False
    new_lines = []
    for line in lines:
        if re.match(r'\s*private\s+boolean\s+compactProductListMode\s*=\s*false\s*;\s*$', line):
            if seen:
                continue
            seen = True
        new_lines.append(line)
    text = '\n'.join(new_lines) + '\n'

    toggle = '''
    @FXML
    private void handleToggleProductView(ActionEvent event) {
        compactProductListMode = !compactProductListMode;

        if (compactProductListMode) {
            showCompactAuctionList();
        } else {
            returnToAuctionGrid();
        }

        updateViewToggleButton(compactProductListMode);
    }
'''
    if re.search(r'\s*@FXML\s+private\s+void\s+handleToggleProductView\s*\(\s*ActionEvent\s+event\s*\)', text):
        text = replace_method(text, r'\s*@FXML\s+private\s+void\s+handleToggleProductView\s*\(\s*ActionEvent\s+event\s*\)', toggle, 'handleToggleProductView')
    else:
        idx = text.rfind('}')
        text = text[:idx] + '\n' + toggle + '\n' + text[idx:]

    compact_row = '''
    private HBox createCompactAuctionRow(int index, JSONObject sessionObj) {
        JSONObject itemObj = getItemObject(sessionObj);
        int sessionId = sessionObj.optInt("id");
        String itemName = itemObj.optString("name", "Không tên");
        String itemType = itemObj.optString("type", "Không rõ danh mục");
        BigDecimal currentPrice = getMoney(sessionObj, "currentPrice", getMoney(sessionObj, "startingPrice", BigDecimal.ZERO));
        String status = sessionObj.optString("status", "UNKNOWN");
        boolean canBid = "ACTIVE".equalsIgnoreCase(status);

        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14, 18, 14, 18));
        row.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 18px; -fx-border-color: #ffe8e8; -fx-border-width: 1.5px; -fx-border-radius: 18px;");

        Label order = new Label(String.valueOf(index));
        order.setAlignment(Pos.CENTER);
        order.setMinSize(34, 34);
        order.setPrefSize(34, 34);
        order.setStyle("-fx-background-color: #ffd6ee; -fx-background-radius: 17px; -fx-font-family: 'DM Sans'; -fx-font-size: 13px; -fx-font-weight: 900; -fx-text-fill: #e040a0;");

        VBox infoBox = new VBox(3);
        Label name = new Label("#" + sessionId + " · " + itemName);
        name.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 15px; -fx-font-weight: 900; -fx-text-fill: #2e1a28;");
        Label type = new Label(itemType);
        type.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 12px; -fx-text-fill: #907898;");
        infoBox.getChildren().addAll(name, type);

        Region rowSpacer = new Region();
        HBox.setHgrow(rowSpacer, Priority.ALWAYS);

        Label statusBadge = new Label(status);
        statusBadge.setStyle("-fx-background-color: #f2e8f2; -fx-background-radius: 999; -fx-padding: 5 12 5 12; -fx-font-family: 'DM Sans'; -fx-font-size: 11px; -fx-font-weight: 900; -fx-text-fill: #604868;");

        Label price = new Label("₫ " + formatPrice(currentPrice));
        price.setMinWidth(110);
        price.setAlignment(Pos.CENTER_RIGHT);
        price.setStyle("-fx-font-family: 'DM Sans'; -fx-font-size: 16px; -fx-font-weight: 900; -fx-text-fill: #e040a0;");

        Button bidButton = new Button(canBid ? "Đấu giá" : "Hết giờ");
        bidButton.setMinWidth(92);
        bidButton.setPrefHeight(38);
        bidButton.setDisable(!canBid);
        if (canBid) {
            bidButton.setStyle("-fx-background-color: #e040a0; -fx-background-radius: 999; -fx-text-fill: white; -fx-font-family: 'DM Sans'; -fx-font-size: 13px; -fx-font-weight: 900; -fx-padding: 8 18 8 18; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(224,64,160,0.25), 10, 0, 0, 3);");
            bidButton.setOnAction(event -> {
                ClientLogger.logViewHistory(User.getUsername(), itemName, sessionId, currentPrice);
                try {
                    FXMLLoader loader = SceneSwitcher.switchScene(event, "AuctionPage.fxml", 1280, 800);
                    AuctionPageController controller = loader.getController();
                    controller.setItem(sessionObj, itemObj);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } else {
            bidButton.setStyle("-fx-background-color: #f2e8f2; -fx-background-radius: 999; -fx-text-fill: #907898; -fx-font-family: 'DM Sans'; -fx-font-size: 13px; -fx-font-weight: 900; -fx-padding: 8 18 8 18;");
        }

        row.getChildren().addAll(order, infoBox, rowSpacer, statusBadge, price, bidButton);
        return row;
    }
'''
    text = replace_method(text, r'\s*private\s+HBox\s+createCompactAuctionRow\s*\(\s*int\s+index\s*,\s*JSONObject\s+sessionObj\s*\)', compact_row, 'createCompactAuctionRow')

    update_method = '''
    private void updateViewToggleButton(boolean compactMode) {
        if (btnToggleProductView == null) {
            return;
        }

        btnToggleProductView.getStyleClass().remove("view-toggle-button-active");
        if (compactMode) {
            btnToggleProductView.getStyleClass().add("view-toggle-button-active");
            btnToggleProductView.setTooltip(new Tooltip("Đang xem dạng danh sách. Bấm để về dạng thẻ."));
        } else {
            btnToggleProductView.setTooltip(new Tooltip("Xem dạng danh sách rút gọn"));
        }

        btnToggleProductView.setText("▦");
    }
'''
    if re.search(r'\s*private\s+void\s+updateViewToggleButton\s*\(\s*boolean\s+compactMode\s*\)', text):
        text = replace_method(text, r'\s*private\s+void\s+updateViewToggleButton\s*\(\s*boolean\s+compactMode\s*\)', update_method, 'updateViewToggleButton')

    write(path, text)
    print('Patched MainController compact toggle and compact bid action')


def patch_css():
    path = p('client/src/main/java/com/auction/client/view/styles.css')
    backup(path)
    text = read(path)
    addition = '''

/* Final bidder dashboard toggle/filter polish */
.filter-controls-bar {
    -fx-alignment: center-right;
}

.filter-label {
    -fx-font-family: 'DM Sans';
    -fx-font-size: 13px;
    -fx-font-weight: 900;
    -fx-text-fill: #604868;
}

.filter-combo {
    -fx-background-color: #fff7fd;
    -fx-border-color: #ffdceb;
    -fx-border-width: 1.5px;
    -fx-border-radius: 999px;
    -fx-background-radius: 999px;
    -fx-padding: 0 8px 0 8px;
    -fx-font-family: 'DM Sans';
    -fx-font-size: 13px;
    -fx-text-fill: #2e1a28;
}

.filter-combo .list-cell {
    -fx-text-fill: #2e1a28;
    -fx-font-family: 'DM Sans';
    -fx-font-size: 13px;
    -fx-padding: 7px 12px;
}

.combo-box-popup .list-view {
    -fx-background-color: white;
    -fx-border-color: #ffdceb;
    -fx-border-radius: 14px;
    -fx-background-radius: 14px;
    -fx-effect: dropshadow(three-pass-box, rgba(224, 64, 160, 0.16), 14, 0, 0, 4);
}

.combo-box-popup .list-cell:hover,
.combo-box-popup .list-cell:selected {
    -fx-background-color: #ffeaf7;
    -fx-text-fill: #e040a0;
}

.view-toggle-button {
    -fx-min-width: 52px;
    -fx-pref-width: 52px;
    -fx-max-width: 52px;
    -fx-min-height: 52px;
    -fx-pref-height: 52px;
    -fx-max-height: 52px;
    -fx-background-color: #ffffff;
    -fx-border-color: #f1ddea;
    -fx-border-width: 1.5px;
    -fx-background-radius: 26px;
    -fx-border-radius: 26px;
    -fx-text-fill: #907898;
    -fx-font-family: 'Material Symbols Outlined';
    -fx-font-size: 21px;
    -fx-font-weight: normal;
    -fx-padding: 0;
    -fx-alignment: center;
    -fx-content-display: center;
    -fx-text-alignment: center;
    -fx-cursor: hand;
}

.view-toggle-button:hover,
.view-toggle-button-active {
    -fx-background-color: #ffeaf7;
    -fx-border-color: #ff9bd5;
    -fx-text-fill: #e040a0;
    -fx-effect: dropshadow(three-pass-box, rgba(224, 64, 160, 0.18), 14, 0, 0, 4);
}
'''
    if 'Final bidder dashboard toggle/filter polish' not in text:
        text += addition
    write(path, text)
    print('Patched styles.css final toggle/filter CSS')


def main():
    print(f'Project root: {ROOT}')
    print(f'Backup folder: {BACKUP}')
    patch_sidebar()
    patch_main_template()
    patch_main_controller()
    patch_css()
    target = ROOT / 'client' / 'target'
    if target.exists():
        shutil.rmtree(target, ignore_errors=True)
        print('Removed client/target')
    print('DONE: fixed sidebar Dashboard/Watchlist navigation + compact list bidding + single centered toggle button.')

if __name__ == '__main__':
    main()
