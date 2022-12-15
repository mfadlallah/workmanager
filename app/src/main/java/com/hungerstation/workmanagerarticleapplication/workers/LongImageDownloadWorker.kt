package com.hungerstation.workmanagerarticleapplication.workers

import android.app.*
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.hungerstation.workmanagerarticleapplication.helpers.ImageDownloader
import com.hungerstation.workmanagerarticleapplication.R
import kotlinx.coroutines.delay
import java.io.*

class LongImageDownloadWorker(private val appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    private val imageDownloader: ImageDownloader = ImageDownloader()

    override suspend fun doWork(): Result {
        // calling setForeground only needed in case of long running tasks which take more than 10 min
        try {
            setForeground(getForegroundInfo())
        } catch (e: Exception) {
            Log.e("SetForeground Exception", e.message ?: "")
        }

        // Long running delay to replicate long running task ;)
        delay(600000)

        val imageUriInput = inputData.getString("IMAGE_URL") ?: return Result.failure()

        return imageDownloader.downloadImage(imageUriInput, appContext)
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(1, createNotification())
    }

    private fun createNotification(): Notification {
        val pendingIntent = WorkManager.getInstance(applicationContext)
            .createCancelPendingIntent(id)
        val channelId = "workDownload"
        val notificationBuilder = NotificationCompat.Builder(appContext, channelId)

        val notification = notificationBuilder.setOngoing(true)
            .setContentTitle("Downloading Image")
            .setTicker("Downloading Image")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            // Add the cancel action to the notification which can
            // be used to cancel the worker
            .addAction(android.R.drawable.ic_delete, "Cancel Download", pendingIntent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(notification, channelId)
        }

        return notification.build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(
        notificationBuilder: NotificationCompat.Builder,
        channelId: String
    ) {
        val notificationManager =
            appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationBuilder.setDefaults(Notification.DEFAULT_VIBRATE)
        val channel = NotificationChannel(
            channelId,
            "WorkManagerApp",
            NotificationManager.IMPORTANCE_HIGH
        )
        channel.description = "WorkManagerApp Notifications"
        notificationManager.createNotificationChannel(channel)
    }
}
