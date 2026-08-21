package com.rovena.garage.presentation.common

import android.content.Context
import androidx.fragment.app.Fragment
import com.rovena.garage.AppContainer
import com.rovena.garage.RovenaApp

val Context.appContainer: AppContainer
    get() = (applicationContext as RovenaApp).container

val Fragment.appContainer: AppContainer
    get() = requireContext().appContainer
