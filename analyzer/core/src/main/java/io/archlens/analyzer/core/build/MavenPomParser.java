package io.archlens.analyzer.core.build;

import io.archlens.analyzer.core.model.Dependency;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser de POM somente local. Não resolve parent/BOM pela rede e não executa Maven.
 * XML é lido com DTD e entidades externas desabilitadas.
 */
public final class MavenPomParser {

    private static final Pattern PROPERTY = Pattern.compile("\\$\\{([^}]+)}");
    private static final int MAX_PROPERTY_DEPTH = 8;

    private MavenPomParser() {
    }

    public record PomParseResult(
            List<Dependency> dependencies,
            List<String> modules,
            Optional<String> javaVersionHint,
            List<ParseLimitation> limitations
    ) {
    }

    public static PomParseResult parse(String xml, String sourcePath) {
        if (xml == null || xml.isBlank()) {
            return empty("pom-empty", "POM vazio em " + sourcePath);
        }
        Document document;
        try {
            document = parseSecure(xml);
        } catch (Exception ex) {
            return empty("pom-unreadable", "POM não interpretado em " + sourcePath + " (XML rejeitado)");
        }

        Element project = document.getDocumentElement();
        if (project == null || !"project".equals(localName(project))) {
            return empty("pom-unreadable", "POM sem elemento project em " + sourcePath);
        }

        List<ParseLimitation> limitations = new ArrayList<>();
        Map<String, String> properties = new LinkedHashMap<>();
        String groupId = firstNonBlank(text(project, "groupId"), text(directChild(project, "parent"), "groupId"));
        String artifactId = text(project, "artifactId");
        String version = firstNonBlank(text(project, "version"), text(directChild(project, "parent"), "version"));
        putProjectProperty(properties, "project.groupId", groupId);
        putProjectProperty(properties, "project.artifactId", artifactId);
        putProjectProperty(properties, "project.version", version);
        putProjectProperty(properties, "pom.groupId", groupId);
        putProjectProperty(properties, "pom.artifactId", artifactId);
        putProjectProperty(properties, "pom.version", version);

        Element parent = directChild(project, "parent");
        if (parent != null) {
            String parentGroup = text(parent, "groupId");
            String parentArtifact = text(parent, "artifactId");
            String parentVersion = text(parent, "version");
            putProjectProperty(properties, "project.parent.groupId", parentGroup);
            putProjectProperty(properties, "project.parent.artifactId", parentArtifact);
            putProjectProperty(properties, "project.parent.version", parentVersion);
            limitations.add(new ParseLimitation(
                    "parent-not-fetched",
                    "Parent POM não baixado: " + coord(parentGroup, parentArtifact, parentVersion)
            ));
        }

        Element props = directChild(project, "properties");
        if (props != null) {
            for (Element child : elementChildren(props)) {
                properties.put(localName(child), child.getTextContent() == null ? "" : child.getTextContent().trim());
            }
        }

        Element dependencyManagement = directChild(project, "dependencyManagement");
        Element managedDependencies = dependencyManagement == null ? null : directChild(dependencyManagement, "dependencies");
        int managedCount = 0;
        if (managedDependencies != null) {
            for (Element managed : elementChildren(managedDependencies)) {
                if (!"dependency".equals(localName(managed))) {
                    continue;
                }
                managedCount++;
                String type = resolve(text(managed, "type"), properties, limitations, false);
                String scope = resolve(text(managed, "scope"), properties, limitations, false);
                if ("pom".equals(type) && "import".equals(scope)) {
                    limitations.add(new ParseLimitation(
                            "bom-not-resolved",
                            "BOM não resolvido localmente: " + dependencyCoordinates(managed, properties, limitations)
                    ));
                }
            }
        }
        if (managedCount > 0) {
            limitations.add(new ParseLimitation(
                    "dependency-management-not-declared",
                    "dependencyManagement não entra como dependência declarada (" + managedCount + " entradas)"
            ));
        }

        List<Dependency> dependencies = new ArrayList<>();
        Element dependenciesElement = directChild(project, "dependencies");
        if (dependenciesElement != null) {
            for (Element dependency : elementChildren(dependenciesElement)) {
                if (!"dependency".equals(localName(dependency))) {
                    continue;
                }
                String coordinates = dependencyCoordinates(dependency, properties, limitations);
                if (coordinates == null) {
                    limitations.add(new ParseLimitation(
                            "dependency-incomplete",
                            "Dependência Maven sem groupId/artifactId em " + sourcePath
                    ));
                    continue;
                }
                String scope = resolve(text(dependency, "scope"), properties, limitations, true);
                if (scope == null || scope.isBlank()) {
                    scope = "compile";
                }
                dependencies.add(new Dependency(coordinates, scope, sourcePath));
            }
        }

        List<String> modules = new ArrayList<>();
        Element modulesElement = directChild(project, "modules");
        if (modulesElement != null) {
            for (Element module : elementChildren(modulesElement)) {
                if (!"module".equals(localName(module)) || module.getTextContent() == null) {
                    continue;
                }
                String name = module.getTextContent().trim();
                if (!name.isEmpty()) {
                    modules.add(name);
                }
            }
        }

        String javaHint = normalizeJavaVersion(firstResolved(properties, limitations,
                "maven.compiler.release",
                "maven.compiler.source",
                "maven.compiler.target",
                "java.version"));

        return new PomParseResult(List.copyOf(dependencies), List.copyOf(modules), Optional.ofNullable(javaHint), List.copyOf(limitations));
    }

    static String normalizeJavaVersion(String raw) {
        if (raw == null || raw.isBlank() || raw.contains("${")) {
            return null;
        }
        String value = raw.trim();
        if (value.matches("1\\.\\d+.*")) {
            String minor = value.substring(2);
            int cut = 0;
            while (cut < minor.length() && Character.isDigit(minor.charAt(cut))) {
                cut++;
            }
            return cut == 0 ? null : minor.substring(0, cut);
        }
        int cut = 0;
        while (cut < value.length() && Character.isDigit(value.charAt(cut))) {
            cut++;
        }
        return cut == 0 ? null : value.substring(0, cut);
    }

    private static PomParseResult empty(String code, String message) {
        return new PomParseResult(List.of(), List.of(), Optional.empty(), List.of(new ParseLimitation(code, message)));
    }

    private static Document parseSecure(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        var builder = factory.newDocumentBuilder();
        builder.setErrorHandler(new DefaultHandler() {
            @Override
            public void fatalError(SAXParseException exception) throws SAXException {
                throw exception;
            }
        });
        builder.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));
        return builder.parse(new InputSource(new StringReader(xml)));
    }

    private static String dependencyCoordinates(Element dependency, Map<String, String> properties, List<ParseLimitation> limitations) {
        String groupId = resolve(text(dependency, "groupId"), properties, limitations, true);
        String artifactId = resolve(text(dependency, "artifactId"), properties, limitations, true);
        String version = resolve(text(dependency, "version"), properties, limitations, true);
        if (groupId == null || groupId.isBlank() || artifactId == null || artifactId.isBlank()) {
            return null;
        }
        return coord(groupId, artifactId, version);
    }

    private static String coord(String groupId, String artifactId, String version) {
        String group = groupId == null ? "" : groupId.trim();
        String artifact = artifactId == null ? "" : artifactId.trim();
        String ver = version == null ? "" : version.trim();
        return group + ":" + artifact + ":" + ver;
    }

    private static String firstResolved(Map<String, String> properties, List<ParseLimitation> limitations, String... keys) {
        for (String key : keys) {
            if (!properties.containsKey(key)) {
                continue;
            }
            String resolved = resolve(properties.get(key), properties, limitations, true);
            if (resolved != null && !resolved.isBlank() && !resolved.contains("${")) {
                return resolved;
            }
        }
        return null;
    }

    private static String resolve(String value, Map<String, String> properties, List<ParseLimitation> limitations, boolean recordUnresolved) {
        if (value == null) {
            return null;
        }
        String current = value.trim();
        for (int depth = 0; depth < MAX_PROPERTY_DEPTH && current.contains("${"); depth++) {
            Matcher matcher = PROPERTY.matcher(current);
            StringBuilder replaced = new StringBuilder();
            boolean changed = false;
            while (matcher.find()) {
                String key = matcher.group(1);
                String replacement = properties.get(key);
                if (replacement == null) {
                    matcher.appendReplacement(replaced, Matcher.quoteReplacement(matcher.group(0)));
                    if (recordUnresolved) {
                        String code = "unresolved-property";
                        String message = "Propriedade Maven não resolvida localmente: ${" + key + "}";
                        if (limitations.stream().noneMatch(item -> code.equals(item.code()) && message.equals(item.message()))) {
                            limitations.add(new ParseLimitation(code, message));
                        }
                    }
                } else {
                    changed = true;
                    matcher.appendReplacement(replaced, Matcher.quoteReplacement(replacement));
                }
            }
            matcher.appendTail(replaced);
            current = replaced.toString();
            if (!changed) {
                break;
            }
        }
        return current;
    }

    private static void putProjectProperty(Map<String, String> properties, String key, String value) {
        if (value != null && !value.isBlank()) {
            properties.put(key, value.trim());
        }
    }

    private static String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary.trim();
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback.trim();
        }
        return null;
    }

    private static String text(Element parent, String childName) {
        Element child = directChild(parent, childName);
        if (child == null || child.getTextContent() == null) {
            return null;
        }
        String value = child.getTextContent().trim();
        return value.isEmpty() ? null : value;
    }

    private static Element directChild(Element parent, String name) {
        if (parent == null) {
            return null;
        }
        for (Element child : elementChildren(parent)) {
            if (name.equals(localName(child))) {
                return child;
            }
        }
        return null;
    }

    private static List<Element> elementChildren(Element parent) {
        List<Element> children = new ArrayList<>();
        Node node = parent.getFirstChild();
        while (node != null) {
            if (node instanceof Element element) {
                children.add(element);
            }
            node = node.getNextSibling();
        }
        return children;
    }

    private static String localName(Element element) {
        String local = element.getLocalName();
        if (local != null && !local.isBlank()) {
            return local;
        }
        String name = element.getNodeName();
        int colon = name.indexOf(':');
        return colon >= 0 ? name.substring(colon + 1) : name.toLowerCase(Locale.ROOT);
    }
}
