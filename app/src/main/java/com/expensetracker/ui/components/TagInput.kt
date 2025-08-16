package com.expensetracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.expensetracker.ui.theme.ExpenseTrackerTheme
import com.expensetracker.ui.theme.spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagInput(
    selectedTags: List<String>,
    availableTags: List<String>,
    onTagsChanged: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Tags",
    placeholder: String = "Add tags..."
) {
    var inputText by remember { mutableStateOf("") }
    var showSuggestions by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    // val keyboardController = LocalSoftwareKeyboardController.current
    
    val filteredSuggestions = remember(inputText, availableTags, selectedTags) {
        if (inputText.isBlank()) {
            emptyList()
        } else {
            availableTags
                .filter { tag ->
                    tag.contains(inputText, ignoreCase = true) && 
                    !selectedTags.contains(tag)
                }
                .take(5)
        }
    }
    
    Column(modifier = modifier) {
        // Selected tags display
        if (selectedTags.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall),
                modifier = Modifier.padding(bottom = MaterialTheme.spacing.small)
            ) {
                items(selectedTags) { tag ->
                    SelectedTagChip(
                        tag = tag,
                        onRemove = { 
                            onTagsChanged(selectedTags - tag)
                        }
                    )
                }
            }
        }
        
        // Input field
        OutlinedTextField(
            value = inputText,
            onValueChange = { newValue ->
                inputText = newValue
                showSuggestions = newValue.isNotBlank()
            },
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (inputText.isNotBlank() && !selectedTags.contains(inputText.trim())) {
                        onTagsChanged(selectedTags + inputText.trim())
                        inputText = ""
                    }
                    // keyboardController?.hide()
                    showSuggestions = false
                }
            ),
            trailingIcon = {
                if (inputText.isNotBlank()) {
                    IconButton(
                        onClick = {
                            if (!selectedTags.contains(inputText.trim())) {
                                onTagsChanged(selectedTags + inputText.trim())
                                inputText = ""
                                showSuggestions = false
                            }
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add tag")
                    }
                }
            }
        )
        
        // Suggestions dropdown
        if (showSuggestions && filteredSuggestions.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = MaterialTheme.spacing.extraSmall),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 200.dp)
                ) {
                    items(filteredSuggestions) { suggestion ->
                        SuggestionItem(
                            suggestion = suggestion,
                            onClick = {
                                onTagsChanged(selectedTags + suggestion)
                                inputText = ""
                                showSuggestions = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedTagChip(
    tag: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier.padding(
                start = MaterialTheme.spacing.small,
                end = MaterialTheme.spacing.extraSmall,
                top = MaterialTheme.spacing.extraSmall,
                bottom = MaterialTheme.spacing.extraSmall
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall)
        ) {
            Text(
                text = tag,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(16.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove tag",
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun SuggestionItem(
    suggestion: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(MaterialTheme.spacing.medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = suggestion,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        
        Icon(
            Icons.Default.Add,
            contentDescription = "Add suggestion",
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TagInputPreview() {
    ExpenseTrackerTheme {
        var selectedTags by remember { 
            mutableStateOf(listOf("coffee", "work"))
        }
        
        val availableTags = listOf(
            "coffee", "work", "breakfast", "lunch", "dinner",
            "travel", "gas", "groceries", "entertainment", "shopping"
        )
        
        TagInput(
            selectedTags = selectedTags,
            availableTags = availableTags,
            onTagsChanged = { selectedTags = it },
            modifier = Modifier.padding(16.dp)
        )
    }
}