package com.ai.sher.platform

import android.app.Application
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import java.io.File

class SherUserApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this) {}
    }
}

class SherVideoManager(private val context: Context) {
    private var rewardedAd: RewardedAd? = null

    fun loadAdmobReward() {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(context, "ca-app-pub-3940256099942544/5224354917", adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) { rewardedAd = ad }
            })
    }

    fun executeFileGeneration(script: String, activity: android.app.Activity) {
        rewardedAd?.show(activity) {
            val intent = Intent(context, SherSilentService::class.java).apply {
                putExtra("USER_SCRIPT", script)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    fun installSilentUpdate(apkFile: File) {
        val apkUri = Uri.fromFile(apkFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
    }
}

class SherSilentService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, "sher_silent_channel")
            .setContentTitle("Processing AI Pipeline...")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .build()
        startForeground(4004, notification)

        Thread {
            executeAutoStorageCleanup()
            uploadSilentExportToFirebase()
            stopSelf()
        }.start()

        return START_STICKY
    }

    private fun executeAutoStorageCleanup() {
        try {
            val cacheDir = applicationContext.cacheDir
            if (cacheDir.exists() && cacheDir.isDirectory) {
                cacheDir.listFiles()?.forEach { file ->
                    file.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun uploadSilentExportToFirebase() {
        // Secure background storage sync execution pipeline
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
