package org.linphone;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import java.util.Calendar;

import static android.content.Intent.ACTION_MAIN;

/**
 * Created by QuocVietDang1 on 3/18/2018.
 */

public class StartServiceReceiver extends BroadcastReceiver {

    private String TAG = "StartServiceReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive: ");
        final Handler h = new Handler();
        final Context contextCopy = context;
        Handler mHandler= new Handler();
//        mHandler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//
//                if (!LinphoneService.isReady()) {
//                    contextCopy.startService(new Intent(ACTION_MAIN).setClass(contextCopy, LinphoneService.class));
//
//                }else {
//                    Intent startService = new Intent("com.example.helloandroid.alarms");
//                    contextCopy.sendBroadcast(startService);
//                }
//            }
//        },10000);
        if (!LinphoneService.isReady()) {
            contextCopy.startService(new Intent(ACTION_MAIN).setClass(contextCopy, LinphoneService.class));
        }



//        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
//        Intent intent123 = new Intent(context, StartServiceReceiver.class);
//        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0,
//                intent123, PendingIntent.FLAG_UPDATE_CURRENT);
//        Calendar calendar = Calendar.getInstance();
//        calendar.setTimeInMillis(System.currentTimeMillis());
//        calendar.add(Calendar.SECOND, 5);
//        alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP,
//                calendar.getTimeInMillis(), 2000, pendingIntent);
//        Intent startService = new Intent("com.example.helloandroid.alarms");
//        context.sendBroadcast(startService);

    }

}
