package com.sleeptracker.app.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** Small reusable factory so ViewModels can take plain constructor parameters without Hilt. */
class ViewModelFactory(private val creator: () -> ViewModel) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = creator() as T
}
