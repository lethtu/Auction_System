package com.auction.client.controller;

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
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FxmlStaticSmokeTest {

    private static final Path FXML_DIR = Path.of("src/main/java/com/auction/client/view");
    private static final String FXML_NAMESPACE = "http://javafx.com/fxml/1";
    private static final Pattern HANDLER_REFERENCE = Pattern.compile("#[A-Za-z_$][A-Za-z0-9_$]*");
    private static final Pattern CSS_HEX_COLOR = Pattern.compile("#[0-9a-fA-F]{3,8}");

    @Test
    void allApplicationFxmlFiles_shouldDeclareExistingControllersAndValidHandlers() throws Exception {
        List<String> failures = new ArrayList<>();
        List<Path> fxmlFiles = listFxmlFiles();

        assertTrue(fxmlFiles.size() >= 15,
                "Expected at least the current 15 application FXML screens, but found " + fxmlFiles.size());

        for (Path fxmlFile : fxmlFiles) {
            validateFxmlFile(fxmlFile, failures);
        }

        assertTrue(failures.isEmpty(),
                "FXML smoke test found broken controller or event-handler wiring:\n"
                        + String.join("\n", failures));
    }

    private static List<Path> listFxmlFiles() throws IOException {
        try (Stream<Path> paths = Files.list(FXML_DIR)) {
            return paths
                    .filter(path -> path.getFileName().toString().endsWith(".fxml"))
                    .sorted()
                    .collect(Collectors.toList());
        }
    }

    private static void validateFxmlFile(Path fxmlFile, List<String> failures) {
        Document document;
        try {
            document = parseXml(fxmlFile);
        } catch (Exception e) {
            failures.add(fxmlFile + " cannot be parsed as XML: " + e.getMessage());
            return;
        }

        Element root = document.getDocumentElement();
        String controllerName = getControllerName(root);
        if (!hasText(controllerName)) {
            failures.add(fxmlFile + " is missing fx:controller on the root element.");
            return;
        }

        Class<?> controllerClass;
        try {
            controllerClass = Class.forName(
                    controllerName,
                    false,
                    Thread.currentThread().getContextClassLoader()
            );
        } catch (ClassNotFoundException e) {
            failures.add(fxmlFile + " references missing controller class: " + controllerName);
            return;
        }

        Set<String> handlerNames = new LinkedHashSet<>();
        collectHandlerNames(root, handlerNames);

        for (String handlerName : handlerNames) {
            if (!hasMethodNamed(controllerClass, handlerName)) {
                failures.add(fxmlFile + " references #" + handlerName
                        + " but " + controllerName + " has no method with that name.");
            }
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
            // Some XML parsers do not support every hardening flag. The test still parses local FXML only.
        }
    }

    private static String getControllerName(Element root) {
        String controllerName = root.getAttributeNS(FXML_NAMESPACE, "controller");
        if (hasText(controllerName)) {
            return controllerName.trim();
        }
        controllerName = root.getAttribute("fx:controller");
        return hasText(controllerName) ? controllerName.trim() : "";
    }

    private static void collectHandlerNames(Node node, Set<String> handlerNames) {
        NamedNodeMap attributes = node.getAttributes();
        if (attributes != null) {
            for (int i = 0; i < attributes.getLength(); i++) {
                Node attribute = attributes.item(i);
                String attributeName = attribute.getNodeName();
                String attributeValue = attribute.getNodeValue();
                if (isFxmlEventHandler(attributeName, attributeValue)) {
                    handlerNames.add(attributeValue.substring(1));
                }
            }
        }

        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            collectHandlerNames(children.item(i), handlerNames);
        }
    }

    private static boolean isFxmlEventHandler(String attributeName, String attributeValue) {
        if (!hasText(attributeName) || !hasText(attributeValue)) {
            return false;
        }
        String value = attributeValue.trim();
        return attributeName.startsWith("on")
                && HANDLER_REFERENCE.matcher(value).matches()
                && !CSS_HEX_COLOR.matcher(value).matches();
    }

    private static boolean hasMethodNamed(Class<?> controllerClass, String methodName) {
        Class<?> currentClass = controllerClass;
        while (currentClass != null) {
            for (Method method : currentClass.getDeclaredMethods()) {
                if (method.getName().equals(methodName)) {
                    return true;
                }
            }
            currentClass = currentClass.getSuperclass();
        }
        return false;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
