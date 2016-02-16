package nu.paheco.patrik.nokendo;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.DigitalClock;
import android.widget.EditText;
import android.widget.TextView;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {
    String callsUrl = "http://192.168.1.3/calls.php";

    // JSON Node names (for Emoncms)
    private static final String TAG_ID = "id";
    private static final String TAG_NAME = "name";
    private static final String TAG_TIME = "time";
    private static final String TAG_VALUE = "value";

    TextView txtCalls;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        // Get all settings
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        Map<String, ?> prefsMap = settings.getAll();
        for (Map.Entry<String, ?> entry: prefsMap.entrySet()) {
            Log.d("SharedPreferences", entry.getKey() + ":" + entry.getValue().toString());
        }

        EditText apikey=(EditText)findViewById(R.id.apikey);
        final TextView header=(TextView)findViewById(R.id.header);
        final TextView temp=(TextView)findViewById(R.id.temp);
        final TextView humidity=(TextView)findViewById(R.id.humidity);
        final TextView pressure=(TextView)findViewById(R.id.pressure);
        final TextView updated=(TextView)findViewById(R.id.updated);
        final TextView washerinfo=(TextView)findViewById(R.id.washerinfo);


        Button btnSave = (Button) findViewById(R.id.save);
        Button btnfindapikey = (Button) findViewById(R.id.emoncms);

        String stored_apikey = getPreferences(MODE_PRIVATE).getString("apikey", "");
        apikey.setText(stored_apikey);
        if (stored_apikey != null && !stored_apikey.isEmpty()) {
            apikey.setVisibility(View.GONE);
            btnSave.setVisibility(View.GONE);
            btnfindapikey.setVisibility(View.GONE);
        }

        header.setText("Nokendo");

        // Date & time
        Calendar c = Calendar.getInstance();
        System.out.println("Current time => "+c.getTime());
        final Date curTime = c.getTime();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String formattedDate = df.format(c.getTime());
        Log.d("Time:", formattedDate);
        header.setText(formattedDate);

        String emonurl = "http://emoncms.org/feed/list.json?apikey=" + stored_apikey;
        Ion.with(getApplicationContext())
                .load(emonurl)
                .asString()
                .setCallback(new FutureCallback<String>() {
                    @Override
                    public void onCompleted(Exception e, String result) {
                        if (result != null && !result.isEmpty()) {
                            result = result.trim();
                            Log.d("result-", result + "-");
                            try {
                                JSONArray jsonArr = new JSONArray(result);

                                int i;
                                String datatime;
                                String washerdata="";
                                List<Map<String, ?>> list = new ArrayList<Map<String, ?>>();

                                // looping through All items
                                for (i = 0; i < jsonArr.length(); i++) {
                                    // Get values
                                    JSONObject c = jsonArr.getJSONObject(i);
                                    String id = c.getString(TAG_ID);
                                    String name = c.getString(TAG_NAME);
                                    String value = c.getString(TAG_VALUE);
                                    String time = c.getString(TAG_TIME);

                                    //Log.d("Id: ", id);
                                    // Is there a settings for this id?
                                    /*
                                    if (settings.contains(id)) {
                                        // Yes
                                        Log.d("Id setting found: ", id);
                                        //String checkedId = settings.getString(id, "");
                                        checkedValue = settings.getString(id, "");
                                        Log.d("Value ", checkedValue);
                                    }*/

                                    // Convert timestamp
                                    long tlong = Long.parseLong(time);
                                    String realtime = getDate(tlong);
                                    //Log.i("Timelong: ",realtime);
                                    datatime = realtime;

                                    // How old are the values
                                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

                                    try {
                                        Date mDate = sdf.parse(realtime);
                                        long timeInMilliseconds = mDate.getTime();
                                        long timenow = System.currentTimeMillis();
                                        long timediff = timenow - timeInMilliseconds;

                                        long days = TimeUnit.MILLISECONDS.toDays(timediff);
                                        timediff -= TimeUnit.DAYS.toMillis(days);
                                        long hours = TimeUnit.MILLISECONDS.toHours(timediff);
                                        timediff -= TimeUnit.HOURS.toMillis(hours);
                                        long minutes = TimeUnit.MILLISECONDS.toMinutes(timediff);
                                        timediff -= TimeUnit.MINUTES.toMillis(minutes);
                                        long seconds = TimeUnit.MILLISECONDS.toSeconds(timediff);

                                        //Log.i("Timelong: ", realtime);
                                        //Log.i("Age: ", days + ":" + hours + ":" + minutes);

                                        //HashMap<String, String, String> item = new HashMap<String, String, String>();
                                        Map<String, Object> stuff = new HashMap<String, Object>();

                                        // Add to list
                                        stuff.put(TAG_NAME, name + ": " + value);
                                        stuff.put("rtime", "Retrieved @ " + realtime + "(" + days + " days, " + hours + " hours, " + minutes + " minutes ago)");
                                        stuff.put(TAG_ID, id);

                                        Log.d("Data", name + ": " + value);

                                        if(name.equals("Temp")) {
                                            Long tsLong = System.currentTimeMillis()/1000;
                                            Long diff = tsLong-Long.valueOf(time);
                                            Log.d ("Age", String.valueOf(diff));
                                            // If value is old, display it in red
                                            if (diff>600){  // 600s=5min
                                                temp.setTextColor(Color.RED);
                                            }

                                            temp.setText(value + "C");
                                            updated.setText("Uppdaterad "+realtime);

                                        }
                                        if(name.equals("Humidity")) {
                                            humidity.setText(value + "%");
                                        }
                                        if(name.equals("Pressure")) {
                                            pressure.setText(value+"hPa");
                                        }
                                        if(name.equals("Emon5110-T")) {
                                            washerdata = value + "C ";
                                            Long tsLong = System.currentTimeMillis()/1000;
                                            Long diff = tsLong-Long.valueOf(time);
                                            Log.d ("Washer data age", String.valueOf(diff));
                                            // If value is old, display it in red
                                            if (diff>600){  // 600s=5min
                                                washerinfo.setTextColor(Color.RED);
                                            }
                                        }
                                        if(name.equals("Emon5110-H")) {
                                            washerdata = washerdata+value +"% ";
                                        }
                                        if(name.equals("Emon5110Power")) {
                                            washerdata = washerdata+value+"W";
                                        }

                                        Log.d("Washerdata", washerdata);
                                        washerinfo.setText(washerdata);


                                            // Fill List with data
                                        list.add(stuff);

                                    } catch (ParseException f) {
                                        f.printStackTrace();
                                    }
                                    //Log.d("Result: ", result);
                                    //txtCalls.setText(result);
                                }

                            } catch (JSONException g) {
                                g.printStackTrace();
                            }
                        }
                    }
                });


        //setSupportActionBar(toolbar);

        // Get calls data
        txtCalls=(TextView)findViewById(R.id.calls);
        Ion.with(getApplicationContext())
                .load(callsUrl)
                .asString()
                .setCallback(new FutureCallback<String>() {
                    @Override
                    public void onCompleted(Exception e, String result) {
                        if (result != null && !result.isEmpty()) {
                            result = result.trim();
                            //Log.d("result-", result + "-");
                            if (result.equals("<br />")) {
                                // No calls
                                result = " No calls";
                            }
                            // Remove Html tags
                            result = Html.fromHtml(result).toString();
                            //Log.d("Result: ", result);

                            txtCalls.setText(result);
                        }
                    }
                });

    }

    public void apikeyClicked(View view){
        TextView apikey = (TextView) findViewById(R.id.apikey );
        apikey.setText("");
    }

    public void saveapikeyClicked(View view){
        // Find gui elements
        TextView header = (TextView) findViewById(R.id.header);
        EditText apikey=(EditText)findViewById(R.id.apikey);
        Button btnSave = (Button) findViewById(R.id.save);
        Button btnEmoncms = (Button) findViewById(R.id.emoncms);

        // Save value
        getPreferences(MODE_PRIVATE).edit().putString("apikey", apikey.getText().toString()).commit();

        // Hide gui elements
        apikey.setVisibility(View.GONE);

        btnSave.setVisibility(View.GONE);
        btnEmoncms.setVisibility(View.GONE);

        header.setText("Api key saved");
    }
    public void emoncmsClicked(View view){
        String emonurl = "http://emoncms.org/user/view";
        //Log.i("Open url", emonurl);
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(emonurl));
        startActivity(i);
    }
                    private String getDate(long time) {
                        String stime = Long.toString(time);
                        //Log.i("stime: ", stime);
                        Date date = new Date(time*1000L); // *1000 is to convert seconds to milliseconds
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // the format of your date
                        String formattedDate = sdf.format(date);
                        //System.out.println(formattedDate);
                        return formattedDate.toString();
                    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
