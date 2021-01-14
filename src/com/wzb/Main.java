package com.wzb;

import com.wzb.controller.BtnVboxController;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("views/Window.fxml"));
        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root,1200,900);


        primaryStage.setScene(scene);
        primaryStage.setTitle("QXAFS DAQ");
        primaryStage.setMinWidth(1200);
        primaryStage.setMinHeight(900);
        primaryStage.setResizable(true);
        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                System.exit(0);
            }
        });
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
