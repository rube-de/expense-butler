package com.expensetracker.domain.validation

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TagValidator @Inject constructor() : Validator<String> {
    
    companion object {
        private const val MIN_TAG_LENGTH = 1
        private const val MAX_TAG_LENGTH = 50
        private const val MAX_TAGS_COUNT = 10
        private val INVALID_CHARACTERS = Regex("[\\s,;]")
    }
    
    override fun validate(value: String): ValidationResult {
        val trimmedValue = value.trim()
        
        return when {
            trimmedValue.isBlank() -> ValidationResult.Error("Tag cannot be empty")
            trimmedValue.length < MIN_TAG_LENGTH -> ValidationResult.Error("Tag is too short")
            trimmedValue.length > MAX_TAG_LENGTH -> ValidationResult.Error("Tag is too long (max $MAX_TAG_LENGTH characters)")
            INVALID_CHARACTERS.containsMatchIn(trimmedValue) -> ValidationResult.Error("Tag cannot contain spaces, commas, or semicolons")
            else -> ValidationResult.Success
        }
    }
    
    fun validateTagList(tags: List<String>): ValidationResult {
        return when {
            tags.size > MAX_TAGS_COUNT -> ValidationResult.Error("Too many tags (max $MAX_TAGS_COUNT)")
            tags.any { it.isBlank() } -> ValidationResult.Error("Tags cannot be empty")
            tags.distinct().size != tags.size -> ValidationResult.Error("Duplicate tags are not allowed")
            else -> {
                // Validate each individual tag
                tags.forEach { tag ->
                    val validation = validate(tag)
                    if (validation.isError) {
                        return validation
                    }
                }
                ValidationResult.Success
            }
        }
    }
    
    fun filterTagInput(input: String): String {
        // Remove spaces and other invalid characters, convert to lowercase
        return input.replace(INVALID_CHARACTERS, "").lowercase()
    }
    
    fun addTagToList(tag: String, currentTags: List<String>): Pair<List<String>, ValidationResult> {
        val trimmedTag = tag.trim()
        
        // Validate the new tag
        val tagValidation = validate(trimmedTag)
        if (tagValidation.isError) {
            return currentTags to tagValidation
        }
        
        // Check if tag already exists
        if (currentTags.contains(trimmedTag)) {
            return currentTags to ValidationResult.Error("Tag '$trimmedTag' already exists")
        }
        
        val newTagList = currentTags + trimmedTag
        
        // Validate the complete list
        val listValidation = validateTagList(newTagList)
        return if (listValidation.isValid) {
            newTagList to ValidationResult.Success
        } else {
            currentTags to listValidation
        }
    }
}