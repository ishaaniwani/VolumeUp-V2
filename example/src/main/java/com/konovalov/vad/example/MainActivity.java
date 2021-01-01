package com.konovalov.vad.example;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.konovalov.vad.example.recorder.VoiceRecorder;
import com.konovalov.vad.VadConfig;
import com.visualizer.amplitude.AudioRecordView;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class MainActivity extends AppCompatActivity implements VoiceRecorder.Listener, View.OnClickListener {

    private VadConfig.SampleRate DEFAULT_SAMPLE_RATE = VadConfig.SampleRate.SAMPLE_RATE_16K;
    private VadConfig.FrameSize DEFAULT_FRAME_SIZE = VadConfig.FrameSize.FRAME_SIZE_480;
    private VadConfig.Mode DEFAULT_MODE = VadConfig.Mode.VERY_AGGRESSIVE;
    private int DEFAULT_SILENCE_DURATION = 100;
    private int DEFAULT_VOICE_DURATION = 400;

    private FloatingActionButton recordingActionButton;
    private TextView speechTextView;
    private TextView amplitudeTextView;
    private TextView headsetTextView;

    private VoiceRecorder recorder;
    private BroadcastReceiver broadcastReceiver;
    private VadConfig config;
    private boolean isRecording = false;
    private boolean isHeadsetPluggedIn = false;

    private AudioRecordView audioRecordView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        config = VadConfig.newBuilder()
                .setSampleRate(DEFAULT_SAMPLE_RATE)
                .setFrameSize(DEFAULT_FRAME_SIZE)
                .setMode(DEFAULT_MODE)
                .setSilenceDurationMillis(DEFAULT_SILENCE_DURATION)
                .setVoiceDurationMillis(DEFAULT_VOICE_DURATION)
                .build();

        recorder = new VoiceRecorder(this, config);
        audioRecordView = findViewById(R.id.audioRecordView);

        speechTextView = findViewById(R.id.speechTextView);
        amplitudeTextView = findViewById(R.id.amplitude);
        headsetTextView = findViewById(R.id.headset);

        recordingActionButton = findViewById(R.id.recordingActionButton);
        recordingActionButton.setOnClickListener(this);
        recordingActionButton.setEnabled(false);

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();
                int iii;
                if (Intent.ACTION_HEADSET_PLUG.equals(action)) {
                    iii = intent.getIntExtra("state", -1);
                    if (iii == 0) {
                        isHeadsetPluggedIn = false;
                        headsetTextView.setText("microphone not plugged in");
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Intent popup = new Intent(getApplicationContext(), Pop.class);
                                startActivity(new Intent(popup));
                            }
                        }, 5000);
                    }
                    if (iii == 1) {
                        isHeadsetPluggedIn = true;
                        headsetTextView.setText("microphone plugged in");
                    }
                }
            }
        };
        IntentFilter receiverFilter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        registerReceiver(broadcastReceiver, receiverFilter);

        MainActivityPermissionsDispatcher.activateAudioPermissionWithPermissionCheck(this);
    }

    private void startRecording() {
        isRecording = true;
        recorder.start();
        recordingActionButton.setImageResource(R.drawable.stop);
    }

    private void stopRecording() {
        isRecording = false;
        recorder.stop();
        recordingActionButton.setImageResource(R.drawable.red_dot);
        audioRecordView.recreate();
    }

    @NeedsPermission(Manifest.permission.RECORD_AUDIO)
    public void activateAudioPermission() {
        recordingActionButton.setEnabled(true);
    }

    @Override
    public void onClick(View v) {
        if (!isRecording) {
            startRecording();
        } else {
            stopRecording();
        }
    }

    @Override
    public void onSpeechDetected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                speechTextView.setText(R.string.speech_detected);
                int amp = recorder.getAmplitude();
                amplitudeTextView.setText("" + amp);
                audioRecordView.update(amp);

            }
        });
    }

    @Override
    public void onNoiseDetected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                speechTextView.setText(R.string.noise_detected);
                int amp = recorder.getAmplitude();
                amplitudeTextView.setText("" + amp);
                audioRecordView.update(amp);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // NOTE: delegate the permission handling to generated method
        MainActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }
}
