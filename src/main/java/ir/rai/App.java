package ir.rai;

import ir.rai.Data.BufferedImageTranscoder;
import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import jfxtras.styles.jmetro.JMetro;
import jfxtras.styles.jmetro.Style;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;

import java.io.IOException;
import java.io.InputStream;

/**
 * JavaFX App
 */
public class App extends Application {

    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
        System.setProperty("prism.lcdtext", "false");
        System.setProperty("awt.useSystemAAFontSettings","on");
        scene = new Scene(loadFXML("primary"), 900, 600);

        ImageView githubImage = new ImageView();
        BufferedImageTranscoder transcoder = new BufferedImageTranscoder();
        try (InputStream file = App.class.getResourceAsStream("icon.svg")) {
            TranscoderInput transIn = new TranscoderInput(file);
            try {
                transcoder.transcode(transIn, null);
                Image img = SwingFXUtils.toFXImage(transcoder.getBufferedImage(), null);
                githubImage.setImage(img);
            } catch (TranscoderException ex) {
                ex.printStackTrace();
            }
        }
        catch (IOException io) {
            io.printStackTrace();
        }

        stage.getIcons().add(githubImage.getImage());
        stage.setMaximized(true);
        stage.setScene(scene);
        JMetro jMetro = new JMetro(Style.LIGHT);
        jMetro.setScene(scene);
        stage.show();
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static InputStream loadFont(String font){
        InputStream is = App.class.getResourceAsStream(font);
        return is;
    }

    public static void main(String[] args) {
        launch();
    }

}