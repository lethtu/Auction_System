import re
import glob

files = glob.glob('client/src/main/java/com/auction/client/view/*.fxml')

replacements = [
    (r'-fx-background-color:\s*white;', '-fx-background-color: -app-surface;'),
]

for file_path in files:
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    new_content = content
    for old, new in replacements:
        new_content = re.sub(old, new, new_content)
        
    if new_content != content:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(new_content)
            print(f"Updated {file_path}")

print("FXML white replacement complete.")
