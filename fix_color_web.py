import re
import glob

files = glob.glob('client/src/main/java/com/auction/client/controller/*.java')

for file_path in files:
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Fix Color.web("-fx-accent") and Color.valueOf("-fx-accent")
    content = content.replace('Color.web("-fx-accent")', 'Color.web("#e040a0")')
    content = content.replace('Color.valueOf("-fx-accent")', 'Color.valueOf("#e040a0")')
    
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(content)

print("Fixed Color.web crashes.")
