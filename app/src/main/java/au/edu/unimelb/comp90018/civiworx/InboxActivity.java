package au.edu.unimelb.comp90018.civiworx;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.List;

import au.edu.unimelb.comp90018.civiworx.db.InboxItem;
import au.edu.unimelb.comp90018.civiworx.db.Message;
import au.edu.unimelb.comp90018.civiworx.db.QueryHandler;


public class InboxActivity extends Activity {

    public static String LOG_TAG = "CWX";

    private List<InboxItem> mMessages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_inbox);

        // Open database connection
        QueryHandler qH = new QueryHandler(getApplicationContext());
        qH.open();

        mMessages = qH.getInbox();

        qH.close();

        // create a list adapter for the report messages
        CustomAdapter adapter = new CustomAdapter(this, mMessages);
        ListView listView = (ListView) findViewById(R.id.inbox_list);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                // Open the report viewer for the clicked report
                String id = Long.toString(mMessages.get(i).reportId);
                Intent infoIntent = new Intent(InboxActivity.this, ViewReportActivity.class);
                infoIntent.putExtra("REPORT_ID", id);
                startActivity(infoIntent);
            }
        });


    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public class CustomAdapter extends ArrayAdapter<InboxItem> {

        private final Activity context;
        private final List<InboxItem> messages;

        public CustomAdapter(Activity context, List<InboxItem> messages) {
            super(context, R.layout.list_message, messages);
            this.context = context;
            this.messages = messages;
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            LayoutInflater inflater = context.getLayoutInflater();
            View rowView = inflater.inflate(R.layout.list_inbox, null, true);
            TextView txtTitle = (TextView) rowView.findViewById(R.id.inbox_message_title);
            TextView txtMessage = (TextView) rowView.findViewById(R.id.inbox_message_text);
            ImageView imageView = (ImageView) rowView.findViewById(R.id.inbox_message_img);
            InboxItem i = messages.get(position);
            txtTitle.setText(i.title);
            txtMessage.setText(i.firstMessageText);
            try {
                byte[] decodedBytes = Base64.decode(i.firstMessageImageB64, Base64.DEFAULT);
                Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                imageView.setImageBitmap(decodedBitmap);
            } catch (Exception e) {
                // Oops...
                Log.i(LOG_TAG, "Could not decode bitmap");
            }
            return rowView;
        }

    }

}
