package ru.itsps.smrecord.smrecord;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class SettingsActivity extends AppCompatActivity {

    private ServiceConnection sConn;
    private boolean bound = false;
    private SmRecordService myService;
    private static final String TAG = "SmRecordMainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        EditText server = (EditText)findViewById(R.id.server);
        EditText phone = (EditText)findViewById(R.id.phone);
        final Button btnSave = (Button)findViewById(R.id.button_save);

        //Вытащим адрес сервера из локального словаря
        SharedPreferences sharedPreferences = this.getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        server.setText(sharedPreferences.getString(getString(R.string.server),"smr.biws.ru"));
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        String phone1 = telephonyManager.getLine1Number();
        if(phone1 == null || "".equals(phone1.trim())) {
            phone.setText(sharedPreferences.getString(getString(R.string.phone), "89XXXXXXXXX"));
        }
        else
            phone.setText(phone1);

        //Установим обработчик
        View.OnClickListener onClickListener = new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                SaveServer(v);
            }
        };
        btnSave.setOnClickListener(onClickListener);

        sConn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder binder) {
                myService = ((SmRecordService.SmRecordBinder) binder).getService();
                bound = true;
                Log.d(TAG,"onServiceConnected");
            }

            public void onServiceDisconnected(ComponentName name) {
                myService = null;
                bound = false;
                Log.d(TAG,"onServiceDisconnected");
            }
        };

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
       // getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        //if (id == R.id.action_settings) {
        //    return true;
        //}

        return super.onOptionsItemSelected(item);
    }


    //Сохраним адрес сервера
    public void SaveServer(View view){
        EditText phone = (EditText)findViewById(R.id.phone);
        EditText webServiceURL = (EditText)findViewById(R.id.server);
        SharedPreferences sharedPreferences = this.getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(getString(R.string.phone), phone.getText().toString());
        editor.putString(getString(R.string.server), webServiceURL.getText().toString());
        editor.commit();
        //Сохраним в главной активити
        SmRecordService.setPhone(phone.getText().toString());
        this.finish();
    }


}
