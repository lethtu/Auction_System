import re
import glob
import os

# Find all java and fxml files
files = glob.glob('client/src/main/java/com/auction/client/**/*.java', recursive=True) + \
        glob.glob('client/src/main/java/com/auction/client/**/*.fxml', recursive=True)

pattern = re.compile(r'-fx-(background-color|text-fill|border-color|prompt-text-fill|fill):\s*-app-[a-zA-Z0-9\-]+;')

for file_path in files:
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    new_content = pattern.sub('', content)
    
    if new_content != content:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f"Removed inline -app- tokens from {os.path.basename(file_path)}")

print("Inline lookup clean up complete.")
