package au.edu.unimelb.comp90018.civiworx;

import android.app.Activity;
import android.app.ListActivity;
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
import android.widget.Toast;

import java.util.List;

import au.edu.unimelb.comp90018.civiworx.db.Message;
import au.edu.unimelb.comp90018.civiworx.db.QueryHandler;
import au.edu.unimelb.comp90018.civiworx.db.Report;


public class ViewReportActivity extends Activity {

    public static String LOG_TAG = "CWX";

    private Long mReportId;
    private Report mReport;
    private List<Message> mMessages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_view_report);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String value = extras.getString("REPORT_ID");
            mReportId = Long.parseLong(value);
        }

        // Open database connection
        QueryHandler qH = new QueryHandler(getApplicationContext());
        qH.open();

        if (mReportId != null) {
            // Load the report from the database
            mReport = qH.getReport(mReportId);
            mMessages = qH.getMessages(mReport);
        }

        qH.close();

        // Set the title of the message
        TextView titleText = (TextView) findViewById(R.id.view_report_title);
        titleText.setText(mReport.title);

        // create a list adapter for the report messages
        CustomAdapter adapter = new CustomAdapter(this, mMessages);
        ListView listView = (ListView) findViewById(R.id.view_report_list);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                doReply();
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.view_report, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.menu_cancel:
                finish();
                return true;
            case R.id.report_reply:
                doReply();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void doReply() {
        // TODO: Dialog for processing replies
        // NOTE: This should take an integer or other parameter
        // which identifies the message the user is replying to
        Toast.makeText(getApplicationContext(), "Replies not implemented", Toast.LENGTH_SHORT).show();
    }


    public class CustomAdapter extends ArrayAdapter<Message> {

        private final Activity context;
        private final List<Message> messages;

        public CustomAdapter(Activity context, List<Message> messages) {
            super(context, R.layout.list_message, messages);
            this.context = context;
            this.messages = messages;
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            LayoutInflater inflater = context.getLayoutInflater();
            View rowView = inflater.inflate(R.layout.list_message, null, true);
            TextView txtTitle = (TextView) rowView.findViewById(R.id.report_message_txt);
            ImageView imageView = (ImageView) rowView.findViewById(R.id.report_message_img);
            Message m = messages.get(position);
            txtTitle.setText(m.messageText);
            try {
                byte[] decodedBytes = Base64.decode(m.b64Image, Base64.DEFAULT);
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
