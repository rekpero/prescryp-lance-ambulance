package com.prescryp.lance.ambulance.Misc;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.prescryp.lance.ambulance.R;

public class NotificationHelper extends ContextWrapper {

    private static final String CHANNEL_ID = "com.prescryp.lance.NOTIFICATION";
    private static final String CHANNEL_NAME = "Prescryp Ambulance";

    private NotificationManager manager;
    private Context context;

    public NotificationHelper(Context context) {
        super(context);
        this.context = context;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            createChannels();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createChannels() {
        NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
        notificationChannel.enableLights(true);
        notificationChannel.enableVibration(true);
        notificationChannel.setLightColor(Color.GRAY);
        notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        
        getManager().createNotificationChannel(notificationChannel);
    }

    public NotificationManager getManager() {
        if (manager == null){
            manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return manager;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public Notification.Builder getCustomerNotification(String title, String content, PendingIntent contentIntent, Uri soundUri){
        return new Notification.Builder(getApplicationContext(), CHANNEL_ID)
                .setContentText(content)
                .setContentTitle(title)
                .setAutoCancel(true)
                .setSound(soundUri)
                .setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.logo_lance)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.logo_lance));
    }
}
