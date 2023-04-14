package com.ump.dc;

import com.mpatric.mp3agic.ID3v1;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.Mp3File;

import java.io.File;
import java.io.RandomAccessFile;
import java.sql.*;
import java.util.*;

public class FileHandler {
    // don't change as we don't have a general case metadata parser
    // so only mp3 is supported as of now
    private static final List<String> validExtensions = Arrays.asList(
            ".mp3"
    );
    private static final List<String> metadataKeys = Arrays.asList(
            "title", "artist", "album", "genre", "year"
    );

    Connection conn;

    public FileHandler() {
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:database/music_player.db");
            deleteTable(); // for demo purposes
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        createTable();

        importDirectory(new File("audio/samples"));
    }

    // prints sql statements scheduled for execution
    private void printSQL(String script) {
        System.out.println("Executing SQL: " + '\n' + script + '\n');
    }

    // for altering the database, basically non-'SELECT' statements
    private void databaseUpdate(String script) {
        printSQL(script);

        Statement state = null;
        try {
            state = conn.createStatement();
            state.executeUpdate(script);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    // for executing queries, basically 'SELECT' statements
    private ResultSet databaseQuery(String script) {
        printSQL(script);

        Statement state = null;
        ResultSet result = null;
        try {
            state = conn.createStatement();
            result = state.executeQuery(script);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return result;
    }

    // creates the table
    private void createTable() {
        // 'key INTEGER PRIMARY KEY' automatically becomes an alias for the rowid
        String createTable = """
                CREATE TABLE catalogue
                (
                key INTEGER PRIMARY KEY,
                title TEXT,
                artist TEXT,
                album TEXT,
                genre TEXT,
                year TEXT,
                path TEXT
                );""";
        databaseUpdate(createTable);
    }

    // deletes the table
    private void deleteTable() {
        String deleteTable = "DROP TABLE catalogue; ";
        databaseUpdate(deleteTable);
    }

    // inserts new entries to the table
    public void insertIntoTable(Map<String, String> metadata, String path) {
        // ignoring key/id/serial input as that is tied to rowid and therefore taken care of
        String insertValues = String.format("""
                        INSERT INTO catalogue
                        (title, artist, album, genre, year, path)
                        VALUES
                        ("%s", "%s", "%s", "%s", "%s", "%s")""",
                metadata.get("title"),
                metadata.get("artist"),
                metadata.get("album"),
                metadata.get("genre"),
                metadata.get("year"),
                path
        );
        databaseUpdate(insertValues);
    }

    // returns number of entries in the table
    public int getTableSize() {
        String query = "SELECT COUNT(*) FROM catalogue";
        var result = databaseQuery(query);
        int size = 0;
        try {
            // this will iterate exactly once
            while (result.next()) {
                size = result.getInt(1);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return size;
    }

    // returns a field corresponding to a key
    public String getFieldFromKey(int key, String fieldName) {
        String query = "SELECT " + fieldName + " FROM catalogue WHERE key = " + key + ";";
        var result = databaseQuery(query);
        String field = "";
        try {
            // this will iterate exactly once
            while (result.next()) {
                field = result.getString(1);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return field;
    }

    // returns a record corresponding to a key
    public List<String> getRecordFromKey(int key) {
        String query = "SELECT * FROM catalogue WHERE key = " + key + ";";
        var result = databaseQuery(query);
        List<String> record = null;
        try {
            // this will iterate exactly once
            while (result.next()) {
                record = Arrays.asList(
                        result.getString("key"),
                        result.getString("title"),
                        result.getString("artist"),
                        result.getString("album"),
                        result.getString("genre"),
                        result.getString("year"),
                        result.getString("path")
                );
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return record;
    }

    // returns all the possible distinct fields
    public List<String> getDistinctFields(String fieldName) {
        String query = "SELECT DISTINCT " + fieldName + " FROM catalogue;";
        var result = databaseQuery(query);
        List<String> values = new ArrayList<>();
        try {
            // this will iterate exactly once
            while (result.next()) {
                values.add(result.getString(1));
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return values;
    }

    // return records that match the given field value for the given field name
    public List<List<String>> getMatchingRecords(String fieldName, String field) {
        String query = "SELECT * FROM catalogue WHERE " + fieldName + " = " + "\"" + field + "\"" + ";";
        var result = databaseQuery(query);
        List<List<String>> entries = new ArrayList<>();
        try {
            // this will iterate exactly once
            while (result.next()) {
                entries.add(Arrays.asList(
                        result.getString("key"),
                        result.getString("title"),
                        result.getString("artist"),
                        result.getString("album"),
                        result.getString("genre"),
                        result.getString("year"),
                        result.getString("path")
                ));
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return entries;
    }

    private boolean hasValidExtension(String path) {
        for (var extension : validExtensions) {
            if (path.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    // returns metadata (title, artist, album, genre, year)
    public Map<String, String> getMp3Metadata(File file) {
        Map<String, String> metadata = new HashMap<>();
        var path = file.getAbsolutePath();
        try {
            var mp3file = new Mp3File(path);

            int metadataCount = 5;
            List<String> metadataValues;
            if (mp3file.hasId3v1Tag()) {
                ID3v1 id3v1Tag = mp3file.getId3v1Tag();
                metadataValues = Arrays.asList(
                        id3v1Tag.getTitle(),
                        id3v1Tag.getArtist(),
                        id3v1Tag.getAlbum(),
                        id3v1Tag.getGenreDescription(),
                        id3v1Tag.getYear()
                );
            } else if (mp3file.hasId3v2Tag()) {
                ID3v2 id3v2Tag = mp3file.getId3v2Tag();
                metadataValues = Arrays.asList(
                        id3v2Tag.getTitle(),
                        id3v2Tag.getArtist(),
                        id3v2Tag.getAlbum(),
                        id3v2Tag.getGenreDescription(),
                        id3v2Tag.getYear()
                );
            } else {
                metadataValues = new ArrayList<>();
                for (int i = 0; i < metadataCount; i++) {
                    metadataValues.add(null);
                }
            }
            for (int i = 0; i < metadataCount; i++) {
                metadata.put(metadataKeys.get(i), metadataValues.get(i));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return metadata;
    }

    // returns metadata (album cover)
    public byte[] getMp3AlbumCover(File file) {
        byte[] imageData = null;
        var path = file.getAbsolutePath();
        try {
            var mp3file = new Mp3File(path);
            if (mp3file.hasId3v2Tag()) {
                ID3v2 id3v2Tag = mp3file.getId3v2Tag();
                imageData = id3v2Tag.getAlbumImage();
                if (imageData != null) {
                    String mimeType = id3v2Tag.getAlbumImageMimeType();
                    RandomAccessFile image = new RandomAccessFile("album-artwork" + '.' + mimeType, "rw");
                    image.write(imageData);
                    image.close();
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return imageData;
    }

    // imports a single valid file
    public void importFile(File file) {
        if (file.isFile()) {
            var path = file.getAbsolutePath();
            if (hasValidExtension(path)) {
                var metadata = getMp3Metadata(file);
                insertIntoTable(metadata, path);
            }
        }
    }

    // imports multiple valid files
    public void importMultipleFiles(List<File> files) {
        for (var file : files) {
            importFile(file);
        }
    }

    // imports all valid files in a directory
    public void importDirectory(File directory) {
        var files = directory.listFiles();
        if (files != null) {
            for (var file : files) {
                importFile(file);
            }
        }
    }

}
