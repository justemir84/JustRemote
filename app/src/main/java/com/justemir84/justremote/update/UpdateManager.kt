package com.justemir84.justremote.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class UpdateManager(private val context: Context) {

    private val client = OkHttpClient()
    private val repo = "justemir84/JustRemote"

    data class ReleaseInfo(val version: String, val downloadUrl: String?)

    suspend fun checkLatestRelease(): ReleaseInfo? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("https://api.github.com/repos/$repo/releases/latest")
                .header("Accept", "application/vnd.github+json")
                .build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext null
            val json = JSONObject(resp.body?.string() ?: "")
            val tag = json.optString("tag_name", "")
            val version = tag.removePrefix("v")
            val assets = json.optJSONArray("assets")
            var apkUrl: String? = null
            for (i in 0 until (assets?.length() ?: 0)) {
                val a = assets.getJSONObject(i)
                if (a.optString("name").endsWith(".apk")) {
                    apkUrl = a.optString("browser_download_url")
                    break
                }
            }
            ReleaseInfo(version, apkUrl)
        } catch (e: Exception) {
            Log.e("UpdateManager", "Check failed", e)
            null
        }
    }

    suspend fun downloadAndInstall(apkUrl: String, onProgress: (Int) -> Unit = {}): Boolean = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url(apkUrl).build()
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext false
            val file = File(context.cacheDir, "update.apk")
            val body = resp.body ?: return@withContext false
            val total = body.contentLength()
            var downloaded = 0L
            body.byteStream().use { input ->
                FileOutputStream(file).use { output ->
                    val buf = ByteArray(8192)
                    var n: Int
                    while (input.read(buf).also { n = it } != -1) {
                        output.write(buf, 0, n)
                        downloaded += n
                        if (total > 0) onProgress((downloaded * 100 / total).toInt())
                    }
                }
            }
            installApk(file)
            true
        } catch (e: Exception) {
            Log.e("UpdateManager", "Download failed", e)
            false
        }
    }

    private fun installApk(file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun getCurrentVersion(): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0"
        } catch (e: Exception) {
            "0"
        }
    }
}