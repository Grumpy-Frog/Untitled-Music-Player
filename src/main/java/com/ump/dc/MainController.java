package com.ump.dc;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.XYChart;
import javafx.scene.effect.Effect;
import javafx.scene.input.DragEvent;
import javafx.scene.media.AudioSpectrumListener;
import javafx.util.Duration;
import org.controlsfx.control.Notifications;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.SepiaTone;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.ResourceBundle;

//https://od.lk/d/NDRfMzM0NTc3NTJf/Eight_Bit_Village_Loop.mp3

public class MainController implements Initializable {

    @FXML
    public Button playButton;
    @FXML
    public Button loopButton;
    @FXML
    public Button shuffleButton;
    @FXML
    public Button muteButton;
    @FXML
    public TextField searchbar;
    @FXML
    public TabPane tabpane;
    @FXML
    public Button themeButton;
    @FXML
    public Label message;
    @FXML
    public Label songName;
    @FXML
    public Label artistName;
    @FXML
    public ImageView albumCover;
    private FileHandler handler;
    private BasicPlayer player;
    @FXML
    private AnchorPane songsTab;
    @FXML
    private AnchorPane artistsTab;
    @FXML
    private AnchorPane albumsTab;
    @FXML
    private AnchorPane genresTab;
    @FXML
    private AnchorPane visualizationsTab;
    @FXML
    private Slider volumeSlider;
    @FXML
    private Slider progressSlider;

    @FXML
    private Label totalTime;
    @FXML
    private Label currentTime;

    @FXML
    private ComboBox<String> speedBox;
    @FXML
    void changeSpeed(ActionEvent event) {
        if (speedBox.getValue() == null) {
            player.fxPlayer.setRate(1.0);
        } else {
            //mediaPlayer.setRate(Double.parseDouble(speedBox.getValue()));
            player.fxPlayer.setRate(Double.parseDouble(speedBox.getValue().substring(1, speedBox.getValue().length() - 1)));
        }
    }

    private Stage stage;
    private Scene scene;
    private Button playingSongButton;

    private enum styles {
        dark,
        light
    }

    private styles current_style;
    private String dark_theme_style;
    private String light_theme_style;
    private String[] keywords;

    private int[] speed = {25, 51, 75, 100, 125, 151, 175, 200};


    @FXML
    private AreaChart<String, Number> spectrum;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        handler = new FileHandler();
        player = new BasicPlayer(handler);

        player.setSpectrum(spectrum);
        player.setVolumeSlider(volumeSlider);
        player.setProgressSlider(progressSlider);
        player.setTcurrentTime(currentTime);
        player.setTotalTime(totalTime);



        songName.textProperty().bind(player.currentSongName);
        songName.textProperty().addListener((observable, oldValue, newValue) ->
                songName.setTooltip(new Tooltip(player.currentSongName.getValue())));
        artistName.textProperty().bind(player.currentArtistName);
        artistName.textProperty().addListener((observable, oldValue, newValue) ->
                artistName.setTooltip(new Tooltip(player.currentArtistName.getValue())));
        albumCover.imageProperty().bind(player.currentAlbumCover);

        dark_theme_style = this.getClass().getResource("stylesheets/dark_theme.css").toExternalForm();
        light_theme_style = this.getClass().getResource("stylesheets/light_theme.css").toExternalForm();
        current_style = styles.dark;

        playingSongButton = new Button("DUMMY VALUE");

        tabpane.widthProperty().addListener((observable, oldValue, newValue) ->
        {
            var tabs = tabpane.getTabs();
            tabpane.setTabMinWidth(tabpane.getWidth() / (tabs.size() + .5));
            tabpane.setTabMaxWidth(tabpane.getWidth() / (tabs.size() + .5));

            tabpane.setTabMinHeight(32);
            tabpane.setTabMaxHeight(32);
        });
        keywords = searchbar.getText().split(" ");
        searchbar.textProperty().addListener((observable, oldValue, newValue) -> {
            keywords = searchbar.getText().split(" ");

            System.out.println("Searching keywords: " + Arrays.toString(keywords));

            updateAllTabs();
        });

        updateAllTabs(); // it's very important that this goes at the very end
        message.setText(handler.getTableSize() + " songs in catalogue");

        for (int i = 0; i < speed.length; i++) {
            speedBox.getItems().add("x" + Double.toString(Double.valueOf(speed[i]) * 0.01));
        }
        speedBox.setOnAction(this::changeSpeed);

        volumeSlider.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
                player.fxPlayer.setVolume(volumeSlider.getValue() * 0.01);
            }
        });

        progressSlider.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number oldValue, Number newValue) {
                double currentTime = player.fxPlayer.getCurrentTime().toSeconds();
                double endTime = player.fxPlayer.getTotalDuration().toSeconds();
                double t = currentTime/endTime;
                t = t*100;
                if(Math.abs(t - newValue.doubleValue()) >0.5)
                {
                    double newVal = newValue.doubleValue()*0.01;
                    newVal = newVal*endTime;
                    player.fxPlayer.seek(Duration.seconds(newVal));
                }
            }
        });



    }

    private void updateAllTabs() {
        updateTab("key", songsTab);
        updateTab("artist", artistsTab);
        updateTab("album", albumsTab);
        updateTab("genre", genresTab);

        System.out.println("All tabs updated.");
    }

    private String padRight(String input, int length) {
        StringBuilder spaces = new StringBuilder();
        if (input.length() <= length) {
            spaces.append(" ".repeat(length - input.length()));
            return input + spaces;
        }
        return input.substring(0, length - 4) + "... ";
    }

    private void updateTab(String fieldName, AnchorPane tab) {
        var tabContents = tab.getChildren();
        tabContents.clear(); // vvi

        var scrollpane = new ScrollPane();
        scrollpane.prefHeightProperty().bind(tab.heightProperty());
        scrollpane.prefWidthProperty().bind(tab.widthProperty());

        // style ScrollPane
//        scrollpane.getStylesheets().add(current_style);

        var vbox = new VBox();
        var vboxContents = vbox.getChildren();
        vbox.setSpacing(5);
        vbox.setPadding(new Insets(10));
        vbox.prefHeightProperty().bind(tab.heightProperty());
        vbox.prefWidthProperty().bind(tab.widthProperty());

        var distinctFields = handler.getDistinctFields(fieldName);
        for (var distinctField : distinctFields) {
            var fieldLabel = new Label();
            fieldLabel.setText(distinctField);
            fieldLabel.setFont(Font.font("Arial", FontWeight.LIGHT, 14));
            if (current_style == styles.dark) {
                fieldLabel.setTextFill(Color.color(1, 1, 1));
            }
            else if (current_style == styles.light) {
                fieldLabel.setTextFill(Color.color(0.25, 0.25, 0.25));
            }
            fieldLabel.setEffect(message.getEffect());
            fieldLabel.setPadding(message.getPadding());

            vboxContents.add(fieldLabel);

            var songsForField = handler.getMatchingRecords(fieldName, distinctField);
            for (var song : songsForField) {
                var title = song.get(1);
                var searched = true;
                for (var word: keywords) {
                    if (!((title.toLowerCase()).contains(word)) && !title.contains(word)) {
                        searched = false;
                        break;
                    }
                }
                if (searched) {
                    var songButton = new Button();
                    songButton.prefWidthProperty().bind(vbox.widthProperty());
                    songButton.setAlignment(Pos.BASELINE_LEFT);
                    if (title == null || title.equals("") || title.equals("null")) {
                        title = song.get(6); // use path if title is unavailable
                    }
                    songButton.setText(
                            padRight(title, 100) +
                                    padRight(song.get(2), 25) +
                                    padRight(song.get(3), 25) +
                                    padRight(song.get(4), 25)
                    );
                    var tooltip = new Tooltip("Imported from \"" + song.get(6) + "\"");
                    songButton.setTooltip(tooltip);
                    songButton.setFont(Font.font("Consolas", FontWeight.EXTRA_LIGHT, 12));
                    if (current_style == styles.dark) {
                        songButton.setTextFill(Color.color(0.75, 0.75, 0.75));
                    } else if (current_style == styles.light) {
                        songButton.setTextFill(Color.color(0.25, 0.25, 0.25));
                    }

                    songButton.setOnMouseClicked(mouseEvent -> {

                        showButtonState(playingSongButton);
                        playingSongButton = songButton;
                        showButtonState(playingSongButton);
                        showButtonState(playButton);

                        player.safeLoadPlayer(Integer.parseInt(song.get(0)));
                        player.playLoaded();
                    });
                    songButton.setOnKeyPressed(keyEvent -> {
                        if (keyEvent.getCode() == KeyCode.ENTER) {
                            songButton.getOnMouseClicked().handle(null);
                        }
                    });

                    vboxContents.add(songButton);
                }
            }
        }

        scrollpane.setContent(vbox);
        tabContents.add(scrollpane);
    }

    @FXML
    public void playButtonAction(ActionEvent event) {
        player.togglePlay();
        showButtonState(playButton);
    }

    @FXML
    public void loopButtonAction(ActionEvent event) {
        player.toggleLoop();
        showButtonState(loopButton);
    }

    @FXML
    public void shuffleButtonAction(ActionEvent event) {
        player.toggleShuffle();
        showButtonState(shuffleButton);
    }

    @FXML
    public void previousButtonAction(ActionEvent event) {
        player.playPrevious();
    }

    @FXML
    public void nextButtonAction(ActionEvent event) {
        player.playNext();
    }

    @FXML
    public void muteButtonAction(ActionEvent event) {
        player.toggleMute();
        showButtonState(muteButton);
    }

    private void showButtonState(Button button) {
        if (button.getEffect() == null) {
            button.setEffect(new SepiaTone(0.75));
        }
        else {
            button.setEffect(null);
        }
    }

    @FXML
    public void floatingModeButtonAction(ActionEvent event) {
        // TODO
    }

    @FXML
    public void importMusicAction(ActionEvent event) {
        // not needed yet
    }


    public void showPushNotification(String title, String text) {
        // show push notifications
        var notification = Notifications.create().title(title).text(text);
        if (current_style == styles.dark) {
            notification.darkStyle();
        }
        notification.hideAfter(Duration.seconds(10));
        notification.showInformation();
    }
    @FXML
    public void fromFileAction(ActionEvent event) {
        var chooser = new FileChooser();
        chooser.setTitle("Select music file");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Audio Files", "*.mp3")
        );
        File file = chooser.showOpenDialog(null);
        if (file != null) {
            var oldTableSize = handler.getTableSize();

            handler.importFile(file);
            updateAllTabs();

            var newTableSize = handler.getTableSize();
            message.setText(newTableSize + " songs in catalogue");

            showPushNotification("Imported " + (newTableSize - oldTableSize) + " song(s)", "from " + file.getParent());
        }
    }

    @FXML
    public void fromMultipleFilesAction(ActionEvent event) {
        var chooser = new FileChooser();
        chooser.setTitle("Select music files");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Audio Files", "*.mp3")
        );
        var files = chooser.showOpenMultipleDialog(null);
        if (files != null) {
            var oldTableSize = handler.getTableSize();

            handler.importMultipleFiles(files);
            updateAllTabs();

            var newTableSize = handler.getTableSize();
            message.setText(newTableSize + " songs in catalogue");

            showPushNotification("Imported " + (newTableSize - oldTableSize) + " song(s)", "from " + files.get(0).getParent());
        }
    }

    @FXML
    public void fromDirectoryAction(ActionEvent event) {
        var chooser = new DirectoryChooser();
        chooser.setTitle("Select directory containing music files");
        var directory = chooser.showDialog(null);
        if (directory != null) {
            var oldTableSize = handler.getTableSize();

            handler.importDirectory(directory);
            updateAllTabs();

            var newTableSize = handler.getTableSize();
            message.setText(newTableSize + " songs in catalogue");

            showPushNotification("Imported " + (newTableSize - oldTableSize) + " song(s)", "from " + directory.getPath());
        }
    }

    @FXML
    public void streamMusicAction(ActionEvent event) {
        var dialog = new TextInputDialog();
        dialog.setTitle("Importing from URL");
        dialog.setHeaderText("Please input a valid URL");
        dialog.setGraphic(null);
        var result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                var url = new URL(result.get());
                player.safeLoadPlayerFromURL(url);

                showPushNotification("Now streaming", url.getPath());
            } catch (MalformedURLException e) {
                Alert popup = new Alert(Alert.AlertType.ERROR, "Please input a valid URL");
                popup.showAndWait();
            }
        }
    }

    @FXML
    public void themeButtonAction(ActionEvent actionEvent) throws IOException {
        var scene = themeButton.getScene();
        scene.getStylesheets().clear();
        if (current_style == styles.dark) {
            scene.getStylesheets().add(light_theme_style);
            current_style = styles.light;
        } else if (current_style == styles.light) {
            scene.getStylesheets().add(dark_theme_style);
            current_style = styles.dark;
        }
        updateAllTabs();
        System.out.println(scene.getStylesheets());
    }

    @FXML
    void progressSliderEntered(MouseEvent event) {

    }

}