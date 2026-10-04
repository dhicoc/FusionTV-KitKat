package com.fongmi.android.tv.ui.dialog;

import com.fongmi.android.tv.player.Players;

public class FileChooserDialog {

    public static FileChooserDialog create() {
        return new FileChooserDialog();
    }

    public FileChooserDialog player(Players player) {
        return this;
    }

    public FileChooserDialog trackDialog(TrackDialog dialog) {
        return this;
    }

    public void show(Object activity) {
    }
}
