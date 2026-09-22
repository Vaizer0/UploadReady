package com.vaizero.uploadready.ui.profiles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vaizero.uploadready.data.model.AppProfile
import com.vaizero.uploadready.ui.components.RequirementBadge

@Composable
fun ProfilesScreen(
    viewModel: ProfilesViewModel,
    onApplyPhotoProfile: (AppProfile) -> Unit,
    onApplySignatureProfile: (AppProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val dialogState by viewModel.dialogState.collectAsStateWithLifecycle()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openCreateDialog() },
                modifier = Modifier.testTag("fab_add_profile"),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Profile")
            }
        },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("profiles_screen_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        text = "Exam & Job Profiles",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Pre-configured dimension and file-size presets for online portals",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(profiles, key = { it.id }) { profile ->
                ProfileDetailCard(
                    profile = profile,
                    onEdit = { viewModel.openEditDialog(profile) },
                    onDelete = { viewModel.deleteProfile(profile) },
                    onApplyPhoto = { onApplyPhotoProfile(profile) },
                    onApplySignature = { onApplySignatureProfile(profile) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    if (dialogState.isOpen) {
        ProfileEditDialog(
            state = dialogState,
            onDismiss = { viewModel.closeDialog() },
            onSave = { viewModel.saveProfile() },
            onNameChange = { viewModel.updateDialogName(it) },
            onPhotoWidthChange = { viewModel.updatePhotoWidth(it) },
            onPhotoHeightChange = { viewModel.updatePhotoHeight(it) },
            onPhotoMinKbChange = { viewModel.updatePhotoMinKb(it) },
            onPhotoMaxKbChange = { viewModel.updatePhotoMaxKb(it) },
            onSignWidthChange = { viewModel.updateSignatureWidth(it) },
            onSignHeightChange = { viewModel.updateSignatureHeight(it) },
            onSignMinKbChange = { viewModel.updateSignatureMinKb(it) },
            onSignMaxKbChange = { viewModel.updateSignatureMaxKb(it) },
            onPdfMaxKbChange = { viewModel.updatePdfMaxKb(it) }
        )
    }
}

@Composable
fun ProfileDetailCard(
    profile: AppProfile,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onApplyPhoto: () -> Unit,
    onApplySignature: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (profile.isPreset) {
                    RequirementBadge(text = "Official Preset", isValid = true)
                } else {
                    Row {
                        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Specs grid
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Photo Requirements:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Text("${profile.photoWidthPx} × ${profile.photoHeightPx} px • ${profile.photoMinKb}–${profile.photoMaxKb} KB", style = MaterialTheme.typography.bodySmall)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Signature Requirements:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Text("${profile.signatureWidthPx} × ${profile.signatureHeightPx} px • ${profile.signatureMinKb}–${profile.signatureMaxKb} KB", style = MaterialTheme.typography.bodySmall)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Document PDF Limit:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    Text("≤ ${profile.pdfMaxKb} KB (${profile.pdfMaxKb / 1024} MB)", style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick launch buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onApplyPhoto,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Crop, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Prepare Photo", style = MaterialTheme.typography.labelMedium)
                }

                OutlinedButton(
                    onClick = onApplySignature,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Draw, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Prepare Sign", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
fun ProfileEditDialog(
    state: ProfileDialogState,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onNameChange: (String) -> Unit,
    onPhotoWidthChange: (String) -> Unit,
    onPhotoHeightChange: (String) -> Unit,
    onPhotoMinKbChange: (String) -> Unit,
    onPhotoMaxKbChange: (String) -> Unit,
    onSignWidthChange: (String) -> Unit,
    onSignHeightChange: (String) -> Unit,
    onSignMinKbChange: (String) -> Unit,
    onSignMaxKbChange: (String) -> Unit,
    onPdfMaxKbChange: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (state.editingProfile == null) "Create Custom Profile" else "Edit Profile",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (state.error != null) {
                    Text(state.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                OutlinedTextField(
                    value = state.name,
                    onValueChange = onNameChange,
                    label = { Text("Profile Name (e.g. State Exam)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("Photo Rules", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = state.photoWidth, onValueChange = onPhotoWidthChange, label = { Text("Width px") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                    OutlinedTextField(value = state.photoHeight, onValueChange = onPhotoHeightChange, label = { Text("Height px") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = state.photoMinKb, onValueChange = onPhotoMinKbChange, label = { Text("Min KB") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                    OutlinedTextField(value = state.photoMaxKb, onValueChange = onPhotoMaxKbChange, label = { Text("Max KB") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                }

                Text("Signature Rules", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = state.signatureWidth, onValueChange = onSignWidthChange, label = { Text("Width px") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                    OutlinedTextField(value = state.signatureHeight, onValueChange = onSignHeightChange, label = { Text("Height px") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = state.signatureMinKb, onValueChange = onSignMinKbChange, label = { Text("Min KB") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                    OutlinedTextField(value = state.signatureMaxKb, onValueChange = onSignMaxKbChange, label = { Text("Max KB") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                }

                Text("Document PDF Rule", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(
                    value = state.pdfMaxKb,
                    onValueChange = onPdfMaxKbChange,
                    label = { Text("Max PDF KB (e.g. 1024)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = onSave) {
                Text("Save Profile")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
