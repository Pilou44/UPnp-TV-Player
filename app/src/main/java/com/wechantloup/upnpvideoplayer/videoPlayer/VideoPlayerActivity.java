package com.wechantloup.upnpvideoplayer.videoPlayer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.wechantloup.upnpvideoplayer.R;
import com.wechantloup.upnpvideoplayer.dataholder.VideoElement;

import org.jetbrains.annotations.NotNull;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.VLCVideoLayout;

import java.util.ArrayList;

import static android.view.KeyEvent.KEYCODE_BACK;
import static android.view.KeyEvent.KEYCODE_MEDIA_PAUSE;
import static android.view.KeyEvent.KEYCODE_MEDIA_PLAY;
import static android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE;

public class VideoPlayerActivity extends AppCompatActivity implements ControlsOverlay.ControlsOverlayListener {
    public static final String ELEMENT = "CURRENT_ELEMENT";

    private static final boolean USE_TEXTURE_VIEW = false;
    private static final boolean ENABLE_SUBTITLES = true;
    public static final String EXTRA_URLS = "urls";
    public static final String EXTRA_INDEX = "index";

    private VLCVideoLayout mVideoLayout = null;

    private LibVLC mLibVLC = null;
    private MediaPlayer mMediaPlayer = null;
    private ControlsOverlay controls;
    private ArrayList<VideoElement> list;
    private VideoElement current = null;
    private int index;
    private static final String TAG = VideoPlayerActivity.class.getSimpleName();
    private MutableLiveData<Long> duration = new MutableLiveData<>();
    private MutableLiveData<Long> time = new MutableLiveData<>();
    private MutableLiveData<Float> progress = new MutableLiveData<>();
    private MutableLiveData<Boolean> isPlaying = new MutableLiveData<>();
    private boolean next;
    private boolean loop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_video_player);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        loop = prefs.getBoolean("loop", false);
        next = prefs.getBoolean("next", true);

        final ArrayList<String> args = new ArrayList<>();
        args.add("-vvv");
        mLibVLC = new LibVLC(this, args);
        mMediaPlayer = new MediaPlayer(mLibVLC);

        mVideoLayout = findViewById(R.id.video_layout);
        controls = findViewById(R.id.controls);
        list = getIntent().getParcelableArrayListExtra(EXTRA_URLS);
        index = getIntent().getIntExtra(EXTRA_INDEX, 0) - 1;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.i(TAG, "Key down, code=" + keyCode);
        if (keyCode == KEYCODE_BACK) {
            return super.onKeyDown(keyCode, event);
        } else if (keyCode == KEYCODE_MEDIA_PLAY_PAUSE ||
                keyCode == KEYCODE_MEDIA_PLAY ||
                keyCode == KEYCODE_MEDIA_PAUSE) {
            if (isPlaying.getValue()) {
                pause();
            } else {
                resume();
            }
            return true;
        } else if (!controls.isOpened) {
            controls.show(this);
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
            finishWithResult();
        }
    }

    private void finishWithResult() {
        Intent returnIntent = new Intent();
        returnIntent.putExtra(ELEMENT, current);
        setResult(RESULT_OK, returnIntent);
        finish();
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
                    if (!next) {
                        finishWithResult();
                        return;
                    } else if (!loop && index == list.size() - 1) {
                        finishWithResult();
                        return;
                    }
                    next();
                } else if (event.type == MediaPlayer.Event.Playing) {
                    isPlaying.setValue(true);
                } else if (event.type == MediaPlayer.Event.Paused) {
                    isPlaying.setValue(false);
                } else if (event.type == MediaPlayer.Event.LengthChanged) {
//                    Log.i(TAG, "LengthChanged event");
                    duration.setValue(event.getLengthChanged());
                } else if (event.type == MediaPlayer.Event.TimeChanged) {
//                    Log.i(TAG, "TimeChanged event " + event.getTimeChanged());
                    time.setValue(event.getTimeChanged());
                } else if (event.type == MediaPlayer.Event.PositionChanged) {
//                    Log.i(TAG, "PositionChanged event " + event.getPositionChanged());
                    progress.setValue(event.getPositionChanged());
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

        next();
    }

    @Override
    protected void onStop() {
        super.onStop();

        mMediaPlayer.stop();
        mMediaPlayer.detachViews();
    }

    @Override
    public void next() {
        index = (index + 1) % list.size();
        current = list.get(index);
        playCurrent();
    }

    @Override
    public void previous() {
        index = (index - 1 + list.size()) % list.size();
        current = list.get(index);
        playCurrent();
    }

    private void playCurrent() {
        String path = current.getPath();
        final Media media = new Media(mLibVLC, Uri.parse(path));
        mMediaPlayer.setMedia(media);
        media.release();
        mMediaPlayer.play();
    }

    @Override
    public void setPosition(float progress) {
        mMediaPlayer.setPosition(progress);
    }

    @NotNull
    @Override
    public LifecycleOwner getOwner() {
        return this;
    }

    @NotNull
    @Override
    public LiveData<Long> getDuration() {
        return duration;
    }

    @NotNull
    @Override
    public LiveData<Float> getProgress() {
        return progress;
    }

    @NotNull
    @Override
    public LiveData<Long> getTime() {
        return time;
    }

    @NotNull
    @Override
    public LiveData<Boolean> isPlaying() {
        return isPlaying;
    }

    @Override
    public void pause() {
        mMediaPlayer.pause();
    }

    @Override
    public void resume() {
        mMediaPlayer.play();
    }
}
