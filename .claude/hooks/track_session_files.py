#!/usr/bin/env python3
"""
Session File Tracker
Tracks files modified during a Claude Code session for the Stop hook to use.
"""

import sys
import json
from pathlib import Path
from typing import Dict, Any, Optional


def parse_hook_input() -> Dict[str, Any]:
    """
    Parse the JSON input from the PostToolUse hook.
    """
    try:
        input_data = sys.stdin.read()
        if not input_data:
            return {}
        return json.loads(input_data)
    except (json.JSONDecodeError, Exception):
        # Silent failure to not interrupt workflow
        return {}


def extract_file_path(hook_data: Dict[str, Any]) -> Optional[str]:
    """
    Extract the file path from the hook data.
    """
    # Direct file_path in hook data
    file_path = hook_data.get("file_path")
    if file_path:
        return file_path
    
    # Check in params
    params = hook_data.get("params", {})
    file_path = params.get("file_path")
    if file_path:
        return file_path
    
    return None


def track_file(file_path: str) -> None:
    """
    Add a file to the session tracking file.
    """
    # Use relative path from script location
    script_dir = Path(__file__).parent.parent  # Goes up to .claude directory
    tracking_file = script_dir / ".session_files"
    
    try:
        # Read existing files
        existing_files = set()
        if tracking_file.exists():
            with open(tracking_file, 'r') as f:
                existing_files = set(line.strip() for line in f if line.strip())
        
        # Add the new file
        existing_files.add(file_path)
        
        # Write back the updated set
        with open(tracking_file, 'w') as f:
            for file in sorted(existing_files):
                f.write(f"{file}\n")
                
    except Exception:
        # Silent failure to not interrupt workflow
        pass


def main():
    """
    Main function that tracks modified files.
    """
    # Parse the hook input
    hook_data = parse_hook_input()
    
    # Extract the file path
    file_path = extract_file_path(hook_data)
    
    # Track the file if found
    if file_path:
        track_file(file_path)
    
    # Always exit successfully
    sys.exit(0)


if __name__ == "__main__":
    main()