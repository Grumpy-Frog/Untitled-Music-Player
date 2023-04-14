module com.ump.ump_dc {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires javafx.media;


    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;

    requires java.sql;
    requires org.xerial.sqlitejdbc;
    requires mp3agic;
    requires org.controlsfx.controls;

    opens com.ump.dc to javafx.fxml;
    exports com.ump.dc;
}