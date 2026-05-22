import os, glob

for root, _, files in os.walk('client/src/main/java/com/auction/client/controller'):
    for file in files:
        if file.endswith('.java'):
            path = os.path.join(root, file)
            with open(path, 'r', encoding='utf-8') as f:
                content = f.read()
            new_content = content.replace('"Login.fxml", 400, 500', '"Login.fxml", 1100, 700')
            new_content = new_content.replace('"Login.fxml", 1000, 650', '"Login.fxml", 1100, 700')
            new_content = new_content.replace('"Login.fxml", 800, 500', '"Login.fxml", 1100, 700')
            new_content = new_content.replace('LOGIN_FXML, 400, 500', 'LOGIN_FXML, 1100, 700')
            if content != new_content:
                with open(path, 'w', encoding='utf-8') as f:
                    f.write(new_content)
                print(f'Updated {file}')
