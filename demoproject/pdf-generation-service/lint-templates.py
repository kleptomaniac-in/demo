#!/usr/bin/env python3
"""
FreeMarker Template CSS Linter

Validates FreeMarker templates against CSS optimization best practices.
Can be run standalone or as part of pre-commit hook.

Usage:
    python lint-templates.py [file.ftl ...]
    python lint-templates.py --all  # Check all templates

Exit codes:
    0 - All checks passed
    1 - Linting errors found
    2 - Script error
"""

import re
import sys
from pathlib import Path
from typing import List, Tuple

# Optimization rules
RULES = {
    'position_fixed': {
        'pattern': r'position\s*:\s*fixed',
        'severity': 'ERROR',
        'message': 'position:fixed causes 10-30x performance penalty. Use margin-based layout instead.'
    },
    'verbose_margin': {
        'pattern': r'margin-top\s*:.*margin-bottom\s*:|margin-left\s*:.*margin-right\s*:',
        'severity': 'WARNING',
        'message': 'Use CSS shorthand: margin: 20px 0 instead of margin-top/margin-bottom separately.'
    },
    'verbose_padding': {
        'pattern': r'padding-top\s*:.*padding-bottom\s*:|padding-left\s*:.*padding-right\s*:',
        'severity': 'WARNING',
        'message': 'Use CSS shorthand: padding: 10px 5px instead of padding-top/padding-bottom separately.'
    },
    'background_color': {
        'pattern': r'background-color\s*:',
        'severity': 'INFO',
        'message': 'Consider using shorthand: background: #f5f5f5 instead of background-color.'
    },
    'large_css': {
        'pattern': None,  # Checked separately
        'severity': 'WARNING',
        'message': 'CSS block exceeds 50 lines. Consider using styles.ftl macros.'
    },
    'no_import': {
        'pattern': None,  # Checked separately
        'severity': 'INFO',
        'message': 'Template does not import styles.ftl. Consider using style macros for consistency.'
    }
}

class TemplateLinter:
    def __init__(self):
        self.errors = []
        self.warnings = []
        self.info = []
        
    def check_file(self, filepath: Path) -> List[Tuple[str, str, int]]:
        """Check a single template file. Returns list of (severity, message, line_number)."""
        issues = []
        
        try:
            content = filepath.read_text()
            lines = content.split('\n')
            
            # Extract CSS block
            css_start = None
            css_end = None
            for i, line in enumerate(lines):
                if '<style>' in line:
                    css_start = i
                elif '</style>' in line and css_start is not None:
                    css_end = i
                    break
            
            if css_start is None:
                # No CSS found, check if using imports
                if '<#import' not in content and 'styles.ftl' not in content:
                    issues.append(('INFO', RULES['no_import']['message'], 0))
                return issues
            
            css_block = '\n'.join(lines[css_start:css_end+1])
            css_lines = css_end - css_start
            
            # Check CSS size
            if css_lines > 50:
                issues.append(('WARNING', f'{RULES["large_css"]["message"]} ({css_lines} lines)', css_start))
            
            # Check for position:fixed (CRITICAL)
            if re.search(RULES['position_fixed']['pattern'], css_block, re.IGNORECASE):
                match_line = self._find_line_number(lines, RULES['position_fixed']['pattern'], css_start, css_end)
                issues.append(('ERROR', RULES['position_fixed']['message'], match_line))
            
            # Check for verbose margin
            if re.search(RULES['verbose_margin']['pattern'], css_block, re.IGNORECASE | re.DOTALL):
                match_line = self._find_line_number(lines, r'margin-top\s*:', css_start, css_end)
                issues.append(('WARNING', RULES['verbose_margin']['message'], match_line))
            
            # Check for verbose padding
            if re.search(RULES['verbose_padding']['pattern'], css_block, re.IGNORECASE | re.DOTALL):
                match_line = self._find_line_number(lines, r'padding-top\s*:', css_start, css_end)
                issues.append(('WARNING', RULES['verbose_padding']['message'], match_line))
            
            # Check for background-color
            matches = re.finditer(RULES['background_color']['pattern'], css_block, re.IGNORECASE)
            for match in matches:
                match_line = self._find_line_number(lines, RULES['background_color']['pattern'], css_start, css_end)
                issues.append(('INFO', RULES['background_color']['message'], match_line))
                break  # Only report once
            
        except Exception as e:
            issues.append(('ERROR', f'Failed to parse file: {e}', 0))
        
        return issues
    
    def _find_line_number(self, lines: List[str], pattern: str, start: int, end: int) -> int:
        """Find the line number where a pattern occurs within a range."""
        for i in range(start, end + 1):
            if re.search(pattern, lines[i], re.IGNORECASE):
                return i + 1
        return start + 1
    
    def lint_file(self, filepath: Path) -> bool:
        """Lint a file and collect issues. Returns True if no errors."""
        issues = self.check_file(filepath)
        has_errors = False
        
        for severity, message, line_num in issues:
            location = f"{filepath}:{line_num}" if line_num > 0 else str(filepath)
            
            if severity == 'ERROR':
                print(f"❌ {location}")
                print(f"   ERROR: {message}")
                self.errors.append((filepath, message, line_num))
                has_errors = True
            elif severity == 'WARNING':
                print(f"⚠️  {location}")
                print(f"   WARNING: {message}")
                self.warnings.append((filepath, message, line_num))
            elif severity == 'INFO':
                print(f"ℹ️  {location}")
                print(f"   INFO: {message}")
                self.info.append((filepath, message, line_num))
        
        return not has_errors
    
    def print_summary(self):
        """Print summary of linting results."""
        print("\n" + "="*60)
        print(f"Errors:   {len(self.errors)}")
        print(f"Warnings: {len(self.warnings)}")
        print(f"Info:     {len(self.info)}")
        print("="*60)
        
        if self.errors:
            print("\n🔴 ERRORS MUST BE FIXED:")
            for filepath, message, line_num in self.errors:
                print(f"  • {filepath}:{line_num} - {message}")
        
        if not self.errors and not self.warnings:
            print("\n✅ All templates follow CSS optimization best practices!")
        elif not self.errors:
            print("\n⚠️  No errors, but consider addressing warnings for better performance.")

def main():
    if len(sys.argv) < 2:
        print("Usage: python lint-templates.py <file.ftl ...> | --all")
        return 2
    
    linter = TemplateLinter()
    
    if '--all' in sys.argv:
        template_dir = Path('src/main/resources/templates')
        if not template_dir.exists():
            print(f"❌ Template directory not found: {template_dir}")
            return 2
        
        files = list(template_dir.rglob('*.ftl'))
        print(f"🔍 Checking {len(files)} templates...\n")
    else:
        files = [Path(f) for f in sys.argv[1:]]
    
    all_passed = True
    for filepath in files:
        if not filepath.exists():
            print(f"❌ File not found: {filepath}")
            all_passed = False
            continue
        
        if not linter.lint_file(filepath):
            all_passed = False
    
    linter.print_summary()
    
    return 0 if all_passed else 1

if __name__ == '__main__':
    sys.exit(main())
