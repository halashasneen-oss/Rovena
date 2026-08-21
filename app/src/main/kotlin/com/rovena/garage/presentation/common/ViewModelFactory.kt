package com.rovena.garage.presentation.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Generic factory backed by a plain lambda, used everywhere in place of a DI
 * framework: `viewModels { viewModelFactory { MyViewModel(container.repo) } }`.
 */
class GenericViewModelFactory<T : ViewModel>(private val creator: () -> T) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <VM : ViewModel> create(modelClass: Class<VM>): VM = creator() as VM
}

fun <T : ViewModel> viewModelFactory(creator: () -> T): ViewModelProvider.Factory = GenericViewModelFactory(creator)
