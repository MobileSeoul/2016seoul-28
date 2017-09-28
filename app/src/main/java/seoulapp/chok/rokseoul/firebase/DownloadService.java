/**
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package seoulapp.chok.rokseoul.firebase;

/**
 * modified by SeongSik Choi (The CHOK) on 2016. 10. 28..
 */

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import seoulapp.chok.rokseoul.firebase.models.DownloadURLs;

public class DownloadService extends Service {

    private static final String TAG = "Storage#DownloadService";

    /** Actions **/
    public static final String ACTION_DOWNLOAD = "action_download";
    public static final String ACTION_COMPLETED = "action_completed";
    public static final String ACTION_ERROR = "action_error";

    /** Extras **/
    public static final String EXTRA_DOWNLOAD_PATH = "extra_download_path";
    public static final String EXTRA_FILE_NAME = "extra_file_name";

    private FirebaseStorage storage;
    private int mNumTasks = 0;

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Storage
        storage = FirebaseStorage.getInstance();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand:" + intent + ":" + startId);

        if (ACTION_DOWNLOAD.equals(intent.getAction())) {
            // Get the path to download from the intent
            final ArrayList<DownloadURLs> urls = StorageSet.getUrls();
            // Mark task started
            taskStarted();
            downloadToLocalAll(urls);
        }

        return START_REDELIVER_INTENT;
    }

    private void taskStarted() {
        changeNumberOfTasks(1);
    }

    private void taskCompleted() {
        changeNumberOfTasks(-1);
    }

    private synchronized void changeNumberOfTasks(int delta) {
        Log.d(TAG, "changeNumberOfTasks:" + mNumTasks + ":" + delta);
        mNumTasks += delta;

        // If there are no tasks left, stop the service
        if (mNumTasks <= 0) {
            Log.d(TAG, "stopping");
            stopSelf();
        }
    }

    public static IntentFilter getIntentFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_COMPLETED);
        filter.addAction(ACTION_ERROR);

        return filter;
    }

    private void downloadToLocalAll(ArrayList<DownloadURLs> urls) {

        for (DownloadURLs url : urls) {
            downloadToLocal(url);
        }
    }

    private void downloadToLocal(final DownloadURLs url) {
        StorageReference islandRef = storage.getReferenceFromUrl(url.getUrl());
        try {
            final File localFile = File.createTempFile(url.getFileName(), "png");
            islandRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    Log.d("Download", "다운로드받기성공");

                    Intent broadcast = new Intent(ACTION_COMPLETED);
                    broadcast.putExtra(EXTRA_DOWNLOAD_PATH, localFile.getAbsolutePath());
                    Log.d("Download", "피일경로 : "+localFile.getAbsolutePath());
                    broadcast.putExtra(EXTRA_FILE_NAME, url.getFileName());
                    LocalBroadcastManager.getInstance(getApplicationContext())
                            .sendBroadcast(broadcast);

                    localFile.deleteOnExit();
                    taskCompleted();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Intent broadcast = new Intent(ACTION_ERROR);
                    broadcast.putExtra(EXTRA_DOWNLOAD_PATH, localFile.getAbsolutePath());
                    LocalBroadcastManager.getInstance(getApplicationContext())
                            .sendBroadcast(broadcast);

                    Log.d("Download", "다운로드받기 실패");
                    taskCompleted();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
