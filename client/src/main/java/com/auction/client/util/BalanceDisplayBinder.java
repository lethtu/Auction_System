package com.auction.client.util;

import com.auction.client.model.User;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;

public final class BalanceDisplayBinder {
    private static final String MASKED_MONEY = "\u20ab \u2022\u2022\u2022\u2022\u2022\u2022";
    private static final String ICON_VISIBLE = "\uE8F4";
    private static final String ICON_HIDDEN = "\uE8F5";

    private BalanceDisplayBinder() {
    }

    public static void bindAvailableBalance(Label valueLabel, Button toggleButton) {
        if (valueLabel == null || toggleButton == null) {
            return;
        }

        ChangeListener<Object> listener = (observable, oldValue, newValue) ->
                runOnFxThread(() -> updateDisplay(valueLabel, toggleButton));

        User.balanceProperty().addListener(listener);
        User.frozenBalanceProperty().addListener(listener);
        User.balanceLoadedProperty().addListener(listener);
        User.balanceVisibleProperty().addListener(listener);

        toggleButton.setOnAction(event -> User.setBalanceVisible(!User.isBalanceVisible()));

        ChangeListener<Scene> sceneListener = new ChangeListener<>() {
            @Override
            public void changed(javafx.beans.value.ObservableValue<? extends Scene> observable, Scene oldScene, Scene newScene) {
                if (newScene == null) {
                    User.balanceProperty().removeListener(listener);
                    User.frozenBalanceProperty().removeListener(listener);
                    User.balanceLoadedProperty().removeListener(listener);
                    User.balanceVisibleProperty().removeListener(listener);
                    valueLabel.sceneProperty().removeListener(this);
                }
            }
        };
        valueLabel.sceneProperty().addListener(sceneListener);

        updateDisplay(valueLabel, toggleButton);
    }

    private static void updateDisplay(Label valueLabel, Button toggleButton) {
        boolean hasLoadedBalance = User.getId() != null && User.isBalanceLoaded();
        toggleButton.setDisable(!hasLoadedBalance);

        if (!hasLoadedBalance) {
            valueLabel.setText("--");
            toggleButton.setText(ICON_VISIBLE);
            toggleButton.setTooltip(new Tooltip("Balance not loaded"));
            return;
        }

        boolean visible = User.isBalanceVisible();
        valueLabel.setText(visible ? MoneyFormatUtil.formatVndPrefix(User.getAvailableBalance()) : MASKED_MONEY);
        toggleButton.setText(visible ? ICON_VISIBLE : ICON_HIDDEN);
        toggleButton.setTooltip(new Tooltip(visible ? "Hide balance" : "Show balance"));
    }

    private static void runOnFxThread(Runnable action) {
        try {
            if (Platform.isFxApplicationThread()) {
                action.run();
            } else {
                Platform.runLater(action);
            }
        } catch (IllegalStateException e) {
            action.run();
        }
    }
}
