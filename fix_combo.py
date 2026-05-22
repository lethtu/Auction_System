for file_path in ['client/src/main/resources/com/auction/client/view/theme-dark.css', 'client/src/main/resources/com/auction/client/view/theme-light.css']:
    with open(file_path, 'a', encoding='utf-8') as f:
        prefix = '.theme-dark' if 'dark' in file_path else '.theme-light'
        f.write(f"\n{prefix} .combo-box .list-cell {{ -fx-text-fill: -app-input-text; }}\n")
print("ComboBox cell text fill appended.")
