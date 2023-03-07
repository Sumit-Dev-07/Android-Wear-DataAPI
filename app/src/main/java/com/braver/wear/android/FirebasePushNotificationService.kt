package com.braver.wear.android

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.MutableLiveData
import com.braver.wear.android.databinding.ActivityMainBinding
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GooglePlayServicesUtil
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.wearable.DataApi
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.*


class FirebasePushNotificationService : FirebaseMessagingService(), GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener {


    private val TAG: String = "###Braver_Mobile_Side"
    private var mGoogleApiClient: GoogleApiClient? = null



    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.e("onMessageReceived", "onMessageReceived: " + remoteMessage.data)
        Log.v(
            "new-message>>>>>>",
            "ORIGINAL MESSAGE DATA PAYLOAD NOTIFICATION==>" + remoteMessage.data
        )
        if (mGoogleApiClient == null) {
            mGoogleApiClient = GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build()
        }
        mGoogleApiClient!!.connect()
        wakeUpScreen()
        takeAction(
            "Notification",
            "Message"
        )
    }

    private fun takeAction(title: String, msg: String) {
        if (isAppIsInBackground(applicationContext)) {
            notification(title, msg)
        } else {
            notification(title, msg)
        }
    }

    @SuppressLint("InvalidWakeLockTag")
    private fun wakeUpScreen() {
        val pm = this.getSystemService(POWER_SERVICE) as PowerManager
        val isScreenOn = pm.isScreenOn
        Log.e("screen on......", "" + isScreenOn)
        if (!isScreenOn) {
            val wl = pm.newWakeLock(
                PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
                "MyLock"
            )
            wl.acquire(10000)
            val wl_cpu = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyCpuLock")
            wl_cpu.acquire(10000)
        }
    }

    private fun notification(
        title: String?,
        msg: String?,
    ) {
        try {
            //playNotificationSound()
            var pendingIntent: PendingIntent? = null
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            pendingIntent = PendingIntent.getActivity(
                this,
                j,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val CHANNEL_ID = System.currentTimeMillis().toString() // The id of the channel.
            val name: CharSequence =
                getString(R.string.app_name) // The user-visible name of the channel.
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
                .setSmallIcon(R.drawable.ic_launcher_background)
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
            notificationManager.notify(j, notificationBuilder.build())
            sendDataToWearApp()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * This method is used to app is background
     */
    private fun isAppIsInBackground(context: Context): Boolean {
        var isInBackground = true
        val am = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT_WATCH) {
            val runningProcesses = am.runningAppProcesses
            for (processInfo in runningProcesses) {
                if (processInfo.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    for (activeProcess in processInfo.pkgList) {
                        if (activeProcess == context.packageName) {
                            isInBackground = false
                        }
                    }
                }
            }
        } else {
            val taskInfo = am.getRunningTasks(1)
            val componentInfo = taskInfo[0].topActivity
            if (componentInfo!!.packageName == context.packageName) {
                isInBackground = false
            }
        }
        return isInBackground
    }

    companion object {
        private const val j = 0
    }

    override fun onConnectionFailed(result: ConnectionResult) {
        Log.e(TAG, "----Status------->onConnectionFailed")
        if (!result.hasResolution()) {
            // Show a localized error dialog.
           /* GooglePlayServicesUtil.getErrorDialog(
                result.errorCode, this, 0
            ) { retryConnecting() }!!.show()*/
        }

    }

    override fun onConnected(p0: Bundle?) {
        Log.e(TAG, "----Status------->onConnected successfully")
        val intent = Intent(this, WearDataListenerService::class.java)
        startService(intent)
    }

    override fun onConnectionSuspended(p0: Int) {
        Log.e(TAG, "----Status------->ConnectionSuspended")
        retryConnecting()
    }

    private fun retryConnecting() {
        if (!mGoogleApiClient!!.isConnecting) {
            mGoogleApiClient!!.connect()
        }
    }

    private fun sendDataToWearApp() {
/*        val username = mainBinding.editTextTextPersonName.text.toString()
        val userEmail = mainBinding.editTextTextEmailAddress.text.toString()
        val userPhone = mainBinding.editTextPhone.text.toString()
        if (username.isEmpty() && userEmail.isEmpty() && userPhone.isEmpty()) {
            return
        }*/
        val wearAvailable = mGoogleApiClient!!.hasConnectedApi(Wearable.API)
        Log.i(TAG, "----hasConnectedApi------->$wearAvailable")
        val dataMapRequest = PutDataMapRequest.create(MainActivity.PATH_FOR_WEAR)
        val map = dataMapRequest.dataMap
        map.putString(MainActivity.EXTRA_USER_NAME, "Working")
        map.putString(MainActivity.EXTRA_USER_EMAIL, "userEmail")
        map.putString(MainActivity.EXTRA_USER_PHONE, "userPhone")
        map.putLong(MainActivity.EXTRA_CURRENT_TIME, Date().time)
        val putDataRequest = dataMapRequest.asPutDataRequest()
        putDataRequest.setUrgent()
        Wearable.DataApi.putDataItem(mGoogleApiClient!!, putDataRequest)
            .setResultCallback { dataItemResult: DataApi.DataItemResult ->
                if (dataItemResult.status.isSuccess) {
                    Log.i(TAG, "----sendDataToWearApp------->Successfully!!")
                } else {
                    Log.i(TAG, "----sendDataToWearApp------->Failed!!")
                }
            }
    }

}