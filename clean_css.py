import re

file_path = 'client/src/main/java/com/auction/client/view/styles.css'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# Remove opacity rules inside major containers
content = re.sub(r'(\.(root|rounded-window-content|app-content-panel|content-area|settings-page|dashboard-page|support-page)[^\{]*\{[^}]*)-fx-opacity:\s*[\d\.]+;([^}]*\})', r'\1\3', content)

# Remove transparent background from rounded-window-content
# Wait, I previously had: .rounded-window-content { -fx-background-color: -app-bg; }
# Let's ensure it doesn't have transparent
content = re.sub(r'(\.rounded-window-content[^\{]*\{[^}]*)-fx-background-color:\s*transparent;([^}]*\})', r'\1\2', content)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)

print("styles.css cleaned of illegal transparent/opacity.")
