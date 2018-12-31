/**
 * Created by penart on 04.10.2016.
 * Работа с линией, запись звонков в БД
 */
package ru.itsps.smrecord.smrecord;

    import java.io.File;
    import java.io.FileOutputStream;
    import java.text.SimpleDateFormat;
    import java.util.Calendar;
    import java.util.Date;
    import java.util.Timer;
    import java.util.TimerTask;
    import java.util.Vector;

    import android.app.PendingIntent;
    import android.app.Service;
    import android.app.AlarmManager;
    import android.content.ContentValues;
    import android.content.Context;
    import android.content.Intent;
    import android.content.SharedPreferences;
    import android.database.Cursor;
    import android.database.DatabaseUtils;
    import android.database.sqlite.SQLiteDatabase;
    import android.location.Location;
    import android.location.LocationListener;
    import android.location.LocationManager;
    import android.media.CamcorderProfile;
    import android.os.Bundle;
    import android.media.MediaRecorder;
    import android.os.Binder;
    import android.os.Environment;
    import android.os.IBinder;
    import android.os.Looper;
    import android.support.annotation.NonNull;
    import android.telephony.PhoneStateListener;
    import android.telephony.TelephonyManager;
    import android.util.Log;

    import com.google.gson.Gson;
    import com.google.gson.GsonBuilder;

    import okhttp3.MediaType;
    import okhttp3.MultipartBody;
    import okhttp3.RequestBody;
    import okhttp3.ResponseBody;
    import retrofit2.Call;
    import retrofit2.Response;
    import retrofit2.Retrofit;
    import retrofit2.converter.gson.GsonConverterFactory;

    import android.hardware.Camera;
    import android.view.SurfaceView;


public class SmRecordService extends Service {

    public static final String MULTIPART_FORM_DATA = "multipart/form-data";

    private TelephonyManager telephonyManager;
    private AlarmManager alarmManager;
    private SmRecordPhoneStateListener listenPhone;
    private SmRecordBinder binder = new SmRecordBinder();
    private RecordStoreContract contract;
    private SQLiteDatabase db;
    private Timer timerSendData;
    private TimerTask tTaskSendData;
    private Timer timerRecMIC;
    private TimerTask tTaskRecMIC;
    private Timer timerRecCAM;
    private TimerTask tTaskRecCAM;
    private MediaRecorder recorder;
    private MediaRecorder recorder2;
    private MediaRecorder recorder3;
    private Camera camera;

    private long intervalSendData = 60000;
    private long intervalRecMIC = 5000;
    //private long interval2 = 30000;
    private long intervalRecCAM = 5000;
    private static boolean isServiceStarted = false;
    private String serviceStatus;
    private String archiveStatus;
    private String dbStatus;
    private String lineStatus;
    private String IMEI;
    private static String phone1;
    private String incoming_nr;
    private static String outcoming_nr;
    private int prev_state;
    private int call_direction;
    private int call_lost;
    private Date call_start;
    private Date call_stop;
    private int call_duration;
    private String call_remote_phone;
    private String call_record_file;
    private Date call_start2;
    private Date call_stop2;
    private int call_duration2;
    private String call_record_file2;
    private Date call_start3;
    private Date call_stop3;
    private int call_duration3;
    private String call_record_file3;
    private static final String TAG = "SmRecordService";
    private String errorMessage;
    private static String audioSource;
    private int isRecMIC;
    private int isRecCAM;
    private int isRecGPS;
    private int hourFrom;
    private int hourTo;
    private boolean isRecordingMIC = false;
    private boolean isRecordingCAM = false;
    private LocationManager locationManager;
    //private static int slot;
    volatile private long time;
    volatile private double lat;
    volatile private double lon;
    // private int sendCount;
    private int statusNet;
    private boolean enabledNet;
    private static SurfaceView mSurfaceView;
    Intent intent1;
    PendingIntent pIntent1;



    public synchronized static boolean isServiceStarted() {
        return isServiceStarted;
    }

    public synchronized static String getOutcoming_nr() {
        return outcoming_nr;
    }

    public synchronized static void setOutcoming_nr(String outcoming_nr) {
        SmRecordService.outcoming_nr = outcoming_nr;
    }

    public synchronized static void setPhone(String phone){
        phone1 = phone;
    }

    public synchronized static void setAudioSource(String source){
        audioSource = source;
    }

    public synchronized static void setmSurfaceView(SurfaceView surfaceView){
        mSurfaceView = surfaceView;
    }

    /*
    public synchronized static int getSlot() {
        return slot;
    }
    public synchronized static void setSlot(int slot) {
        SmRecordService.slot = slot;
    }
*/
    public SmRecordBinder getBinder() {
        return binder;
    }

    public String getServiceStatus(){
        return serviceStatus;
    }

    public String getArchiveStatus(){
        return archiveStatus;
    }

    public String getErrorMessage(){
        return errorMessage;
    }

    public String getDbStatus(){
        long count = DatabaseUtils.queryNumEntries(db, RecordStoreContract.Record.TABLE_NAME);
        dbStatus = Long.toString(count);
        return dbStatus;
    }

    public String getLineStatus(){
        return lineStatus;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // TODO Auto-generated method stub
        return super.onUnbind(intent);
        // return true;
    }

    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
        /*TelephonyInfo telephonyInfo = TelephonyInfo.getInstance(this);
        boolean isDualSIM = telephonyInfo.isDualSIM();*/

        telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        IMEI = telephonyManager.getDeviceId();
//        phone1 = telephonyManager.getLine1Number();


        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        //получим alarmService
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        //Вытащим номер телефона из локального словаря
        SharedPreferences sharedPreferences = this.getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        phone1 = sharedPreferences.getString(getString(R.string.phone),"890000000000");

        audioSource = sharedPreferences.getString("audioSource","MIC");

        isRecMIC = sharedPreferences.getInt("isRecMIC", 1);
        isRecCAM = sharedPreferences.getInt("isRecCAM", 1);
        hourFrom = sharedPreferences.getInt("hourFrom", 8);
        hourTo = sharedPreferences.getInt("hourTo", 16);
        isRecGPS = sharedPreferences.getInt("isRecordingGPS", 1);

        //todo only for testing
        isRecCAM = 1;

        //slot = 0;
        listenPhone = new SmRecordPhoneStateListener();
        //таймер отправки записей
        timerSendData = new Timer(true);
        //таймер записи по расписанию
        timerRecMIC = new Timer(true);
        //таймер записи по расписанию
        //timerRecCAM = new Timer(true);
        timerRecCAM = new Timer();

        //Видео
        contract = new RecordStoreContract();
        RecordStoreContract.RecordStoreDbHelper mDbHelper = contract.getRecordStoreDbHelper(getApplicationContext());
        db = mDbHelper.getWritableDatabase();
        dbStatus="Ok";



        /*Notification.Builder builder = new Notification.Builder(this)
                .setSmallIcon(R.drawable.common_ic_googleplayservices)
                .setWhen(System.currentTimeMillis() + interval2);
        Notification notification;
        if (Build.VERSION.SDK_INT < 16)
            notification = builder.getNotification();
        else
            notification = builder.build();
        startForeground(777, notification);*/

        Log.d(TAG,"onCreate");
    }

    /**
     * Отправка сообщений через каждые Interval милисекунд
     */
    private void Schedule() {
        if (tTaskSendData != null)
            tTaskSendData.cancel();
        if (intervalSendData > 0) {
            tTaskSendData = new TimerTask() {
                public void run() {
                    SendRecords();
                }
            };
            timerSendData.schedule(tTaskSendData, intervalSendData, intervalSendData);
        }

        if (tTaskRecMIC != null)
            tTaskRecMIC.cancel();

        tTaskRecMIC = new TimerTask() {
                    public void run() {
                        RecordMIC();
                    }
        };
        timerRecMIC.schedule(tTaskRecMIC, intervalRecMIC, intervalRecMIC);

        if (tTaskRecCAM != null)
            tTaskRecCAM.cancel();

        tTaskRecCAM = new TimerTask() {
            public void run() {
                //Looper.prepare();
                //Looper.loop();
                RecordShutCAM();
            }
        };
        timerRecCAM.schedule(tTaskRecCAM, intervalRecCAM, intervalRecCAM);

        //Настроим аларм для пробуждения сервиса
        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.setAction("Wake UP");
        intent.putExtra("extra", "extra 1");
        pIntent1 = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, 5000, 5000, pIntent1);
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        telephonyManager.listen(listenPhone, PhoneStateListener.LISTEN_NONE);
        try {
            locationManager.removeUpdates(locationListener);
        }
        catch (Exception e){
            Log.w(TAG, e.getMessage());
        }

        if(camera!=null) {
            camera.stopPreview();
            camera.release();
        }

        tTaskSendData.cancel();
        if(tTaskRecMIC!=null)
            tTaskRecMIC.cancel();
        if(tTaskRecCAM!=null)
            tTaskRecCAM.cancel();
        timerSendData.cancel();
        timerRecMIC.cancel();
        timerRecCAM.cancel();

        //Служба выключена
        isServiceStarted = false;
        super.onDestroy();

        //Removing any notifications
        //notificationManager.cancel(DEFAULT_NOTIFICATION_ID);

        //Disabling service
        //stopSelf();


        Log.d(TAG,"onDestroy");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // TODO Auto-generated method stub
        try {
            telephonyManager.listen(listenPhone, PhoneStateListener.LISTEN_CALL_STATE);
        } catch (Exception e) {
            Log.w(TAG,"telephonyManager.listen() error: "+e.getMessage());
        }

        try {
            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    180000,
                    0,
                    locationListener);
            Log.d(TAG, "requestLocationUpdates OK!");
        } catch (Exception e) {
            Log.w(TAG, "locationManager.requestLocationUpdates() error: "+e.getMessage());
        }
        serviceStatus = "running";
        // Запустим таймер отправки сообщений
        Schedule();
        Log.d(TAG,"onStartCommand");

        //служба запущена
        isServiceStarted = true;
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return binder;
    }

    /* Custom PhoneStateListener */
    private class SmRecordPhoneStateListener extends PhoneStateListener {

        private static final String TAG = "SmRecordStateListener";
        SimpleDateFormat sdtf = new SimpleDateFormat("yyyyMMddHHmmss");

        @Override
        public void onCallStateChanged(int state, String incomingNumber){

            if(incomingNumber!=null && incomingNumber.length()>0) incoming_nr=incomingNumber;

            switch(state){
                case TelephonyManager.CALL_STATE_RINGING:
                    if((prev_state==TelephonyManager.CALL_STATE_IDLE)) {
                        Log.d(TAG, "CALL_STATE_RINGING==>" + incoming_nr);
                        prev_state = state;
                        lineStatus = "RING";
                    }
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    if((prev_state==TelephonyManager.CALL_STATE_IDLE && getOutcoming_nr()!=null)){
                        prev_state=state;
                        //совершение исходящего звонка
                        call_direction=0;
                        call_remote_phone=getOutcoming_nr();
                        lineStatus = "CALL OUT";
                        call_start = new Date();
                        call_stop = null;
                        call_duration = 0;
                        call_record_file = String.format("%s/%s-%s-%s-%d.3gp", getFullSdPath(), sdtf.format(call_start), phone1, call_remote_phone, call_direction);
                        Log.d(TAG, "CALL_STATE_OFFHOOK==>"+call_remote_phone);
                        //Начать запись
                        StartRecordingCALL(call_record_file);
                    } else if((prev_state==TelephonyManager.CALL_STATE_RINGING
                            || (prev_state==TelephonyManager.CALL_STATE_IDLE && incoming_nr!=null && getOutcoming_nr()==null))){
                        prev_state=state;
                        //ответ на входящий звонок
                        call_direction=1;
                        call_remote_phone = incoming_nr;
                        lineStatus = "CALL IN";
                        call_start = new Date();
                        call_stop = null;
                        call_duration = 0;
                        call_record_file = String.format("%s/%s-%s-%s-%d.3gp", getFullSdPath(), sdtf.format(call_start), phone1, call_remote_phone, call_direction);
                        Log.d(TAG, "CALL_STATE_OFFHOOK==>"+call_remote_phone);
                        //Начать запись
                        StartRecordingCALL(call_record_file);
                    }
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    Log.d(TAG, "CALL_STATE_IDLE=>"+call_remote_phone);
                    if((prev_state==TelephonyManager.CALL_STATE_OFFHOOK)){
                        //конец звонка входящего или исходящего
                        call_lost = 0;
                        call_stop = new Date();
                        call_duration = (int)(call_stop.getTime() - call_start.getTime());
                        //Остановить запись
                        StopRecordingCALL();
                        //сохранить в БД
                        ContentValues values = new ContentValues();
                        values.put(RecordStoreContract.Record.COLUMN_NAME_STARTDT,  sdtf.format(call_start));
                        values.put(RecordStoreContract.Record.COLUMN_NAME_STOPDT,  sdtf.format(call_stop));
                        values.put(RecordStoreContract.Record.COLUMN_NAME_DURATION,  call_duration);
                        values.put(RecordStoreContract.Record.COLUMN_NAME_DIRECTION,  call_direction);
                        values.put(RecordStoreContract.Record.COLUMN_NAME_UNANSWERED,  call_lost);
                        values.put(RecordStoreContract.Record.COLUMN_NAME_REMOTE_PHONE,  call_remote_phone);
                        values.put(RecordStoreContract.Record.COLUMN_NAME_RECORD_FILE,  call_record_file);
                        db.insert(RecordStoreContract.Record.TABLE_NAME, "", values);
                        Log.d(TAG,"Call record store");
                    } else if((prev_state==TelephonyManager.CALL_STATE_RINGING)){
                        //Отклонёные или пропушщенные звонки
                        call_start = new Date();
                        call_lost = 1;
                        call_stop = null;
                        call_duration = 0;
                        //сохранить в БД
                        ContentValues values = new ContentValues();
                        values.put(RecordStoreContract.Record.COLUMN_NAME_STARTDT,  sdtf.format(call_start));
                        values.put(RecordStoreContract.Record.COLUMN_NAME_STOPDT,  "");
                        values.put(RecordStoreContract.Record.COLUMN_NAME_DURATION,  call_duration);
                        values.put(RecordStoreContract.Record.COLUMN_NAME_DIRECTION,  call_direction);
                        values.put(RecordStoreContract.Record.COLUMN_NAME_UNANSWERED,  call_lost);
                        values.put(RecordStoreContract.Record.COLUMN_NAME_REMOTE_PHONE,  call_remote_phone);
                        values.put(RecordStoreContract.Record.COLUMN_NAME_RECORD_FILE,  call_record_file);
                        db.insert(RecordStoreContract.Record.TABLE_NAME, "", values);
                        Log.d(TAG,"Reject record store");
                    }
                    prev_state=state;
                    outcoming_nr = null;
                    incoming_nr=null;
                    lineStatus = "IDLE";
                    break;
            }
            Log.d(TAG, lineStatus);
        }
    }



    public class SmRecordBinder extends Binder {
        SmRecordService getService() {
            return SmRecordService.this;
        }
    }


    private void StartRecordingCALL(String record_file){
        Log.d(this.getClass().getName(),record_file);
        recorder = new MediaRecorder();
        try {
            if("MIC".equals(audioSource))
                recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            else
                recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            //recorder.setMaxDuration(3600000);
            recorder.setOutputFile(record_file);
            recorder.prepare();
            recorder.start();   // Recording is now started
        } catch(Exception e){
            if(recorder!=null) {
                recorder.release();
                recorder = null;
            }
            errorMessage = "Error record, "+e.getMessage();
            Log.w(this.getClass().getName(),  errorMessage);
            e.printStackTrace();
        }
        Log.i(this.getClass().getName(), "Start Recording");
    }

    private void StopRecordingCALL(){
        Log.i(this.getClass().getName(), "Stop Recording");
        if(recorder != null) {
            try {
                recorder.stop();
                recorder.reset();
                recorder.release();
                recorder = null;
            } catch (Exception e) {
                Log.w(this.getClass().getName(), "Error Stopping record");
            }
        }
    }


    private void StartRecordingMIC(String record_file){
        Log.d(this.getClass().getName(),record_file);
        try {
            recorder2 = new MediaRecorder();
            if("MIC".equals(audioSource))
                recorder2.setAudioSource(MediaRecorder.AudioSource.MIC);
            else
                recorder2.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL);
            recorder2.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder2.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            //recorder.setMaxDuration(60000);
            recorder2.setOutputFile(record_file);
            recorder2.prepare();
            recorder2.start();   // Recording is now started
        } catch(Exception e){
            if(recorder2!=null){
                recorder2.release();
                recorder2 = null;
            }
            errorMessage = "Error record, "+e.getMessage();
            Log.w(this.getClass().getName(),  errorMessage);
            e.printStackTrace();
        }
        Log.i(this.getClass().getName(), "Start Recording");
    }

    private void StopRecordingMIC(){
        Log.i(this.getClass().getName(), "Stop Recording");
        if(recorder2 != null) {
            try {
                recorder2.stop();
                recorder2.reset();
                recorder2.release();
                recorder2 = null;
            } catch (Exception e) {
                Log.w(this.getClass().getName(), "Error Stopping record");
            }
        }
    }

    private void StartRecordingCAM(String record_file){
        Log.d(this.getClass().getName(),record_file);
        try {

            if(camera==null)
                camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
            else
                camera.reconnect();

            // Step 1: Unlock and set camera to MediaRecorder
            //camera.unlock();
            camera.startPreview();
            //mSurfaceView = new CameraPreview(this, camera);
            //mSurfaceView.setId(107);
            recorder3 = new MediaRecorder();
            recorder3.setCamera(camera);

            // Step 2: Set sources
            recorder3.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
            recorder3.setVideoSource(MediaRecorder.VideoSource.CAMERA);

            // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
            //recorder3.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder3.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_LOW));
            //recorder3.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
            //recorder3.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);
            //recorder3.setVideoFrameRate(15);
            //recorder3.setMaxDuration(60000);
            recorder3.setOutputFile(record_file);
            recorder3.setPreviewDisplay(mSurfaceView.getHolder().getSurface());

            recorder3.prepare();
            recorder3.start();   // Recording is now started

        } catch(Exception e){
            StopRecordingCAM();
//            recorder3.stop();
//            recorder3.reset();
//            recorder3.release();
//            recorder3 = null;

//            camera.stopPreview();
            //camera.lock();
            //camera.release();
            errorMessage = "Error record, "+e.getMessage();
            Log.w(this.getClass().getName(),  errorMessage);
            e.printStackTrace();
        }
        Log.i(this.getClass().getName(), "Start Recording");
    }

    private void StopRecordingCAM(){
        Log.i(this.getClass().getName(), "Stop Recording");
        if(recorder3 != null) {
            try {
                recorder3.stop();
                recorder3.reset();
                recorder3.release();
                recorder3 = null;

            } catch (Exception e) {
                Log.w(this.getClass().getName(), "Error Stopping record video");
            }

            if(camera != null){
                camera.stopPreview();
                //camera.lock();
                //camera.release();
                //camera = null;
            }
        }
    }

    //Сделать снимок с камеры
    private void ShutCAM(){
        if(isRecCAM == 1) {
            try {

                if (camera == null) {
                    camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
                    //mSurfaceView = new CameraPreview(this.getApplicationContext(), camera);
                }
//                else
//                    camera.reconnect();
                camera.setPreviewDisplay(mSurfaceView.getHolder());
                camera.startPreview();

                camera.takePicture(null, null, new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {

                        Date call_start = new Date();
                        SimpleDateFormat sdtf = new SimpleDateFormat("yyyyMMddHHmmss");
                        String record_file = String.format("%s/%s-%s-%s-%d.jpg", getFullSdPath(), sdtf.format(call_start), phone1, "CAM", 1);

                        try {
                            Log.d(this.getClass().getName(), " Запись файла снимка: " + record_file);
                            FileOutputStream fos = new FileOutputStream(record_file);
                            fos.write(data);
                            fos.close();

                            Date call_stop = new Date();
                            int call_duration = (int) (call_stop.getTime() - call_start.getTime());

                            //сохранить в БД
                            ContentValues values = new ContentValues();
                            values.put(RecordStoreContract.Record.COLUMN_NAME_STARTDT, sdtf.format(call_start));
                            values.put(RecordStoreContract.Record.COLUMN_NAME_STOPDT, sdtf.format(call_stop));
                            values.put(RecordStoreContract.Record.COLUMN_NAME_DURATION, call_duration);
                            values.put(RecordStoreContract.Record.COLUMN_NAME_DIRECTION, 1);
                            values.put(RecordStoreContract.Record.COLUMN_NAME_UNANSWERED, 0);
                            values.put(RecordStoreContract.Record.COLUMN_NAME_REMOTE_PHONE, "CAM");
                            values.put(RecordStoreContract.Record.COLUMN_NAME_RECORD_FILE, record_file);
                            db.insert(RecordStoreContract.Record.TABLE_NAME, "", values);

                        } catch (Exception e) {
                            Log.w(this.getClass().getName(), " Ошибка запись файла снимка: " + record_file);
                            Log.w(this.getClass().getName(), e.getMessage());
                        }
                        camera.startPreview();
                    }
                });
                Log.i(this.getClass().getName(), "Shut cam");

            } catch (Exception e) {
                camera.stopPreview();
                camera.release();
                camera = null;
                errorMessage = "Error shut cam, " + e.getMessage();
                Log.w(this.getClass().getName(), errorMessage);
                e.printStackTrace();
            }
        }
    }



    private String getFullSdPath(){
        String dirRecords = "SmRecordStore";
        String sdPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        //String sdPath = Environment.getDataDirectory().getAbsolutePath();
        File sdCard = new File(sdPath +"/"+ dirRecords);
        if (!sdCard.exists()) {
            sdCard.mkdir();
            Log.w(TAG,"Created Dir "+sdCard.getAbsolutePath());
        }
        Log.d(TAG, "Full path of record sound is : "+sdCard.getAbsolutePath());
        return sdCard.getAbsolutePath();
    }

    @NonNull
    private RequestBody createPartFromString(String descriptionString) {
        return RequestBody.create(MediaType.parse(MULTIPART_FORM_DATA), descriptionString);
    }

    @NonNull
    private MultipartBody.Part createPartFromFile(String recordfile, File file) {
        return MultipartBody.Part.createFormData(
                RecordStoreContract.Record.COLUMN_NAME_RECORD_FILE,
                recordfile,
                RequestBody.create(MediaType.parse(MULTIPART_FORM_DATA), file));
    }

    private void SendRecords() {
        //Запросим данные из БД 10 строк
        RecordStoreContract contract = new RecordStoreContract();
        RecordStoreContract.RecordStoreDbHelper mDbHelper = contract.getRecordStoreDbHelper(getApplicationContext());
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        try {

            SharedPreferences sharedPreferences = this.getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
            String server = sharedPreferences.getString(getString(R.string.server), "smr.biws.ru");

            if ("".equals(server)) {
                Log.i(TAG, "Server not defined!");
                return;
            }


            Gson gson = new GsonBuilder()
                    .setLenient()
                    .create();
            //Отправим данные на сервер
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("http://" + server + "/smrecord/")
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
            SmRecordHttpAPI service = retrofit.create(SmRecordHttpAPI.class);

            //Получим настройки с сервера
            Call<Conf> call = service.GetConf(phone1);
            if (call != null) {
                try {
                    Response<Conf> response = call.execute();
                    if (response != null) {
                        Log.d(TAG, response.toString());
                        if (response.isSuccessful() && response.code() == 200) {


                            int _isRecMIC = response.body().getIsRecMIC();
                            Log.d(TAG, "isRecMIC=" + _isRecMIC);
                            int _isRecCAM = response.body().getIsRecCAM();
                            Log.d(TAG, "isRecCAM=" + _isRecCAM);
                            int _hourFrom = response.body().getHourFrom();
                            Log.d(TAG, "hourFrom=" + _hourFrom);
                            int _hourTo = response.body().getHourTo();
                            Log.d(TAG, "hourTo=" + _hourTo);

                            int _isRec = response.body().getIsRec();
                            Log.d(TAG, "isRec=" + _isRec);
                            _isRecMIC = _isRec;
                            _isRecCAM = _isRec;

                            //Если параметры изменились то запомним их
                            if (_isRecMIC != isRecMIC ||_isRecCAM != isRecCAM || _hourFrom != hourFrom || _hourTo != hourTo) {
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putInt("isRecMIC", _isRecMIC);
                                editor.putInt("isRecCAM", _isRecCAM);
                                editor.putInt("hourFrom", _hourFrom);
                                editor.putInt("hourTo", _hourTo);
                                editor.commit();
                                isRecMIC = _isRecMIC;
                                isRecCAM = _isRecCAM;
                                hourFrom = _hourFrom;
                                hourTo = _hourTo;
                            }

                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, e.getMessage());
                }
            }


            String[] columns = {
                    RecordStoreContract.Record._ID,
                    RecordStoreContract.Record.COLUMN_NAME_STARTDT,
                    RecordStoreContract.Record.COLUMN_NAME_STOPDT,
                    RecordStoreContract.Record.COLUMN_NAME_DURATION,
                    RecordStoreContract.Record.COLUMN_NAME_DIRECTION,
                    RecordStoreContract.Record.COLUMN_NAME_UNANSWERED,
                    RecordStoreContract.Record.COLUMN_NAME_REMOTE_PHONE,
                    RecordStoreContract.Record.COLUMN_NAME_RECORD_FILE,
            };

            String[] locColumns = {
                    RecordStoreContract.LocRecord._ID,
                    RecordStoreContract.LocRecord.COLUMN_NAME_DT,
                    RecordStoreContract.LocRecord.COLUMN_NAME_LAT,
                    RecordStoreContract.LocRecord.COLUMN_NAME_LON,
            };

            SimpleDateFormat sdtf = new SimpleDateFormat("yyyyMMddHHmmss");
            //Брать только записи старше 10 минут
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MINUTE, -10);
            Date timeFrom = cal.getTime();

            Cursor cur = db.query(
                    RecordStoreContract.Record.TABLE_NAME,
                    columns,
                    null,//RecordStoreContract.Record.COLUMN_NAME_STARTDT +" < ?",
                    null,//new String[] {sdtf.format(timeFrom)},
                    null,
                    null,
                    null, //RecordStoreContract.Record.COLUMN_NAME_STARTDT + " ASC",
                    "10"
            );

            Vector<RecordRow> rows = new Vector<RecordRow>();
            if (cur.moveToFirst()) {
                do {
                    RecordRow row = new RecordRow();
                    row.setId(cur.getInt(0));
                    row.setCall_start(cur.getString(1));
                    row.setCall_stop(cur.getString(2));
                    row.setCall_duration(cur.getInt(3));
                    row.setCall_direction(cur.getInt(4));
                    row.setCall_unanswered(cur.getInt(5));
                    row.setCall_remote_phone(cur.getString(6));
                    row.setCall_record_file(cur.getString(7));
                    rows.add(row);
                } while (cur.moveToNext());
            }
            cur.close();


            cur = db.query(
                    RecordStoreContract.LocRecord.TABLE_NAME,
                    locColumns,
                    null,//RecordStoreContract.Record.COLUMN_NAME_STARTDT +" < ?",
                    null,//new String[] {sdtf.format(timeFrom)},
                    null,
                    null,
                    null, //RecordStoreContract.Record.COLUMN_NAME_STARTDT + " ASC",
                    "100"
            );

            Vector<LocRow> locRows = new Vector<LocRow>();
            if (cur.moveToFirst()) {
                do {
                    LocRow row = new LocRow();
                    row.setId(cur.getInt(0));
                    row.setDt(cur.getString(1));
                    row.setLat(String.format("%.6f",cur.getFloat(2)));
                    row.setLon(String.format("%.6f",cur.getFloat(3)));
                    locRows.add(row);
                } while (cur.moveToNext());
            }
            cur.close();

            //Отправим на сервер данные
            for (RecordRow row : rows) {
                Log.w(TAG, String.format("phone1=%s, startdt=%s, stopdt=%s, direction=%d, unanswered=%d, duration=%d, remote_phone=%s, record_file=%s",
                        phone1,
                        row.getCall_start(),
                        row.getCall_stop(),
                        row.getCall_direction(),
                        row.getCall_unanswered(),
                        row.getCall_duration(),
                        row.getCall_remote_phone(),
                        row.getCall_record_file()
                ));
                File file = new File(row.getCall_record_file() == null ? "" : row.getCall_record_file());
                Call<ResponseBody> call2;
                if (row.getCall_record_file() != null && file.exists()) {
                    Log.d(TAG, "service.SaveRecord2");
                    //todo сделать обрезание пути у файла записи
                    call2 = service.SaveRecord2(
                            createPartFromString(row.getCall_start()),
                            createPartFromString(row.getCall_stop() == null ? "" : row.getCall_stop()),
                            createPartFromString(Integer.toString(row.getCall_direction())),
                            createPartFromString(Integer.toString(row.getCall_duration())),
                            createPartFromString(Integer.toString(row.getCall_unanswered())),
                            createPartFromString(phone1 == null ? "" : phone1),
                            createPartFromString(row.getCall_remote_phone() == null ? "" : row.getCall_remote_phone()),
                            createPartFromString(row.getCall_record_file() == null ? "" : row.getCall_record_file()),
                            createPartFromFile(row.getCall_record_file(), file));
                } else {
                    Log.d(TAG, "service.SaveRecord");
                    call2 = service.SaveRecord(
                            row.getCall_start(),
                            row.getCall_stop(),
                            row.getCall_direction(),
                            row.getCall_duration(),
                            row.getCall_unanswered(),
                            phone1,
                            (row.getCall_remote_phone() == null ? "" : row.getCall_remote_phone()),
                            row.getCall_record_file());
                }
                Log.d(TAG, "End SaveRecord");

                if (call2 != null) {
                    try {
                        Response<ResponseBody> response2 = call2.execute();
                        if (response2 != null) {
                            Log.d(TAG, response2.toString());
                            if (response2.isSuccessful()) {
                                Log.v(TAG, "Upload success");
                                db.delete(RecordStoreContract.Record.TABLE_NAME, RecordStoreContract.Record._ID + "=" + row.getId(), null);

                                //todo сделать удаление файла
                                if (file.exists()) {
                                    try {
                                        file.delete();
                                    } catch (Exception e) {
                                        Log.w(TAG, "Delete file error: " + file.getName());
                                    }
                                }

                                Log.i(TAG, "Transmit record OK!");
                            } else {
                                Log.w(TAG, "Upload error: " + response2.raw().toString());
                            }
                        }
                    } catch (Exception e) {
                        Log.w(TAG, e.getMessage());
                    }

                }

            }

            Log.d(TAG, "locRows.count="+locRows.size());
            //Передадим позицию
            for(LocRow row : locRows) {
                //todo
                Call<ResponseBody> call3 = service.SendLoc(phone1, row.getDt(), row.getLat(), row.getLon());
                Response<ResponseBody> response = call3.execute();
                if(response.isSuccessful()){
                    db.delete(RecordStoreContract.LocRecord.TABLE_NAME, RecordStoreContract.LocRecord._ID + "=" + row.getId(), null);
                    Log.w(TAG, "Sendloc is successful!");
                }
                else
                    Log.w(TAG, "Sendloc is not successful!");
            }

        }
        catch(Exception e){
            Log.w(TAG, "Error Transmit");
            e.printStackTrace();
        }


        db.close();
    }

    //Запись с микрофона по расписанию
    private void RecordMIC() {
        //Выполним остановку и запуск записи за один цикл

        //Если запись идёт проверим, проверим необходимость остановки
        if(isRecordingMIC) {
            call_stop2 = new Date();
            call_duration2 = (int) (call_stop2.getTime() - call_start2.getTime());
            if (call_duration2 >= 300000) {
                SimpleDateFormat sdtf = new SimpleDateFormat("yyyyMMddHHmmss");
                //Остановить запись
                StopRecordingMIC();
                if (recorder2 == null) {
                    //сохранить в БД
                    ContentValues values = new ContentValues();
                    values.put(RecordStoreContract.Record.COLUMN_NAME_STARTDT, sdtf.format(call_start2));
                    values.put(RecordStoreContract.Record.COLUMN_NAME_STOPDT, sdtf.format(call_stop2));
                    values.put(RecordStoreContract.Record.COLUMN_NAME_DURATION, call_duration2);
                    values.put(RecordStoreContract.Record.COLUMN_NAME_DIRECTION, 1);
                    values.put(RecordStoreContract.Record.COLUMN_NAME_UNANSWERED, 0);
                    values.put(RecordStoreContract.Record.COLUMN_NAME_REMOTE_PHONE, "MIC");
                    values.put(RecordStoreContract.Record.COLUMN_NAME_RECORD_FILE, call_record_file2);
                    db.insert(RecordStoreContract.Record.TABLE_NAME, "", values);
                    isRecordingMIC = false;
                }
            }
        }

        //Если разрешено и запись не начата, то начнём запись
        if (isRecMIC ==1 && !isRecordingMIC) {
            call_start2 = new Date();
            int hour = call_start2.getHours();
            if (hour >= hourFrom && hour <= hourTo) {
                SimpleDateFormat sdtf = new SimpleDateFormat("yyyyMMddHHmmss");
                call_record_file2 = String.format("%s/%s-%s-%s-%d.3gp", getFullSdPath(), sdtf.format(call_start2), phone1, "MIC", 1);
                //Начать запись
                StartRecordingMIC(call_record_file2);
                if (recorder2 != null)
                    isRecordingMIC = true;
            }
        }

    }

    //Фото с камеры по расписанию
    private void RecordShutCAM() {
        if(isRecCAM == 1){
            //Сделать снимок
            //ShutCAM();
        }
    }

    //Запись с камеры по расписанию
    private void RecordCAM() {

        if(isRecordingCAM) {
            call_stop3 = new Date();
            call_duration3 = (int)(call_stop3.getTime() - call_start3.getTime());
            if(call_duration3 >= 5000) {
                SimpleDateFormat sdtf = new SimpleDateFormat("yyyyMMddHHmmss");
                //Остановить запись
                StopRecordingCAM();
                if(recorder3==null) {
                    //сохранить в БД
                    ContentValues values = new ContentValues();
                    values.put(RecordStoreContract.Record.COLUMN_NAME_STARTDT, sdtf.format(call_start3));
                    values.put(RecordStoreContract.Record.COLUMN_NAME_STOPDT, sdtf.format(call_stop3));
                    values.put(RecordStoreContract.Record.COLUMN_NAME_DURATION, call_duration3);
                    values.put(RecordStoreContract.Record.COLUMN_NAME_DIRECTION, 1);
                    values.put(RecordStoreContract.Record.COLUMN_NAME_UNANSWERED, 0);
                    values.put(RecordStoreContract.Record.COLUMN_NAME_REMOTE_PHONE, "CAM");
                    values.put(RecordStoreContract.Record.COLUMN_NAME_RECORD_FILE, call_record_file3);
                    db.insert(RecordStoreContract.Record.TABLE_NAME, "", values);
                    isRecordingCAM = false;
                }
            }
        }

        if(isRecCAM ==1 && !isRecordingCAM){
            call_start3 = new Date();
            int hour = call_start3.getHours();
            if(hour >= hourFrom && hour <= hourTo) {
                SimpleDateFormat sdtf = new SimpleDateFormat("yyyyMMddHHmmss");
                call_record_file3 = String.format("%s/%s-%s-%s-%d.3gp", getFullSdPath(), sdtf.format(call_start3), phone1, "CAM", 1);
                //Начать запись
                StartRecordingCAM(call_record_file3);
                if(recorder3!=null)
                    isRecordingCAM= true;
            }
        }

    }


    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            time = location.getTime();
            lat = location.getLatitude();
            lon = location.getLongitude();
            Log.d(TAG, String.format("Location change: %f, %f", lat, lon));
            if (time > 0 && lat != 0 && lon != 0) {
                //сохранить в БД
                SimpleDateFormat sdtf = new SimpleDateFormat("yyyyMMddHHmmss");
                ContentValues values = new ContentValues();
                values.put(RecordStoreContract.LocRecord.COLUMN_NAME_DT, sdtf.format(time));
                values.put(RecordStoreContract.LocRecord.COLUMN_NAME_LAT, lat);
                values.put(RecordStoreContract.LocRecord.COLUMN_NAME_LON, lon);
                db.insert(RecordStoreContract.LocRecord.TABLE_NAME, "", values);
            }
        }

        @Override
        public void onProviderDisabled(String arg0) {
            // TODO Auto-generated method stub
            enabledNet = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        }

        @Override
        public void onProviderEnabled(String arg0) {
            // TODO Auto-generated method stub
            enabledNet = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // TODO Auto-generated method stub
            try {
                if (provider.equals(LocationManager.NETWORK_PROVIDER))
                    statusNet = status;
            } catch (Exception e) {
                statusNet = 0;
            }
        }

    };
}
