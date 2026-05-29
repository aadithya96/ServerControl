package com.servercontrol.presentation.security

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.servercontrol.data.local.db.AuditLogDao
import com.servercontrol.data.local.entity.toDomain
import com.servercontrol.domain.model.AuditLogEntry
import com.servercontrol.domain.model.FailedLoginAttempt
import com.servercontrol.domain.model.SslCertInfo
import com.servercontrol.domain.usecase.BlockIpUseCase
import com.servercontrol.domain.usecase.GetFailedLoginsUseCase
import com.servercontrol.domain.usecase.GetSslCertsUseCase
import com.servercontrol.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SecurityViewModel @Inject constructor(
    private val getFailedLoginsUseCase: GetFailedLoginsUseCase,
    private val getSslCertsUseCase: GetSslCertsUseCase,
    private val blockIpUseCase: BlockIpUseCase,
    private val auditLogDao: AuditLogDao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val serverId: Long = savedStateHandle["serverId"] ?: 0L
    val selectedTab = MutableStateFlow(0)

    private val _failedLogins = MutableStateFlow<Resource<List<FailedLoginAttempt>>>(Resource.Loading)
    val failedLogins: StateFlow<Resource<List<FailedLoginAttempt>>> = _failedLogins.asStateFlow()

    private val _sslCerts = MutableStateFlow<Resource<List<SslCertInfo>>?>(null)
    val sslCerts: StateFlow<Resource<List<SslCertInfo>>?> = _sslCerts.asStateFlow()

    val auditLog: StateFlow<List<AuditLogEntry>> = auditLogDao.getForServer(serverId.toString())
        .map { list -> list.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val blockResult: MutableStateFlow<Resource<String>?> = MutableStateFlow(null)
    val domainsInput = MutableStateFlow("")
    val showAllServersAudit = MutableStateFlow(false)

    val auditLogDisplay: StateFlow<List<AuditLogEntry>> = combine(
        auditLogDao.getAll().map { list -> list.map { it.toDomain() } },
        auditLog,
        showAllServersAudit
    ) { all, serverOnly, showAll ->
        if (showAll) all else serverOnly
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadFailedLogins()
    }

    private fun loadFailedLogins() {
        viewModelScope.launch {
            _failedLogins.value = Resource.Loading
            _failedLogins.value = getFailedLoginsUseCase(serverId, 50)
        }
    }

    fun blockIp(ip: String) {
        viewModelScope.launch {
            blockResult.value = Resource.Loading
            blockResult.value = blockIpUseCase(serverId, ip)
        }
    }

    fun checkSslCerts() {
        val domains = domainsInput.value.trim()
        if (domains.isBlank()) return
        viewModelScope.launch {
            _sslCerts.value = Resource.Loading
            _sslCerts.value = getSslCertsUseCase(serverId, domains)
        }
    }

    fun refresh() {
        loadFailedLogins()
    }

    fun clearBlockResult() {
        blockResult.value = null
    }
}
