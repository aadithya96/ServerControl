package com.servercontrol.domain.usecase

import com.servercontrol.domain.model.FailedLoginAttempt
import com.servercontrol.domain.repository.SecurityRepository
import com.servercontrol.util.Resource
import javax.inject.Inject

class GetFailedLoginsUseCase @Inject constructor(
    private val repository: SecurityRepository
) {
    suspend operator fun invoke(serverId: Long, limit: Int = 50): Resource<List<FailedLoginAttempt>> =
        repository.getFailedLogins(serverId, limit)
}
