package jp.ac.titech.itpro.sdl.shakealarm;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.TimePicker;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = MainActivity.class.getSimpleName();
    private MyAlarmManager myAlarmManager;
    private TextView setTimeText;
    private SimpleDateFormat dataFormat = new SimpleDateFormat("HH:mm", Locale.US);

    public int setHour;
    public int setMinute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Context context = getApplicationContext();
//        Toast.makeText(context, "Hello, World!", Toast.LENGTH_LONG).show();
        myAlarmManager = new MyAlarmManager(this);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        setHour = sharedPreferences.getInt("SETTIMER_HOUR", 0);
        setMinute = sharedPreferences.getInt("SETTIMER_MINUTE", 0);

        //保存していた時刻の呼び出し
        setTimeText = findViewById(R.id.setting_time);
        setTimeText.setText(String.format("%02d : %02d", setHour, setMinute));
    }

    public void settingTimer(View v) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        TimePickerDialog dialog = new TimePickerDialog(
                this,
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        Log.d("setting time is ", String.format("%02d:%02d", hourOfDay, minute));
                        myAlarmManager.addAlarm(hourOfDay, minute);

                        setHour = hourOfDay;
                        setMinute = minute;

                        setTimeTextBox();

                    }
                },
                hour,
                minute,
                true
        );
        dialog.show();
    }

    public void nowTimer(View v) {
        Intent intent = new Intent(this, MyAlarmService.class);
        startService(intent);
    }

    public void setTimeTextBox() {
        //セットした時刻を保存しておく
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("SETTIMER_HOUR", setHour);
        editor.putInt("SETTIMER_MINUTE", setMinute);
        editor.commit();

        setTimeText.setText(String.format("%02d : %02d", setHour, setMinute));
    }

}
