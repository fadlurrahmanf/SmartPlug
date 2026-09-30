package com.smartplug.app.ui.screens.devices

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartplug.app.domain.model.RelayState
import com.smartplug.app.ui.components.EmptyState
import com.smartplug.app.ui.components.PollWhileVisible
import com.smartplug.app.ui.components.SmartPlugCard
import com.smartplug.app.ui.components.StatusPill
import com.smartplug.app.ui.components.relayStatusColor
import com.smartplug.app.ui.components.relayStatusLabel
import com.smartplug.app.util.PollingCadence
import com.smartplug.app.ui.localization.LocalAppLanguage
import com.smartplug.app.ui.localization.localized

@Composable
fun DeviceListScreen(
    onOpenDevice: (String) -> Unit,
    onAddSmartPlug: () -> Unit,
    onAddServer: () -> Unit,
    viewModel: DeviceListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val language = LocalAppLanguage.current
    var addMenuOpen by remember { mutableStateOf(false) }

    PollWhileVisible(intervalMs = PollingCadence.DEVICE_LIST.intervalMs) {
        viewModel.refreshAll()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(localized(language, "Perangkat", "Devices")) },
                actions = {
                    androidx.compose.material3.IconButton(onClick = { addMenuOpen = true }) {
                        Icon(Icons.Filled.Add, contentDescription = localized(language, "Tambah SmartPlug", "Add SmartPlug"))
                    }
                    DropdownMenu(expanded = addMenuOpen, onDismissRequest = { addMenuOpen = false }) {
                        DropdownMenuItem(text = { Text("Add SmartPlug") }, onClick = { addMenuOpen = false; onAddSmartPlug() })
                        DropdownMenuItem(text = { Text("Add ServerSmartPlug") }, onClick = { addMenuOpen = false; onAddServer() })
                    }
                },
            )
        },
    ) { padding ->
        if (uiState.rows.isEmpty()) {
            EmptyState(
                title = localized(language, "Belum ada SmartPlug", "No SmartPlugs yet"),
                description = localized(language, "Tambahkan SmartPlug pertama Anda untuk mulai memantau energi.", "Add your first SmartPlug to begin monitoring energy."),
                modifier = Modifier.padding(padding),
                action = { Button(onClick = onAddSmartPlug) { Text(localized(language, "Tambah SmartPlug", "Add SmartPlug")) } },
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(uiState.rows, key = { it.device.deviceId }) { row ->
                    DeviceRowCard(row = row, onClick = { onOpenDevice(row.device.deviceId) })
                }
            }
        }
    }
}

@Composable
private fun DeviceRowCard(row: DeviceRow, onClick: () -> Unit) {
    val language = LocalAppLanguage.current
    SmartPlugCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(row.device.displayName, style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusPill(
                        label = if (row.online) "Online" else localized(language, "Offline", "Offline"),
                        color = if (row.online) com.smartplug.app.ui.theme.AccentGreen else com.smartplug.app.ui.theme.AccentRed,
                    )
                    Spacer(Modifier.width(8.dp))
                    val relayState = row.status?.relayState ?: RelayState.UNKNOWN
                    StatusPill(label = relayStatusLabel(relayState), color = relayStatusColor(relayState))
                }
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null)
        }
    }
}
