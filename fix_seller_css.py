import re

file_path = 'client/src/main/java/com/auction/client/view/seller_dashboard.css'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

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

for old, new in replacements:
    content = re.sub(old, new, content)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Seller Dashboard styles replaced.")
