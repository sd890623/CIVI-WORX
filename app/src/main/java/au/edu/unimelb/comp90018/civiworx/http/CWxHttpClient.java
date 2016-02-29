package au.edu.unimelb.comp90018.civiworx.http;

import android.content.Context;

import com.squareup.okhttp.OkHttpClient;

import java.util.concurrent.TimeUnit;

import au.edu.unimelb.comp90018.civiworx.R;

public class CWxHttpClient {

    // List of codes that may be useful - assume all others are errors
    public static final int
        HTTP_OK = 200,
        HTTP_CREATED = 201,
        HTTP_NO_CONTENT = 204,
        HTTP_MOVED = 301,
        HTTP_FOUND = 302,
        HTTP_NOT_MODIFIED = 304,
        HTTP_UNAUTHORISED = 401,
        HTTP_FORBIDDEN = 403,
        HTTP_NOT_FOUND = 404,
        HTTP_NOT_ALLOWED = 405;

    // The singleton instance of the client
    private static OkHttpClient mInstance;

    // Because 2.0.0 uses final on the class
    public static OkHttpClient Instance(Context context) {
        if (null == mInstance) {
            mInstance = new OkHttpClient();
            // Set default timeouts
            int cTime = context.getResources().getInteger(R.integer.http_connect_timeout),
                    wTime = context.getResources().getInteger(R.integer.http_write_timeout),
                    rTime = context.getResources().getInteger(R.integer.http_read_timeout);
            mInstance.setConnectTimeout(cTime, TimeUnit.SECONDS);
            mInstance.setWriteTimeout(wTime, TimeUnit.SECONDS);
            mInstance.setReadTimeout(rTime, TimeUnit.SECONDS);
        }

        // Return the client cloned so can be modified
        return mInstance.clone();
    }

}
