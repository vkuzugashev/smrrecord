/**
 * Created by penart on 04.10.2016.
 * Работа с линией, запись звонков в БД
 */
package ru.itsps.smrecord.smrecord;

    import java.io.File;
    import java.text.SimpleDateFormat;
    import java.util.Calendar;
    import java.util.Date;
    import java.util.Timer;
    import java.util.TimerTask;
    import java.util.Vector;

    import android.app.Service;
    import android.content.ContentValues;
    import android.content.Context;
    import android.content.Intent;
    import android.content.SharedPreferences;
    import android.database.Cursor;
    import android.database.DatabaseUtils;
    import android.database.sqlite.SQLiteDatabase;
    import android.media.MediaRecorder;
    import android.os.Binder;
    import android.os.Environment;
    import android.os.IBinder;
    import android.support.annotation.NonNull;
    import android.telephony.PhoneStateListener;
    import android.telephony.TelephonyManager;
    import android.util.Log;
    import okhttp3.MediaType;
    import okhttp3.MultipartBody;
    import okhttp3.RequestBody;
    import okhttp3.ResponseBody;
    import retrofit2.Call;
    import retrofit2.Response;
    import retrofit2.Retrofit;
    import android.media.CamcorderProfile;
    import android.hardware.Camera;


public class SmRecordService extends Service {

    public static final String MULTIPART_FORM_DATA = "multipart/form-data";

    private TelephonyManager telephonyManager;
    private SmRecordPhoneStateListener listenPhone;
    private SmRecordBinder binder = new SmRecordBinder();
    private RecordStoreContract contract;
    private Camera camera;
    private SQLiteDatabase db;
    private Timer timer;
    private TimerTask tTask;
    private Timer timer2;
    private TimerTask tTask2;
    private Timer timer3;
    private TimerTask tTask3;
    private MediaRecorder recorder;
    private MediaRecorder recorder2;
    private MediaRecorder recorder3;
    private long interval = 60000;
    private long interval2 = 60000;
    private long interval3 = 60000;
    private boolean IsRecordingMIC = false;
    private boolean IsRecordingVideo = false;
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
    //private static int slot;


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

        //Вытащим номер телефона из локального словаря
        SharedPreferences sharedPreferences = this.getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        phone1 = sharedPreferences.getString(getString(R.string.phone),"89XXXXXXXXX");

        audioSource = sharedPreferences.getString("audioSource","MIC");

        //slot = 0;
        listenPhone = new SmRecordPhoneStateListener();
        //таймер отправки записей
        timer = new Timer(true);
        //таймер записи по расписанию
        timer2 = new Timer(true);
        timer3 = new Timer(true);

        //Видео
        contract = new RecordStoreContract();
        RecordStoreContract.RecordStoreDbHelper mDbHelper = contract.getRecordStoreDbHelper(getApplicationContext());
        db = mDbHelper.getWritableDatabase();
        dbStatus="Ok";
        //mDbHelper.ClearDb(db);
        Log.d(TAG,"onCreate");
    }

    /**
     * Отправка сообщений через каждые Interval милисекунд
     */
    void Schedule() {
        if (tTask != null)
            tTask.cancel();
        if (interval > 0) {
            tTask = new TimerTask() {
                public void run() {
                    SendRecords();
                }
            };
            timer.schedule(tTask, interval, interval);
        }

        if (tTask2 != null)
            tTask2.cancel();
        if (interval2 > 0) {
            tTask2 = new TimerTask() {
                public void run() {
                    RecordMIC();
                }
            };
            timer2.schedule(tTask2, interval2, interval2);
        }

        /*if (tTask3 != null)
            tTask3.cancel();
        if (interval3 > 0) {
            tTask3 = new TimerTask() {
                public void run() {
                    RecordVideo();
                }
            };
            timer3.schedule(tTask3, interval3, interval3);
        }*/
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        telephonyManager.listen(listenPhone, PhoneStateListener.LISTEN_NONE);
        tTask.cancel();
        tTask2.cancel();
        tTask3.cancel();
        timer.cancel();
        timer2.cancel();
        timer3.cancel();
        super.onDestroy();
        Log.d(TAG,"onDestroy");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // TODO Auto-generated method stub
        try {
            telephonyManager.listen(listenPhone, PhoneStateListener.LISTEN_CALL_STATE);
        } catch (Exception e) {
            Log.w(TAG,e.getMessage());
        }
        serviceStatus = "running";
        // Запустим таймер отправки сообщений
        Schedule();
        Log.d(TAG,"onStartCommand");

        return super.onStartCommand(intent, flags, startId);
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
                        StartRecording(call_record_file);
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
                        StartRecording(call_record_file);
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
                        StopRecording();
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


    private void StartRecording(String record_file){
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

    private void StartRecording2(String record_file){
        Log.d(this.getClass().getName(),record_file);
        try {
            recorder2 = new MediaRecorder();
            if("MIC".equals(audioSource))
                recorder2.setAudioSource(MediaRecorder.AudioSource.MIC);
            else
                recorder2.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL);
            recorder2.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder2.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            //recorder.setMaxDuration(3600000);
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


    private void StopRecording(){
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

    private void StopRecording2(){
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

    private void StartRecordingVideo(String record_file){
        Log.d(this.getClass().getName(),record_file);
        try {
            //camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
            camera = Camera.open();
            recorder3 = new MediaRecorder();

            // Step 1: Unlock and set camera to MediaRecorder
            camera.unlock();
            recorder3.setCamera(camera);

            // Step 2: Set sources
            recorder3.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
            recorder3.setVideoSource(MediaRecorder.VideoSource.CAMERA);

            // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
            recorder3.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_LOW));
            //recorder3.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            //recorder3.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            //recorder3.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);
            //recorder3.setVideoFrameRate(15);
            recorder3.setMaxDuration(60000);
            recorder3.setOutputFile(record_file);
            recorder3.prepare();
            recorder3.start();   // Recording is now started
        } catch(Exception e){
            //recorder3.stop();
            recorder3.release();
            recorder3 = null;
            camera.lock();
            camera.release();
            errorMessage = "Error record, "+e.getMessage();
            Log.w(this.getClass().getName(),  errorMessage);
            e.printStackTrace();
        }
        Log.i(this.getClass().getName(), "Start Recording");
    }

    private void StopRecordingVideo(){
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
                camera.lock();
                camera.release();
                camera = null;
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
            String server = sharedPreferences.getString(getString(R.string.server),"smr.biws.ru");
            if("".equals(server)) {
                Log.i(TAG,"Server not defined!");
                return;
            }
/*
            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(AuthScope.ANY, new NTCredentials(
                    (String) params[1]));
            hc.setCredentialsProvider(credsProvider);
             */


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

            Cursor cur = db.query(
                    RecordStoreContract.Record.TABLE_NAME,
                    columns,
                    null,
                    null,
                    null,
                    null,
                    RecordStoreContract.Record.COLUMN_NAME_STARTDT + " ASC",
                    "10"
            );

            if (cur == null || !cur.moveToFirst()) {
                db.close();
                return;
            }
            Vector<RecordRow> rows = new Vector<RecordRow>();
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
            cur.close();

            //Отправим данные на сервер
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("http://"+server+"/smrecord/")
                    .build();
            SmRecordHttpAPI service = retrofit.create(SmRecordHttpAPI.class);

            //Отправим на сервер данные
            for (RecordRow row : rows) {
                Log.w(TAG,String.format("phone1=%s, startdt=%s, stopdt=%s, direction=%d, unanswered=%d, duration=%d, remote_phone=%s, record_file=%s",
                        phone1,
                        row.getCall_start(),
                        row.getCall_stop(),
                        row.getCall_direction(),
                        row.getCall_unanswered(),
                        row.getCall_duration(),
                        row.getCall_remote_phone(),
                        row.getCall_record_file()
                ));
                Call<ResponseBody> call;
                File file = new File(row.getCall_record_file()==null?"":row.getCall_record_file());
                if(file.exists()) {
                    Log.d(TAG,"service.SaveRecord2");
                    //todo сделать обрезание пути у файла записи
                    call = service.SaveRecord2(
                            createPartFromString(row.getCall_start()),
                            createPartFromString(row.getCall_stop() == null ? "" : row.getCall_stop()),
                            createPartFromString(Integer.toString(row.getCall_direction())),
                            createPartFromString(Integer.toString(row.getCall_duration())),
                            createPartFromString(Integer.toString(row.getCall_unanswered())),
                            createPartFromString(phone1 == null ? "" : phone1),
                            createPartFromString(row.getCall_remote_phone() == null ? "" : row.getCall_remote_phone()),
                            createPartFromString(row.getCall_record_file() == null ? "" : row.getCall_record_file()),
                            createPartFromFile(row.getCall_record_file(), file));
                }
                else{
                    Log.d(TAG,"service.SaveRecord");
                    call = service.SaveRecord(
                            row.getCall_start(),
                            row.getCall_stop(),
                            row.getCall_direction(),
                            row.getCall_duration(),
                            row.getCall_unanswered(),
                            phone1,
                            (row.getCall_remote_phone()==null ? "" : row.getCall_remote_phone()),
                            row.getCall_record_file());
                }
                Log.d(TAG,"End SaveRecord");

                if(call == null)
                    Log.w(TAG,"call=null");
                else {
                    Response<ResponseBody> response = call.execute();
                    if (response == null)
                        Log.w(TAG, "response=null");
                    else {
                        Log.d(TAG, response.toString());
                        if (response.isSuccessful() && response.code()==200) {
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
                            Log.w(TAG, "Upload error: " + response.raw().toString());
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error Transmit");
            e.printStackTrace();
        }
        db.close();
    }

    //Запись с микрофона по расписанию
    private void RecordMIC() {

        if(!IsRecordingMIC) {
            call_start2 = new Date();
            int hour = call_start2.getHours();
            if(hour >= 8 && hour <= 16) {
                SimpleDateFormat sdtf = new SimpleDateFormat("yyyyMMddHHmmss");
                call_record_file2 = String.format("%s/%s-%s-%s-%d.3gp", getFullSdPath(), sdtf.format(call_start2), phone1, "MIC", 1);
                //Начать запись
                StartRecording2(call_record_file2);
                if(recorder2!=null)
                    IsRecordingMIC = true;
            }
        } else {
            call_stop2 = new Date();
            call_duration2 = (int)(call_stop2.getTime() - call_start2.getTime());
            if(call_duration2 >= 300000) {
                SimpleDateFormat sdtf = new SimpleDateFormat("yyyyMMddHHmmss");
                //Остановить запись
                StopRecording2();
                if(recorder2==null){
                    //сохранить в БД
                    ContentValues values = new ContentValues();
                    values.put(RecordStoreContract.Record.COLUMN_NAME_STARTDT,  sdtf.format(call_start2));
                    values.put(RecordStoreContract.Record.COLUMN_NAME_STOPDT,  sdtf.format(call_stop2));
                    values.put(RecordStoreContract.Record.COLUMN_NAME_DURATION,  call_duration2);
                    values.put(RecordStoreContract.Record.COLUMN_NAME_DIRECTION,  1);
                    values.put(RecordStoreContract.Record.COLUMN_NAME_UNANSWERED,  0);
                    values.put(RecordStoreContract.Record.COLUMN_NAME_REMOTE_PHONE,  "MIC");
                    values.put(RecordStoreContract.Record.COLUMN_NAME_RECORD_FILE,  call_record_file2);
                    db.insert(RecordStoreContract.Record.TABLE_NAME, "", values);
                    IsRecordingMIC = false;
                }
            }
        }
    }

    //Запись с микрофона по расписанию
    private void RecordVideo() {

        if(!IsRecordingVideo) {
            call_start3 = new Date();
            int hour = call_start3.getHours();
            if(hour >= 8 && hour <= 23) {
                SimpleDateFormat sdtf = new SimpleDateFormat("yyyyMMddHHmmss");
                call_record_file3 = String.format("%s/%s-%s-%s-%d.3gp", getFullSdPath(), sdtf.format(call_start3), phone1, "CAM", 1);
                //Начать запись
                StartRecordingVideo(call_record_file3);
                if(recorder3!=null)
                    IsRecordingVideo = true;
            }
        } else {
            call_stop3 = new Date();
            call_duration3 = (int)(call_stop3.getTime() - call_start3.getTime());
            if(call_duration3 >= 120000) {
                SimpleDateFormat sdtf = new SimpleDateFormat("yyyyMMddHHmmss");
                //Остановить запись
                StopRecordingVideo();
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
                    IsRecordingVideo = false;
                }
            }
        }
    }

}
