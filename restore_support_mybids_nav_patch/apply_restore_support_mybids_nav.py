from pathlib import Path
from datetime import datetime
import shutil
import re

ROOT = Path.cwd()
backup = ROOT / f"backup_restore_support_mybids_nav_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
backup.mkdir(exist_ok=True)

sidebar_path = ROOT / "client/src/main/java/com/auction/client/controller/SidebarController.java"
seller_path = ROOT / "client/src/main/java/com/auction/client/controller/SellerDashboardController.java"

for p in (sidebar_path, seller_path):
    if not p.exists():
        raise SystemExit(f"Không tìm thấy file cần sửa: {p}")
    shutil.copy2(p, backup / p.name)


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def write_text(path: Path, text: str) -> None:
    path.write_text(text, encoding="utf-8", newline="")


def find_method_bounds(text: str, method_name: str):
    m = re.search(r"@FXML\s+public\s+void\s+" + re.escape(method_name) + r"\s*\([^)]*\)\s*\{", text)
    if not m:
        m = re.search(r"public\s+void\s+" + re.escape(method_name) + r"\s*\([^)]*\)\s*\{", text)
    if not m:
        raise SystemExit(f"Không tìm thấy method {method_name}")
    start = m.start()
    brace = text.find("{", m.end() - 1)
    depth = 0
    i = brace
    in_str = None
    esc = False
    while i < len(text):
        ch = text[i]
        if in_str:
            if esc:
                esc = False
            elif ch == "\\":
                esc = True
            elif ch == in_str:
                in_str = None
        else:
            if ch in ('"', "'"):
                in_str = ch
            elif ch == "{":
                depth += 1
            elif ch == "}":
                depth -= 1
                if depth == 0:
                    return start, i + 1
        i += 1
    raise SystemExit(f"Không tìm thấy dấu đóng }} cho method {method_name}")

# 1) Restore Support sidebar button to open Support.fxml page instead of old TextInputDialog popup
sidebar = read_text(sidebar_path)
new_support = '''    @FXML
    public void handleSupport(ActionEvent event) {
        autoCollapse();
        setActiveSupport();
        try {
            Stage stage = resolveStage(event);
            boolean wasMaximized = stage != null && stage.isMaximized();
            int currentWidth = stage == null ? 1280 : Math.max(1280, (int) Math.round(stage.getWidth()));
            int currentHeight = stage == null ? 800 : Math.max(800, (int) Math.round(stage.getHeight()));

            if (onBeforeNavigate != null) onBeforeNavigate.run();
            SceneSwitcher.switchScene(event, "Support.fxml", currentWidth, currentHeight);

            if (stage != null && wasMaximized) {
                Platform.runLater(() -> stage.setMaximized(true));
            }
        } catch (IOException e) {
            logger.error("Lỗi chuyển cảnh sang Support: ", e);
            showInfo("Support", "Không thể mở màn hỗ trợ. Vui lòng thử lại.");
        }
    }
'''
start, end = find_method_bounds(sidebar, "handleSupport")
sidebar = sidebar[:start] + new_support + sidebar[end:]
# Remove TextInputDialog/Optional imports if they are now unused; harmless if not present.
sidebar = sidebar.replace("import javafx.scene.control.TextInputDialog;\n", "")
if "Optional<" not in sidebar and "Optional." not in sidebar and "Optional" not in sidebar.split("class SidebarController", 1)[0]:
    sidebar = sidebar.replace("import java.util.Optional;\n", "")
write_text(sidebar_path, sidebar)
print("Patched Support navigation in SidebarController.java")

# 2) Fix My Bids from SellerDashboard sidebar: it must leave SellerDashboard and open MainTemplate in MY_BIDS mode
seller = read_text(seller_path)
if "void onFilterMyBids(javafx.event.ActionEvent event)" not in seller and "void onFilterMyBids(ActionEvent event)" not in seller:
    target = '''                @Override
                public void onFilterWatchlist(javafx.event.ActionEvent event) {
                    try {
                        MainController.initialShowWatchlist = true;
                        SceneSwitcher.switchScene(event, "MainTemplate.fxml", 1280, 800);
                    } catch (IOException e) {
                        logger.error("Lỗi điều hướng:", e);
                    }
                }
'''
    insert = target + '''
                @Override
                public void onFilterMyBids(javafx.event.ActionEvent event) {
                    try {
                        MainController.initialHomeFilterMode = "MY_BIDS";
                        SceneSwitcher.switchScene(event, "MainTemplate.fxml", 1280, 800);
                    } catch (IOException e) {
                        logger.error("Lỗi điều hướng sang My Bids:", e);
                    }
                }
'''
    if target not in seller:
        # Fallback: insert before onResetFilter inside SidebarListener block.
        marker = '''                @Override
                public void onResetFilter(javafx.event.ActionEvent event) {'''
        if marker not in seller:
            raise SystemExit("Không tìm thấy vị trí chèn onFilterMyBids trong SellerDashboardController")
        seller = seller.replace(marker, '''                @Override
                public void onFilterMyBids(javafx.event.ActionEvent event) {
                    try {
                        MainController.initialHomeFilterMode = "MY_BIDS";
                        SceneSwitcher.switchScene(event, "MainTemplate.fxml", 1280, 800);
                    } catch (IOException e) {
                        logger.error("Lỗi điều hướng sang My Bids:", e);
                    }
                }

''' + marker, 1)
    else:
        seller = seller.replace(target, insert, 1)
    print("Patched My Bids navigation in SellerDashboardController.java")
else:
    print("SellerDashboardController.java already has onFilterMyBids; skipped insertion")
write_text(seller_path, seller)

# Clean stale target so JavaFX/FXML/CSS cache does not lie.
for rel in ("client/target",):
    target = ROOT / rel
    if target.exists():
        shutil.rmtree(target, ignore_errors=True)
        print(f"Removed stale target: {rel}")

print(f"Backup folder: {backup}")
print("DONE. Run client tests and check Support/My Bids navigation.")
