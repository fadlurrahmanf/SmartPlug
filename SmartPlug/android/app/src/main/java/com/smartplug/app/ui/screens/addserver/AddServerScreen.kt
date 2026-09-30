package com.smartplug.app.ui.screens.addserver

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AddServerScreen(onDone: () -> Unit, onBack: () -> Unit, viewModel: AddServerViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { viewModel.refreshPrerequisites() }
    Scaffold(topBar = { TopAppBar(title = { Text("Add ServerSmartPlug") }, navigationIcon = {
        IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
    }) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.permissionMissing) {
                Text("Wi-Fi permission is required to connect to the server setup access point.")
                Button(onClick = { permissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION) }) { Text("Grant Wi-Fi permission") }
                return@Column
            }
            if (state.locationServiceDisabled) {
                Text("Enable Location so Android permits Wi-Fi onboarding.")
                Button(onClick = { context.startActivity(android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }) { Text("Open Location settings") }
                Button(onClick = viewModel::refreshPrerequisites) { Text("I have enabled it") }
                return@Column
            }
            Text("The app will connect to ServerSmartPlug-Setup, send this Wi-Fi configuration, wait for the server IP, then save the server profile securely.")
            OutlinedTextField(state.displayName, { value -> viewModel.update { it.copy(displayName = value) } }, label = { Text("Server name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(state.ssid, { value -> viewModel.update { it.copy(ssid = value) } }, label = { Text("Home Wi-Fi SSID") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(state.password, { value -> viewModel.update { it.copy(password = value) } }, label = { Text("Home Wi-Fi password") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(state.apiToken, { value -> viewModel.update { it.copy(apiToken = value) } }, label = { Text("Server API token (min. 16 characters)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(state.mqttUsername, { value -> viewModel.update { it.copy(mqttUsername = value) } }, label = { Text("MQTT username") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(state.mqttPassword, { value -> viewModel.update { it.copy(mqttPassword = value) } }, label = { Text("MQTT password") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            state.message?.let { Text(it) }
            if (state.completed) Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Done") }
            else Button(onClick = viewModel::provision, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) {
                if (state.busy) CircularProgressIndicator() else Text("Apply to ServerSmartPlug")
            }
        }
    }
}
