package com.hungerstation.workmanagerarticleapplication.workers

import android.content.Context
import androidx.work.*
import com.hungerstation.workmanagerarticleapplication.helpers.ImageDownloader
import kotlinx.coroutines.delay

class DownloadImageWorker(private val appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    private val imageDownloader: ImageDownloader = ImageDownloader()

    override suspend fun doWork(): Result {
        // add some delay
        delay(5000)

        val imageUriInput = inputData.getString("IMAGE_URL") ?: return Result.failure()

        return imageDownloader.downloadImage(imageUriInput, appContext)
    }
}
