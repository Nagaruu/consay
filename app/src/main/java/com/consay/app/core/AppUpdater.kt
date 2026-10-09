package com.consay.app.core

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import com.consay.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object AppUpdater {
    // Tên repository GitHub ch?a releases APK (B?n có th? d?i sang username/repo c?a b?n)
    var githubRepo: String = "Nagaruu/consay"
    private const val APK_ASSET_NAME = "app-debug.apk"

    data class UpdateInfo(
        val tag: String,
        val tagNumber: Int,
        val notes: String,
        val apkUrl: String
    )

    sealed interface CheckResult {
        data class UpdateAvailable(val info: UpdateInfo) : CheckResult
        data object UpToDate : CheckResult
        data class Error(val message: String) : CheckResult
    }

    suspend fun checkForUpdates(customRepo: String? = null): CheckResult = withContext(Dispatchers.IO) {
        val repo = customRepo ?: githubRepo
        val apiUrl = "https://api.github.com/repos/$repo/releases/latest"

        try {
            val url = URL(apiUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
            conn.setRequestProperty("User-Agent", "ConSay-App")

            if (conn.responseCode != 200) {
                return@withContext CheckResult.Error("Không tìm th?y b?n phát hành (Mã: ${conn.responseCode})")
            }

            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val tagName = json.optString("tag_name", "")
            val tagNumber = tagName.removePrefix("v").toIntOrNull() ?: 0
            val bodyNotes = json.optString("body", "B?n c?p nh?t m?i")

            // Tìm asset APK
            val assets = json.optJSONArray("assets")
            var downloadUrl = ""
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk")) {
                        downloadUrl = asset.optString("browser_download_url", "")
                        break
                    }
                }
            }

            if (downloadUrl.isBlank()) {
                return@withContext CheckResult.Error("Release chua có file APK dính kèm.")
            }

            val currentVersionCode = BuildConfig.VERSION_CODE
            if (tagNumber > currentVersionCode) {
                CheckResult.UpdateAvailable(
                    UpdateInfo(
                        tag = tagName,
                        tagNumber = tagNumber,
                        notes = bodyNotes,
                        apkUrl = downloadUrl
                    )
                )
            } else {
                CheckResult.UpToDate
            }
        } catch (e: Exception) {
            CheckResult.Error("L?i k?t n?i ki?m tra c?p nh?t: ${e.localizedMessage}")
        }
    }

    /**
     * T? d?ng t?i file APK b?ng DownloadManager và m? c?a s? cài d?t c?a h? di?u hành.
     */
    fun startUpdateDownload(context: Context, updateInfo: UpdateInfo) {
        val fileName = "con-say-${updateInfo.tag}.apk"
        val destinationFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
        if (destinationFile.exists()) {
            destinationFile.delete()
        }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(updateInfo.apkUrl)).apply {
            setTitle("Ðang t?i Con Say ${updateInfo.tag}...")
            setDescription("T? d?ng t?i b?n c?p nh?t m?i nh?t")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationUri(Uri.fromFile(destinationFile))
            setMimeType("application/vnd.android.package-archive")
        }

        val downloadId = downloadManager.enqueue(request)

        // Ðang ký Receiver ch? t?i xong d? l?p t?c kích ho?t cài d?t
        val onCompleteReceiver = object : BroadcastReceiver() {
            override fun onReceive(recvContext: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (id == downloadId) {
                    try {
                        context.unregisterReceiver(this)
                    } catch (_: Exception) {}

                    triggerApkInstall(context, destinationFile)
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                onCompleteReceiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            context.registerReceiver(
                onCompleteReceiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            )
        }
    }

    /**
     * Kích ho?t trình cài d?t h? th?ng (Ngu?i dùng ch? c?n b?m 1 ch?m vào nút "C?p nh?t").
     */
    fun triggerApkInstall(context: Context, apkFile: File) {
        if (!apkFile.exists()) return

        // Ki?m tra quy?n cài d?t t? ngu?n ngoài trên Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val grantIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(grantIntent)
                return
            }
        }

        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(installIntent)
    }
}
