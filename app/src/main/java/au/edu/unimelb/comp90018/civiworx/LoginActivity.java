package au.edu.unimelb.comp90018.civiworx;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import au.edu.unimelb.comp90018.civiworx.fragment.LoginFragment;
import au.edu.unimelb.comp90018.civiworx.singleton.UserProfileSingleton;


public class LoginActivity extends Activity
        implements LoginFragment.LoginFragmentInteractionListener,
        UserProfileSingleton.AuthenticationCallbackInterface {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Setup button click listeners
        Button btnLogin = (Button) findViewById(R.id.login_activity_do);
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                View lFrag = (View) findViewById(R.id.login_activity_fragment);
                EditText mUserEmail = (EditText) lFrag.findViewById(R.id.login_fragment_user);
                EditText mPasswordText = (EditText) lFrag.findViewById(R.id.login_fragment_password);
                String email = mUserEmail.getText().toString(),
                        password = mPasswordText.getText().toString();
                onLogin(email, password);
            }
        });

        Button btnCancel = (Button) findViewById(R.id.login_activity_cancel);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setResult(Activity.RESULT_CANCELED);
                finish();
            }
        });
    }

    @Override
    public boolean validateEmail(String email) {
        // This does nothing - don't need to validate email here
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
    }

    @Override
    public void onLogin(String username, String password) {
        // Use the profile singleton to authenticate
        if (username.length() == 0 || !validateEmail(username)) {
            Toast.makeText(getApplicationContext(),
                    "Must enter a valid email address", Toast.LENGTH_LONG).show();
            return;
        }
        UserProfileSingleton.getInstance(getBaseContext()).authenticate(username,
                password, this);
        // TODO: Trigger some form of spinner?
    }

    @Override
    public void authResult(int code) {
        // TODO: Turn off the spinner
        // Check the result and if OK then exit else toast the error
        switch (code) {
            case UserProfileSingleton.AuthenticationCallbackInterface.CODE_OK:
                setResult(Activity.RESULT_OK);
                finish();
                return;
            default:
                Toast.makeText(getApplicationContext(),
                        "Authentication failed", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void logoutDone() {
        // Not implemented - don't need to logout in login
    }

}
