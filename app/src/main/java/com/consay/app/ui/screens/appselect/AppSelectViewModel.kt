package com.consay.app.ui.screens.appselect

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.consay.app.ConSayApplication
import com.consay.app.model.AppItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppSelectUiState(
    val isLoading: Boolean = true,
    val allApps: List<AppItem> = emptyList(),
    val filteredApps: List<AppItem> = emptyList(),
    val selectedPackages: Set<String> = emptySet(),
    val searchQuery: String = ""
)

class AppSelectViewModel : ViewModel() {
    private val repository = ConSayApplication.instance.sessionRepository

    private val _isLoading = MutableStateFlow(true)
    private val _allApps = MutableStateFlow<List<AppItem>>(emptyList())
    private val _selectedPackages = MutableStateFlow<Set<String>>(emptySet())
    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<AppSelectUiState> = combine(
        _isLoading,
        _allApps,
        _selectedPackages,
        _searchQuery
    ) { loading, apps, selected, query ->
        val updatedApps = apps.map { it.copy(isSelected = selected.contains(it.packageName)) }
        val filtered = if (query.isBlank()) {
            updatedApps
        } else {
            updatedApps.filter {
                it.label.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true)
            }
        }
        AppSelectUiState(
            isLoading = loading,
            allApps = updatedApps,
            filteredApps = filtered,
            selectedPackages = selected,
            searchQuery = query
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppSelectUiState()
    )

    fun loadApps(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            // Nạp các package đã được chọn từ trước trong DataStore
            val config = repository.getCurrentConfig()
            _selectedPackages.value = config.restrictedPackages

            val apps = withContext(Dispatchers.IO) {
                queryInstalledLauncherApps(context)
            }
            _allApps.value = apps
            _isLoading.value = false
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun toggleAppSelection(packageName: String) {
        val current = _selectedPackages.value.toMutableSet()
        if (current.contains(packageName)) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        _selectedPackages.value = current
    }

    fun selectAllSocialApps() {
        val socialPackages = _allApps.value.filter { it.isSocialOrMessaging }.map { it.packageName }.toSet()
        val current = _selectedPackages.value.toMutableSet()
        current.addAll(socialPackages)
        _selectedPackages.value = current
    }

    fun clearAllSelection() {
        _selectedPackages.value = emptySet()
    }

    fun saveSelection() {
        viewModelScope.launch {
            repository.updateRestrictedPackages(_selectedPackages.value)
        }
    }

    private fun queryInstalledLauncherApps(context: Context): List<AppItem> {
        val pm = context.packageManager
        val selfPackage = context.packageName
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolvedList = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        val socialCandidates = setOf(
            "com.facebook.katana",
            "com.facebook.orca",
            "com.zing.zalo",
            "com.instagram.android",
            "com.zhiliaoapp.musically",
            "org.telegram.messenger",
            "com.twitter.android",
            "com.snapchat.android"
        )

        val result = mutableListOf<AppItem>()
        val seenPackages = mutableSetOf<String>()

        for (resolveInfo in resolvedList) {
            val pkg = resolveInfo.activityInfo.packageName
            if (pkg == selfPackage || seenPackages.contains(pkg)) {
                continue
            }
            seenPackages.add(pkg)

            val label = try {
                resolveInfo.loadLabel(pm).toString()
            } catch (_: Exception) {
                pkg
            }

            val icon = try {
                resolveInfo.loadIcon(pm)
            } catch (_: Exception) {
                null
            }

            val isSocial = socialCandidates.contains(pkg) ||
                    label.contains("facebook", ignoreCase = true) ||
                    label.contains("messenger", ignoreCase = true) ||
                    label.contains("zalo", ignoreCase = true) ||
                    label.contains("instagram", ignoreCase = true) ||
                    label.contains("tiktok", ignoreCase = true) ||
                    label.contains("telegram", ignoreCase = true)

            result.add(
                AppItem(
                    packageName = pkg,
                    label = label,
                    icon = icon,
                    isSocialOrMessaging = isSocial
                )
            )
        }

        // Ưu tiên hiển thị app mạng xã hội lên đầu, sau đó sắp xếp theo tên
        return result.sortedWith(
            compareByDescending<AppItem> { it.isSocialOrMessaging }
                .thenBy { it.label.lowercase() }
        )
    }
}
