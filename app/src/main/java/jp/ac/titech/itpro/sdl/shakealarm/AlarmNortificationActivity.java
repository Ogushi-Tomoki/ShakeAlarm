package jp.ac.titech.itpro.sdl.shakealarm;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.text.SimpleDateFormat;

public class AlarmNortificationActivity extends AppCompatActivity implements SensorEventListener, Runnable {
    //タイマー関連
    private Handler timerhandler = new Handler();
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            count ++;
            timerText.setText(dataFormat.format((shakeTime * 10 - count)*period));
            timerhandler.postDelayed(this, period);
        }
    };
    private TextView timerText;
    private SimpleDateFormat dataFormat = new SimpleDateFormat("mm:ss.S", Locale.US);
    private int count, period;

    //シェイクゲージ
    private ProgressBar bar;

    //mp3音源
    private MediaPlayer mp;

    //加速度表示
    private TextView infoView;
    private TextView timeView;

    //タイマーを止めるのに振り続けなければいけない時間（秒）
    private static final int shakeTime = 6;

    //センサー関連
    private final static long GRAPH_REFRESH_PERIOD_MS = 20;
    private static final float ALPHA = 0.75f;

    private SensorManager manager;
    private Sensor sensor;

    private final Handler handler = new Handler();
    private final Timer timer = new Timer();

    private float gx, gy, gz;
    private float rx, ry, rz;
    private float vx, vy, vz;

    private final int N = 25;
    private float[] ax = new float[N];
    private float[] ay = new float[N];
    private float[] az = new float[N];
    private int index = 0;
    private float sx, sy, sz;
    private float wx, wy, wz;

    private int rate;
    private int accuracy;
    private double weighted_acceleration;
    private double moving_acceleration;
    private double accelerationMAX;
    private long prevTimestamp;

    //タイマーが起動しているか否か
    private boolean shakeflag;

    private int delay = SensorManager.SENSOR_DELAY_NORMAL;
    private int type = Sensor.TYPE_ACCELEROMETER;

    private final static String TAG = AlarmNortificationActivity.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate in" + Thread.currentThread());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm);

        count = 0;
        period = 100;
        shakeflag = false;

        infoView = findViewById(R.id.info_view);
        timerText = findViewById(R.id.timer);
        timerText.setText(dataFormat.format(shakeTime * 10 * period));

        timeView = findViewById(R.id.time_view);
        timeView.setText(getString(R.string.time_format, shakeTime));

        bar = findViewById(R.id.progressBar);
        bar.setMax(100);
        bar.setProgress(0);

        //スクリーンロックを解除する
        //権限が必要
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //加速度計の用意
        manager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (manager == null) {
//            Toast.makeText(this, R.string.toast_no_sensor_manager, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        sensor = manager.getDefaultSensor(type);
        if (sensor == null) {
//            String text = getString(R.string.toast_no_sensor_available, sensorTypeName(type));
//            Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public void onStart() {
        Log.d(TAG, "onStart in" + Thread.currentThread());
        super.onStart();
        Toast.makeText(getApplicationContext(), "アラーム！", Toast.LENGTH_LONG).show();

        //音を鳴らす
        if (mp == null) {
            //resのrawディレクトリにtest.mp3を置いてある
            mp = MediaPlayer.create(this, R.raw.test);

            //ループ設定
            mp.setLooping(true);
        }

        mp.start();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy in" + Thread.currentThread());
        super.onDestroy();
        stopAndRelease();
    }

    private void stopAndRelease() {
        if (mp != null) {
            mp.stop();
            mp.release();
        }
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume in" + Thread.currentThread());
        super.onResume();
//        alarmNowText = (TextView) findViewById(R.id.alarm_now_time);
//        handler.sendEmptyMessage(WHAT);
        // mam.stopAlarm();
        manager.registerListener(this, sensor, delay);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                handler.post(AlarmNortificationActivity.this);
            }
        }, 0, GRAPH_REFRESH_PERIOD_MS);
    }

//    @Override
//    protected void onPause() {
//        super.onPause();
//        manager.unregisterListener(this);
//    }

    public void stopAlarm(View v) {
        initSetting();
        stopAndRelease();
        Intent intent = new Intent(AlarmNortificationActivity.this, MainActivity.class);
        startActivity(intent);
    }

//    public void settingTimer(View v) {
//        Calendar calendar = Calendar.getInstance();
//        int hour = calendar.get(Calendar.HOUR_OF_DAY);
//        int minute = calendar.get(Calendar.MINUTE);
//        TimePickerDialog dialog = new TimePickerDialog(
//                this,
//                new TimePickerDialog.OnTimeSetListener() {
//                    @Override
//                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
//                        Log.d("setting time is ", String.format("%02d:%02d", hourOfDay, minute));
//                        myAlarmManager.addAlarm(hourOfDay, minute);
//                    }
//                },
//                hour,
//                minute,
//                true
//        );
//        dialog.show();
//    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        //重力加速度の除去
        gx = ALPHA * gx + (1 - ALPHA) * event.values[0];
        gy = ALPHA * gy + (1 - ALPHA) * event.values[1];
        gz = ALPHA * gz + (1 - ALPHA) * event.values[2];

        rx = event.values[0] - gx;
        ry = event.values[1] - gy;
        rz = event.values[2] - gz;
        Log.i(TAG, "x=" + rx + ", y=" + ry + ", z=" + rz);

        //移動平均の計算
        sx = sx - ax[index] + rx;
        ax[index] = rx;
        wx = sx / N;

        sy = sy - ay[index] + ry;
        ay[index] = ry;
        wy = sy / N;

        sz = sz - az[index] + rz;
        az[index] = rz;
        wz = sz / N;

        index = (index + 1) % N;

        //重み付き平均の計算
        vx = ALPHA * vx + (1 - ALPHA) * rx;
        vy = ALPHA * vy + (1 - ALPHA) * ry;
        vz = ALPHA * vz + (1 - ALPHA) * rz;
        long ts = event.timestamp;
        rate = (int) (ts - prevTimestamp) / 1000;
        prevTimestamp = ts;

        //android端末の加速度の計算、最大加速度の記憶
        weighted_acceleration = Math.sqrt(vx * vx + vy * vy + vz * vz) * 100;
        moving_acceleration = Math.sqrt(wx * wx + wy * wy + wz * wz) * 100;

        if(weighted_acceleration > accelerationMAX) {
            accelerationMAX = weighted_acceleration;
        }

        //プログレスバーを更新
        if(moving_acceleration > 500) {
            bar.setProgress(100);
        } else if(moving_acceleration > 100) {
            bar.setProgress(50 + (int) (moving_acceleration - 100)/8);
        } else if(moving_acceleration > 30) {
            bar.setProgress(25 + (int)(moving_acceleration - 30) * 25 / 70);
        } else {
            bar.setProgress((int)(moving_acceleration * 25 / 30));
        }

        //重み付き平均加速度が500を超えたらタイマー起動
        if(weighted_acceleration >= 400 && moving_acceleration >= 100 && !shakeflag) {
//            Toast.makeText(getApplicationContext(), "タイマー始動！", Toast.LENGTH_LONG).show();
            timerhandler.post(runnable);
            shakeflag = true;
            //移動平均加速度が100を下回ったらタイマー停止
        } else if(moving_acceleration < 30 && shakeflag) {
//            Toast.makeText(getApplicationContext(), "残念！もう1回！", Toast.LENGTH_LONG).show();
            timerhandler.removeCallbacks(runnable);
            timerText.setText(dataFormat.format(shakeTime * 10 * period));
            count = 0;
            shakeflag = false;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.i(TAG, "onAccuracyChanged");
        this.accuracy = accuracy;
    }

    @Override
    public void run() {
        infoView.setText(getString(R.string.info_format, moving_acceleration));

        //タイマーが6秒を超えたら
        if(count >= shakeTime * 10) {
            Toast.makeText(getApplicationContext(), "アラーム終了！", Toast.LENGTH_LONG).show();

            //mp音楽の停止
            mp.stop();

            //設定を初期化する
            initSetting();

            //300ms停止
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Toast.makeText(this, "take an error!!", Toast.LENGTH_LONG).show();
            }

            //設定画面に移る
            stopAndRelease();
            Intent intent = new Intent(AlarmNortificationActivity.this, MainActivity.class);
            startActivity(intent);
        }
    }

    public void initSetting() {
        //センサーの停止
        manager.unregisterListener(this);

        //各フィールド変数を初期化
        count = 0;
        weighted_acceleration = 0;
        moving_acceleration = 0;
        accelerationMAX = 0;
        shakeflag = false;

        //タイマーを初期化
        timerhandler.removeCallbacks(runnable);
        timerText.setText(dataFormat.format(0));
    }
}
