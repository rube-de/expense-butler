package com.expensetracker.ui.util

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.expensetracker.R
import com.expensetracker.domain.error.UserFacingError

/**
 * Maps UserFacingError to appropriate string resources for display in the UI.
 * Technical error details are preserved for logging while user sees friendly messages.
 */
@Composable
fun UserFacingError.toDisplayString(): String {
    return when (this) {
        is UserFacingError.SaveFailed -> stringResource(R.string.error_save_failed)
        is UserFacingError.LoadFailed -> stringResource(R.string.error_load_failed)
        is UserFacingError.UpdateFailed -> stringResource(R.string.error_update_failed)
        is UserFacingError.DeleteFailed -> stringResource(R.string.error_delete_failed)
        is UserFacingError.NetworkUnavailable -> stringResource(R.string.error_network_unavailable)
        is UserFacingError.UnexpectedError -> stringResource(R.string.error_unexpected)
        is UserFacingError.AmountRequired -> stringResource(R.string.error_amount_required)
        is UserFacingError.InvalidAmount -> stringResource(R.string.error_invalid_amount)
        is UserFacingError.DescriptionRequired -> stringResource(R.string.error_description_required)
        is UserFacingError.CategoryRequired -> stringResource(R.string.error_category_required)
        
        // For errors with technical details, show user-friendly message
        // (Technical details should be logged separately)
        is UserFacingError.SaveFailedWithReason -> stringResource(R.string.error_save_failed)
        is UserFacingError.LoadFailedWithReason -> stringResource(R.string.error_load_failed)
        is UserFacingError.UpdateFailedWithReason -> stringResource(R.string.error_update_failed)
        is UserFacingError.DeleteFailedWithReason -> stringResource(R.string.error_delete_failed)
    }
}

/**
 * Gets the string resource ID for a UserFacingError without composable context.
 * Useful for testing or when you need the resource ID directly.
 */
@StringRes
fun UserFacingError.getStringResourceId(): Int {
    return when (this) {
        is UserFacingError.SaveFailed -> R.string.error_save_failed
        is UserFacingError.LoadFailed -> R.string.error_load_failed
        is UserFacingError.UpdateFailed -> R.string.error_update_failed
        is UserFacingError.DeleteFailed -> R.string.error_delete_failed
        is UserFacingError.NetworkUnavailable -> R.string.error_network_unavailable
        is UserFacingError.UnexpectedError -> R.string.error_unexpected
        is UserFacingError.AmountRequired -> R.string.error_amount_required
        is UserFacingError.InvalidAmount -> R.string.error_invalid_amount
        is UserFacingError.DescriptionRequired -> R.string.error_description_required
        is UserFacingError.CategoryRequired -> R.string.error_category_required
        is UserFacingError.SaveFailedWithReason -> R.string.error_save_failed
        is UserFacingError.LoadFailedWithReason -> R.string.error_load_failed
        is UserFacingError.UpdateFailedWithReason -> R.string.error_update_failed
        is UserFacingError.DeleteFailedWithReason -> R.string.error_delete_failed
    }
}