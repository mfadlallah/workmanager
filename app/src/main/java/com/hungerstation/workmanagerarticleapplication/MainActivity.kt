package com.hungerstation.workmanagerarticleapplication

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.work.*
import com.hungerstation.workmanagerarticleapplication.databinding.ActivityMainBinding
import com.hungerstation.workmanagerarticleapplication.workers.DownloadImageWorker
import com.hungerstation.workmanagerarticleapplication.workers.LongImageDownloadWorker
import java.util.concurrent.TimeUnit

private const val WORK_TAG = "DOWNLOAD_IMAGE_WORK"
private const val EXPEDITED_WORK_TAG = "EXPIDETED_DOWNLOAD_IMAGE_WORK"
private const val PERIODIC_WORK_TAG = "PERIODIC_DOWNLOAD_IMAGE_WORK"

class MainActivity : AppCompatActivity() {

    private val workManager = WorkManager.getInstance(this)
    private lateinit var binding: ActivityMainBinding

    private val imageUrl = "https://images.pexels.com/photos/169573/pexels-photo-169573.jpeg" +
            "?auto=compress&cs=tinysrgb&dpr=2&h=650&w=940"

    private val constraints = Constraints.Builder()
        .setRequiresCharging(true)
        .build()

    private val downloadWorkRequest = OneTimeWorkRequestBuilder<DownloadImageWorker>()
        .setConstraints(constraints)
        .setInputData(
            workDataOf(
                "IMAGE_URL" to imageUrl
            )
        )
        .addTag(WORK_TAG).build()

    private val expeditedDownloadWorkRequest = OneTimeWorkRequestBuilder<DownloadImageWorker>()
        .setInputData(
            workDataOf(
                "IMAGE_URL" to imageUrl
            )
        )
        .addTag(EXPEDITED_WORK_TAG).setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
        .build()

    private val periodicDownloadWorkRequest = PeriodicWorkRequestBuilder<DownloadImageWorker>(
        1, TimeUnit.HOURS, // repeatInterval (the period cycle)
        15, TimeUnit.MINUTES  // flexInterval
    ).setInputData(
        workDataOf(
            "IMAGE_URL" to imageUrl
        )
    ).addTag(PERIODIC_WORK_TAG).build()

    private val longDownloadWorkRequest = OneTimeWorkRequestBuilder<LongImageDownloadWorker>()
        .setInputData(
            workDataOf(
                "IMAGE_URL" to imageUrl
            )
        )
        .addTag(WORK_TAG).build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onResume() {
        super.onResume()

        binding.regularDownloadImageButton.setOnClickListener {
            workManager.enqueue(downloadWorkRequest)
        }

        binding.expeditedDownloadImageButton.setOnClickListener {
            workManager.enqueue(expeditedDownloadWorkRequest)
        }

        binding.periodicDownloadImageButton.setOnClickListener {
            workManager.enqueue(periodicDownloadWorkRequest)
        }

        binding.longDownloadImageButton.setOnClickListener {
            workManager.enqueue(longDownloadWorkRequest)
        }

        observeWork(WORK_TAG)
        observeWork(EXPEDITED_WORK_TAG)
        observeWork(PERIODIC_WORK_TAG)
    }

    private fun observeWork(tag: String) {
        workManager.getWorkInfosByTagLiveData(tag)
            .observe(this) { info ->
                if (info.firstOrNull()?.state != null && info.first().state.isFinished) {
                    val path = info.first().outputData.keyValueMap["path"] as? String

                    val bitmap: Bitmap? =
                        BitmapFactory.decodeFile(path)
                    binding.image.setImageBitmap(bitmap)
                    binding.downloadCount.text =
                        "${Integer.parseInt(binding.downloadCount.text.toString()) + 1}"
                }
            }
    }
}
