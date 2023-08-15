package com.example.everycallrecorder;

import static android.Manifest.permission.READ_SMS;
import static android.os.Environment.*;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import static android.Manifest.permission.READ_PHONE_NUMBERS;
import static android.Manifest.permission.READ_PHONE_STATE;
import static android.Manifest.permission.READ_SMS;
public class RecordingsService extends Service {


    private static final int PERMISSION_REQUEST_CODE = 100;
    private MediaRecorder rec;
    private boolean started;
    private File dir;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Toast.makeText(getApplicationContext(),"zapocne recordings service",Toast.LENGTH_SHORT);

        rec=new MediaRecorder();
        rec.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL);
        rec.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        rec.setOutputFile(getRecordingFilePath());
        rec.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        TelephonyManager telephonyManager = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), READ_SMS) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(getApplicationContext(), READ_PHONE_NUMBERS) !=
                        PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(getApplicationContext(),
                READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) getApplicationContext(), new String[]{READ_SMS, READ_PHONE_NUMBERS, READ_PHONE_STATE}, PERMISSION_REQUEST_CODE);
        } else {
            Toast.makeText(getApplicationContext(),"IMEI No. "+telephonyManager.getLine1Number(),Toast.LENGTH_SHORT);
        }

        telephonyManager.listen(new PhoneStateListener(){
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                super.onCallStateChanged(state, incomingNumber);

                if (TelephonyManager.CALL_STATE_IDLE==state && rec==null){
                    rec.stop();
                    rec.release();
                    started=false;
                    rec=null;
                    Toast.makeText(getApplicationContext(),"Call recording stopped", Toast.LENGTH_SHORT).show();

                } else if (TelephonyManager.CALL_STATE_OFFHOOK==state || TelephonyManager.CALL_STATE_RINGING==state){//zvoni stalno, previse poziva
                    try {
                        rec.prepare();
                        rec.start();
                        started=true;
                        Toast.makeText(getApplicationContext(),"Call recording started", Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        Toast.makeText(getApplicationContext(),"A problem happened", Toast.LENGTH_SHORT).show();
                    }
                }

            }
        },PhoneStateListener.LISTEN_CALL_STATE);
        return Service.START_STICKY;//ako dodje do problema zbog memorije, ponovo ce se ovo pokrenuti
    }
    private String getRecordingFilePath() {
        dir= getExternalStoragePublicDirectory(DIRECTORY_RECORDINGS);
        Date date= new Date();
        android.text.format.DateFormat df = new android.text.format.DateFormat();
        CharSequence sdf=df.format("MM-dd-yy-hh-mm-ss",date.getTime());
        //dodaj sdf+ ispred stringa
        File file= new File(dir,"rec.mp3");
        Toast.makeText(getApplicationContext(),"snima se: "+file.getPath(),Toast.LENGTH_SHORT);
        return file.getPath();
    }

}
