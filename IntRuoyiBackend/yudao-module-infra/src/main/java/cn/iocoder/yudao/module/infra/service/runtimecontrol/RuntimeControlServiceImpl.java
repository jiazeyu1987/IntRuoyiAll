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
import cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow.ReleaseDigestContract;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow.ReleaseWorkflowExecutorContract;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow.ReleaseWorkflowRecord;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow.ReleaseWorkflowStore;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow.ReleaseWorkflowBackupAuthorizationService;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.releaseworkflow.ReleaseWorkflowWorktreeFactory;
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
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Set;

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
    private final Set<String> canceledOperations = java.util.concurrent.ConcurrentHashMap.newKeySet();

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
        validateNoUnknownOperation(reqVO.getEnvironment(), null);
        String restoreMarker = environment.isLocal() ? null : RuntimeControlRestoreIsolationConfig.resolve(properties);

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
        if (restoreMarker != null) {
            appendRequiredArgument(command.getArguments(), "-RestoreIsolationMarkerPath", restoreMarker);
        }
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
        validateIndependentBackupBinding(action, reqVO, operator);
        RuntimeControlOperationRespVO operation = new RuntimeControlOperationRespVO();
        operation.setOperationId(resolveOperationId(reqVO));
        operation.setRequestedBy(operator);
        operation.setRequestedAt(LocalDateTime.now());
        operation.setEnvironment(action.resolveEnvironment(reqVO));
        operation.setComponent("ops");
        operation.setAction(action.getAction());
        operation.setActionLabel(action.getLabel());
        operation.setParameters(operationParameters(action, reqVO));
        operation.setReason(StrUtil.trim(reqVO.getReason()));
        operation.setStatus("running");
        operation.setSummary(action.getLabel() + " dispatched");
        Path logPath = operationStore.getOperationLogPath(operation.getOperationId());
        operation.setResultLogPath(logPath.toString());
        if (!operationStore.createIfAbsent(operation)) {
            RuntimeControlOperationRespVO existing = operationStore.findById(operation.getOperationId());
            if (isIndependentBackup(action, reqVO) && existing != null
                    && java.util.Objects.equals(existing.getAction(), operation.getAction())
                    && java.util.Objects.equals(existing.getEnvironment(), operation.getEnvironment())
                    && java.util.Objects.equals(existing.getRequestedBy(), operation.getRequestedBy())
                    && java.util.Objects.equals(existing.getReason(), operation.getReason())
                    && java.util.Objects.equals(existing.getParameters(), operation.getParameters())) {
                return existing;
            }
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, "RELEASE_WORKFLOW_OPERATION_ALREADY_CLAIMED");
        }
        operationStore.initializeLog(logPath);

        RuntimeControlReleasePackageConfig backendRuntimeBaseConfig;
        try {
            backendRuntimeBaseConfig = validateActionGuard(action, reqVO);
        } catch (RuntimeException ex) {
            String blockedMessage = StrUtil.blankToDefault(ex.getMessage(), "Operation blocked");
            appendOperationLog(logPath, "BLOCKED: " + blockedMessage + System.lineSeparator(), ex);
            operation.setEnvironment(action.resolveEnvironment(reqVO));
            operation.setParameters(operationParameters(action, reqVO));
            operation.setStatus("blocked");
            operation.setSummary(blockedMessage);
            operation.setZeroWriteEvidence(true);
            operationStore.save(operation);
            throw ex;
        }

        operation.setEnvironment(action.resolveEnvironment(reqVO));
        operation.setParameters(operationParameters(action, reqVO));
        operationStore.save(operation);

        Path nasConfigPath = null;
        try {
            RuntimeControlCommand command = new RuntimeControlCommand(operation.getEnvironment(), "ops",
                    action.resolveScriptPath(properties), action.buildArguments(reqVO, operation.getRequestedBy(), properties));
            bindReleaseWorkflowToolchain(action, command, reqVO);
            appendRestoreIsolationArguments(action, command);
            nasConfigPath = appendNasReleaseArguments(action, command, operation.getOperationId());
            appendBackendRuntimeBaseArguments(action, command, backendRuntimeBaseConfig);
            commandExecutor.registerOperation(operation.getOperationId(), logPath);
            Path operationNasConfigPath = nasConfigPath;
            operation.setZeroWriteEvidence(false);
            operationStore.save(operation);
            if (action.requiresDetachedLinuxLocalRunner(properties)) {
                operationExecutor.submit(() -> executeDetachedActionCommand(operation.getOperationId(), action, command,
                        logPath, operationNasConfigPath));
            } else {
                operationExecutor.submit(() -> executeActionCommand(operation.getOperationId(), action, command,
                        logPath, operationNasConfigPath, reqVO, operator));
            }
        } catch (RuntimeException ex) {
            cleanupNasReleaseConfig(nasConfigPath, ex);
            String blockedMessage = StrUtil.blankToDefault(ex.getMessage(), "Operation blocked");
            appendOperationLog(logPath, "BLOCKED: " + blockedMessage + System.lineSeparator(), ex);
            operationStore.updateResult(operation.getOperationId(), "blocked", blockedMessage, true);
            throw ex;
        }
        return operation;
    }

    @Override
    public RuntimeControlOperationRespVO dispatchWorkflowAction(RuntimeControlActionReqVO request, String requestedBy) {
        String operator = requireOperator(requestedBy, "requestedBy");
        RuntimeControlOperationAction action = RuntimeControlOperationAction.fromAction(request.getAction());
        if (action == null || !isIndependentBackup(action, request)) {
            throw exception(RUNTIME_CONTROL_INVALID_ACTION, "Independent backup workflow dispatch required");
        }
        try {
            return executeAction(request, operator);
        } catch (RuntimeException rejection) {
            recordUndispatchedWorkflowRejection(action, request, operator, rejection);
            throw rejection;
        }
    }

    private void recordUndispatchedWorkflowRejection(RuntimeControlOperationAction action,
                                                     RuntimeControlActionReqVO request, String operator,
                                                     RuntimeException rejection) {
        try {
            ReleaseWorkflowRecord record = new ReleaseWorkflowStore(properties).require(request.getReleaseWorkflowId());
            if (record.backupIntent() == null || record.operationId() == null
                    || !record.operationId().equals(request.getPreassignedOperationId())
                    || !java.util.Objects.equals(record.requestedBy(), operator)) {
                return;
            }
            RuntimeControlActionReqVO binding = new RuntimeControlActionReqVO();
            binding.setReleaseWorkflowId(record.workflowId());
            binding.setReleaseAuthorizationId(record.backupIntent().authorizationId());
            binding.setTargetEnvironment("backup");
            binding.setPublishScope(record.publishScope());
            binding.setIncludeOnlyOffice(false);
            binding.setIncludeShowroomBuildPackage(false);
            binding.setReleaseTag(record.releaseTag());
            binding.setSourceSelectionId(record.sourceSelectionId());
            binding.setExpectedMaintenanceCommit(record.maintenanceCommit());
            binding.setExpectedApplicationCommit(record.applicationCommit());
            binding.setExpectedFrontendCommit(record.frontendCommit());
            binding.setExpectedPackageDigest(record.packageDigest());
            binding.setExpectedManifestDigest(record.manifestDigest());
            RuntimeControlOperationRespVO rejected = new RuntimeControlOperationRespVO();
            rejected.setOperationId(record.operationId());
            rejected.setEnvironment("backup");
            rejected.setComponent("ops");
            rejected.setAction(action.getAction());
            rejected.setActionLabel(action.getLabel());
            rejected.setRequestedBy(operator);
            rejected.setRequestedAt(LocalDateTime.now());
            rejected.setReason(record.reason());
            rejected.setParameters(operationParameters(action, binding));
            rejected.setStatus("blocked");
            rejected.setSummary(StrUtil.blankToDefault(rejection.getMessage(), "Workflow dispatch rejected before process launch"));
            rejected.setZeroWriteEvidence(true);
            Path logPath = operationStore.getOperationLogPath(record.operationId());
            rejected.setResultLogPath(logPath.toString());
            // The exclusive claim proves this attempt never dispatched; a previous claim is never overwritten.
            if (operationStore.createIfAbsent(rejected)) {
                operationStore.initializeLog(logPath);
                appendOperationLog(logPath, "BLOCKED: " + rejected.getSummary() + System.lineSeparator(), rejection);
            }
        } catch (RuntimeException evidenceFailure) {
            rejection.addSuppressed(evidenceFailure);
        }
    }

    @Override
    public void validateBackupPublishPrerequisites() {
        properties.requireBackupPublishTarget();
        responsibilityService.validateRequiredOwners("backup", "publish-backup");
        RuntimeControlRestoreIsolationConfig.resolve(properties);
    }

    @Override
    public boolean isOperationExecutorAlive(String operationId) {
        return commandExecutor.isOperationExecutorAlive(operationId);
    }

    @Override
    public Optional<RuntimeControlBackupPublicationReceipt> inspectBackupReceipt(ReleaseWorkflowRecord expected) {
        ReleaseWorkflowRecord workflow = requireBackupConfirmationContext(expected);
        RuntimeControlCommand command = backupConfirmationCommand(workflow, "inspect");
        Optional<RuntimeControlBackupPublicationReceipt> receipt = RuntimeControlBackupPublicationReceipt.parseOutput(
                commandExecutor.executeForOutput(command, java.time.Duration.ofMinutes(2)));
        receipt.ifPresent(value -> {
            value.verifyFor(workflow);
            operationStore.archiveBackupReceipt(workflow.operationId(), value, null);
        });
        return receipt;
    }

    @Override
    public RuntimeControlBackupPublicationReceipt acknowledgeBackupReceipt(ReleaseWorkflowRecord expected,
            RuntimeControlBackupPublicationReceipt receipt, String decisionDigest) {
        ReleaseWorkflowRecord workflow = requireBackupConfirmationContext(expected);
        if (!"BACKUP_FINALIZING".equals(workflow.state().name())) {
            throw new IllegalStateException("BACKUP_FINALIZING_REQUIRED");
        }
        receipt.verifyFor(workflow);
        requireAcceptedDecision(workflow, receipt, decisionDigest);
        RuntimeControlCommand command = backupConfirmationCommand(workflow, "ack");
        appendRequiredArgument(command.getArguments(), "-LeaseToken", receipt.binding().leaseToken());
        appendRequiredArgument(command.getArguments(), "-ExpectedReceiptDigest", receipt.receiptDigest());
        appendRequiredArgument(command.getArguments(), "-ConfirmationDecisionDigest", decisionDigest);
        appendRequiredArgument(command.getArguments(), "-ConfirmText", "PROD");
        // Even a recovered CONFIRMED receipt must finish the idempotent owner-release ACK.
        RuntimeControlBackupPublicationReceipt confirmed = RuntimeControlBackupPublicationReceipt.parseOutput(
                        commandExecutor.executeForOutput(command, java.time.Duration.ofMinutes(2)))
                .orElseThrow(() -> new IllegalStateException("BACKUP_ACK_CONFIRMATION_REQUIRED"));
        confirmed.verifyFor(workflow);
        if (!"CONFIRMED".equals(confirmed.state()) || !receipt.sameImmutableReceipt(confirmed)
                || !decisionDigest.equals(confirmed.confirmationDecisionDigest())) {
            throw new IllegalStateException("BACKUP_ACK_CONFIRMATION_MISMATCH");
        }
        operationStore.archiveBackupReceipt(workflow.operationId(), confirmed, null);
        return confirmed;
    }

    @Override
    public void completeBackupConfirmation(ReleaseWorkflowRecord expected,
                                            RuntimeControlBackupPublicationReceipt confirmedReceipt) {
        ReleaseWorkflowRecord workflow = requireBackupConfirmationContext(expected);
        if (!List.of("BACKUP_FINALIZING", "BACKUP_DEPLOYED").contains(workflow.state().name())) {
            throw new IllegalStateException("BACKUP_FINALIZING_REQUIRED");
        }
        confirmedReceipt.verifyFor(workflow);
        if (!"CONFIRMED".equals(confirmedReceipt.state())) throw new IllegalStateException("BACKUP_ACK_CONFIRMATION_REQUIRED");
        requireAcceptedDecision(workflow, confirmedReceipt, confirmedReceipt.confirmationDecisionDigest());
        Path confirmation = backupFinalizationPath(workflow, ".confirmation.json");
        try {
            if (!Files.isRegularFile(confirmation) || Files.isSymbolicLink(confirmation)
                    || !confirmedReceipt.equals(objectMapper.readValue(confirmation.toFile(), RuntimeControlBackupPublicationReceipt.class))) {
                throw new IllegalStateException("BACKUP_LOCAL_CONFIRMATION_REQUIRED");
            }
        } catch (IOException ex) {
            throw new IllegalStateException("BACKUP_LOCAL_CONFIRMATION_INVALID", ex);
        }
        operationStore.completeBackupPublication(workflow.operationId(), confirmedReceipt);
    }

    private ReleaseWorkflowRecord requireBackupConfirmationContext(ReleaseWorkflowRecord expected) {
        if (expected == null) throw new IllegalArgumentException("BACKUP_WORKFLOW_REQUIRED");
        ReleaseWorkflowRecord current = new ReleaseWorkflowStore(properties).require(expected.workflowId());
        if (current.backupIntent() == null || !List.of("BACKUP_DEPLOYING", "BACKUP_FINALIZING", "RECOVERY_REQUIRED", "BACKUP_DEPLOYED")
                .contains(current.state().name()) || !java.util.Objects.equals(current.operationId(), expected.operationId())
                || !java.util.Objects.equals(current.releaseTag(), expected.releaseTag())
                || !java.util.Objects.equals(current.packageDigest(), expected.packageDigest())
                || !java.util.Objects.equals(current.manifestDigest(), expected.manifestDigest())
                || !java.util.Objects.equals(current.sourceSelectionId(), expected.sourceSelectionId())
                || !java.util.Objects.equals(current.maintenanceCommit(), expected.maintenanceCommit())
                || !java.util.Objects.equals(current.applicationCommit(), expected.applicationCommit())
                || !java.util.Objects.equals(current.frontendCommit(), expected.frontendCommit())
                || !java.util.Objects.equals(current.backupIntent(), expected.backupIntent())
                || !java.util.Objects.equals(current.requestedBy(), expected.requestedBy())
                || !current.backupIntent().targetFingerprint().equals(ReleaseWorkflowBackupAuthorizationService.targetFingerprint(properties))) {
            throw new IllegalStateException("BACKUP_CONFIRMATION_CONTEXT_MISMATCH");
        }
        RuntimeControlOperationRespVO operation = operationStore.findById(current.operationId());
        if (operation == null || !"publish-backup".equals(operation.getAction()) || !"backup".equals(operation.getEnvironment())
                || !java.util.Objects.equals(current.requestedBy(), operation.getRequestedBy()) || operation.getParameters() == null) {
            throw new IllegalStateException("BACKUP_CONFIRMATION_OPERATION_MISMATCH");
        }
        Map<String, String> tuple = Map.of("workflowId", current.workflowId(), "releaseTag", current.releaseTag(),
                "authorizationId", current.backupIntent().authorizationId(), "sourceSelectionId", current.sourceSelectionId(),
                "maintenanceCommit", current.maintenanceCommit(), "applicationCommit", current.applicationCommit(),
                "frontendCommit", current.frontendCommit(), "packageDigest", java.util.Objects.requireNonNull(current.packageDigest()),
                "manifestDigest", java.util.Objects.requireNonNull(current.manifestDigest()));
        for (var field : tuple.entrySet()) {
            if (!field.getValue().equals(operation.getParameters().get(field.getKey()))) {
                throw new IllegalStateException("BACKUP_CONFIRMATION_OPERATION_BINDING_MISMATCH");
            }
        }
        return current;
    }

    private RuntimeControlCommand backupConfirmationCommand(ReleaseWorkflowRecord workflow, String mode) {
        var target = properties.requireBackupPublishTarget();
        var frozen = new ReleaseWorkflowWorktreeFactory(properties).requirePrepared(workflow);
        Path script = frozen.maintenanceRoot().resolve("ops/deploy/confirm-review-publish.ps1").normalize();
        if (!script.startsWith(frozen.maintenanceRoot()) || !Files.isRegularFile(script) || Files.isSymbolicLink(script)) {
            throw new IllegalStateException("BACKUP_CONFIRMATION_EXECUTOR_MISSING");
        }
        List<String> arguments = new ArrayList<>();
        appendRequiredArgument(arguments, "-Mode", mode);
        appendRequiredArgument(arguments, "-ServerHost", target.getHost());
        appendRequiredArgument(arguments, "-ServerUser", target.getServerUser());
        appendRequiredArgument(arguments, "-RemoteAppDir", target.getRemoteAppDir());
        appendRequiredArgument(arguments, "-ReleaseWorkflowId", workflow.workflowId());
        appendRequiredArgument(arguments, "-JavaOperationId", workflow.operationId());
        appendRequiredArgument(arguments, "-ReleaseTag", workflow.releaseTag());
        appendRequiredArgument(arguments, "-ExpectedPackageDigest", workflow.packageDigest());
        appendRequiredArgument(arguments, "-ExpectedManifestDigest", workflow.manifestDigest());
        return new RuntimeControlCommand("backup", "ops", script.toString(), arguments, frozen.maintenanceRoot().toString());
    }

    private Path backupFinalizationPath(ReleaseWorkflowRecord workflow, String suffix) {
        return Path.of(properties.getStateDir()).resolve("backup-finalization").resolve(workflow.workflowId() + suffix);
    }

    private void requireAcceptedDecision(ReleaseWorkflowRecord workflow, RuntimeControlBackupPublicationReceipt receipt,
                                         String decisionDigest) {
        Path decision = backupFinalizationPath(workflow, ".decision.json");
        try {
            if (decisionDigest == null || !decisionDigest.matches("[0-9a-f]{64}")
                    || !Files.isRegularFile(decision) || Files.isSymbolicLink(decision)) {
                throw new IllegalStateException("BACKUP_ACCEPTED_DECISION_REQUIRED");
            }
            byte[] bytes = Files.readAllBytes(decision);
            if (!RuntimeControlBackupPublicationReceipt.sha256(bytes).equals(decisionDigest)) {
                throw new IllegalStateException("BACKUP_ACCEPTED_DECISION_DIGEST_MISMATCH");
            }
            JsonNode data = objectMapper.readTree(bytes);
            if (data == null || data.path("schemaVersion").asInt() != 1 || !"ACCEPTED".equals(data.path("decision").asText())
                    || !workflow.workflowId().equals(data.path("workflowId").asText())
                    || !workflow.operationId().equals(data.path("operationId").asText())
                    || !workflow.releaseTag().equals(data.path("releaseTag").asText())
                    || !workflow.sourceSelectionId().equals(data.path("sourceSelectionId").asText())
                    || !workflow.maintenanceCommit().equals(data.path("maintenanceCommit").asText())
                    || !workflow.applicationCommit().equals(data.path("applicationCommit").asText())
                    || !workflow.frontendCommit().equals(data.path("frontendCommit").asText())
                    || !receipt.awaitingConfirmation().equals(objectMapper.treeToValue(data.path("receipt"), RuntimeControlBackupPublicationReceipt.class))) {
                throw new IllegalStateException("BACKUP_ACCEPTED_DECISION_BINDING_MISMATCH");
            }
        } catch (IOException ex) {
            throw new IllegalStateException("BACKUP_ACCEPTED_DECISION_INVALID", ex);
        }
    }

    @Override
    public void rejectLegacyProductionAction(RuntimeControlActionReqVO reqVO) {
        RuntimeControlOperationAction action = reqVO == null ? null
                : RuntimeControlOperationAction.fromAction(reqVO.getAction());
        if (action != null && action.requiresReleaseWorkflowContext()) {
            throw exception(RUNTIME_CONTROL_INVALID_ACTION,
                    action.getAction() + " 必须通过程序发布工作流按钮发起");
        }
    }

    @Override
    public boolean cancelOperation(String operationId) {
        RuntimeControlOperationRespVO operation = operationStore.findById(operationId);
        if (operation == null) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, "operationId");
        }
        if (!"running".equals(operation.getStatus())) {
            return true;
        }
        canceledOperations.add(operationId);
        boolean terminated;
        try {
            terminated = commandExecutor.cancelOperation(operationId);
        } catch (RuntimeException ex) {
            canceledOperations.remove(operationId);
            throw ex;
        }
        if (!terminated) {
            canceledOperations.remove(operationId);
            return false;
        }
        operationStore.updateStatus(operationId, "canceled", "Operation canceled after process termination");
        return true;
    }

    @Override
    public RuntimeControlActionPreviewRespVO previewAction(RuntimeControlActionReqVO reqVO, String requestedBy) {
        String operator = requireOperator(requestedBy, "requestedBy");
        RuntimeControlOperationAction action = RuntimeControlOperationAction.fromAction(reqVO.getAction());
        if (action == null) {
            throw exception(RUNTIME_CONTROL_INVALID_ACTION, reqVO.getAction());
        }
        validateIndependentBackupBinding(action, reqVO, operator);
        RuntimeControlReleasePackageConfig backendRuntimeBaseConfig = validateActionGuard(action, reqVO);
        RuntimeControlCommand command = new RuntimeControlCommand(action.resolveEnvironment(reqVO), "ops",
                action.resolveScriptPath(properties), action.buildArguments(reqVO, operator, properties));
        bindReleaseWorkflowToolchain(action, command, reqVO);
        appendRestoreIsolationArguments(action, command);
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

    private void bindReleaseWorkflowToolchain(RuntimeControlOperationAction action, RuntimeControlCommand command,
                                               RuntimeControlActionReqVO request) {
        if (!action.requiresReleaseWorkflowContext()) {
            return;
        }
        if (isIndependentBackup(action, request)) {
            bindFrozenBackupToolchain(action, command, request);
            return;
        }
        RuntimeControlProperties.ReleaseWorkflow releaseWorkflow = properties.getReleaseWorkflow();
        ReleaseWorkflowExecutorContract.VerifiedExecutor executor;
        try {
            executor = ReleaseWorkflowExecutorContract.verify(properties);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, ex.getMessage());
        }
        command.setWorkingDirectory(executor.maintenanceRoot().toString());
        command.setScriptPath(executor.script().toString());
        if (action == RuntimeControlOperationAction.BUILD_RELEASE) {
            appendRequiredArgument(command.getArguments(), "-BackendRepoRoot",
                    appendConfiguredPath(releaseWorkflow.getApplicationRepoRoot(), "IntRuoyiBackend"));
            appendRequiredArgument(command.getArguments(), "-FrontendRepoRoot",
                    appendConfiguredPath(releaseWorkflow.getApplicationRepoRoot(), "IntRuoyiFronted"));
        }
    }

    private void bindFrozenBackupToolchain(RuntimeControlOperationAction action, RuntimeControlCommand command,
                                           RuntimeControlActionReqVO request) {
        try {
            if (StrUtil.isBlank(request.getFrozenPublishScriptSha256())
                    || !request.getFrozenPublishScriptSha256().equalsIgnoreCase(
                            properties.getReleaseWorkflow().getExpectedPublishScriptSha256())) {
                throw new IllegalArgumentException("RELEASE_EXECUTOR_DIGEST_MISMATCH: frozen executor is not approved");
            }
            String configuredRoot = properties.getReleaseWorkflow().getWorktreeRoot();
            if (StrUtil.isBlank(configuredRoot)) {
                throw new IllegalArgumentException("RELEASE_WORKFLOW_WORKTREE_ROOT_REQUIRED");
            }
            Path workflowRoot = Path.of(configuredRoot).toRealPath().resolve(request.getReleaseWorkflowId());
            Path maintenance = requireFrozenRoot(request.getFrozenMaintenanceRoot(), workflowRoot.resolve("maintenance"));
            Path backend = requireFrozenRoot(request.getFrozenBackendRoot(), workflowRoot.resolve("application/IntRuoyiBackend"));
            Path frontend = requireFrozenRoot(request.getFrozenFrontendRoot(), workflowRoot.resolve("application/IntRuoyiFronted"));
            RuntimeControlProperties isolated = new RuntimeControlProperties();
            org.springframework.beans.BeanUtils.copyProperties(properties.getReleaseWorkflow(), isolated.getReleaseWorkflow());
            isolated.getReleaseWorkflow().setMaintenanceRepoRoot(maintenance.toString());
            isolated.getReleaseWorkflow().setExpectedPublishScriptSha256(request.getFrozenPublishScriptSha256());
            var verified = ReleaseWorkflowExecutorContract.verify(isolated);
            command.setWorkingDirectory(verified.maintenanceRoot().toString());
            command.setScriptPath(verified.script().toString());
            if (action == RuntimeControlOperationAction.BUILD_RELEASE) {
                appendRequiredArgument(command.getArguments(), "-BackendRepoRoot", backend.toString());
                appendRequiredArgument(command.getArguments(), "-FrontendRepoRoot", frontend.toString());
            }
        } catch (IOException | IllegalArgumentException ex) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID,
                    "RELEASE_WORKFLOW_FROZEN_TOOLCHAIN_INVALID: " + ex.getMessage());
        }
    }

    private Path requireFrozenRoot(String actual, Path expected) throws IOException {
        if (StrUtil.isBlank(actual) || !Files.isDirectory(expected)
                || !Path.of(actual).toAbsolutePath().normalize().equals(expected.toAbsolutePath().normalize())
                || !Path.of(actual).toRealPath().equals(expected.toAbsolutePath().normalize())) {
            throw new IllegalArgumentException("RELEASE_WORKFLOW_FROZEN_ROOT_INVALID");
        }
        return expected;
    }

    private String appendConfiguredPath(String root, String child) {
        if (StrUtil.isBlank(root)) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, "releaseWorkflow.applicationRepoRoot");
        }
        try {
            return Path.of(StrUtil.trim(root)).resolve(child).normalize().toString().replace('\\', '/');
        } catch (InvalidPathException ex) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, "releaseWorkflow.applicationRepoRoot");
        }
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
        NasConnectionConfig config = releaseNasConfig();
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
        if ("publish-backup".equals(operation.getAction())
                || "build-release".equals(operation.getAction()) && "backup".equals(operation.getEnvironment())) {
            // Review workflow completion belongs to the executor/receipt/ACK protocol, never diagnostic text.
            return operation;
        }
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
        if (logContent.contains("操作完成：未知") || logContent.contains("结果代码：INTBK-UNKNOWN")) {
            String unknownSummary = StrUtil.blankToDefault(extractResultDescription(logContent),
                    actionLabel + " status unknown");
            return new RuntimeControlTerminalStatus("unknown", unknownSummary);
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
    public Optional<RuntimeControlReleasePackageRespVO> getReleasePackage(String releaseTag) {
        if (nasSettingsService == null || nasBrowserService == null) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, "nas release package dependencies");
        }
        String directoryName = StrUtil.trim(releaseTag);
        if (StrUtil.isBlank(directoryName)) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, "releaseTag");
        }
        if (StrUtil.containsAny(directoryName, "/", "\\")) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, "releaseTag");
        }
        NasConnectionConfig nasConfig = releaseNasConfig();
        String packagePath = releasePackagesRoot() + "/" + directoryName;
        FileNasListRespVO.Item item = new FileNasListRespVO.Item()
                .setName(directoryName)
                .setPath(packagePath)
                .setDir(true)
                .setSize(0L);
        RuntimeControlReleasePackageRespVO releasePackage = buildReleasePackageResponse(item, nasConfig);
        if (!RELEASE_PACKAGE_STATUS_AVAILABLE.equals(releasePackage.getStatus())) {
            return Optional.empty();
        }
        return Optional.of(releasePackage);
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
        String manifestPath = packagePath + "/manifest.json";
        List<String> blockedReasons = new ArrayList<>();
        respVO.setReleaseTag(directoryName);
        respVO.setPackageDirectoryName(directoryName);
        respVO.setManifestPath(manifestPath);

        List<String> packageFileNames = listReleasePackageFileNames(nasConfig, packagePath, blockedReasons);
        JsonNode manifest = null;
        if (packageFileNames.contains("manifest.json")) {
            ManifestContent manifestContent = readReleasePackageManifest(nasConfig, manifestPath, blockedReasons);
            if (manifestContent != null) {
                manifest = manifestContent.node();
                respVO.setManifestDigest(manifestContent.digest());
            }
        } else {
            blockedReasons.add("缺少 manifest.json");
        }
        if (manifest != null) {
            String releaseTag = text(manifest, "releaseTag");
            if (StrUtil.isNotBlank(releaseTag)) {
                respVO.setReleaseTag(releaseTag);
            }
            String packageDirectoryName = text(manifest, "packageId");
            respVO.setPackageDirectoryName(packageDirectoryName);
            respVO.setImageTag(packageDirectoryName);
            respVO.setBuiltAt(text(manifest, "createdAt"));
            respVO.setPublishScope(text(manifest, "publishScope"));
            respVO.setPackageDigest(text(manifest, "packageDigest"));
            respVO.setSourceRoots(readSourceRoots(manifest.get("sourceRoots")));
            respVO.setSourceRoles(readSourceRoles(manifest.get("sourceRoles")));
            String component = text(manifest, "component");
            respVO.setComponent(component);
            JsonNode components = manifest.get("components");
            Boolean includeShowroomBuildPackage = null;
            if (components != null && components.isArray()) {
                boolean validComponents = true;
                boolean includesWebsite = false;
                for (JsonNode entry : components) {
                    if (!entry.isTextual() || !List.of("backend", "admin-frontend", "website",
                            "database-contract", "required-sql", "runtime-env", "onlyoffice",
                            "packaging-manifest").contains(entry.asText())) {
                        validComponents = false;
                    }
                    includesWebsite |= "website".equals(entry.asText());
                }
                if (validComponents) {
                    includeShowroomBuildPackage = includesWebsite;
                }
            }
            respVO.setIncludeShowroomBuildPackage(includeShowroomBuildPackage);
            if (StrUtil.isBlank(component)) {
                blockedReasons.add("manifest.json 缺少 component");
            } else if (!List.of("full", "intruoyi", "backend", "frontend", "website").contains(component)) {
                blockedReasons.add("manifest.json component 非法");
            }
            if (includeShowroomBuildPackage == null) {
                blockedReasons.add("manifest.json components 缺失或非法");
            }
            Boolean onlyOfficeIncluded = booleanValue(manifest, "onlyOfficeIncluded");
            respVO.setOnlyOfficeIncluded(onlyOfficeIncluded);
            if (onlyOfficeIncluded == null) {
                blockedReasons.add("manifest.json 缺少 onlyOfficeIncluded");
            }
            if (StrUtil.isBlank(packageDirectoryName)) {
                blockedReasons.add("manifest.json 缺少 packageId");
            } else if (!directoryName.equals(packageDirectoryName)) {
                blockedReasons.add("manifest packageId 与目录不一致");
            }
            if (StrUtil.isBlank(respVO.getPackageDigest())) {
                blockedReasons.add("manifest.json 缺少 packageDigest");
            }
            if (respVO.getSourceRoots().size() != 2) {
                blockedReasons.add("manifest.json 缺少 sourceRoots");
            }
            if (respVO.getSourceRoles().size() != 3) {
                blockedReasons.add("manifest.json 缺少 sourceRoles");
            }
            boolean checksumPresent = hasReleasePackageChecksum(manifest);
            respVO.setChecksumPresent(checksumPresent);
            if (!checksumPresent) {
                blockedReasons.add("manifest.json 缺少 artifact sha256");
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

    private void populateReleasePackageTestedMetadata(NasConnectionConfig nasConfig, String packagePath,
                                                      List<String> packageFileNames,
                                                      RuntimeControlReleasePackageRespVO respVO) {
        String testedPath = packagePath + "/tested.json";
        if (!packageFileNames.contains("tested.json")) {
            respVO.setTested(false);
            return;
        }
        try {
            NasFileReadResult file = nasBrowserService.readFile(nasConfig, testedPath);
            JsonNode tested = objectMapper.readTree(file.bytes());
            if (tested == null || !tested.isObject()) {
                throw new IllegalArgumentException("tested.json must be an object");
            }
            respVO.setTested(true);
            respVO.setTestedDigest(ReleaseDigestContract.manifestDigest(file.bytes()));
            respVO.setTestedSchemaVersion(text(tested, "schemaVersion"));
            respVO.setTestedReleaseTag(text(tested, "releaseTag"));
            respVO.setTestedPackageDirectoryName(text(tested, "packageDirectoryName"));
            respVO.setTestedPackageDigest(text(tested, "packageDigest"));
            respVO.setTestedManifestDigest(text(tested, "manifestDigest"));
            respVO.setTestedEnvironment(text(tested, "testEnvironment"));
            respVO.setTestedOperationId(text(tested, "publishTestOperationId"));
            respVO.setTestedOperationStatus(text(tested, "publishTestOperationStatus"));
            respVO.setTestedOperationRequestedAt(text(tested, "publishTestRequestedAt"));
            respVO.setTestedResult(text(tested, "testResult"));
            respVO.setTestedConclusion(text(tested, "testConclusion"));
            respVO.setTestedAt(text(tested, "testedAt"));
            respVO.setOperatorName(text(tested, "testedBy"));
            JsonNode recoverySet = tested.get("recoverySet");
            respVO.setTestedRecoverySetCandidateId(text(recoverySet, "selectedRecoverySetCandidateId"));
            respVO.setTestedRecoverySetId(text(recoverySet, "recoverySetId"));
            respVO.setTestedRecoverySetManifestHash(text(recoverySet, "recoverySetManifestHash"));
        } catch (IOException | IllegalArgumentException | ServiceException ex) {
            respVO.setTested(false);
            respVO.setTestedValidationError("tested.json unreadable or invalid: " + ex.getClass().getSimpleName());
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

    private ManifestContent readReleasePackageManifest(NasConnectionConfig nasConfig, String path,
                                                       List<String> blockedReasons) {
        try {
            NasFileReadResult result = nasBrowserService.readFile(nasConfig, path);
            JsonNode node = objectMapper.readTree(new String(result.bytes(), StandardCharsets.UTF_8));
            return new ManifestContent(node, ReleaseDigestContract.manifestDigest(result.bytes()));
        } catch (ServiceException ex) {
            blockedReasons.add("缺少 manifest.json");
            return null;
        } catch (IOException | IllegalArgumentException ex) {
            blockedReasons.add("manifest.json 解析失败：" + ex.getMessage());
            return null;
        }
    }

    private List<RuntimeControlReleasePackageRespVO.SourceRoot> readSourceRoots(JsonNode sourceRoots) {
        if (sourceRoots == null || !sourceRoots.isArray()) {
            return List.of();
        }
        List<RuntimeControlReleasePackageRespVO.SourceRoot> result = new ArrayList<>();
        for (JsonNode sourceRoot : sourceRoots) {
            RuntimeControlReleasePackageRespVO.SourceRoot item = new RuntimeControlReleasePackageRespVO.SourceRoot();
            item.setRootRole(text(sourceRoot, "rootRole"));
            item.setNormalizedRoot(text(sourceRoot, "normalizedRoot"));
            item.setApprovedCommit(text(sourceRoot, "approvedCommit"));
            item.setCommit(text(sourceRoot, "commit"));
            item.setDirty(booleanValue(sourceRoot, "dirty"));
            result.add(item);
        }
        return result;
    }

    private List<RuntimeControlReleasePackageRespVO.SourceRole> readSourceRoles(JsonNode sourceRoles) {
        if (sourceRoles == null || !sourceRoles.isArray()) {
            return List.of();
        }
        List<RuntimeControlReleasePackageRespVO.SourceRole> result = new ArrayList<>();
        for (JsonNode sourceRole : sourceRoles) {
            RuntimeControlReleasePackageRespVO.SourceRole item = new RuntimeControlReleasePackageRespVO.SourceRole();
            item.setSourceRole(text(sourceRole, "sourceRole"));
            item.setRootRole(text(sourceRole, "rootRole"));
            item.setRelativePath(text(sourceRole, "relativePath"));
            item.setCommit(text(sourceRole, "commit"));
            result.add(item);
        }
        return result;
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
                                      RuntimeControlCommand command, Path logPath, Path nasConfigPath,
                                      RuntimeControlActionReqVO request, String operator) {
        RuntimeException failure = null;
        boolean dispatched = false;
        try {
            validateIndependentBackupBinding(action, request, operator, false);
            if (isIndependentBackup(action, request)) {
                validateNoUnknownOperation("backup", action == RuntimeControlOperationAction.PUBLISH_BACKUP
                        ? request.getReleaseWorkflowId() : null);
            }
            dispatched = true;
            commandExecutor.executeOperation(command, logPath);
        } catch (RuntimeException ex) {
            failure = ex;
        }
        if (action == RuntimeControlOperationAction.PUBLISH_BACKUP && dispatched) {
            try {
                RuntimeControlBackupPublicationReceipt receipt = RuntimeControlBackupPublicationReceipt.parseLog(logPath)
                        .orElseThrow(() -> new IllegalStateException("BACKUP_PUBLICATION_RECEIPT_REQUIRED"));
                ReleaseWorkflowRecord workflow = new ReleaseWorkflowStore(properties).require(request.getReleaseWorkflowId());
                receipt.verifyFor(workflow);
                operationStore.archiveBackupReceipt(operationId, receipt, failure == null ? null : failure.getMessage());
                // Receipt and its retained remote owner are independent of local credential-file cleanup.
                RuntimeException cleanupFailure = cleanupNasReleaseConfig(nasConfigPath, null);
                if (cleanupFailure != null) {
                    operationStore.recordLocalCleanupError(operationId, cleanupFailure.getMessage());
                    if (failure == null) failure = cleanupFailure;
                    else failure.addSuppressed(cleanupFailure);
                }
                canceledOperations.remove(operationId);
                if (failure != null) throw new BackupReceiptRetainedException(failure);
                return;
            } catch (BackupReceiptRetainedException retained) {
                throw retained;
            } catch (IOException | RuntimeException receiptFailure) {
                if (failure == null) failure = new IllegalStateException(
                        "BACKUP_PUBLICATION_RECEIPT_UNCONFIRMED: " + receiptFailure.getMessage(), receiptFailure);
                else failure.addSuppressed(receiptFailure);
            }
        }
        boolean zeroWriteEvidence = failure != null && !dispatched;
        if (failure != null && dispatched && isIndependentBackup(action, request)) {
            try {
                zeroWriteEvidence = RuntimeControlExecutionEvidence.confirmedZeroWriteRejection(logPath);
            } catch (IOException evidenceFailure) {
                failure.addSuppressed(evidenceFailure);
            }
        }
        failure = cleanupNasReleaseConfig(nasConfigPath, failure);
        if (canceledOperations.contains(operationId)) {
            if (failure != null) {
                operationStore.updateResult(operationId, "canceled", "Operation canceled after process termination", zeroWriteEvidence);
            }
            canceledOperations.remove(operationId);
            return;
        }
        if (failure != null) {
            operationStore.updateResult(operationId, "failed", StrUtil.blankToDefault(failure.getMessage(), "Operation failed"), zeroWriteEvidence);
            throw failure;
        }
        operationStore.updateStatus(operationId, "succeeded", action.getLabel() + " completed");
    }

    private static final class BackupReceiptRetainedException extends IllegalStateException {
        private BackupReceiptRetainedException(RuntimeException cause) {
            super("BACKUP_PUBLICATION_RECEIPT_RETAINED: " + cause.getMessage(), cause);
        }
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
        if (canceledOperations.contains(operationId)) {
            if (failure != null) {
                operationStore.updateStatus(operationId, "canceled", "Operation canceled after process termination");
            }
            canceledOperations.remove(operationId);
            return;
        }
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
            String testResult = StrUtil.trimToEmpty(reqVO.getTestResult()).toUpperCase(Locale.ROOT);
            if (StrUtil.isBlank(testResult)) {
                throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, "testResult");
            }
            if (!"PASS".equals(testResult)) {
                throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, "testResult");
            }
            reqVO.setTestResult(testResult);
            if (StrUtil.isBlank(reqVO.getTestConclusion())) {
                throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, "testConclusion");
            }
            reqVO.setTestConclusion(StrUtil.trim(reqVO.getTestConclusion()));
            if (StrUtil.isBlank(reqVO.getTestOperationId())) {
                throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, "testOperationId");
            }
            if (StrUtil.isBlank(reqVO.getTestOperationEvidencePath())) {
                throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, "testOperationEvidencePath");
            }
            reqVO.setReleaseTag(resolveCurrentReleaseTag("test"));
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
        validateReleaseWorkflowContext(action, reqVO);
        if (action.requiresReleaseWorkflowContext() && action != RuntimeControlOperationAction.BUILD_RELEASE) {
            if (StrUtil.isBlank(reqVO.getExpectedPackageDigest())
                    || StrUtil.isBlank(reqVO.getExpectedManifestDigest())) {
                throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, "expectedPackageDigest/expectedManifestDigest");
            }
            if (!reqVO.getExpectedPackageDigest().matches("[0-9a-f]{64}")
                    || !reqVO.getExpectedManifestDigest().matches("[0-9a-f]{64}")) {
                throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, "expectedPackageDigest/expectedManifestDigest");
            }
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
                || action == RuntimeControlOperationAction.PUBLISH_BACKUP
                || action == RuntimeControlOperationAction.PROMOTE_PROD
                || action == RuntimeControlOperationAction.PROMOTE_BACKUP) {
            validateRemoteDeployTargetHostConfig(action.resolveEnvironment(reqVO));
            validateReleaseTargetHostConfig();
        }
        if ((action == RuntimeControlOperationAction.PROMOTE_PROD
                || action == RuntimeControlOperationAction.PROMOTE_BACKUP)
                && (StrUtil.isBlank(reqVO.getTestOperationId())
                || StrUtil.isBlank(reqVO.getTestOperationEvidencePath()))) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, "testOperationEvidence");
        }
        if (action.requiresResponsibilityGate()) {
            responsibilityService.validateRequiredOwners(action.resolveEnvironment(reqVO), action.getAction());
        }
        RuntimeControlReleasePackageRespVO releasePackage = validateReleasePackageAvailability(action,
                reqVO.getReleaseTag());
        if (releasePackage != null
                && (!reqVO.getExpectedPackageDigest().equals(releasePackage.getPackageDigest())
                || !reqVO.getExpectedManifestDigest().equals(releasePackage.getManifestDigest()))) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, "RELEASE_WORKFLOW_PACKAGE_BINDING_MISMATCH");
        }
        if (action == RuntimeControlOperationAction.PROMOTE_BACKUP) {
            recheckPromoteBackupRecoverySet(releasePackage);
        }
        if (action == RuntimeControlOperationAction.ROLLBACK_APP) {
            RuntimeControlRollbackCandidateRespVO candidate =
                    candidateService.requireAvailableRollbackCandidate(reqVO.getSelectedImageCandidateId());
            reqVO.setSelectedImageTag(candidate.getImageTag());
        }
        if (action == RuntimeControlOperationAction.REHEARSAL || action == RuntimeControlOperationAction.RESTORE_DATA) {
            bindRecoverySetCandidate(action, reqVO);
        }
        return backendRuntimeBaseConfig;
    }

    private String resolveOperationId(RuntimeControlActionReqVO reqVO) {
        String preassigned = StrUtil.trimToNull(reqVO.getPreassignedOperationId());
        if (preassigned == null) {
            return UUID.randomUUID().toString();
        }
        if (!preassigned.matches("(?:op-)?[a-z0-9-]{8,64}")) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, "preassignedOperationId");
        }
        return preassigned;
    }

    private void validateReleaseWorkflowContext(RuntimeControlOperationAction action, RuntimeControlActionReqVO reqVO) {
        if (!action.requiresReleaseWorkflowContext()) {
            return;
        }
        if (StrUtil.isBlank(reqVO.getReleaseWorkflowId())
                || reqVO.getReleaseWorkflowExpectedStateVersion() == null
                || StrUtil.isBlank(reqVO.getPreassignedOperationId())) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, "releaseWorkflowContext");
        }
        if (!StrUtil.trim(reqVO.getReleaseWorkflowId()).matches("rw-(?:backup-)?[a-z0-9]{8,32}")
                && !StrUtil.trim(reqVO.getReleaseWorkflowId()).matches("wf-[a-z0-9-]{6,64}")) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, "releaseWorkflowId");
        }
        if (reqVO.getReleaseWorkflowExpectedStateVersion() < 0) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, "releaseWorkflowExpectedStateVersion");
        }
        resolveOperationId(reqVO);
    }

    private void appendRestoreIsolationArguments(RuntimeControlOperationAction action, RuntimeControlCommand command) {
        if (action.requiresReleaseWorkflowContext() && action != RuntimeControlOperationAction.MARK_RELEASE_TESTED) {
            appendRequiredArgument(command.getArguments(), "-RestoreIsolationMarkerPath",
                    RuntimeControlRestoreIsolationConfig.resolve(properties));
        }
    }

    private boolean isIndependentBackup(RuntimeControlOperationAction action, RuntimeControlActionReqVO request) {
        return action == RuntimeControlOperationAction.PUBLISH_BACKUP
                || action == RuntimeControlOperationAction.BUILD_RELEASE && "backup".equals(request.getTargetEnvironment());
    }

    private Map<String, String> operationParameters(RuntimeControlOperationAction action, RuntimeControlActionReqVO request) {
        Map<String, String> parameters = new LinkedHashMap<>(action.safeParameters(request));
        if (isIndependentBackup(action, request)) {
            parameters.put("workflowId", request.getReleaseWorkflowId());
            parameters.put("authorizationId", request.getReleaseAuthorizationId());
            parameters.put("sourceSelectionId", request.getSourceSelectionId());
            parameters.put("maintenanceCommit", request.getExpectedMaintenanceCommit());
            parameters.put("applicationCommit", request.getExpectedApplicationCommit());
            parameters.put("frontendCommit", request.getExpectedFrontendCommit());
            parameters.put("packageDigest", request.getExpectedPackageDigest());
            parameters.put("manifestDigest", request.getExpectedManifestDigest());
        }
        return parameters;
    }

    private void validateIndependentBackupBinding(RuntimeControlOperationAction action,
                                                   RuntimeControlActionReqVO request, String operator) {
        validateIndependentBackupBinding(action, request, operator, true);
    }

    private void validateIndependentBackupBinding(RuntimeControlOperationAction action,
                                                   RuntimeControlActionReqVO request, String operator,
                                                   boolean exactVersion) {
        if (!isIndependentBackup(action, request)) {
            return;
        }
        validateReleaseWorkflowContext(action, request);
        ReleaseWorkflowRecord record = new ReleaseWorkflowStore(properties).require(request.getReleaseWorkflowId());
        boolean expectedState = action == RuntimeControlOperationAction.BUILD_RELEASE
                ? (exactVersion ? record.state() == ReleaseWorkflowRecord.State.PREFLIGHTING
                    : Set.of(ReleaseWorkflowRecord.State.PREFLIGHTING, ReleaseWorkflowRecord.State.TESTING,
                            ReleaseWorkflowRecord.State.BUILDING).contains(record.state()))
                : record.state() == ReleaseWorkflowRecord.State.BACKUP_DEPLOYING;
        if (record.backupIntent() == null || !expectedState
                || (exactVersion ? request.getReleaseWorkflowExpectedStateVersion() != record.stateVersion()
                    : request.getReleaseWorkflowExpectedStateVersion() > record.stateVersion())
                || !java.util.Objects.equals(record.operationId(), request.getPreassignedOperationId())
                || !java.util.Objects.equals(record.requestedBy(), operator)
                || !java.util.Objects.equals(record.reason(), request.getReason())
                || !java.util.Objects.equals(record.releaseTag(), request.getReleaseTag())
                || !java.util.Objects.equals(record.sourceSelectionId(), request.getSourceSelectionId())
                || !java.util.Objects.equals(record.maintenanceCommit(), request.getExpectedMaintenanceCommit())
                || !java.util.Objects.equals(record.applicationCommit(), request.getExpectedApplicationCommit())
                || !java.util.Objects.equals(record.frontendCommit(), request.getExpectedFrontendCommit())
                || !java.util.Objects.equals(record.backupIntent().authorizationId(), request.getReleaseAuthorizationId())
                || !java.util.Objects.equals(record.backupIntent().targetFingerprint(),
                    ReleaseWorkflowBackupAuthorizationService.targetFingerprint(properties))) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, "RELEASE_WORKFLOW_BACKUP_BINDING_MISMATCH");
        }
        properties.requireBackupPublishTarget();
        if (!"PROD".equals(request.getProdConfirmText())) {
            throw exception(RUNTIME_CONTROL_PROD_GUARD_REQUIRED);
        }
        if (action == RuntimeControlOperationAction.PUBLISH_BACKUP
                && (record.packageDigest() == null || record.manifestDigest() == null
                || !record.packageDigest().equals(request.getExpectedPackageDigest())
                || !record.manifestDigest().equals(request.getExpectedManifestDigest()))) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, "RELEASE_WORKFLOW_PACKAGE_BINDING_MISMATCH");
        }
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
                && action != RuntimeControlOperationAction.PUBLISH_BACKUP
                && action != RuntimeControlOperationAction.PROMOTE_PROD
                && action != RuntimeControlOperationAction.PROMOTE_BACKUP) {
            return null;
        }
        RuntimeControlReleasePackageRespVO releasePackage = getReleasePackage(releaseTag)
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

    private void bindRecoverySetCandidate(RuntimeControlOperationAction action, RuntimeControlActionReqVO reqVO) {
        String requiredCandidateType = action == RuntimeControlOperationAction.REHEARSAL
                ? "REHEARSAL" : "CONTROLLED_RESTORE";
        RuntimeControlRestoreCandidateRespVO candidate =
                candidateService.requireAvailableRestoreCandidate(reqVO.getSelectedRecoverySetCandidateId(),
                        requiredCandidateType);
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
                candidateService.requireAvailableRestoreCandidate(releasePackage.getTestedRecoverySetCandidateId(),
                        "CONTROLLED_RESTORE");
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
        if (isIndependentBackup(action, reqVO)) {
            if (StrUtil.isNotBlank(reqVO.getTargetEnvironment()) && !"backup".equals(reqVO.getTargetEnvironment())) {
                throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, "targetEnvironment");
            }
            return;
        }
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
        // Only this action has already passed the persisted independent-publisher binding checks.
        validateNoUnknownOperation(environment, action == RuntimeControlOperationAction.PUBLISH_BACKUP
                ? reqVO.getReleaseWorkflowId() : null);
        if (!runtimeEnvironment.isAccessEnabled()) {
            if (action == RuntimeControlOperationAction.BACKUP_NOW && "prod".equals(environment)) {
                return;
            }
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, environment + " 环境未启用："
                    + StrUtil.blankToDefault(runtimeEnvironment.getAccessDisabledReason(), "环境访问未启用"));
        }
    }

    private void validateNoUnknownOperation(String environment, String validatedPublisherWorkflowId) {
        RuntimeControlOperationRespVO unknownOperation = operationStore.findUnknownOperation(environment);
        if (unknownOperation != null) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID,
                    environment + " 环境存在 UNKNOWN 操作，保持写保护直到人工确认并收口："
                            + unknownOperation.getOperationId());
        }
        if ("backup".equals(environment)) {
            for (ReleaseWorkflowRecord workflow : new ReleaseWorkflowStore(properties).list()) {
                if (!"backup".equals(workflow.targetEnvironment())) {
                    continue;
                }
                boolean recoveryRequired = workflow.state() == ReleaseWorkflowRecord.State.RECOVERY_REQUIRED;
                boolean finalizing = "BACKUP_FINALIZING".equals(workflow.state().name());
                boolean anotherPublisher = workflow.state() == ReleaseWorkflowRecord.State.BACKUP_DEPLOYING
                        && !workflow.workflowId().equals(validatedPublisherWorkflowId);
                if (recoveryRequired || finalizing || anotherPublisher) {
                    throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID,
                            "backup 环境存在 " + workflow.state() + " 工作流，验收或正式恢复完成前保持写保护："
                                    + workflow.workflowId());
                }
            }
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

    private record ManifestContent(JsonNode node, String digest) {
    }
}
