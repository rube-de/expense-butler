package com.expensetracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.expensetracker.R
import com.expensetracker.data.model.RecurrenceFrequency
import com.expensetracker.ui.theme.ExpenseTrackerTheme
import com.expensetracker.ui.theme.spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrequencySelector(
    selectedFrequency: RecurrenceFrequency,
    onFrequencySelected: (RecurrenceFrequency) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
    ) {
        Text(
            text = stringResource(R.string.label_frequency),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        
        // Use a simple Row with toggle buttons since SegmentedButton may not be available
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("frequency_selector"),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            RecurrenceFrequency.values().forEach { frequency ->
                FilterChip(
                    selected = selectedFrequency == frequency,
                    onClick = { onFrequencySelected(frequency) },
                    label = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = getFrequencyIcon(frequency),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = getFrequencyLabel(frequency),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("frequency_option_${frequency.name.lowercase()}")
                )
            }
        }
        
        // Show frequency description
        Text(
            text = getFrequencyDescription(selectedFrequency),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = MaterialTheme.spacing.small)
        )
    }
}

private fun getFrequencyIcon(frequency: RecurrenceFrequency): ImageVector {
    return when (frequency) {
        RecurrenceFrequency.DAILY -> Icons.Filled.Star
        RecurrenceFrequency.WEEKLY -> Icons.Filled.Refresh
        RecurrenceFrequency.MONTHLY -> Icons.Filled.Home
        RecurrenceFrequency.YEARLY -> Icons.Filled.DateRange
    }
}

@Composable
private fun getFrequencyLabel(frequency: RecurrenceFrequency): String {
    return when (frequency) {
        RecurrenceFrequency.DAILY -> stringResource(R.string.frequency_daily)
        RecurrenceFrequency.WEEKLY -> stringResource(R.string.frequency_weekly)
        RecurrenceFrequency.MONTHLY -> stringResource(R.string.frequency_monthly)
        RecurrenceFrequency.YEARLY -> stringResource(R.string.frequency_yearly)
    }
}

@Composable
private fun getFrequencyDescription(frequency: RecurrenceFrequency): String {
    return when (frequency) {
        RecurrenceFrequency.DAILY -> stringResource(R.string.frequency_daily_description)
        RecurrenceFrequency.WEEKLY -> stringResource(R.string.frequency_weekly_description)
        RecurrenceFrequency.MONTHLY -> stringResource(R.string.frequency_monthly_description)
        RecurrenceFrequency.YEARLY -> stringResource(R.string.frequency_yearly_description)
    }
}

@Preview(showBackground = true, name = "Daily Selected")
@Composable
private fun FrequencySelectorDailyPreview() {
    ExpenseTrackerTheme {
        FrequencySelector(
            selectedFrequency = RecurrenceFrequency.DAILY,
            onFrequencySelected = { },
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, name = "Monthly Selected")
@Composable
private fun FrequencySelectorMonthlyPreview() {
    ExpenseTrackerTheme {
        FrequencySelector(
            selectedFrequency = RecurrenceFrequency.MONTHLY,
            onFrequencySelected = { },
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, name = "Yearly Selected")
@Composable
private fun FrequencySelectorYearlyPreview() {
    ExpenseTrackerTheme {
        FrequencySelector(
            selectedFrequency = RecurrenceFrequency.YEARLY,
            onFrequencySelected = { },
            modifier = Modifier.padding(16.dp)
        )
    }
}