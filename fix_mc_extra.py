import re

file_path = 'client/src/main/java/com/auction/client/controller/MainController.java'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

replacements = [
    (r'#f2e8f2', '-app-surface-2'),
    (r'#ffd6ee', '-app-surface-2'),
    (r'#f4f4f4', '-app-surface-2'),
    (r'#dee2e6', '-app-border'),
    (r'#cccccc', '-app-border'),
    (r'#6c757d', '-app-border'),
    (r'#888888', '-app-text-muted'),
]

for old, new in replacements:
    content = content.replace(old, new)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)

print("MainController extra hardcoded colors replaced")
