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

    # Wrap auth-shell in auth-shadow-wrapper
    if 'fx:id="authShell"' not in content:
        # Replace <HBox styleClass="auth-shell"
        # with <StackPane styleClass="auth-shadow-wrapper"><HBox fx:id="authShell" styleClass="auth-shell"
        content = re.sub(
            r'<HBox\s+styleClass="auth-shell"\s+maxWidth="[^"]+"\s+maxHeight="[^"]+">',
            '<StackPane styleClass="auth-shadow-wrapper">\n        <HBox fx:id="authShell" styleClass="auth-shell" maxWidth="1050.0" maxHeight="680.0">\n            <clip>\n                <Rectangle arcWidth="64.0" arcHeight="64.0" width="${authShell.width}" height="${authShell.height}"/>\n            </clip>',
            content
        )
        
        # Add closing </StackPane> before the last </StackPane>
        content = re.sub(r'(</StackPane>\s*)$', r'    </StackPane>\n\1', content)

        with open(file, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f'Updated {file}')
