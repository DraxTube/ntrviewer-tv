package com.ntrviewer.hr;

import org.libsdl.app.SDLActivity;

public class NTRViewerActivity extends SDLActivity {
    @Override
    protected String[] getLibraries() {
        return new String[]{
            "SDL3",
            "main"
        };
    }

    @Override
    protected String[] getArguments() {
        return new String[]{"--sdl-sw"};
    }
}
