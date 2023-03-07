/*
 *  Created by https://github.com/braver-tool on 05/05/22, 12:10 PM
 *  Copyright (c) 2022 . All rights reserved.
 */

package com.braver.wear.android

import android.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.media.RingtoneManager.getDefaultUri
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.braver.wear.android.WearActivity.Companion.EXTRA_USER_EMAIL
import com.braver.wear.android.WearActivity.Companion.EXTRA_USER_NAME
import com.braver.wear.android.WearActivity.Companion.EXTRA_USER_PHONE
import com.braver.wear.android.WearActivity.Companion.PATH_FOR_WEAR
import com.google.android.gms.common.data.FreezableUtils
import com.google.android.gms.wearable.*


class MobileDataListenerService : WearableListenerService(), DataApi.DataListener {
    var TAG = "MobileDataListenerService"
    private lateinit var notificationManager: NotificationManager
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        super.onDataChanged(dataEvents)
        Log.i(
            "##BTApp-Wear@@$TAG",
            "--------->onDataChanged"
        )
        val events: List<DataEvent> =
            FreezableUtils.freezeIterable(dataEvents)
        dataEvents.close()
        for (event in events) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                val path = event.dataItem.uri.path
                if (PATH_FOR_WEAR == path) {
                    val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
                    val mUserName = dataMapItem.dataMap.getString(EXTRA_USER_NAME)
                    val mUserEmail = dataMapItem.dataMap.getString(EXTRA_USER_EMAIL)
                    val mUserPhone = dataMapItem.dataMap.getString(EXTRA_USER_PHONE)
                    Log.i(
                        "##BTApp-Wear@@$TAG",
                        "----UserName--->$mUserName-----UserEmail--->$mUserEmail---UserPhone--->$mUserPhone"
                    )
                    Toast.makeText(
                        this,
                        "UserName from mobile device is :$mUserName",
                        Toast.LENGTH_SHORT
                    ).show()
                    val notification = generateNotification(
                        "Hello"
                    )
                    notificationManager.notify(NOTIFICATION_ID, notification)
                    notification("Hello","World")
                } else {
                    Log.i(
                        "##BTApp-Wear@@$TAG",
                        "--------->path is none"
                    )
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(
            "##BTApp-Wear@@$TAG",
            "--------->onCreate"
        )
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onMessageReceived(p0: MessageEvent) {
        super.onMessageReceived(p0)
        Log.i(
            "##BTApp-Wear@@$TAG",
            "--------->onMessageReceived"
        )
    }

    /*private fun notification(
        title: String?,
        msg: String?,
    ) {
        val alarmSound: Uri = getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)


        val displayIntent = Intent(this, WearActivity::class.java)
        //displayIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        //displayIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        val displayPendingIntent = PendingIntent.getActivity(
            this,
            0, displayIntent, PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification: Notification = Notification.Builder(this)
            //.setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("WeirdShit")
            .setContentText("some random crap")
            .extend(
                Notification.WearableExtender()
                    .setDisplayIntent(displayPendingIntent)
            )
            .setSound(alarmSound)
            .build()

        val notificationManager = NotificationManagerCompat.from(this)
        notificationManager.notify(25454, notification)
    }*/


    private fun notification(
        title: String?,
        msg: String?,
    ) {
        try {
            //playNotificationSound()
            var pendingIntent: PendingIntent? = null
            val intent = Intent(this, WearActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val CHANNEL_ID = System.currentTimeMillis().toString() // The id of the channel.
            val name: CharSequence =
                getString(com.braver.wear.android.R.string.app_name) // The user-visible name of the channel.
            var importance = 0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                importance = NotificationManager.IMPORTANCE_HIGH
            }
            var mChannel: NotificationChannel? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mChannel = NotificationChannel(CHANNEL_ID, name, importance)
            }
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val notificationBuilder = NotificationCompat.Builder(
                applicationContext, CHANNEL_ID
            )
                //.setSmallIcon(com.braver.wear.android.R.drawable.ic_launcher_background)
                .setContentTitle(title)
                .setStyle(NotificationCompat.BigTextStyle().bigText(msg))
                .setContentText(msg)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                //.setSound(soundUri,audioAttributes)
                //.setSound("android.resource://com.innovative.custompushnotification/"+R.raw.notification_sound)
                //.setSound(Uri.parse("android.resource://com.innovative.custompushnotification/" + R.raw.fluate), AudioManager.STREAM_NOTIFICATION)
                .setContentIntent(pendingIntent)
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                assert(notificationManager != null)
                notificationManager.createNotificationChannel(mChannel!!)
            }
            notificationManager.notify(0, notificationBuilder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    companion object {
        const val PATH_FOR_WEAR = "/path_for_wear"
        const val PATH_FOR_MOBILE = "/path_for_mobile"
        const val EXTRA_CURRENT_TIME = "extra_current_time"
        const val EXTRA_USER_NAME = "extra_user_name"
        const val EXTRA_USER_EMAIL = "extra_user_email"
        const val EXTRA_USER_PHONE = "extra_user_phone"
        const val EXTRA_MESSAGE_FROM_WEAR = "extra_message_from_wear"
        private const val ALLOWED_CHARACTERS = "0123456789QWERTYUIOPASDFGHJKLZXCVBNM"

        private const val TAG = "ForegroundOnlyService"

        private const val THREE_SECONDS_MILLISECONDS = 3000L

        private const val PACKAGE_NAME = "com.android.example.wear.ongoingactivity"

        private const val EXTRA_CANCEL_WORKOUT_FROM_NOTIFICATION =
            "$PACKAGE_NAME.extra.CANCEL_SUBSCRIPTION_FROM_NOTIFICATION"

        private const val NOTIFICATION_ID = 12345678

        private const val NOTIFICATION_CHANNEL_ID = "walking_workout_channel_01"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun generateNotification(mainText: String): Notification {
        Log.d(TAG, "generateNotification()")
        // 0. Get data (note, the main notification text comes from the parameter above).
        val titleText = "HEllo"

        // 1. Create Notification Channel.
        val notificationChannel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID, titleText, NotificationManager.IMPORTANCE_DEFAULT)

        // Adds NotificationChannel to system. Attempting to create an
        // existing notification channel with its original values performs
        // no operation, so it's safe to perform the below sequence.
        notificationManager.createNotificationChannel(notificationChannel)

        // 2. Build the BIG_TEXT_STYLE.
        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText(mainText)
            .setBigContentTitle(titleText)

        // 3. Set up main Intent/Pending Intents for notification.
        val launchActivityIntent = Intent(this, WearActivity::class.java)
        val activityPendingIntent = PendingIntent.getActivity(
            this, 0, launchActivityIntent, 0
        )


        // 4. Build and issue the notification.
        val notificationCompatBuilder =
            NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)

        // TODO: Review Notification builder code.
        val notificationBuilder = notificationCompatBuilder
            .setStyle(bigTextStyle)
            .setContentTitle(titleText)
            .setContentText(mainText)
            .setSmallIcon(com.braver.wear.android.R.mipmap.ic_launcher)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            // Makes Notification an Ongoing Notification (a Notification with a background task).
            .setContentIntent(activityPendingIntent)
            // For an Ongoing Activity, used to decide priority on the watch face.
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        // TODO: Create an Ongoing Activity.
        return notificationBuilder.build()
    }


}
