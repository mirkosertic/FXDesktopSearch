/*
 * Copyright (c) 2013 by Gerrit Grunwald
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.hansolo.enzo.notification;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;


/**
 * Created by
 * User: hansolo
 * Date: 01.07.13
 * Time: 07:10
 */
public class Notification {
    public static final Image INFO_ICON    = new Image(Notifier.class.getResourceAsStream("info.png"));
    public static final Image WARNING_ICON = new Image(Notifier.class.getResourceAsStream("warning.png"));
    public static final Image SUCCESS_ICON = new Image(Notifier.class.getResourceAsStream("success.png"));
    public static final Image ERROR_ICON   = new Image(Notifier.class.getResourceAsStream("error.png"));
    public final String       TITLE;
    public final String       MESSAGE;
    public final Image        IMAGE;


    // ******************** Constructors **************************************
    public Notification(final String TITLE, final String MESSAGE) {
        this(TITLE, MESSAGE, null);
    }
    public Notification(final String MESSAGE, final Image IMAGE) {
        this("", MESSAGE, IMAGE);
    }
    public Notification(final String TITLE, final String MESSAGE, final Image IMAGE) {
        this.TITLE   = TITLE;
        this.MESSAGE = MESSAGE;
        this.IMAGE   = IMAGE;
    }


    // ******************** Inner Classes *************************************
    public enum Notifier {
        INSTANCE;

        private static final double ICON_WIDTH    = 24;
        private static final double ICON_HEIGHT   = 24;
        private static       double width         = 300;
        private static       double height        = 80;
        private static       double offsetX       = 0;
        private static       double offsetY       = 25;
        private static       double spacingY      = 5;
        private static       Pos    popupLocation = Pos.TOP_RIGHT;
        private static       Stage  stageRef      = null;
        private Duration              popupLifetime;
        private Stage                 stage;
        private Scene                 scene;
        private ObservableList<Popup> popups;


        // ******************** Constructor ***************************************
        Notifier() {
            init();
            initGraphics();
        }


        // ******************** Initialization ************************************
        private void init() {
            popupLifetime = Duration.millis(5000);
            popups = FXCollections.observableArrayList();
        }

        private void initGraphics() {
            scene = new Scene(new Region());
            scene.setFill(null);
            scene.getStylesheets().add(getClass().getResource("notifier.css").toExternalForm());

            stage = new Stage();
            stage.initStyle(StageStyle.TRANSPARENT);
            stage.setScene(scene);
        }


        // ******************** Methods *******************************************
        /**
         * @param STAGE_REF  The Notification will be positioned relative to the given Stage.<br>
         * 					If null then the Notification will be positioned relative to the primary Screen.
         * @param POPUP_LOCATION  The default is TOP_RIGHT of primary Screen.
         */
        public static void setPopupLocation(final Stage STAGE_REF, final Pos POPUP_LOCATION) {
            if (null != STAGE_REF) {
                INSTANCE.stage.initOwner(STAGE_REF);
                Notifier.stageRef = STAGE_REF;
            }
            Notifier.popupLocation = POPUP_LOCATION;
        }

        /**
         * Sets the Notification's owner stage so that when the owner
         * stage is closed Notifications will be shut down as well.<br>
         * This is only needed if <code>setPopupLocation</code> is called
         * <u>without</u> a stage reference.
         * @param OWNER
         */
        public static void setNotificationOwner(final Stage OWNER) {
            INSTANCE.stage.initOwner(OWNER);
        }

        /**
         * @param OFFSET_X  The horizontal shift required.
         * <br> The default is 0 px.
         */
        public static void setOffsetX(final double OFFSET_X) {
            Notifier.offsetX = OFFSET_X;
        }

        /**
         * @param OFFSET_Y  The vertical shift required.
         * <br> The default is 25 px.
         */
        public static void setOffsetY(final double OFFSET_Y) {
            Notifier.offsetY = OFFSET_Y;
        }

        /**
         * @param WIDTH  The default is 300 px.
         */
        public static void setWidth(final double WIDTH) {
            Notifier.width = WIDTH;
        }

        /**
         * @param HEIGHT  The default is 80 px.
         */
        public static void setHeight(final double HEIGHT) {
            Notifier.height = HEIGHT;
        }

        /**
         * @param SPACING_Y  The spacing between multiple Notifications.
         * <br> The default is 5 px.
         */
        public static void setSpacingY(final double SPACING_Y) {
            Notifier.spacingY = SPACING_Y;
        }

        public void stop() {
            popups.clear();
            stage.close();
        }

        /**
         * Returns the Duration that the notification will stay on screen before it
         * will fade out.
         * @return the Duration the popup notification will stay on screen
         */
        public Duration getPopupLifetime() {
            return popupLifetime;
        }

        /**
         * Defines the Duration that the popup notification will stay on screen before it
         * will fade out. The parameter is limited to values between 2 and 20 seconds.
         * @param POPUP_LIFETIME
         */
        public void setPopupLifetime(final Duration POPUP_LIFETIME) {
            popupLifetime = Duration.millis(clamp(2000, 20000, POPUP_LIFETIME.toMillis()));
        }

        /**
         * Show the given Notification on the screen
         * @param NOTIFICATION
         */
        public void notify(final Notification NOTIFICATION) {
            preOrder();
            showPopup(NOTIFICATION);
        }

        /**
         * Show a Notification with the given parameters on the screen
         * @param TITLE
         * @param MESSAGE
         * @param IMAGE
         */
        public void notify(final String TITLE, final String MESSAGE, final Image IMAGE) {
            notify(new Notification(TITLE, MESSAGE, IMAGE));
        }

        /**
         * Show a Notification with the given title and message and an Info icon
         * @param TITLE
         * @param MESSAGE
         */
        public void notifyInfo(final String TITLE, final String MESSAGE) {
            notify(new Notification(TITLE, MESSAGE, Notification.INFO_ICON));
        }

        /**
         * Show a Notification with the given title and message and a Warning icon
         * @param TITLE
         * @param MESSAGE
         */
        public void notifyWarning(final String TITLE, final String MESSAGE) {
            notify(new Notification(TITLE, MESSAGE, Notification.WARNING_ICON));
        }

        /**
         * Show a Notification with the given title and message and a Checkmark icon
         * @param TITLE
         * @param MESSAGE
         */
        public void notifySuccess(final String TITLE, final String MESSAGE) {
            notify(new Notification(TITLE, MESSAGE, Notification.SUCCESS_ICON));
        }

        /**
         * Show a Notification with the given title and message and an Error icon
         * @param TITLE
         * @param MESSAGE
         */
        public void notifyError(final String TITLE, final String MESSAGE) {
            notify(new Notification(TITLE, MESSAGE, Notification.ERROR_ICON));
        }

        /**
         * Makes sure that the given VALUE is within the range of MIN to MAX
         * @param MIN
         * @param MAX
         * @param VALUE
         * @return
         */
        private double clamp(final double MIN, final double MAX, final double VALUE) {
            if (VALUE < MIN) return MIN;
            if (VALUE > MAX) return MAX;
            return VALUE;
        }

        /**
         * Reorder the popup Notifications on screen so that the latest Notification will stay on top
         */
        private void preOrder() {
            if (popups.isEmpty()) return;
            for (Popup popup : popups) {
                switch (popupLocation) {
                    case TOP_LEFT:
                    case TOP_CENTER:
                    case TOP_RIGHT:
                        popup.setY(popup.getY() + height + spacingY);
                        break;
                    default:
                        popup.setY(popup.getY() - height - spacingY);
                }
            }
        }

        /**
         * Creates and shows a popup with the data from the given Notification object
         * @param NOTIFICATION
         */
        private void showPopup(final Notification NOTIFICATION) {
            final Label title = new Label(NOTIFICATION.TITLE);
            title.getStyleClass().add("title");

            final ImageView icon = new ImageView(NOTIFICATION.IMAGE);
            icon.setFitWidth(ICON_WIDTH);
            icon.setFitHeight(ICON_HEIGHT);

            final Label message = new Label(NOTIFICATION.MESSAGE, icon);
            message.getStyleClass().add("message");

            final VBox popupLayout = new VBox();
            popupLayout.setSpacing(10);
            popupLayout.setPadding(new Insets(10, 10, 10, 10));
            popupLayout.getChildren().addAll(title, message);

            final StackPane popupContent = new StackPane();
            popupContent.setPrefSize(width, height);
            popupContent.getStyleClass().add("notification");
            popupContent.getChildren().addAll(popupLayout);

            final Popup POPUP = new Popup();
            POPUP.setX( getX() );
            POPUP.setY( getY() );
            POPUP.getContent().add(popupContent);

            popups.add(POPUP);

            // Add a timeline for popup fade out
            final KeyValue fadeOutBegin = new KeyValue(POPUP.opacityProperty(), 1.0);
            final KeyValue fadeOutEnd   = new KeyValue(POPUP.opacityProperty(), 0.0);

            final KeyFrame kfBegin = new KeyFrame(Duration.ZERO, fadeOutBegin);
            final KeyFrame kfEnd   = new KeyFrame(Duration.millis(500), fadeOutEnd);

            final Timeline timeline = new Timeline(kfBegin, kfEnd);
            timeline.setDelay(popupLifetime);
            timeline.setOnFinished(actionEvent -> Platform.runLater(() -> {
                POPUP.hide();
                popups.remove(POPUP);
            }));

            // Move popup to the right during fade out
            //POPUP.opacityProperty().addListener((observableValue, oldOpacity, opacity) -> popup.setX(popup.getX() + (1.0 - opacity.doubleValue()) * popup.getWidth()) );

            if (stage.isShowing()) {
                stage.toFront();
            } else {
                stage.show();
            }

            POPUP.show(stage);
            timeline.play();
        }

        private double getX() {
            if (null == stageRef) return calcX( 0.0, Screen.getPrimary().getBounds().getWidth() );

            return calcX(stageRef.getX(), stageRef.getWidth());
        }
        private double getY() {
            if (null == stageRef) return calcY( 0.0, Screen.getPrimary().getBounds().getHeight() );

            return calcY(stageRef.getY(), stageRef.getHeight());
        }

        private double calcX(final double LEFT, final double TOTAL_WIDTH) {
            switch (popupLocation) {
                case TOP_LEFT  : case CENTER_LEFT : case BOTTOM_LEFT  : return LEFT + offsetX;
                case TOP_CENTER: case CENTER      : case BOTTOM_CENTER: return LEFT + (TOTAL_WIDTH - width) * 0.5 - offsetX;
                case TOP_RIGHT : case CENTER_RIGHT: case BOTTOM_RIGHT : return LEFT + TOTAL_WIDTH - width - offsetX;
                default: return 0.0;
            }
        }
        private double calcY(final double TOP, final double TOTAL_HEIGHT ) {
            switch (popupLocation) {
                case TOP_LEFT   : case TOP_CENTER   : case TOP_RIGHT   : return TOP + offsetY;
                case CENTER_LEFT: case CENTER       : case CENTER_RIGHT: return TOP + (TOTAL_HEIGHT- height)/2 - offsetY;
                case BOTTOM_LEFT: case BOTTOM_CENTER: case BOTTOM_RIGHT: return TOP + TOTAL_HEIGHT - height - offsetY;
                default: return 0.0;
            }
        }
    }
}
