package ru.mirea.dikanev.nik.os.UI;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import ru.mirea.dikanev.nik.os.WebParser;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class UIParseWindow{
    private WebParser parser;

    public UIParseWindow(WebParser parser) {
        this.parser = parser;
    }

    public void show(){
        Stage stage = new Stage();

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25, 25, 25, 25));

        Scene scene = new Scene(grid, 300, 100);

        Text status = new Text("Вывод собранных данных");
        status.setFont(Font.font("Tahoma", FontWeight.NORMAL, 20));
        grid.add(status, 0, 0, 1, 3);

        Button btnBuildHtml = new Button("Показать");
        HBox hbBtn = new HBox(10);
        hbBtn.setAlignment(Pos.BOTTOM_RIGHT);
        hbBtn.getChildren().add(btnBuildHtml);
        grid.add(hbBtn, 0, 4);

        btnBuildHtml.setOnAction(e -> {
            System.out.println("Click");
            try {
                Desktop.getDesktop().browse(new URI(parser.buildHtml()));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        stage.setOnCloseRequest((e) -> {
            parser.exit();
        });

        stage.setScene(scene);
        stage.show();
    }


}
