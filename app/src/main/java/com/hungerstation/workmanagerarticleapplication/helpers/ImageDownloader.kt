package com.hungerstation.workmanagerarticleapplication.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import androidx.work.Data
import androidx.work.ListenableWorker
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class ImageDownloader {
    @SuppressLint("RestrictedApi")
    fun downloadImage(imageUrl: String, context: Context): ListenableWorker.Result {
        var connection: HttpURLConnection? = null
        var inputStream: InputStream? = null
        var out: ByteArrayOutputStream? = null
        var result: ListenableWorker.Result
        try {
            connection = URL(imageUrl).openConnection() as HttpURLConnection
            connection.connect()
            val length: Int = connection.contentLength
            if (length <= 0) {
                // error
                result = ListenableWorker.Result.failure()
            }
            inputStream = BufferedInputStream(connection.inputStream, 8192)
            out = ByteArrayOutputStream()
            val bytes = ByteArray(8192)
            var count: Int
            var read: Long = 0
            while (inputStream.read(bytes).also { count = it } != -1) {
                read += count.toLong()
                out.write(bytes, 0, count)

                // progress  ((read * 100 / length).toInt())
                // update progress
            }
            val bitmap = BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size())
            val uri = bitmap.saveToInternalStorage(context)
            result = if (uri?.path != null) {
                val data = Data.Builder().put("path", uri.path!!).build()
                ListenableWorker.Result.success(data)
            } else {
                ListenableWorker.Result.failure()
            }

        } catch (e: Throwable) {
            // error
            result = ListenableWorker.Result.failure()
        } finally {
            try {
                connection?.disconnect()
                if (out != null) {
                    out.flush()
                    out.close()
                }
                inputStream?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return result
    }
}