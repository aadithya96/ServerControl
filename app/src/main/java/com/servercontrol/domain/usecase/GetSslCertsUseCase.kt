package com.servercontrol.domain.usecase

import com.servercontrol.domain.model.SslCertInfo
import com.servercontrol.domain.repository.SecurityRepository
import com.servercontrol.util.Resource
import javax.inject.Inject

class GetSslCertsUseCase @Inject constructor(
    private val repository: SecurityRepository
) {
    suspend operator fun invoke(serverId: Long, domains: String): Resource<List<SslCertInfo>> =
        repository.getSslCerts(serverId, domains)
}
