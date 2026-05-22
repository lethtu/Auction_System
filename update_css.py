import re

css_path = 'client/src/main/java/com/auction/client/view/styles.css'
with open(css_path, 'r', encoding='utf-8') as f:
    css = f.read()

# Make a backup
with open(css_path + '.bak', 'w', encoding='utf-8') as f:
    f.write(css)

# Replace all background and border colors in common classes to use the tokens
# We'll use regex to target the classes specified by the user:

replacements = {
    # backgrounds
    r'-fx-background-color:\s*linear-gradient\(to bottom right, #fffbff 0%, #fff6fb 52%, #fffaf6 100%\);': '-fx-background-color: -app-bg;',
    r'-fx-background-color:\s*rgba\(255, 251, 255, 0\.97\);': '-fx-background-color: -app-surface;',
    r'-fx-background-color:\s*rgba\(255, 255, 255, 0\.92\);': '-fx-background-color: -app-surface;',
    r'-fx-background-color:\s*linear-gradient\(to bottom right, rgba\(255, 251, 255, 0\.90\), rgba\(254, 247, 255, 0\.96\)\);': '-fx-background-color: -app-surface-2;',
    r'-fx-background-color:\s*rgba\(255, 255, 255, 0\.94\);': '-fx-background-color: -app-card;',
    r'-fx-background-color:\s*#ffffff;': '-fx-background-color: -app-surface;',
    r'-fx-background-color:\s*rgba\(255, 255, 255, 0\.98\);': '-fx-background-color: -app-surface;',
    
    # borders
    r'-fx-border-color:\s*rgba\(224, 64, 160, 0\.55\);': '-fx-border-color: -app-border;',
    r'-fx-border-color:\s*rgba\(224, 64, 160, 0\.75\);': '-fx-border-color: -app-border;',
    r'-fx-border-color:\s*#f6ddea;': '-fx-border-color: -app-border;',
    r'-fx-border-color:\s*#f3deea;': '-fx-border-color: -app-border;',
    r'-fx-border-color:\s*#f4dfeb;': '-fx-border-color: -app-border;',
    r'-fx-border-color:\s*#f1e1ec;': '-fx-border-color: -app-border;',
    r'-fx-border-color:\s*#f2d7e9;': '-fx-border-color: -app-border;',
    r'-fx-border-color:\s*#f2e8f2;': '-fx-border-color: -app-border;',
    
    # Text
    r'-fx-text-fill:\s*#2e1a28;': '-fx-text-fill: -app-text;',
    r'-fx-text-fill:\s*#28142d;': '-fx-text-fill: -app-text;',
    r'-fx-text-fill:\s*#654a67;': '-fx-text-fill: -app-text-muted;',
    r'-fx-text-fill:\s*#8c6f8f;': '-fx-text-fill: -app-text-muted;',
    r'-fx-text-fill:\s*#ac92ab;': '-fx-text-fill: -app-text-muted;',
    r'-fx-text-fill:\s*#705a73;': '-fx-text-fill: -app-text-muted;',
    r'-fx-text-fill:\s*#8f7a93;': '-fx-text-fill: -app-text-muted;',
    r'-fx-text-fill:\s*#9b85a0;': '-fx-text-fill: -app-text-muted;',
    r'-fx-text-fill:\s*#907898;': '-fx-text-fill: -app-text-muted;',
    r'-fx-text-fill:\s*#b08aad;': '-fx-text-fill: -app-text-muted;',
}

for old, new in replacements.items():
    css = re.sub(old, new, css)

# Fix .label and text classes to use variables
css = re.sub(r'(\.label, \.text, \.brand-title, \.page-title {\n.*?)-fx-text-fill:.*?(!important)?;(.*?})', r'\1-fx-text-fill: -app-text \2;\3', css, flags=re.DOTALL)
css = re.sub(r'(\.brand-subtitle, \.page-subtitle {\n.*?)-fx-text-fill:.*?(!important)?;(.*?})', r'\1-fx-text-fill: -app-text-muted \2;\3', css, flags=re.DOTALL)

with open(css_path, 'w', encoding='utf-8') as f:
    f.write(css)

print("Replaced colors with tokens.")
