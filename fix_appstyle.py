import re

file_path = 'client/src/main/java/com/auction/client/service/AppStyleManager.java'
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# Replace updateRootStyleClass to handle accent classes
replacement = '''    public static void updateRootStyleClass(Parent root) {
        if (root == null) return;
        SettingsService settings = SettingsService.getInstance();
        String theme = settings.getTheme().toLowerCase();
        String colorName = settings.getPrimaryColor().toLowerCase();
        
        root.getStyleClass().remove("theme-light");
        root.getStyleClass().remove("theme-dark");
        root.getStyleClass().remove("accent-pink");
        root.getStyleClass().remove("accent-purple");
        root.getStyleClass().remove("accent-emerald");
        root.getStyleClass().remove("accent-blue");
        root.getStyleClass().remove("accent-orange");
        
        if (theme.contains("dark")) {
            root.getStyleClass().add("theme-dark");
        } else {
            root.getStyleClass().add("theme-light");
        }
        
        String accentClass = "accent-pink";
        if (colorName.contains("purple")) {
            accentClass = "accent-purple";
        } else if (colorName.contains("emerald") || colorName.contains("green")) {
            accentClass = "accent-emerald";
        } else if (colorName.contains("blue")) {
            accentClass = "accent-blue";
        } else if (colorName.contains("orange")) {
            accentClass = "accent-orange";
        }
        root.getStyleClass().add(accentClass);
        
        logger.debug("Current Root Style Classes: {}", root.getStyleClass());
    }'''

content = re.sub(r'public static void updateRootStyleClass\(Parent root\) \{.*?(?=private static void addStylesheet)', replacement + '\n\n    ', content, flags=re.DOTALL)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
print("Updated AppStyleManager")
