package cn.iocoder.yudao.module.system.service.invoicevoucherprintassistant;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.nio.file.Files;
import java.nio.file.Path;

@Component
@ConfigurationProperties(prefix = "yudao.erp.invoice-voucher-print-assistant")
@Validated
@Data
public class InvoiceVoucherPrintAssistantProperties implements InitializingBean {

    public static final int FIXED_ASSISTANT_PORT = 18733;

    private String assistantHost = "127.0.0.1";

    @Min(value = 1, message = "助手端口必须大于 0")
    private Integer assistantPort = FIXED_ASSISTANT_PORT;

    private String assistantRootDir = "E:/ProjectPackage/erp-invoice-voucher-print-assistant";

    private String assistantScript = "server.js";

    private String nodeCommand = "D:/Programs/node.exe";

    private String ticketValidateUrl = "http://127.0.0.1:48081/admin-api/system/auth/invoice-voucher-print-ticket/validate";

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

    public Path getNodeCommandPath() {
        return Path.of(nodeCommand).toAbsolutePath().normalize();
    }

    @Override
    public void afterPropertiesSet() {
        if (assistantPort == null || assistantPort != FIXED_ASSISTANT_PORT) {
            throw new IllegalArgumentException("yudao.erp.invoice-voucher-print-assistant.assistant-port must be fixed at 18733");
        }
        if (startupTimeoutSeconds == null || startupTimeoutSeconds <= 0) {
            throw new IllegalArgumentException("yudao.erp.invoice-voucher-print-assistant.startup-timeout-seconds must be greater than 0");
        }
        if (probeTimeoutSeconds == null || probeTimeoutSeconds <= 0) {
            throw new IllegalArgumentException("yudao.erp.invoice-voucher-print-assistant.probe-timeout-seconds must be greater than 0");
        }
        if (probeIntervalMillis == null || probeIntervalMillis <= 0) {
            throw new IllegalArgumentException("yudao.erp.invoice-voucher-print-assistant.probe-interval-millis must be greater than 0");
        }
        if (sessionTtlSeconds == null || sessionTtlSeconds <= 0) {
            throw new IllegalArgumentException("yudao.erp.invoice-voucher-print-assistant.session-ttl-seconds must be greater than 0");
        }
    }

    public boolean isAssistantRootDirectoryAvailable() {
        return assistantRootDir != null && !assistantRootDir.isBlank() && Files.isDirectory(getAssistantRootDirectoryPath());
    }

    public boolean isAssistantScriptAvailable() {
        if (assistantScript == null || assistantScript.isBlank() || !isAssistantRootDirectoryAvailable()) {
            return false;
        }
        return Files.isRegularFile(getAssistantScriptPath());
    }

    public boolean isNodeCommandAvailable() {
        if (nodeCommand == null || nodeCommand.isBlank()) {
            return false;
        }
        Path commandPath = Path.of(nodeCommand);
        if (commandPath.isAbsolute()) {
            return Files.isRegularFile(commandPath.toAbsolutePath().normalize());
        }
        return true;
    }

}
