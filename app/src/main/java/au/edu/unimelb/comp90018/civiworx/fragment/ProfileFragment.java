package au.edu.unimelb.comp90018.civiworx.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import java.io.InputStream;

import au.edu.unimelb.comp90018.civiworx.R;

public class ProfileFragment extends Fragment
        implements EditText.OnEditorActionListener {

    // Result from taking/choosing photo
    static final int SELECT_PHOTO = 0x3224;

    // Need to keep a reference to the inflated view
    View inflatedView;

    // Where the profile photo image data is stored
    Bitmap mProfileImage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Start with an empty image
        mProfileImage = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        inflatedView = inflater.inflate(R.layout.fragment_profile, container, false);

        // Find the image button and setup click listeners
        ImageButton profileImageButton = (ImageButton) inflatedView.findViewById(R.id.profile_fragment_image);
        if (null != profileImageButton) {
            profileImageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent takePhoto = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(takePhoto, SELECT_PHOTO);
                }
            });
        }

        // return the completed view
        return inflatedView;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (SELECT_PHOTO == requestCode && Activity.RESULT_OK == resultCode) {
            Log.i("XFOO", "Received an image from the intent");
            mProfileImage = (Bitmap) data.getExtras().get("data");
            ((ImageButton) inflatedView.findViewById(R.id.profile_fragment_image))
                    .setImageBitmap(mProfileImage);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        /*
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
        */
    }

    @Override
    public void onDetach() {
        super.onDetach();
        //mListener = null;
    }

    @Override
    public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
        return false;
    }

    public interface ProfileFragmentInteractionListener {
        public void onRegister();
    }

}
