package com.shadowcrypt.game.ui.mainmenu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shadowcrypt.game.ServiceLocator
import com.shadowcrypt.game.data.MetaProgressRepository.MetaProgress
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class MainMenuViewModel : ViewModel() {

    val progress: StateFlow<MetaProgress> =
        ServiceLocator.metaProgressRepository.progress
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = MetaProgress()
            )
}
