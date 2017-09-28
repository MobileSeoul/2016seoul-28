package seoulapp.chok.rokseoul.firebase;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

import seoulapp.chok.rokseoul.drawingtool.DrawingActivity;
import seoulapp.chok.rokseoul.drawingtool.sensorset.SensorSet2;
import seoulapp.chok.rokseoul.drawingtool.view.DrawingView;
import seoulapp.chok.rokseoul.firebase.models.DownloadURLs;

/**
 * Created by SeongSik Choi (The CHOK) on 2016. 9. 13..
 */

public class StorageSet {

    private static final String TAG = "StorageSet";
    private DrawingActivity drawingActivity;

    //Auth
    private FirebaseAuth mAuth;

    //DB
    private DatabaseReference mDatabase;
    private DatabaseReference mPlaceDB;
    private DatabaseReference mPlaceDoodles;
    private DatabaseReference mTotalDoodles;

    private ValueEventListener mPlaceValueEventListener;

    // Create a storage reference from our app
    private FirebaseStorage storage = FirebaseStorage.getInstance();
    private StorageReference storageRef = storage.getReferenceFromUrl("gs://rokseoul-16bb2.appspot.com/");

    private ProgressDialog mProgressDialog;

    private String placeName;
    private Uri mDownloadUrl = null;
    private Uri mThumnailUrl = null;


    private static ArrayList<DownloadURLs> urls;


    private final String doodles = "doodles";

    public StorageSet(final DrawingActivity activity, final String placeName) {
        this.drawingActivity = activity;
        this.placeName = placeName;
        mAuth = FirebaseAuth.getInstance();
        urls = new ArrayList<DownloadURLs>();

        //업로드를 위한 레퍼런스
        mDatabase = FirebaseDatabase.getInstance().getReference().child("users").child(mAuth.getCurrentUser().getUid());
        mPlaceDB = FirebaseDatabase.getInstance().getReference().child("QRPlace").child(placeName).child("downloadURL");
        mPlaceDoodles = FirebaseDatabase.getInstance().getReference().child("QRPlace").child(placeName).child(doodles);
        mTotalDoodles = FirebaseDatabase.getInstance().getReference().child("totaldoodles");
        //앱 시작 후(해당 엑티비티 내에서 DB변경이 있을 경우 실시간 변경
        mPlaceValueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    int i = 0;
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        urls.add(snapshot.getValue(DownloadURLs.class));
                        Log.d(TAG, i + " object URL url : " + urls.get(i++).getUrl());
                    }

                } catch (Exception e) {
                    Log.d(TAG, "downloadurl's key : 에러" + e);
                }
                try {
                    if (!urls.isEmpty()) {
                        activity.showProgressDialog();
                        Intent intent = new Intent(activity, DownloadService.class);
                        intent.setAction(DownloadService.ACTION_DOWNLOAD);
                        activity.startService(intent);
                    }
                } catch (Exception e) {
                    Log.d(TAG, "downloadurl : 에러" + e);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, "mPlaceListener error" + databaseError.toString());
                Toast.makeText(activity, "DB Error", Toast.LENGTH_SHORT).show();
            }
        };
    }

    public void onPause() {
        urls.clear();
        if (mPlaceValueEventListener != null)
            mPlaceDB.removeEventListener(mPlaceValueEventListener);
    }
    public void onResume(){
        mPlaceDB.addListenerForSingleValueEvent(mPlaceValueEventListener);
    }

    public void uploadFromMemory(DrawingView drawView, final String userID
            , final String direction, final int azimuth, final int pitch, final int roll) {


        // [START_EXCLUDE]
        showProgressDialog("Please, Wait...");
        // [END_EXCLUDE]


        Bitmap bitmap = Bitmap.createScaledBitmap(drawView.getDrawingCache(), 360, 640, true);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        final byte[] data = baos.toByteArray();

        bitmap = Bitmap.createScaledBitmap(bitmap, 198, 198, true);
        ByteArrayOutputStream tumbnailBaos = new ByteArrayOutputStream();

        bitmap.compress(Bitmap.CompressFormat.PNG, 80, tumbnailBaos);
        byte[] thumbnail = tumbnailBaos.toByteArray();


        // [START get_child_ref]
        Date date = new Date();
        final String createdTime = DateFormat.getDateTimeInstance().format(date);
        String rawTime = "" + date.getTime();
        final String fileName = azimuth+","+pitch+","+rawTime;
        final StorageReference doodleRef = storageRef.child(placeName).child(direction).child(userID)
                .child(fileName);
        final StorageReference thumnailRef = storageRef.child(placeName).child(direction).child(userID)
                .child(fileName+"(thumnail)");
        // [END get_child_ref]

        // Create file metadata including the content type
        final StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType("image/png")
                .setCustomMetadata("userID", userID)
                .setCustomMetadata("time", rawTime)
                .setCustomMetadata("place", placeName)
                .setCustomMetadata("direction", direction)
                .setCustomMetadata("azimuth", "" + azimuth)
                .setCustomMetadata("pitch", ""+pitch)
                .setCustomMetadata("roll", ""+roll)
                .build();

        UploadTask thumnailUploadTask = thumnailRef.putBytes(thumbnail, metadata);
        thumnailUploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                // Handle unsuccessful uploads
                Log.w(TAG, "uploadFromUri:onFailure", e);

                mThumnailUrl = null;
                // [START_EXCLUDE]
                hideProgressDialog();
                // [END_EXCLUDE]
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Log.d(TAG, "uploadFromUri:thumnailUploadSuccess");

                //업로드한 썸네일 다운로드 경로..
                mThumnailUrl = taskSnapshot.getDownloadUrl();
                UploadTask uploadTask = doodleRef.putBytes(data, metadata);
                uploadTask.addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Handle unsuccessful uploads
                        Log.w(TAG, "uploadFromUri:onFailure", exception);

                        mDownloadUrl = null;
                        // [START_EXCLUDE]
                        hideProgressDialog();
                        // [END_EXCLUDE]
                    }
                }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // Upload succeeded
                        Log.d(TAG, "uploadFromUri:UploadSuccess");

                        //업로드한 그림 다운로드 경로..
                        mDownloadUrl = taskSnapshot.getDownloadUrl();

                        //downloadURL 하위 푸시키 얻기
                        String urlKey = mDatabase.child("downloadURL").push().getKey();


                        // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
                        // users / uID /
                        // 해당유저DB에 낙서 수 +1 , download URL
                        DownloadURLs dUrl = new DownloadURLs(createdTime, fileName, placeName, direction, mDownloadUrl.toString(),
                                mThumnailUrl.toString(), azimuth, pitch, roll);
                        mDatabase.child("downloadURL").child(urlKey).setValue(dUrl);
                        mDatabase.child(doodles).runTransaction(new Transaction.Handler() {
                            int myDoodles;

                            @Override
                            public Transaction.Result doTransaction(MutableData mutableData) {
                                try {
                                    myDoodles = Integer.parseInt(mutableData.getValue().toString());
                                    Log.d(TAG, "myDoodles transaction try : " + mutableData);
                                } catch (Exception e) {
                                    Log.d(TAG, "myDoodles transaction e:" + e);
                                }
                                mutableData.setValue(myDoodles + 1);
                                return Transaction.success(mutableData);
                            }

                            @Override
                            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                                Log.d(TAG, placeName + "-Transaction:onComplete myDoodles : " + dataSnapshot.getValue());
                            }
                        });

                        //장소에 낙서 수 + 1
                        mPlaceDB.child(urlKey).setValue(dUrl);
                        mPlaceDoodles.runTransaction(new Transaction.Handler() {
                            int placeDoodles;

                            @Override
                            public Transaction.Result doTransaction(MutableData mutableData) {
                                try {
                                    placeDoodles = Integer.parseInt(mutableData.getValue().toString());
                                    Log.d(TAG, "placeDoodles transaction try : " + mutableData);
                                } catch (Exception e) {
                                    Log.d(TAG, "placeDoodles transaction e:" + e);
                                }
                                //if(placeDoodles == 0) return Transaction.success(mutableData);
                                mutableData.setValue(placeDoodles + 1);
                                return Transaction.success(mutableData);
                            }

                            @Override
                            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                                Log.d(TAG, placeName + "-Transaction:onComplete placeDoodles : " + dataSnapshot.getValue());
                            }
                        });

                        //TotalDoodles + 1
                        mTotalDoodles.runTransaction(new Transaction.Handler() {
                            int totalDoodles;

                            @Override
                            public Transaction.Result doTransaction(MutableData mutableData) {
                                try {
                                    totalDoodles = Integer.parseInt(mutableData.getValue().toString());
                                } catch (Exception e) {
                                    Log.d(TAG, "totalDoodles transaction e:" + e);
                                }

                                //if(totalDoodles == 0) return Transaction.success(mutableData);
                                Log.d(TAG, "totalDoodles transaction try : " + mutableData);
                                mutableData.setValue(totalDoodles + 1);
                                return Transaction.success(mutableData);
                            }

                            @Override
                            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                                // Transaction completed
                                Log.d(TAG, "totaldoodle Transaction:onComplete totalDoodles : " + dataSnapshot.getValue());
                            }
                        });
                        urls.add(dUrl);
                        downloadToLocal(dUrl);
                        // [START_EXCLUDE]
                        hideProgressDialog();
                        // [END_EXCLUDE]
                        Log.d(TAG, "Upload and download :"+ dUrl.getUrl());
                        Toast.makeText(drawingActivity, "업로드 성공!",
                                Toast.LENGTH_SHORT).show();


                    }
                });
            }
        });

    }

    // [END upload_from_uri]

    /** AR카메라 상태에서 그림을 올렸을 때 바로 동작하기 위한 UI스레드 다운로드
     *  그외 처음은 백그라운드서비스 다운로드
     *  **/


    /** 로컬파일로 받아오기**/
    private void downloadToLocal(final DownloadURLs url) {
        StorageReference islandRef = storage.getReferenceFromUrl(url.getUrl());
        try {
            final File localFile = File.createTempFile(url.getFileName(), "png");
            islandRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    Log.d("Download", "다운로드받기성공");

                    drawingActivity.getSensorSet2().makeValueFromFileName(url.getFileName(), localFile.getAbsolutePath(), true);
                    drawingActivity.getSensorSet2().limitImageList(SensorSet2.LIMITED_CONCURRENT_IMAGE_VISIBILITY_COUNT, localFile.getAbsolutePath(), urls.size()-1);

                    drawingActivity.setProgressDoodles();

                    localFile.deleteOnExit();
                    drawingActivity.setDownloadCheck(true);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d("Download", "다운로드받기 실패");
                    drawingActivity.setDownloadCheck(false);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ArrayList<DownloadURLs> getUrls() { return urls; }

    private void showProgressDialog(String msg) {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(drawingActivity);
            mProgressDialog.setMessage(msg);
            mProgressDialog.setIndeterminate(true);
        }

        mProgressDialog.show();
    }
    private void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }
}
