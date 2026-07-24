package cn.iocoder.yudao.module.infra.service.runtimecontrol;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlActionReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlActionPreviewRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlLogRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlOperationRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlOverviewRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlReleasePackageRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlReleaseStatusRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlRestartReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlRollbackCandidateRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlRestoreCandidateRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlStatusRespVO;
import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FileNasListRespVO;
import cn.iocoder.yudao.module.infra.service.file.NasBrowserService;
import cn.iocoder.yudao.module.infra.service.file.NasConnectionConfig;
import cn.iocoder.yudao.module.infra.service.file.NasFileReadResult;
import cn.iocoder.yudao.module.infra.service.file.NasSettingsService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.RUNTIME_CONTROL_ACTION_PARAMETER_INVALID;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.RUNTIME_CONTROL_INVALID_ACTION;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.RUNTIME_CONTROL_INVALID_TARGET;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.RUNTIME_CONTROL_LOG_NOT_EXISTS;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.RUNTIME_CONTROL_LOG_PATH_INVALID;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.RUNTIME_CONTROL_PROD_GUARD_REQUIRED;

@Service
public class RuntimeControlServiceImpl implements RuntimeControlService {

    private static final int OPERATION_HISTORY_LIMIT = 50;
    private static final int DEFAULT_LOG_TAIL_BYTES = 64 * 1024;
    private static final int MAX_LOG_TAIL_BYTES = 256 * 1024;
    private static final int STATUS_ENVIRONMENT_CONCURRENCY_LIMIT = 2;
    private static final String RELEASE_PACKAGE_STATUS_AVAILABLE = "AVAILABLE";

    private final RuntimeControlProperties properties;
    private final RuntimeControlCommandExecutor commandExecutor;
    private final RuntimeControlOperationStore operationStore;
    private final RuntimeOpsResponsibilityService responsibilityService;
    private final RuntimeOpsCandidateService candidateService;
    private final RuntimeControlReleasePackageConfigService releasePackageConfigService;
    private final NasSettingsService nasSettingsService;
    private final NasBrowserService nasBrowserService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExecutorService operationExecutor = Executors.newCachedThreadPool();

    @Autowired
    public RuntimeControlServiceImpl(RuntimeControlProperties properties,
                                     RuntimeControlCommandExecutor commandExecutor,
                                     RuntimeControlOperationStore operationStore,
                                     RuntimeOpsResponsibilityService responsibilityService,
                                     RuntimeOpsCandidateService candidateService,
                                     RuntimeControlReleasePackageConfigService releasePackageConfigService,
                                     NasSettingsService nasSettingsService,
                                     NasBrowserService nasBrowserService) {
        this.properties = properties;
        this.commandExecutor = commandExecutor;
        this.operationStore = operationStore;
        this.responsibilityService = responsibilityService;
        this.candidateService = candidateService;
        this.releasePackageConfigService = releasePackageConfigService;
        this.nasSettingsService = nasSettingsService;
        this.nasBrowserService = nasBrowserService;
    }

    RuntimeControlServiceImpl(RuntimeControlProperties properties,
                              RuntimeControlCommandExecutor commandExecutor,
                              RuntimeControlOperationStore operationStore,
                              RuntimeOpsResponsibilityService responsibilityService,
                              RuntimeOpsCandidateService candidateService,
                              NasSettingsService nasSettingsService,
                              NasBrowserService nasBrowserService) {
        this(properties, commandExecutor, operationStore, responsibilityService, candidateService,
                missingReleasePackageConfigService(), nasSettingsService, nasBrowserService);
    }

    RuntimeControlServiceImpl(RuntimeControlProperties properties,
                              RuntimeControlCommandExecutor commandExecutor,
                              RuntimeControlOperationStore operationStore,
                              RuntimeOpsResponsibilityService responsibilityService,
                              RuntimeOpsCandidateService candidateService,
                              NasSettingsService nasSettingsService) {
        this(properties, commandExecutor, operationStore, responsibilityService, candidateService,
                missingReleasePackageConfigService(), nasSettingsService, null);
    }

    RuntimeControlServiceImpl(RuntimeControlProperties properties,
                              RuntimeControlCommandExecutor commandExecutor,
                              RuntimeControlOperationStore operationStore,
                              RuntimeOpsResponsibilityService responsibilityService,
                              RuntimeOpsCandidateService candidateService) {
        this(properties, commandExecutor, operationStore, responsibilityService, candidateService,
                missingReleasePackageConfigService(), null, null);
    }

    private static RuntimeControlReleasePackageConfigService missingReleasePackageConfigService() {
        return () -> {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, "runtimeControlReleasePackageConfigService");
        };
    }

    @Override
    public RuntimeControlOverviewRespVO getOverview() {
        RuntimeControlOverviewRespVO respVO = new RuntimeControlOverviewRespVO();
        respVO.setEnvironments(List.copyOf(properties.getEnvironments().keySet()));
        respVO.setComponents(properties.getComponents());
        respVO.setStatuses(queryStatusesConcurrently());
        return respVO;
    }

    private Map<String, Map<String, RuntimeControlStatusRespVO>> queryStatusesConcurrently() {
        int concurrency = Math.max(1, Math.min(STATUS_ENVIRONMENT_CONCURRENCY_LIMIT,
                properties.getEnvironments().size()));
        ExecutorService executorService = Executors.newFixedThreadPool(concurrency);
        try {
            List<CompletableFuture<List<StatusResult>>> futures = properties.getEnvironments().entrySet().stream()
                    .map(entry -> CompletableFuture.supplyAsync(() ->
                            queryEnvironmentStatuses(entry.getKey(), entry.getValue()), executorService))
                    .toList();
            List<StatusResult> results = futures.stream()
                    .map(CompletableFuture::join)
                    .flatMap(List::stream)
                    .toList();

            Map<String, Map<String, RuntimeControlStatusRespVO>> statuses = new LinkedHashMap<>();
            properties.getEnvironments().keySet().forEach(environmentKey ->
                    statuses.put(environmentKey, new LinkedHashMap<>()));
            results.forEach(result -> statuses.get(result.environment()).put(result.component(), result.status()));
            return statuses;
        } finally {
            executorService.shutdownNow();
        }
    }

    private List<StatusResult> queryEnvironmentStatuses(String environmentKey,
                                                        RuntimeControlProperties.Environment environment) {
        List<StatusResult> results = new ArrayList<>();
        for (String component : properties.getComponents()) {
            RuntimeControlProperties.Target target = validateTarget(environmentKey, component);
            RuntimeControlCommand command = new RuntimeControlCommand(environmentKey, component,
                    target.getStatusScript(), target.buildStatusArguments(environment));
            RuntimeControlStatusResult statusResult = commandExecutor.queryStatus(command);
            results.add(new StatusResult(environmentKey, component,
                    buildStatus(environmentKey, component, target, environment, statusResult)));
        }
        return results;
    }

    @Override
    public RuntimeControlOperationRespVO restart(RuntimeControlRestartReqVO reqVO, String requestedBy) {
        String operator = requireOperator(requestedBy, "requestedBy");
        RuntimeControlProperties.Environment environment = properties.getEnvironments().get(reqVO.getEnvironment());
        RuntimeControlProperties.Target target = validateTarget(reqVO.getEnvironment(), reqVO.getComponent());
        validateRestartGuard(reqVO);

        RuntimeControlOperationRespVO operation = new RuntimeControlOperationRespVO();
        operation.setOperationId(UUID.randomUUID().toString());
        operation.setRequestedBy(operator);
        operation.setRequestedAt(LocalDateTime.now());
        operation.setEnvironment(reqVO.getEnvironment());
        operation.setComponent(reqVO.getComponent());
        operation.setReason(StrUtil.trim(reqVO.getReason()));
        operation.setStatus("running");
        operation.setSummary("Restart dispatched");
        operation.setResultLogPath(operationStore.getOperationPath(operation.getOperationId()).toString());
        operationStore.save(operation);

        RuntimeControlCommand command = new RuntimeControlCommand(reqVO.getEnvironment(), reqVO.getComponent(),
                target.getRestartScript(), target.buildRestartArguments(environment, operation.getResultLogPath()));
        commandExecutor.restart(command);
        return operation;
    }

    @Override
    public RuntimeControlOperationRespVO executeAction(RuntimeControlActionReqVO reqVO, String requestedBy) {
        String operator = requireOperator(requestedBy, "requestedBy");
        RuntimeControlOperationAction action = RuntimeControlOperationAction.fromAction(reqVO.getAction());
        if (action == null) {
            throw exception(RUNTIME_CONTROL_INVALID_ACTION, reqVO.getAction());
        }
        RuntimeControlOperationRespVO operation = new RuntimeControlOperationRespVO();
        operation.setOperationId(UUID.randomUUID().toString());
        operation.setRequestedBy(operator);
        operation.setRequestedAt(LocalDateTime.now());
        operation.setEnvironment(action.resolveEnvironment(reqVO));
        operation.setComponent("ops");
        operation.setAction(action.getAction());
        operation.setActionLabel(action.getLabel());
        operation.setParameters(action.safeParameters(reqVO));
        operation.setReason(StrUtil.trim(reqVO.getReason()));
        operation.setStatus("running");
        operation.setSummary(action.getLabel() + " dispatched");
        Path logPath = operationStore.getOperationLogPath(operation.getOperationId());
        operation.setResultLogPath(logPath.toString());
        operationStore.initializeLog(logPath);
        operationStore.save(operation);

        RuntimeControlReleasePackageConfig backendRuntimeBaseConfig;
        try {
            backendRuntimeBaseConfig = validateActionGuard(action, reqVO);
        } catch (RuntimeException ex) {
            String blockedMessage = StrUtil.blankToDefault(ex.getMessage(), "Operation blocked");
            appendOperationLog(logPath, "BLOCKED: " + blockedMessage + System.lineSeparator(), ex);
            operation.setEnvironment(action.resolveEnvironment(reqVO));
            operation.setParameters(action.safeParameters(reqVO));
            operation.setStatus("blocked");
            operation.setSummary(blockedMessage);
            operationStore.save(operation);
            throw ex;
        }

        operation.setEnvironment(action.resolveEnvironment(reqVO));
        operation.setParameters(action.safeParameters(reqVO));
        operationStore.save(operation);

        RuntimeControlCommand command = new RuntimeControlCommand(operation.getEnvironment(), "ops",
                action.resolveScriptPath(properties), action.buildArguments(reqVO, operation.getRequestedBy(), properties));
        Path nasConfigPath = appendNasReleaseArguments(action, command, operation.getOperationId());
        appendBackendRuntimeBaseArguments(action, command, backendRuntimeBaseConfig);
        if (action.requiresDetachedLinuxLocalRunner(properties)) {
            operationExecutor.submit(() -> executeDetachedActionCommand(operation.getOperationId(), action, command, logPath,
                    nasConfigPath));
        } else {
            operationExecutor.submit(() -> executeActionCommand(operation.getOperationId(), action, command, logPath,
                    nasConfigPath));
        }
        return operation;
    }

    @Override
    public RuntimeControlActionPreviewRespVO previewAction(RuntimeControlActionReqVO reqVO, String requestedBy) {
        String operator = requireOperator(requestedBy, "requestedBy");
        RuntimeControlOperationAction action = RuntimeControlOperationAction.fromAction(reqVO.getAction());
        if (action == null) {
            throw exception(RUNTIME_CONTROL_INVALID_ACTION, reqVO.getAction());
        }
        RuntimeControlReleasePackageConfig backendRuntimeBaseConfig = validateActionGuard(action, reqVO);
        RuntimeControlCommand command = new RuntimeControlCommand(action.resolveEnvironment(reqVO), "ops",
                action.resolveScriptPath(properties), action.buildArguments(reqVO, operator, properties));
        appendNasReleasePreviewArguments(action, command);
        appendBackendRuntimeBaseArguments(action, command, backendRuntimeBaseConfig);

        RuntimeControlActionPreviewRespVO respVO = new RuntimeControlActionPreviewRespVO();
        respVO.setAction(action.getAction());
        respVO.setActionLabel(action.getLabel());
        respVO.setEnvironment(command.getEnvironment());
        respVO.setComponent(command.getComponent());
        respVO.setScriptPath(command.getScriptPath());
        respVO.setArguments(List.copyOf(command.getArguments()));
        respVO.setParameters(action.safeParameters(reqVO));
        respVO.setEnableSmartReleaseReport(Boolean.TRUE.equals(reqVO.getEnableSmartReleaseReport()));
        respVO.setSummary(action.getLabel() + " command preview; no operation dispatched");
        return respVO;
    }

    @Override
    public RuntimeControlLogRespVO getOperationLog(String operationId, Integer maxBytes) {
        RuntimeControlOperationRespVO operation = operationStore.findById(operationId);
        if (operation == null) {
            throw exception(RUNTIME_CONTROL_LOG_NOT_EXISTS, operationId);
        }
        Path logPath = validateRegisteredLogPath(operation);
        if (!Files.isRegularFile(logPath)) {
            throw exception(RUNTIME_CONTROL_LOG_NOT_EXISTS, operationId);
        }
        int tailBytes = normalizeTailBytes(maxBytes);
        try {
            byte[] bytes = readTailBytes(logPath, tailBytes);
            long length = Files.size(logPath);
            String content = new String(bytes, StandardCharsets.UTF_8);
            operation = reconcileTerminalOperationStatus(operation, content);
            RuntimeControlLogRespVO respVO = new RuntimeControlLogRespVO();
            respVO.setOperationId(operationId);
            respVO.setStatus(operation.getStatus());
            respVO.setContent(content);
            respVO.setLength(length);
            respVO.setTruncated(length > bytes.length);
            respVO.setLogPath(logPath.toString());
            return respVO;
        } catch (IOException ex) {
            throw exception(RUNTIME_CONTROL_LOG_NOT_EXISTS, ex.getMessage());
        }
    }

    private Path appendNasReleaseArguments(RuntimeControlOperationAction action, RuntimeControlCommand command,
                                           String operationId) {
        if (!action.requiresNasReleaseRepository()) {
            return null;
        }
        Path nasConfigPath = writeNasReleaseConfig(operationId);
        command.getArguments().add("-NasConfigPath");
        command.getArguments().add(nasConfigPath.toString());
        command.getArguments().add("-NasServer");
        command.getArguments().add(properties.getReleasePackage().getNasServer());
        command.getArguments().add("-NasShare");
        command.getArguments().add(properties.getReleasePackage().getNasShare());
        command.getArguments().add("-NasReleaseRoot");
        command.getArguments().add(properties.getReleasePackage().getNasReleaseRoot());
        return nasConfigPath;
    }

    private void appendNasReleasePreviewArguments(RuntimeControlOperationAction action, RuntimeControlCommand command) {
        if (!action.requiresNasReleaseRepository()) {
            return;
        }
        appendRequiredArgument(command.getArguments(), "-NasServer", properties.getReleasePackage().getNasServer());
        appendRequiredArgument(command.getArguments(), "-NasShare", properties.getReleasePackage().getNasShare());
        appendRequiredArgument(command.getArguments(), "-NasReleaseRoot",
                properties.getReleasePackage().getNasReleaseRoot());
    }

    private void appendBackendRuntimeBaseArguments(RuntimeControlOperationAction action, RuntimeControlCommand command,
                                                   RuntimeControlReleasePackageConfig releasePackage) {
        if (action != RuntimeControlOperationAction.BUILD_RELEASE) {
            return;
        }
        if (releasePackage == null) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, "runtimeControlReleasePackageConfig");
        }
        appendRequiredArgument(command.getArguments(), "-BackendRuntimeBaseMode",
                releasePackage.backendRuntimeBaseMode());
        appendRequiredArgument(command.getArguments(), "-BackendRuntimeBaseTarPath",
                releasePackage.backendRuntimeBaseTarPath());
        appendRequiredArgument(command.getArguments(), "-BackendRuntimeBaseTarSha256",
                releasePackage.backendRuntimeBaseTarSha256());
        appendRequiredArgument(command.getArguments(), "-BackendRuntimeBaseImage",
                releasePackage.backendRuntimeBaseImage());
        appendRequiredArgument(command.getArguments(), "-BackendRuntimeBaseDigest",
                releasePackage.backendRuntimeBaseDigest());
        appendRequiredArgument(command.getArguments(), "-BackendRuntimeBaseVersion",
                releasePackage.backendRuntimeBaseVersion());
    }

    private void appendRequiredArgument(List<String> arguments, String name, String value) {
        if (StrUtil.isBlank(value)) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, name);
        }
        arguments.add(name);
        arguments.add(StrUtil.trim(value));
    }

    private Path writeNasReleaseConfig(String operationId) {
        if (nasSettingsService == null) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, "nasSettingsService");
        }
        NasConnectionConfig config = nasSettingsService.getRequiredNasConfig();
        Path configPath = Path.of(properties.getStateDir()).normalize()
                .resolve("nas-release-config")
                .resolve(operationId + ".json");
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("server", config.server());
        payload.put("port", String.valueOf(config.port()));
        payload.put("share", config.share());
        payload.put("domain", config.domain());
        payload.put("username", config.username());
        payload.put("password", config.password());
        try {
            Files.createDirectories(configPath.getParent());
            objectMapper.writeValue(configPath.toFile(), payload);
            return configPath;
        } catch (IOException ex) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID,
                    "写入 NAS 发布包配置失败：" + ex.getMessage());
        }
    }

    @Override
    public List<RuntimeControlOperationRespVO> getOperations() {
        return operationStore.listLatest(OPERATION_HISTORY_LIMIT);
    }

    private RuntimeControlOperationRespVO reconcileTerminalOperationStatus(RuntimeControlOperationRespVO operation,
                                                                           String logContent) {
        if (!"running".equals(operation.getStatus()) || StrUtil.isBlank(logContent)) {
            return operation;
        }
        RuntimeControlTerminalStatus terminalStatus = resolveTerminalStatus(operation, logContent);
        if (terminalStatus == null) {
            return operation;
        }
        operationStore.updateStatus(operation.getOperationId(), terminalStatus.status(), terminalStatus.summary());
        RuntimeControlOperationRespVO updated = operationStore.findById(operation.getOperationId());
        return updated == null ? operation : updated;
    }

    private RuntimeControlTerminalStatus resolveTerminalStatus(RuntimeControlOperationRespVO operation,
                                                               String logContent) {
        String actionLabel = StrUtil.blankToDefault(operation.getActionLabel(), "Operation");
        if (logContent.contains("操作完成：成功")) {
            return new RuntimeControlTerminalStatus("succeeded", actionLabel + " completed");
        }
        if (logContent.contains("操作完成：失败")) {
            String failureSummary = StrUtil.blankToDefault(extractResultDescription(logContent),
                    actionLabel + " failed");
            return new RuntimeControlTerminalStatus("failed", failureSummary);
        }
        return null;
    }

    private String extractResultDescription(String logContent) {
        for (String line : logContent.split("\\R")) {
            String trimmed = StrUtil.trim(line);
            if (StrUtil.startWith(trimmed, "结果说明：")) {
                return StrUtil.trim(trimmed.substring("结果说明：".length()));
            }
        }
        return "";
    }

    @Override
    public List<RuntimeControlReleasePackageRespVO> getReleasePackages() {
        if (nasSettingsService == null || nasBrowserService == null) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, "nas release package dependencies");
        }
        NasConnectionConfig nasConfig = releaseNasConfig();
        FileNasListRespVO response = nasBrowserService.listFiles(nasConfig, releasePackagesRoot());
        return response.getItems().stream()
                .filter(item -> Boolean.TRUE.equals(item.getDir()))
                .sorted(Comparator.comparingLong(this::releasePackageModifiedAt).reversed()
                        .thenComparing(FileNasListRespVO.Item::getName, Comparator.reverseOrder()))
                .map(item -> buildReleasePackageResponse(item, nasConfig))
                .filter(item -> RELEASE_PACKAGE_STATUS_AVAILABLE.equals(item.getStatus()))
                .sorted(Comparator.comparing(RuntimeControlReleasePackageRespVO::getBuiltAt,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(RuntimeControlReleasePackageRespVO::getReleaseTag, Comparator.reverseOrder()))
                .toList();
    }

    @Override
    public RuntimeControlReleaseStatusRespVO getReleaseStatus() {
        RuntimeControlOverviewRespVO overview = getOverview();
        List<RuntimeControlReleasePackageRespVO> packages = getReleasePackages();
        RuntimeControlReleaseStatusRespVO respVO = new RuntimeControlReleaseStatusRespVO();
        respVO.setReleasePackages(packages);
        respVO.setTargetStates(overview.getStatuses());
        respVO.setRecentOperations(getOperations().stream()
                .filter(operation -> StrUtil.containsAny(operation.getAction(),
                        "build-release", "publish-test", "mark-release-tested", "promote-prod", "promote-backup"))
                .limit(10)
                .toList());
        respVO.setTestCurrentReleaseTag(resolveCurrentReleaseTag(overview, "test"));
        respVO.setLatestTestedReleaseTag(packages.stream()
                .filter(item -> Boolean.TRUE.equals(item.getTested()))
                .map(RuntimeControlReleasePackageRespVO::getReleaseTag)
                .findFirst()
                .orElse(null));
        return respVO;
    }

    private long releasePackageModifiedAt(FileNasListRespVO.Item item) {
        return item.getModifiedAt() == null ? Long.MIN_VALUE : item.getModifiedAt();
    }

    private String resolveCurrentReleaseTag(RuntimeControlOverviewRespVO overview, String environment) {
        Map<String, RuntimeControlStatusRespVO> componentStatuses = overview.getStatuses().get(environment);
        if (componentStatuses == null) {
            return null;
        }
        return componentStatuses.values().stream()
                .map(RuntimeControlStatusRespVO::getCurrentReleaseTag)
                .filter(StrUtil::isNotBlank)
                .findFirst()
                .orElse(null);
    }

    private RuntimeControlReleasePackageRespVO buildReleasePackageResponse(FileNasListRespVO.Item item,
                                                                           NasConnectionConfig nasConfig) {
        RuntimeControlReleasePackageRespVO respVO = new RuntimeControlReleasePackageRespVO();
        String directoryName = item.getName();
        String packagePath = StrUtil.blankToDefault(item.getPath(), releasePackagesRoot() + "/" + directoryName)
                .replace("\\", "/");
        String manifestPath = packagePath + "/release-manifest.json";
        List<String> blockedReasons = new ArrayList<>();
        respVO.setReleaseTag(directoryName);
        respVO.setPackageDirectoryName(directoryName);
        respVO.setManifestPath(manifestPath);

        List<String> packageFileNames = listReleasePackageFileNames(nasConfig, packagePath, blockedReasons);
        JsonNode manifest = null;
        if (packageFileNames.contains("release-manifest.json")) {
            manifest = readReleasePackageJson(nasConfig, manifestPath, "release-manifest.json", blockedReasons);
        } else {
            blockedReasons.add("缺少 release-manifest.json");
        }
        if (manifest != null) {
            String releaseTag = text(manifest, "releaseTag");
            if (StrUtil.isNotBlank(releaseTag)) {
                respVO.setReleaseTag(releaseTag);
            }
            String packageDirectoryName = text(manifest, "packageDirectoryName");
            respVO.setPackageDirectoryName(packageDirectoryName);
            respVO.setImageTag(packageDirectoryName);
            respVO.setBuiltAt(text(manifest, "builtAt"));
            respVO.setPublishScope(text(manifest, "publishScope"));
            String component = text(manifest, "component");
            respVO.setComponent(component);
            Boolean includeShowroomBuildPackage = booleanValue(manifest, "includeShowroomBuildPackage");
            respVO.setIncludeShowroomBuildPackage(includeShowroomBuildPackage);
            if (StrUtil.isBlank(component)) {
                blockedReasons.add("release-manifest.json 缺少 component");
            } else if (!List.of("full", "intruoyi", "backend", "frontend", "website").contains(component)) {
                blockedReasons.add("release-manifest.json component 非法");
            }
            if (includeShowroomBuildPackage == null) {
                blockedReasons.add("release-manifest.json 缺少 includeShowroomBuildPackage");
            }
            Boolean onlyOfficeIncluded = booleanValue(manifest, "onlyOfficeIncluded");
            respVO.setOnlyOfficeIncluded(onlyOfficeIncluded);
            if (onlyOfficeIncluded == null) {
                blockedReasons.add("release-manifest.json 缺少 onlyOfficeIncluded");
            }
            if (StrUtil.isBlank(packageDirectoryName)) {
                blockedReasons.add("release-manifest.json 缺少 packageDirectoryName");
            } else if (!directoryName.equals(packageDirectoryName)) {
                blockedReasons.add("release-manifest packageDirectoryName 与目录不一致");
            }
            boolean checksumPresent = hasReleasePackageChecksum(manifest);
            respVO.setChecksumPresent(checksumPresent);
            if (!checksumPresent) {
                blockedReasons.add("release-manifest.json 缺少 artifact sha256");
            }
        } else {
            respVO.setChecksumPresent(false);
        }
        populateReleasePackageTestedMetadata(nasConfig, packagePath, packageFileNames, respVO);
        respVO.setStatus(blockedReasons.isEmpty() ? RELEASE_PACKAGE_STATUS_AVAILABLE : "BLOCKED");
        respVO.setBlockedReasons(blockedReasons);
        return respVO;
    }

    private List<String> listReleasePackageFileNames(NasConnectionConfig nasConfig, String packagePath,
                                                     List<String> blockedReasons) {
        try {
            return nasBrowserService.listFiles(nasConfig, packagePath).getItems().stream()
                    .filter(item -> !Boolean.TRUE.equals(item.getDir()))
                    .map(FileNasListRespVO.Item::getName)
                    .toList();
        } catch (ServiceException ex) {
            blockedReasons.add("发布包目录读取失败：" + ex.getMessage());
            return List.of();
        }
    }

    private JsonNode readReleasePackageJson(NasConnectionConfig nasConfig, String path, String fileName,
                                            List<String> blockedReasons) {
        try {
            NasFileReadResult result = nasBrowserService.readFile(nasConfig, path);
            return objectMapper.readTree(new String(result.bytes(), StandardCharsets.UTF_8));
        } catch (ServiceException ex) {
            blockedReasons.add("缺少 " + fileName);
            return null;
        } catch (IOException ex) {
            blockedReasons.add(fileName + " 解析失败：" + ex.getMessage());
            return null;
        }
    }

    private void populateReleasePackageTestedMetadata(NasConnectionConfig nasConfig, String packagePath,
                                                      List<String> packageFileNames,
                                                      RuntimeControlReleasePackageRespVO respVO) {
        String testedPath = packagePath + "/tested.json";
        if (!packageFileNames.contains("tested.json")) {
            respVO.setTested(false);
            return;
        }
        List<String> ignoredReasons = new ArrayList<>();
        JsonNode tested = readReleasePackageJson(nasConfig, testedPath, "tested.json", ignoredReasons);
        respVO.setTested(tested != null);
        if (tested != null) {
            respVO.setTestedAt(text(tested, "testedAt"));
            respVO.setOperatorName(text(tested, "operatorName"));
            JsonNode recoverySet = tested.get("recoverySet");
            respVO.setTestedRecoverySetCandidateId(text(recoverySet, "selectedRecoverySetCandidateId"));
            respVO.setTestedRecoverySetId(text(recoverySet, "recoverySetId"));
            respVO.setTestedRecoverySetManifestHash(text(recoverySet, "recoverySetManifestHash"));
        }
    }

    private boolean hasReleasePackageChecksum(JsonNode manifest) {
        JsonNode artifacts = manifest == null ? null : manifest.get("artifacts");
        if (artifacts == null || !artifacts.isArray() || artifacts.isEmpty()) {
            return false;
        }
        for (JsonNode artifact : artifacts) {
            if (StrUtil.isNotBlank(text(artifact, "sha256"))) {
                return true;
            }
        }
        return false;
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode field = node == null ? null : node.get(fieldName);
        return field == null || field.isNull() ? "" : field.asText();
    }

    private Boolean booleanValue(JsonNode node, String fieldName) {
        JsonNode field = node == null ? null : node.get(fieldName);
        return field != null && field.isBoolean() ? field.booleanValue() : null;
    }

    private void executeActionCommand(String operationId, RuntimeControlOperationAction action,
                                      RuntimeControlCommand command, Path logPath, Path nasConfigPath) {
        RuntimeException failure = null;
        try {
            commandExecutor.executeOperation(command, logPath);
        } catch (RuntimeException ex) {
            failure = ex;
        }
        failure = cleanupNasReleaseConfig(nasConfigPath, failure);
        if (failure != null) {
            operationStore.updateStatus(operationId, "failed", StrUtil.blankToDefault(failure.getMessage(), "Operation failed"));
            throw failure;
        }
        operationStore.updateStatus(operationId, "succeeded", action.getLabel() + " completed");
    }

    private void executeDetachedActionCommand(String operationId, RuntimeControlOperationAction action,
                                             RuntimeControlCommand command, Path logPath, Path nasConfigPath) {
        RuntimeException failure = null;
        try {
            commandExecutor.executeDetachedOperation(command, logPath, operationId, action.getLabel() + " completed");
        } catch (RuntimeException ex) {
            failure = ex;
        }
        failure = cleanupNasReleaseConfig(nasConfigPath, failure);
        if (failure != null) {
            operationStore.updateStatus(operationId, "failed", StrUtil.blankToDefault(failure.getMessage(), "Operation failed"));
            throw failure;
        }
    }

    private RuntimeException cleanupNasReleaseConfig(Path nasConfigPath, RuntimeException existingFailure) {
        if (nasConfigPath == null) {
            return existingFailure;
        }
        try {
            Files.deleteIfExists(nasConfigPath);
            return existingFailure;
        } catch (IOException ex) {
            IllegalStateException cleanupFailure = new IllegalStateException(
                    "删除 NAS 发布包临时配置失败：" + nasConfigPath, ex);
            if (existingFailure != null) {
                existingFailure.addSuppressed(cleanupFailure);
                return existingFailure;
            }
            return cleanupFailure;
        }
    }

    private void appendOperationLog(Path logPath, String content, RuntimeException existingFailure) {
        try {
            Files.writeString(logPath, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (IOException ex) {
            existingFailure.addSuppressed(ex);
        }
    }

    private RuntimeControlStatusRespVO buildStatus(String environment, String component,
                                                   RuntimeControlProperties.Target target,
                                                   RuntimeControlProperties.Environment runtimeEnvironment,
                                                   RuntimeControlStatusResult statusResult) {
        RuntimeControlStatusRespVO respVO = new RuntimeControlStatusRespVO();
        respVO.setStatus(statusResult.getStatus());
        respVO.setHttpStatus(statusResult.getHttpStatus());
        respVO.setRuntimeState(statusResult.getRuntimeState());
        respVO.setUrl(resolveStatusUrl(environment, component, target, statusResult));
        respVO.setPort(resolveStatusPort(environment, component, target, statusResult));
        respVO.setCurrentReleaseTag(StrUtil.trimToNull(statusResult.getCurrentReleaseTag()));
        String statusBlockedReason = StrUtil.blankToDefault(target.getBlockedReason(), statusResult.getBlockedReason());
        String writeBlockedReason = runtimeEnvironment.isAccessEnabled()
                ? null
                : StrUtil.blankToDefault(runtimeEnvironment.getAccessDisabledReason(), environment + " 环境写动作未启用");
        respVO.setActionEnabled(runtimeEnvironment.isAccessEnabled()
                && target.isActionEnabled()
                && StrUtil.isBlank(statusBlockedReason));
        respVO.setBlockedReason(joinReasons(writeBlockedReason, statusBlockedReason));
        respVO.setLastOperation(operationStore.findLatest(environment, component));
        return respVO;
    }

    private String joinReasons(String first, String second) {
        List<String> reasons = java.util.stream.Stream.of(first, second)
                .map(StrUtil::trimToNull)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        return reasons.isEmpty() ? null : String.join("；", reasons);
    }

    private String resolveStatusUrl(String environment, String component, RuntimeControlProperties.Target target,
                                    RuntimeControlStatusResult statusResult) {
        if (!"local".equals(environment)) {
            return target.getUrl();
        }
        if ("intruoyi-backend".equals(component) && statusResult.getBackendPort() != null) {
            return "http://127.0.0.1:" + statusResult.getBackendPort() + "/actuator/health";
        }
        if (("intruoyi-frontend".equals(component) || "intruoyi-full".equals(component))
                && statusResult.getFrontendPort() != null) {
            return "http://127.0.0.1:" + statusResult.getFrontendPort() + "/";
        }
        return target.getUrl();
    }

    private Integer resolveStatusPort(String environment, String component, RuntimeControlProperties.Target target,
                                      RuntimeControlStatusResult statusResult) {
        if (!"local".equals(environment)) {
            return target.getPort();
        }
        if ("intruoyi-backend".equals(component) && statusResult.getBackendPort() != null) {
            return statusResult.getBackendPort();
        }
        if ("intruoyi-frontend".equals(component) && statusResult.getFrontendPort() != null) {
            return statusResult.getFrontendPort();
        }
        return target.getPort();
    }

    private RuntimeControlProperties.Target validateTarget(String environment, String component) {
        RuntimeControlProperties.Target target = properties.getTarget(environment, component);
        if (target == null) {
            throw exception(RUNTIME_CONTROL_INVALID_TARGET, environment, component);
        }
        return target;
    }

    private String requireOperator(String operator, String fieldName) {
        if (StrUtil.isBlank(operator)) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, fieldName);
        }
        return StrUtil.trim(operator);
    }

    private void validateRestartGuard(RuntimeControlRestartReqVO reqVO) {
        if (!List.of("prod", "backup").contains(reqVO.getEnvironment())) {
            if (StrUtil.isBlank(reqVO.getReason())) {
                throw exception(RUNTIME_CONTROL_PROD_GUARD_REQUIRED);
            }
            return;
        }
        if (StrUtil.isBlank(reqVO.getReason()) || !"PROD".equals(reqVO.getProdConfirmText())) {
            throw exception(RUNTIME_CONTROL_PROD_GUARD_REQUIRED);
        }
    }

    private RuntimeControlReleasePackageConfig validateActionGuard(RuntimeControlOperationAction action,
                                                                   RuntimeControlActionReqVO reqVO) {
        RuntimeControlReleasePackageConfig backendRuntimeBaseConfig = null;
        if (StrUtil.isBlank(reqVO.getReason())) {
            throw exception(RUNTIME_CONTROL_PROD_GUARD_REQUIRED);
        }
        if (Boolean.TRUE.equals(reqVO.getEnableSmartReleaseReport()) && !action.supportsSmartReleaseReport()) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, "enableSmartReleaseReport");
        }
        validateActionTargetEnvironment(action, reqVO);
        if (action.isProdConfirmRequired(reqVO) && !"PROD".equals(reqVO.getProdConfirmText())) {
            throw exception(RUNTIME_CONTROL_PROD_GUARD_REQUIRED);
        }
        if (action == RuntimeControlOperationAction.MARK_RELEASE_TESTED) {
            reqVO.setReleaseTag(resolveCurrentReleaseTag("test"));
            if (StrUtil.isBlank(reqVO.getTestConclusion())) {
                throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, "testConclusion");
            }
            reqVO.setTestConclusion(StrUtil.trim(reqVO.getTestConclusion()));
            bindRecoverySetCandidate(reqVO);
        }
        if (action.requiresPublishScope()) {
            if (StrUtil.isBlank(reqVO.getPublishScope())) {
                throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, "publishScope");
            }
            if (!action.supportsPublishScope(reqVO.getPublishScope())) {
                throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, "publishScope");
            }
            if (reqVO.getIncludeOnlyOffice() == null) {
                throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, "includeOnlyOffice");
            }
            if (reqVO.getIncludeShowroomBuildPackage() == null) {
                throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, "includeShowroomBuildPackage");
            }
            if (action == RuntimeControlOperationAction.BUILD_RELEASE) {
                backendRuntimeBaseConfig = validateBackendRuntimeBaseConfig();
                validateReleaseTargetHostConfig();
            }
        } else if (StrUtil.isNotBlank(reqVO.getPublishScope())) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, "publishScope");
        }
        if (!action.requiresPublishScope() && reqVO.getIncludeOnlyOffice() != null) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, "includeOnlyOffice");
        }
        if (!action.requiresPublishScope() && reqVO.getIncludeShowroomBuildPackage() != null) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, "includeShowroomBuildPackage");
        }
        if (action.requiresReleaseTag()) {
            validateReleaseTag(reqVO.getReleaseTag());
        } else if (StrUtil.isNotBlank(reqVO.getReleaseTag())) {
            validateReleaseTag(reqVO.getReleaseTag());
        }
        if (action.requiresSqlPath()) {
            validateApplyTestDbSqlTargetConfig();
            validateSqlPath(reqVO);
        } else if (StrUtil.isNotBlank(reqVO.getSqlPath())) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, "sqlPath");
        }
        if (action.requiresSelectedImageCandidateId() && StrUtil.isBlank(reqVO.getSelectedImageCandidateId())) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, "selectedImageCandidateId");
        }
        if (action.requiresSelectedRecoverySetCandidateId()
                && StrUtil.isBlank(reqVO.getSelectedRecoverySetCandidateId())) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, "selectedRecoverySetCandidateId");
        }
        if (action == RuntimeControlOperationAction.ROLLBACK_APP && StrUtil.isNotBlank(reqVO.getSelectedImageTag())) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, "selectedImageTag 只能由服务端候选解析");
        }
        if ((action == RuntimeControlOperationAction.REHEARSAL || action == RuntimeControlOperationAction.RESTORE_DATA)
                && StrUtil.isNotBlank(reqVO.getSelectedBackupId())) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, "selectedBackupId 只能由服务端候选解析");
        }
        validateActionEnvironmentEnabled(action, reqVO);
        if (action == RuntimeControlOperationAction.PUBLISH_TEST
                || action == RuntimeControlOperationAction.PROMOTE_PROD
                || action == RuntimeControlOperationAction.PROMOTE_BACKUP) {
            validateRemoteDeployTargetHostConfig(action.resolveEnvironment(reqVO));
            validateReleaseTargetHostConfig();
        }
        if (action.requiresResponsibilityGate()) {
            responsibilityService.validateRequiredOwners(action.resolveEnvironment(reqVO), action.getAction());
        }
        RuntimeControlReleasePackageRespVO releasePackage = validateReleasePackageAvailability(action,
                reqVO.getReleaseTag());
        if (action == RuntimeControlOperationAction.PROMOTE_BACKUP) {
            recheckPromoteBackupRecoverySet(releasePackage);
        }
        if (action == RuntimeControlOperationAction.ROLLBACK_APP) {
            RuntimeControlRollbackCandidateRespVO candidate =
                    candidateService.requireAvailableRollbackCandidate(reqVO.getSelectedImageCandidateId());
            reqVO.setSelectedImageTag(candidate.getImageTag());
        }
        if (action == RuntimeControlOperationAction.REHEARSAL || action == RuntimeControlOperationAction.RESTORE_DATA) {
            bindRecoverySetCandidate(reqVO);
        }
        return backendRuntimeBaseConfig;
    }

    private void validateApplyTestDbSqlTargetConfig() {
        RuntimeControlProperties.Environment testEnvironment = properties.getEnvironments().get("test");
        if (testEnvironment == null || testEnvironment.isLocal()) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, "-ServerHost");
        }
        if (!RuntimeControlProperties.TEST_SERVER_HOST.equals(StrUtil.trim(testEnvironment.getHost()))) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID,
                    "-ServerHost 必须是测试服 " + RuntimeControlProperties.TEST_SERVER_HOST);
        }
        validateRemoteDeployTargetHostConfig("test");
        if (StrUtil.isBlank(testEnvironment.getServerUser())) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, "-ServerUser");
        }
        if (StrUtil.isBlank(testEnvironment.getRemoteAppDir())) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, "-RemoteAppDir");
        }
    }

    private void validateSqlPath(RuntimeControlActionReqVO reqVO) {
        String trimmedSqlPath = StrUtil.trim(reqVO.getSqlPath());
        if (StrUtil.isBlank(trimmedSqlPath)) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, "sqlPath");
        }
        Path sqlPath;
        try {
            sqlPath = Path.of(trimmedSqlPath).toAbsolutePath().normalize();
        } catch (InvalidPathException ex) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID,
                    "sqlPath 路径不合法：" + ex.getInput());
        }
        String fileName = sqlPath.getFileName() == null ? "" : sqlPath.getFileName().toString();
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".sql")) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID,
                    "sqlPath must point to a .sql file");
        }
        if (!Files.isRegularFile(sqlPath)) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID,
                    "sqlPath 文件不存在或不是普通文件：" + sqlPath);
        }
        try {
            if (Files.size(sqlPath) <= 0) {
                throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, "SQL file is empty");
            }
        } catch (IOException ex) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID,
                    "sqlPath 文件不可读：" + sqlPath);
        }
        reqVO.setSqlPath(sqlPath.toString());
    }

    private void validateReleaseTargetHostConfig() {
        requireEnvironmentHost("test", "-TestServerHost");
        requireEnvironmentHost("backup", "-BackupServerHost");
    }

    private void validateRemoteDeployTargetHostConfig(String environment) {
        requireEnvironmentHost(environment, "-ServerHost");
    }

    private void requireEnvironmentHost(String environment, String argumentName) {
        RuntimeControlProperties.Environment runtimeEnvironment = properties.getEnvironments().get(environment);
        if (runtimeEnvironment == null || runtimeEnvironment.isLocal() || StrUtil.isBlank(runtimeEnvironment.getHost())) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, argumentName);
        }
    }

    private RuntimeControlReleasePackageConfig validateBackendRuntimeBaseConfig() {
        return releasePackageConfigService.getRequiredBackendRuntimeBaseConfig();
    }

    private RuntimeControlReleasePackageRespVO validateReleasePackageAvailability(RuntimeControlOperationAction action,
                                                                                 String releaseTag) {
        if (action != RuntimeControlOperationAction.PUBLISH_TEST
                && action != RuntimeControlOperationAction.PROMOTE_PROD
                && action != RuntimeControlOperationAction.PROMOTE_BACKUP) {
            return null;
        }
        RuntimeControlReleasePackageRespVO releasePackage = getReleasePackages().stream()
                .filter(item -> releaseTag.equals(item.getReleaseTag())
                        || releaseTag.equals(item.getPackageDirectoryName()))
                .findFirst()
                .orElseThrow(() -> exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID,
                        "releaseTag 发布包缺少 manifest/checksum 或不存在：" + releaseTag));
        if ((action == RuntimeControlOperationAction.PROMOTE_PROD
                || action == RuntimeControlOperationAction.PROMOTE_BACKUP)
                && !Boolean.TRUE.equals(releasePackage.getTested())) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID,
                    "releaseTag 尚未测试通过：" + releaseTag);
        }
        return releasePackage;
    }

    private void bindRecoverySetCandidate(RuntimeControlActionReqVO reqVO) {
        RuntimeControlRestoreCandidateRespVO candidate =
                candidateService.requireAvailableRestoreCandidate(reqVO.getSelectedRecoverySetCandidateId());
        reqVO.setSelectedBackupId(candidate.getBackupId());
        reqVO.setRecoverySetId(candidate.getRecoverySetId());
        reqVO.setRecoverySetManifestHash(candidate.getRecoverySetManifestHash());
        reqVO.setRecoverySetProgramVersion(candidate.getProgramVersion());
        reqVO.setRecoverySetRedisPolicy(candidate.getRedisPolicy());
    }

    private void recheckPromoteBackupRecoverySet(RuntimeControlReleasePackageRespVO releasePackage) {
        if (releasePackage == null) {
            return;
        }
        if (StrUtil.isBlank(releasePackage.getTestedRecoverySetCandidateId())) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID,
                    "tested.json 缺少 selectedRecoverySetCandidateId");
        }
        RuntimeControlRestoreCandidateRespVO candidate =
                candidateService.requireAvailableRestoreCandidate(releasePackage.getTestedRecoverySetCandidateId());
        if (!StrUtil.equals(candidate.getRecoverySetId(), releasePackage.getTestedRecoverySetId())) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID,
                    "tested.json recoverySetId 与当前恢复集候选不一致");
        }
        if (!StrUtil.equals(candidate.getRecoverySetManifestHash(), releasePackage.getTestedRecoverySetManifestHash())) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID,
                    "tested.json recoverySetManifestHash 与当前恢复集候选不一致");
        }
    }

    private void validateActionTargetEnvironment(RuntimeControlOperationAction action, RuntimeControlActionReqVO reqVO) {
        if (action != RuntimeControlOperationAction.BACKUP_NOW
                && action != RuntimeControlOperationAction.ROLLBACK_APP
                && action != RuntimeControlOperationAction.RESTORE_DATA) {
            if (StrUtil.isNotBlank(reqVO.getTargetEnvironment())) {
                throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, "targetEnvironment");
            }
            return;
        }
        String targetEnvironment = StrUtil.trimToEmpty(reqVO.getTargetEnvironment());
        if (StrUtil.isBlank(targetEnvironment)) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, "targetEnvironment");
        }
        List<String> allowedTargets = action == RuntimeControlOperationAction.RESTORE_DATA
                || action == RuntimeControlOperationAction.ROLLBACK_APP
                ? List.of("test", "backup")
                : List.of("test", "prod");
        if (!allowedTargets.contains(targetEnvironment)) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, "targetEnvironment");
        }
        reqVO.setTargetEnvironment(targetEnvironment);
    }

    private void validateActionEnvironmentEnabled(RuntimeControlOperationAction action, RuntimeControlActionReqVO reqVO) {
        String environment = action.resolveEnvironment(reqVO);
        RuntimeControlProperties.Environment runtimeEnvironment = properties.getEnvironments().get(environment);
        if (runtimeEnvironment == null) {
            if (action == RuntimeControlOperationAction.BUILD_RELEASE) {
                return;
            }
            throw exception(RUNTIME_CONTROL_INVALID_TARGET, environment, "ops");
        }
        if (!runtimeEnvironment.isAccessEnabled()) {
            if (action == RuntimeControlOperationAction.BACKUP_NOW && "prod".equals(environment)) {
                return;
            }
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, environment + " 环境未启用："
                    + StrUtil.blankToDefault(runtimeEnvironment.getAccessDisabledReason(), "环境访问未启用"));
        }
    }

    private void validateReleaseTag(String releaseTag) {
        if (StrUtil.isBlank(releaseTag)) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, "releaseTag");
        }
        String trimmed = StrUtil.trim(releaseTag);
        if (trimmed.contains("..") || trimmed.contains("/") || trimmed.contains("\\")) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, "releaseTag 不合法：" + trimmed);
        }
    }

    private String resolveCurrentReleaseTag(String environment) {
        RuntimeControlProperties.Environment runtimeEnvironment = properties.getEnvironments().get(environment);
        if (runtimeEnvironment == null) {
            throw exception(RUNTIME_CONTROL_INVALID_TARGET, environment, "current-release-tag");
        }
        for (String component : List.of("intruoyi-full", "intruoyi-backend", "intruoyi-frontend", "website-frontend")) {
            RuntimeControlProperties.Target target = validateTarget(environment, component);
            RuntimeControlCommand command = new RuntimeControlCommand(environment, component,
                    target.getStatusScript(), target.buildStatusArguments(runtimeEnvironment));
            RuntimeControlStatusResult statusResult = commandExecutor.queryStatus(command);
            String currentReleaseTag = StrUtil.trimToNull(statusResult.getCurrentReleaseTag());
            if (currentReleaseTag != null) {
                return currentReleaseTag;
            }
        }
        throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, "testCurrentReleaseTag");
    }

    private NasConnectionConfig releaseNasConfig() {
        NasConnectionConfig baseConfig = nasSettingsService.getRequiredNasConfig();
        return new NasConnectionConfig(
                properties.getReleasePackage().getNasServer(),
                baseConfig.port(),
                properties.getReleasePackage().getNasShare(),
                baseConfig.domain(),
                baseConfig.username(),
                baseConfig.password()
        );
    }

    private String releasePackagesRoot() {
        String root = StrUtil.trimToEmpty(properties.getReleasePackage().getNasReleaseRoot()).replace("\\", "/");
        if (StrUtil.isBlank(root)) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, "releasePackage.nasReleaseRoot");
        }
        return String.join("/", java.util.Arrays.stream(root.split("/"))
                .map(StrUtil::trim)
                .filter(StrUtil::isNotBlank)
                .toList());
    }

    private Path validateRegisteredLogPath(RuntimeControlOperationRespVO operation) {
        if (StrUtil.isBlank(operation.getResultLogPath())) {
            throw exception(RUNTIME_CONTROL_LOG_NOT_EXISTS, operation.getOperationId());
        }
        Path stateDir = operationStore.getStateDir().toAbsolutePath().normalize();
        Path logPath = Path.of(operation.getResultLogPath()).toAbsolutePath().normalize();
        if (!logPath.startsWith(stateDir)) {
            throw exception(RUNTIME_CONTROL_LOG_PATH_INVALID, operation.getResultLogPath());
        }
        return logPath;
    }

    private int normalizeTailBytes(Integer maxBytes) {
        if (maxBytes == null || maxBytes <= 0) {
            return DEFAULT_LOG_TAIL_BYTES;
        }
        return Math.min(maxBytes, MAX_LOG_TAIL_BYTES);
    }

    private byte[] readTailBytes(Path logPath, int maxBytes) throws IOException {
        long length = Files.size(logPath);
        if (length <= maxBytes) {
            return Files.readAllBytes(logPath);
        }
        try (java.io.InputStream inputStream = Files.newInputStream(logPath)) {
            inputStream.skipNBytes(length - maxBytes);
            return inputStream.readAllBytes();
        }
    }

    private record StatusTask(String environment, String component, RuntimeControlProperties.Target target,
                              RuntimeControlProperties.Environment runtimeEnvironment, RuntimeControlCommand command) {
    }

    private record StatusResult(String environment, String component, RuntimeControlStatusRespVO status) {
    }

    private record RuntimeControlTerminalStatus(String status, String summary) {
    }
}
