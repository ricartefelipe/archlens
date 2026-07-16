package io.archlens.analyzer.core.java;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class JavaSourceParser {

    private static final Pattern PACKAGE = Pattern.compile("^\\s*package\\s+([\\w.]+)\\s*;", Pattern.MULTILINE);
    private static final Pattern IMPORT = Pattern.compile("^\\s*import\\s+(?:static\\s+)?([\\w.*]+)\\s*;", Pattern.MULTILINE);
    private static final Pattern TYPE_DECL = Pattern.compile(
            "(?:public\\s+|protected\\s+|private\\s+|abstract\\s+|final\\s+|static\\s+)*"
                    + "(class|interface|enum|@interface|record)\\s+(\\w+)",
            Pattern.MULTILINE
    );
    private static final Pattern ANNOTATION = Pattern.compile("@(\\w+(?:\\.\\w+)*)");

    private JavaSourceParser() {
    }

    public static Optional<String> packageName(String content) {
        Matcher matcher = PACKAGE.matcher(content);
        if (matcher.find()) {
            return Optional.of(matcher.group(1));
        }
        return Optional.empty();
    }

    public static List<String> imports(String content) {
        List<String> result = new ArrayList<>();
        Matcher matcher = IMPORT.matcher(content);
        while (matcher.find()) {
            result.add(matcher.group(1));
        }
        return result;
    }

    public static List<String> annotations(String content) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        Matcher matcher = ANNOTATION.matcher(content);
        while (matcher.find()) {
            result.add(matcher.group(1));
        }
        return List.copyOf(result);
    }

    public static Optional<TypeDeclaration> primaryType(String content) {
        Matcher matcher = TYPE_DECL.matcher(content);
        if (matcher.find()) {
            String kind = matcher.group(1).toUpperCase(Locale.ROOT);
            String name = matcher.group(2);
            return Optional.of(new TypeDeclaration(mapKind(kind), name));
        }
        return Optional.empty();
    }

    public static int lineCount(String content) {
        if (content == null || content.isEmpty()) {
            return 0;
        }
        return content.split("\\R", -1).length;
    }

    private static String mapKind(String kind) {
        return switch (kind) {
            case "CLASS" -> "CLASS";
            case "INTERFACE" -> "INTERFACE";
            case "ENUM" -> "ENUM";
            case "@INTERFACE" -> "ANNOTATION";
            case "RECORD" -> "RECORD";
            default -> "UNKNOWN";
        };
    }

    public record TypeDeclaration(String kind, String simpleName) {
    }
}
