import re
import glob

files = glob.glob('client/src/main/java/com/auction/client/controller/*.java')

for file_path in files:
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Replace hardcoded primary color #e040a0 with -fx-accent
    new_content = content.replace('#e040a0', '-fx-accent')
    # Replace #604868 with -app-text-muted
    new_content = new_content.replace('#604868', '-app-text-muted')
    
    if new_content != content:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(new_content)
            print(f"Updated accent in {file_path}")

print("Replaced #e040a0 with -fx-accent in controllers")
