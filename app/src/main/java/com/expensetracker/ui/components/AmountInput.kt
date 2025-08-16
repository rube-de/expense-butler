package com.expensetracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.expensetracker.ui.theme.ExpenseTrackerTheme
import com.expensetracker.ui.theme.spacing
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmountInput(
    amount: BigDecimal?,
    currency: String,
    onAmountChanged: (BigDecimal?) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Amount",
    isError: Boolean = false,
    errorMessage: String? = null,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {}
) {
    var textValue by remember(amount) {
        mutableStateOf(amount?.toPlainString() ?: "")
    }
    
    val focusRequester = remember { FocusRequester() }
    // val keyboardController = LocalSoftwareKeyboardController.current
    
    val currencySymbol = remember(currency) {
        try {
            Currency.getInstance(currency).symbol
        } catch (e: Exception) {
            currency
        }
    }
    
    Column(modifier = modifier) {
        OutlinedTextField(
            value = textValue,
            onValueChange = { newValue ->
                // Allow only numbers and decimal point
                val filteredValue = newValue.filter { it.isDigit() || it == '.' }
                
                // Prevent multiple decimal points
                val decimalCount = filteredValue.count { it == '.' }
                if (decimalCount <= 1) {
                    textValue = filteredValue
                    
                    // Convert to BigDecimal
                    val bigDecimalValue = try {
                        if (filteredValue.isBlank()) {
                            null
                        } else {
                            BigDecimal(filteredValue)
                        }
                    } catch (e: NumberFormatException) {
                        null
                    }
                    
                    onAmountChanged(bigDecimalValue)
                }
            },
            label = { Text(label) },
            leadingIcon = {
                Text(
                    text = currencySymbol,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = imeAction
            ),
            keyboardActions = KeyboardActions(
                onNext = { onImeAction() },
                onDone = { 
                    onImeAction()
                    // keyboardController?.hide()
                }
            ),
            isError = isError,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            singleLine = true
        )
        
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = MaterialTheme.spacing.medium, top = MaterialTheme.spacing.extraSmall)
            )
        }
        
        // Show formatted amount preview
        if (amount != null && amount > BigDecimal.ZERO) {
            Text(
                text = formatAmountPreview(amount, currency),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = MaterialTheme.spacing.extraSmall)
            )
        }
    }
}

@Composable
fun AmountInputWithCurrency(
    amount: BigDecimal?,
    currency: String,
    onAmountChanged: (BigDecimal?) -> Unit,
    onCurrencyChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    amountLabel: String = "Amount",
    currencyLabel: String = "Currency",
    isAmountError: Boolean = false,
    amountErrorMessage: String? = null
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
            verticalAlignment = Alignment.Top
        ) {
            AmountInput(
                amount = amount,
                currency = currency,
                onAmountChanged = onAmountChanged,
                label = amountLabel,
                isError = isAmountError,
                errorMessage = amountErrorMessage,
                modifier = Modifier.weight(2f)
            )
            
            CurrencySelector(
                selectedCurrency = currency,
                onCurrencySelected = onCurrencyChanged,
                label = currencyLabel,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private fun formatAmountPreview(amount: BigDecimal, currency: String): String {
    return try {
        val formatter = NumberFormat.getCurrencyInstance(Locale.getDefault())
        formatter.currency = Currency.getInstance(currency)
        formatter.format(amount)
    } catch (e: Exception) {
        "$currency ${DecimalFormat("#,##0.00").format(amount)}"
    }
}

@Preview(showBackground = true)
@Composable
private fun AmountInputPreview() {
    ExpenseTrackerTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            var amount1 by remember { mutableStateOf<BigDecimal?>(null) }
            var currency1 by remember { mutableStateOf("USD") }
            
            Text("Amount Input with Currency:")
            AmountInputWithCurrency(
                amount = amount1,
                currency = currency1,
                onAmountChanged = { amount1 = it },
                onCurrencyChanged = { currency1 = it }
            )
            
            var amount2 by remember { mutableStateOf(BigDecimal("25.50")) }
            
            Text("Amount Input with Error:")
            AmountInput(
                amount = amount2,
                currency = "EUR",
                onAmountChanged = { amount2 = it ?: BigDecimal.ZERO },
                isError = true,
                errorMessage = "Amount must be greater than 0"
            )
        }
    }
}