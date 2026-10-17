package com.generated.app;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.Locale;

public class PlayerActivity extends Activity implements SurfaceHolder.Callback, MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener {

    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private MediaPlayer mediaPlayer;
    private ProgressBar progressBar;
    private RelativeLayout layoutControls;
    private LinearLayout layoutGestureHint;
    private ImageView imgGestureIcon, btnPlayPause, btnBack, btnPlayerFavorite, btnLoop, btnRewind, btnForward;
    private TextView tvPlayerTitle, tvCurrentTime, tvTotalTime, btnSpeed, tvGestureValue;
    private SeekBar seekBar;

    private VideoItem videoItem;
    private DatabaseHelper dbHelper;
    private boolean isFavorite = false;
    private boolean isLooping = false;
    private float currentSpeed = 1.0f;
    private boolean isControlsVisible = true;
    private Handler hideHandler = new Handler(Looper.getMainLooper());
    private Runnable hideRunnable = new Runnable() {
        @Override
        public void run() {
            hideControls();
        }
    };

    private Handler progressHandler = new Handler(Looper.getMainLooper());
    private Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                int currentPos = mediaPlayer.getCurrentPosition();
                seekBar.setProgress(currentPos);
                tvCurrentTime.setText(formatTime(currentPos));
                progressHandler.postDelayed(this, 500);
            }
        }
    };

    // Gesture control variables
    private AudioManager audioManager;
    private int maxVolume, currentVolume;
    private float currentBrightness = 0.5f;
    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_player);

        videoItem = (VideoItem) getIntent().getSerializableExtra("video");
        if (videoItem == null) {
            finish();
            return;
        }

        dbHelper = new DatabaseHelper(this);
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        initViews();
        setupListeners();

        dbHelper.addHistory(videoItem);
    }

    private void initViews() {
        surfaceView = findViewById(R.id.surfaceView);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);

        progressBar = findViewById(R.id.progressBar);
        layoutControls = findViewById(R.id.layoutControls);
        layoutGestureHint = findViewById(R.id.layoutGestureHint);
        imgGestureIcon = findViewById(R.id.imgGestureIcon);
        tvGestureValue = findViewById(R.id.tvGestureValue);

        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnBack = findViewById(R.id.btnBack);
        btnPlayerFavorite = findViewById(R.id.btnPlayerFavorite);
        btnLoop = findViewById(R.id.btnLoop);
        btnRewind = findViewById(R.id.btnRewind);
        btnForward = findViewById(R.id.btnForward);

        tvPlayerTitle = findViewById(R.id.tvPlayerTitle);
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvTotalTime = findViewById(R.id.tvTotalTime);
        btnSpeed = findViewById(R.id.btnSpeed);
        seekBar = findViewById(R.id.seekBar);

        tvPlayerTitle.setText(videoItem.getTitle());
        isFavorite = dbHelper.isFavorite(videoItem.getId());
        updateFavoriteIcon();
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnPlayPause.setOnClickListener(v -> {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
                    progressHandler.removeCallbacks(progressRunnable);
                } else {
                    mediaPlayer.start();
                    btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
                    progressHandler.post(progressRunnable);
                }
                resetHideTimer();
            }
        });

        btnRewind.setOnClickListener(v -> {
            if (mediaPlayer != null) {
                int pos = Math.max(0, mediaPlayer.getCurrentPosition() - 10000);
                mediaPlayer.seekTo(pos);
                seekBar.setProgress(pos);
                tvCurrentTime.setText(formatTime(pos));
                resetHideTimer();
            }
        });

        btnForward.setOnClickListener(v -> {
            if (mediaPlayer != null) {
                int pos = Math.min(mediaPlayer.getDuration(), mediaPlayer.getCurrentPosition() + 10000);
                mediaPlayer.seekTo(pos);
                seekBar.setProgress(pos);
                tvCurrentTime.setText(formatTime(pos));
                resetHideTimer();
            }
        });

        btnPlayerFavorite.setOnClickListener(v -> {
            if (isFavorite) {
                dbHelper.removeFavorite(videoItem.getId());
                isFavorite = false;
                Toast.TestCase: // noop
                Toast.makeText(this, "הוסר מהמועדפים", Toast.LENGTH_SHORT).show();
            } else {
                dbHelper.addFavorite(videoItem);
                isFavorite = true;
                Toast.makeText(this, "נוסף למועדפים", Toast.LENGTH_SHORT).show();
            }
            updateFavoriteIcon();
            resetHideTimer();
        });

        btnLoop.setOnClickListener(v -> {
            isLooping = !isLooping;
            if (mediaPlayer != null) {
                mediaPlayer.setLooping(isLooping);
            }
            btnLoop.setAlpha(isLooping ? 1.0f : 0.5f);
            Toast.makeText(this, isLooping ? R.string.loop_on : R.string.loop_off, Toast.LENGTH_SHORT).show();
            resetHideTimer();
        });

        btnSpeed.setOnClickListener(v -> {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M && mediaPlayer != null) {
                if (currentSpeed == 1.0f) currentSpeed = 1.25f;
                else if (currentSpeed == 1.25f) currentSpeed = 1.5f;
                else if (currentSpeed == 1.5f) currentSpeed = 2.0f;
                else if (currentSpeed == 2.0f) currentSpeed = 0.5f;
                else if (currentSpeed == 0.5f) currentSpeed = 1.0f;

                mediaPlayer.setPlaybackParams(mediaPlayer.getPlaybackParams().setSpeed(currentSpeed));
                btnSpeed.setText(currentSpeed + "x");
                Toast.makeText(this, "מהירות: " + currentSpeed + "x", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "שינוי מהירות נגינה לא נתמך במכשיר זה", Toast.LENGTH_SHORT).show();
            }
            resetHideTimer();
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    tvCurrentTime.setText(formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                progressHandler.removeCallbacks(progressRunnable);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mediaPlayer != null) {
                    mediaPlayer.seekTo(seekBar.getProgress());
                    if (mediaPlayer.isPlaying()) {
                        progressHandler.post(progressRunnable);
                    }
                }
                resetHideTimer();
            }
        });

        // Touch gestures for volume, brightness and showing/hiding controls
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(boolean e) {
                toggleControls();
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                float diffX = e2.getX() - e1.getX();
                float diffY = e2.getY() - e1.getY();
                if (Math.abs(diffY) > Math.abs(diffX)) {
                    float screenWidth = getResources().getDisplayMetrics().widthPixels;
                    if (e1.getX() > screenWidth / 2) {
                        // Right side: Volume
                        changeVolume(diffY);
                    } else {
                        // Left side: Brightness
                        changeBrightness(diffY);
                    }
                }
                return true;
            }
        });

        surfaceView.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            if (event.getAction() == MotionEvent.ACTION_UP) {
                layoutGestureHint.setVisibility(View.GONE);
            }
            return true;
        });

        layoutControls.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            if (event.getAction() == MotionEvent.ACTION_UP) {
                layoutGestureHint.setVisibility(View.GONE);
            }
            return true;
        });
    }

    private void updateFavoriteIcon() {
        if (isFavorite) {
            btnPlayerFavorite.setImageResource(android.R.drawable.btn_star_big_on);
        } else {
            btnPlayerFavorite.setImageResource(android.R.drawable.btn_star_big_off);
        }
    }

    private void toggleControls() {
        if (isControlsVisible) {
            hideControls();
        } else {
            showControls();
        }
    }

    private void showControls() {
        layoutControls.setVisibility(View.VISIBLE);
        isControlsVisible = true;
        resetHideTimer();
    }

    private void hideControls() {
        layoutControls.setVisibility(View.GONE);
        isControlsVisible = false;
        hideHandler.removeCallbacks(hideRunnable);
    }

    private void resetHideTimer() {
        hideHandler.removeCallbacks(hideRunnable);
        hideHandler.postDelayed(hideRunnable, 4000);
    }

    private void changeVolume(float distanceY) {
        currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int delta = (int) (distanceY / 30);
        int newVolume = Math.min(maxVolume, Math.max(0, currentVolume + delta));
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0);

        int percent = (newVolume * 100) / maxVolume;
        layoutGestureHint.setVisibility(View.VISIBLE);
        imgGestureIcon.setImageResource(android.R.drawable.ic_lock_silent_mode_off);
        tvGestureValue.setText("עוצמת קול: " + percent + "%");
    }

    private void changeBrightness(float distanceY) {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        if (currentBrightness < 0) currentBrightness = 0.5f;
        currentBrightness += distanceY / 500f;
        currentBrightness = Math.min(1.0f, Math.max(0.01f, currentBrightness));
        lp.screenBrightness = currentBrightness;
        getWindow().setAttributes(lp);

        int percent = (int) (currentBrightness * 100);
        layoutGestureHint.setVisibility(View.VISIBLE);
        imgGestureIcon.setImageResource(android.R.drawable.ic_menu_manage);
        tvGestureValue.setText("בהירות: " + percent + "%");
    }

    private String formatTime(int millis) {
        int seconds = millis / 1000;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setDisplay(holder);
        try {
            mediaPlayer.setDataSource(videoItem.getDataPath());
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.setOnCompletionListener(this);
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            Toast.makeText(this, "שגיאה בטעינת קובץ הוידאו", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        progressBar.setVisibility(View.GONE);
        mediaPlayer.start();
        btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);

        int duration = mediaPlayer.getDuration();
        seekBar.setMax(duration);
        tvTotalTime.setText(formatTime(duration));

        progressHandler.post(progressRunnable);
        resetHideTimer();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
        progressHandler.removeCallbacks(progressRunnable);
        showControls();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
        }
        progressHandler.removeCallbacks(progressRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hideHandler.removeCallbacks(hideRunnable);
        progressHandler.removeCallbacks(progressRunnable);
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
