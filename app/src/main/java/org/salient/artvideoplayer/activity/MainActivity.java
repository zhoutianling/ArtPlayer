package org.salient.artvideoplayer.activity;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import org.salient.artplayer.AbsMediaPlayer;
import org.salient.artplayer.MediaPlayerManager;
import org.salient.artplayer.SystemMediaPlayer;
import org.salient.artplayer.VideoView;
import org.salient.artplayer.exo.ExoPlayer;
import org.salient.artplayer.ijk.IjkPlayer;
import org.salient.artplayer.ui.ControlPanel;
import org.salient.artvideoplayer.BaseActivity;
import org.salient.artvideoplayer.R;
import org.salient.artvideoplayer.activity.api.ApiActivity;
import org.salient.artvideoplayer.activity.extension.ExtensionActivity;
import org.salient.artvideoplayer.activity.list.ListActivity;
import org.salient.artvideoplayer.activity.tiny.TinyWindowActivity;
import org.salient.artvideoplayer.activity.orientation.OrientationActivity;

public class MainActivity extends BaseActivity {

    private VideoView videoView;
    private EditText edUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        edUrl = findViewById(R.id.edUrl);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // note : usage sample
        videoView = findViewById(R.id.salientVideoView);
        //optional: set ControlPanel
        final ControlPanel controlPanel = new ControlPanel(this);
        videoView.setControlPanel(controlPanel);
        //optional: set title
        TextView tvTitle = controlPanel.findViewById(R.id.tvTitle);
        tvTitle.setText("西虹市首富 百变首富预告");
        //required: set url
        videoView.setUp("http://vfx.mtime.cn/Video/2018/07/06/mp4/180706094003288023.mp4");
        //videoView.start();
        //optional: set cover
        Glide.with(MainActivity.this)
                .load("http://img5.mtime.cn/mg/2018/07/06/093947.51483272.jpg")
                .into((ImageView) controlPanel.findViewById(R.id.video_cover));
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onBackPressed() {
        if (MediaPlayerManager.instance().backPress()) {
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        hideSoftInput();
        MediaPlayerManager.instance().pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MediaPlayerManager.instance().releasePlayerAndView(this);
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.play:
                String url = edUrl.getText().toString();
                if (!TextUtils.isEmpty(url)) {
                    videoView.setUp(url);
                    videoView.start();
                    TextView tvTitle = videoView.getControlPanel().findViewById(R.id.tvTitle);
                    tvTitle.setText(url);
                    ((ImageView) videoView.getControlPanel().findViewById(R.id.video_cover)).setImageResource(0);
                }
                break;
            case R.id.api:
                startActivity(new Intent(this, ApiActivity.class));
                break;
            case R.id.list:
                startActivity(new Intent(this, ListActivity.class));
                break;
            case R.id.fullWindow:
                hideSoftInput();
                VideoView videoView = new VideoView(this);
                videoView.setUp("http://vfx.mtime.cn/Video/2018/06/29/mp4/180629124637890547.mp4");
                videoView.setControlPanel(new ControlPanel(this));
                videoView.start();
                videoView.startFullscreen(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                break;
            case R.id.tinyWindow:
                startActivity(new Intent(this, TinyWindowActivity.class));
                break;
            case R.id.extension:
                startActivity(new Intent(this, ExtensionActivity.class));
                break;
            case R.id.orientation:
                startActivity(new Intent(this, OrientationActivity.class));
                break;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        hideSoftInput();
    }

    private Menu mMenu;

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        if (mMenu == null) return super.onMenuOpened(featureId, menu);
        AbsMediaPlayer mediaPlayer = MediaPlayerManager.instance().getMediaPlayer();
        if (mediaPlayer instanceof SystemMediaPlayer) {
            mMenu.getItem(1).getSubMenu().getItem(0).setChecked(true);
            mMenu.getItem(0).setTitle("Using: MediaPlayer");
        } else if (mediaPlayer instanceof IjkPlayer) {
            mMenu.getItem(1).getSubMenu().getItem(1).setChecked(true);
            mMenu.getItem(0).setTitle("Using: IjkPlayer");
        } else if (mediaPlayer instanceof ExoPlayer) {
            mMenu.getItem(1).getSubMenu().getItem(2).setChecked(true);
            mMenu.getItem(0).setTitle("Using: ExoPlayer");
        }
        return super.onMenuOpened(featureId, mMenu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_bar_setting, menu);
        mMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        if (item.isChecked()) return super.onOptionsItemSelected(item);
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.menu_MediaPlayer:
                mMenu.getItem(0).setTitle("Using: MediaPlayer");
                MediaPlayerManager.instance().releasePlayerAndView(this);
                MediaPlayerManager.instance().setMediaPlayer(new SystemMediaPlayer());
                break;
            case R.id.menu_IjkPlayer:
                mMenu.getItem(0).setTitle("Using: IjkPlayer");
                MediaPlayerManager.instance().releasePlayerAndView(this);
                MediaPlayerManager.instance().setMediaPlayer(new IjkPlayer());
                break;
            case R.id.menu_ExoPlayer:
                mMenu.getItem(0).setTitle("Using: ExoPlayer");
                MediaPlayerManager.instance().releasePlayerAndView(this);
                MediaPlayerManager.instance().setMediaPlayer(new ExoPlayer(this));
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
