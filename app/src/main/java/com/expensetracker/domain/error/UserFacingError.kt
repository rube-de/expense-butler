package com.expensetracker.domain.error

/**
 * Represents user-facing errors that can be translated and shown in the UI.
 * Technical details are logged separately while user sees localized, friendly messages.
 */
sealed class UserFacingError {
    object SaveFailed : UserFacingError()
    object LoadFailed : UserFacingError()
    object UpdateFailed : UserFacingError()
    object DeleteFailed : UserFacingError()
    object NetworkUnavailable : UserFacingError()
    object UnexpectedError : UserFacingError()
    
    // Validation errors
    object AmountRequired : UserFacingError()
    object InvalidAmount : UserFacingError()
    object DescriptionRequired : UserFacingError()
    object CategoryRequired : UserFacingError()
    
    // Error with additional context (for logging)
    data class SaveFailedWithReason(val technicalReason: String) : UserFacingError()
    data class LoadFailedWithReason(val technicalReason: String) : UserFacingError()
    data class UpdateFailedWithReason(val technicalReason: String) : UserFacingError()
    data class DeleteFailedWithReason(val technicalReason: String) : UserFacingError()
}