package com.auction.client.staticanalysis;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientViewResourceStaticTest {

    private static final Path MAIN_JAVA = Path.of("src/main/java");
    private static final Path MAIN_RESOURCES = Path.of("src/main/resources");
    private static final Path VIEW_DIR = MAIN_JAVA.resolve("com/auction/client/view");
    private static final List<Path> SOURCE_ROOTS = List.of(MAIN_JAVA, MAIN_RESOURCES);
    private static final Pattern CSS_URL = Pattern.compile("url\\((['\\\"]?)([^'\\\")]+)\\1\\)");

    @Test
    void viewResourcesLoadedByStyleManager_shouldExistOnSourceClasspath() {
        List<String> requiredResources = List.of(
                "com/auction/client/view/styles.css",
                "com/auction/client/view/seller_dashboard.css",
                "com/auction/client/view/theme-light.css",
                "com/auction/client/view/theme-dark.css",
                "com/auction/client/view/accent-pink.css",
                "com/auction/client/view/accent-purple.css",
                "com/auction/client/view/accent-emerald.css",
                "com/auction/client/view/accent-blue.css",
                "com/auction/client/view/accent-orange.css",
                "com/auction/client/view/global-tokens.css",
                "com/auction/client/view/fonts/DMSans-Variable.ttf",
                "com/auction/client/view/fonts/MaterialIcons-Regular.ttf",
                "com/auction/client/view/fonts/MaterialSymbolsOutlined.ttf",
                "com/auction/client/css/auth-theme.css"
        );

        List<String> missing = requiredResources.stream()
                .filter(resource -> SOURCE_ROOTS.stream().noneMatch(root -> Files.exists(root.resolve(resource))))
                .collect(Collectors.toList());

        assertTrue(missing.isEmpty(),
                "Required JavaFX style/font resources are missing from source classpath:\n"
                        + String.join("\n", missing));
    }

    @Test
    void fxmlStylesheetsAndIncludes_shouldPointToExistingSourceResources() throws Exception {
        List<String> failures = new ArrayList<>();

        for (Path fxmlFile : listFiles(VIEW_DIR, ".fxml")) {
            Document document;
            try {
                document = parseXml(fxmlFile);
            } catch (Exception e) {
                failures.add(fxmlFile + " cannot be parsed: " + e.getMessage());
                continue;
            }
            validateFxmlNode(fxmlFile, document.getDocumentElement(), failures);
        }

        assertTrue(failures.isEmpty(),
                "FXML resource wiring contains missing includes or stylesheets:\n"
                        + String.join("\n", failures));
    }

    @Test
    void cssUrlReferences_shouldPointToExistingSourceResources() throws Exception {
        List<String> failures = new ArrayList<>();

        for (Path sourceRoot : SOURCE_ROOTS) {
            for (Path cssFile : listFiles(sourceRoot, ".css")) {
                validateCssUrls(cssFile, failures);
            }
        }

        assertTrue(failures.isEmpty(),
                "CSS url(...) references point to missing resources:\n"
                        + String.join("\n", failures));
    }

    private static void validateFxmlNode(Path fxmlFile, Node node, List<String> failures) {
        NamedNodeMap attributes = node.getAttributes();
        if (attributes != null) {
            for (int i = 0; i < attributes.getLength(); i++) {
                Node attribute = attributes.item(i);
                String name = attribute.getNodeName();
                String value = attribute.getNodeValue();

                if ("source".equals(name) && isFxInclude(node)) {
                    assertExistingRelativeResource(fxmlFile, value, failures, "fx:include source");
                }

                if ("stylesheets".equals(name)) {
                    for (String stylesheet : splitStylesheetAttribute(value)) {
                        assertExistingRelativeResource(fxmlFile, stylesheet, failures, "FXML stylesheet");
                    }
                }
            }
        }

        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            validateFxmlNode(fxmlFile, children.item(i), failures);
        }
    }

    private static boolean isFxInclude(Node node) {
        return "fx:include".equals(node.getNodeName()) || "include".equals(node.getLocalName());
    }

    private static List<String> splitStylesheetAttribute(String stylesheets) {
        if (stylesheets == null || stylesheets.trim().isEmpty()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String token : stylesheets.split(",")) {
            String cleaned = token.trim();
            if (cleaned.startsWith("@")) {
                cleaned = cleaned.substring(1).trim();
            }
            if (!cleaned.isEmpty() && !isExternalResource(cleaned)) {
                result.add(cleaned);
            }
        }
        return result;
    }

    private static void validateCssUrls(Path cssFile, List<String> failures) throws IOException {
        String css = Files.readString(cssFile);
        Matcher matcher = CSS_URL.matcher(css);
        while (matcher.find()) {
            String reference = matcher.group(2).trim();
            if (!isExternalResource(reference)) {
                assertExistingRelativeResource(cssFile, reference, failures, "CSS url");
            }
        }
    }

    private static void assertExistingRelativeResource(
            Path ownerFile,
            String reference,
            List<String> failures,
            String referenceKind
    ) {
        if (reference == null || reference.trim().isEmpty() || isExternalResource(reference)) {
            return;
        }
        String cleaned = stripJavaFxAtPrefix(reference.trim());
        if (!existsOnAnySourceRoot(ownerFile, cleaned)) {
            failures.add(ownerFile + " has missing " + referenceKind + ": " + reference);
        }
    }

    private static String stripJavaFxAtPrefix(String reference) {
        return reference.startsWith("@") ? reference.substring(1).trim() : reference;
    }

    private static boolean existsOnAnySourceRoot(Path ownerFile, String reference) {
        Optional<Path> ownerRoot = sourceRootFor(ownerFile);
        if (ownerRoot.isEmpty()) {
            return Files.exists(ownerFile.getParent().resolve(reference).normalize());
        }

        Path resourcePath;
        if (reference.startsWith("/")) {
            resourcePath = Path.of(reference.substring(1)).normalize();
        } else {
            Path ownerDirectoryResourcePath = ownerRoot.get().relativize(ownerFile.getParent());
            resourcePath = ownerDirectoryResourcePath.resolve(reference).normalize();
        }

        for (Path sourceRoot : SOURCE_ROOTS) {
            if (Files.exists(sourceRoot.resolve(resourcePath))) {
                return true;
            }
        }
        return false;
    }

    private static Optional<Path> sourceRootFor(Path file) {
        Path normalizedFile = file.normalize();
        for (Path sourceRoot : SOURCE_ROOTS) {
            if (normalizedFile.startsWith(sourceRoot)) {
                return Optional.of(sourceRoot);
            }
        }
        return Optional.empty();
    }

    private static boolean isExternalResource(String value) {
        String lower = value.toLowerCase();
        return lower.startsWith("http:")
                || lower.startsWith("https:")
                || lower.startsWith("data:")
                || lower.startsWith("file:");
    }

    private static List<Path> listFiles(Path root, String suffix) throws IOException {
        if (!Files.exists(root)) {
            return List.of();
        }
        try (Stream<Path> paths = Files.walk(root)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(suffix))
                    .sorted()
                    .collect(Collectors.toList());
        }
    }

    private static Document parseXml(Path fxmlFile)
            throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        trySetFeature(factory, XMLConstants.FEATURE_SECURE_PROCESSING, true);
        trySetFeature(factory, "http://apache.org/xml/features/disallow-doctype-decl", true);
        trySetFeature(factory, "http://xml.org/sax/features/external-general-entities", false);
        trySetFeature(factory, "http://xml.org/sax/features/external-parameter-entities", false);
        return factory.newDocumentBuilder().parse(fxmlFile.toFile());
    }

    private static void trySetFeature(DocumentBuilderFactory factory, String feature, boolean value) {
        try {
            factory.setFeature(feature, value);
        } catch (ParserConfigurationException ignored) {
            // Some parsers do not support every hardening flag. This test only parses local FXML.
        }
    }
}
