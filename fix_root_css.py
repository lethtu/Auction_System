import re

file_path = 'client/src/main/java/com/auction/client/view/styles.css'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

content = re.sub(r'(\.rounded-window-root\s*\{[^\}]*)-fx-background-color:\s*transparent;', r'\1-fx-background-color: #fff7fc;', content)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)

print("styles.css rounded-window-root transparency removed.")
