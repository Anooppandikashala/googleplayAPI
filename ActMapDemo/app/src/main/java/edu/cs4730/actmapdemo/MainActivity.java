package edu.cs4730.actmapdemo;

import android.app.PendingIntent;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, ResultCallback<Status> {

    String TAG = "MainActivity";
    ViewPager viewPager;
    myListFragment listfrag;
    myMapFragment mapfrag;

    int currentActivity = DetectedActivity.UNKNOWN;
    List<String> DataList = new ArrayList<String>();
    List<objData> objDataList = new ArrayList<objData>();

    //for the location.
    GoogleApiClient mGoogleApiClient;
    Boolean mRequestingLocationUpdates = false;
    LocationRequest mLocationRequest;
    /**
     * The desired time between activity detections. Larger values result in fewer activity
     * detections while improving battery life. A value of 0 results in activity detections at the
     * fastest possible rate. Getting frequent updates negatively impact battery life and a real
     * app may prefer to request less frequent updates.
     */
    public static final long DETECTION_INTERVAL_IN_MILLISECONDS = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //setup fragments
        listfrag = new myListFragment();
        mapfrag = new myMapFragment();

        FragmentManager fragmentManager = getSupportFragmentManager();

        viewPager = (ViewPager) findViewById(R.id.pager);
        myFragmentPagerAdapter adapter = new myFragmentPagerAdapter(fragmentManager);
        viewPager.setAdapter(adapter);
        //viewPager.setCurrentItem(1);
        //new Tablayout from the support design library
        TabLayout mTabLayout = (TabLayout) findViewById(R.id.tablayout1);
        mTabLayout.setupWithViewPager(viewPager);

        buildGoogleApiClient();
    }


    protected synchronized void buildGoogleApiClient() {
        Log.v(TAG, "Building GoogleApiClient");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .addApi(ActivityRecognition.API)
                .build();

    }

    /**
    * The service will call the handler to send back information.
    **/
    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            currentActivity = msg.arg1;
            Log.v(TAG, "handler, update activity");
            return true;
        }

        ;
    });

    //menu methods
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
        } else if (id == R.id.action_start) {
            //first reset everything (in case this is the 2+ time)
            mapfrag.clearmap();
            DataList.clear();
            objDataList.clear();
            //now do the stetup and start it.
            createLocationRequest();
            startLocationUpdates();
            setupActivityRec(true);
            mRequestingLocationUpdates = true;
            return true;
        } else if (id == R.id.action_stop) {
            //add end marker
            Location mlocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            mapfrag.finishMap(new objData(
                    mlocation.getLatitude(),
                    mlocation.getLongitude(),
                    mlocation.getTime(),
                    currentActivity
            ));
            stopLocationUpdates();
            setupActivityRec(false);
            mRequestingLocationUpdates = false;
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        setupActivityRec(false);
        mGoogleApiClient.disconnect();

        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop location updates to save battery, but don't disconnect the GoogleApiClient object.
        if (mGoogleApiClient.isConnected()) {
            stopLocationUpdates();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mGoogleApiClient.isConnected() && mRequestingLocationUpdates) {
            setupActivityRec(true);
            startLocationUpdates();
        }

    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    //two methods to start and stop location updates.
    protected void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }


    //method to start or stop the activity recognition.
    void setupActivityRec(boolean gettingupdates) {
        if (!mGoogleApiClient.isConnected()) {
            Log.v(TAG, "GoogleAPIclient is not connected, ActRec issues.");
            return;
        }
        if (gettingupdates) { //true to start it.
            ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(
                    mGoogleApiClient,
                    DETECTION_INTERVAL_IN_MILLISECONDS,
                    getActivityDetectionPendingIntent()
            ).setResultCallback(this);
            Log.v(TAG, "starting ActLocation");
        } else {
            ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(
                    mGoogleApiClient,
                    getActivityDetectionPendingIntent()
            ).setResultCallback(this);
        }
    }


    /**
     * Gets a PendingIntent to be sent for each activity detection.
     */
    private PendingIntent getActivityDetectionPendingIntent() {
        Intent intent = new Intent(this, DetectedActivitiesIntentService.class);
        Messenger messenger = new Messenger(handler);
        intent.putExtra("MESSENGER", messenger);

        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // requestActivityUpdates() and removeActivityUpdates().
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }


    //GoogleApiCloient call back methods
    @Override
    public void onConnected(Bundle bundle) {

        Log.v(TAG, "onConnected");
        Location mlocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        mapfrag.setupInitialloc(mlocation.getLatitude(), mlocation.getLongitude());
        //initial spot maybe?
        if (mRequestingLocationUpdates)
            addData(mlocation);

    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.v(TAG, "onConnectionSuspected");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.v(TAG, "onConnectionFailed");
        mRequestingLocationUpdates = false;
    }

    //resultcallback for the Activity Recognition... not sure if need this or not, but
    //shows when the system is running correctly.
    @Override
    public void onResult(Status status) {
        Log.v(TAG, "onResult");
        if (status.isSuccess()) {
            Log.v(TAG, "onResult success");
        } else {
            Log.v(TAG, "onResult failed. " + status.getStatusMessage());
        }
    }

    //location listener methods
    @Override
    public void onLocationChanged(Location location) {
        addData(location);
    }

    public void addData(Location mlocation) {
        objData newData;


        if (mlocation != null) {

            newData = new objData(
                    mlocation.getLatitude(),
                    mlocation.getLongitude(),
                    mlocation.getTime(),
                    currentActivity
            );

            //figure distance info.
            if (objDataList.isEmpty()) {
                newData.distance =0.0f;
            } else {
                newData.distance = distanceBetween(objDataList.get(objDataList.size() -1).myLatlng, newData.myLatlng) * 0.3048f; //converted to feet
                newData.distance += objDataList.get(objDataList.size() -1).distance;  //previous distance, to ge the total.
            }
            //add everything and add to the data structures.
            objDataList.add(newData);
            listfrag.updateAdatper(objDataList);
            mapfrag.updateMapDraw(newData);
        }
    }
    private float distanceBetween(LatLng latLng1, LatLng latLng2) {

        Location loc1 = new Location(LocationManager.GPS_PROVIDER);
        Location loc2 = new Location(LocationManager.GPS_PROVIDER);

        loc1.setLatitude(latLng1.latitude);
        loc1.setLongitude(latLng1.longitude);

        loc2.setLatitude(latLng2.latitude);
        loc2.setLongitude(latLng2.longitude);


        return loc1.distanceTo(loc2);
    }

    //view page for the two fragments map and list.
    public class myFragmentPagerAdapter extends FragmentPagerAdapter {
        int PAGE_COUNT = 2;

        //required constructor that simply supers.
        public myFragmentPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        // return the correct fragment based on where in pager we are.
        @Override
        public Fragment getItem(int position) {

            switch (position) {
                case 0:
                    return mapfrag;
                case 1:
                    return listfrag;
                default:
                    return null;
            }
        }

        //how many total pages in the viewpager there are.  3 in this case.
        @Override
        public int getCount() {

            return PAGE_COUNT;
        }

        //getPageTitle required for the PageStripe to work and have a value.
        @Override
        public CharSequence getPageTitle(int position) {

            switch (position) {
                case 0:
                    return "Map";
                case 1:
                    return "List";
                default:
                    return null;
            }
            //return String.valueOf(position);  //returns string of position for title


        }

    }

}
