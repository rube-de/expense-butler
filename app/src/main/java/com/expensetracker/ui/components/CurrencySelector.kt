package com.expensetracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.expensetracker.ui.theme.ExpenseTrackerTheme
import com.expensetracker.ui.theme.spacing
import java.util.*

data class CurrencyInfo(
    val code: String,
    val displayName: String,
    val symbol: String
)

private val supportedCurrencies = listOf(
    CurrencyInfo("USD", "US Dollar", "$"),
    CurrencyInfo("EUR", "Euro", "€"),
    CurrencyInfo("CHF", "Swiss Franc", "CHF"),
    CurrencyInfo("GBP", "British Pound", "£"),
    CurrencyInfo("JPY", "Japanese Yen", "¥"),
    CurrencyInfo("CAD", "Canadian Dollar", "C$"),
    CurrencyInfo("AUD", "Australian Dollar", "A$"),
    CurrencyInfo("CNY", "Chinese Yuan", "¥"),
    CurrencyInfo("INR", "Indian Rupee", "₹"),
    CurrencyInfo("BRL", "Brazilian Real", "R$")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencySelector(
    selectedCurrency: String,
    onCurrencySelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Currency"
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedCurrencyInfo = supportedCurrencies.find { it.code == selectedCurrency }
        ?: supportedCurrencies.first()
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = "${selectedCurrencyInfo.symbol} ${selectedCurrencyInfo.code}",
            onValueChange = { },
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = "Select currency"
                )
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            supportedCurrencies.forEach { currency ->
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                        ) {
                            Text(
                                text = currency.symbol,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Column {
                                Text(
                                    text = currency.code,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = currency.displayName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    onClick = {
                        onCurrencySelected(currency.code)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CurrencySelectorPreview() {
    ExpenseTrackerTheme {
        var selectedCurrency by remember { mutableStateOf("USD") }
        
        CurrencySelector(
            selectedCurrency = selectedCurrency,
            onCurrencySelected = { selectedCurrency = it },
            modifier = Modifier.padding(16.dp)
        )
    }
}