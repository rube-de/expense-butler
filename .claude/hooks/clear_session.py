#!/usr/bin/env python3
"""
Clear Session Tracker
Clears the session file tracking at the start of a new Claude Code session.
"""

import sys
from pathlib import Path


def clear_tracking_file() -> None:
    """
    Clear or create an empty session tracking file.
    """
    # Use relative path from script location
    script_dir = Path(__file__).parent.parent  # Goes up to .claude directory
    tracking_file = script_dir / ".session_files"
    
    try:
        # Create an empty file (overwrites if exists)
        with open(tracking_file, 'w') as f:
            f.write("")
    except Exception:
        # Silent failure to not interrupt workflow
        pass


def main():
    """
    Main function that clears the session tracking.
    """
    clear_tracking_file()
    
    # Always exit successfully
    sys.exit(0)


if __name__ == "__main__":
    main()