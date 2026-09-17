package com.ussdauto.android.ui.devicesetup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ussdauto.android.R
import com.ussdauto.android.ui.theme.UssdAutomationTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DeviceSetupActivity : ComponentActivity() {

    private val viewModel: DeviceSetupViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UssdAutomationTheme {
                val state by viewModel.uiState.collectAsState()
                DeviceSetupScreen(
                    state = state,
                    onDeviceIdChange = viewModel::onDeviceIdChange,
                    onApiKeyChange = viewModel::onApiKeyChange,
                    onSave = viewModel::save
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceSetupScreen(
    state: DeviceSetupUiState,
    onDeviceIdChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.device_setup_title)) }) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = state.deviceId,
                onValueChange = onDeviceIdChange,
                label = { Text(stringResource(R.string.device_setup_device_id)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.apiKey,
                onValueChange = onApiKeyChange,
                label = { Text(stringResource(R.string.device_setup_api_key)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onSave,
                enabled = !state.saving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(
                        if (state.saving) R.string.device_setup_saving else R.string.device_setup_save
                    )
                )
            }
            state.resultMessage?.let { message ->
                Spacer(Modifier.height(12.dp))
                Text(
                    text = message,
                    color = if (state.resultIsError) Color(0xFFB71C1C) else Color(0xFF2E7D32),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
