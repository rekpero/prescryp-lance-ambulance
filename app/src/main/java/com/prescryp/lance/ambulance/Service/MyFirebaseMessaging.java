package com.prescryp.lance.ambulance.Service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;
import com.prescryp.lance.ambulance.CustomerCallActivity;
import com.prescryp.lance.ambulance.Misc.NotificationHelper;
import com.prescryp.lance.ambulance.R;

public class MyFirebaseMessaging extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        String messageTitle = remoteMessage.getNotification().getTitle();
        String messageBode = remoteMessage.getNotification().getBody();
        String click_action = remoteMessage.getNotification().getClickAction();

        String dataCustomerId = remoteMessage.getData().get("from_customer_id");
        String dataCustomerPrice = remoteMessage.getData().get("customer_price");


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            showNotificationAPI26(messageTitle, messageBode, click_action, dataCustomerId, dataCustomerPrice);
        }else {
            showNotification(messageTitle, messageBode, click_action, dataCustomerId, dataCustomerPrice);
        }

        Intent i = new Intent("android.intent.action.NEWRIDE");
        i.putExtra("customerId", dataCustomerId);
        i.putExtra("ridePrice", dataCustomerPrice);
        this.sendBroadcast(i);

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void showNotificationAPI26(String messageTitle, String messageBode, String click_action, String dataCustomerId, String dataCustomerPrice) {
        Intent resultIntent = new Intent(click_action);
        resultIntent.putExtra("rideId", dataCustomerId);
        resultIntent.putExtra("ridePrice", dataCustomerPrice);
        PendingIntent contentIntent = PendingIntent.getActivity(getBaseContext(), 0, resultIntent, PendingIntent.FLAG_ONE_SHOT);
        Uri defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationHelper notificationHelper = new NotificationHelper((getBaseContext()));
        Notification.Builder builder = notificationHelper.getCustomerNotification(messageTitle, messageBode, contentIntent, defaultSound);

        int mNotificationId = (int) System.currentTimeMillis();
        notificationHelper.getManager().notify(mNotificationId, builder.build());
    }

    private void showNotification(String messageTitle, String messageBode, String click_action, String dataRideId, String dataCustomerPrice) {
        Uri defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this, getString(R.string.default_notification_channel_id))
                        .setContentText(messageBode)
                        .setContentTitle(messageTitle)
                        .setAutoCancel(true)
                        .setSound(defaultSound)
                        .setSmallIcon(R.drawable.logo_lance)
                        .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.logo_lance));
        Intent resultIntent = new Intent(click_action);
        resultIntent.putExtra("rideId", dataRideId);
        resultIntent.putExtra("ridePrice", dataCustomerPrice);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder.setContentIntent(resultPendingIntent);

        int mNotificationId = (int) System.currentTimeMillis();

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        mNotificationManager.notify(mNotificationId, mBuilder.build());

    }
}

