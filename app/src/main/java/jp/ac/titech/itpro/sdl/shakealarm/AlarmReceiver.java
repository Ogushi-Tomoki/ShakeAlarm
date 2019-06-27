package jp.ac.titech.itpro.sdl.shakealarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        //アラームを受け取って起動するActivityを指定、起動
        Intent notification = new Intent(context, AlarmNortificationActivity.class);

        //画面起動に必要
        notification.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(notification);
    }
}
