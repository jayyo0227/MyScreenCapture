package jayyo.myscreencapture;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.EncoderProfiles;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcelable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.util.UUID;

public class MyMediaProjectionService extends Service {
    private static final String TAG = "MyMediaProjectionService";

    private static final String VIDEO_MAIN_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC;
    private static final String AUDIO_MAIN_TYPE = MediaFormat.MIMETYPE_AUDIO_RAW;

    private int resultCode;
    private Intent resultData;
    private MediaProjection mediaProjection = null;
    private MediaCodec mediaCodecEncoder = null;
    private Surface encoderInputSurface;
    private MediaCodec.Callback encoderCallback = new MediaCodec.Callback() {

        @Override
        public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
            Log.d(TAG, "onInputBufferAvailable");
        }

        @Override
        public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {
            Log.d(TAG, "onOutputBufferAvailable");
        }

        @Override
        public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {
            Log.e(TAG, "onError, MediaCodec:" + codec.getName() + ", CodecException:", e);
        }

        @Override
        public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {
            Log.d(TAG, "onOutputFormatChanged, format:" + format);
        }
    };

    private DisplayMetrics displayMetrics = new DisplayMetrics();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        release();
        super.onDestroy();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        getMetrics();
        prepareVideoEncoder(displayMetrics.widthPixels, displayMetrics.heightPixels);
    }

    private void prepareVideoEncoder(int aWidth, int aHeight) {
        Log.i(TAG, "prepareVideoEncoder");
//        aWidth = 1920;
//        aHeight = 1080;
        int aVideoFrameRate = 30;
        int aVideoKeyFrameInterval = 1;
        int aVideoBitrate = 1024 * 1024;//1Mbps=125KB/s
        if (aWidth * aHeight >= 1920 * 1080) {//1080p, 30fps
            aVideoBitrate = 3 * 1024 * 1024;
        } else if (aWidth * aHeight >= 3980 * 2160) {//2160p, 30fps
            aVideoBitrate = 16 * 1024 * 1024;
        }
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            EncoderProfiles.VideoProfile.HDR_DOLBY_VISION;
//        }
        MediaFormat aVideoFormat = MediaFormat.createVideoFormat(VIDEO_MAIN_TYPE, aWidth, aHeight);
//        aVideoFormat.setInteger(MediaFormat.KEY_PROFILE, 0);
//        aVideoFormat.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline);

//        aVideoFormat.setInteger(MediaFormat.KEY_COLOR_RANGE, MediaFormat.COLOR_RANGE_FULL);
//        aVideoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        aVideoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        aVideoFormat.setFloat(MediaFormat.KEY_I_FRAME_INTERVAL, aVideoKeyFrameInterval);
        aVideoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, aVideoFrameRate);
        aVideoFormat.setInteger(MediaFormat.KEY_CAPTURE_RATE, aVideoFrameRate);
//        aVideoFormat.setInteger(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, 1000000 / aVideoFrameRate);
        aVideoFormat.setInteger(MediaFormat.KEY_PRIORITY, 0);
        aVideoFormat.setInteger(MediaFormat.KEY_BIT_RATE, aVideoBitrate);
        aVideoFormat.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR);

        // need to call them as below if failed to start MediaCodec
//        aVideoFormat.setInteger(MediaFormat.KEY_MAX_WIDTH, aWidth);
//        aVideoFormat.setInteger(MediaFormat.KEY_MAX_HEIGHT, aHeight);
        aVideoFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 0);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            aVideoFormat.setInteger(MediaFormat.KEY_PREPEND_HEADER_TO_SYNC_FRAMES, 1);
            aVideoFormat.setFloat(MediaFormat.KEY_MAX_FPS_TO_ENCODER, aVideoFrameRate);
//            aVideoFormat.setFloat(MediaFormat.KEY_MAX_PTS_GAP_TO_ENCODER, 1000000 / aVideoFrameRate);
//            aVideoFormat.setInteger(MediaFormat.KEY_CREATE_INPUT_SURFACE_SUSPENDED, 0);
        }

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            aVideoFormat.setInteger(MediaFormat.KEY_ALLOW_FRAME_DROP, 1);
//        }
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            aVideoFormat.setInteger(MediaFormat.KEY_CROP_TOP, 0);
//            aVideoFormat.setInteger(MediaFormat.KEY_CROP_BOTTOM, 0);
//            aVideoFormat.setInteger(MediaFormat.KEY_CROP_LEFT, 0);
//            aVideoFormat.setInteger(MediaFormat.KEY_CROP_RIGHT, 0);
//        }

        Log.d(TAG, "aVideoFormat:" + aVideoFormat);
        // Create a MediaCodec encoder and configure it. Get a Surface we can use for recording into.
        try {
//            encoderInputSurface = MediaCodec.createPersistentInputSurface();

            mediaCodecEncoder = MediaCodec.createEncoderByType(VIDEO_MAIN_TYPE);
            mediaCodecEncoder.setCallback(encoderCallback);
            mediaCodecEncoder.configure(aVideoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

            encoderInputSurface = mediaCodecEncoder.createInputSurface();

            mediaCodecEncoder.start();
        } catch (MediaCodec.CodecException | IOException e) {
            e.printStackTrace();
            release();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        if ("START_MEDIA_PROJECTION".equals(intent.getAction())) {
            resultCode = intent.getIntExtra("resultCode", 0);
            resultData = (Intent) intent.getParcelableExtra("resultData");
            Log.d(TAG, "resultCode:" + resultCode + ", resultData:" + resultData);
            startNotification();
            getMetrics();
//            prepareVideoEncoder(displayMetrics.widthPixels, displayMetrics.heightPixels);
            startCapture();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void startCapture() {
        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, resultData);
        mediaProjection.registerCallback(new MediaProjection.Callback() {
            @Override
            public void onStop() {
                Log.d(TAG, "mediaProjection.onStop");
                // TODO: request the MediaProjection again
                super.onStop();
//                startActivity(mediaProjectionManager.createScreenCaptureIntent());
            }
        }, null);

        VirtualDisplay virtualDisplay = mediaProjection.createVirtualDisplay(
                "My Virtual Display",
                displayMetrics.widthPixels,
                displayMetrics.heightPixels,
                displayMetrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                encoderInputSurface,//MediaCodec.encoder.getSurface
                new VirtualDisplay.Callback() {
                    @Override
                    public void onPaused() {
                        super.onPaused();
                    }

                    @Override
                    public void onResumed() {
                        super.onResumed();
                    }

                    @Override
                    public void onStopped() {
                        super.onStopped();
                    }
                },
                null);
        Log.d(TAG, "virtualDisplay:" + virtualDisplay);
    }

    private void getMetrics() {
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        Log.d(TAG, "displayMetrics:" + displayMetrics);
    }

    private void requestKeyFrame() {
        if (mediaCodecEncoder == null)
            return;

        Log.i(TAG, "requestKeyFrame");
        Bundle bundle = new Bundle();
        bundle.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0);
        mediaCodecEncoder.setParameters(bundle);
    }

    private void release() {
        Log.d(TAG, "releaseEncoders");
        if (mediaCodecEncoder != null) {
            mediaCodecEncoder.stop();
            mediaCodecEncoder.release();
            mediaCodecEncoder = null;
        }
        if (encoderInputSurface != null) {
            encoderInputSurface.release();
            encoderInputSurface = null;
        }
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }
//        if (mDisplay != null) {
//            mDisplay.release();
//            mDisplay = null;
//        }
    }

    /**
     * PendingIntent on Notification
     */
    private void startNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            String mChannelId = "My Channel Id";
            CharSequence mChannelName = "My Channel Name";
            String mChannelDescription = "My Channel Description";
            String mContentTitle = "My Content Title";
            String mContentText = "My Content Text";

            NotificationChannel channel = new NotificationChannel(mChannelId, mChannelName, NotificationManager.IMPORTANCE_LOW);
            channel.setDescription(mChannelDescription);
            channel.enableLights(false);
            channel.enableVibration(false);
            channel.setVibrationPattern(new long[]{0});
            channel.setSound(null, null);
            notificationManager.createNotificationChannel(channel);

            Intent notificationIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE);

            Bundle bundle = new Bundle();

            Notification.Builder notificationBuilder = new Notification.Builder(getApplicationContext(), mChannelId)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setColor(Color.BLACK)
                    .setExtras(bundle)
                    .setWhen(System.currentTimeMillis())
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setContentTitle(mContentTitle)
                    .setContentText(mContentText)
                    .setContentIntent(pendingIntent);

            Notification notification = notificationBuilder.build();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
            } else {
                startForeground(1, notification);
            }
        }
    }
}
