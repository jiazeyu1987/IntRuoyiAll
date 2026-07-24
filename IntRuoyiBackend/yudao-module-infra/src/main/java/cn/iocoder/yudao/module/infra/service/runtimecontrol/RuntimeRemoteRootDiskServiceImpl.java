package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlRemoteRootCleanupReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlRemoteRootCleanupRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlRemoteRootDiskStatusRespVO;
import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.RUNTIME_CONTROL_ACTION_PARAMETER_INVALID;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.RUNTIME_CONTROL_COMMAND_FAILED;

@Service
public class RuntimeRemoteRootDiskServiceImpl implements RuntimeRemoteRootDiskService {

    private static final Map<String, String> TARGET_HOSTS = targetHosts();
    private static final String SCRIPT_PATH = "script/deploy/manage-int-ruoyi-remote-root-disk.ps1";
    private static final List<String> ALLOWED_CLEANUP_PATHS = List.of("/opt/intruoyi/ops/backup/tmp", "/tmp");
    private static final Duration STATUS_TIMEOUT = Duration.ofSeconds(45);
    private static final Duration CLEANUP_TIMEOUT = Duration.ofMinutes(15);

    private final RuntimeControlProperties properties;
    private final RuntimeControlCommandExecutor commandExecutor;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public RuntimeRemoteRootDiskServiceImpl(RuntimeControlProperties properties,
                                            RuntimeControlCommandExecutor commandExecutor) {
        this.properties = properties;
        this.commandExecutor = commandExecutor;
    }

    @Override
    public RuntimeControlRemoteRootDiskStatusRespVO getStatus(String targetEnvironment) {
        RuntimeControlProperties.Environment environment = requireKnownEnvironment(targetEnvironment);
        String output = commandExecutor.executeForOutput(buildCommand("status", environment, null, null),
                STATUS_TIMEOUT);
        RuntimeControlRemoteRootDiskStatusRespVO status =
                readJson(output, RuntimeControlRemoteRootDiskStatusRespVO.class);
        assertStatusBoundary(status);
        return status;
    }

    @Override
    public RuntimeControlRemoteRootCleanupRespVO cleanup(RuntimeControlRemoteRootCleanupReqVO reqVO,
                                                        String loginUserId) {
        if (StrUtil.isBlank(reqVO.getReason())) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, "reason");
        }
        RuntimeControlProperties.Environment environment = requireKnownEnvironment(reqVO.getTargetEnvironment());
        if (requiresProtectedCleanupConfirm(reqVO.getTargetEnvironment())
                && !"PROD".equals(reqVO.getProdConfirmText())) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, "正式服/备用服务器根分区清理必须输入 PROD");
        }
        String output = commandExecutor.executeForOutput(buildCommand("cleanup", environment, reqVO.getReason(),
                loginUserId, reqVO.getProdConfirmText()), CLEANUP_TIMEOUT);
        RuntimeControlRemoteRootCleanupRespVO response = readJson(output, RuntimeControlRemoteRootCleanupRespVO.class);
        assertCleanupBoundary(response);
        response.setReason(reqVO.getReason());
        response.setRequestedBy(loginUserId);
        if (response.getCleanedAt() == null) {
            response.setCleanedAt(LocalDateTime.now());
        }
        return response;
    }

    private RuntimeControlProperties.Environment requireKnownEnvironment(String targetEnvironment) {
        if (!TARGET_HOSTS.containsKey(targetEnvironment)) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, "根分区操作必须显式提交 targetEnvironment=test/prod/backup");
        }
        RuntimeControlProperties.Environment environment = properties.getEnvironments().get(targetEnvironment);
        if (environment == null || StrUtil.isBlank(environment.getHost())) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, targetEnvironment + ".serverHost");
        }
        if (!TARGET_HOSTS.get(targetEnvironment).equals(environment.getHost())) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID,
                    "无法证明目标环境和固定服务器 IP 匹配，拒绝执行：" + targetEnvironment);
        }
        return environment;
    }

    private RuntimeControlCommand buildCommand(String mode, RuntimeControlProperties.Environment environment,
                                               String reason, String loginUserId) {
        return buildCommand(mode, environment, reason, loginUserId, null);
    }

    private RuntimeControlCommand buildCommand(String mode, RuntimeControlProperties.Environment environment,
                                               String reason, String loginUserId, String prodConfirmText) {
        List<String> args = new ArrayList<>();
        args.add("-Mode");
        args.add(mode);
        args.add("-TargetEnvironment");
        args.add(resolveEnvironmentKey(environment));
        args.add("-ServerHost");
        args.add(environment.getHost());
        args.add("-ServerUser");
        args.add(environment.getServerUser());
        if (StrUtil.isNotBlank(reason)) {
            args.add("-Reason");
            args.add(reason);
        }
        if (StrUtil.isNotBlank(loginUserId)) {
            args.add("-RequestedBy");
            args.add(loginUserId);
        }
        if (StrUtil.isNotBlank(prodConfirmText)) {
            args.add("-ProdConfirmText");
            args.add(prodConfirmText);
        }
        return new RuntimeControlCommand(resolveEnvironmentKey(environment), "remote-root-disk", SCRIPT_PATH, args);
    }

    private void assertCleanupBoundary(RuntimeControlRemoteRootCleanupRespVO response) {
        if (response == null) {
            throw exception(RUNTIME_CONTROL_COMMAND_FAILED, "empty cleanup response");
        }
        if (!TARGET_HOSTS.containsKey(response.getTargetEnvironment())
                || !TARGET_HOSTS.get(response.getTargetEnvironment()).equals(response.getServerHost())) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID,
                    "清理结果无法证明目标环境和固定服务器 IP 匹配");
        }
        if (!ALLOWED_CLEANUP_PATHS.equals(response.getCleanupPaths())) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID,
                    "清理目录不在允许列表：" + response.getCleanupPaths());
        }
        assertStatusBoundary(response.getBefore());
        assertStatusBoundary(response.getAfter());
    }

    private void assertStatusBoundary(RuntimeControlRemoteRootDiskStatusRespVO status) {
        if (status == null) {
            throw exception(RUNTIME_CONTROL_COMMAND_FAILED, "empty remote root disk status");
        }
        if (!TARGET_HOSTS.containsKey(status.getTargetEnvironment())
                || !TARGET_HOSTS.get(status.getTargetEnvironment()).equals(status.getServerHost())) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID,
                    "容量结果无法证明目标环境和固定服务器 IP 匹配");
        }
        if (!"/".equals(status.getMountPoint())) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, "远程根分区挂载点必须为 /");
        }
    }

    private String resolveEnvironmentKey(RuntimeControlProperties.Environment environment) {
        return TARGET_HOSTS.entrySet().stream()
                .filter(entry -> entry.getValue().equals(environment.getHost()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(() -> exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID,
                        "无法根据固定服务器 IP 解析目标环境"));
    }

    private static Map<String, String> targetHosts() {
        Map<String, String> hosts = new LinkedHashMap<>();
        hosts.put("test", RuntimeControlProperties.TEST_SERVER_HOST);
        hosts.put("prod", RuntimeControlProperties.PROD_SERVER_HOST);
        hosts.put("backup", RuntimeControlProperties.BACKUP_SERVER_HOST);
        return hosts;
    }

    private boolean requiresProtectedCleanupConfirm(String targetEnvironment) {
        return "prod".equals(targetEnvironment) || "backup".equals(targetEnvironment);
    }

    private <T> T readJson(String output, Class<T> type) {
        if (StrUtil.isBlank(output)) {
            throw exception(RUNTIME_CONTROL_COMMAND_FAILED, "empty script output");
        }
        try {
            return objectMapper.readValue(output, type);
        } catch (JsonProcessingException ex) {
            throw exception(RUNTIME_CONTROL_COMMAND_FAILED, "invalid script json: " + ex.getMessage());
        }
    }
}
