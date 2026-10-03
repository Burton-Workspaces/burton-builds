package com.burton.builds.data.repository

import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InstalledApps @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun applicationIds(): Set<String> {
        val pm = context.packageManager
        val flags = PackageManager.GET_META_DATA
        return pm.getInstalledApplications(flags)
            .map { it.packageName }
            .filter { it.startsWith("com.burton.") }
            .toSet()
    }
}
