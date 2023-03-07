/*
 *  Created by https://github.com/braver-tool on 05/05/22, 12:10 PM
 *  Copyright (c) 2022 . All rights reserved.
 */

package com.braver.wear.android

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.braver.wear.android.databinding.ActivityWearBinding
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GooglePlayServicesUtil
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.wearable.DataApi
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import java.util.*

class WearActivity : Activity(), GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener {
    private val TAG: String = "###Braver_Wear_Side"
    private lateinit var mainBinding: ActivityWearBinding
    private var mGoogleApiClient: GoogleApiClient? = null

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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainBinding = ActivityWearBinding.inflate(layoutInflater)
        setContentView(mainBinding.root)
        mainBinding.sendDataButton.setOnClickListener { v ->
           // sendRandomMessageToMobileApp()
            val notification = generateNotification(
               "Hello"
            )
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onStart() {
        super.onStart()
        if (mGoogleApiClient == null) {
            mGoogleApiClient = GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build()
        }
        mGoogleApiClient!!.connect()
    }

    override fun onStop() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient!!.disconnect()
        }
        super.onStop()
    }

    override fun onConnectionFailed(result: ConnectionResult) {
        Log.e(TAG, "----Status------->onConnectionFailed")
        if (!result.hasResolution()) {
            // Show a localized error dialog.
            GooglePlayServicesUtil.getErrorDialog(
                result.errorCode, this, 0
            ) { retryConnecting() }!!.show()
        }

    }

    override fun onConnected(p0: Bundle?) {
        Log.e(TAG, "----Status------->onConnected successfully")
        val intent = Intent(this@WearActivity, MobileDataListenerService::class.java)
        startService(intent)
    }

    override fun onConnectionSuspended(p0: Int) {
        Log.e(TAG, "----Status------->ConnectionSuspended")
        retryConnecting()
    }

    private fun sendRandomMessageToMobileApp() {
        val wearAvailable = mGoogleApiClient!!.hasConnectedApi(Wearable.API)
        Log.i(TAG, "----hasConnectedApi------->$wearAvailable")
        val dataMapRequest = PutDataMapRequest.create(PATH_FOR_MOBILE)
        val map = dataMapRequest.dataMap
        //map.putString(EXTRA_MESSAGE_FROM_WEAR, getRandomString())
        map.putString(EXTRA_MESSAGE_FROM_WEAR, mainBinding.screenTitle.text.toString().trim())
        map.putLong(EXTRA_CURRENT_TIME, Date().time)
        val putDataRequest = dataMapRequest.asPutDataRequest()
        putDataRequest.setUrgent()
        Wearable.DataApi.putDataItem(mGoogleApiClient!!, putDataRequest)
            .setResultCallback { dataItemResult: DataApi.DataItemResult ->
                if (dataItemResult.status.isSuccess) {
                    Log.i(TAG, "----sendRandomMessageToMobileApp------->Successfully!!")
                } else {
                    Log.i(TAG, "----sendRandomMessageToMobileApp------->Failed!!")
                }
            }
    }

    private fun retryConnecting() {
        if (!mGoogleApiClient!!.isConnecting) {
            mGoogleApiClient!!.connect()
        }
    }

    private fun getRandomString(): String {
        val random = Random()
        val sb = StringBuilder(15)
        for (i in 0 until 15)
            sb.append(ALLOWED_CHARACTERS[random.nextInt(ALLOWED_CHARACTERS.length)])
        return sb.toString()
    }

    private lateinit var notificationManager: NotificationManager
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

        // 4. Build and issue the notification.
        val notificationCompatBuilder =
            NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)

        // TODO: Review Notification builder code.
        val notificationBuilder = notificationCompatBuilder
            .setStyle(bigTextStyle)
            .setContentTitle(titleText)
            .setContentText(mainText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            // Makes Notification an Ongoing Notification (a Notification with a background task).
            .setOngoing(true)
            // For an Ongoing Activity, used to decide priority on the watch face.
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        // TODO: Create an Ongoing Activity.
        return notificationBuilder.build()
    }



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
            notificationManager.notify(10, notificationBuilder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}