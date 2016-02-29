package au.edu.unimelb.comp90018.civiworx.http;

import android.content.Context;

import com.squareup.okhttp.Request;

import au.edu.unimelb.comp90018.civiworx.R;
import au.edu.unimelb.comp90018.civiworx.singleton.UserProfileSingleton;

public class CWxRequest extends Request.Builder {

    public CWxRequest(Context context) {
        // new Request.Builder
        super();

        // Set the API access key header
        String token = context.getString(R.string.api_token_name),
                key = context.getString(R.string.api_token_key);
        this.header(token, key);

        // If this is an active profile then add session key
        if (UserProfileSingleton.getInstance(context).isActiveProfile()) {
            String sessionToken = context.getString(R.string.api_session_name),
                    sessionKey = UserProfileSingleton.getInstance(context).getSessionKey();
            this.header(sessionToken, sessionKey);
        }
    }

}
