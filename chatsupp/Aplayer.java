package org.mintsoft.mintly.chatsupp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.mintsoft.mintly.R;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;


public class Aplayer {
    private static final int progressUpdateTime = 500;
    private Handler mProgressUpdateHandler;
    private MediaPlayer mMediaPlayer;
    private SeekBar mSeekBar;
    private TextView mPlaybackTime;
    private ImageView mPlayButton;
    private static Uri mUri;
    private static Map<String, String> hdr;
    @SuppressLint("StaticFieldLeak")
    private static Aplayer mAplayer;

    public static Aplayer getInstance() {
        if (mAplayer == null) mAplayer = new Aplayer();
        return mAplayer;
    }

    private final Runnable mUpdateProgress = new Runnable() {
        public void run() {
            if (mSeekBar == null) return;
            if (mProgressUpdateHandler != null && mMediaPlayer.isPlaying()) {
                mSeekBar.setProgress((int) mMediaPlayer.getCurrentPosition());
                int currentTime = mMediaPlayer.getCurrentPosition();
                updatePlaytime(currentTime);
                mProgressUpdateHandler.postDelayed(this, progressUpdateTime);
            }
        }
    };

    public void play() {
        /*
        if (mPlayButton == null) {
            throw new IllegalStateException("Play view cannot be null");
        }
        if (mUri == null) {
            throw new IllegalStateException("Uri cannot be null. Call init() before calling this method");
        }
        if (mMediaPlayer == null) {
            throw new IllegalStateException("Call init() before calling this method");
        }
        if (mMediaPlayer.isPlaying()) return;
        */
        if (mPlayButton == null || mUri == null || mMediaPlayer == null || mMediaPlayer.isPlaying())
            return;
        mProgressUpdateHandler.postDelayed(mUpdateProgress, progressUpdateTime);
        setViewsVisibility();
        mMediaPlayer.start();
        setPausable();
    }

    private void setViewsVisibility() {
        if (mSeekBar != null) mSeekBar.setVisibility(View.VISIBLE);
        if (mPlaybackTime != null) mPlaybackTime.setVisibility(View.VISIBLE);
        if (mPlayButton != null) mPlayButton.setVisibility(View.VISIBLE);
    }

    public void pause() {
        if (mMediaPlayer == null) return;
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
            setPlayable();
        }
    }

    private void updatePlaytime(int currentTime) {
        if (mPlaybackTime == null || currentTime < 0) return;
        /*
        if (currentTime < 0) throw new IllegalArgumentException("Current playback time cannot be negative");
        */
        StringBuilder playbackStr = new StringBuilder();
        playbackStr.append(String.format(Locale.getDefault(), "%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes((long) currentTime),
                TimeUnit.MILLISECONDS.toSeconds((long) currentTime) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes((long) currentTime))));
        playbackStr.append("/");
        long totalDuration = 0;
        if (mMediaPlayer != null) {
            try {
                totalDuration = mMediaPlayer.getDuration();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (totalDuration != 0) {
            playbackStr.append(String.format(Locale.getDefault(), "%02d:%02d",
                    TimeUnit.MILLISECONDS.toMinutes((long) totalDuration),
                    TimeUnit.MILLISECONDS.toSeconds((long) totalDuration) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes((long) totalDuration))));
        }
        mPlaybackTime.setText(playbackStr);
    }

    private void setPlayable() {
        if (mPlayButton != null) mPlayButton.setImageResource(R.drawable.ic_play);
    }

    private void setPausable() {
        if (mPlayButton != null) mPlayButton.setImageResource(R.drawable.ic_pause);
    }

    public Aplayer init(Context ctx, Uri uri, Map<String, String> header) {
        if (mAplayer == null) mAplayer = new Aplayer();
        mUri = uri;
        mProgressUpdateHandler = new Handler();
        hdr = header;
        initPlayer(ctx);
        return this;
    }

    public Aplayer setPlayView(ImageView play) {
        if (play == null) throw new NullPointerException("PlayView cannot be null");
        mPlayButton = play;
        return this;
    }

    public void setPlaytime(TextView playTime) {
        mPlaybackTime = playTime;
        updatePlaytime(0);
    }

    public Aplayer setSeekBar(SeekBar seekbar) {
        mSeekBar = seekbar;
        initMediaSeekBar();
        return this;
    }

    private void initPlayer(Context ctx) {
        try {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            if (hdr == null) {
                mMediaPlayer.setDataSource(ctx, mUri);
            } else {
                mMediaPlayer.setDataSource(ctx, mUri, hdr);
            }
            mMediaPlayer.prepare();
            mMediaPlayer.setOnCompletionListener(mOnCompletion);
        } catch (Exception e) {
            mMediaPlayer = null;
            Toast.makeText(ctx, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private final OnCompletionListener mOnCompletion = new OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            int currentPlayTime = 0;
            mSeekBar.setProgress((int) currentPlayTime);
            updatePlaytime(currentPlayTime);
            setPlayable();
        }
    };

    private void initMediaSeekBar() {
        if (mSeekBar == null) return;
        try {
            long finalTime = mMediaPlayer.getDuration();
            mSeekBar.setMax((int) finalTime);
            mSeekBar.setProgress(0);
            mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    if (seekBar.getMax() > seekBar.getProgress()) {
                        mMediaPlayer.seekTo(seekBar.getProgress());
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                }
            });
        } catch (Exception e) {
            mSeekBar.setMax(1);
            mSeekBar.setProgress(1);
        }
    }


    public void release() {
        if (mMediaPlayer != null) {
            mSeekBar.setProgress(0);
            pause();
            mMediaPlayer.stop();
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
            mProgressUpdateHandler = null;
        }
    }

    public boolean isPlaying() {
        return mMediaPlayer != null && mMediaPlayer.isPlaying();
    }
}