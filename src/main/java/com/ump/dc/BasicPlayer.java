package com.ump.dc;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.media.AudioSpectrumListener;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class BasicPlayer {
    private final FileHandler handler;
    private final Random rand;
    public StringProperty currentSongName;
    public StringProperty currentArtistName;
    public ObjectProperty<Image> currentAlbumCover;
    private boolean loop;
    private boolean shuffle;
    private boolean mute;
    private boolean paused;
    private Media musicMedia;
    public MediaPlayer fxPlayer;
    private int currentKey;
    private String currentPath;

    private Slider volumeSlider;
    private Slider progressSlider;

    private Label totalTime;
    private Label currentTime;
    private AreaChart<String, Number> spectrum;

    public void setSpectrum(AreaChart<String, Number> spectrum) {
        this.spectrum = spectrum;
        // visualization
        initSpectrumChart();
        // visualization end
    }

    public void setTotalTime(Label totalTime) {
        this.totalTime = totalTime;
    }

    public void setTcurrentTime(Label tcurrentTime) {
        this.currentTime = tcurrentTime;
    }

    public void setVolumeSlider(Slider volumeSlider) {
        this.volumeSlider = volumeSlider;
    }

    public void setProgressSlider(Slider progressSlider) {
        this.progressSlider = progressSlider;
    }

    public BasicPlayer(FileHandler val) {
        handler = val;

        loop = false;
        shuffle = false;
        mute = false;
        paused = true;
        currentKey = 1; // starts from 1

        // ordering of these three statements is important
        currentSongName = new SimpleStringProperty("");
        currentArtistName = new SimpleStringProperty("");
        currentAlbumCover = new SimpleObjectProperty<Image>();
        loadPlayer(currentKey);

        rand = new Random();
    }

    // use 'safeLoad' instead of 'load' when possible
    private void loadPlayer(int key) {
        var data = handler.getRecordFromKey(key);
        currentKey = key;
        currentPath = data.get(6);

        // wrapping in a File to take care of invalid characters in the name
        var musicFile = new File(currentPath);

        currentSongName.setValue(data.get(1));
        currentArtistName.setValue(data.get(2));
        var coverImage = handler.getMp3AlbumCover(musicFile);
        if (coverImage == null) {
            var dummyImagePath = this.getClass().getResource("images/from_zaed/play_button.png").toExternalForm();
            currentAlbumCover.setValue(new Image(dummyImagePath));
        } else {
            currentAlbumCover.setValue(new Image(new ByteArrayInputStream(coverImage)));
        }
        System.out.println("\"" + currentPath + "\"" + " loaded in player\n");

        musicMedia = new Media(musicFile.toURI().toString());
        fxPlayer = new MediaPlayer(musicMedia);
        fxPlayer.setOnEndOfMedia(this::handleEnd);

//        controller.albumCover.setImage(new Image("album-artwork"));
    }

    public void safeLoadPlayer(int index) {
        fxPlayer.dispose();
        fxPlayer.setAudioSpectrumListener(new SpektrumListener());
        fxPlayer.setVolume(volumeSlider.getValue() * 0.01);
        loadPlayer(index);
    }

    public void safeLoadPlayerFromURL(URL link) {
        var path = link.toString();

        if (path.endsWith(".mp3")) {
            var musicMedia = new Media(path);
            fxPlayer.dispose();
            fxPlayer = new MediaPlayer(musicMedia);
            fxPlayer.setOnEndOfMedia(this::handleEnd);

            currentPath = path;
            currentSongName.setValue(path);
            currentArtistName.setValue("Streaming *.mp3 file from " + link.getHost());

            var dummyImagePath = this.getClass().getResource("images/from_zaed/play_button.png").toExternalForm();
            currentAlbumCover.setValue(new Image(dummyImagePath));

        } else {
            Alert popup = new Alert(Alert.AlertType.ERROR, "Given URL doesn't point to a *.mp3 file");
            popup.showAndWait();
        }
    }

    public void playLoaded() {
        //cancelTimer();
        beginTimer();
        fxPlayer.setAudioSpectrumListener(new SpektrumListener());
        paused = false;
        fxPlayer.play();
    }

    private void playLoadedFromStart() {
        fxPlayer.seek(Duration.ZERO);
        playLoaded();
    }

    public void pauseLoaded() {
        paused = true;
        fxPlayer.pause();
        cancelTimer();
    }

    public void resetLoaded() {
        safeLoadPlayer(currentKey);
        if (!paused) {
            playLoaded();
        }
    }

    private void circularIncrement() {
        if (currentKey + 1 > handler.getTableSize()) {
            currentKey = 1;
        } else {
            currentKey += 1;
        }
    }

    private void circularDecrement() {
        if (currentKey - 1 < 1) {
            currentKey = handler.getTableSize();
        } else {
            currentKey -= 1;
        }
    }

    public void playPrevious() {
        circularDecrement();
        safeLoadPlayer(currentKey);
        if (!paused) {
            playLoaded();
        }
    }

    public void playNext() {
        circularIncrement();
        safeLoadPlayer(currentKey);
        if (!paused) {
            playLoaded();
        }
    }

    public void playRandom() {
        currentKey = rand.nextInt(1, handler.getTableSize() + 1); // +1 because exclusive
        safeLoadPlayer(currentKey);
        // since there is no dedicated play-random button, this conditional is redundant
//        if (!paused) {
        playLoaded();
//        }
    }

    public void toggleLoop() {
        loop = !loop;
    }

    public void toggleShuffle() {
        shuffle = !shuffle;
    }

    public void toggleMute() {
        mute = !mute;
        fxPlayer.setMute(mute);
    }

    public void togglePlay() {
        if (paused) {
            playLoaded();
        } else {
            pauseLoaded();
        }
    }

    public void handleEnd() {
        if (loop) {
            playLoadedFromStart();
            System.out.println("Playing the same song once again.");
        } else if (shuffle) {
            playRandom();
            System.out.println("Playing a random song from catalogue.");
        } else {
            playNext();
            System.out.println("Playing the next song in catalogue.");
        }

    }


    private Timer timer;
    private TimerTask task;
    private boolean running;

    public void beginTimer() {
        timer = new Timer();
        task = new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() ->{
                    running = true;
                    double current = fxPlayer.getCurrentTime().toSeconds();
                    double end = musicMedia.getDuration().toSeconds();
                    //System.out.println(current);

                    int minutesEnd = ((int)end % 3600) / 60;
                    int secondsEnd = (int)end % 60;
                    String timeStringEnd = String.format("%02d:%02d", minutesEnd, secondsEnd);

                    int minutesCurrent = ((int)current % 3600) / 60;
                    int secondsCurrent = (int)current % 60;
                    String timeStringCurrent = String.format("%02d:%02d", minutesCurrent, secondsCurrent);

                    totalTime.setText(timeStringEnd);
                    currentTime.setText(timeStringCurrent);

                    progressSlider.setValue((current / end)*100);


                    if (current / end == 1) {
                        cancelTimer();
                    }
                });
            }
        };

        timer.scheduleAtFixedRate(task, 0, 1000);
    }

    public void cancelTimer() {
        running = false;
        timer.cancel();
    }


    // visualization
    private static final int BANDS = 128;
    private static final double DROPDOWN = 0.25;
    private XYChart.Data[] series1Data;

    public void initSpectrumChart()
    {
        XYChart.Series<String, Number> series1 = new XYChart.Series<>();
        series1Data = new XYChart.Data[BANDS + 2];
        for (int i = 0; i < series1Data.length; i++) {
            series1Data[i] = new XYChart.Data<>(Integer.toString(i + 1), 0);
            //noinspection unchecked
            series1.getData().add(series1Data[i]);
        }
        spectrum.getData().add(series1);
    }


    private class SpektrumListener implements AudioSpectrumListener {
        float[] buffer = createFilledBuffer(BANDS, fxPlayer.getAudioSpectrumThreshold());

        @Override
        public void spectrumDataUpdate(double timestamp, double duration, float[] magnitudes, float[] phases) {

            for (int i = 0; i < magnitudes.length; i++) {
                if (magnitudes[i] >= buffer[i]) {
                    buffer[i] = magnitudes[i];
                    series1Data[i].setYValue(magnitudes[i] - fxPlayer.getAudioSpectrumThreshold());
                } else {
                    series1Data[i].setYValue(buffer[i] - fxPlayer.getAudioSpectrumThreshold());
                    buffer[i] -= 0.25;
                }
            }
        }
    }

    private float[] createFilledBuffer(int size, float fillValue) {
        float[] floats = new float[size];
        Arrays.fill(floats, fillValue);
        return floats;
    }
}
