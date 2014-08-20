package insidefx.undecorator;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;

import java.util.logging.Level;

/**
 *
 * @author in-sideFX
 */
class UndecoratorController {

    private static final int DOCK_NONE = 0x0;
    private static final int DOCK_LEFT = 0x1;
    private static final int DOCK_RIGHT = 0x2;
    private static final int DOCK_TOP = 0x4;
    private int lastDocked = DOCK_NONE;
    private static double initX = -1;
    private static double initY = -1;
    private static double newX;
    private static double newY;
    private static int RESIZE_PADDING;
    private static int SHADOW_WIDTH;
    private final Undecorator undecorator;
    private BoundingBox savedBounds;
    private BoundingBox savedFullScreenBounds;
    private boolean maximized = false;
    private static boolean isMacOS = false;
    private static final int MAXIMIZE_BORDER = 20;  // Allow double click to maximize on top of the Scene

    {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("mac")) {
            isMacOS = true;
        }
    }

    public UndecoratorController(Undecorator ud) {
        undecorator = ud;
    }


    /*
     * Actions
     */
    void maximizeOrRestore() {


        Stage stage = undecorator.getStage();

        if (maximized) {
            restoreSavedBounds(stage, false);
            undecorator.setShadow(true);
            savedBounds = null;
            maximized = false;
        } else {
            ObservableList<Screen> screensForRectangle = Screen.getScreensForRectangle(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
            Screen screen = screensForRectangle.get(0);
            Rectangle2D visualBounds = screen.getVisualBounds();

            savedBounds = new BoundingBox(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());

            undecorator.setShadow(false);

            stage.setX(visualBounds.getMinX());
            stage.setY(visualBounds.getMinY());
            stage.setWidth(visualBounds.getWidth());
            stage.setHeight(visualBounds.getHeight());
            maximized = true;
        }
    }

    public void saveBounds() {
        Stage stage = undecorator.getStage();
        savedBounds = new BoundingBox(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
    }

    public void saveFullScreenBounds() {
        Stage stage = undecorator.getStage();
        savedFullScreenBounds = new BoundingBox(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
    }

    void restoreSavedBounds(Stage stage, boolean fullscreen) {

        stage.setX(savedBounds.getMinX());
        stage.setY(savedBounds.getMinY());
        stage.setWidth(savedBounds.getWidth());
        stage.setHeight(savedBounds.getHeight());
        savedBounds = null;
    }

    public void restoreFullScreenSavedBounds(Stage stage) {

        stage.setX(savedFullScreenBounds.getMinX());
        stage.setY(savedFullScreenBounds.getMinY());
        stage.setWidth(savedFullScreenBounds.getWidth());
        stage.setHeight(savedFullScreenBounds.getHeight());
        savedFullScreenBounds = null;
    }

    void setFullScreen(boolean value) {
        Stage stage = undecorator.getStage();
        stage.setFullScreen(value);
    }

    public void close() {
        final Stage stage = undecorator.getStage();
        Platform.runLater(() -> stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST)));

    }

    public void minimize() {

        if (!Platform.isFxApplicationThread()) // Ensure on correct thread else hangs X under Unbuntu
        {
            Platform.runLater(this::_minimize);
        } else {
            _minimize();
        }
    }

    private void _minimize() {
        Stage stage = undecorator.getStage();
        stage.setIconified(true);
    }

    /**
     * Stage resize management
     *
     * @param stage
     * @param node
     * @param PADDING
     * @param SHADOW
     */
    public void setStageResizableWith(final Stage stage, final Node node, int PADDING, int SHADOW) {

        RESIZE_PADDING = PADDING;
        SHADOW_WIDTH = SHADOW;
        node.setOnMouseClicked(mouseEvent -> {
            if (undecorator.getStageStyle() != StageStyle.UTILITY && !stage.isFullScreen() && mouseEvent.getClickCount() > 1) {
                if (mouseEvent.getSceneY() - SHADOW_WIDTH < MAXIMIZE_BORDER) {
                    undecorator.maximizeProperty().set(!undecorator.maximizeProperty().get());
                    mouseEvent.consume();
                }
            }
        });

        node.setOnMousePressed(mouseEvent -> {
            if (mouseEvent.isPrimaryButtonDown()) {
                initX = mouseEvent.getScreenX();
                initY = mouseEvent.getScreenY();
                mouseEvent.consume();
            }
        });
        node.setOnMouseDragged(mouseEvent -> {
            if (!mouseEvent.isPrimaryButtonDown() || (initX == -1 && initY == -1)) {
                return;
            }
            if (stage.isFullScreen()) {
                return;
            }
            /*
             * Long press generates drag event!
             */
            if (mouseEvent.isStillSincePress()) {
                return;
            }
            if (maximized) {
                // Remove maximized state
                undecorator.maximizeProperty.set(false);
                return;
            } // Docked then moved, so restore state
            else if (savedBounds != null) {
                undecorator.setShadow(true);
            }


            newX = mouseEvent.getScreenX();
            newY = mouseEvent.getScreenY();
            double deltax = newX - initX;
            double deltay = newY - initY;

            Cursor cursor = node.getCursor();
            if (Cursor.E_RESIZE.equals(cursor)) {
                setStageWidth(stage, stage.getWidth() + deltax);
                mouseEvent.consume();
            } else if (Cursor.NE_RESIZE.equals(cursor)) {
                if (setStageHeight(stage, stage.getHeight() - deltay)) {
                    setStageY(stage, stage.getY() + deltay);
                }
                setStageWidth(stage, stage.getWidth() + deltax);
                mouseEvent.consume();
            } else if (Cursor.SE_RESIZE.equals(cursor)) {
                setStageWidth(stage, stage.getWidth() + deltax);
                setStageHeight(stage, stage.getHeight() + deltay);
                mouseEvent.consume();
            } else if (Cursor.S_RESIZE.equals(cursor)) {
                setStageHeight(stage, stage.getHeight() + deltay);
                mouseEvent.consume();
            } else if (Cursor.W_RESIZE.equals(cursor)) {
                if (setStageWidth(stage, stage.getWidth() - deltax)) {
                    stage.setX(stage.getX() + deltax);
                }
                mouseEvent.consume();
            } else if (Cursor.SW_RESIZE.equals(cursor)) {
                if (setStageWidth(stage, stage.getWidth() - deltax)) {
                    stage.setX(stage.getX() + deltax);
                }
                setStageHeight(stage, stage.getHeight() + deltay);
                mouseEvent.consume();
            } else if (Cursor.NW_RESIZE.equals(cursor)) {
                if (setStageWidth(stage, stage.getWidth() - deltax)) {
                    stage.setX(stage.getX() + deltax);
                }
                if (setStageHeight(stage, stage.getHeight() - deltay)) {
                    setStageY(stage, stage.getY() + deltay);
                }
                mouseEvent.consume();
            } else if (Cursor.N_RESIZE.equals(cursor)) {
                if (setStageHeight(stage, stage.getHeight() - deltay)) {
                    setStageY(stage, stage.getY() + deltay);
                }
                mouseEvent.consume();
            }

        });
        node.setOnMouseMoved(mouseEvent -> {
            if (maximized) {
                setCursor(node, Cursor.DEFAULT);
                return; // maximized mode does not support resize
            }
            if (stage.isFullScreen()) {
                return;
            }
            if (!stage.isResizable()) {
                return;
            }
            double x = mouseEvent.getX();
            double y = mouseEvent.getY();
            Bounds boundsInParent = node.getBoundsInParent();
            if (isRightEdge(x, y, boundsInParent)) {
                if (y < RESIZE_PADDING + SHADOW_WIDTH) {
                    setCursor(node, Cursor.NE_RESIZE);
                } else if (y > boundsInParent.getHeight() - (double) (RESIZE_PADDING + SHADOW_WIDTH)) {
                    setCursor(node, Cursor.SE_RESIZE);
                } else {
                    setCursor(node, Cursor.E_RESIZE);
                }

            } else if (isLeftEdge(x, y, boundsInParent)) {
                if (y < RESIZE_PADDING + SHADOW_WIDTH) {
                    setCursor(node, Cursor.NW_RESIZE);
                } else if (y > boundsInParent.getHeight() - (double) (RESIZE_PADDING + SHADOW_WIDTH)) {
                    setCursor(node, Cursor.SW_RESIZE);
                } else {
                    setCursor(node, Cursor.W_RESIZE);
                }
            } else if (isTopEdge(x, y, boundsInParent)) {
                setCursor(node, Cursor.N_RESIZE);
            } else if (isBottomEdge(x, y, boundsInParent)) {
                setCursor(node, Cursor.S_RESIZE);
            } else {
                setCursor(node, Cursor.DEFAULT);
            }
        });
    }

    /**
     * Under Windows, the undecorator Stage could be been dragged below the Task
     * bar and then no way to grab it again... On Mac, do not drag above the
     * menu bar
     *
     * @param y
     */
    void setStageY(Stage stage, double y) {
        try {
            ObservableList<Screen> screensForRectangle = Screen.getScreensForRectangle(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
            if (screensForRectangle.size() > 0) {
                Screen screen = screensForRectangle.get(0);
                Rectangle2D visualBounds = screen.getVisualBounds();
                if (y < visualBounds.getHeight() - 30 && y + SHADOW_WIDTH >= visualBounds.getMinY()) {
                    stage.setY(y);
                }
            }
        } catch (Exception e) {
            Undecorator.LOGGER.log(Level.SEVERE, "setStageY issue", e);
        }
    }

    boolean setStageWidth(Stage stage, double width) {
        if (width >= stage.getMinWidth()) {
            stage.setWidth(width);
            initX = newX;
            return true;
        }
        return false;
    }

    boolean setStageHeight(Stage stage, double height) {
        if (height >= stage.getMinHeight()) {
            stage.setHeight(height);
            initY = newY;
            return true;
        }
        return false;
    }

    /**
     * Allow this node to drag the Stage
     *
     * @param stage
     * @param node
     */
    public void setAsStageDraggable(final Stage stage, final Node node) {

        node.setOnMouseClicked(mouseEvent -> {
            if (undecorator.getStageStyle() != StageStyle.UTILITY && !stage.isFullScreen() && mouseEvent.getClickCount() > 1) {
                if (mouseEvent.getSceneY() - SHADOW_WIDTH < MAXIMIZE_BORDER) {
                    undecorator.maximizeProperty().set(!undecorator.maximizeProperty().get());
                    mouseEvent.consume();
                }
            }
        });
        node.setOnMousePressed(mouseEvent -> {
            if (mouseEvent.isPrimaryButtonDown()) {
                initX = mouseEvent.getScreenX();
                initY = mouseEvent.getScreenY();
                mouseEvent.consume();
            } else {
                initX = -1;
                initY = -1;
            }
        });
        node.setOnMouseDragged(mouseEvent -> {
            if (!mouseEvent.isPrimaryButtonDown() || initX == -1) {
                return;
            }
            if (stage.isFullScreen()) {
                return;
            }
            /*
             * Long press generates drag event!
             */
            if (mouseEvent.isStillSincePress()) {
                return;
            }
            if (maximized) {
                // Remove Maximized state
                undecorator.maximizeProperty.set(false);
                // Center
                stage.setX(mouseEvent.getScreenX() - stage.getWidth() / 2);
                stage.setY(mouseEvent.getScreenY() - SHADOW_WIDTH);
            } // Docked then moved, so restore state
            else if (savedBounds != null) {
                restoreSavedBounds(stage, false);
                undecorator.setShadow(true);
                // Center
                stage.setX(mouseEvent.getScreenX() - stage.getWidth() / 2);
                stage.setY(mouseEvent.getScreenY() - SHADOW_WIDTH);
            }
            double newX1 = mouseEvent.getScreenX();
            double newY1 = mouseEvent.getScreenY();
            double deltax = newX1 - initX;
            double deltay = newY1 - initY;
            initX = newX1;
            initY = newY1;
            setCursor(node, Cursor.HAND);
            stage.setX(stage.getX() + deltax);
            setStageY(stage, stage.getY() + deltay);

            testDock(stage, mouseEvent);
            mouseEvent.consume();
        });
        node.setOnMouseReleased(t -> {
            if (stage.isResizable()) {
                undecorator.setDockFeedbackInvisible();
                setCursor(node, Cursor.DEFAULT);
                initX = -1;
                initY = -1;
                dockActions(stage, t);
            }
        });

        node.setOnMouseExited(mouseEvent -> {
            //setCursor(node, Cursor.DEFAULT);
        });

    }

    /**
     * (Humble) Simulation of Windows behavior on screen's edges Feedbacks
     */
    void testDock(Stage stage, MouseEvent mouseEvent) {

        if (!stage.isResizable()) {
            return;
        }

        int dockSide = getDockSide(mouseEvent);
        // Dock Left
        if (dockSide == DOCK_LEFT) {
            if (lastDocked == DOCK_LEFT) {
                return;
            }
            ObservableList<Screen> screensForRectangle = Screen.getScreensForRectangle(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
            Screen screen = screensForRectangle.get(0);
            Rectangle2D visualBounds = screen.getVisualBounds();
            // Dock Left
            double x = visualBounds.getMinX();
            double y = visualBounds.getMinY();
            double width = visualBounds.getWidth() / 2;
            double height = visualBounds.getHeight();

            undecorator.setDockFeedbackVisible(x, y, width, height);
            lastDocked = DOCK_LEFT;
        } // Dock Right
        else if (dockSide == DOCK_RIGHT) {
            if (lastDocked == DOCK_RIGHT) {
                return;
            }
            ObservableList<Screen> screensForRectangle = Screen.getScreensForRectangle(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
            Screen screen = screensForRectangle.get(0);
            Rectangle2D visualBounds = screen.getVisualBounds();
            // Dock Right (visualBounds = (javafx.geometry.Rectangle2D) Rectangle2D [minX = 1440.0, minY=300.0, maxX=3360.0, maxY=1500.0, width=1920.0, height=1200.0])
            double x = visualBounds.getMinX() + visualBounds.getWidth() / 2;
            double y = visualBounds.getMinY();
            double width = visualBounds.getWidth() / 2;
            double height = visualBounds.getHeight();

            undecorator.setDockFeedbackVisible(x, y, width, height);
            lastDocked = DOCK_RIGHT;
        } // Dock top
        else if (dockSide == DOCK_TOP) {
            if (lastDocked == DOCK_TOP) {
                return;
            }
            ObservableList<Screen> screensForRectangle = Screen.getScreensForRectangle(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
            Screen screen = screensForRectangle.get(0);
            Rectangle2D visualBounds = screen.getVisualBounds();
            // Dock Left
            double x = visualBounds.getMinX();
            double y = visualBounds.getMinY();
            double width = visualBounds.getWidth();
            double height = visualBounds.getHeight();
            undecorator.setDockFeedbackVisible(x, y, width, height);
            lastDocked = DOCK_TOP;
        } else {
            undecorator.setDockFeedbackInvisible();
            lastDocked = DOCK_NONE;
        }
    }

    /**
     * Based on mouse position returns dock side
     *
     * @param mouseEvent
     * @return DOCK_LEFT,DOCK_RIGHT,DOCK_TOP
     */
    int getDockSide(MouseEvent mouseEvent) {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = 0;
        double maxY = 0;

        // Get "big" screen bounds
        ObservableList<Screen> screens = Screen.getScreens();
        for (Screen screen : screens) {
            Rectangle2D visualBounds = screen.getVisualBounds();
            minX = Math.min(minX, visualBounds.getMinX());
            minY = Math.min(minY, visualBounds.getMinY());
            maxX = Math.max(maxX, visualBounds.getMaxX());
            maxY = Math.max(maxY, visualBounds.getMaxY());
        }
        // Dock Left
        if (mouseEvent.getScreenX() == minX) {
            return DOCK_LEFT;
        } else if (mouseEvent.getScreenX() >= maxX - 1) { // MaxX returns the width? Not width -1 ?!
            return DOCK_RIGHT;
        } else if (mouseEvent.getScreenY() <= minY) {   // Mac menu bar
            return DOCK_TOP;
        }
        return 0;
    }

    /**
     * (Humble) Simulation of Windows behavior on screen's edges Actions
     */
    void dockActions(Stage stage, MouseEvent mouseEvent) {
        ObservableList<Screen> screensForRectangle = Screen.getScreensForRectangle(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
        Screen screen = screensForRectangle.get(0);
        Rectangle2D visualBounds = screen.getVisualBounds();
        // Dock Left
        if (mouseEvent.getScreenX() == visualBounds.getMinX()) {
            savedBounds = new BoundingBox(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());

            stage.setX(visualBounds.getMinX());
            stage.setY(visualBounds.getMinY());
            stage.setWidth(visualBounds.getWidth() / 2);
            stage.setHeight(visualBounds.getHeight());
            undecorator.setShadow(false);
        } // Dock Right (visualBounds = [minX = 1440.0, minY=300.0, maxX=3360.0, maxY=1500.0, width=1920.0, height=1200.0])
        else if (mouseEvent.getScreenX() >= visualBounds.getMaxX() - 1) { // MaxX returns the width? Not width -1 ?!
            savedBounds = new BoundingBox(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());

            stage.setX(visualBounds.getWidth() / 2 + visualBounds.getMinX());
            stage.setY(visualBounds.getMinY());
            stage.setWidth(visualBounds.getWidth() / 2);
            stage.setHeight(visualBounds.getHeight());
            undecorator.setShadow(false);
        } else if (mouseEvent.getScreenY() <= visualBounds.getMinY()) { // Mac menu bar
            undecorator.maximizeProperty.set(true);
        }

    }

    boolean isRightEdge(double x, double y, Bounds boundsInParent) {
        if (x < boundsInParent.getWidth() && x > boundsInParent.getWidth() - RESIZE_PADDING - SHADOW_WIDTH) {
            return true;
        }
        return false;
    }

    boolean isTopEdge(double x, double y, Bounds boundsInParent) {
        if (y >= 0 && y < RESIZE_PADDING + SHADOW_WIDTH) {
            return true;
        }
        return false;
    }

    boolean isBottomEdge(double x, double y, Bounds boundsInParent) {
        if (y < boundsInParent.getHeight() && y > boundsInParent.getHeight() - RESIZE_PADDING - SHADOW_WIDTH) {
            return true;
        }
        return false;
    }

    boolean isLeftEdge(double x, double y, Bounds boundsInParent) {
        if (x >= 0 && x < RESIZE_PADDING + SHADOW_WIDTH) {
            return true;
        }
        return false;
    }

    void setCursor(Node n, Cursor c) {
        n.setCursor(c);
    }
}
