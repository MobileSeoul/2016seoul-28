package seoulapp.chok.rokseoul.drawingtool;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import android.os.Build;
import android.os.Bundle;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.support.annotation.RequiresApi;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

import seoulapp.chok.rokseoul.BaseActivity;
import seoulapp.chok.rokseoul.MainActivity;
import seoulapp.chok.rokseoul.drawingtool.cameraset.Camera2Preview;
import seoulapp.chok.rokseoul.drawingtool.cameraset.Camera_SurfaceTextureListener;
import seoulapp.chok.rokseoul.drawingtool.sensorset.SensorSet2;
import seoulapp.chok.rokseoul.drawingtool.view.AutoFitTextureView;
import seoulapp.chok.rokseoul.drawingtool.view.DrawingView;
import seoulapp.chok.rokseoul.firebase.DownloadService;
import seoulapp.chok.rokseoul.firebase.StorageSet;
import seoulapp.chok.rokseoul.R;
import seoulapp.chok.rokseoul.util.TranslationUtil;

/**
 * This is demo code to accompany the Mobiletuts+ tutorial series:
 * - Android SDK: Create a Drawing App
 *
 * Sue Smith
 * August 2013
 * Modified by SeongSik Choi (The CHOK)
 *
 */
public class DrawingActivity extends BaseActivity implements OnClickListener {

    static String TAG = "DrawingAcitivty";
    //custom drawing view
    private DrawingView drawView;
    //buttons
    private ImageButton currPaint, drawBtn, eraseBtn, newBtn, saveBtn;
    //sizes
    private float smallBrush, mediumBrush, largeBrush;

    //Camera
    //private TextureView textureView;
    private AutoFitTextureView textureView;

    //Camera1preview
    private Camera_SurfaceTextureListener cameraListener;

    //Camera2preview
    private Camera2Preview camera2Preview;
    public static final int REQUEST_CAMERA = 1;

    //저장된 이미지 불러올 뷰
    public FrameLayout imageViewFrame;

    //센서
    private SensorSet2 sensorSet2;

    //다운받은 낙서 수 / 총 낙서수
    private TextView progressDoodles;
    private int doodleCount;


    /**
     * firebase
     */
    private StorageSet storageSet;
    private String placeName = "royalpalace";
    private FirebaseAuth mAuth;

    private BroadcastReceiver mDownloadReceiver;

    private boolean downloadCheck = false;  // 다운로드 완료 됐는지 체크

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_drawing);

        doodleCount = 0;

        mAuth = FirebaseAuth.getInstance();

        imageViewFrame = (FrameLayout) findViewById(R.id.imageViewFrame);
        progressDoodles = (TextView) findViewById(R.id.progress_doodles);

        if(!MainActivity.placeName.isEmpty()) placeName = MainActivity.placeName;
        Log.d("QRcode", "Draw-placeName : " +placeName);
        storageSet = new StorageSet(this, placeName);

        //get drawing view
        drawView = (DrawingView) findViewById(R.id.drawing);

        //get the palette and first color button
        LinearLayout paintLayout = (LinearLayout) findViewById(R.id.paint_colors);
        currPaint = (ImageButton) paintLayout.getChildAt(0);
        currPaint.setImageDrawable(getResources().getDrawable(R.drawable.paint_pressed));

        //sizes from dimensions
        smallBrush = getResources().getInteger(R.integer.small_size);
        mediumBrush = getResources().getInteger(R.integer.medium_size);
        largeBrush = getResources().getInteger(R.integer.large_size);

        //draw button
        drawBtn = (ImageButton) findViewById(R.id.draw_btn);
        drawBtn.setOnClickListener(this);

        //set initial size
        drawView.setBrushSize(smallBrush);

        //erase button
        eraseBtn = (ImageButton) findViewById(R.id.erase_btn);
        eraseBtn.setOnClickListener(this);

        //new button
        newBtn = (ImageButton) findViewById(R.id.new_btn);
        newBtn.setOnClickListener(this);

        //save button
        saveBtn = (ImageButton) findViewById(R.id.save_btn);
        saveBtn.setOnClickListener(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //API21이상
            //textureView = (TextureView) findViewById(R.id.textureView);
            textureView = (AutoFitTextureView) findViewById(R.id.textureView);
            camera2Preview = new Camera2Preview(this, textureView);
        } else {
            //API 19
            cameraListener = new Camera_SurfaceTextureListener(this);
        }

        sensorSet2 = new SensorSet2(this, storageSet);



        mDownloadReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "downloadReceiver:onReceive:" + intent);
                hideProgressDialog();

                if (DownloadService.ACTION_COMPLETED.equals(intent.getAction())) {

                    sensorSet2.makeValueFromFileName(intent.getStringExtra(DownloadService.EXTRA_FILE_NAME)
                            , intent.getStringExtra(DownloadService.EXTRA_DOWNLOAD_PATH), false);
                    doodleCount++;
                    progressDoodles.setText("Download : "+doodleCount+"\nTotal : "+storageSet.getUrls().size());


                    downloadCheck = true;
                    hideProgressDialog();
                }

                if (DownloadService.ACTION_ERROR.equals(intent.getAction())) {
                    String path = intent.getStringExtra(DownloadService.EXTRA_DOWNLOAD_PATH);
                    downloadCheck = false;
                    hideProgressDialog();
                    Log.e(TAG, "download fail path" + path);
                }
            }
        };


    }

    @Override
    public void onStart() {
        super.onStart();

        // Register download receiver
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(mDownloadReceiver, DownloadService.getIntentFilter());
    }

    @Override
    public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mDownloadReceiver);
        Log.d(TAG, "Cachedir : "+getCacheDir());
        getCacheDir().deleteOnExit();

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            camera2Preview.onPause();
            Log.d("SDK", "SDK version 21+: " + Build.VERSION.SDK_INT);
        } else {
            Log.d("SDK", "SDK version 21- : " + Build.VERSION.SDK_INT);
        }
        storageSet.onPause();
        sensorSet2.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            camera2Preview.onResume();
            Log.d("SDK", "SDK version 21+: " + Build.VERSION.SDK_INT);
        } else {
            textureView = (AutoFitTextureView) findViewById(R.id.textureView);
            textureView.setSurfaceTextureListener(cameraListener);
            Log.d("SDK", "SDK version 21- : " + Build.VERSION.SDK_INT);
        }
        storageSet.onResume();
        sensorSet2.onResume();
    }

    /**
     * API 21+에서 카메라 사용을 승인했을 때 다시 카메라뷰를 띄우기위해
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CAMERA:
                for (int i = 0; i < permissions.length; i++) {
                    String permission = permissions[i];
                    int grantResult = grantResults[i];
                    if (permission.equals(Manifest.permission.CAMERA)) {
                        if(grantResult == PackageManager.PERMISSION_GRANTED) {
                            textureView = (AutoFitTextureView) findViewById(R.id.textureView);
                            camera2Preview = new Camera2Preview(this, textureView);
                            camera2Preview.openCamera(textureView.getWidth(), textureView.getHeight());
                            Log.d(TAG,"mPreview set");
                        } else {
                            Toast.makeText(this,"Should have camera permission to run", Toast.LENGTH_LONG).show();
                            finish();
                        }
                    }
                }
                break;
        }
    }



    //download check
    public void setDownloadCheck(boolean check){ downloadCheck = check; }
    public boolean getDownloadCheck(){ return downloadCheck; }

    public SensorSet2 getSensorSet2(){ return sensorSet2;}

    public void setProgressDoodles(){
        progressDoodles.setText("Download : "+ ++doodleCount+"\nTotal : "+ storageSet.getUrls().size());
    }

    //user clicked paint
    public void paintClicked(View view) {
        //use chosen color

        //set erase false
        drawView.setErase(false);
        drawView.setBrushSize(drawView.getLastBrushSize());

        if (view != currPaint) {
            ImageButton imgView = (ImageButton) view;
            String color = view.getTag().toString();
            drawView.setColor(color);
            //update ui
            imgView.setImageDrawable(getResources().getDrawable(R.drawable.paint_pressed));
            currPaint.setImageDrawable(getResources().getDrawable(R.drawable.paint));
            currPaint = (ImageButton) view;
        }
    }


    //그리기 도구 버튼들
    @Override
    public void onClick(View view) {

        if (view.getId() == R.id.draw_btn) {
            //draw button clicked
            final Dialog brushDialog = new Dialog(this);
            brushDialog.setTitle("Brush size:");
            brushDialog.setContentView(R.layout.brush_chooser);
            //listen for clicks on size buttons
            ImageButton smallBtn = (ImageButton) brushDialog.findViewById(R.id.small_brush);
            smallBtn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    drawView.setErase(false);
                    drawView.setBrushSize(smallBrush);
                    drawView.setLastBrushSize(smallBrush);
                    brushDialog.dismiss();
                }
            });
            ImageButton mediumBtn = (ImageButton) brushDialog.findViewById(R.id.medium_brush);
            mediumBtn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    drawView.setErase(false);
                    drawView.setBrushSize(mediumBrush);
                    drawView.setLastBrushSize(mediumBrush);
                    brushDialog.dismiss();
                }
            });
            ImageButton largeBtn = (ImageButton) brushDialog.findViewById(R.id.large_brush);
            largeBtn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    drawView.setErase(false);
                    drawView.setBrushSize(largeBrush);
                    drawView.setLastBrushSize(largeBrush);
                    brushDialog.dismiss();
                }
            });
            //show and wait for user interaction
            brushDialog.show();
        } else if (view.getId() == R.id.erase_btn) {
            //switch to erase - choose size
            final Dialog brushDialog = new Dialog(this);
            brushDialog.setTitle("Eraser size:");
            brushDialog.setContentView(R.layout.brush_chooser);
            //size buttons
            ImageButton smallBtn = (ImageButton) brushDialog.findViewById(R.id.small_brush);
            smallBtn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    drawView.setErase(true);
                    drawView.setBrushSize(smallBrush);
                    brushDialog.dismiss();
                }
            });
            ImageButton mediumBtn = (ImageButton) brushDialog.findViewById(R.id.medium_brush);
            mediumBtn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    drawView.setErase(true);
                    drawView.setBrushSize(mediumBrush);
                    brushDialog.dismiss();
                }
            });
            ImageButton largeBtn = (ImageButton) brushDialog.findViewById(R.id.large_brush);
            largeBtn.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    drawView.setErase(true);
                    drawView.setBrushSize(largeBrush);
                    brushDialog.dismiss();
                }
            });
            brushDialog.show();
        } else if (view.getId() == R.id.new_btn) {
            //new button
            AlertDialog.Builder newDialog = new AlertDialog.Builder(this);
            newDialog.setTitle("New drawing");
            newDialog.setMessage("Start new drawing (you will lose the current drawing)?");
            newDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    drawView.startNew();
                    dialog.dismiss();
                    //imageView.setImageResource(android.R.color.transparent);
                }
            });
            newDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            newDialog.show();
        } else if (view.getId() == R.id.save_btn) {
            //save drawing
            AlertDialog.Builder saveDialog = new AlertDialog.Builder(this);
            saveDialog.setTitle("낙서남기기");
            saveDialog.setMessage(TranslationUtil.transPlaceNameENtoKOR(placeName)
                    +"\n"+DateFormat.getDateTimeInstance().format(new Date())
                    +"\n가로(Azimuth): "+ sensorSet2.getSensorAzimuth()+"°["+sensorSet2.getSensorDirection()+"]"
                    +"\n세로(Pitch) : "+ sensorSet2.getSensorPitch()
                    +"\n이곳에 낙서를 남길까요?");
            saveDialog.setPositiveButton("네", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    //save drawing
                    drawView.setDrawingCacheEnabled(true);

                    //firebase에 저장
                    storageSet.uploadFromMemory(drawView, mAuth.getCurrentUser().getUid()
                            , sensorSet2.getSensorDirection(), sensorSet2.getSensorAzimuth()
                            ,sensorSet2.getSensorPitch(), sensorSet2.getSensorRoll());

                    drawView.destroyDrawingCache();
                    drawView.startNew();
                }
            });
            saveDialog.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            saveDialog.show();
        }
    }
}
