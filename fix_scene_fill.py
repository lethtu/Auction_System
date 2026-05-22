import re

file_path = 'client/src/main/java/com/auction/client/Main.java'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('scene.setFill(Color.TRANSPARENT);', 'scene.setFill(Color.web("#fff7fc"));')

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)

file_path = 'client/src/main/java/com/auction/client/controller/SceneSwitcher.java'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('scene.setFill(javafx.scene.paint.Color.TRANSPARENT);', 'scene.setFill(javafx.scene.paint.Color.web("#fff7fc"));')

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Scene fill transparency removed.")
