package cn.iocoder.yudao.module.system.service.invoicevoucherprintassistant;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.system.controller.admin.auth.vo.AuthInvoiceVoucherPrintAssistantStatusRespVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception0;
import static cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants.BAD_REQUEST;
import static cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants.INTERNAL_SERVER_ERROR;

@Service
public class InvoiceVoucherPrintAssistantServiceImpl implements InvoiceVoucherPrintAssistantService {

    @Resource
    private InvoiceVoucherPrintAssistantProperties properties;
    @Resource
    private InvoiceVoucherPrintAssistantHealthProbe healthProbe;
    @Resource
    private InvoiceVoucherPrintAssistantProcessStarter processStarter;

    @Override
    public AuthInvoiceVoucherPrintAssistantStatusRespVO getStatus() {
        if (healthProbe.isRunning()) {
            return buildStatus(true, true, "发票凭证打印助手已启动");
        }
        List<String> blockers = collectLaunchBlockers();
        if (!blockers.isEmpty()) {
            return buildStatus(false, false, "发票凭证打印助手启动配置缺失：" + String.join("，", blockers));
        }
        return buildStatus(false, true, "发票凭证打印助手尚未启动，请点击启动助手。");
    }

    @Override
    public synchronized AuthInvoiceVoucherPrintAssistantStatusRespVO start() {
        if (healthProbe.isRunning()) {
            return buildStatus(true, true, "发票凭证打印助手已启动");
        }
        List<String> blockers = collectLaunchBlockers();
        if (!blockers.isEmpty()) {
            throw exception0(BAD_REQUEST.getCode(), "发票凭证打印助手启动配置缺失：" + String.join("，", blockers));
        }

        Process process = processStarter.start();
        waitForAssistantOnline(process);
        return buildStatus(true, true, "发票凭证打印助手已启动");
    }

    private void waitForAssistantOnline(Process process) {
        long deadline = System.nanoTime() + Duration.ofSeconds(properties.getStartupTimeoutSeconds()).toNanos();
        boolean processExitedSuccessfully = false;
        while (System.nanoTime() < deadline) {
            if (healthProbe.isRunning()) {
                return;
            }
            if (!process.isAlive()) {
                int exitCode = process.exitValue();
                if (exitCode != 0) {
                    throw exception0(INTERNAL_SERVER_ERROR.getCode(),
                            "发票凭证打印助手启动脚本退出码 " + exitCode + sanitizeProcessOutput(readProcessOutput(process)));
                }
                processExitedSuccessfully = true;
            }
            sleepForProbeInterval();
        }
        if (process.isAlive()) {
            process.destroyForcibly();
        }
        String reason = processExitedSuccessfully
                ? "发票凭证打印助手启动脚本已退出，但助手未注册在线"
                : "发票凭证打印助手启动超时，助手仍未注册在线";
        throw exception0(INTERNAL_SERVER_ERROR.getCode(), reason);
    }

    private AuthInvoiceVoucherPrintAssistantStatusRespVO buildStatus(boolean running, boolean launchable, String message) {
        return AuthInvoiceVoucherPrintAssistantStatusRespVO.builder()
                .running(running)
                .launchable(launchable)
                .message(message)
                .build();
    }

    private List<String> collectLaunchBlockers() {
        List<String> blockers = new ArrayList<>();
        if (StrUtil.isBlank(properties.getAssistantHost())) {
            blockers.add("assistant-host");
        }
        if (properties.getAssistantPort() == null || properties.getAssistantPort() <= 0 || properties.getAssistantPort() > 65535) {
            blockers.add("assistant-port");
        }
        if (StrUtil.isBlank(properties.getAssistantRootDir())) {
            blockers.add("assistant-root-dir");
        } else if (!properties.isAssistantRootDirectoryAvailable()) {
            blockers.add("assistant-root-dir=" + properties.getAssistantRootDirectoryPath());
        }
        if (StrUtil.isBlank(properties.getAssistantScript())) {
            blockers.add("assistant-script");
        } else if (!properties.isAssistantScriptAvailable()) {
            blockers.add("assistant-script=" + properties.getAssistantScriptPath());
        }
        if (StrUtil.isBlank(properties.getNodeCommand())) {
            blockers.add("node-command");
        } else if (!properties.isNodeCommandAvailable()) {
            blockers.add("node-command=" + properties.getNodeCommand());
        }
        if (StrUtil.isBlank(properties.getTicketValidateUrl())) {
            blockers.add("ticket-validate-url");
        }
        if (properties.getStartupTimeoutSeconds() == null || properties.getStartupTimeoutSeconds() <= 0) {
            blockers.add("startup-timeout-seconds");
        }
        if (properties.getProbeTimeoutSeconds() == null || properties.getProbeTimeoutSeconds() <= 0) {
            blockers.add("probe-timeout-seconds");
        }
        if (properties.getProbeIntervalMillis() == null || properties.getProbeIntervalMillis() <= 0) {
            blockers.add("probe-interval-millis");
        }
        return blockers;
    }

    private void sleepForProbeInterval() {
        try {
            Thread.sleep(Math.max(50, properties.getProbeIntervalMillis()));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw exception0(INTERNAL_SERVER_ERROR.getCode(), "等待发票凭证打印助手启动时被中断");
        }
    }

    private String readProcessOutput(Process process) {
        try {
            return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            return "无法读取启动输出：" + ex.getMessage();
        }
    }

    private String sanitizeProcessOutput(String rawOutput) {
        if (StrUtil.isBlank(rawOutput)) {
            return "";
        }
        String sanitized = rawOutput.replaceAll("[\\r\\n]+", " ").trim();
        if (sanitized.length() > 800) {
            sanitized = sanitized.substring(0, 800) + "...";
        }
        return "，输出：" + sanitized;
    }

}
