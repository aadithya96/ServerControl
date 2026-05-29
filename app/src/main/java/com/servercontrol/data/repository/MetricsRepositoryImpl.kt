package com.servercontrol.data.repository

import com.servercontrol.data.local.db.MetricSampleDao
import com.servercontrol.data.local.entity.toDomain
import com.servercontrol.data.local.entity.toEntity
import com.servercontrol.domain.model.MetricSample
import com.servercontrol.domain.repository.MetricsRepository
import javax.inject.Inject

class MetricsRepositoryImpl @Inject constructor(
    private val metricSampleDao: MetricSampleDao
) : MetricsRepository {

    override suspend fun saveSample(sample: MetricSample) {
        metricSampleDao.insert(sample.toEntity())
    }

    override suspend fun getSamples(serverId: String, since: Long): List<MetricSample> {
        return metricSampleDao.getSince(serverId, since).map { it.toDomain() }
    }

    override suspend fun pruneOldSamples(keepDays: Int) {
        val cutoff = System.currentTimeMillis() - keepDays * 24 * 60 * 60 * 1000L
        metricSampleDao.deleteOlderThan(cutoff)
    }

    override suspend fun exportCsv(serverId: String, since: Long): String {
        val samples = metricSampleDao.getSince(serverId, since).map { it.toDomain() }
        val header = "timestamp,cpu_percent,mem_used_bytes,mem_total_bytes,disk_used_bytes,disk_total_bytes\n"
        val rows = samples.joinToString("\n") { s ->
            "${s.timestamp},${s.cpuPercent},${s.memUsedBytes},${s.memTotalBytes},${s.diskUsedBytes},${s.diskTotalBytes}"
        }
        return header + rows
    }
}
