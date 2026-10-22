import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Offline formatting of the explicit G33-owned Java files only. */
class G33OwnedFormatter {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("EXACT_REPOSITORY_PATH_REQUIRED");
        Path root = Path.of(args[0]).toRealPath();
        StaticJavaParser.getParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);
        List<String> production = List.of("DccLegacyMaintenanceAuthAdapter", "DccLegacyMaintenanceGate",
                "DccLegacyMaintenanceExecutor", "DccLegacyNameRegistrationMaintenanceCommand",
                "DccLegacyNameRegistrationMaintenanceRunner");
        List<String> tests = List.of("DccLegacyMaintenanceEntryTest", "DccLegacyMaintenanceGateTest",
                "DccLegacyMaintenanceCommandTest", "DccLegacyMaintenanceKernelTest", "DccLegacyMaintenanceLoggingTest");
        for (String tree : List.of("main", "test")) {
            for (String name : tree.equals("main") ? production : tests) {
                Path source = root.resolve("IntRuoyiBackend/yudao-module-dcc/src/" + tree
                        + "/java/cn/iocoder/yudao/module/dcc/service/file/" + name + ".java");
                if (!source.toRealPath().startsWith(root)) throw new IllegalArgumentException("OWNED_PATH_ESCAPE");
                Files.writeString(source, StaticJavaParser.parse(source).toString(), StandardCharsets.UTF_8);
            }
        }
        System.out.println("FORMATTED_EXACT_5_PRODUCTION_5_TEST_FILES");
    }
}
