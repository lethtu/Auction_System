import re

file_path = 'client/src/main/java/com/auction/client/view/styles.css'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# For lines with white or #ffffff that are backgrounds, let's replace them carefully if they are major UI components.
# I'll just replace specific classes:
content = re.sub(r'(\.(account-form-card|account-stat-card|admin-sidebar|admin-table-row-cell|admin-table|search-shell|icon-btn)[\s\S]*?)-fx-background-color:\s*(white|#ffffff);', r'\1-fx-background-color: -app-card;', content)
content = re.sub(r'(\.profile-menu-btn[\s\S]*?)-fx-background-color:\s*(white|#ffffff);', r'\1-fx-background-color: -app-surface;', content)

# Replace gradients containing #ffffff
content = re.sub(r'-fx-background-color:\s*linear-gradient\(to bottom right, #ffffff, #fff7fc\);', '-fx-background-color: -app-surface;', content)
content = re.sub(r'-fx-background-color:\s*linear-gradient\(to bottom right, #ffffff, #fff0f8\);', '-fx-background-color: -app-surface;', content)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)

print("styles.css backgrounds replaced.")
