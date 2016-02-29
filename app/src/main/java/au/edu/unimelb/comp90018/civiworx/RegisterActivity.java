package au.edu.unimelb.comp90018.civiworx;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import au.edu.unimelb.comp90018.civiworx.fragment.LoginFragment;
import au.edu.unimelb.comp90018.civiworx.fragment.ProfileFragment;
import au.edu.unimelb.comp90018.civiworx.singleton.UserProfileSingleton;


public class RegisterActivity extends Activity
        implements LoginFragment.LoginFragmentInteractionListener,
        ProfileFragment.ProfileFragmentInteractionListener,
        UserProfileSingleton.AuthenticationCallbackInterface {

    // Tag for log entries
    static final String LOG_TAG = "CWX";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Trigger on click handlers for the form
        Button btnRegister = (Button) findViewById(R.id.register_activity_do);
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                processRegistration();
            }
        });

        Button btnCancel = (Button) findViewById(R.id.register_activity_cancel);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cancelRegistration();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.accept_cancel, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.menu_accept:
                processRegistration();
                return true;
            case R.id.menu_cancel:
                cancelRegistration();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void cancelRegistration() {
        // Cancel the registration form
        setResult(Activity.RESULT_CANCELED);
        finish(); // and end activity
    }

    private void processRegistration() {
        // TODO: Process the registration details
        Log.i(LOG_TAG, "Processing registration details");

        // Find the profile fragments to extract fields from
        View loginFragment = findViewById(R.id.register_login_fragment);
        View profileFragment = findViewById(R.id.register_profile_fragment);

        // Extract the actual form fields
        EditText userEmail = (EditText) loginFragment.findViewById(R.id.login_fragment_user),
                password = (EditText) loginFragment.findViewById(R.id.login_fragment_password),
                passwordConfirm = (EditText) findViewById(R.id.register_password_confirm),
                pName = (EditText) profileFragment.findViewById(R.id.profile_fragment_name),
                pLocation = (EditText) profileFragment.findViewById(R.id.profile_fragment_location),
                pBio = (EditText) profileFragment.findViewById(R.id.profile_fragment_bio);
        ImageButton pImage = (ImageButton) profileFragment.findViewById(R.id.profile_fragment_image);

        // And now get the values for verification
        String regEmail = userEmail.getText().toString(),
                regPassword = password.getText().toString(),
                regConfirm = passwordConfirm.getText().toString(),
                regName = pName.getText().toString(),
                regLocation = pLocation.getText().toString(),
                regBio = pBio.getText().toString();
        Bitmap regImage = ((BitmapDrawable)pImage.getDrawable()).getBitmap();

        // TODO: Better email validation
        if (regEmail.length() == 0 || !validateEmail(regEmail)) {
            Toast.makeText(getApplicationContext(),
                    "Must enter a valid email address", Toast.LENGTH_LONG).show();
            return;
        }

        // Validate the password confirmation
        if (regPassword.compareTo(regConfirm) != 0) {
            Toast.makeText(getApplicationContext(),
                    "Passwords do not match", Toast.LENGTH_LONG).show();
            return;
        }

        // Base64 encode the bitmap if any content
        String regImageB64 = "";
        if (regImage != null) {
            ByteArrayOutputStream imgBytes = new ByteArrayOutputStream();
            regImage.compress(Bitmap.CompressFormat.PNG, 75, imgBytes);
            regImageB64 = Base64.encodeToString(imgBytes.toByteArray(), Base64.DEFAULT);
        }

        // Get a reference to the profile singleton and try to register
        UserProfileSingleton.getInstance(getBaseContext()).register(regEmail, regPassword,
                regName, regLocation, regBio, regImageB64, this);
    }


    public boolean validateEmail(String email) {
        String pattern="^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+$";
        Pattern r = Pattern.compile(pattern);
        Matcher m=r.matcher(email);
        if(m.find())
        {
            return true;
        }
        else
        {
            return false;
        }
        // TODO: Validate the email address and that hasn't already been used
    }

    @Override
    public void onLogin(String username, String password) {
        // Ignore - we're not processing logins
    }

    @Override
    public void onRegister() {
        processRegistration();
    }

    @Override
    public void authResult(int code) {
        // Check the result and if OK then exit else toast the error
        switch (code) {
            case UserProfileSingleton.AuthenticationCallbackInterface.CODE_OK:
                setResult(Activity.RESULT_OK);
                finish();
                return;
            default:
                Toast.makeText(getApplicationContext(),
                        "Could not create account", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void logoutDone() {
        // Not implemented - can't logout while registering
    }

}
