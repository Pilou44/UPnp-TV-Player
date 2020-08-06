package com.wechantloup.upnpvideoplayer.videoPlayer;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;

import com.wechantloup.upnpvideoplayer.R;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.VLCVideoLayout;

import java.util.ArrayList;

import static android.view.KeyEvent.KEYCODE_BACK;
import static android.view.KeyEvent.KEYCODE_MEDIA_PAUSE;
import static android.view.KeyEvent.KEYCODE_MEDIA_PLAY;
import static android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE;

public class VideoPlayerActivity extends Activity {
    private static final boolean USE_TEXTURE_VIEW = false;
    private static final boolean ENABLE_SUBTITLES = true;
    public static final String EXTRA_URLS = "urls";

    private VLCVideoLayout mVideoLayout = null;

    private LibVLC mLibVLC = null;
    private MediaPlayer mMediaPlayer = null;
    private ControlsOverlay controls;
    private String[] list;
    private int index;
    private static final String TAG = VideoPlayerActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_video_player);

        final ArrayList<String> args = new ArrayList<>();
        args.add("-vvv");
        mLibVLC = new LibVLC(this, args);
        mMediaPlayer = new MediaPlayer(mLibVLC);

        mVideoLayout = findViewById(R.id.video_layout);
        controls = findViewById(R.id.controls);

        list = getIntent().getStringArrayExtra(EXTRA_URLS);
        index = 0;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KEYCODE_BACK) {
            return super.onKeyDown(keyCode, event);
        } else if (keyCode == KEYCODE_MEDIA_PLAY_PAUSE ||
                keyCode == KEYCODE_MEDIA_PLAY ||
                keyCode == KEYCODE_MEDIA_PAUSE) {
            onPlayPausePressed();
            return true;
        } else if (!controls.isOpened) {
            controls.show();
            return true;
        } else {
            controls.launchTimer();
            return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    public void onBackPressed() {
        if (controls.isOpened) {
            controls.hide();
        } else {
            super.onBackPressed();
        }
    }

    private void onPlayPausePressed() {
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
        } else {
            mMediaPlayer.play();
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return super.onKeyUp(keyCode, event);
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

        mMediaPlayer.setEventListener(new MediaPlayer.EventListener() {
            private int audioTracksCount = -1;
            private int spuTracksCount = -1;

            @Override
            public void onEvent(MediaPlayer.Event event) {
                if (event.type == MediaPlayer.Event.Stopped) {
                    Log.i(TAG, "Video stopped");
                    index = (index + 1) % list.length;
                    launchNextMedia();
                }
                int newAudioTracksCount = mMediaPlayer.getAudioTracksCount();
                if (newAudioTracksCount != audioTracksCount) {
                    Log.i(TAG, "Audio track count: " + mMediaPlayer.getAudioTracksCount());
                    audioTracksCount = newAudioTracksCount;
                    MediaPlayer.TrackDescription[] audioTracks = mMediaPlayer.getAudioTracks();
                    int frenchTrack = -1;
                    if (audioTracksCount > 0) {
                        for (MediaPlayer.TrackDescription audioTrack : audioTracks) {
                            Log.i(TAG, "Audio track: " + audioTrack.name);
                            Log.i(TAG, "Audio track: " + audioTrack.id);
                            if (audioTrack.name.toLowerCase().contains("vf") || audioTrack.name.toLowerCase().contains("fr")) {
                                frenchTrack = audioTrack.id;
                            }
                        }
                    }
                    if (frenchTrack >= 0) {
                        Log.i(TAG, "Set french audio track: " + mMediaPlayer.setAudioTrack(frenchTrack));
//                        mMediaPlayer.setAudioTrack(2);
                    }
                }

                int newSpuTracks = mMediaPlayer.getSpuTracksCount();
                if (newSpuTracks != spuTracksCount) {
                    Log.i(TAG, "SPU track count: " + mMediaPlayer.getAudioTracksCount());
                    spuTracksCount = newSpuTracks;
                    MediaPlayer.TrackDescription[] spuTracks = mMediaPlayer.getSpuTracks();
                    if (spuTracksCount > 0) {
                        for (MediaPlayer.TrackDescription spuTrack : spuTracks) {
                            Log.i(TAG, "SPU track: " + spuTrack.name);
                            Log.i(TAG, "Audio track: " + spuTrack.id);
                        }
                    }
                    if (spuTracksCount > 0) {
                        Log.i(TAG, "Set spu track: " + spuTracks[0].name + " " + mMediaPlayer.setSpuTrack(spuTracks[0].id));
//                        mMediaPlayer.setSpuTrack(0);
                    }
                }
            }
        });

        launchNextMedia();
    }

    private void launchNextMedia() {
        String path = list[index];
        final Media media = new Media(mLibVLC, Uri.parse(path));
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
