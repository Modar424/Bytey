package com.bytewise.game.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bytewise.game.R
import com.bytewise.game.data.model.AppLanguage
import com.bytewise.game.ui.bytey.ByteyEvent
import com.bytewise.game.ui.bytey.ByteyView
import com.bytewise.game.ui.bytey.rememberByteyController

/** Honest placeholder for screens that arrive in later phases (Levels: phase 5, Progress: phase 6/7). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceholderScreen(title: String, language: AppLanguage, byteyTier: Int, onBack: () -> Unit) {
    val bytey = rememberByteyController()
    val resources = LocalContext.current.resources
    LaunchedEffect(language) { bytey.react(ByteyEvent.COMING_SOON, resources) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ByteyView(bytey, byteyTier)
            Text(stringResource(R.string.coming_soon), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 16.dp))
        }
    }
}
