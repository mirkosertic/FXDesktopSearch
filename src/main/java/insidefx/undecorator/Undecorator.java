/*
 * BSD
 * Copyright (c) 2013, Arnaud Nouard
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
 notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 notice, this list of conditions and the following disclaimer in the
 documentation and/or other materials provided with the distribution.
 * Neither the name of the In-SideFX nor the
 names of its contributors may be used to endorse or promote products
 derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package insidefx.undecorator;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Side;
import javafx.scene.CacheHint;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class, with the UndecoratorController, is the central class for the
 * decoration of Transparent Stages. The Stage Undecorator TODO: Themes, manage
 * Quit (main stage)
 *
 * Bugs (Mac only?): Accelerators + Fullscreen crashes JVM KeyCombination does
 * not respect keyboard's locale Multi screen: On second screen JFX returns
 * wrong value for MinY (300)
 */
public class Undecorator extends StackPane {

    private static int SHADOW_WIDTH = 15;
    private static int SAVED_SHADOW_WIDTH = 15;
    private static int RESIZE_PADDING = 7;
    static public int FEEDBACK_STROKE = 4;
    public static final Logger LOGGER = Logger.getLogger("Undecorator");
    private static ResourceBundle LOC;
    private StageStyle stageStyle;
    @FXML
    private Button menu;
    @FXML
    private Button close;
    @FXML
    private Button maximize;
    @FXML
    private Button minimize;
    @FXML
    private Button resize;
    @FXML
    private Button fullscreen;
    @FXML
    private Label title;
    private MenuItem maximizeMenuItem;
    private CheckMenuItem fullScreenMenuItem;
    private Region clientArea;
    private Pane stageDecoration = null;
    private Rectangle shadowRectangle;
    private Pane glassPane;
    private Rectangle dockFeedback;
    private FadeTransition dockFadeTransition;
    private Stage dockFeedbackPopup;
    ParallelTransition parallelTransition;
    private DropShadow dsFocused;
    private DropShadow dsNotFocused;
    private UndecoratorController undecoratorController;
    private Stage stage;
    private Rectangle resizeRect;
    SimpleBooleanProperty maximizeProperty;
    private SimpleBooleanProperty minimizeProperty;
    private SimpleBooleanProperty closeProperty;
    private SimpleBooleanProperty fullscreenProperty;
    private final String backgroundStyleClass = "undecorator-background";
    private TranslateTransition fullscreenButtonTransition;

    public SimpleBooleanProperty maximizeProperty() {
        return maximizeProperty;
    }

    SimpleBooleanProperty minimizeProperty() {
        return minimizeProperty;
    }

    SimpleBooleanProperty closeProperty() {
        return closeProperty;
    }

    public SimpleBooleanProperty fullscreenProperty() {
        return fullscreenProperty;
    }

    public Undecorator(final Stage stage, final Region root) {
        this(stage, root, "stagedecoration.fxml", StageStyle.UNDECORATED);
    }

    private Undecorator(final Stage stag, final Region clientArea, final String stageDecorationFxml, final StageStyle st) {
        create(stag, clientArea, getClass().getResource(stageDecorationFxml), st);
    }
    private Undecorator(final Stage stag, final Region clientArea, final URL stageDecorationFxmlAsURL, final StageStyle st) {
        create(stag, clientArea, stageDecorationFxmlAsURL, st);
    }
    void create(final Stage stag, final Region clientArea, final URL stageDecorationFxmlAsURL, final StageStyle st) {
        this.stage = stag;
        this.clientArea = clientArea;

        setStageStyle(st);
        loadConfig();

        // Properties 
        maximizeProperty = new SimpleBooleanProperty(false);
        maximizeProperty.addListener((ov, t, t1) -> getController().maximizeOrRestore());
        minimizeProperty = new SimpleBooleanProperty(false);
        minimizeProperty.addListener((ov, t, t1) -> getController().minimize());

        closeProperty = new SimpleBooleanProperty(false);
        closeProperty.addListener((ov, t, t1) -> getController().close());
        fullscreenProperty = new SimpleBooleanProperty(false);
        fullscreenProperty.addListener((ov, t, t1) -> getController().setFullScreen(!stage.isFullScreen()));

        // The controller
        undecoratorController = new UndecoratorController(this);

        undecoratorController.setAsStageDraggable(stage, clientArea);

        // Focus drop shadows: radius, spread, offsets
        dsFocused = new DropShadow(BlurType.THREE_PASS_BOX, Color.BLACK, SHADOW_WIDTH, 0.1, 0, 0);
        dsNotFocused = new DropShadow(BlurType.THREE_PASS_BOX, Color.DARKGREY, SHADOW_WIDTH, 0, 0, 0);

        shadowRectangle = new Rectangle();

        // UI part of the decoration
        try {
            final FXMLLoader fxmlLoader = new FXMLLoader(stageDecorationFxmlAsURL);
//            fxmlLoader.setController(new StageDecorationController(this));
            fxmlLoader.setController(this);
            stageDecoration = fxmlLoader.load();
        } catch (final Exception ex) {
            LOGGER.log(Level.SEVERE, "Decorations not found", ex);
        }
        initDecoration();

        /*
         * Resize rectangle
         */
        resizeRect = new Rectangle();
        resizeRect.setFill(null);
        resizeRect.setStrokeWidth(RESIZE_PADDING);
        resizeRect.setStrokeType(StrokeType.INSIDE);
        resizeRect.setStroke(Color.TRANSPARENT);
        undecoratorController.setStageResizableWith(stage, resizeRect, RESIZE_PADDING, SHADOW_WIDTH);

        // If not resizable (quick fix)
        if (fullscreen != null) {
            fullscreen.setVisible(stage.isResizable());
        }
        resize.setVisible(stage.isResizable());
        if (maximize != null) {
            maximize.setVisible(stage.isResizable());
        }
        if (minimize != null && !stage.isResizable()) {
            AnchorPane.setRightAnchor(minimize, 34d);
        }

        // Glass Pane
        glassPane = new Pane();
        glassPane.setMouseTransparent(true);
        buildDockFeedbackStage();

        title.getStyleClass().add("undecorator-label-titlebar");
        // TODO: how to programmatically get css values? wait for JavaFX 8 custom CSS
        shadowRectangle.getStyleClass().add(backgroundStyleClass);
        // Do not intercept mouse events on stage's drop shadow
        shadowRectangle.setMouseTransparent(true);

        // Add all layers
        super.getChildren().addAll(shadowRectangle, clientArea, stageDecoration, resizeRect, glassPane);

        /*
         * Focused stage
         */
        stage.focusedProperty().addListener((ov, t, t1) -> setShadowFocused(t1));
        /*
         * Fullscreen
         */
        if (fullscreen != null) {
            fullscreen.setOnMouseEntered(t -> {
                if (stage.isFullScreen()) {
                    fullscreen.setOpacity(1);
                }
            });

            fullscreen.setOnMouseExited(t -> {
                if (stage.isFullScreen()) {
                    fullscreen.setOpacity(0.4);
                }
            });

            stage.fullScreenProperty().addListener((ov, t, fullscreenState) -> {
                setShadow(!fullscreenState);
                fullScreenMenuItem.setSelected(fullscreenState);
                maximize.setVisible(!fullscreenState);
                minimize.setVisible(!fullscreenState);
                resize.setVisible(!fullscreenState);
                if (fullscreenState) {
                    // String and icon
                    fullscreen.getStyleClass().add("decoration-button-unfullscreen");
                    fullscreen.setTooltip(new Tooltip(LOC.getString("Restore")));

                    undecoratorController.saveFullScreenBounds();
                    if (fullscreenButtonTransition != null) {
                        fullscreenButtonTransition.stop();
                    }
                    // Animate the fullscreen button
                    fullscreenButtonTransition = new TranslateTransition();
                    fullscreenButtonTransition.setDuration(Duration.millis(3000));
                    fullscreenButtonTransition.setToX(66);
                    fullscreenButtonTransition.setNode(fullscreen);
                    fullscreenButtonTransition.setOnFinished(t1 -> fullscreenButtonTransition = null);

                    fullscreenButtonTransition.play();
                    fullscreen.setOpacity(0.2);
                } else {
                    // String and icon
                    fullscreen.getStyleClass().remove("decoration-button-unfullscreen");
                    fullscreen.setTooltip(new Tooltip(LOC.getString("FullScreen")));

                    undecoratorController.restoreFullScreenSavedBounds(stage);
                    fullscreen.setOpacity(1);
                    if (fullscreenButtonTransition != null) {
                        fullscreenButtonTransition.stop();
                    }
                    // Animate the change
                    fullscreenButtonTransition = new TranslateTransition();
                    fullscreenButtonTransition.setDuration(Duration.millis(1000));
                    fullscreenButtonTransition.setToX(0);
                    fullscreenButtonTransition.setNode(fullscreen);
                    fullscreenButtonTransition.setOnFinished(t1 -> fullscreenButtonTransition = null);

                    fullscreenButtonTransition.play();
                }

            });
        }
        computeAllSizes();
    }

    /**
     * Install default accelerators
     *
     * @param scene
     */
    public void installAccelerators(final Scene scene) {
        // Accelerators
        if (stage.isResizable()) {
            scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN, KeyCombination.SHORTCUT_DOWN),
                    this::switchFullscreen);
        }
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.M, KeyCombination.SHORTCUT_DOWN), this::switchMinimize);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN), this::switchClose);
    }

    /**
     * Init the minimum/pref/max size in order to be reflected in the primary
     * stage
     */
    private void computeAllSizes() {
        final double minWidth = minWidth(getHeight());
        setMinWidth(minWidth);
        final double minHeight = minHeight(getWidth());
        setMinHeight(minHeight);

        final double prefHeight = prefHeight(getWidth());
        setPrefHeight(prefHeight);
        final double prefWidth = prefWidth(getHeight());
        setPrefWidth(prefWidth);

        final double maxWidth = maxWidth(getHeight());
        if (maxWidth > 0) {
            setMaxWidth(maxWidth);
        }
        final double maxHeight = maxHeight(getWidth());
        if (maxHeight > 0) {
            setMaxHeight(maxHeight);
        }
    }
    /*
     * The sizing is based on client area's bounds.
     */

    @Override
    protected double computePrefWidth(final double d) {
        return clientArea.getPrefWidth() + SHADOW_WIDTH * 2 + RESIZE_PADDING * 2;
    }

    @Override
    protected double computePrefHeight(final double d) {
        return clientArea.getPrefHeight() + SHADOW_WIDTH * 2 + RESIZE_PADDING * 2;
    }

    @Override
    protected double computeMaxHeight(final double d) {
        return clientArea.getMaxHeight() + SHADOW_WIDTH * 2 + RESIZE_PADDING * 2;
    }

    @Override
    protected double computeMinHeight(final double d) {
        double d2 = super.computeMinHeight(d);
        d2 += SHADOW_WIDTH * 2 + RESIZE_PADDING * 2;
        return d2;
    }

    @Override
    protected double computeMaxWidth(final double d) {
        return clientArea.getMaxWidth() + SHADOW_WIDTH * 2 + RESIZE_PADDING * 2;
    }

    @Override
    protected double computeMinWidth(final double d) {
        double d2 = super.computeMinWidth(d);
        d2 += SHADOW_WIDTH * 2 + RESIZE_PADDING * 2;
        return d2;
    }

    void setStageStyle(final StageStyle st) {
        stageStyle = st;
    }

    public StageStyle getStageStyle() {
        return stageStyle;
    }

    /**
     * Activate fade in transition on showing event
     */
    public void setFadeInTransition() {
        super.setOpacity(0);
        stage.showingProperty().addListener((ov, t, t1) -> {
            if (t1) {
                final FadeTransition fadeTransition = new FadeTransition(Duration.seconds(2), Undecorator.this);
                fadeTransition.setToValue(1);
                fadeTransition.play();
            }
        });
    }

    /**
     * Launch the fade out transition. Must be invoked when the
     * application/window is supposed to be closed
     */
    public void setFadeOutTransition() {
        final FadeTransition fadeTransition = new FadeTransition(Duration.seconds(1), Undecorator.this);
        fadeTransition.setToValue(0);
        fadeTransition.play();
        fadeTransition.setOnFinished(t -> {
            stage.hide();
            if (dockFeedbackPopup != null && dockFeedbackPopup.isShowing()) {
                dockFeedbackPopup.hide();
            }
        });
    }

    public void removeDefaultBackgroundStyleClass() {
        shadowRectangle.getStyleClass().remove(backgroundStyleClass);
    }

    public Rectangle getBackgroundNode() {
        return shadowRectangle;
    }

    /**
     * Manage buttons and menu items
     */
    void initDecoration() {
        MenuItem minimizeMenuItem = null;
        // Menu
        final ContextMenu contextMenu = new ContextMenu();
        contextMenu.setAutoHide(true);
        if (minimize != null) { // Utility Stage
            minimizeMenuItem = new MenuItem(LOC.getString("Minimize"));
            minimizeMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.M, KeyCombination.SHORTCUT_DOWN));

            minimizeMenuItem.setOnAction(e -> switchMinimize());
            contextMenu.getItems().add(minimizeMenuItem);
        }
        if (maximize != null && stage.isResizable()) { // Utility Stage type
            maximizeMenuItem = new MenuItem(LOC.getString("Maximize"));
            maximizeMenuItem.setOnAction(e -> {
                switchMaximize();
                contextMenu.hide(); // Stay stuck on screen
            });
            contextMenu.getItems().addAll(maximizeMenuItem, new SeparatorMenuItem());
        }

        // Fullscreen
        if (stageStyle != StageStyle.UTILITY && stage.isResizable()) {
            fullScreenMenuItem = new CheckMenuItem(LOC.getString("FullScreen"));
            fullScreenMenuItem.setOnAction(e -> switchFullscreen());
            fullScreenMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN, KeyCombination.SHORTCUT_DOWN));

            contextMenu.getItems().addAll(fullScreenMenuItem, new SeparatorMenuItem());
        }

        // Close
        final MenuItem closeMenuItem = new MenuItem(LOC.getString("Close"));
        closeMenuItem.setOnAction(e -> switchClose());
        closeMenuItem.setAccelerator(new KeyCodeCombination(KeyCode.W, KeyCombination.SHORTCUT_DOWN));

        contextMenu.getItems().add(closeMenuItem);

        menu.setOnMousePressed(t -> {
            if (contextMenu.isShowing()) {
                contextMenu.hide();
            } else {
                contextMenu.show(menu, Side.BOTTOM, 0, 0);
            }
        });

        // Close button
        close.setTooltip(new Tooltip(LOC.getString("Close")));
        close.setOnAction(t -> switchClose());

        // Maximize button
        // If changed via contextual menu
        maximizeProperty().addListener((ov, t, t1) -> {
            final Tooltip tooltip = maximize.getTooltip();
            if (tooltip.getText().equals(LOC.getString("Maximize"))) {
                tooltip.setText(LOC.getString("Restore"));
                maximizeMenuItem.setText(LOC.getString("Restore"));
                maximize.getStyleClass().add("decoration-button-restore");
                resize.setVisible(false);
            } else {
                tooltip.setText(LOC.getString("Maximize"));
                maximizeMenuItem.setText(LOC.getString("Maximize"));
                maximize.getStyleClass().remove("decoration-button-restore");
                resize.setVisible(true);
            }
        });

        if (maximize != null) { // Utility Stage
            maximize.setTooltip(new Tooltip(LOC.getString("Maximize")));
            maximize.setOnAction(t -> switchMaximize());
        }
        if (fullscreen != null) { // Utility Stage
            fullscreen.setTooltip(new Tooltip(LOC.getString("FullScreen")));
            fullscreen.setOnAction(t -> switchFullscreen());
        }

        // Minimize button
        if (minimize != null) { // Utility Stage
            minimize.setTooltip(new Tooltip(LOC.getString("Minimize")));
            minimize.setOnAction(t -> switchMinimize());
        }
        // Transfer stage title to undecorator tiltle label

        title.setText(stage.getTitle());
    }

    void switchFullscreen() {
        // Invoke runLater even if it's on EDT: Crash apps on Mac
        Platform.runLater(() -> undecoratorController.setFullScreen(!stage.isFullScreen()));
    }

    void switchMinimize() {
        minimizeProperty().set(!minimizeProperty().get());
    }

    void switchMaximize() {
        maximizeProperty().set(!maximizeProperty().get());
    }

    void switchClose() {
        closeProperty().set(!closeProperty().get());
    }

    /**
     * Bridge to the controller to enable the specified node to drag the stage
     *
     * @param stage
     * @param node
     */
    public void setAsStageDraggable(final Stage stage, final Node node) {
        undecoratorController.setAsStageDraggable(stage, node);
    }

    /**
     * Switch the visibility of the window's drop shadow
     */
    void setShadow(final boolean shadow) {
        // Already removed?
        if (!shadow && shadowRectangle.getEffect() == null) {
            return;
        }
        // From fullscreen to maximize case
        if (shadow && maximizeProperty.get()) {
            return;
        }
        if (!shadow) {
            shadowRectangle.setEffect(null);
            SAVED_SHADOW_WIDTH = SHADOW_WIDTH;
            SHADOW_WIDTH = 0;
        } else {
            shadowRectangle.setEffect(dsFocused);
            SHADOW_WIDTH = SAVED_SHADOW_WIDTH;
        }
    }

    /**
     * Set on/off the stage shadow effect
     *
     * @param b
     */
    void setShadowFocused(final boolean b) {
        if (b) {
            shadowRectangle.setEffect(dsFocused);
        } else {
            shadowRectangle.setEffect(dsNotFocused);
        }
    }

    /**
     * Set the layout of different layers of the stage
     */
    @Override
    public void layoutChildren() {
        final Bounds b = super.getLayoutBounds();
        final double w = b.getWidth();
        final double h = b.getHeight();
        final ObservableList<Node> list = super.getChildren();
        for (final Node node : list) {
            if (node == shadowRectangle) {
                shadowRectangle.setWidth(w - SHADOW_WIDTH * 2);
                shadowRectangle.setHeight(h - SHADOW_WIDTH * 2);
                shadowRectangle.setX(SHADOW_WIDTH);
                shadowRectangle.setY(SHADOW_WIDTH);
            } else if (node == stageDecoration) {
                stageDecoration.resize(w - SHADOW_WIDTH * 2, h - SHADOW_WIDTH * 2);
                stageDecoration.setLayoutX(SHADOW_WIDTH);
                stageDecoration.setLayoutY(SHADOW_WIDTH);
            } else if (node == resizeRect) {
                resizeRect.setWidth(w - SHADOW_WIDTH * 2);
                resizeRect.setHeight(h - SHADOW_WIDTH * 2);
                resizeRect.setLayoutX(SHADOW_WIDTH);
                resizeRect.setLayoutY(SHADOW_WIDTH);
            } else {
                node.resize(w - SHADOW_WIDTH * 2 - RESIZE_PADDING * 2, h - SHADOW_WIDTH * 2 - RESIZE_PADDING * 2);
                node.setLayoutX(SHADOW_WIDTH + RESIZE_PADDING);
                node.setLayoutY(SHADOW_WIDTH + RESIZE_PADDING);
            }
        }
    }

    public int getShadowBorderSize() {
        return SHADOW_WIDTH * 2 + RESIZE_PADDING * 2;
    }

    UndecoratorController getController() {
        return undecoratorController;
    }

    public Stage getStage() {
        return stage;
    }

    protected Pane getGlassPane() {
        return glassPane;
    }

    public void addGlassPane(final Node node) {
        glassPane.getChildren().add(node);
    }

    public void removeGlassPane(final Node node) {
        glassPane.getChildren().remove(node);
    }

    /**
     * Returns the decoration (buttons...)
     *
     * @return
     */
    public Pane getStageDecorationNode() {
        return stageDecoration;
    }

    /**
     * Prepare Stage for dock feedback display
     */
    void buildDockFeedbackStage() {
        dockFeedbackPopup = new Stage(StageStyle.TRANSPARENT);
        dockFeedback = new Rectangle(0, 0, 100, 100);
        dockFeedback.setArcHeight(10);
        dockFeedback.setArcWidth(10);
        dockFeedback.setFill(Color.TRANSPARENT);
        dockFeedback.setStroke(Color.BLACK);
        dockFeedback.setStrokeWidth(2);
        dockFeedback.setCache(true);
        dockFeedback.setCacheHint(CacheHint.SPEED);
        dockFeedback.setEffect(new DropShadow(BlurType.TWO_PASS_BOX, Color.BLACK, 10, 0.2, 3, 3));
        dockFeedback.setMouseTransparent(true);
        final BorderPane borderpane = new BorderPane();
        borderpane.setCenter(dockFeedback);
        final Scene scene = new Scene(borderpane);
        scene.setFill(Color.TRANSPARENT);
        dockFeedbackPopup.setScene(scene);
        dockFeedbackPopup.sizeToScene();
    }
    /* void buildDockFeedback() {
     `   dockFeedbackPopup = new Popup();
     dockFeedbackPopup.setHideOnEscape(false);
     dockFeedbackPopup.setAutoFix(false);
     dockFeedback = new Rectangle(0, 0, 100, 100);
     dockFeedback.setFill(Color.TRANSPARENT);
     dockFeedback.setStroke(Color.BLACK);
     dockFeedback.setStrokeWidth(2);
     dockFeedback.setMouseTransparent(true);
      
     // dockFeedback.setStyle("-fx-border-color:black; -fx-border-width:1"); //-fx-background-color: #FFFFFFFF; -fx-background-insets:10;");
     dockFeedback.setEffect(new DropShadow(SHADOW_WIDTH, Color.BLACK));
     //BorderPane borderpane = new BorderPane();
     //        borderpane.setCenter(dockFeedback);
     dockFeedbackPopup.getContent().add(dockFeedback);
     //        dockFeedbackPopup.sizeToScene();

     }*/

    /**
     * Activate dock feedback on screen's bounds
     *
     * @param x
     * @param y
     */
    public void setDockFeedbackVisible(final double x, final double y, final double width, final double height) {
        dockFeedbackPopup.setX(x);
        dockFeedbackPopup.setY(y);

        dockFeedback.setX(SHADOW_WIDTH);
        dockFeedback.setY(SHADOW_WIDTH);
        dockFeedback.setHeight(height - SHADOW_WIDTH * 2);
        dockFeedback.setWidth(width - SHADOW_WIDTH * 2);

        dockFeedbackPopup.setWidth(width);
        dockFeedbackPopup.setHeight(height);

        dockFeedback.setOpacity(1);
        dockFeedbackPopup.show();

        dockFadeTransition = new FadeTransition();
        dockFadeTransition.setDuration(Duration.millis(200));
        dockFadeTransition.setNode(dockFeedback);
        dockFadeTransition.setFromValue(0);
        dockFadeTransition.setToValue(1);
        dockFadeTransition.setAutoReverse(true);
        dockFadeTransition.setCycleCount(3);
        dockFadeTransition.setOnFinished(t -> {
                    //dockFeedback.setVisible(false);
                    //dockFeedbackPopup.hide();
                });
        dockFadeTransition.play();
    }

    public void setDockFeedbackInvisible() {
        if (dockFeedbackPopup.isShowing()) {
            dockFeedbackPopup.hide();
            if (dockFadeTransition != null) {
                dockFadeTransition.stop();
            }
        }
    }

    private static void loadConfig() {
        final Properties prop = new Properties();

        try {
            prop.load(Undecorator.class.getClassLoader().getResourceAsStream("skin/undecorator.properties"));
            SHADOW_WIDTH = Integer.parseInt(prop.getProperty("window-shadow-width"));
            RESIZE_PADDING = Integer.parseInt(prop.getProperty("window-resize-padding"));
        } catch (final IOException ex) {
            LOGGER.log(Level.SEVERE, "Error while loading confguration flie", ex);
        }
        LOC = ResourceBundle.getBundle("insidefx/undecorator/resources/localization", Locale.getDefault());

    }
}
