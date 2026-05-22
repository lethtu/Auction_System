import os
import re

files_to_patch = [
    'client/src/main/java/com/auction/client/view/Login.fxml',
    'client/src/main/java/com/auction/client/view/SignUp.fxml',
    'client/src/main/java/com/auction/client/view/ForgotPassword.fxml'
]

for file in files_to_patch:
    if not os.path.exists(file): continue
    with open(file, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Remove the Maximize button block
    new_content = re.sub(r'\s*<Button\s+onAction=\"#handleMaximize\"[^>]*>.*?</Button>', '', content, flags=re.DOTALL)
    
    if content != new_content:
        with open(file, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f'Updated {file}')
