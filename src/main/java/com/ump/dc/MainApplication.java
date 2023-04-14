package com.ump.dc;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApplication extends Application {
    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("main_window.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1280, 720); // everything is based on these dimensions
        scene.getStylesheets().add(this.getClass().getResource("stylesheets/dark_theme.css").toExternalForm()); // default css stylesheet
        stage.setTitle("Untitled Music Player");
        stage.getIcons().add(new Image(getClass().getResourceAsStream("images/from_zaed/logo.png")));
        stage.setResizable(false); // TODO fix auto resizing and delete this
        stage.setScene(scene);
        //stage.setMaximized(true);
        stage.show();
    }
}