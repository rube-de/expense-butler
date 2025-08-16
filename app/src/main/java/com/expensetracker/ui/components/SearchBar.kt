package com.expensetracker.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.expensetracker.ui.theme.ExpenseTrackerTheme

@Composable
fun SearchBar(
    searchText: String,
    onSearchTextChanged: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = searchText,
        onValueChange = onSearchTextChanged,
        placeholder = { Text(placeholder) },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = "Search"
            )
        },
        trailingIcon = {
            if (searchText.isNotEmpty()) {
                IconButton(onClick = { onSearchTextChanged("") }) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = "Clear search"
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun SearchBarPreview() {
    ExpenseTrackerTheme {
        SearchBar(
            searchText = "coffee",
            onSearchTextChanged = { },
            placeholder = "Search expenses...",
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchBarEmptyPreview() {
    ExpenseTrackerTheme {
        SearchBar(
            searchText = "",
            onSearchTextChanged = { },
            placeholder = "Search expenses...",
            modifier = Modifier.fillMaxWidth()
        )
    }
}