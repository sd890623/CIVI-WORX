package au.edu.unimelb.comp90018.civiworx;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.UUID;

import au.edu.unimelb.comp90018.civiworx.singleton.UserProfileSingleton;


public class StartupActivity extends Activity
    implements View.OnClickListener,
        UserProfileSingleton.AuthenticationCallbackInterface{

    // Log key
    private static String LOG_TAG = "CWX";

    // Intent request codes
    static final int
            LOGIN_REQUEST = 0xff3240,
            REGISTER_REQUEST = 0xff3241;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Ensure default preference values are set
        PreferenceManager.setDefaultValues(this, R.xml.preferences, true);

        // Decide if we need to login first
        if ( UserProfileSingleton.getInstance(getBaseContext()).isActiveProfile()) {
            // There is an active profile so move straight to the
            // map view activity - the user doesn't need to login
            startMapViewActivity();
        } else {
            // We don't have an active profile so prompt the user
            setContentView(R.layout.activity_startup);

            // Need to trigger a few button click actions
            Button btnClick;
            int[] btnClickIds = new int[] {R.id.btn_register,
                    R.id.btn_anonymous, R.id.btn_login};
            for (int i=0; i<btnClickIds.length; i++) {
                btnClick = (Button) findViewById(btnClickIds[i]);
                btnClick.setOnClickListener(this);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // If the request was cancelled then do nothing, only process OKs
        if (resultCode == RESULT_OK) {
            // Check what the result request code was for
            switch (requestCode) {
                case LOGIN_REQUEST:
                    // TODO: Do we need to re-check the login here?
                    startMapViewActivity();
                    break;
                case REGISTER_REQUEST:
                    // TODO: Do we need to login the user here?
                    startMapViewActivity();
                    break;
                default:
                    startMapViewActivity();

            }
        }
    }

    private void startMapViewActivity() {
        // Fire an intent to start the map view activity
        Intent mapIntent = new Intent(StartupActivity.this,
                MapViewActivity.class);
        startActivity(mapIntent);
        finish(); // and dispose of the startup
    }

    private void startLoginActivity() {
        Log.i(LOG_TAG, "Login button clicked");
        // Trigger the LoginActivity for a result
        Intent loginIntent = new Intent(StartupActivity.this,
                LoginActivity.class);
        startActivityForResult(loginIntent, LOGIN_REQUEST);
    }

    private void startRegisterActivity() {
        Log.i(LOG_TAG, "Register button clicked");
        // Trigger the RegisterActivity for a result
        Intent registerIntent = new Intent(StartupActivity.this,
                RegisterActivity.class);
        startActivityForResult(registerIntent, REGISTER_REQUEST);
    }
    private void startAnynomous()
    {
        final TelephonyManager tm = (TelephonyManager) getBaseContext().getSystemService(Context.TELEPHONY_SERVICE);
        final String tmDevice, tmSerial, androidId;
        tmDevice = "" + tm.getDeviceId();
        tmSerial = "" + tm.getSimSerialNumber();
        androidId = "" + android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);

        UUID deviceUuid = new UUID(androidId.hashCode(), ((long)tmDevice.hashCode() << 32) | tmSerial.hashCode());
        String deviceId = "##ANONYMOUS"+deviceUuid.toString();
        Log.e("sadasd",deviceId);
        UserProfileSingleton.getInstance(getBaseContext()).register(deviceId, ""
             ,"","","","",this);



    }

    @Override
    public void onClick(View view) {
        // Decide which button was clicked
        int id = view.getId();
        switch (id) {
            case R.id.btn_login:
                startLoginActivity();
                break;
            case R.id.btn_anonymous:
                Log.i(LOG_TAG, "Anonymous use button clicked");
                startAnynomous();
                break;
            case R.id.btn_register:
                startRegisterActivity();
                break;
        }
    }

    @Override
    public void authResult(int code) {
        switch (code) {
            case UserProfileSingleton.AuthenticationCallbackInterface.CODE_OK:
                //setResult(Activity.RESULT_OK);
                //finish();
                //it should run startmapview...where does this finally call startmapview
                startMapViewActivity();
                return;
            default:
                Toast.makeText(getApplicationContext(),
                        "register failed", Toast.LENGTH_LONG).show();
        }    }

    @Override
    public void logoutDone() {

    }
}
