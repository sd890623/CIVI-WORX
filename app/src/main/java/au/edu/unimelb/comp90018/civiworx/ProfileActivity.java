package au.edu.unimelb.comp90018.civiworx;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

import java.io.ByteArrayOutputStream;

import au.edu.unimelb.comp90018.civiworx.singleton.UserProfileSingleton;


public class ProfileActivity extends Activity {

    static final String LOG_TAG = "CWX";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        readCurrentProfile();
    }

    private void readCurrentProfile() {
        // Find the elements in the fragment to assign values
        View profileFragment = findViewById(R.id.profile_profile_fragment);
        EditText pName = (EditText) profileFragment.findViewById(R.id.profile_fragment_name),
                pLocation = (EditText) profileFragment.findViewById(R.id.profile_fragment_location),
                pBio = (EditText) profileFragment.findViewById(R.id.profile_fragment_bio);
        ImageButton pImage = (ImageButton) profileFragment.findViewById(R.id.profile_fragment_image);

        // Assign values based on the current active profile
        UserProfileSingleton ups = UserProfileSingleton.getInstance(getBaseContext());
        pName.setText(ups.getProfileName());
        pLocation.setText(ups.getProfileLocation());
        pBio.setText(ups.getProfileBio());

        // Decode the Base64 image
        try {
            byte[] decodedBytes = Base64.decode(ups.getProfileB64Image(), Base64.DEFAULT);
            Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
            pImage.setImageBitmap(decodedBitmap);
        } catch (Exception e) {
            // Oops...
            Log.i(LOG_TAG, "Could not decode bitmap");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.accept_cancel, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.menu_accept:
                commitProfileChanges();
                return true;
            case R.id.menu_cancel:
                // the changes will be lost when we kill the activity
                setResult(Activity.RESULT_CANCELED);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void commitProfileChanges() {
        Log.i(LOG_TAG, "Saving the changes to current profile");
        // Make changes to the local profile and trigger file reload
        UserProfileSingleton ups = UserProfileSingleton.getInstance(getBaseContext());
        View profileFragment = findViewById(R.id.profile_profile_fragment);
        EditText pName = (EditText) profileFragment.findViewById(R.id.profile_fragment_name),
                pLocation = (EditText) profileFragment.findViewById(R.id.profile_fragment_location),
                pBio = (EditText) profileFragment.findViewById(R.id.profile_fragment_bio);
        ImageButton pImage = (ImageButton) profileFragment.findViewById(R.id.profile_fragment_image);

        // Get the field values
        String name = pName.getText().toString(),
                location = pLocation.getText().toString(),
                bio = pBio.getText().toString();
        Bitmap image = ((BitmapDrawable)pImage.getDrawable()).getBitmap();

        // Base64 encode the bitmap if any content
        String imageB64 = "";
        if (image != null) {
            ByteArrayOutputStream imgBytes = new ByteArrayOutputStream();
            image.compress(Bitmap.CompressFormat.PNG, 75, imgBytes);
            imageB64 = Base64.encodeToString(imgBytes.toByteArray(), Base64.DEFAULT);
        }

        // Use the profile manager to set the values
        ups.updateActiveProfile(name, location, bio, imageB64);
        setResult(Activity.RESULT_OK);
        finish();
    }

}
