
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class BuildScript {

    private static final String conditionFilePath = "/src/main/java/top/dreamlike/panama/generator/marco/Condition.java";

    public static void main(String[] args) {
        String buildPath = args[0];
        String debug = args[1];
        System.out.println(STR."Build path: \{buildPath} debug: \{debug}");
        Files.write(Path.of(STR."\{buildPath}/\{conditionFilePath}"), template.formatted(debug).getBytes(StandardCharsets.UTF_8), StandardOpenOption.WRITE, StandardOpenOption.CREATE);
    }

    private static String template = """
            package top.dreamlike.panama.generator.marco;
            public class Condition {
                public static final boolean DEBUG = %s;
            }                  
            """;
}
