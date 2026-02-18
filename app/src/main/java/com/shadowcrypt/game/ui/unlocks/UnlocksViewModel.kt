package com.shadowcrypt.game.ui.unlocks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shadowcrypt.game.ServiceLocator
import com.shadowcrypt.game.data.model.MetaProgress
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class UnlocksViewModel : ViewModel() {

    val progress: StateFlow<MetaProgress> =
        ServiceLocator.metaProgressRepository.progress
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = MetaProgress()
            )
}
