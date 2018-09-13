package com.example.julian.photogallery;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.nfc.Tag;
import android.os.SystemClock;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class PollService extends IntentService {
    private static final String TAG = "PoolService";
    private static final long POLL_INTERVAL_MS = TimeUnit.MINUTES.toMillis(15);
    public static final String ACTION_SHOW_NOTIFICATION = "com.bignerdranch.android.photogallery.SHOW_NOTIFICATION";
    public static final String PERM_PRIVATE = "com.example.julian.photogallery.PRIVATE";
    public static final String REQUESTT_CODE = "REQUEST_CODE";
    public static final String NOTIFICATION = "NOTIFICATION";

    public static Intent newIntent(Context context) {
        return new Intent(context,PollService.class);
    }

    public PollService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

        if(!isNetworkAvailableAndConected()) {
//            Do the thing:
            String query = QueryPreferences.getStoredQuery(this);
            String lastResultId = QueryPreferences.getLastResultId(this);
            List<GalleryItem> items;

            if(query == null) {
                items = new FlickrFetchr().fetchRecentPhotos();
            }else {
                items = new FlickrFetchr().searchPhotos(query);
            }

            if(items.size() == 0) {
                return;
            }

            String resultId = items.get(0).getId();
            if(resultId.equals(lastResultId)){
                Log.i(TAG,"Got an old resutl: " + resultId);
            }else {
                Log.i(TAG, "Got a new result: " + resultId);

                Resources resources = getResources();
                Intent i = PhotoGalleryActivity.newIntent(this);
                PendingIntent pi = PendingIntent.getActivity(this,0,i,0);
                Notification notification = new NotificationCompat.Builder(this)
                        .setTicker(resources.getString(R.string.new_pictures_text))
                        .setSmallIcon(android.R.drawable.ic_menu_report_image)
                        .setContentTitle(resources.getString(R.string.new_pictures_title))
                        .setContentIntent(pi)
                        .setAutoCancel(true)
                        .build();

                showBackgroundNotification(0,notification);
            }

            QueryPreferences.setLastResultId(this,resultId);
        }

        Log.i(TAG, "Recieved an intent: " + intent);
    }

    private void showBackgroundNotification(int req_code, Notification notification) {
        Intent i = new Intent(ACTION_SHOW_NOTIFICATION);
        i.putExtra(REQUESTT_CODE, req_code);
        i.putExtra(NOTIFICATION, notification);
        sendOrderedBroadcast(i,PERM_PRIVATE, null,null,
                Activity.RESULT_OK,null,null);
    }

    private boolean isNetworkAvailableAndConected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        boolean isNetworkAvailbale = cm.getActiveNetworkInfo() != null;
        boolean isNetworkConnected = isNetworkAvailbale && cm.getActiveNetworkInfo().isConnected();
        return isNetworkConnected;
    }

    public static void setServiceAlarm(Context context,boolean isOn){
        Intent i = PollService.newIntent(context);
        PendingIntent pi = PendingIntent.getService(context,0,i,0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if(isOn) {
            alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(),
                    POLL_INTERVAL_MS,pi);
        } else {
            alarmManager.cancel(pi);
            pi.cancel();
        }

        QueryPreferences.setAlarmOn(context,isOn);

    }

    public static boolean isServiceAlarmOn(Context context) {
        Intent i = PollService.newIntent(context);
        PendingIntent pi = PendingIntent.getService(context,0,i, PendingIntent.FLAG_NO_CREATE);
        return pi!=null;
    }
}
