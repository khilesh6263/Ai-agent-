package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.QuestionAnswer
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.AgentSettings
import com.example.data.FaqItem

@Composable
fun FaqsScreen(
    settings: AgentSettings,
    faqs: List<FaqItem>,
    onSavePersona: (agentName: String, ownerName: String, availabilityNote: String, customGreeting: String) -> Unit,
    onSaveFaq: (FaqItem) -> Unit,
    onToggleFaq: (FaqItem) -> Unit,
    onDeleteFaq: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var agentName by remember { mutableStateOf(settings.agentName) }
    var ownerName by remember { mutableStateOf(settings.ownerName) }
    var availabilityNote by remember { mutableStateOf(settings.availabilityNote) }
    var customGreeting by remember { mutableStateOf(settings.customGreeting) }
    var savedBanner by remember { mutableStateOf(false) }

    var editingFaq by remember { mutableStateOf<FaqItem?>(null) }
    var showFaqDialog by remember { mutableStateOf(false) }

    LaunchedEffect(settings) {
        agentName = settings.agentName
        ownerName = settings.ownerName
        availabilityNote = settings.availabilityNote
        customGreeting = settings.customGreeting
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Greeting & Common Q&A",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.RecordVoiceOver,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Greeting & Caller Context",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = agentName,
                                onValueChange = {
                                    agentName = it
                                    savedBanner = false
                                },
                                label = { Text("Agent Name") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = ownerName,
                                onValueChange = {
                                    ownerName = it
                                    savedBanner = false
                                },
                                label = { Text("Your Name") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        OutlinedTextField(
                            value = availabilityNote,
                            onValueChange = {
                                availabilityNote = it
                                savedBanner = false
                            },
                            label = { Text("Availability Note (shared with callers)") },
                            placeholder = { Text("e.g., In a client meeting until 4:00 PM") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = customGreeting,
                            onValueChange = {
                                customGreeting = it
                                savedBanner = false
                            },
                            label = { Text("Custom Opening Greeting (Optional)") },
                            placeholder = { Text("Leave blank for smart Gemini greeting") },
                            supportingText = { Text("Supports {agent}, {owner}, {status} placeholders") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (savedBanner) "Saved" else "",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Button(
                                onClick = {
                                    onSavePersona(agentName, ownerName, availabilityNote, customGreeting)
                                    savedBanner = true
                                },
                                modifier = Modifier.testTag("save_greeting_persona_button")
                            ) {
                                Text("Save Greeting")
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Common Questions (FAQs)",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "AI uses active FAQs to answer callers directly",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(
                        onClick = {
                            editingFaq = null
                            showFaqDialog = true
                        },
                        modifier = Modifier.testTag("add_faq_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Q&A")
                    }
                }
            }

            if (faqs.isEmpty()) {
                item {
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.QuestionAnswer,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "No FAQ entries yet",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Add common questions like office hours, address, or delivery instructions.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(faqs, key = { it.id }) { item ->
                    FaqCard(
                        item = item,
                        onToggle = { onToggleFaq(item) },
                        onEdit = {
                            editingFaq = item
                            showFaqDialog = true
                        },
                        onDelete = { onDeleteFaq(item.id) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    if (showFaqDialog) {
        FaqEditorDialog(
            initial = editingFaq,
            onDismiss = { showFaqDialog = false },
            onSave = { saved ->
                onSaveFaq(saved)
                showFaqDialog = false
            }
        )
    }
}

@Composable
private fun FaqCard(
    item: FaqItem,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            if (item.isEnabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = item.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Edit FAQ",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Delete FAQ",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = item.isEnabled,
                        onCheckedChange = { onToggle() }
                    )
                }
            }
            Text(
                text = item.question,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = item.answer,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FaqEditorDialog(
    initial: FaqItem?,
    onDismiss: () -> Unit,
    onSave: (FaqItem) -> Unit
) {
    var category by remember { mutableStateOf(initial?.category ?: "General") }
    var question by remember { mutableStateOf(initial?.question ?: "") }
    var answer by remember { mutableStateOf(initial?.answer ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add Common Question" else "Edit Common Question") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    label = { Text("Caller Question") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it },
                    label = { Text("AI Response Answer") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (question.isNotBlank() && answer.isNotBlank()) {
                        onSave(
                            FaqItem(
                                id = initial?.id ?: 0L,
                                category = category.ifBlank { "General" },
                                question = question.trim(),
                                answer = answer.trim(),
                                isEnabled = initial?.isEnabled ?: true
                            )
                        )
                    }
                },
                enabled = question.isNotBlank() && answer.isNotBlank(),
                modifier = Modifier.testTag("save_faq_dialog_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
