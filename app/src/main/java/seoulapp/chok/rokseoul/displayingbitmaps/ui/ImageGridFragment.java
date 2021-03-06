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

import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.storage.FirebaseStorage;

import java.util.ArrayList;

import seoulapp.chok.rokseoul.BuildConfig;
import seoulapp.chok.rokseoul.R;
import seoulapp.chok.rokseoul.displayingbitmaps.util.ImageCache;
import seoulapp.chok.rokseoul.displayingbitmaps.util.ImageFetcher;
import seoulapp.chok.rokseoul.displayingbitmaps.util.Utils;
import seoulapp.chok.rokseoul.firebase.models.DownloadURLs;


/**
 * The main fragment that powers the ImageGridActivity screen. Fairly straight forward GridView
 * implementation with the key addition being the ImageWorker class w/ImageCache to load children
 * asynchronously, keeping the UI nice and smooth and caching thumbnails for quick retrieval. The
 * cache is retained over configuration changes like orientation change so the images are populated
 * quickly if, for example, the user rotates the device.
 */
public class ImageGridFragment extends Fragment implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {
    private static final String TAG = "ImageGridFragment";
    private static final String IMAGE_CACHE_DIR = "thumbs";

    private int mImageThumbSize;
    private int mImageThumbSpacing;
    private ImageAdapter mAdapter;
    private ImageFetcher mImageFetcher;
    private ArrayList<DownloadURLs> urlList;
    private ArrayList<String> urlsKey;
    private boolean removeAllFlag;

    /**
     * Empty constructor as per the Fragment documentation
     */
    public ImageGridFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        Log.d(TAG, "onCreate");
        mImageThumbSize = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size);
        mImageThumbSpacing = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing);

        mAdapter = new ImageAdapter(getActivity());

        ImageCache.ImageCacheParams cacheParams =
                new ImageCache.ImageCacheParams(getActivity(), IMAGE_CACHE_DIR);

        cacheParams.setMemCacheSizePercent(0.25f); // Set memory cache to 25% of app memory

        // The ImageFetcher takes care of loading images into our ImageView children asynchronously
        mImageFetcher = new ImageFetcher(getActivity(), mImageThumbSize);
        mImageFetcher.setLoadingImage(R.drawable.empty_photo);
        mImageFetcher.addImageCache(getActivity().getSupportFragmentManager(), cacheParams);

        urlList = new ArrayList<DownloadURLs>();
        urlsKey = new ArrayList<String>();

        for(int i = (ImageGridActivity.getUrlList().size() -1 ); i>=0 ; i--){
            urlList.add((DownloadURLs) ImageGridActivity.getUrlList().get(i));
            urlsKey.add((String) ImageGridActivity.getUrlsKey().get(i));
        }

        removeAllFlag = false;
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final View v = inflater.inflate(R.layout.image_grid_fragment, container, false);
        final GridView mGridView = (GridView) v.findViewById(R.id.gridView);
        mGridView.setAdapter(mAdapter);
        mGridView.setOnItemClickListener(this);
        mGridView.setOnItemLongClickListener(this);
        mGridView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int scrollState) {
                // Pause fetcher to ensure smoother scrolling when flinging
                if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING) {
                    // Before Honeycomb pause image loading on scroll to help with performance
                    if (!Utils.hasHoneycomb()) {
                        mImageFetcher.setPauseWork(true);
                    }
                } else {
                    mImageFetcher.setPauseWork(false);
                }
            }

            @Override
            public void onScroll(AbsListView absListView, int firstVisibleItem,
                    int visibleItemCount, int totalItemCount) {
            }
        });

        // This listener is used to get the final width of the GridView and then calculate the
        // number of columns and the width of each column. The width of each column is variable
        // as the GridView has stretchMode=columnWidth. The column width is used to set the height
        // of each view so we get nice square thumbnails.
        mGridView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (mAdapter.getNumColumns() == 0) {
                            final int numColumns = (int) Math.floor(
                                    mGridView.getWidth() / (mImageThumbSize + mImageThumbSpacing));
                            if (numColumns > 0) {
                                final int columnWidth =
                                        (mGridView.getWidth() / numColumns) - mImageThumbSpacing;
                                mAdapter.setNumColumns(numColumns);
                                mAdapter.setItemHeight(columnWidth);
                                if (BuildConfig.DEBUG) {
                                    Log.d(TAG, "onCreateView - numColumns set to " + numColumns);
                                }
                                if (Utils.hasJellyBean()) {
                                    mGridView.getViewTreeObserver()
                                            .removeOnGlobalLayoutListener(this);
                                } else {
                                    mGridView.getViewTreeObserver()
                                            .removeGlobalOnLayoutListener(this);
                                }
                            }
                        }
                    }
                });

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        mImageFetcher.setExitTasksEarly(false);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onPause() {
        super.onPause();
        mImageFetcher.setPauseWork(false);
        mImageFetcher.setExitTasksEarly(true);
        mImageFetcher.flushCache();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //mImageFetcher.clearCache();
        mImageFetcher.closeCache();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        Log.d(TAG, "onClick!");
        final Intent i = new Intent(getActivity(), ImageDetailActivity.class);
        i.putExtra(ImageDetailActivity.EXTRA_IMAGE, (int) id);
        if (Utils.hasJellyBean()) {
            // makeThumbnailScaleUpAnimation() looks kind of ugly here as the loading spinner may
            // show plus the thumbnail image in GridView is cropped. so using
            // makeScaleUpAnimation() instead.
            ActivityOptions options =
                    ActivityOptions.makeScaleUpAnimation(v, 0, 0, v.getWidth(), v.getHeight());
            getActivity().startActivity(i, options.toBundle());
        } else {
            startActivity(i);
        }
    }

    // 롱 클릭 시 낙서 지우기 다이얼로그
    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int i, final long id) {
        Log.d(TAG, "LongClick!");
        AlertDialog.Builder removeDialog = new AlertDialog.Builder(getActivity());
        removeDialog.setTitle("낙서 지우기");
        removeDialog.setMessage("이 낙서를 지울까요?");
        removeDialog.setPositiveButton("지우기", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                //save drawing
                Log.d(TAG, "getID : " +id + " / urlList.size : "+urlList.size());
                FirebaseStorage.getInstance().getReferenceFromUrl(urlList.get((int)id).getUrl()).delete()
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "DB Error"+e);
                    }
                });
                FirebaseStorage.getInstance().getReferenceFromUrl(urlList.get((int)id).getThumbnail()).delete()
                        .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "DB Error"+e);
                    }
                });
                removeDB((int)id);
                dialog.dismiss();
            }
        });
        removeDialog.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        removeDialog.show();
        return true;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.main_menu, menu);
    }

    // 메뉴버튼 -> 모두 지우기
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.remove_all:
                AlertDialog.Builder removeDialog = new AlertDialog.Builder(getActivity());
                removeAllFlag = true;
                removeDialog.setTitle("낙서 모두 지우기");
                removeDialog.setMessage("모든 낙서를 지울까요?");
                removeDialog.setPositiveButton("지우기", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(getActivity(), "Remove All", Toast.LENGTH_SHORT).show();
                        //save drawing
                        for(int i =0 ; i<urlList.size(); i++) {
                            FirebaseStorage.getInstance().getReferenceFromUrl(urlList.get(i).getUrl()).delete()
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.e(TAG, "Storage Error"+e);
                                        }
                                    });
                            FirebaseStorage.getInstance().getReferenceFromUrl(urlList.get(i).getThumbnail()).delete()
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.e(TAG, "Storage Error"+e);
                                        }
                                    });
                            removeDB(i);
                        }
                    }
                });
                removeDialog.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                removeDialog.show();

                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // DB 지우기
    private void removeDB(final int id){
        //내 DB의 낙서정보
        FirebaseDatabase.getInstance().getReference()
                .child("users")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child("downloadURL")
                //.child(ImageGridActivity.getUrlsKey().get(id).toString())
                .child(urlsKey.get(id).toString())
                .removeValue().addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(TAG, "DB Error"+e);
            }
        });
        //장소DB의 낙서정보
        FirebaseDatabase.getInstance().getReference()
                .child("QRPlace")
                .child(urlList.get(id).getPlaceName())
                .child("downloadURL")
                //.child(ImageGridActivity.getUrlsKey().get(id).toString())
                .child(urlsKey.get(id).toString())
                .removeValue().addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(TAG, "DB Error"+e);
            }
        });
        //장소 DB의 낙서 갯수
        FirebaseDatabase.getInstance().getReference()
                .child("QRPlace")
                .child(urlList.get(id).getPlaceName())
                .child("doodles")
                .runTransaction(new Transaction.Handler() {
                    int placeDoodles;
                    @Override
                    public Transaction.Result doTransaction(MutableData mutableData) {
                        try {
                            if (mutableData.getValue() == null) {
                                return Transaction.success(mutableData);
                            }
                            placeDoodles = Integer.parseInt(mutableData.getValue().toString());
                        } catch (Exception e) {
                            android.util.Log.d(TAG, "placeDoodles transaction e:" + e);
                        }
                        android.util.Log.d(TAG, "placeDoodles transaction try : " + mutableData);

                        mutableData.setValue(placeDoodles - 1);

                        return Transaction.success(mutableData);
                    }
                    @Override
                    public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                        Log.d(TAG, "Transaction:onComplete placeDoodles : " + dataSnapshot.getValue());
                    }
                });
        //나의 DB 낙서 수 -1
        FirebaseDatabase.getInstance().getReference()
                .child("users")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child("doodles")
                .runTransaction(new Transaction.Handler() {
                    int myDoodles;
                    @Override
                    public Transaction.Result doTransaction(MutableData mutableData) {
                        try {
                            if (mutableData.getValue() == null) {
                                return Transaction.success(mutableData);
                            }
                            myDoodles = Integer.parseInt(mutableData.getValue().toString());
                        } catch (Exception e) {
                            android.util.Log.d(TAG, "myDoodles transaction e:" + e);
                        }

                        android.util.Log.d(TAG, "myDoodles transaction try : " + mutableData);
                        if(!removeAllFlag) mutableData.setValue(myDoodles - 1);
                        else if( id == (urlList.size()-1)) mutableData.setValue(myDoodles - urlList.size());
                        return Transaction.success(mutableData);
                    }

                    @Override
                    public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                        Log.d(TAG, "Transaction:onComplete placeDoodles : " + dataSnapshot.getValue());
                    }
                });
        //총 낙서 수 -1
        FirebaseDatabase.getInstance().getReference()
                .child("totaldoodles")
                .runTransaction(new Transaction.Handler() {
                    int totalDoodles;
                    @Override
                    public Transaction.Result doTransaction(MutableData mutableData) {
                        try {
                            if (mutableData.getValue() == null) {
                                return Transaction.success(mutableData);
                            }
                            totalDoodles = Integer.parseInt(mutableData.getValue().toString());
                        } catch (Exception e) {
                            android.util.Log.d(TAG, "totalDoodles transaction e:" + e);
                        }

                        android.util.Log.d(TAG, "totalDoodles transaction try : " + mutableData);
                        if(!removeAllFlag) mutableData.setValue(totalDoodles - 1);
                        else if( id == (urlList.size()-1)) {
                            mutableData.setValue(totalDoodles -= urlList.size());
                        }
                        return Transaction.success(mutableData);
                    }

                    @Override
                    public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                        Log.d(TAG, "Transaction:onComplete totalDoodles : " + dataSnapshot.getValue());
                        if(!removeAllFlag) {
                            Toast.makeText(getActivity(), "해당 낙서를 삭제했어요.", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(getContext(), ImageGridActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                        }else if( id == (urlList.size()-1)){
                            Intent intent = new Intent(getContext(), ImageGridActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                        }

                    }
                });
    }

    /**
     * The main adapter that backs the GridView. This is fairly standard except the number of
     * columns in the GridView is used to create a fake top row of empty views as we use a
     * transparent ActionBar and don't want the real top row of images to start off covered by it.
     */
    private class ImageAdapter extends BaseAdapter {

        private final Context mContext;
        private int mItemHeight = 0;
        private int mNumColumns = 0;
        private int mActionBarHeight = 0;
        private GridView.LayoutParams mImageViewLayoutParams;

        public ImageAdapter(Context context) {
            super();
            mContext = context;
            mImageViewLayoutParams = new GridView.LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            // Calculate ActionBar height
            TypedValue tv = new TypedValue();
            if (context.getTheme().resolveAttribute(
                    android.R.attr.actionBarSize, tv, true)) {
                mActionBarHeight = TypedValue.complexToDimensionPixelSize(
                        tv.data, context.getResources().getDisplayMetrics());
            }
        }


        @Override
        public int getCount() {
            // If columns have yet to be determined, return no items
            if (getNumColumns() == 0) {
                return 0;
            }

            // Size + number of columns for top empty row
            return urlList.size() + mNumColumns;
        }

        @Override
        public Object getItem(int position) {
            return position < mNumColumns ?
                    null : urlList.get(position - mNumColumns).getThumbnail();

        }

        @Override
        public long getItemId(int position) {
            return position < mNumColumns ? 0 : position - mNumColumns;
        }

        @Override
        public int getViewTypeCount() {
            // Two types of views, the normal ImageView and the top row of empty views
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            return (position < mNumColumns) ? 1 : 0;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup container) {
            //BEGIN_INCLUDE(load_gridview_item)
            // First check if this is the top row
            if (position < mNumColumns) {
                if (convertView == null) {
                    convertView = new View(mContext);
                }
                // Set empty view with height of ActionBar
                convertView.setLayoutParams(new AbsListView.LayoutParams(
                        LayoutParams.MATCH_PARENT, mActionBarHeight));
                return convertView;
            }

            // Now handle the main ImageView thumbnails
            ImageView imageView;
            if (convertView == null) { // if it's not recycled, instantiate and initialize
                imageView = new RecyclingImageView(mContext);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setLayoutParams(mImageViewLayoutParams);
            } else { // Otherwise re-use the converted view
                imageView = (ImageView) convertView;
            }

            // Check the height matches our calculated column width
            if (imageView.getLayoutParams().height != mItemHeight) {
                imageView.setLayoutParams(mImageViewLayoutParams);
            }

            // Finally load the image asynchronously into the ImageView, this also takes care of
            // setting a placeholder image while the background thread runs
            mImageFetcher.loadImage(urlList.get(position - mNumColumns).getThumbnail(), imageView);
            Log.d("Detail", "loadImage num = "+(position-mNumColumns)
                    +"position = "+position+"mNum = "+ mNumColumns);

            return imageView;
            //END_INCLUDE(load_gridview_item)
        }

        /**
         * Sets the item height. Useful for when we know the column width so the height can be set
         * to match.
         *
         * @param height
         */
        public void setItemHeight(int height) {
            if (height == mItemHeight) {
                return;
            }
            mItemHeight = height;
            mImageViewLayoutParams =
                    new GridView.LayoutParams(LayoutParams.MATCH_PARENT, mItemHeight);
            mImageFetcher.setImageSize(height);
            notifyDataSetChanged();
        }

        public void setNumColumns(int numColumns) {
            mNumColumns = numColumns;
        }

        public int getNumColumns() {
            return mNumColumns;
        }
    }
}
