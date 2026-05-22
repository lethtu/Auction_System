package com.auction.client.util;

import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Control;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.WeakHashMap;

/**
 * Window chrome for a JavaFX StageStyle.TRANSPARENT stage.
 *
 * <p>
 * The stage itself stays transparent. Rounded corners are drawn only by CSS
 * on the page surface (.auth-shell / .rounded-window-content). This class is
 * responsible solely for border hit-testing, drag-to-move and custom maximize.
 * </p>
 */
public final class ResizeHelper {
    private static final double RESIZE_BORDER = 10.0;
    private static final double AUTH_DRAG_BAR_HEIGHT = 68.0;
    private static final String MAXIMIZED_CLASS = "window-maximized";
    private static final String DRAG_AREA_CLASS = "window-drag-area";

    private static final WeakHashMap<Stage, WindowState> STATES = new WeakHashMap<>();

    private ResizeHelper() {
    }

    /** Installs custom drag/resize handlers for the current Scene root. */
    public static void install(Stage stage, Parent root) {
        if (stage == null || root == null) {
            return;
        }

        WindowState state = STATES.computeIfAbsent(stage, ignored -> new WindowState());
        state.stage = stage;

        // Scene switching creates a new root; do not register twice on one root.
        if (state.root == root) {
            applyMaximizedCss(root, state.maximized);
            return;
        }
        state.root = root;
        applyMaximizedCss(root, state.maximized);

        root.addEventFilter(MouseEvent.MOUSE_MOVED, event -> handleMouseMoved(stage, root, state, event));
        root.addEventFilter(MouseEvent.MOUSE_EXITED, event -> {
            if (state.operation == Operation.NONE) {
                root.setCursor(Cursor.DEFAULT);
            }
        });
        root.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> handleMousePressed(stage, root, state, event));
        root.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> handleMouseDragged(stage, root, state, event));
        root.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
            state.operation = Operation.NONE;
            state.resizeCursor = Cursor.DEFAULT;
            root.setCursor(Cursor.DEFAULT);
        });
        root.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() == MouseButton.PRIMARY
                    && event.getClickCount() == 2
                    && !isInteractiveTarget(event.getTarget())
                    && isDragArea(root, event)) {
                if (stage.isResizable()) {
                    toggleMaximize(stage);
                }
                event.consume();
            }
        });
    }

    private static void handleMouseMoved(Stage stage, Parent root, WindowState state, MouseEvent event) {
        if (state.maximized || stage.isFullScreen() || !stage.isResizable()) {
            root.setCursor(Cursor.DEFAULT);
            return;
        }
        root.setCursor(resolveResizeCursor(root, event.getSceneX(), event.getSceneY()));
    }

    private static void handleMousePressed(Stage stage, Parent root, WindowState state, MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY || stage.isFullScreen()) {
            return;
        }

        if (!state.maximized && stage.isResizable()) {
            Cursor cursor = resolveResizeCursor(root, event.getSceneX(), event.getSceneY());
            if (cursor != Cursor.DEFAULT) {
                captureInitialBounds(stage, state, event, cursor, Operation.RESIZE);
                root.setCursor(cursor);
                event.consume();
                return;
            }
        }

        if (!isInteractiveTarget(event.getTarget()) && isDragArea(root, event)) {
            if (state.maximized) {
                // Cho phép kéo title bar xuống để thoát maximize giống cửa sổ hệ điều hành.
                state.restoreFromMaximizedOnDrag = true;
                state.pointerRatioX = clamp(event.getSceneX() / Math.max(1.0, stage.getWidth()), 0.12, 0.88);
                state.pressedScreenY = event.getScreenY();
                state.operation = Operation.MOVE;
            } else {
                captureInitialBounds(stage, state, event, Cursor.DEFAULT, Operation.MOVE);
            }
            event.consume();
        }
    }

    private static void handleMouseDragged(Stage stage, Parent root, WindowState state, MouseEvent event) {
        if (!event.isPrimaryButtonDown() || stage.isFullScreen()) {
            return;
        }

        if (state.operation == Operation.RESIZE && !state.maximized) {
            resizeStage(stage, state, event.getScreenX(), event.getScreenY());
            event.consume();
            return;
        }

        if (state.operation != Operation.MOVE) {
            return;
        }

        if (state.restoreFromMaximizedOnDrag && state.maximized) {
            state.restoreFromMaximizedOnDrag = false;
            double pointerRatioX = state.pointerRatioX;
            double topOffset = Math.min(42.0, Math.max(18.0, event.getSceneY()));
            restore(stage);
            stage.setX(event.getScreenX() - stage.getWidth() * pointerRatioX);
            stage.setY(event.getScreenY() - topOffset);
            state.dragOffsetX = stage.getWidth() * pointerRatioX;
            state.dragOffsetY = topOffset;
        }

        if (!state.maximized) {
            stage.setX(event.getScreenX() - state.dragOffsetX);
            stage.setY(event.getScreenY() - state.dragOffsetY);
            event.consume();
        }
    }

    private static void captureInitialBounds(Stage stage, WindowState state, MouseEvent event,
            Cursor cursor, Operation operation) {
        state.operation = operation;
        state.resizeCursor = cursor;
        state.startScreenX = event.getScreenX();
        state.startScreenY = event.getScreenY();
        state.startStageX = stage.getX();
        state.startStageY = stage.getY();
        state.startWidth = stage.getWidth();
        state.startHeight = stage.getHeight();
        state.dragOffsetX = event.getSceneX();
        state.dragOffsetY = event.getSceneY();
        state.restoreFromMaximizedOnDrag = false;
    }

    private static Cursor resolveResizeCursor(Parent root, double sceneX, double sceneY) {
        Scene scene = root.getScene();
        double width = scene == null ? root.getLayoutBounds().getWidth() : scene.getWidth();
        double height = scene == null ? root.getLayoutBounds().getHeight() : scene.getHeight();

        boolean left = sceneX <= RESIZE_BORDER;
        boolean right = sceneX >= width - RESIZE_BORDER;
        boolean top = sceneY <= RESIZE_BORDER;
        boolean bottom = sceneY >= height - RESIZE_BORDER;

        if (top && left)
            return Cursor.NW_RESIZE;
        if (top && right)
            return Cursor.NE_RESIZE;
        if (bottom && left)
            return Cursor.SW_RESIZE;
        if (bottom && right)
            return Cursor.SE_RESIZE;
        if (left)
            return Cursor.W_RESIZE;
        if (right)
            return Cursor.E_RESIZE;
        if (top)
            return Cursor.N_RESIZE;
        if (bottom)
            return Cursor.S_RESIZE;
        return Cursor.DEFAULT;
    }

    /**
     * Math cho bốn cạnh và bốn góc. Với LEFT/TOP phải cập nhật cả X/Y,
     * nếu không nội dung sẽ giật/nhảy trong lúc resize.
     */
    private static void resizeStage(Stage stage, WindowState state, double screenX, double screenY) {
        double deltaX = screenX - state.startScreenX;
        double deltaY = screenY - state.startScreenY;
        Cursor cursor = state.resizeCursor;
        double minWidth = Math.max(1.0, stage.getMinWidth());
        double minHeight = Math.max(1.0, stage.getMinHeight());

        if (cursor == Cursor.E_RESIZE || cursor == Cursor.NE_RESIZE || cursor == Cursor.SE_RESIZE) {
            stage.setWidth(Math.max(minWidth, state.startWidth + deltaX));
        }
        if (cursor == Cursor.S_RESIZE || cursor == Cursor.SE_RESIZE || cursor == Cursor.SW_RESIZE) {
            stage.setHeight(Math.max(minHeight, state.startHeight + deltaY));
        }
        if (cursor == Cursor.W_RESIZE || cursor == Cursor.NW_RESIZE || cursor == Cursor.SW_RESIZE) {
            double newWidth = Math.max(minWidth, state.startWidth - deltaX);
            stage.setX(state.startStageX + state.startWidth - newWidth);
            stage.setWidth(newWidth);
        }
        if (cursor == Cursor.N_RESIZE || cursor == Cursor.NW_RESIZE || cursor == Cursor.NE_RESIZE) {
            double newHeight = Math.max(minHeight, state.startHeight - deltaY);
            stage.setY(state.startStageY + state.startHeight - newHeight);
            stage.setHeight(newHeight);
        }
    }

    public static void toggleMaximize(Stage stage) {
        if (stage == null || !stage.isResizable()) {
            return;
        }
        if (isMaximized(stage)) {
            restore(stage);
        } else {
            maximize(stage);
        }
    }

    public static boolean isMaximized(Stage stage) {
        WindowState state = STATES.get(stage);
        return state != null && state.maximized;
    }

    /**
     * Custom maximize; never call Stage#setMaximized(true) on the transparent main
     * stage.
     */
    public static void maximize(Stage stage) {
        if (stage == null) {
            return;
        }
        WindowState state = STATES.computeIfAbsent(stage, ignored -> new WindowState());
        state.stage = stage;
        if (!state.maximized) {
            state.restoreX = stage.getX();
            state.restoreY = stage.getY();
            state.restoreWidth = stage.getWidth();
            state.restoreHeight = stage.getHeight();
        }

        // Nếu có code cũ từng bật native maximize, hủy trạng thái đó trước.
        if (stage.isMaximized()) {
            stage.setMaximized(false);
        }
        state.maximized = true;
        Rectangle2D bounds = screenBounds(stage);
        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());
        applyMaximizedCss(state.root, true);
    }

    public static void restore(Stage stage) {
        WindowState state = STATES.get(stage);
        if (stage == null || state == null || !state.maximized) {
            return;
        }
        state.maximized = false;
        stage.setX(state.restoreX);
        stage.setY(state.restoreY);
        stage.setWidth(Math.max(stage.getMinWidth(), state.restoreWidth));
        stage.setHeight(Math.max(stage.getMinHeight(), state.restoreHeight));
        applyMaximizedCss(state.root, false);
    }

    /** Re-applies bounds/CSS after switching the FXML root while maximized. */
    public static void reapplyMaximizedState(Stage stage) {
        WindowState state = STATES.get(stage);
        if (state == null) {
            return;
        }
        applyMaximizedCss(state.root, state.maximized);
        if (state.maximized) {
            Rectangle2D bounds = screenBounds(stage);
            stage.setX(bounds.getMinX());
            stage.setY(bounds.getMinY());
            stage.setWidth(bounds.getWidth());
            stage.setHeight(bounds.getHeight());
        }
    }

    private static Rectangle2D screenBounds(Stage stage) {
        var screens = Screen.getScreensForRectangle(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
        Screen target = screens.isEmpty() ? Screen.getPrimary() : screens.get(0);
        return target.getVisualBounds();
    }

    private static void applyMaximizedCss(Parent root, boolean maximized) {
        if (root == null) {
            return;
        }
        root.getStyleClass().remove(MAXIMIZED_CLASS);
        if (maximized) {
            root.getStyleClass().add(MAXIMIZED_CLASS);
        }
    }

    private static boolean isDragArea(Parent root, MouseEvent event) {
        Node node = event.getPickResult().getIntersectedNode();
        while (node != null) {
            if (node.getStyleClass().contains(DRAG_AREA_CLASS)
                    || node.getStyleClass().contains("app-topbar")
                    || node.getStyleClass().contains("app-topbar-even")
                    || node.getStyleClass().contains("admin-bidpop-topbar")) {
                return true;
            }
            if (node == root) {
                break;
            }
            node = node.getParent();
        }
        return root.getStyleClass().contains("auth-root")
                && event.getSceneY() >= RESIZE_BORDER
                && event.getSceneY() <= AUTH_DRAG_BAR_HEIGHT;
    }

    private static boolean isInteractiveTarget(Object target) {
        if (!(target instanceof Node node)) {
            return false;
        }
        Node current = node;
        while (current != null) {
            if (current instanceof Control) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private enum Operation {
        NONE, MOVE, RESIZE
    }

    private static final class WindowState {
        private Stage stage;
        private Parent root;
        private Operation operation = Operation.NONE;
        private Cursor resizeCursor = Cursor.DEFAULT;
        private boolean maximized;
        private boolean restoreFromMaximizedOnDrag;
        private double pointerRatioX;
        private double pressedScreenY;
        private double startScreenX;
        private double startScreenY;
        private double startStageX;
        private double startStageY;
        private double startWidth;
        private double startHeight;
        private double dragOffsetX;
        private double dragOffsetY;
        private double restoreX;
        private double restoreY;
        private double restoreWidth;
        private double restoreHeight;
    }
}
