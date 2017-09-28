/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package seoulapp.chok.rokseoul.displayingbitmaps.ui;

/**
 * modified by SeongSik Choi (The CHOK) on 2016. 10. 28..
 */

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import seoulapp.chok.rokseoul.firebase.models.DownloadURLs;

/**
 * Simple FragmentActivity to hold the main {@link ImageGridFragment} and not much else.
 */
public class ImageGridActivity extends FragmentActivity implements ValueEventListener{
    private static final String TAG = "ImageGridActivity";

    private FirebaseAuth mAuth;
    private DatabaseReference myDatabaseReference;
    private ValueEventListener myValueEventListener;

    private static ArrayList<DownloadURLs> urlList;
    private static ArrayList<String> urlsKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        myDatabaseReference = FirebaseDatabase.getInstance().getReference().child("users")
                .child(mAuth.getCurrentUser().getUid()).child("downloadURL");

        myValueEventListener = this;
        urlList = new ArrayList<DownloadURLs>();
        urlsKey = new ArrayList<String >();
        loadURSLs();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(myValueEventListener != null){
            myDatabaseReference.removeEventListener(myValueEventListener);
        }
    }

    private void loadURSLs(){
        myDatabaseReference.addListenerForSingleValueEvent(myValueEventListener);
    }
    public static ArrayList getUrlList(){ return urlList; }
    public static ArrayList getUrlsKey(){ return urlsKey; }

    @Override
    public void onDataChange(DataSnapshot dataSnapshot) {
        android.util.Log.d(TAG, "DB success" );
        try{
            android.util.Log.d(TAG, "in DB urlListSize : "+ urlList.size());
            for(DataSnapshot snapshot : dataSnapshot.getChildren()){
                urlList.add(snapshot.getValue(DownloadURLs.class));
                urlsKey.add(snapshot.getKey());
            }



            if(urlList.isEmpty()){
                android.util.Log.d(TAG, "DB isEmpty");
                finish();
            }
            android.util.Log.d(TAG, "children : "+dataSnapshot.getChildrenCount()+
                    "\nlistCount : "+urlList.size());

            if(getSupportFragmentManager().findFragmentByTag(TAG) == null){
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.replace(android.R.id.content, new ImageGridFragment(), TAG).commit();
            }
        }catch (Exception e){
            android.util.Log.e(TAG, "wait"+e);
            e.printStackTrace();
        }
    }

    @Override
    public void onCancelled(DatabaseError databaseError) {
        Log.e(TAG, "DB error"+ databaseError);
        Toast.makeText(getApplicationContext() , "DB Error", Toast.LENGTH_SHORT).show();
        finish();
    }
}
