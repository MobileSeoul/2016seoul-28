package seoulapp.chok.rokseoul;

/**
 * Created by SeongSik Choi, JIEUN (The CHOK) on 2016. 9. 29..
 */

import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import seoulapp.chok.rokseoul.displayingbitmaps.ui.ImageGridActivity;
import seoulapp.chok.rokseoul.drawingtool.DrawingActivity;
import seoulapp.chok.rokseoul.firebase.GoogleSignInActivity;
import seoulapp.chok.rokseoul.firebase.models.User;
import seoulapp.chok.rokseoul.maps.MapsActivity;
import seoulapp.chok.rokseoul.util.TranslationUtil;


public class MainActivity extends BaseActivity
        implements NavigationView.OnNavigationItemSelectedListener {


    private static final String TAG = "MainActivity";
    private FirebaseAuth mAuth;
    private DatabaseReference mMyDatabase;
    private DatabaseReference mTotalDoodles;
    private ValueEventListener mMyValueEventListener;
    private ValueEventListener mTotalValueEventListener;

    private FragmentManager fm;

    private TextView profile_userName, profile_email, profile_doodles, profile_totaldoodles;
    private String myDoodleCount="";
    public static String placeName="";

    private ProgressBar progressBar_main;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        progressBar_main = (ProgressBar) findViewById(R.id.progressbar_main);
        progressBar_main.setVisibility(View.VISIBLE);
        mAuth = FirebaseAuth.getInstance();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                IntentIntegrator scanIntegrator = new IntentIntegrator(MainActivity.this);
                scanIntegrator.setCaptureActivity(QRAcvitivy.class);
                scanIntegrator.setOrientationLocked(true);
                scanIntegrator.initiateScan();


                //startActivity(new Intent(getApplicationContext(), DrawingActivity.class));
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        drawer.openDrawer(navigationView);

        View v = navigationView.getHeaderView(0);
        profile_userName = (TextView) v.findViewById(R.id.profile_userName);
        profile_email = (TextView) v.findViewById(R.id.profile_email);
        profile_doodles = (TextView) v.findViewById(R.id.profile_doodles2);
        profile_totaldoodles = (TextView) v.findViewById(R.id.profile_totaldoodles);

        v.findViewById(R.id.profile_imageView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GoogleSignInActivity.stayLogin = false;
                Intent intent = new Intent(getApplicationContext(),GoogleSignInActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
            }
        });
        v.findViewById(R.id.profile_doodles1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(myDoodleCount.isEmpty() || myDoodleCount.equals("0")){
                    Toast.makeText(getApplicationContext(), "게시물이 없습니다.", Toast.LENGTH_SHORT).show();
                }else {
                    startActivity(new Intent(getApplicationContext(), ImageGridActivity.class));
                }
            }
        });


        //DB경로
        mMyDatabase = FirebaseDatabase.getInstance().getReference().child("users").child(mAuth.getCurrentUser().getUid());
        mTotalDoodles = FirebaseDatabase.getInstance().getReference().child("totaldoodles");

        //앱 시작 후(해당 엑티비티 내에서 DB변경이 있을 경우 실시간 변경
        mMyValueEventListener = new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    User user = dataSnapshot.getValue(User.class);
                    profile_userName.setText(user.username);
                    profile_email.setText(user.email);
                    myDoodleCount = dataSnapshot.child("doodles").getValue().toString();
                    profile_doodles.setText(myDoodleCount);
                    Log.d("MainActivity", "datasnapshot.doodles(realtime): " + myDoodleCount);
                }catch (Exception e){
                    Log.e(TAG, "databaseDoodle Error"+e);
                }
                progressBar_main.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d("MainActivity", "DB Error");
            }
        };

        mTotalValueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    String totalDoodles = dataSnapshot.getValue().toString();
                    if(Integer.parseInt(totalDoodles) < 0) totalDoodles = "0";
                    profile_totaldoodles.setText(totalDoodles);
                    Log.d(TAG, "Totaldoodles : " +totalDoodles);
                }catch (Exception e){
                    Log.e(TAG,"Totaldoodles : null");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d("DrawingActivity", "totalDB error"+ databaseError);
            }
        };

        fm = getFragmentManager();
        fm.beginTransaction().replace(R.id.content_frame, new MapsActivity()).commit();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }




    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        FragmentManager fm = getFragmentManager();
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        android.app.FragmentManager fragmentManager = getFragmentManager();

        if (id == R.id.nav_camera) {

            /**
             * create an instance of the IntentIntegrator class we imported,
             * and then call on the initiateScan() method to start scanning
             */
                IntentIntegrator scanIntegrator = new IntentIntegrator(this);
                scanIntegrator.setCaptureActivity(QRAcvitivy.class);
                scanIntegrator.setOrientationLocked(true);
                scanIntegrator.initiateScan();

        } else if (id == R.id.nav_mydoodle) {

            if(myDoodleCount.isEmpty() || myDoodleCount.equals("0")){
                Toast.makeText(getApplicationContext(), "게시물이 없습니다.", Toast.LENGTH_SHORT).show();
            }else {
                startActivity(new Intent(getApplicationContext(), ImageGridActivity.class));
            }

        }  else if (id == R.id.account_settings) {

            GoogleSignInActivity.stayLogin = false;
            Intent intent = new Intent(getApplicationContext(),GoogleSignInActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);

        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        //로그인 안되어있으면 앱 종료
        if(mAuth.getCurrentUser() == null){
            finish();
        }
        mMyDatabase.addListenerForSingleValueEvent(mMyValueEventListener);
        mTotalDoodles.addListenerForSingleValueEvent(mTotalValueEventListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mMyValueEventListener != null) mMyDatabase.removeEventListener(mMyValueEventListener);
        if(mTotalValueEventListener != null) mTotalDoodles.removeEventListener(mTotalValueEventListener);
    }

    /**
     * The results of the scan will be returned and we'll be able to retrieve it in the onActivityResult() method
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        /**
         * Parse the result into an instance of the IntentResult class we imported
         */
        IntentResult scanningIntentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        /**
         * Check the scanningIntentResult object is it null or not, to proceed only if we have a valid result
         */
        if (scanningIntentResult != null) {
            /**
             * Retrieve the content and the format of the scan as strings value.
             */
            String scanContent = scanningIntentResult.getContents();
            String scanFormat = scanningIntentResult.getFormatName();
            /**
             * if condition after getting scanContent and scanFormat,
             * checking on them if there are consisting real data or not.
             */
            if(scanContent != null && scanFormat != null) {
                /**
                 * Now our program has the format and content of the scanned data,
                 * so you can do whatever you want with it.
                 */
                if(TranslationUtil.possibilitiyQRPlace(scanContent)) {
                    placeName = TranslationUtil.transPlaceNameSightIDtoEN(scanContent);
                    Toast.makeText(getApplicationContext(), "이곳은 " +
                            TranslationUtil.transPlaceNameENtoKOR(placeName)+"입니다!", Toast.LENGTH_LONG).show();

                    Intent intent = new Intent(getApplicationContext(), DrawingActivity.class);
                    startActivity(intent);
                    Log.d("QRcode", "Main-placeName : " + placeName);
                }else Toast.makeText(getApplicationContext(), "DB에 없는 지명입니다", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "QR코드 데이터를 받지 못했습니다.", Toast.LENGTH_SHORT).show();
            }
        } else {
            /**
             * If scan data is not received
             * (for example, if the user cancels the scan by pressing the back button),
             * we can simply output a message.
             */
            Toast.makeText(getApplicationContext(), "QR코드 데이터를 받지 못했습니다", Toast.LENGTH_SHORT).show();
        }
    }

}
