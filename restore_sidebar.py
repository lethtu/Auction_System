import re
import glob

# Re-add -fx-text-fill for sidebar buttons in Sidebar.fxml that lost their text fill
# Buttons in sidebar had -fx-text-fill: -app-text-muted removed

files = glob.glob('client/src/main/java/com/auction/client/view/Sidebar.fxml')

for file_path in files:
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Find button inline styles that have -fx-background-color: transparent but no text-fill
    # Add -fx-text-fill: -app-text-muted back
    def add_text_fill(m):
        style = m.group(0)
        if '-fx-text-fill' not in style:
            # Insert text-fill before the closing quote
            style = style.rstrip('\"') + '-fx-text-fill: -app-text-muted; \"'
        return style
    
    # Look for style attributes containing transparent background (sidebar nav buttons)
    content = re.sub(r'style=\"([^\"]*-fx-background-color:\s*transparent[^\"]*?)\"', 
                     lambda m: 'style=\"' + m.group(1) + '-fx-text-fill: -app-text-muted; \"' 
                     if '-fx-text-fill' not in m.group(1) else m.group(0), 
                     content)
    
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(content)
    print(f'Fixed {file_path}')

print('Done restoring sidebar text fills.')
