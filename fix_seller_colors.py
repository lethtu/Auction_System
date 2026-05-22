import re

file_path = 'client/src/main/java/com/auction/client/view/seller_dashboard.css'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

replacements = [
    (r'-fx-text-fill:\s*#604868;', '-fx-text-fill: -app-text-muted;'),
    (r'-fx-text-fill:\s*#7b617e;', '-fx-text-fill: -app-text-muted;'),
    (r'-fx-text-fill:\s*#6d5574;', '-fx-text-fill: -app-text-muted;'),
    (r'-fx-text-fill:\s*#9b839f;', '-fx-text-fill: -app-text-muted;'),
    (r'-fx-text-fill:\s*#a088a8;', '-fx-text-fill: -app-text-muted;'),
    
    (r'-fx-text-fill:\s*#201427;', '-fx-text-fill: -app-text;'),
    (r'-fx-text-fill:\s*#39213d;', '-fx-text-fill: -app-text;'),
    
    (r'-fx-text-fill:\s*#b01873;', '-fx-text-fill: -fx-accent;'),
    (r'-fx-text-fill:\s*#a02070;', '-fx-text-fill: -fx-accent;'),
    (r'-fx-text-fill:\s*#e040a0;', '-fx-text-fill: -fx-accent;'),
    (r'-fx-text-fill:\s*#e23ea1;', '-fx-text-fill: -fx-accent;'),
    
    (r'-fx-fill:\s*#604868;', '-fx-fill: -app-text-muted;'),
    (r'-fx-fill:\s*#201427;', '-fx-fill: -app-text;'),
    
    (r'-fx-border-color:\s*#ead8e6;', '-fx-border-color: -app-border;'),
    (r'-fx-border-color:\s*#e8d4e4;', '-fx-border-color: -app-border;'),
    (r'-fx-border-color:\s*#e0d0dd;', '-fx-border-color: -app-border;'),
    (r'-fx-border-color:\s*#f2e8f2;', '-fx-border-color: -app-border;'),
    (r'-fx-border-color:\s*#e5d5e1;', '-fx-border-color: -app-border;'),
]

for old, new in replacements:
    content = re.sub(old, new, content)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)

print("seller_dashboard.css hardcoded colors replaced.")
