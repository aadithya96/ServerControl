package com.servercontrol.domain.usecase

import com.servercontrol.domain.model.SavedCommand
import com.servercontrol.domain.repository.CommandRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCommandsUseCase @Inject constructor(
    private val repository: CommandRepository
) {
    operator fun invoke(serverId: String): Flow<List<SavedCommand>> =
        repository.getCommandsForServer(serverId)
}
