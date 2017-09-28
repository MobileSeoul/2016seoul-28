package seoulapp.chok.rokseoul.drawingtool.sensorset;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import seoulapp.chok.rokseoul.R;
import seoulapp.chok.rokseoul.drawingtool.DrawingActivity;
import seoulapp.chok.rokseoul.drawingtool.models.FileNameInfo;
import seoulapp.chok.rokseoul.firebase.StorageSet;

/**
 * Created by SeongSik Choi (The CHOK) on 2016. 9. 30..
 */

public class SensorSet2 implements SensorEventListener {

    public static int LIMITED_CONCURRENT_IMAGE_VISIBILITY_COUNT = 30;

    private SensorManager mSensorManager;
    private Sensor mSensor;
    private int mAzimuth = 0; // degree
    private int mPitch = 0;
    private int mRoll = 0;
    private float fAzimuth = 0;
    private float fPitch=0;
    private float fRoll=0;

    private float[] orientation = new float[3];

    private float[] inRotationMatrix = new float[16];
    private float[] outRotationMatrix = new float[16];
    private DrawingActivity drawingActivity;
    private int sensorCount =0;


    private float mfAzimuth;
    private float mfPitch;
    private float preX;
    private float preY;

    private TextView drawing_degree;
    private FrameLayout imageViewFrame;
    private ArrayList<ImageView> imageList;
    private ArrayList<FileNameInfo> fileNameInfoList;

    private ArrayList<Integer> imageIndex;


    public SensorSet2(DrawingActivity acvitivy, StorageSet storageSet){
        this.drawingActivity = acvitivy;
        mSensorManager = (SensorManager) acvitivy.getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        drawing_degree = (TextView) acvitivy.findViewById(R.id.drawing_degree);
        imageViewFrame = (FrameLayout) acvitivy.findViewById(R.id.imageViewFrame);
        imageList = new ArrayList<ImageView>();
        fileNameInfoList = new ArrayList<FileNameInfo>();
        imageIndex = new ArrayList<Integer>();
    }


    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if( sensorEvent.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR){
            sensorCount++;
            // calculate th rotation matrix
            SensorManager.getRotationMatrixFromVector(inRotationMatrix, sensorEvent.values);
            SensorManager.remapCoordinateSystem(inRotationMatrix, SensorManager.AXIS_X,
                    SensorManager.AXIS_Z, outRotationMatrix);
            SensorManager.getOrientation(outRotationMatrix, orientation);

            fAzimuth = (float) Math.toDegrees( orientation[0] );
            fPitch = (float) Math.toDegrees( orientation[1] );
            fRoll = (float) Math.toDegrees( orientation[2] );

            mfAzimuth = (fAzimuth+360)%360;
            mfPitch = (fPitch+90)%180;

            mAzimuth = (int) (fAzimuth+360) % 360;
            mPitch = (int) (fPitch+90) % 180;
            mRoll = (int) (fRoll+360) % 360;


            drawing_degree.setText("A : "+ mAzimuth+"("+getDirectionFromDegrees(fAzimuth)+")" +"\nP : "+mPitch);

            Log.d("sensorset", sensorCount + " / degree : "+mAzimuth + " / preAZ : " + fAzimuth+ "/ 방위 :" + getDirectionFromDegrees(fAzimuth));
            if(sensorCount %5 == 0 ){
                if(drawingActivity.getDownloadCheck()) {
                    try {
                        showDoodles();
                    }catch (Exception e){
                        Log.d("showdoodle", "burningtime "+e);
                    }
                }
                sensorCount = 0;
            }
            try{
                movingImage(imageList);
            }catch (Exception e){
                Log.d("sensorcheck", "Moving error"+e);
            }
            preX = mfAzimuth;
            preY = mfPitch;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    private String getDirectionFromDegrees(float degrees) {
        if(degrees >= -22.5 && degrees < 22.5) { return "N"; }
        if(degrees >= 22.5 && degrees < 67.5) { return "NE"; }
        if(degrees >= 67.5 && degrees < 112.5) { return "E"; }
        if(degrees >= 112.5 && degrees < 157.5) { return "SE"; }
        if(degrees >= 157.5 || degrees < -157.5) { return "S"; }
        if(degrees >= -157.5 && degrees < -112.5) { return "SW"; }
        if(degrees >= -112.5 && degrees < -67.5) { return "W"; }
        if(degrees >= -67.5 && degrees < -22.5) { return "NW"; }

        return null;
    }

    public String getSensorDirection(){ return getDirectionFromDegrees(fAzimuth); }
    public Integer getSensorAzimuth() { return mAzimuth; }
    public Integer getSensorPitch() { return  mPitch; }
    public Integer getSensorRoll() { return mRoll; }

    public void onResume() {
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_UI);
    }

    public void onPause() {
        mSensorManager.unregisterListener(this);
        imageList.clear();
        fileNameInfoList.clear();
    }

    private void showDoodles(){
        int i = 0;
        for (FileNameInfo fileNameInfo : fileNameInfoList) {
            if (       ((Math.abs(fileNameInfo.fileAzimuth - mAzimuth) <= 10 || Math.abs(fileNameInfo.fileAzimuth - mAzimuth) >= 350))
                    && ((Math.abs(fileNameInfo.filePitch - mPitch) <= 10) || (Math.abs(fileNameInfo.filePitch - mPitch) >= 170))
                    && !fileNameInfo.visivility){
                limitImageList(LIMITED_CONCURRENT_IMAGE_VISIBILITY_COUNT, fileNameInfoList.get(i).filePath, i);
                fileNameInfo.visivility = true;
                Log.d("showdoodle", "createImageView : " + i);
                Log.d("showdoodle", "imageListSize : " + imageList.size());
            }
            else if(   (((Math.abs(fileNameInfo.fileAzimuth - mAzimuth) > 25)
                    && (Math.abs(fileNameInfo.fileAzimuth - mAzimuth) < 335))
                    || (Math.abs(fileNameInfo.filePitch - mPitch) > 25))
                    && fileNameInfo.visivility) {
                removeImageView(i);
                fileNameInfo.visivility = false;
                Log.d("showdoodle", "removeImageView : " + i);
                Log.d("showdoodle", "imageListSize : " + imageList.size());
            }
            i++;
        }
    }

    public void createImageView(String filePath, int fileNum){
        try {
            ImageView imageView = new ImageView(drawingActivity);
            Bitmap bitmap = decodeSampledBitmapFromFile(filePath, imageViewFrame.getWidth(), imageViewFrame.getHeight());
            imageView.setImageBitmap(bitmap);
            imageList.add(imageView);
            imageViewFrame.addView(imageView);
            imageIndex.add(fileNum);
        }catch (Exception e){
            Log.e("showdoodle", "create error "+e);
        }
    }

    private void removeImageView(int i){
        try {
            int j = 0;
            for(int value : imageIndex) {
                if(value == i) {
                    imageViewFrame.removeView(imageList.get(j));
                    imageList.remove(imageList.get(j));
                    imageIndex.remove(j);
                    break;
                }
                j++;
            }

        }catch (Exception e){
            Log.e("showdoodle", "remove error "+e);
        }
    }

    public void limitImageList(int limit, String filePath , int fileNum){
        if(imageIndex.size()>=limit){
            int j = 0;
            long temp = fileNameInfoList.get(fileNum).fileTime;
            int tempIndex = -1;
            for(int value : imageIndex){
                if(temp > fileNameInfoList.get(value).fileTime) {
                    temp = fileNameInfoList.get(value).fileTime;
                    tempIndex = j;
                }
                j++;
            }
            if(tempIndex >= 0){
                imageViewFrame.removeView(imageList.get(tempIndex));
                imageList.remove(tempIndex);
                imageIndex.remove(tempIndex);
                createImageView(filePath, fileNum);
            }
        }else {
            createImageView(filePath, fileNum);
        }
    }

    private int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private Bitmap decodeSampledBitmapFromFile(String filePath, int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(filePath, options);
    }

    private void movingImage(ArrayList<ImageView> imageList){
        int i = 0;
        for(ImageView imageView : imageList) {
            if (imageView.getVisibility() == View.VISIBLE) {
                int x = fileNameInfoList.get(imageIndex.get(i)).fileAzimuth;
                int y = fileNameInfoList.get(imageIndex.get(i)).filePitch;
                if(Math.abs(preX - mfAzimuth)>0.5 || Math.abs(preY - mfPitch)>0.5) {
                    if(x-mfAzimuth < -300 ){
                        imageView.setTranslationX((x-(mfAzimuth-360)) *20);
                    }else if(x-mfAzimuth > 300){
                        imageView.setTranslationX((x-(mfAzimuth+360)) *20);
                    } else {
                        imageView.setTranslationX((x - mfAzimuth) * 20);
                    }
                    imageView.setTranslationY((y - mfPitch) * 20);
                    imageView.invalidate();
                }
                Log.d("movingImage", i + " / x-mfAzimuth = " + (int)(x-mfAzimuth) + " / x : " + x+" / mfAzimuth : " +mfAzimuth);
            }
            i++;
        }
    }

    public void makeValueFromFileName(String fileName, String filePath, boolean visibility){
        FileNameInfo fileNameInfo = new FileNameInfo();
        String value[] = fileName.split(",");
        fileNameInfo.fileAzimuth = Integer.parseInt(value[0]);
        fileNameInfo.filePitch = Integer.parseInt(value[1]);
        fileNameInfo.fileTime = Long.parseLong(value[2]);
        fileNameInfo.filePath = filePath;
        fileNameInfo.visivility = visibility;
        fileNameInfoList.add(fileNameInfo);
    }
}
