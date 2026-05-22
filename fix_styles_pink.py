import re

file_path = 'client/src/main/java/com/auction/client/view/styles.css'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

replacements = [
    (r'-fx-background-color:\s*#fff3fb;', '-fx-background-color: -app-surface-2;'),
    (r'-fx-background-color:\s*#fffbfe;', '-fx-background-color: -app-surface;'),
    (r'-fx-background-color:\s*#fff3f9;', '-fx-background-color: -app-surface-2;'),
    (r'-fx-background-color:\s*#fff5fa;', '-fx-background-color: -app-surface-2;'),
    (r'-fx-background-color:\s*#fef7ff;', '-fx-background-color: -app-surface;'),
    (r'-fx-background-color:\s*#fff9fd;', '-fx-background-color: -app-surface;'),
    (r'-fx-background-color:\s*#fffaf3;', '-fx-background-color: -app-surface;'),
    (r'-fx-background-color:\s*#fff0f8;', '-fx-background-color: -app-surface-2;'),
    (r'-fx-background-color:\s*#f7eef7;', '-fx-background-color: -app-surface-2;'),
    (r'-fx-background-color:\s*#f3e5f3;', '-fx-background-color: -app-surface;'),
    
    (r'-fx-border-color:\s*#f5c7df;', '-fx-border-color: -app-border;'),
    (r'-fx-border-color:\s*#f5edf5;', '-fx-border-color: -app-border;'),
    (r'-fx-border-color:\s*#f4edf4;', '-fx-border-color: -app-border;'),
    (r'-fx-border-color:\s*#f3b6dc;', '-fx-border-color: -app-border;'),
    (r'-fx-border-color:\s*#f4ddeb;', '-fx-border-color: -app-border;'),
    (r'-fx-border-color:\s*#f7ddeb;', '-fx-border-color: -app-border;'),
]

for old, new in replacements:
    content = re.sub(old, new, content)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)

print("styles.css light pinks replaced.")
