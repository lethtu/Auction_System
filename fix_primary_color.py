import re

for file_path in ['client/src/main/resources/com/auction/client/view/theme-light.css', 'client/src/main/resources/com/auction/client/view/theme-dark.css']:
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    content = re.sub(r'-app-primary:.*?;', '-app-primary: -fx-accent;', content)
    
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(content)

print("Updated theme-light and theme-dark to use -fx-accent")
