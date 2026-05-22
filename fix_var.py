import re

file_path = 'client/src/main/java/com/auction/client/view/styles.css'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# Replace var(-...) with just -...
content = re.sub(r'var\((-app-[^)]+)\)', r'\1', content)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)

print("styles.css var() syntax fixed.")
