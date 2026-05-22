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
    
    # Check if already patched
    if 'fx:id="authBlobContainer"' in content:
        continue

    new_content = content.replace(
        '<AnchorPane mouseTransparent="true">',
        '''<AnchorPane mouseTransparent="true" fx:id="authBlobContainer">
                <clip>
                    <Rectangle arcWidth="64.0" arcHeight="64.0" width="${authBlobContainer.width}" height="${authBlobContainer.height}"/>
                </clip>'''
    )
    
    if content != new_content:
        with open(file, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f'Updated {file}')
