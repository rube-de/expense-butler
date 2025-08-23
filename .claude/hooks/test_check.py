#!/usr/bin/env python3
"""
Test Check Hook Script
Checks if Kotlin files or test files were changed during the Claude Code session,
and triggers the android-test-runner agent if needed.
"""

import sys
import json
import os
from pathlib import Path
from typing import List, Set, Dict, Any


def parse_hook_input() -> Dict[str, Any]:
    """
    Parse the JSON input from the Stop hook.
    Stop hooks receive session information including files modified.
    """
    try:
        input_data = sys.stdin.read()
        if not input_data:
            return {}
        return json.loads(input_data)
    except (json.JSONDecodeError, Exception) as e:
        print(f"Debug: Error parsing hook input: {e}", file=sys.stderr)
        return {}


def get_session_changes() -> Set[str]:
    """
    Get list of files that were modified during this Claude Code session.
    The Stop hook provides this information in its input.
    """
    hook_data = parse_hook_input()
    
    # Try to extract files from session data
    changed_files = set()
    
    # The Stop hook might provide session files in different formats
    # Check for common patterns in the hook data
    if "files" in hook_data:
        changed_files.update(hook_data["files"])
    
    if "modified_files" in hook_data:
        changed_files.update(hook_data["modified_files"])
    
    # Also check for session data
    session = hook_data.get("session", {})
    if "files" in session:
        changed_files.update(session["files"])
    
    # If no session data available, fall back to checking a session tracking file
    if not changed_files:
        changed_files = read_session_tracking_file()
    
    return changed_files


def read_session_tracking_file() -> Set[str]:
    """
    Read from a session tracking file if one exists.
    This is a fallback mechanism if session data isn't in the hook input.
    """
    # Use relative path from script location
    script_dir = Path(__file__).parent.parent  # Goes up to .claude directory
    tracking_file = script_dir / ".session_files"
    if tracking_file.exists():
        try:
            with open(tracking_file, 'r') as f:
                return set(line.strip() for line in f if line.strip())
        except Exception:
            pass
    return set()


def is_test_related_file(file_path: str) -> bool:
    """
    Check if a file is test-related or could affect tests.
    This includes:
    - Test files (*Test.kt)
    - Kotlin source files (.kt, .kts)
    - Build configuration files (build.gradle.kts)
    """
    path = Path(file_path)
    
    # Check if it's a Kotlin file
    if path.suffix in [".kt", ".kts"]:
        return True
    
    # Check if it's a test file specifically
    if path.name.endswith("Test.kt"):
        return True
    
    # Check if it's in a test directory
    if "test" in path.parts or "androidTest" in path.parts:
        return True
    
    # Check if it's a build configuration file
    if path.name == "build.gradle.kts" or path.name == "settings.gradle.kts":
        return True
    
    return False


def categorize_changes(changed_files: Set[str]) -> dict:
    """
    Categorize changed files into different types.
    """
    categories = {
        "test_files": [],
        "source_files": [],
        "build_files": []
    }
    
    for file_path in changed_files:
        path = Path(file_path)
        
        # Skip if file doesn't exist or is not relevant
        if not is_test_related_file(file_path):
            continue
        
        # Categorize the file
        if "Test.kt" in file_path or "test" in path.parts or "androidTest" in path.parts:
            categories["test_files"].append(file_path)
        elif path.name.endswith(".gradle.kts"):
            categories["build_files"].append(file_path)
        elif path.suffix in [".kt", ".kts"]:
            categories["source_files"].append(file_path)
    
    return categories


def display_test_recommendation(categories: dict) -> None:
    """
    Display a recommendation to run tests based on the changes.
    """
    # Determine the severity and type of testing needed
    needs_full_test = bool(categories["build_files"])
    needs_unit_tests = bool(categories["source_files"] or categories["test_files"])
    
    if not needs_unit_tests and not needs_full_test:
        return
    
    # Create the recommendation message
    message = """
╔════════════════════════════════════════════════════════════╗
║  🧪 Android Test Verification Recommended                 ║
╚════════════════════════════════════════════════════════════╝

Changes detected that may affect tests:
"""
    
    if categories["source_files"]:
        message += f"\n📝 Source files modified: {len(categories['source_files'])}"
        for file in categories["source_files"][:3]:  # Show first 3
            message += f"\n   • {file}"
        if len(categories["source_files"]) > 3:
            message += f"\n   ... and {len(categories['source_files']) - 3} more"
    
    if categories["test_files"]:
        message += f"\n\n🧪 Test files modified: {len(categories['test_files'])}"
        for file in categories["test_files"][:3]:  # Show first 3
            message += f"\n   • {file}"
        if len(categories["test_files"]) > 3:
            message += f"\n   ... and {len(categories['test_files']) - 3} more"
    
    if categories["build_files"]:
        message += f"\n\n🔧 Build files modified: {len(categories['build_files'])}"
        for file in categories["build_files"]:
            message += f"\n   • {file}"
    
    message += """

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

To verify all tests still pass, run:

  Task agent: android-test-runner

This will:
  ✓ Run unit tests (./gradlew test)
  ✓ Run instrumented tests if device available
  ✓ Analyze test results and failures
  ✓ Generate coverage reports
  ✓ Provide debugging assistance for failures

"""
    
    if needs_full_test:
        message += "⚠️  Build files changed - full test suite recommended!\n"
    
    message += "════════════════════════════════════════════════════════════════\n"
    
    print(message)


def main():
    """
    Main function that coordinates the test check.
    """
    # Get session changes (files modified during this Claude Code session)
    changed_files = get_session_changes()
    
    if not changed_files:
        # No changes detected, exit silently
        sys.exit(0)
    
    # Categorize the changes
    categories = categorize_changes(changed_files)
    
    # Display recommendation if test-related files were changed
    if any(categories.values()):
        display_test_recommendation(categories)
    
    # Always exit successfully to not interrupt the workflow
    sys.exit(0)


if __name__ == "__main__":
    main()