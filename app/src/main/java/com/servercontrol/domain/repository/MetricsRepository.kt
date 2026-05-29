package com.servercontrol.domain.repository

import com.servercontrol.domain.model.MetricSample

interface MetricsRepository {
    suspend fun saveSample(sample: MetricSample)
    suspend fun getSamples(serverId: String, since: Long): List<MetricSample>
    suspend fun pruneOldSamples(keepDays: Int = 7)
    suspend fun exportCsv(serverId: String, since: Long): String
}
