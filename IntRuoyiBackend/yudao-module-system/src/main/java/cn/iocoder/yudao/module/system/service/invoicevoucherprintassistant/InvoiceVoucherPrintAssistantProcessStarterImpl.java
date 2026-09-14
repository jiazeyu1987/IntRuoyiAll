package cn.iocoder.yudao.module.system.service.invoicevoucherprintassistant;

import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception0;
import static cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants.BAD_REQUEST;
import static cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants.INTERNAL_SERVER_ERROR;

@Component
public class InvoiceVoucherPrintAssistantProcessStarterImpl implements InvoiceVoucherPrintAssistantProcessStarter {

    private final InvoiceVoucherPrintAssistantProperties properties;

    public InvoiceVoucherPrintAssistantProcessStarterImpl(InvoiceVoucherPrintAssistantProperties properties) {
        this.properties = properties;
    }

    @Override
    public Process start() {
        validateLaunchConfiguration();
        Path rootDir = properties.getAssistantRootDirectoryPath();
        Path scriptPath = properties.getAssistantScriptPath();

        List<String> command = new ArrayList<>();
        command.add(properties.getNodeCommand());
        command.add(scriptPath.toString());

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(rootDir.toFile());
        processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
        processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);

        Map<String, String> environment = processBuilder.environment();
        environment.put("INVOICE_RENAMER_HOST", properties.getAssistantHost().trim());
        environment.put("INVOICE_RENAMER_PORT", String.valueOf(properties.getAssistantPort()));
        environment.put("INVOICE_VOUCHER_PRINT_TICKET_VALIDATE_URL", properties.getTicketValidateUrl().trim());
        environment.put("INVOICE_VOUCHER_PRINT_SESSION_TTL_SECONDS", String.valueOf(properties.getSessionTtlSeconds()));

        try {
            return processBuilder.start();
        } catch (IOException ex) {
            throw exception0(INTERNAL_SERVER_ERROR.getCode(), "启动发票凭证打印助手失败：" + ex.getMessage());
        }
    }

    private void validateLaunchConfiguration() {
        List<String> blockers = new ArrayList<>();
        if (StrUtil.isBlank(properties.getAssistantHost())) {
            blockers.add("assistant-host");
        }
        if (properties.getAssistantPort() == null || properties.getAssistantPort() <= 0 || properties.getAssistantPort() > 65535) {
            blockers.add("assistant-port");
        }
        if (StrUtil.isBlank(properties.getAssistantRootDir())) {
            blockers.add("assistant-root-dir");
        } else if (!Files.isDirectory(properties.getAssistantRootDirectoryPath())) {
            blockers.add("assistant-root-dir=" + properties.getAssistantRootDirectoryPath());
        }
        if (StrUtil.isBlank(properties.getAssistantScript())) {
            blockers.add("assistant-script");
        } else if (!Files.isRegularFile(properties.getAssistantScriptPath())) {
            blockers.add("assistant-script=" + properties.getAssistantScriptPath());
        }
        if (StrUtil.isBlank(properties.getNodeCommand())) {
            blockers.add("node-command");
        } else {
            Path nodeCommandPath = Path.of(properties.getNodeCommand());
            if (nodeCommandPath.isAbsolute() && !Files.isRegularFile(nodeCommandPath.toAbsolutePath().normalize())) {
                blockers.add("node-command=" + nodeCommandPath.toAbsolutePath().normalize());
            }
        }
        if (StrUtil.isBlank(properties.getTicketValidateUrl())) {
            blockers.add("ticket-validate-url");
        }
        if (!blockers.isEmpty()) {
            throw exception0(BAD_REQUEST.getCode(), "发票凭证打印助手启动配置缺失：" + String.join("，", blockers));
        }
    }

}
