package ru.itsps.smrecord.smrecord;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    TextView tvServiceStatus;
    TextView tvDescription;
    TextView tvArchiveStatus;
    TextView tvDbStatus;
    TextView tvLineStatus;
    TextView tvErrorMessage;
    Toolbar  mTopToolbar;
    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;
    private Intent intent;
    boolean bound = false;
    private SmRecordService myService;
    private ServiceConnection sConn;
    private Handler customHandler;
    long interval = 1000;
    private static final String TAG = "SmRecordMainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvServiceStatus = (TextView) findViewById(R.id.tvServiceStatus);
        tvArchiveStatus = (TextView) findViewById(R.id.tvArchiveStatus);
        tvDbStatus = (TextView) findViewById(R.id.tvDbStatus);
        tvLineStatus = (TextView) findViewById(R.id.tvLineStatus);
        tvDescription = (TextView) findViewById(R.id.tvDescription);
        tvErrorMessage = (TextView) findViewById(R.id.tvErrorMessage);
        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        surfaceHolder = surfaceView.getHolder();
        //surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);


        mTopToolbar = (Toolbar) findViewById(R.id.toolbar6);
        setSupportActionBar(mTopToolbar);

        intent = new Intent(this, SmRecordService.class);
        //Запустим сервис на всякий случай
        startService(intent);

        sConn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder binder) {
                bound = true;
                myService = ((SmRecordService.SmRecordBinder) binder).getService();
                tvServiceStatus.setText(myService.getServiceStatus());
                tvArchiveStatus.setText(myService.getArchiveStatus());
                tvLineStatus.setText(myService.getLineStatus());
                tvDbStatus.setText(myService.getDbStatus());
                tvErrorMessage.setText(myService.getErrorMessage());
                myService.setmSurfaceView(surfaceView);

                Log.d(TAG,"onServiceConnected");
            }

            public void onServiceDisconnected(ComponentName name) {
                bound = false;
                myService = null;
                tvServiceStatus.setText("---");
                tvArchiveStatus.setText("---");
                tvLineStatus.setText("---");
                tvDbStatus.setText("---");
                tvErrorMessage.setText("---");
                Log.d(TAG,"onServiceDisconnected");
            }
        };

        customHandler = new Handler();
        //Запустим таймер для отображения очереди
        customHandler.postDelayed(updateTimerThread, interval);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.config) {
            Intent settingsActivity = new Intent(this, SettingsActivity.class);
            startActivity(settingsActivity);
            //Toast.makeText(MainActivity.this, "Action clicked", Toast.LENGTH_LONG).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private Runnable updateTimerThread = new Runnable() {
        public void run() {
            if(bound){
                try{
                    tvServiceStatus.setText(myService.getServiceStatus());
                    tvDbStatus.setText(myService.getDbStatus());
                    tvArchiveStatus.setText(myService.getArchiveStatus());
                    tvLineStatus.setText(myService.getLineStatus());
                    tvErrorMessage.setText(myService.getErrorMessage());
                    Log.d(TAG,"updateTimerThread");
                }catch(Exception e){
//                    tvLineStatus.setText(e.getErrorMessage());
                    tvServiceStatus.setText("---");
                    tvArchiveStatus.setText("---");
                    tvLineStatus.setText("---");
                    tvDbStatus.setText("---");
                    tvErrorMessage.setText("---");
                }
            }
            else
            {
                tvLineStatus.setText("---");
            }
            customHandler.postDelayed(updateTimerThread, interval);
        }
    };


    @Override
    protected void onResume() {
        Log.d(TAG,"onResume");
        super.onResume();
        bindService(intent, sConn, 0 /*BIND_AUTO_CREATE*/);
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart");
        super.onStart();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
        // TODO Auto-generated method stub
        if (!bound)
            return;
        unbindService(sConn);
        bound = false;
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
        // TODO Auto-generated method stub
        if (!bound)
            return;
        unbindService(sConn);
        bound = false;
    }

//    // Запуск службы
//    public void onClickStartService(View view) {
//        Log.d(TAG,"onClickStartService");
//        startService(new Intent(this, SmRecordService.class));
//    };

//    // Остановка службы
//    public void onClickStopService(View view) {
//        stopService(new Intent(this, SmRecordService.class));
//    };
}
