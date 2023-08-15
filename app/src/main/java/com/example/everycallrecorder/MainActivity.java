package com.example.everycallrecorder;

import static android.Manifest.permission.READ_PHONE_NUMBERS;
import static android.Manifest.permission.READ_PHONE_STATE;
import static android.Manifest.permission.READ_SMS;
import static android.os.Environment.DIRECTORY_RECORDINGS;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

import static android.os.Environment.getExternalStoragePublicDirectory;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Switch;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.IOException;
import java.util.Date;


public class MainActivity extends AppCompatActivity {
    private static int MICROPHONE_PERMISSION_CODE=200;
    private static final int PERMISSION_REQUEST_CODE = 100;

    private MediaRecorder rec;
    private boolean started;
    private File dir;

    MediaPlayer mediaPlayer;
    MediaRecorder mediaRecorder;
    Switch onoff;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        onoff=(Switch)findViewById(R.id.sw_onoff);
        if (isMicrophonePresent()){
            getMicrophoneAndStoragePermission();
        }

    }


    private boolean isMicrophonePresent(){
        return this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_MICROPHONE);

    }
    private void getMicrophoneAndStoragePermission(){
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)==
                PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.RECORD_AUDIO,WRITE_EXTERNAL_STORAGE},MICROPHONE_PERMISSION_CODE);
        }
    }
    public void onSwitch(View view) throws InterruptedException {
        boolean checked=((Switch)view).isChecked();
        String text;
        Intent intent=new Intent(this,RecordingsService.class);
        Toast.makeText(getApplicationContext(),"pre ifa",Toast.LENGTH_SHORT).show();

        if (checked) {
            text = "Every Call Recorder turned on";
            //startService(intent);

            rec=new MediaRecorder();
            rec.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL);
            rec.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            rec.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            rec.setOutputFile(getRecordingFilePath());
            //dovde znam da je sve okej jer se kreira prazan fajl.
            // umesto  PhoneStateListener, treba implementirati TelephonyCallback jer je ono prvo deprecated...
            //preskoci 1 pasus

            TelephonyManager telephonyManager = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), READ_SMS) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(getApplicationContext(), READ_PHONE_NUMBERS) !=
                            PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(getApplicationContext(),
                    READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this, new String[]{READ_SMS, READ_PHONE_NUMBERS, READ_PHONE_STATE}, PERMISSION_REQUEST_CODE);
            } else {
                Toast.makeText(getApplicationContext(),"IMEI No. ",Toast.LENGTH_SHORT).show();
            }


            telephonyManager.listen(new PhoneStateListener(){
                @Override
                public void onCallStateChanged(int state, String incomingNumber) {
                   // super.onCallStateChanged(state, incomingNumber);

                    if (TelephonyManager.CALL_STATE_IDLE==state && rec==null){
                        rec.stop();
                        rec.release();
                        started=false;
                        rec=null;
                        Toast.makeText(getApplicationContext(),"Call recording stopped", Toast.LENGTH_SHORT).show();

                    } else if (TelephonyManager.CALL_STATE_OFFHOOK==state || TelephonyManager.CALL_STATE_RINGING==state){//zvoni stalno, previse poziva
                        try {
                            rec.prepare();
                            Toast.makeText(getApplicationContext(),"posle prepare", Toast.LENGTH_SHORT).show();
                            rec.start();
                            started=true;
                            Toast.makeText(getApplicationContext(),"Call recording started", Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            Toast.makeText(getApplicationContext(),"A problem happened", Toast.LENGTH_SHORT).show();
                        }
                    }

                }
            },PhoneStateListener.LISTEN_CALL_STATE);
        }else{
            text = "Every Call Recorder turned off";
            stopService(intent);
        }
    }

    public void playRecording(View view) {
        Snackbar snackbar = Snackbar
                .make(view,  "pusta se: "+dir.getPath(),Snackbar.LENGTH_SHORT);
        snackbar.show();
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(getRecordingFilePath());

            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (Exception e){
            e.printStackTrace();
            snackbar = Snackbar
                    .make(view, "Can't play the video", Snackbar.LENGTH_SHORT);
            snackbar.show();
        }
    }
    private String getRecordingFilePath() {
        File dir= getExternalStoragePublicDirectory(DIRECTORY_RECORDINGS);
        Date date= new Date();
        android.text.format.DateFormat df = new android.text.format.DateFormat();
        CharSequence sdf=df.format("MM-dd-yy-hh-mm-ss",date.getTime());
        //dodaj sdf+ ispred stringa
        File file= new File(dir,"rec.mp3");
        if (file.exists()){
            Toast.makeText(getApplicationContext(),"Postoji fajl :)"+file.getPath(), Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(getApplicationContext(),"Ne postoji fajl :(", Toast.LENGTH_SHORT).show();
        }
        return file.getPath();
    }
}