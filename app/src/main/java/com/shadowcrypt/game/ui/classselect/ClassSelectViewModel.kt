package com.shadowcrypt.game.ui.classselect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shadowcrypt.game.ServiceLocator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class ClassSelectViewModel : ViewModel() {

    val unlockedClassIds: StateFlow<Set<String>> =
        ServiceLocator.metaProgressRepository.progress
            .map { it.unlockedClassIds }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = setOf("warrior")
            )
}
