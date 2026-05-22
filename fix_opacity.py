import re

file_path = 'client/src/main/java/com/auction/client/controller/MainController.java'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('card.setOpacity(0.6);', '// card.setOpacity(0.6);')
content = content.replace('toast.setOpacity(0);', 'toast.setOpacity(0); // OK for animation')

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Removed opacity in MainController")
