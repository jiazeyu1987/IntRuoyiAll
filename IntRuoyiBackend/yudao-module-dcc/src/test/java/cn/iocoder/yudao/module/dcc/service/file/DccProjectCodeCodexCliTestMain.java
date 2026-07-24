package cn.iocoder.yudao.module.dcc.service.file;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

public final class DccProjectCodeCodexCliTestMain {

    private DccProjectCodeCodexCliTestMain() {
    }

    public static void main(String[] args) throws Exception {
        List<String> arguments = List.of(args);
        if (arguments.contains("hold-lock-child")) {
            Path lockFile = Path.of(arguments.get(arguments.indexOf("hold-lock-child") + 1));
            try (var ignored = Files.newOutputStream(lockFile, StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
                Thread.sleep(5000L);
            }
            return;
        }
        int execIndex = arguments.indexOf("exec");
        int approvalIndex = arguments.indexOf("--ask-for-approval");
        if (execIndex < 0) {
            System.err.println("error: missing exec subcommand");
            System.exit(2);
        }
        if (approvalIndex < 0 || approvalIndex > execIndex) {
            System.err.println("error: unexpected argument '--ask-for-approval' found");
            System.exit(2);
        }
        int outputIndex = arguments.indexOf("--output-last-message");
        if (outputIndex < 0 || outputIndex + 1 >= arguments.size()) {
            System.err.println("error: missing --output-last-message");
            System.exit(2);
        }
        String payload = arguments.contains("non-json")
                ? "not-json"
                : "{\"projectCodeId\":700,\"matchType\":\"PROJECT_CODE\",\"matchText\":\"CODE-A\"}";
        Path outputFile = Path.of(arguments.get(outputIndex + 1));
        Files.writeString(outputFile, payload, StandardCharsets.UTF_8);
        if (arguments.contains("exit-nonzero-after-output")) {
            System.err.println("warning: result was written before process exit");
            System.exit(1);
        }
        if (arguments.contains("hold-output")) {
            Path lockFile = outputFile.getParent().resolve("cleanup-lock.tmp");
            Files.writeString(lockFile, "lock", StandardCharsets.UTF_8);
            String javaBinary = Path.of(System.getProperty("java.home"), "bin",
                    isWindows() ? "java.exe" : "java").toString();
            new ProcessBuilder(javaBinary, "-cp", System.getProperty("java.class.path"),
                    DccProjectCodeCodexCliTestMain.class.getName(), "hold-lock-child", lockFile.toString())
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            Thread.sleep(300L);
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }
}
