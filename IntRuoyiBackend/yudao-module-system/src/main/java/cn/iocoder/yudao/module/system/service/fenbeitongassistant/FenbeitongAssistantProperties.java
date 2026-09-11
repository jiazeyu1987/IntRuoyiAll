package cn.iocoder.yudao.module.system.service.fenbeitongassistant;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.nio.file.Files;
import java.nio.file.Path;

@Component
@ConfigurationProperties(prefix = "yudao.erp.fenbeitong-assistant")
@Validated
@Data
public class FenbeitongAssistantProperties implements InitializingBean {

    public static final int FIXED_ASSISTANT_PORT = 18734;

    private String assistantHost = "127.0.0.1";

    @Min(value = 1, message = "助手端口必须大于 0")
    private Integer assistantPort = FIXED_ASSISTANT_PORT;

    private String assistantRootDir = "E:/ProjectPackage/fenbeitong/student-collaboration-kit";

    private String assistantScript = "server.js";

    private String nodeCommand = "D:/Programs/node.exe";

    private String ticketValidateUrl = "http://127.0.0.1:48081/admin-api/system/auth/fenbeitong-assistant-ticket/validate";

    @Min(value = 1, message = "助手会话有效期必须大于 0")
    private Long sessionTtlSeconds = 14_400L;

    @Min(value = 1, message = "助手启动超时时间必须大于 0")
    private Integer startupTimeoutSeconds = 30;

    @Min(value = 1, message = "助手探测超时时间必须大于 0")
    private Integer probeTimeoutSeconds = 3;

    @Min(value = 1, message = "助手探测间隔必须大于 0")
    private Integer probeIntervalMillis = 1000;

    public String getAssistantBaseUrl() {
        if (assistantHost == null || assistantHost.isBlank() || assistantPort == null) {
            return "";
        }
        return "http://" + assistantHost.trim() + ":" + assistantPort;
    }

    public String getAssistantHealthUrl() {
        String baseUrl = getAssistantBaseUrl();
        return baseUrl.isBlank() ? "" : baseUrl + "/api/health";
    }

    public Path getAssistantRootDirectoryPath() {
        return Path.of(assistantRootDir).toAbsolutePath().normalize();
    }

    public Path getAssistantScriptPath() {
        Path scriptPath = Path.of(assistantScript);
        if (scriptPath.isAbsolute()) {
            return scriptPath.toAbsolutePath().normalize();
        }
        return getAssistantRootDirectoryPath().resolve(scriptPath).normalize();
    }

    @Override
    public void afterPropertiesSet() {
        if (assistantPort == null || assistantPort != FIXED_ASSISTANT_PORT) {
            throw new IllegalArgumentException("yudao.erp.fenbeitong-assistant.assistant-port must be fixed at 18734");
        }
        requirePositive(startupTimeoutSeconds, "startup-timeout-seconds");
        requirePositive(probeTimeoutSeconds, "probe-timeout-seconds");
        requirePositive(probeIntervalMillis, "probe-interval-millis");
        requirePositive(sessionTtlSeconds, "session-ttl-seconds");
    }

    private static void requirePositive(Number value, String property) {
        if (value == null || value.longValue() <= 0) {
            throw new IllegalArgumentException(
                    "yudao.erp.fenbeitong-assistant." + property + " must be greater than 0");
        }
    }

    public boolean isAssistantRootDirectoryAvailable() {
        return assistantRootDir != null && !assistantRootDir.isBlank()
                && Files.isDirectory(getAssistantRootDirectoryPath());
    }

    public boolean isAssistantScriptAvailable() {
        return assistantScript != null && !assistantScript.isBlank()
                && isAssistantRootDirectoryAvailable() && Files.isRegularFile(getAssistantScriptPath());
    }

    public boolean isNodeCommandAvailable() {
        if (nodeCommand == null || nodeCommand.isBlank()) {
            return false;
        }
        Path commandPath = Path.of(nodeCommand);
        return !commandPath.isAbsolute() || Files.isRegularFile(commandPath.toAbsolutePath().normalize());
    }

}
