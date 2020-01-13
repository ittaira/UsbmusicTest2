package com.example.usbmusictest2;

import android.net.Uri;

public class Songs {

    String title;
    String atrist;
    String album;
    String filename;

    Uri songUri;

    public Songs(Uri songUri, String title, String atrist, String album, String filename) {
        this.title = title;
        this.atrist = atrist;
        this.album = album;
        this.filename = filename;
        this.songUri = songUri;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAtrist() {
        return atrist;
    }

    public void setAtrist(String atrist) {
        this.atrist = atrist;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Uri getSongUri() {
        return songUri;
    }

    public void setSongUri(Uri songUri) {
        this.songUri = songUri;
    }
}
