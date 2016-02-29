package au.edu.unimelb.comp90018.civiworx;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.JsonReader;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import au.edu.unimelb.comp90018.civiworx.db.BoundingBox;
import au.edu.unimelb.comp90018.civiworx.db.Message;
import au.edu.unimelb.comp90018.civiworx.db.QueryHandler;
import au.edu.unimelb.comp90018.civiworx.db.Report;
import au.edu.unimelb.comp90018.civiworx.http.CWxHttpClient;
import au.edu.unimelb.comp90018.civiworx.http.CWxRequest;
import au.edu.unimelb.comp90018.civiworx.singleton.UserProfileSingleton;
import au.edu.unimelb.comp90018.civiworx.ui.ReportDetailFragment;

/**
 * Hover text on the message action bar icon derived from
 * http://stackoverflow.com/questions/13288989/how-to-get-text-on-an-actionbar-icon
 */

public class MapViewActivity extends FragmentActivity
        implements UserProfileSingleton.AuthenticationCallbackInterface,
        ReportDetailFragment.ReportDetailListener, LocationListener {

    static final float LOCATION_ACCURACY_MARGIN = 200.0f; // meters
    static final float LOCATION_UPDATE_MIN_DISTANCE = 5000.0f; // meters
    static final long LOCATION_UPDATE_INTERVAL = 1000; // milliseconds

    // Result from taking/choosing photo
    public static final int SELECT_PHOTO = 0x3225;

    private Dialog mActiveDialog;

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.

    private LocationManager mLocator;
    private Location mLastLocation;
    private boolean mLocationListening = false;

    // so can disable when moving for gps fix
    private boolean mNoteMapMoves = false;
    private boolean mMapMovedSinceFix = false;

    // id of current "new" event marker
    String mNewReportMarker;
    Map<String, Marker> mMapMarkers;
    Map<String, String> mMapIds;

    private static String LOG_TAG = "CWX";

    // Number of unread messages overlaid on action bar
    private int msgCount = 0;
    private TextView mMsgCountText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_view);
        startLocationServices();
        setUpMapIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startLocationServices();
        setUpMapIfNeeded();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationServices();
    }

    @Override
    protected void onNewIntent(Intent i) {
        //Log.i(LOG_TAG, "Called onNewIntent");
        super.onNewIntent(i);
        setIntent(i);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        // Get the message count layout and manually set the
        // onclick handler - because in some circumstances the
        // onOptionsItemSelected handler is not invoked
        RelativeLayout msgCountLayout = (RelativeLayout) menu.findItem(R.id.open_messages)
                .getActionView();
        ImageButton msgCountImage = (ImageButton) msgCountLayout.findViewById(R.id.actionbar_messages_image);
        msgCountImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startMessageActivity();
            }
        });
        // and then set the number of unread messages overlaying the button
        mMsgCountText = (TextView) msgCountLayout.findViewById(R.id.actionbar_messages_textview);
        QueryHandler qH = new QueryHandler(getApplicationContext());
        qH.open();
        msgCount = qH.countUnreadMessages();
        qH.close();
        mMsgCountText.setText(Integer.toString(msgCount));

        // and the menu has been created
        return true;
    }

    private void dropNewReportMarker(LatLng point) {
        // If a report has been added then move that marker to the center
        // if nothing is waiting to be edited then create a new marker
        Marker nMark;
        if (mNewReportMarker == null) {
            // Drop a marker onto the map which can be moved
            String mTitle = getResources().getString(R.string.map_new_marker_title),
                    mSnippet = getResources().getString(R.string.map_new_marker_instructions);
            nMark = mMap.addMarker(new MarkerOptions()
                    .position(point)
                    .title(mTitle)
                    .snippet(mSnippet)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                    .draggable(true));
            nMark.showInfoWindow();
            mNewReportMarker = nMark.getId();
            mMapMarkers.put(mNewReportMarker, nMark);
            mMapIds.put(mNewReportMarker, null);
        } else {
            nMark = mMapMarkers.get(mNewReportMarker);
            nMark.setPosition(point);
            nMark.showInfoWindow();
        }
    }

    private void startReportActivity() {
        Log.i(LOG_TAG, "Starting a new civic works report");
        LatLng mCenter = mMap.getCameraPosition().target;
        dropNewReportMarker(mCenter);
    }

    private void startMessageActivity() {
        Log.i(LOG_TAG, "Starting the messages inbox activity");
        Intent inboxIntent = new Intent(MapViewActivity.this,
                InboxActivity.class);
        startActivity(inboxIntent);
    }

    private void startUserProfileActivity() {
        Log.i(LOG_TAG, "Starting the user profile activity");
        Intent editProfileActivityIntent = new Intent(MapViewActivity.this,
                ProfileActivity.class);
        startActivity(editProfileActivityIntent);
    }

    private void endCurrentUserActivity() {
        Log.i(LOG_TAG, "Current user will be logged out");
        UserProfileSingleton.getInstance(getBaseContext()).logout(this);
    }

    private void startApplicationSettings() {
        Log.i(LOG_TAG, "Starting the settings activity");
        Intent settingsActivityIntent = new Intent(MapViewActivity.this,
                SettingsActivity.class);
        startActivity(settingsActivityIntent);
    }

    private void triggerManualSync() {
        Log.i(LOG_TAG, "Starting manual sync of database");
        Toast.makeText(getApplicationContext(), "Sync started", Toast.LENGTH_SHORT).show();
        new AsyncTask<Void, Void, Void>() {

            private String apiEndPoint;
            private QueryHandler qH;

            private long responseCWXID(Response resp) {
                long res = 0;
                ResponseBody rbody = resp.body();
                JsonReader jsonReader = null;
                try {
                    jsonReader = new JsonReader(new InputStreamReader(rbody.byteStream(), "UTF-8"));
                    String attrName;
                    jsonReader.beginObject();
                    while (jsonReader.hasNext()) {
                        attrName = jsonReader.nextName();
                        if (attrName.equals("id")) {
                            res = jsonReader.nextLong();
                        } else {
                            jsonReader.skipValue();
                        }
                    }
                    jsonReader.endObject();
                } catch (UnsupportedEncodingException uee) {
                    res = -1;
                } catch (IOException ioe) {
                    res = -1;
                } finally {
                    try {
                        jsonReader.close();
                        rbody.close();
                    } catch (IOException ioex) {}
                }
                return res;
            }

            @Override
            protected Void doInBackground(Void... voids) {
                // First sync up the reports; updating the local id's as necessary
                qH.open();
                final OkHttpClient client = CWxHttpClient.Instance(getApplicationContext());
                final String reportPostURI = apiEndPoint.concat("reports/");
                List<Report> syncReports = qH.reportsToSync();
                for (Report r : syncReports) {
                    try {
                        RequestBody data = new FormEncodingBuilder()
                                .add("title", r.title)
                                .add("latitude", Double.toString(r.lat))
                                .add("longitude", Double.toString(r.lng))
                                .build();
                        Request req = new CWxRequest(getApplicationContext())
                                .url(reportPostURI)
                                .post(data)
                                .build();
                        long cwxid = responseCWXID(client.newCall(req).execute());
                        qH.setCiviWorxReport(r.id, cwxid);
                    } catch (IOException ioe) {
                        // oops
                    }
                }

                // then sync up the messages; again updating local id's as necessary
                List<Message> syncMessages = qH.messagesToSync();
                for (Message m : syncMessages) {
                    final String messagePostURI = apiEndPoint.concat("report/" + m.report + "/messages/");
                    try {
                        RequestBody data = new FormEncodingBuilder()
                                .add("message_text", m.messageText)
                                .add("img_data", m.b64Image)
                                .add("reply_to", Long.toString(m.replyTo))
                                .build();
                        Request req = new CWxRequest(getApplicationContext())
                                .url(messagePostURI)
                                .post(data)
                                .build();
                        long cwxid = responseCWXID(client.newCall(req).execute());
                        qH.setCiviWorxMessage(m.id, cwxid);
                    } catch (IOException ioe) {
                        // oops
                    }
                }

                // TODO: now pull down everything that we don't have

                // nothing to return on completion
                qH.close();
                return null;
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                qH = new QueryHandler(getApplicationContext());
                apiEndPoint = getApplicationContext().getString(R.string.api_endpoint);
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                Toast.makeText(getApplicationContext(), "Sync finished", Toast.LENGTH_SHORT).show();
                redrawMarkers();
            }

            @Override
            protected void onCancelled(Void aVoid) {
                super.onCancelled(aVoid);
            }

        }.execute();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.make_new_report:
                startReportActivity();
                return true;
            case R.id.open_messages:
            case R.id.actionbar_messages_image:
                startMessageActivity();
                return true;
            case R.id.open_user_profile:
                startUserProfileActivity();
                return true;
            case R.id.logout_user_account:
                endCurrentUserActivity();
                return true;
            case R.id.open_application_settings:
                startApplicationSettings();
                return true;
            case R.id.manually_sync:
                triggerManualSync();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Starts the location listener and picks the most accurate last known position
     * for the starting location. The map will move until accuracy is within
     * ACCEPTABLE_LOCATION_ERROR meters.
     */
    private void startLocationServices() {
        if (!mLocationListening) {
            mLocationListening = true;
            mLocator = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            Location gpsOld = null, networkOld = null;

            try {
                mLocator.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_UPDATE_INTERVAL,
                        LOCATION_UPDATE_MIN_DISTANCE, this);
                gpsOld = mLocator.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            } catch (IllegalArgumentException gpsIArgEx) {
                // gps provider is not available on device
                Log.w(LOG_TAG, "No GPS available on device");
            } catch (SecurityException gpsSecEx) {
                // not allowed
                Log.e(LOG_TAG, "GPS Access Denied...?");
            }

            try {
                mLocator.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, LOCATION_UPDATE_INTERVAL,
                        LOCATION_UPDATE_MIN_DISTANCE, this);
                networkOld = mLocator.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            } catch (IllegalArgumentException netIArgEx) {
                // network provider not available on device
                Log.w(LOG_TAG, "No network location available on device");
            } catch (SecurityException netSecEx) {
                // not allowed
                Log.e(LOG_TAG, "Network location access denied...?");
            }

            if (gpsOld != null && networkOld != null) {
                if (gpsOld.getElapsedRealtimeNanos() < networkOld.getElapsedRealtimeNanos()) {
                    mLastLocation = networkOld;
                } else {
                    mLastLocation = gpsOld;
                }
            } else if (gpsOld != null) {
                mLastLocation = gpsOld;
            } else if (networkOld != null) {
                mLastLocation = networkOld;
            } else {
                Log.i(LOG_TAG, "No previous known location - need to wait for fix");
                Toast.makeText(getApplicationContext(), "Searching for current location",
                        Toast.LENGTH_LONG).show();
                mLastLocation = null; // no previous known location
            }
        }
    }

    /**
     * Stops the location listeners when application disappears.
     */
    private void stopLocationServices() {
        if (mLocationListening) {
            mLocationListening = false;
            mLocator.removeUpdates(this);
        }
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        // move to the current location
        LatLng latLng;
        if (mLastLocation != null) {
            latLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
        } else {
            // Center to Melbourne by default: 37.8136° S, 144.9631° E
            latLng = new LatLng(-37.8136, 144.9631);
        }
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 10);
        mMap.moveCamera(cameraUpdate);

        // make sure map moves are noticed
        mNoteMapMoves = true;

        // and setup a hash map for markers in the map
        mMapMarkers = new HashMap<String, Marker>();
        mMapIds = new HashMap<String, String>();

        // when the map is moved
        mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                // the LBS will center the map unless it is moved by the user
                if (mNoteMapMoves) {
                    mMapMovedSinceFix = true;
                }

                // get the new extents and trigger a marker load for newly visible areas
                Log.i(LOG_TAG, "Moved");
                redrawMarkers();
            }
        });

        // when long press on map - create a new report in that place
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                dropNewReportMarker(latLng);
            }
        });

        // marker drag events
        mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {
                // hide the info bubble associated with this marker
                marker.hideInfoWindow();
            }

            @Override
            public void onMarkerDrag(Marker marker) {
                // Not implemented - doesn't matter
            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                // show the info bubble about this marker
                marker.showInfoWindow();
            }
        });

        // marker info window click for further details / edit if new report
        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                // is this the new report marker - if so open a report dialog
                if (marker.getId().equals(mNewReportMarker)) {
                    //Toast.makeText(getApplicationContext(), "NEW", Toast.LENGTH_LONG).show();
                    showReportDialog();
                } else {
                    //Toast.makeText(getApplicationContext(), "OLD", Toast.LENGTH_LONG).show();
                    String id = mMapIds.get(marker.getId());
                    Intent infoIntent = new Intent(MapViewActivity.this, ViewReportActivity.class);
                    infoIntent.putExtra("REPORT_ID", id);
                    startActivity(infoIntent);
                }
            }
        });

    }

    private void redrawMarkers() {
        QueryHandler qH = new QueryHandler(getApplicationContext());
        qH.open();
        List<Report> mbrReports = qH.reportsByBoundingBox(mapBoundingBox());
        for (Report r : mbrReports) {
            // Is the report already in the hash - if so don't worry
            if (!mMapMarkers.containsKey(Long.toString(r.id))) {
                Marker m = mMap.addMarker(new MarkerOptions()
                                .position(new LatLng(r.lat, r.lng))
                                .title(r.title)
                                .draggable(false)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
                );
                mMapMarkers.put(Long.toString(r.id), m);
                mMapIds.put(m.getId(), Long.toString(r.id));
            }
        }
        qH.close();
    }

    @Override
    public void authResult(int code) {
        // Not implemented in this
        // mMap.getProjection().getVisibleRegion().latLngBounds.northeast.
    }

    @Override
    public void logoutDone() {
        // Called once the logout process is complete - returns to startup
        Intent startUp = new Intent(MapViewActivity.this, StartupActivity.class);
        startActivity(startUp);
        finish();
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i(LOG_TAG, "New location received: " + location.toString());
        // Check the accuracy and stop if within accuracy margin
        if (location.getAccuracy() < LOCATION_ACCURACY_MARGIN) {
            Log.i(LOG_TAG, "Stopping location updates");
            mLocator.removeUpdates(this);
        }

        // Record the last known location
        mLastLocation = location;
        if (!mMapMovedSinceFix) {
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 10);
            mNoteMapMoves = false; // don't trigger map move listener
            mMap.animateCamera(cameraUpdate);
            mNoteMapMoves = true; // can hear them again now
        }
    }


    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
        Log.i(LOG_TAG, "onStatusChanged(" + s + ", " + i + ")");
    }

    @Override
    public void onProviderEnabled(String s) {
        Log.i(LOG_TAG, "onProviderEnabled(" + s + ")");
    }

    @Override
    public void onProviderDisabled(String s) {
        Log.i(LOG_TAG, "onProviderDisabled(" + s + ")");
    }


    /**
     * Converts the current map view to a database bounding box
     */
    private BoundingBox mapBoundingBox() {
        LatLngBounds mbr = mMap.getProjection().getVisibleRegion().latLngBounds;
        return new BoundingBox(mbr.northeast.latitude, mbr.northeast.longitude,
                mbr.southwest.latitude, mbr.southwest.longitude);
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        // Get the form fields and save
        mActiveDialog = dialog.getDialog();
        Intent takePhoto = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(takePhoto, SELECT_PHOTO);
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        // Cancel the dialog and remove the marker from the map
        dialog.getDialog().cancel();
        if (mNewReportMarker != null) {
            Marker nMarker = mMapMarkers.remove(mNewReportMarker);
            mMapIds.remove(nMarker.getId());
            nMarker.remove();
            mNewReportMarker = null;
        }
    }

    private void showReportDialog() {
        DialogFragment dialog = new ReportDetailFragment();
        dialog.show(getFragmentManager(), "ReportDetailFragment");
    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (SELECT_PHOTO == requestCode && Activity.RESULT_OK == resultCode) {
            Log.i(LOG_TAG, "Received an image from the intent");
            Bitmap imgData = (Bitmap) data.getExtras().get("data");
            if (mNewReportMarker != null) {
                EditText dTitle = (EditText) mActiveDialog.findViewById(R.id.report_title),
                        dDetail = (EditText) mActiveDialog.findViewById(R.id.report_details);
                Marker nMarker = mMapMarkers.remove(mNewReportMarker);
                String oldId = mMapIds.remove(nMarker.getId());

                // Convert these into reports and messages
                Calendar cal = Calendar.getInstance();
                String rTitle = dTitle.getText().toString(),
                        mDetail = dDetail.getText().toString();
                Date rDate = cal.getTime();
                Double lat = nMarker.getPosition().latitude,
                        lng = nMarker.getPosition().longitude;

                // and extract the image -> Base64 encoding
                String imageB64 = "";
                try {
                    if (imgData != null) {
                        ByteArrayOutputStream imgBytes = new ByteArrayOutputStream();
                        imgData.compress(Bitmap.CompressFormat.PNG, 75, imgBytes);
                        imageB64 = Base64.encodeToString(imgBytes.toByteArray(), Base64.DEFAULT);
                    }
                } catch (NullPointerException npe) {
                    // do nothing - just means photo wasn't taken
                }

                // Finally extract the current user name/id
                String userId = UserProfileSingleton.getInstance(getBaseContext()).getProfileId(),
                        userText = UserProfileSingleton.getInstance(getBaseContext()).getProfileName();

                QueryHandler qH = new QueryHandler(getApplicationContext());
                qH.open();

                // Save the report then save the message
                Report report = qH.saveReport(new Report(null, userId, userText, rDate, rTitle, lat, lng));

                // Now save the message content
                Message message = qH.saveMessage(new Message(null, report.id, userId, userText, rDate, null, mDetail, imageB64));

                qH.close();
                Log.i(LOG_TAG, "Created new report " + report.id + " and message " + message.id);

                // The new marker is no longer "new" so transform
                nMarker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
                nMarker.setDraggable(false);
                nMarker.setTitle(rTitle);
                nMarker.setSnippet(null);

                // and re-inject into the markers hash using the report id
                mMapMarkers.put(Long.toString(report.id), nMarker);
                mMapIds.put(nMarker.getId(), Long.toString(report.id));
                mNewReportMarker = null;
            }

            // and finally close the dialog
            mActiveDialog.cancel();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }


}
