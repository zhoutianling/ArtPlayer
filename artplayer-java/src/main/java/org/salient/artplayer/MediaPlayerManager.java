package org.salient.artplayer;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * > Created by Mai on 2018/7/10
 * *
 * > Description: 视频播放器管理类
 * *
 */
public class MediaPlayerManager implements TextureView.SurfaceTextureListener {

    private final String TAG = getClass().getSimpleName();
    private final int CLICK_EVENT_SPAN = 300;
    //surface
    private ResizeTextureView textureView;
    private SurfaceTexture surfaceTexture;
    private Surface surface;

    private PlayerState mPlayerState = PlayerState.IDLE;
    private Object mCurrentData;
    private long mClickTime = 0;
    private Timer mProgressTimer;
    private ProgressTimerTask mProgressTimerTask;

    private int currentOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;

    private int isRotate;//0 代表方向锁定，1 代表没方向锁定

    // settable by client
    private AudioManager.OnAudioFocusChangeListener onAudioFocusChangeListener;
    private AbsMediaPlayer mediaPlayer;
    private boolean isMute = false;
    private boolean isLooping = false;

    private MediaPlayerManager() {
        if (mediaPlayer == null) {
            mediaPlayer = new SystemMediaPlayer();
            onAudioFocusChangeListener = new AudioFocusManager();
        }
    }

    /**
     * 加速度传感器监听
     */
    private OrientationEventListener orientationEventListener;

    public static MediaPlayerManager instance() {
        return ManagerHolder.INSTANCE;
    }

    public PlayerState getPlayerState() {
        return mPlayerState;
    }

    //正在播放的url或者uri
    public Object getDataSource() {
        return mediaPlayer.getDataSource();
    }

    public void setDataSource(Object dataSource, Map<String, String> headers) {
        mediaPlayer.setDataSource(dataSource.toString());
        mediaPlayer.setHeaders(headers);
    }

    public long getDuration() {
        return mediaPlayer.getDuration();
    }

    public void seekTo(long time) {
        mediaPlayer.seekTo(time);
    }

    public void orientationDisable() {
        if (orientationEventListener != null) {
            orientationEventListener.disable();
            orientationEventListener = null;
        }
    }

    /**
     * 竖屏
     */
    public void onOrientationPortrait(Activity activity) {
        VideoView currentFloor = getCurrentVideoView();
        if (currentFloor != null) {
            if (currentOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                return;
            }
            if ((currentOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    || currentOrientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE)
                    && !(currentFloor.getWindowType() == VideoView.WindowType.FULLSCREEN)) {
                currentOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                return;
            }
            currentOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
            VideoView parentVideoView = currentFloor.getParentVideoView();
            if (parentVideoView != null) {
                parentVideoView.setControlPanel(getCurrentControlPanel());
            }
            currentFloor.exitFullscreen();
        }
    }

    /**
     * 横屏
     */
    public void onOrientationLandscape(Activity activity) {
        VideoView currentFloor = getCurrentVideoView();
        if (currentFloor != null) {
            if (currentOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE) return;
            if (currentOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    && (currentFloor.getWindowType() == VideoView.WindowType.FULLSCREEN)) {
                currentOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
                return;
            }
            currentOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
            if (currentFloor.getWindowType() != VideoView.WindowType.FULLSCREEN) {
                //new VideoView
                VideoView videoView = new VideoView(activity);
                //set parent
                videoView.setParentVideoView(currentFloor);
                //optional: set ControlPanel
                videoView.setControlPanel(getCurrentControlPanel());
                videoView.setUp(currentFloor.getDataSourceObject(), VideoView.WindowType.FULLSCREEN, currentFloor.getData());
                //start fullscreen
                videoView.startFullscreen(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            }
        }
    }

    /**
     * 反向横屏
     */
    public void onOrientationReverseLandscape(Activity activity) {
        VideoView currentFloor = getCurrentVideoView();
        if (currentFloor != null) {
            if (currentOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE) return;
            if (currentOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    && (currentFloor.getWindowType() == VideoView.WindowType.FULLSCREEN)) {
                currentOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
                return;
            }
            currentOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
            if (currentFloor.getWindowType() != VideoView.WindowType.FULLSCREEN) {
                //new VideoView
                VideoView videoView = new VideoView(activity);
                //set parent
                videoView.setParentVideoView(currentFloor);
                videoView.setUp(currentFloor.getDataSourceObject(), VideoView.WindowType.FULLSCREEN, currentFloor.getData());
                //optional: set ControlPanel
                videoView.setControlPanel(getCurrentControlPanel());
                //start fullscreen
                videoView.startFullscreen(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
            }
        }
    }

    private long orientationListenerDelayTime = 0;

    public void orientationEnable(final Context context) {
        if (orientationEventListener == null) {
            orientationEventListener = new OrientationEventListener(context, 5) { // 加速度传感器监听，用于自动旋转屏幕
                @Override
                public void onOrientationChanged(int orientation) {
                    Log.d(getClass().getSimpleName(), "onOrientationChanged: " + orientation);
                    Activity activity = (Activity) context;
                    try {
                        //获取是否开启系统
                        isRotate = Settings.System.getInt(context.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION);
                    } catch (Settings.SettingNotFoundException e) {
                        e.printStackTrace();
                    }
                    if (isRotate == 0) return;
                    if ((orientation >= 300 || orientation <= 30) && System.currentTimeMillis() - orientationListenerDelayTime > 1000) { //屏幕顶部朝上
                        onOrientationPortrait(activity);
                        orientationListenerDelayTime = System.currentTimeMillis();
                    } else if (orientation >= 260 && orientation <= 280
                            && System.currentTimeMillis() - orientationListenerDelayTime > 1000) { //屏幕左边朝上
                        onOrientationLandscape(activity);
                        orientationListenerDelayTime = System.currentTimeMillis();
                    } else if (orientation >= 70 && orientation <= 90
                            && System.currentTimeMillis() - orientationListenerDelayTime > 1000) { //屏幕右边朝上
                        onOrientationReverseLandscape(activity);
                        orientationListenerDelayTime = System.currentTimeMillis();
                    }
                }
            };
            currentOrientation = ((AppCompatActivity) context).getRequestedOrientation();
            orientationEventListener.enable();
        }
    }

    public void pause() {
        if (isPlaying()) {
            mediaPlayer.pause();
        }
    }

    public void start() {
        mediaPlayer.start();
    }

    public boolean isMute() {
        return isMute;
    }

    /**
     * 设置静音
     *
     * @param isMute boolean
     */
    public void setMute(boolean isMute) {
        this.isMute = isMute;
        mediaPlayer.mute(isMute);
    }


    /**
     * 设置音量
     *
     * @param leftVolume  左声道
     * @param rightVolume 右声道
     */
    public void setVolume(float leftVolume, float rightVolume) {
        mediaPlayer.setVolume(leftVolume, rightVolume);
    }

    public boolean isLooping() {
        return isLooping;
    }

    /**
     * 设置循环播放
     *
     * @param isLooping boolean
     */
    public void setLooping(boolean isLooping) {
        this.isLooping = isLooping;
        mediaPlayer.setLooping(isLooping);
    }

    /**
     * 在指定的VideoView上播放,
     * 即把textureView放到目标VideoView上
     *
     * @param target VideoView
     */
    public void playAt(VideoView target) {
        if (target == null) return;
        removeTextureView();
        addTextureView(target);
    }

    public boolean isPlaying() {
        return mPlayerState == PlayerState.PLAYING && mediaPlayer.isPlaying();
    }

    public void releasePlayerAndView(Context context) {
        if ((System.currentTimeMillis() - mClickTime) > CLICK_EVENT_SPAN) {
            Log.d(TAG, "release");
            if (context != null) {
                clearFullscreenLayout(context);
                clearTinyLayout(context);
                Utils.scanForActivity(context).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                abandonAudioFocus(context);
                Utils.showSupportActionBar(context);
            }
            releaseMediaPlayer();
            removeTextureView();
            if (surfaceTexture != null) {
                surfaceTexture.release();
            }
            if (surface != null) {
                surface.release();
            }
            textureView = null;
            surfaceTexture = null;
        }
    }

    public void clearFullscreenLayout(Context context) {
        ViewGroup vp = (Utils.scanForActivity(context)).findViewById(Window.ID_ANDROID_CONTENT);
        View oldF = vp.findViewById(R.id.salient_video_fullscreen_id);
        if (oldF != null) {
            vp.removeView(oldF);
            oldF = null;
        }
    }

    public void clearTinyLayout(Context context) {
        ViewGroup vp = (Utils.scanForActivity(context)).findViewById(Window.ID_ANDROID_CONTENT);
        View oldT = vp.findViewById(R.id.salient_video_tiny_id);
        if (oldT != null) {
            vp.removeView(oldT);
            oldT = null;
        }
    }

    public void releaseMediaPlayer() {
        mediaPlayer.release();
        mediaPlayer.setHeaders(null);
        mediaPlayer.setDataSource(null);
        mCurrentData = null;
        updateState(MediaPlayerManager.PlayerState.IDLE);
    }

    /**
     * go into prepare and start
     */
    private void prepare() {
        mediaPlayer.prepare();
        if (surfaceTexture != null) {
            if (surface != null) {
                surface.release();
            }
            surface = new Surface(surfaceTexture);
            mediaPlayer.setSurface(surface);
        }
    }

    public void abandonAudioFocus(Context context) {
        AudioManager mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (mAudioManager != null) {
            mAudioManager.abandonAudioFocus(onAudioFocusChangeListener);
        }
    }

    public void bindAudioFocus(Context context) {
        AudioManager mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (mAudioManager != null) {
            mAudioManager.requestAudioFocus(onAudioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }
    }


    public void setOnAudioFocusChangeListener(AudioManager.OnAudioFocusChangeListener onAudioFocusChangeListener) {
        this.onAudioFocusChangeListener = onAudioFocusChangeListener;
    }

    public void updateState(PlayerState playerState) {
        Log.i(TAG, "updateState [" + playerState.name() + "] ");
        mPlayerState = playerState;
        switch (mPlayerState) {
            case PLAYING:
            case PAUSED:
                startProgressTimer();
                break;
            case ERROR:
            case IDLE:
            case PLAYBACK_COMPLETED:
                cancelProgressTimer();
                break;
        }
        VideoView currentFloor = getCurrentVideoView();
        if (currentFloor != null && currentFloor.isCurrentPlaying()) {
            AbsControlPanel controlPanel = currentFloor.getControlPanel();
            if (controlPanel != null) {
                controlPanel.notifyStateChange();//通知当前的控制面板改变布局
            }
        }
    }

    public void onVideoSizeChanged(int width, int height) {
        Log.i(TAG, "onVideoSizeChanged " + " [" + this.hashCode() + "] ");
        if (textureView != null) {
            textureView.setVideoSize(width, height);
        }
    }

    public void setScreenScale(ScaleType scaleType) {
        if (textureView != null) {
            textureView.setScreenScale(scaleType);
        }
    }

    public void initTextureView(Context context) {
        removeTextureView();
        surfaceTexture = null;
        textureView = new ResizeTextureView(context);
        textureView.setSurfaceTextureListener(MediaPlayerManager.instance());
    }

    public void addTextureView(@NonNull VideoView videoView) {
        if (textureView == null) {
            return;
        }
        Log.d(TAG, "addTextureView [" + this.hashCode() + "] ");
        FrameLayout.LayoutParams layoutParams =
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        Gravity.CENTER);
        videoView.getTextureViewContainer().addView(textureView, layoutParams);
    }

    public void removeTextureView() {
        if (textureView != null && textureView.getParent() != null) {
            ((ViewGroup) textureView.getParent()).removeView(textureView);
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
        if (getCurrentVideoView() == null) return;
        Log.i(TAG, "onSurfaceTextureAvailable [" + "] ");
        if (this.surfaceTexture == null) {
            this.surfaceTexture = surfaceTexture;
            prepare();
        } else {
            textureView.setSurfaceTexture(this.surfaceTexture);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {
        Log.i(TAG, "onSurfaceTextureSizeChanged [" + "] ");
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        Log.i(TAG, "onSurfaceTextureDestroyed [" + "] ");
        return this.surfaceTexture == null;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

    }

    public void setMediaPlayer(AbsMediaPlayer mediaPlayer) {
        this.mediaPlayer = mediaPlayer;
    }

    public AbsMediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    /**
     * 拦截返回键
     */
    public boolean backPress() {
        Log.i(TAG, "backPress");
        try {
            VideoView currentVideoView = getCurrentVideoView();
            if (currentVideoView != null && currentVideoView.getWindowType() == VideoView.WindowType.FULLSCREEN) {//退出全屏
                currentVideoView.exitFullscreen();
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public void startProgressTimer() {
        Log.i(TAG, "startProgressTimer: " + " [" + this.hashCode() + "] ");
        cancelProgressTimer();
        mProgressTimer = new Timer();
        mProgressTimerTask = new ProgressTimerTask();
        mProgressTimer.schedule(mProgressTimerTask, 0, 300);
    }

    public void cancelProgressTimer() {
        if (mProgressTimer != null) {
            mProgressTimer.cancel();
        }
        if (mProgressTimerTask != null) {
            mProgressTimerTask.cancel();
        }
    }

    public long getCurrentPositionWhenPlaying() {
        long position = 0;
        if (mPlayerState == PlayerState.PLAYING || mPlayerState == PlayerState.PAUSED) {
            try {
                position = mediaPlayer.getCurrentPosition();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
        return position;
    }

    /**
     * 获得 MediaPlayer 绑定的 VideoView
     *
     * @return VideoView
     */
    public VideoView getCurrentVideoView() {
        if (textureView == null) return null;
        ViewParent surfaceContainer = textureView.getParent();

        if (surfaceContainer == null) return null;
        ViewParent parent = surfaceContainer.getParent();

        if (parent != null && parent instanceof VideoView) {
            return (VideoView) parent;
        }
        return null;
    }

    public AbsControlPanel getCurrentControlPanel() {
        VideoView currentVideoView = getCurrentVideoView();
        if (currentVideoView == null) return null;
        return currentVideoView.getControlPanel();
    }

    public Object getCurrentData() {
        return mCurrentData;
    }

    public void setCurrentData(Object mCurrentData) {
        this.mCurrentData = mCurrentData;
    }

    // all possible MediaPlayer states
    public enum PlayerState {
        ERROR,
        IDLE,
        PREPARING,
        PREPARED,
        PLAYING,
        PAUSED,
        PLAYBACK_COMPLETED
    }

    //内部类实现单例模式
    private static class ManagerHolder {
        private static final MediaPlayerManager INSTANCE = new MediaPlayerManager();
    }

    public class ProgressTimerTask extends TimerTask {
        @Override
        public void run() {
            if (mPlayerState == PlayerState.PLAYING || mPlayerState == PlayerState.PAUSED) {
                long position = getCurrentPositionWhenPlaying();
                long duration = getDuration();
                int progress = (int) (position * 100 / (duration == 0 ? 1 : duration));
                VideoView currentVideoView = getCurrentVideoView();
                if (currentVideoView == null) return;
                AbsControlPanel controlPanel = currentVideoView.getControlPanel();
                if (controlPanel != null) {
                    controlPanel.onProgressUpdate(progress, position, duration);
                }
            }
        }
    }
}
