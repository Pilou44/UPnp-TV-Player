package com.wechantloup.upnpvideoplayer.videoPlayer;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;

import com.wechantloup.upnpvideoplayer.R;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.VLCVideoLayout;

import java.util.ArrayList;

public class VideoPlayerActivity extends Activity {
    private static final boolean USE_TEXTURE_VIEW = false;
    private static final boolean ENABLE_SUBTITLES = true;
    public static final String EXTRA_URL = "url";

    private VLCVideoLayout mVideoLayout = null;

    private LibVLC mLibVLC = null;
    private MediaPlayer mMediaPlayer = null;
    private Uri file;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_video_player);

        final ArrayList<String> args = new ArrayList<>();
        args.add("-vvv");
        mLibVLC = new LibVLC(this, args);
        mMediaPlayer = new MediaPlayer(mLibVLC);

        mVideoLayout = findViewById(R.id.video_layout);

        file = Uri.parse(getIntent().getStringExtra(EXTRA_URL));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMediaPlayer.release();
        mLibVLC.release();
    }

    @Override
    protected void onStart() {
        super.onStart();

        mMediaPlayer.attachViews(mVideoLayout, null, ENABLE_SUBTITLES, USE_TEXTURE_VIEW);

        final Media media = new Media(mLibVLC, file);
        mMediaPlayer.setMedia(media);
        media.release();

        mMediaPlayer.play();
    }

    @Override
    protected void onStop() {
        super.onStop();

        mMediaPlayer.stop();
        mMediaPlayer.detachViews();
    }
}
