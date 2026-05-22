import re
import glob

files = glob.glob('client/src/main/java/com/auction/client/controller/*.java')

replacements = [
    (r'-fx-background-color:\s*#ffffff;', '-fx-background-color: -app-card;'),
    (r'-fx-background-color:\s*#fafafa;', '-fx-background-color: -app-surface-2;'),
    (r'-fx-border-color:\s*#ffe8e8;', '-fx-border-color: -app-border;'),
    (r'-fx-border-color:\s*#f2e8f2;', '-fx-border-color: -app-border;'),
    (r'-fx-text-fill:\s*#2e1a28;', '-fx-text-fill: -app-text;'),
    (r'-fx-text-fill:\s*#604868;', '-fx-text-fill: -app-text-muted;'),
    (r'-fx-text-fill:\s*#907898;', '-fx-text-fill: -app-text-muted;'),
    (r'-fx-background-color:\s*white;', '-fx-background-color: -app-card;'),
    (r'-fx-border-color:\s*#f0e6f0;', '-fx-border-color: -app-border;'),
]

for file_path in files:
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    new_content = content
    for old, new in replacements:
        new_content = re.sub(old, new, new_content)
        
    if new_content != content:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(new_content)
            print(f"Updated {file_path}")

print("Controller Java styles replaced.")
