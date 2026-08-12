package com.example.dingtoneclone

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

// Navigation destinations
@Serializable data object Login : NavKey
@Serializable data object Dashboard : NavKey
@Serializable data object Numbers : NavKey
@Serializable data object Calls : NavKey
