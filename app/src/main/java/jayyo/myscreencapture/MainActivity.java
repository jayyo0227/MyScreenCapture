package jayyo.myscreencapture;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    public static final int REQUEST_CODE = 1;
    private Intent mMyMediaProjectionService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mMyMediaProjectionService = new Intent(this, MyMediaProjectionService.class);

        requestMediaProjectionPermission();
    }

    private void requestMediaProjectionPermission() {
        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        Intent intent = mediaProjectionManager.createScreenCaptureIntent();
        startActivityForResult(intent, REQUEST_CODE);
//        registerForActivityResult(new ActivityResultContract<Object, Object>() {
//            @NonNull
//            @Override
//            public Intent createIntent(@NonNull Context context, Object o) {
//                return null;
//            }
//
//            @Override
//            public Object parseResult(int i, @Nullable Intent intent) {
//                return null;
//            }
//        }, new ActivityResultCallback<Object>() {
//            @Override
//            public void onActivityResult(Object result) {
//
//            }
//        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult, requestCode:" + requestCode + ", resultCode:" + resultCode + ", data:" + data);
        if (REQUEST_CODE == requestCode && RESULT_OK == resultCode) {
            Log.d(TAG, "prepareMediaProjection");
            if (data == null)
                return;

            mMyMediaProjectionService.setAction("START_MEDIA_PROJECTION");
            mMyMediaProjectionService.putExtra("resultCode", resultCode);
            mMyMediaProjectionService.putExtra("resultData", data);
            startService(mMyMediaProjectionService);
        }
    }

    @Override
    protected void onDestroy() {
        stopService(mMyMediaProjectionService);
        super.onDestroy();
    }
}