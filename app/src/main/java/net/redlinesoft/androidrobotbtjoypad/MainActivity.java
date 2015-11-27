package net.redlinesoft.androidrobotbtjoypad;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.support.design.widget.CoordinatorLayout;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;
import app.akexorcist.bluetotohspp.library.DeviceList;

public class MainActivity extends AppCompatActivity {

    private View mDecorView;
    ImageView ivUp, ivDown, ivLeft, ivRight, ivA, ivB, ivC, ivD;
    TextView txtResult;
    SharedPreferences prefs;
    BluetoothSPP bt;
    Context context;
    Boolean btConnect = false;
    int RESULT_SETTING = 0;
    int QR_CODE = 1;
    Menu menu;
    CoordinatorLayout coordinatorLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // set view
        mDecorView = getWindow().getDecorView();
        // hide system ui
        hideSystemUI();

        // set textview for result
        txtResult = (TextView) findViewById(R.id.txtResult);

        // set coordinate layout
        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinateLayout);

        // setup bluetooth
        bt = new BluetoothSPP(context);
        //checkBluetoothState();

        bt.setBluetoothConnectionListener(new BluetoothSPP.BluetoothConnectionListener() {
            public void onDeviceConnected(String name, String address) {
                // Do something when successfully connected
                //Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_SHORT).show();
                Snackbar.make(coordinatorLayout, "Connected", Snackbar.LENGTH_SHORT).show();
                btConnect = true;
                setMenuTitle("Disconnect");
            }

            public void onDeviceDisconnected() {
                // Do something when connection was disconnected
                //Toast.makeText(getApplicationContext(), "Disconnected", Toast.LENGTH_SHORT).show();
                Snackbar.make(coordinatorLayout, "Disconnected", Snackbar.LENGTH_SHORT).show();
                btConnect = false;
                btConnect = true;
                setMenuTitle("Connect");
            }

            public void onDeviceConnectionFailed() {
                // Do something when connection failed
                //Toast.makeText(getApplicationContext(), "Connection Failed", Toast.LENGTH_SHORT).show();
                Snackbar.make(coordinatorLayout, "Connection Failed", Snackbar.LENGTH_SHORT).show();
                btConnect = false;
                setMenuTitle("Connect");
            }
        });



    }

    private void checkBluetoothState() {

        if (this.btConnect == true) {
            bt.disconnect();
        }
        bt.setupService();
        bt.startService(BluetoothState.DEVICE_OTHER);

        Snackbar.make(coordinatorLayout, "Choose device to connect", Snackbar.LENGTH_LONG)
                .setAction("Setup", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // load device list
                        Intent intent = new Intent(getApplicationContext(), DeviceList.class);
                        startActivityForResult(intent, BluetoothState.REQUEST_CONNECT_DEVICE);
                    }
                }).show();
    }

    public void setTextResult(String button) {
        if (prefs.getBoolean("pref_debug_switch", false) == true) {
            txtResult.setText(button);
        } else {
            txtResult.setText("");
        }

        if (prefs.getBoolean("pref_vibrate_switch", false) == true) {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(50);
        }
    }

    public void setMenuTitle(String title) {
        // change setting menu
        MenuItem settingsItem = menu.findItem(R.id.mnuBluetooth);
        settingsItem.setTitle(title);
    }

    public void sendBluetoothData(final String data) {

        final int delay = Integer.parseInt(prefs.getString("pref_delay_list", "100"));

        final Handler handler = new Handler();

        final Runnable r = new Runnable() {
            public void run() {
                bt.send(data, true);
            }
        };
        handler.postDelayed(r, delay);
    }


    public void loadPreference() {
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Map<String, ?> keys = prefs.getAll();
        Log.d("LOG", "Keys = " + keys.size() + "");
        if (keys.size() >= 11) {
            for (Map.Entry<String, ?> entry : keys.entrySet()) {
                Log.d("LOG", entry.getKey() + ": " +
                        entry.getValue().toString());
            }
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Setup button first!");
            builder.setCancelable(false);
            builder.setPositiveButton("Setting", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // button setting
                    Intent i = new Intent(getApplicationContext(), SettingsActivity.class);
                    startActivityForResult(i, RESULT_SETTING);
                }
            });
            builder.setPositiveButton("QRCode", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // scan qrcode
                    qrcodeScanner();
                }
            });
            builder.show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BluetoothState.REQUEST_CONNECT_DEVICE) {
            if (resultCode == Activity.RESULT_OK)
                bt.connect(data);
                // setup josypad view
                setupJoyPadView();
        } else if (requestCode == BluetoothState.REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                bt.setupService();
                bt.startService(BluetoothState.DEVICE_OTHER);
                // setup josypad view
                setupJoyPadView();
            } else {
                // Do something if user doesn't choose any device (Pressed back)
            }
        } else if (requestCode == QR_CODE) {
            if (resultCode == RESULT_OK) {
                String contents = data.getStringExtra("SCAN_RESULT");
                Log.d("LOG", contents);
                try {
                    JSONObject reader = new JSONObject(contents);
                    // set sharepreference
                    prefs = PreferenceManager.getDefaultSharedPreferences(this);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("pref_debug_switch",false);
                    editor.putBoolean("pref_vibrate_switch",true);
                    editor.putString("pref_delay_list","50");
                    editor.putString("pref_pos_up",reader.getString("up"));
                    editor.putString("pref_pos_down",reader.getString("dw"));
                    editor.putString("pref_pos_left",reader.getString("lf"));
                    editor.putString("pref_pos_right",reader.getString("rt"));
                    editor.putString("pref_pos_a",reader.getString("a"));
                    editor.putString("pref_pos_b",reader.getString("b"));
                    editor.putString("pref_pos_c",reader.getString("c"));
                    editor.putString("pref_pos_d",reader.getString("d"));
                    editor.commit();
                    Snackbar.make(coordinatorLayout, "Config button complete, let's Play!", Snackbar.LENGTH_SHORT).show();
                } catch (JSONException e) {
                    e.printStackTrace();
                    Snackbar.make(coordinatorLayout, "Wrong QRCode!!", Snackbar.LENGTH_SHORT).show();
                }

            }
        }
    }


    private void setupJoyPadView() {

        ivUp = (ImageView) findViewById(R.id.ivUp);
        ivUp.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                ImageView iv = (ImageView) v;
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    iv.setImageResource(R.drawable.bg_stick_pressed);
                    String button = prefs.getString("pref_pos_up", "x");
                    Log.d("LOG", "Up=" + button);
                    setTextResult("Up=" + button);
                    sendBluetoothData(button);
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    iv.setImageResource(R.drawable.bg_stick_unpressed);
                    return true;
                }
                return false;
            }
        });

        ivDown = (ImageView) findViewById(R.id.ivDown);
        ivDown.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                ImageView iv = (ImageView) v;
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    iv.setImageResource(R.drawable.bg_stick_pressed);
                    String button = prefs.getString("pref_pos_down", "x");
                    Log.d("LOG", "Down=" + button);
                    setTextResult("Down=" + button);
                    sendBluetoothData(button);
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    iv.setImageResource(R.drawable.bg_stick_unpressed);
                    return true;
                }
                return false;
            }
        });

        ivLeft = (ImageView) findViewById(R.id.ivLeft);
        ivLeft.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                ImageView iv = (ImageView) v;
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    iv.setImageResource(R.drawable.bg_stick_pressed);
                    String button = prefs.getString("pref_pos_left", "x");
                    Log.d("LOG", "Left=" + button);
                    setTextResult("Left=" + button);
                    sendBluetoothData(button);
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    iv.setImageResource(R.drawable.bg_stick_unpressed);
                    return true;
                }
                return false;
            }
        });

        ivRight = (ImageView) findViewById(R.id.ivRight);
        ivRight.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                ImageView iv = (ImageView) v;
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    iv.setImageResource(R.drawable.bg_stick_pressed);
                    String button = prefs.getString("pref_pos_right", "x");
                    Log.d("LOG", "Right=" + button);
                    setTextResult("Right=" + button);
                    sendBluetoothData(button);
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    iv.setImageResource(R.drawable.bg_stick_unpressed);
                    return true;
                }
                return false;
            }
        });

        ivA = (ImageView) findViewById(R.id.ivA);
        ivA.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                ImageView iv = (ImageView) v;
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    iv.setImageResource(R.drawable.bg_stick_pressed);
                    String button = prefs.getString("pref_pos_a", "x");
                    Log.d("LOG", "A=" + button);
                    setTextResult("A=" + button);
                    sendBluetoothData(button);
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    iv.setImageResource(R.drawable.bg_stick_unpressed);
                    return true;
                }
                return false;
            }
        });

        ivB = (ImageView) findViewById(R.id.ivB);
        ivB.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                ImageView iv = (ImageView) v;
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    iv.setImageResource(R.drawable.bg_stick_pressed);
                    String button = prefs.getString("pref_pos_b", "x");
                    Log.d("LOG", "B=" + button);
                    setTextResult("B=" + button);
                    sendBluetoothData(button);
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    iv.setImageResource(R.drawable.bg_stick_unpressed);
                    return true;
                }
                return false;
            }
        });

        ivC = (ImageView) findViewById(R.id.ivC);
        ivC.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                ImageView iv = (ImageView) v;
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    iv.setImageResource(R.drawable.bg_stick_pressed);
                    String button = prefs.getString("pref_pos_c", "x");
                    Log.d("LOG", "C=" + button);
                    setTextResult("C=" + button);
                    sendBluetoothData(button);
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    iv.setImageResource(R.drawable.bg_stick_unpressed);
                    return true;
                }
                return false;
            }
        });


        ivD = (ImageView) findViewById(R.id.ivD);
        ivD.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                ImageView iv = (ImageView) v;
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    iv.setImageResource(R.drawable.bg_stick_pressed);
                    String button = prefs.getString("pref_pos_d", "x");
                    Log.d("LOG", "D=" + button);
                    setTextResult("D=" + button);
                    sendBluetoothData(button);
                    return true;
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    iv.setImageResource(R.drawable.bg_stick_unpressed);
                    return true;
                }
                return false;
            }
        });


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bt.stopService();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        loadPreference();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(!bt.isBluetoothEnabled()) {
            // Do something if bluetooth is disable
            Snackbar.make(coordinatorLayout, "Bluetooth Disable", Snackbar.LENGTH_LONG)
                    .setAction("Turn On", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                            startActivityForResult(enableBtIntent, BluetoothState.REQUEST_ENABLE_BT);
                        }
                    }).show();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        this.menu = menu;
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.mnuSettings) {
            Intent i = new Intent(getApplicationContext(), SettingsActivity.class);
            startActivityForResult(i, RESULT_SETTING);
        } else if (itemId == R.id.mnuBluetooth) {
            checkBluetoothState();
        } else if (itemId == R.id.mnuFullscreen) {
            hideSystemUI();
        } else if (itemId == R.id.mnuScan) {
            qrcodeScanner();
        }

        return super.onOptionsItemSelected(item);
    }

    private void qrcodeScanner() {
        try {
            Intent intent = new Intent("com.google.zxing.client.android.SCAN");
            intent.putExtra("SCAN_MODE", "QR_CODE_MODE"); // "PRODUCT_MODE for bar codes
            startActivityForResult(intent, QR_CODE);
        } catch (Exception e) {
            Uri marketUri = Uri.parse("market://details?id=com.google.zxing.client.android");
            Intent marketIntent = new Intent(Intent.ACTION_VIEW, marketUri);
            startActivity(marketIntent);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus)
            hideSystemUI();
    }

    // This snippet hides the system bars.
    private void hideSystemUI() {
        // Set the IMMERSIVE flag.
        // Set the content to appear under the system bars so that the content
        // doesn't resize when the system bars hide and show.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mDecorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                            | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                            | View.SYSTEM_UI_FLAG_IMMERSIVE
            );
        }
    }

    // This snippet shows the system bars. It does this by removing all the flags
// except for the ones that make the content appear under the system bars.
    private void showSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mDecorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
    }
}
