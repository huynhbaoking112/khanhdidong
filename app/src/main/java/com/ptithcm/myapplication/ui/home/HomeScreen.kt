package com.ptithcm.myapplication.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ptithcm.myapplication.data.UserSession

@Composable
internal fun HomeScreen(
    user: UserSession,
    onChangePassword: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        UserHeaderCard(user)
        RoleAccessCard(user.role)
        RoleWorkspaceCard(user.role)
        AccountActions(
            onChangePassword = onChangePassword,
            onLogout = onLogout
        )
    }
}

@Composable
private fun UserHeaderCard(user: UserSession) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Welcome, ${user.fullName}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text("Username: ${user.username}")
            Text("Role: ${user.role.value}")
        }
    }
}

@Composable
private fun AccountActions(
    onChangePassword: () -> Unit,
    onLogout: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            modifier = Modifier.weight(1f),
            onClick = onChangePassword
        ) {
            Text("Change password")
        }
        Spacer(Modifier.width(12.dp))
        Button(
            modifier = Modifier.weight(1f),
            onClick = onLogout
        ) {
            Text("Logout")
        }
    }
}
