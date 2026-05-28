package com.yuntong.vpn.service

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import com.yuntong.vpn.api.V2BoardApi
import com.yuntong.vpn.model.ProfileEntity
import com.yuntong.vpn.model.toEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SubscriptionUpdateService : JobService() {

    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        const val JOB_ID = 1001
        const val INTERVAL_MS = 6 * 60 * 60 * 1000L // 6 hours

        fun schedule(context: Context) {
            val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            val jobInfo = JobInfo.Builder(JOB_ID, ComponentName(context, SubscriptionUpdateService::class.java))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(INTERVAL_MS)
                .setPersisted(true)
                .build()
            scheduler.schedule(jobInfo)
        }
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        scope.launch {
            try {
                // TODO: Fetch updated server list from v2board API
                // and update local Room database
            } catch (_: Exception) {}
            jobFinished(params, false)
        }
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean = true
}
