package seoulapp.chok.rokseoul;



import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.os.Bundle;
import android.view.WindowManager;

import seoulapp.chok.rokseoul.firebase.GoogleSignInActivity;

public class IntroActivity extends BaseActivity {

    Handler handler;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);


        if(!isOnline()){
            final AlertDialog.Builder onlineDialog = new AlertDialog.Builder(this);
            onlineDialog.setTitle("Online Check");
            onlineDialog.setMessage("You are Offline T.T");
            onlineDialog.setPositiveButton("Close", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                    finish();
                }
            });
            onlineDialog.setNegativeButton("Retry", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if(isOnline()) {
                        startActivity(new Intent(getApplicationContext(), GoogleSignInActivity.class));
                        GoogleSignInActivity.stayLogin = true;
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                        finish();
                    }else{
                        onlineDialog.show();
                    }
                }
            });
            onlineDialog.show();
        }else {
            handler = new Handler();
            handler.postDelayed(mrun, 3000);
        }
    }
    Runnable mrun = new Runnable() {
        @Override
        public void run() {
            startActivity(new Intent(getApplicationContext(), GoogleSignInActivity.class));
            GoogleSignInActivity.stayLogin = true;
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }
    };
    @Override
    public void onBackPressed(){
        super.onBackPressed();
        if(!isOnline()) finish();
        else handler.removeCallbacks(mrun);
    }

    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }
}