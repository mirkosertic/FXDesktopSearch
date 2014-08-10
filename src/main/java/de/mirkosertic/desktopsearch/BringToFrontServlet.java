package de.mirkosertic.desktopsearch;

import javafx.application.Platform;
import javafx.stage.Stage;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class BringToFrontServlet extends HttpServlet {

    private final Stage stage;

    public BringToFrontServlet(Stage aStage) {
        stage = aStage;
    }

    @Override
    protected void doGet(HttpServletRequest aRequest, HttpServletResponse aResponse) throws ServletException, IOException {
        Platform.runLater(() -> {
            stage.show();
            stage.toFront();
        });
        aResponse.setStatus(HttpServletResponse.SC_OK);
        aResponse.setContentType("text/plain");
        aResponse.getWriter().print("Ok");
    }
}
