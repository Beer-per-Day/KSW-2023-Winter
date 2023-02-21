package com.dji.GSDemo.GoogleMap;

import static android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.virtualstick.FlightControlData;
import dji.common.flightcontroller.virtualstick.FlightCoordinateSystem;
import dji.common.flightcontroller.virtualstick.RollPitchControlMode;
import dji.common.flightcontroller.virtualstick.VerticalControlMode;
import dji.common.flightcontroller.virtualstick.YawControlMode;
import dji.common.useraccount.UserAccountState;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.common.error.DJIError;
import dji.sdk.products.Aircraft;
import dji.sdk.useraccount.UserAccountManager;

public class Waypoint1Activity extends FragmentActivity implements View.OnClickListener, GoogleMap.OnMapClickListener, OnMapReadyCallback {

    protected static final String TAG = "GSDemoActivity";

    private GoogleMap gMap;

    private Button locate, add, clear;
    private Button config, upload, start, stop;

    private boolean isAdd = false;

    private static double droneLocationLat = 181;
    private static double droneLocationLng = 181;
    private static float altitude = 100.0f;
    private final Map<Integer, Marker> mMarkers = new ConcurrentHashMap<Integer, Marker>();
    private Marker droneMarker = null;
    private FlightController mFlightController;

    private Timer mSendVirtualStickDataTimer;
    private SendVirtualStickDataTask mSendVirtualStickDataTask;

    @Override
    protected void onResume(){
        super.onResume();
        initFlightController();
    }

    @Override
    protected void onPause(){
        super.onPause();
    }

    @Override
    protected void onDestroy(){
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    /**
     * @Description : RETURN Button RESPONSE FUNCTION
     */
    public void onReturn(View view){
        Log.d(TAG, "onReturn");
        this.finish();
    }

    private void setResultToToast(final String string){
        Waypoint1Activity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(Waypoint1Activity.this, string, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initUI() {

        locate = (Button) findViewById(R.id.locate);
        add = (Button) findViewById(R.id.add);
        clear = (Button) findViewById(R.id.clear);
        config = (Button) findViewById(R.id.config);
        upload = (Button) findViewById(R.id.upload);
        start = (Button) findViewById(R.id.start);
        stop = (Button) findViewById(R.id.stop);

        locate.setOnClickListener(this);
        add.setOnClickListener(this);
        clear.setOnClickListener(this);
        config.setOnClickListener(this);
        upload.setOnClickListener(this);
        start.setOnClickListener(this);
        stop.setOnClickListener(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // When the compile and target version is higher than 22, please request the
        // following permissions at runtime to ensure the
        // SDK work well.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.VIBRATE,
                            Manifest.permission.INTERNET, Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.WAKE_LOCK, Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.SYSTEM_ALERT_WINDOW,
                            Manifest.permission.READ_PHONE_STATE,
                    }
                    , 1);
        }

        setContentView(R.layout.activity_waypoint1);

        IntentFilter filter = new IntentFilter();
        filter.addAction(DJIDemoApplication.FLAG_CONNECTION_CHANGE);
        registerReceiver(mReceiver, filter);

        initUI();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            onProductConnectionChange();
        }
    };

    private void onProductConnectionChange()
    {
        initFlightController();
        loginAccount();
    }

    private void loginAccount(){

        UserAccountManager.getInstance().logIntoDJIUserAccount(this,
                new CommonCallbacks.CompletionCallbackWith<UserAccountState>() {
                    @Override
                    public void onSuccess(final UserAccountState userAccountState) {
                        Log.e(TAG, "Login Success");
                    }
                    @Override
                    public void onFailure(DJIError error) {
                        setResultToToast("Login Error:"
                                + error.getDescription());
                    }
                });
    }

    private void initFlightController() {

        BaseProduct product = DJIDemoApplication.getProductInstance();
        if (product != null && product.isConnected()) {
            if (product instanceof Aircraft) {
                mFlightController = ((Aircraft) product).getFlightController();

                // this control mode gives which method to use with roll, pitch and yaw
                mFlightController.setRollPitchControlMode(RollPitchControlMode.VELOCITY);
                mFlightController.setYawControlMode(YawControlMode.ANGULAR_VELOCITY);
                mFlightController.setVerticalControlMode(VerticalControlMode.VELOCITY);
                mFlightController.setRollPitchCoordinateSystem(FlightCoordinateSystem.BODY);

                // this method give the drone to move with virtual stick
                // it makes drone autopilot available
                // https://bit.ly/3lT3VCB => api reference for setVirtualStickModeEnabled
                mFlightController.setVirtualStickModeEnabled(true, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (djiError != null){
                            showToast(djiError.getDescription());
                        }else
                        {
                            showToast("Enable Virtual Stick Success");
                        }
                    }
                });
            }
        }

        if (mFlightController != null) {
            mFlightController.setStateCallback(new FlightControllerState.Callback() {

                @Override
                public void onUpdate(FlightControllerState djiFlightControllerCurrentState) {
                    droneLocationLat = djiFlightControllerCurrentState.getAircraftLocation().getLatitude();
                    droneLocationLng = djiFlightControllerCurrentState.getAircraftLocation().getLongitude();
                    altitude = djiFlightControllerCurrentState.getAircraftLocation().getAltitude();
                    updateDroneLocation();
                }
            });
        }

    }

    private void setUpMap() {
        gMap.setOnMapClickListener(this);// add the listener for click for amap object

    }

    float mPitch, mRoll, mYaw, mThrottle;
    float pitchJoyControlMaxSpeed = 14;
    float rollJoyControlMaxSpeed = 14;
    float verticalJoyControlMaxSpeed = 4;
    float yawJoyControlMaxSpeed = 30;

    LatLng targetCoor;
    float targetAlt;

    // when new location is fed in it trigger drone to move.
    public void newTargetLocate(LatLng target){
        LatLng next_locat = new LatLng(target.latitude, target.longitude);
        targetCoor = next_locat;
        targetAlt = testAltitude();

        if (null == mSendVirtualStickDataTimer) {
            mSendVirtualStickDataTask = new SendVirtualStickDataTask();
            mSendVirtualStickDataTimer = new Timer();
            mSendVirtualStickDataTimer.schedule(mSendVirtualStickDataTask, 0, 200);
        }
    }

    // Timer makes the task run at every "x" period.
    // It run similar as Thread
    class SendVirtualStickDataTask extends TimerTask {

        @Override
        public void run() {

            if (mFlightController != null) {

                needDirection(targetCoor);
                System.out.println("mPitch : " + mPitch + "\tmRoll : " + mRoll + "\tmThrottle : " + mThrottle);
                System.out.println("lat : " + droneLocationLat + "\tlng : " + droneLocationLng + "\tAlt : " + altitude);
                System.out.println("tar lat : " + targetCoor.latitude + "\ttar lng : " + targetCoor.longitude + "\ttar Alt : " + targetAlt);


                // the main method to move drone
                mFlightController.sendVirtualStickFlightControlData(
                        new FlightControlData(
                                mPitch, mRoll, mYaw, mThrottle
                        ), djiError -> {
                        }
                );

                // check drone is near to target
                // both of value is based on meter
                double distance = getDistanceMeter(targetCoor);
                double distanceAlt = targetAlt - altitude;
                System.out.println(distance);

                // when timer is stop (template)
                if (distance < 10 && distanceAlt < 10){
                    mSendVirtualStickDataTimer.cancel();
                    mSendVirtualStickDataTimer.purge();
                    mSendVirtualStickDataTimer = null;
                }
            }
        }
    }

    // modify
    public void needDirection(LatLng temp){
        // all of variable that yaw, throttle, pitch, roll has there range
        // need to check their API reference (pitch, roll was -15m/s ~ 15m/s and throttle was -4m/s ~ 4m/s)

        double idx = checkAngle(new LatLng(droneLocationLat, droneLocationLng), temp);

        double leftpX = 0, leftpY = (targetAlt - altitude) / getDistanceMeter(temp);
        double rightpX = Math.cos(idx), rightpY = Math.sin(idx);

        mYaw = (float)(yawJoyControlMaxSpeed * leftpX);
        mThrottle = (float)(verticalJoyControlMaxSpeed * leftpY);
        mPitch = (float)(pitchJoyControlMaxSpeed * rightpX);
        mRoll = (float)(rollJoyControlMaxSpeed * rightpY);

        if (mThrottle >= 4){
            mThrottle = (float) 3.99;
        }else if (mThrottle <= -4){
            mThrottle = (float) -3.99;
        }
    }

    public double checkAngle(LatLng from, LatLng to){
        return Math.atan2(to.latitude - from.latitude, to.longitude - from.longitude);
    }

    public double getDistanceMeter(LatLng temp) {
        double lat1 = droneLocationLat, lng1 = droneLocationLng;
        double lat2 = temp.latitude, lng2 = temp.longitude;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lng2 - lng1);

        double a =
                Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                        Math.cos(Math.toRadians(lat1)) *
                                Math.cos(Math.toRadians(lat2)) *
                                Math.sin(dLon / 2) *
                                Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        // 6371 * 1000 // Radius of the earth in meter
        return 6371 * 1000 * c;
    }

    // making a random altitude
    // this is for simulator dataset. it doesnt have a altitude control.
    public float testAltitude(){
        return (float) Math.random() * 500;
    }

    // modify
    @Override
    public void onMapClick(LatLng point) {
        markWaypoint(point);
        newTargetLocate(point);
    }

    public static boolean checkGpsCoordination(double latitude, double longitude) {
        return (latitude > -90 && latitude < 90 && longitude > -180 && longitude < 180) && (latitude != 0f && longitude != 0f);
    }

    private void updateDroneLocation(){
        LatLng pos = new LatLng(droneLocationLat, droneLocationLng);
        //Create MarkerOptions object
        final MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(pos);
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.aircraft));

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (droneMarker != null) {
                    droneMarker.remove();
                }

                if (checkGpsCoordination(droneLocationLat, droneLocationLng)) {
                    droneMarker = gMap.addMarker(markerOptions);
                }
            }
        });
    }

    private void markWaypoint(LatLng point){
        //Create MarkerOptions object
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(point);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        Marker marker = gMap.addMarker(markerOptions);
        mMarkers.put(mMarkers.size(), marker);
    }

    public void showToast(final String msg) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(Waypoint1Activity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cameraUpdate(){
        LatLng pos = new LatLng(droneLocationLat, droneLocationLng);
        float zoomlevel = (float) 15.0;
        CameraUpdate cu = CameraUpdateFactory.newLatLngZoom(pos, zoomlevel);
        gMap.moveCamera(cu);

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (gMap == null) {
            gMap = googleMap;
            setUpMap();
        }

        LatLng shenzhen = new LatLng(22.5362, 113.9454);
        gMap.addMarker(new MarkerOptions().position(shenzhen).title("Marker in Shenzhen"));
        gMap.moveCamera(CameraUpdateFactory.newLatLng(shenzhen));
    }

    Timer testDataTimer;
    TimerTask testTask;
    int testIdx = 0;

    // It is array for the making the dataset.
    // It needs instance of LatLng that consist the new trajectory.
    ArrayList<LatLng> testPos = new ArrayList<>(
            Arrays.asList(
                    new LatLng(40.5368527982882, -86.97808279390566)

            )
    );

    ArrayList<String[]> logs = new ArrayList<>();
    Timer logTimer;
    TimerTask logTask;

    // interval is period of leaving a log
    int interval = 500;

    // testInterval is period of drawing a marker
    int testInterval = 40000;
    int fileNum = 15;
    final int APP_STORAGE_ACCESS_REQUEST_CODE = 501;

    public void csvOperate() throws IOException {
        // after the task that running the trajectory.
        // the log is save in a /uavData folder.
        // It needs the permission to make and saving.

        try {
            // gets the permission from user.
            // it will keep ask user to check the permission.
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                Intent intent = new Intent(ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:" + BuildConfig.APPLICATION_ID));
                startActivity(intent);
            }

            String fileName = "/Test_uavTrajectoryData" + fileNum +".csv";
            String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/uavData";
            File filePath = new File(path);

            if(!filePath.exists()){
                filePath.mkdir();
            }

            File file = new File(filePath + fileName);
            if (file.createNewFile()){
                System.out.println("Success");
            }else{
                System.out.println("Fail");
            }

            BufferedWriter bw = new BufferedWriter(new FileWriter(file, true));
            String[] head = {"date", "time", "lat", "lon", "alt"};
            String[] divide = {",", ",", ",", ",", ""};

            System.out.println(filePath + fileName);

            for (int i = 0; i < divide.length; i++)
                bw.write(head[i] + divide[i]);
            bw.write("\n");

            for (String[] item : logs) {
                for (int i = 0; i < divide.length; i++) {
                    bw.write(item[i] + divide[i]);
                }
                bw.write("\n");
            }

            bw.flush();
            bw.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public String[] dateAndTime(){
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd@HH:mm:ss").format(new Date());
        String[] ret = timeStamp.split("@");

        return ret;
    }

    public void triggerLog(){
        if (logTimer == null){
            logTask = new logTask();
            logTimer = new Timer();
            logTimer.schedule(logTask, 200, interval);
        }
    }

    public void stopLog(){
        if (logTimer == null)
            return;

        logTimer.cancel();
        logTimer.purge();
        logTimer = null;
        try {
            csvOperate();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    class logTask extends TimerTask {

        @Override
        public void run() {
            String[] temp = new String[5];
            String[] dates = dateAndTime();
            temp[0] = dates[0];
            temp[1] = dates[1];
            temp[2] = Double.toString(droneLocationLat);
            temp[3] = Double.toString(droneLocationLng);
            temp[4] = Double.toString(altitude);
            logs.add(temp);
        }
    }
    // modify, only for test
    class testDataTask extends TimerTask{
        @Override
        public void run(){
            double testLat = testPos.get(testIdx).latitude;
            double testLng = testPos.get(testIdx).longitude;


            testClick(new LatLng(testLat, testLng));
            testIdx++;

            if (testIdx == testPos.size() - 1){
                if (testDataTimer != null){
                    testDataTimer.cancel();
                    testDataTimer.purge();
                    testDataTimer = null;
                    System.out.println("done");
                    stopLog();
                    showToast("Test ended!!!!");
                }
            }
        }
    }

    // modify, only for test
    public void testClick(LatLng point) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                markWaypoint(point);
            }
        });

        newTargetLocate(point);
    }

    // modify
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.add: {
                mSendVirtualStickDataTimer.cancel();
                mSendVirtualStickDataTimer.purge();
                mSendVirtualStickDataTimer = null;
                break;
            }

            case R.id.locate:{
                updateDroneLocation();
                cameraUpdate(); // Locate the drone's place
                break;
            }
            case R.id.clear:{
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        gMap.clear();
                    }

                });

                updateDroneLocation();
                break;
            }
            case R.id.config:{

                mFlightController.startTakeoff(
                        new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                if (djiError != null) {
                                    showToast(djiError.getDescription());
                                } else {
                                    showToast("Take off Success");
                                }
                            }
                        }
                );

                triggerLog();
                break;
            }
            case R.id.upload:{
                stopLog();

                mFlightController.startLanding(
                        djiError -> {
                            if (djiError != null) {
                                showToast(djiError.getDescription());
                            } else {
                                showToast("Start Landing");
                            }
                        }
                );

                mFlightController.turnOffMotors(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {

                    }
                });
                break;
            }
            case R.id.start:{
                if (testDataTimer == null){
                    testTask = new testDataTask();
                    testDataTimer = new Timer();
                    testDataTimer.schedule(testTask, 300, testInterval);
                }
                break;
            }
            case R.id.stop:{
                break;
            }
            default:
                break;
        }
    }
}
