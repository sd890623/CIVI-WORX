package au.edu.unimelb.comp90018.civiworx.singleton;

import android.content.Context;
import android.os.AsyncTask;
import android.util.JsonReader;
import android.util.Log;

import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import au.edu.unimelb.comp90018.civiworx.R;
import au.edu.unimelb.comp90018.civiworx.http.CWxHttpClient;
import au.edu.unimelb.comp90018.civiworx.http.CWxRequest;

public class UserProfileSingleton {

    // Where the user profile data is stored
    public static final String PROFILE_FILE = "auth_profile.dat";

    // Private static singleton instance
    private static UserProfileSingleton sInstance;

    /**
     * Retrieve an instance of the user profile singleton.
     * @param c The context of the caller.
     */
    public static UserProfileSingleton getInstance(Context c) {
        if (null == sInstance) {
            sInstance = new UserProfileSingleton(c.getApplicationContext());
        }

        return sInstance;
    }

    // Reference to the application context
    private Context appContext;

    // The profile file contents
    private UserProfile activeProfile;

    /**
     * Private constructor for the user profile - needs application context.
     *
     * @param ac The application context.
     */
    private UserProfileSingleton(Context ac) {
        appContext = ac;
        activeProfile = null;
    }

    private void processProfileFile() throws IOException {
        // Read the input file bytes
        FileInputStream fis = appContext.openFileInput(PROFILE_FILE);
        JsonReader jsonReader = new JsonReader(new InputStreamReader(fis, "UTF-8"));
        UserProfile tProfile = new UserProfile();
        try {
            String attrName;
            jsonReader.beginObject();
            while (jsonReader.hasNext()) {
                attrName = jsonReader.nextName();
                if (attrName.equals("session_key")) {
                    tProfile.sessionKey = jsonReader.nextString();
                } else if (attrName.equals("name")) {
                    tProfile.name = jsonReader.nextString();
                } else if (attrName.equals("location")) {
                    tProfile.location = jsonReader.nextString();
                } else if (attrName.equals("bio")) {
                    tProfile.bio = jsonReader.nextString();
                } else if (attrName.equals("img_data")) {
                    tProfile.b64Image = jsonReader.nextString();
                } else if (attrName.equals("id")) {
                    tProfile.id = jsonReader.nextString();
                } else {
                    jsonReader.skipValue();
                }
            }
            jsonReader.endObject();
            // and assign the active profile that we built
            activeProfile = tProfile;
            Log.i("PROF", activeProfile.toString());
        } finally {
            jsonReader.close();
        }
    }

    public String getProfileId() {
        if (activeProfile != null) {
            return activeProfile.id;
        } else {
            return null;
        }
    }

    public String getProfileName() {
        if (activeProfile != null) {
            return activeProfile.name;
        } else {
            return null;
        }
    }

    public String getProfileLocation() {
        if (activeProfile != null) {
            return activeProfile.location;
        } else {
            return null;
        }
    }

    public String getProfileBio() {
        if (activeProfile != null) {
            return activeProfile.bio;
        } else {
            return null;
        }
    }

    public String getProfileB64Image() {
        if (activeProfile != null) {
            return activeProfile.b64Image;
        } else {
            return null;
        }
    }

    public void updateActiveProfile(String n, String l, String b, String i) {
        if (activeProfile != null) {
            activeProfile.name = n;
            activeProfile.location = l;
            activeProfile.bio = b;
            activeProfile.b64Image = i;
            new AsyncTask<Void, Void, Void>() {

                private String apiEndPoint;

                @Override
                protected Void doInBackground(Void... voids) {
                    // The username and password are PUT to the auth api
                    final String uri = apiEndPoint.concat("auth/profile/me/");
                    Log.i("XDBG", uri);
                    final OkHttpClient client = CWxHttpClient.Instance(appContext);
                    try {
                        RequestBody data = new FormEncodingBuilder()
                                .add("real_name", activeProfile.name)
                                .add("location", activeProfile.location)
                                .add("bio", activeProfile.bio)
                                .add("img_data", activeProfile.b64Image)
                                .build();
                        Request req = new CWxRequest(appContext)
                                .url(uri)
                                .put(data)
                                .build();
                        client.newCall(req).execute();
                    } catch (Exception e1) {
                        // The IOException occurs for cancel, connectivity or timeout
                        Log.i("XDBG", e1.toString());
                    }
                    // return an empty response code
                    return null;
                }

                @Override
                protected void onPreExecute() {
                    super.onPreExecute();
                    apiEndPoint = appContext.getString(R.string.api_endpoint);
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    super.onPostExecute(aVoid);
                    // TODO: Probably should trigger some notice this has been done
                }
            }.execute();
        }
    }

    /**
     * Is a user profile active in this install?
     */
    public boolean isActiveProfile() {
        // If we've already processed the file then don't worry
        if (null != activeProfile) {
            return true;
        }

        // If the PROFILE_FILE exists and has the session_key then
        // assume the device is logged in - there are a few potential
        // issues with this - but ...
        try {
            processProfileFile();
            return activeProfile != null;
        } catch (FileNotFoundException fnfe) {
            // if the file is not there then definitely not active
            return false;
        } catch (IOException ioe) {
            // an io error can be considered failure for this purpose
            return false;
        }
    }

    /**
     * Return the current profile session key string
     */
    public String getSessionKey() {
        if (activeProfile != null) {
            return activeProfile.sessionKey;
        } else {
            return null;
        }
    }

    /**
     * Forces the current session to be removed from the device.
     */
    public void logout(final AuthenticationCallbackInterface cb) {
        // Setup an async task to send the HTTP
        new AsyncTask<Void, Void, Integer>() {

            private String apiEndPoint;

            @Override
            protected Integer doInBackground(Void... voids) {
                // The username and password are PUT to the auth api
                final String uri = apiEndPoint.concat("auth/session/");
                Log.i("XDBG", uri);
                final OkHttpClient client = CWxHttpClient.Instance(appContext);
                try {
                    Request req = new CWxRequest(appContext)
                            .url(uri)
                            .delete()
                            .build();
                    client.newCall(req).execute();
                    return AuthenticationCallbackInterface.CODE_OK;
                } catch (Exception e1) {
                    // The IOException occurs for cancel, connectivity or timeout
                    Log.i("XDBG", e1.toString());
                    return AuthenticationCallbackInterface.CODE_TIMEOUT;
                }
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                apiEndPoint = appContext.getString(R.string.api_endpoint);
            }

            @Override
            protected void onPostExecute(Integer integer) {
                super.onPostExecute(integer);
                appContext.deleteFile(PROFILE_FILE);
                activeProfile = null;
                cb.logoutDone();
            }

        }.execute();
    }

    private Integer processResponseBody(ResponseBody rb) throws IOException {
        // and it was so all good, we're logged in
        String json = null;
        try {
            json = rb.string();
            Log.i("XDBG", json);
        } catch (IOException ioe2) {
            // pass - leave as null to indicate this
        } finally {
            // Make sure the body gets closed
            rb.close();
        }

        // if we got some json then good, otherwise error out
        if (null == json) {
            Log.i("XDBG", "json null");
            return AuthenticationCallbackInterface.CODE_ERROR;
        } else {
            Log.i("XDBG", "writing to disk");
            // write the auth token to internal storage
            String fd = appContext.getFilesDir().getAbsolutePath();
            Log.i("XXXX", fd);
            FileOutputStream fos = appContext.openFileOutput(PROFILE_FILE, Context.MODE_PRIVATE);
            fos.write(json.getBytes());
            fos.close();
            processProfileFile(); // make sure profile is loaded
            return AuthenticationCallbackInterface.CODE_OK;
        }
    }

    /**
     * Attempts to authenticate the given username/password against
     * the server; call back interface is triggered on result.
     *
     * @param username The username to authenticate.
     * @param password The proposed password for username.
     * @param callback The callback on result.
     */
    public void authenticate(final String username, final String password,
                             final AuthenticationCallbackInterface callback) {
        // Setup an async task to send the HTTP
        new AsyncTask<Void, Void, Integer>() {

            private String apiEndPoint;

            private Integer processResponse(Response res) throws IOException {
                // The result should be the session was created
                if (CWxHttpClient.HTTP_OK == res.code()) {
                    return processResponseBody(res.body());
                } else {
                    // but it wasn't so now need to decide what happened
                    if (CWxHttpClient.HTTP_UNAUTHORISED == res.code()) {
                        // failed the username / password test
                        Log.i("XDBG", "unauthorised");
                        return AuthenticationCallbackInterface.CODE_INCORRECT;
                    } else {
                        // some form of error
                        Log.i("XDBG", "other error");
                        return AuthenticationCallbackInterface.CODE_ERROR;
                    }
                }
            }

            @Override
            protected Integer doInBackground(Void... voids) {
                // The username and password are PUT to the auth api
                final String uri = apiEndPoint.concat("auth/session/");
                Log.i("XDBG", uri);
                final OkHttpClient client = CWxHttpClient.Instance(appContext);
                try {
                    RequestBody data = new FormEncodingBuilder()
                            .add("username", username)
                            .add("password", password)
                            .build();
                    Request req = new CWxRequest(appContext)
                            .url(uri)
                            .post(data)
                            .build();
                    return processResponse(client.newCall(req).execute());
                } catch (IOException ioe1) {
                    // The IOException occurs for cancel, connectivity or timeout
                    Log.i("XDBG", ioe1.toString());
                    return AuthenticationCallbackInterface.CODE_TIMEOUT;
                }
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                apiEndPoint = "http://cwx.andrewbevitt.com:10808/api/";
            }

            @Override
            protected void onPostExecute(Integer integer) {
                super.onPostExecute(integer);
                callback.authResult(integer);
            }

            @Override
            protected void onCancelled(Integer integer) {
                super.onCancelled(integer);
                callback.authResult(integer);
            }

        }.execute();
    }

    /**
     * Attempts to register a new profile with the given details
     * @param username The email address for the new account.
     * @param password Password
     * @param personName Person's name
     * @param personLocation Where they live / report
     * @param personBio Short description
     * @param b64Image Base64 encoded image
     * @param callback Callback for when registration succeeds
     */
    public void register(final String username, final String password, final String personName,
                         final String personLocation, final String personBio, final String b64Image,
                         final AuthenticationCallbackInterface callback) {
        // Setup an async task to send the HTTP
        new AsyncTask<Void, Void, Integer>() {

            private String apiEndPoint;

            private Integer processResponse(Response res) throws IOException {
                // The result should be that a session was created
                if (CWxHttpClient.HTTP_OK == res.code()) {
                    return processResponseBody(res.body());
                } else {
                    // but it wasn't so now need to decide what happened
                    Log.i("XDBG", "invalid account creation call");
                    return AuthenticationCallbackInterface.CODE_ERROR;
                }
            }

            @Override
            protected Integer doInBackground(Void... voids) {
                // The username and password are PUT to the auth api
                final String uri = apiEndPoint.concat("auth/profile/");
                Log.i("XDBG", uri);
                final OkHttpClient client = CWxHttpClient.Instance(appContext);
                try {
                    RequestBody data = new FormEncodingBuilder()
                            .add("username", username)
                            .add("password", password)
                            .add("real_name", personName)
                            .add("location", personLocation)
                            .add("bio", personBio)
                            .add("b64_img", b64Image)
                            .build();
                    Request req = new CWxRequest(appContext)
                            .url(uri)
                            .post(data)
                            .build();
                    return processResponse(client.newCall(req).execute());
                } catch (IOException ioe1) {
                    // The IOException occurs for cancel, connectivity or timeout
                    Log.i("XDBG", ioe1.toString());
                    return AuthenticationCallbackInterface.CODE_TIMEOUT;
                }
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                apiEndPoint = appContext.getString(R.string.api_endpoint);
            }

            @Override
            protected void onPostExecute(Integer integer) {
                super.onPostExecute(integer);
                callback.authResult(integer);
            }

            @Override
            protected void onCancelled(Integer integer) {
                super.onCancelled(integer);
                callback.authResult(integer);
            }

        }.execute();
    }


    /**
     * Interface which any one calling authenticate should implement to
     * receive results of the authentication task.
     */
    public interface AuthenticationCallbackInterface {
        static final int
            CODE_OK = 0x01,
            CODE_INCORRECT = 0x02,
            CODE_TIMEOUT = 0x03,
            CODE_ERROR = 0x04;

        public void authResult(int code);
        public void logoutDone();
    }

    public static class UserProfile {
        public String id;
        public String name;
        public String location;
        public String bio;
        public String b64Image;
        public String sessionKey;

        public UserProfile() {
            this("", "", "", "", "", "");
        }

        public UserProfile(String id, String n, String l, String b, String i, String k) {
            this.id = id;
            this.name = n;
            this.location = l;
            this.bio = b;
            this.b64Image = i;
            this.sessionKey = k;
        }

        @Override
        public String toString() {
            return name + ":" + location + ":" + bio + ":" + sessionKey;
        }

    }

}
