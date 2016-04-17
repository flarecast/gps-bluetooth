package com.example.paulo.bluegps;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    BluetoothAdapter blue_adp;
    private static final int REQUEST_ENABLE_BT = 1;
    ArrayAdapter<String> mArrayAdapter;
    List<BluetoothDevice> deviceList;
    Button on_button, off_button, search_button, get_coordinates;
    ListView lv;
    LocationTracker gps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        deviceList = new ArrayList<>();
        setBluetooth();
        //setGPS();
    }

    public void setBluetooth(){
        blue_adp = BluetoothAdapter.getDefaultAdapter();
        if (blue_adp == null) {
            Toast.makeText(getApplicationContext(), "Your device does not support Bluetooth", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getApplicationContext(), "Your device supports Bluetooth", Toast.LENGTH_LONG).show();
            on();
        }

        setBluetoothButtons();
    }

    public void on(){
        if (!blue_adp.isEnabled()) {
            Intent turnOnIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOnIntent, REQUEST_ENABLE_BT);
        }
        else {
            Toast.makeText(getApplicationContext(),"Bluetooth is already on",
                    Toast.LENGTH_LONG).show();
        }
    }

    public void sendMsgToDevice(BluetoothDevice dv) throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        //new BlueConnection(dv, blue_adp).start();
        /*OutputStream out = dv.createInsecureRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")).getOutputStream();
        out.write("Test\n".getBytes("UTF-8"));
        out.flush();*/

        if(blue_adp.isDiscovering()) blue_adp.cancelDiscovery();

        int bt_port_to_connect = 21;
        BluetoothSocket deviceSocket = null;

        Method m = dv.getClass().getMethod("createInsecureRfcommSocket", new Class[] {int.class});

        deviceSocket = (BluetoothSocket) m.invoke(dv,bt_port_to_connect);
        deviceSocket.connect();
        //deviceSocket.connect();
        OutputStream out = deviceSocket.getOutputStream();
        out.write("Test\n".getBytes("UTF-8"));
        out.flush();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_ENABLE_BT){
            if(blue_adp.isEnabled()) {
                Toast.makeText(getApplicationContext(),"Bluetooth turned on" ,
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    public void list(){
        // get paired devices
        mArrayAdapter.clear();
        deviceList.clear();
        Set<BluetoothDevice> pairedDevices = blue_adp.getBondedDevices();
        // put it's one to the adapter
        for(BluetoothDevice device : pairedDevices){
                mArrayAdapter.add(device.getName()+ "\n" + device.getAddress());
                deviceList.add(device);
        }

        find();
        mArrayAdapter.notifyDataSetChanged();

    }

    final BroadcastReceiver bReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // add the name and the MAC address of the object to the arrayAdapter
                if(!deviceList.contains(device)){
                    mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                    deviceList.add(device);
                    mArrayAdapter.notifyDataSetChanged();
                }
            }
        }
    };

    public void find() {
        if (blue_adp.isDiscovering()) {
            blue_adp.cancelDiscovery();
        }
        else {
            blue_adp.startDiscovery();
            registerReceiver(bReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        }
    }

    public void off(){
        blue_adp.disable();
        Toast.makeText(getApplicationContext(),"Bluetooth turned off" , Toast.LENGTH_LONG).show();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(bReceiver);
    }

    public void setGPS(){
        //gps = new GPSTracker(MainActivity.this);
        gps= new LocationTracker(this);
        get_coordinates=(Button)findViewById(R.id.get_coordinates_button);

        get_coordinates.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (gps.canGetLocation()) {
                    //String nmea = gps.getNmea();
                    Location loc = gps.getLocation();

                    if (loc == null) {
                        Toast.makeText(getApplicationContext(), "Location null", Toast.LENGTH_LONG).show();

                    } else {
                        Toast.makeText(getApplicationContext(), "New coordinates: " + loc.getLongitude() + " " + loc.getLatitude(), Toast.LENGTH_LONG).show();
                    }

                    TextView lat = (TextView) findViewById(R.id.lat);
                    lat.setText("Lat: " + new String(String.valueOf(gps.getLatitude())));

                    TextView lon = (TextView) findViewById(R.id.lon);
                    lon.setText("Lon: " + new String(String.valueOf(gps.getLongitude())));


                    //TextView alt = (TextView) findViewById(R.id.alt);
                    //alt.setText("Alt: " + new String(String.valueOf(gps.location.getAltitude())));

                    //TextView bear = (TextView) findViewById(R.id.bear);
                    //bear.setText("Bear: " + new String(String.valueOf(gps.location.getBearing())));

                    //TextView speed = (TextView) findViewById(R.id.speed);
                    //speed.setText("Speed: " + new String(String.valueOf(gps.location.getSpeed())));

                    //Toast.makeText(getApplicationContext(), "New coordinates: "+nmea, Toast.LENGTH_LONG).show();
                } else {
                    //gps.showSettingsAlert();
                    Toast.makeText(getApplicationContext(), "Can't get location", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void setBluetoothButtons() {
        on_button = (Button) findViewById(R.id.on_button);
        on_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                on();
            }
        });
        off_button = (Button) findViewById(R.id.off_button);
        off_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                off();
            }
        });

        search_button = (Button) findViewById(R.id.search_button);
        search_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                list();
            }
        });

        lv = (ListView) findViewById(R.id.listView);
        lv.setAdapter(mArrayAdapter);

        lv.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BluetoothDevice dv = deviceList.get(position);
                Toast.makeText(getApplicationContext(), "You selected : " + dv.getName(), Toast.LENGTH_SHORT).show();
                try {
                    sendMsgToDevice(dv);
                } catch (/*IOException*/Exception e1) {
                    Toast.makeText(getApplicationContext(), "Error sending message", Toast.LENGTH_LONG).show();
                }/* catch (NoSuchMethodException e2) {
                    Toast.makeText(getApplicationContext(), "Error sending message", Toast.LENGTH_LONG).show();
                } catch (InvocationTargetException e3) {
                    Toast.makeText(getApplicationContext(), "Error sending message", Toast.LENGTH_LONG).show();
                } catch (IllegalAccessException e4) {
                    Toast.makeText(getApplicationContext(), "Error sending message", Toast.LENGTH_LONG).show();
                }*/
            }
        });
    }
}
