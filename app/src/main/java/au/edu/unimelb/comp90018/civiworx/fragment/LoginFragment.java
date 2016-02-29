package au.edu.unimelb.comp90018.civiworx.fragment;


import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import au.edu.unimelb.comp90018.civiworx.R;

public class LoginFragment extends Fragment
        implements EditText.OnEditorActionListener {

    // Tag for log messages
    public static String LOG_TAG = "CWX";

    // Reference to the username and password fields
    EditText mUserEmail, mPasswordText;

    // Which activity is listening for login done messages
    LoginFragmentInteractionListener mListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rView = inflater.inflate(R.layout.fragment_login, container, false);

        // Get a reference to the user email field for capturing "next" events
        mUserEmail = (EditText) rView.findViewById(R.id.login_fragment_user);
        mUserEmail.setOnEditorActionListener(this);

        // Get a reference to the password field for capturing "done" events
        mPasswordText = (EditText) rView.findViewById(R.id.login_fragment_password);
        mPasswordText.setOnEditorActionListener(this);

        // Return the complete fragment view
        return rView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mListener = (LoginFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement LoginFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        if (null != mPasswordText) {
            mPasswordText.setOnEditorActionListener(null);
        }
    }

    private void doLogin() {
        Log.i(LOG_TAG, "done on login form fragment password");
        // Notify the listener that login should be attempted
        String email = mUserEmail.getText().toString(),
                password = mPasswordText.getText().toString();
        mListener.onLogin(email, password);
    }

    private void validateEmail() {
        Log.i(LOG_TAG, "next on login form fragment email");
        // Notify the listener that email can be validated
        String email = mUserEmail.getText().toString();
        if (!mListener.validateEmail(email)) {
            // TODO: Email was not valid
        }
    }

    @Override
    public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
        // If was "done" action on the password field then consume
        if (textView.getId() == R.id.login_fragment_password &&
                i == EditorInfo.IME_ACTION_DONE) {
            doLogin();
            return true;
        }

        // Similarly if "next" on username trigger the validate then consume
        if (textView.getId() == R.id.login_fragment_user &&
                i == EditorInfo.IME_ACTION_NEXT) {
            validateEmail();
            return false; // don't consume the next event
        }

        // otherwise didn't consume the event so bubble
        return false;
    }

    public interface LoginFragmentInteractionListener {
        public boolean validateEmail(String email);
        public void onLogin(String username, String password);
    }

}
