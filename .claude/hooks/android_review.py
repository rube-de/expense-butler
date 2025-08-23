#!/usr/bin/env python3
"""
Android Review Hook Script
Checks if Kotlin files (.kt or .kts) were edited or created,
and triggers the android-best-practice-reviewer agent if needed.
"""

import sys
import json
import subprocess
from pathlib import Path
from typing import List, Dict, Any, Optional


def parse_hook_input() -> Dict[str, Any]:
    """
    Parse the JSON input from the hook.
    The hook provides information about the tool that was used and its results.
    """
    try:
        # Read from stdin (hooks pass data via stdin)
        input_data = sys.stdin.read()
        if not input_data:
            return {}
        return json.loads(input_data)
    except (json.JSONDecodeError, Exception) as e:
        # Log to stderr for debugging, but don't interrupt workflow
        print(f"Debug: Error parsing hook input: {e}", file=sys.stderr)
        return {}


def extract_file_path(hook_data: Dict[str, Any]) -> Optional[Path]:
    """
    Extract the file path that was modified from the hook data.
    PostToolUse hooks receive data in a specific format.
    """
    # The hook data structure for PostToolUse includes the file_path directly
    file_path = hook_data.get("file_path")
    if file_path:
        return Path(file_path)
    
    # Also check in params as a fallback
    params = hook_data.get("params", {})
    file_path = params.get("file_path")
    if file_path:
        return Path(file_path)
    
    return None


def is_kotlin_file(file_path: Path) -> bool:
    """
    Check if a file is a Kotlin source file (.kt or .kts).
    """
    return file_path.suffix in [".kt", ".kts"]


def get_kotlin_files_in_project() -> List[Path]:
    """
    Get all Kotlin files that were recently modified in the project.
    This is a fallback method if we can't extract from hook data.
    """
    project_root = Path(__file__).parent
    kotlin_files = []
    
    # Search for .kt and .kts files in the project
    for pattern in ["**/*.kt", "**/*.kts"]:
        kotlin_files.extend(project_root.glob(pattern))
    
    return kotlin_files


def trigger_android_reviewer(file_path: Path) -> None:
    """
    Trigger the android-best-practice-reviewer agent by displaying a notification.
    """
    # Create a notification message
    message = f"""
╔════════════════════════════════════════════════════════════╗
║  🤖 Android Best Practice Review Recommended              ║
╚════════════════════════════════════════════════════════════╝

Kotlin file modified: {file_path}

To review this file for Android best practices, run:
  
  Task agent: android-best-practice-reviewer
  
This will analyze the code for:
  • Android architecture patterns
  • Kotlin best practices
  • Performance considerations
  • Memory management
  • UI/UX guidelines

════════════════════════════════════════════════════════════════
"""
    print(message)
    
    # Don't exit with error code as that would interrupt the workflow
    # Just provide the recommendation


def main():
    """
    Main function that coordinates the review check.
    """
    # Parse the hook input
    hook_data = parse_hook_input()
    
    # Extract the modified file path from the hook data
    file_path = extract_file_path(hook_data)
    
    # Check if a Kotlin file was modified
    if file_path and is_kotlin_file(file_path):
        trigger_android_reviewer(file_path)
    
    # Always exit successfully to not interrupt the workflow
    sys.exit(0)


if __name__ == "__main__":
    main()