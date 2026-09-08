package cn.iocoder.yudao.module.infra.service.backupplan;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.infra.controller.admin.backupplan.vo.BackupPlanHistoryPageReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.backupplan.vo.BackupPlanScheduleSaveReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.backupplan.vo.BackupPlanStatusRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlBackupPointRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo.RuntimeControlOperationRespVO;
import cn.iocoder.yudao.module.infra.framework.runtimecontrol.config.RuntimeControlProperties;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeBackupDrillService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.RUNTIME_CONTROL_ACTION_PARAMETER_INVALID;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.RUNTIME_CONTROL_OPERATION_STORE_FAILED;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.RUNTIME_CONTROL_SCRIPT_NOT_EXISTS;

@Service
public class BackupPlanServiceImpl implements BackupPlanService {

    private static final List<String> WEEKDAYS = List.of("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN");
    private static final List<String> BACKUP_KINDS = List.of("FULL", "INCREMENTAL");

    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    @Resource
    private RuntimeControlProperties properties;
    @Resource
    private BackupPlanSchedulerGateway schedulerGateway;
    @Resource
    private RuntimeBackupDrillService backupDrillService;
    @Resource
    private BackupPlanOperationGateway operationGateway;

    public BackupPlanServiceImpl() {
    }

    BackupPlanServiceImpl(RuntimeControlProperties properties, BackupPlanSchedulerGateway schedulerGateway,
                          RuntimeBackupDrillService backupDrillService, BackupPlanOperationGateway operationGateway) {
        this.properties = properties;
        this.schedulerGateway = schedulerGateway;
        this.backupDrillService = backupDrillService;
        this.operationGateway = operationGateway;
    }

    @Override
    public BackupPlanStatusRespVO getStatus() {
        BackupPlanSchedule schedule = readSchedule(false);
        BackupPlanSchedulerStatus schedulerStatus = schedulerGateway.getStatus();
        List<RuntimeControlBackupPointRespVO> backupPoints = backupDrillService.listBackupPoints();
        return buildStatus(schedule, schedulerStatus, backupPoints.isEmpty() ? null : backupPoints.get(0));
    }

    @Override
    public BackupPlanStatusRespVO saveSchedule(BackupPlanScheduleSaveReqVO reqVO) {
        BackupPlanSchedule schedule = normalizeSchedule(reqVO);
        assertBackupScriptsExist(schedule);
        writeSchedule(schedule);
        schedulerGateway.registerOrUpdate(schedule);
        return getStatus();
    }

    @Override
    public BackupPlanStatusRespVO enable() {
        BackupPlanSchedule schedule = readSchedule(true);
        assertBackupScriptsExist(schedule);
        schedulerGateway.registerOrUpdate(schedule);
        schedulerGateway.enable();
        return getStatus();
    }

    @Override
    public BackupPlanStatusRespVO disable() {
        schedulerGateway.disable();
        return getStatus();
    }

    @Override
    public RuntimeControlOperationRespVO backupNow(Long loginUserId, String backupKind) {
        String normalizedKind = StrUtil.trimToEmpty(backupKind).toUpperCase(Locale.ROOT);
        if (!BACKUP_KINDS.contains(normalizedKind)) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, "backupKind");
        }
        return operationGateway.backupNow(loginUserId, normalizedKind);
    }

    @Override
    public PageResult<RuntimeControlBackupPointRespVO> getHistoryPage(BackupPlanHistoryPageReqVO pageReqVO) {
        List<RuntimeControlBackupPointRespVO> backupPoints = backupDrillService.listBackupPoints();
        int fromIndex = Math.min((pageReqVO.getPageNo() - 1) * pageReqVO.getPageSize(), backupPoints.size());
        int toIndex = Math.min(fromIndex + pageReqVO.getPageSize(), backupPoints.size());
        return new PageResult<>(backupPoints.subList(fromIndex, toIndex), (long) backupPoints.size());
    }

    private BackupPlanStatusRespVO buildStatus(BackupPlanSchedule schedule, BackupPlanSchedulerStatus schedulerStatus,
                                               RuntimeControlBackupPointRespVO latestBackupPoint) {
        BackupPlanStatusRespVO respVO = new BackupPlanStatusRespVO();
        respVO.setFullSchedule(schedule.getFullSchedule());
        respVO.setIncrementalSchedule(schedule.getIncrementalSchedule());
        respVO.setRetentionSource(schedule.getRetentionSource());
        respVO.setQualityApprovalRef(schedule.getQualityApprovalRef());
        respVO.setRepositoryEnvironment(schedule.getRepositoryEnvironment());
        respVO.setMaxFreshnessHours(schedule.getMaxFreshnessHours());
        respVO.setNextRunTime(schedulerStatus.getNextRunTime());
        respVO.setLastRunTime(schedulerStatus.getLastRunTime());
        respVO.setLastResultCode(schedulerStatus.getLastResultCode());
        String blockedReason = firstBlockedReason(scheduleConfigBlockedReason(schedule), schedulerBlockedReason(schedule, schedulerStatus),
                backupFreshnessBlockedReason(schedule, latestBackupPoint));
        respVO.setBlockedReason(blockedReason);
        respVO.setLatestBackupPoint(latestBackupPoint);
        boolean enabled = Boolean.TRUE.equals(schedulerStatus.getEnabled());
        respVO.setPlanStatus(enabled ? "已开启" : "已关闭");
        if (schedulerStatus.getQueryExitCode() != null && schedulerStatus.getQueryExitCode() != 0) {
            respVO.setHealthStatus("配置异常");
        } else if (!enabled) {
            respVO.setHealthStatus("已关闭");
        } else if (schedulerStatus.getLastResultCode() != null && schedulerStatus.getLastResultCode() != 0) {
            respVO.setHealthStatus("上次失败");
            if (StrUtil.isBlank(respVO.getBlockedReason())) {
                respVO.setBlockedReason("计划任务上次运行失败：" + schedulerStatus.getLastResultCode());
            }
        } else if (StrUtil.isNotBlank(blockedReason)) {
            respVO.setHealthStatus("配置异常");
        } else {
            respVO.setHealthStatus("正常");
        }
        return respVO;
    }

    private String firstBlockedReason(String... reasons) {
        for (String reason : reasons) {
            if (StrUtil.isNotBlank(reason)) {
                return reason;
            }
        }
        return null;
    }

    private String scheduleConfigBlockedReason(BackupPlanSchedule schedule) {
        String repositoryEnvironment = schedule.getRepositoryEnvironment();
        if (!"test".equals(repositoryEnvironment)) {
            return "v2-minimal 的 backup.repositoryEnvironment 必须显式配置为 test";
        }
        if (schedule.getMaxFreshnessHours() == null || schedule.getMaxFreshnessHours() <= 0) {
            return "backup.maxFreshnessHours 必须配置为正整数";
        }
        if (StrUtil.isBlank(schedule.getRetentionSource())) {
            return "保存期限来源缺失：backup.retentionSource 必须填写质量批准的记录保存期限矩阵或等价来源";
        }
        if (StrUtil.isBlank(schedule.getQualityApprovalRef())) {
            return "质量批准引用缺失：backup.qualityApprovalRef 必须填写";
        }
        return null;
    }

    private String schedulerBlockedReason(BackupPlanSchedule schedule, BackupPlanSchedulerStatus schedulerStatus) {
        if (StrUtil.isNotBlank(schedulerStatus.getBlockedReason())) {
            return schedulerStatus.getBlockedReason();
        }
        String taskToRun = schedulerStatus.getTaskToRun();
        if (StrUtil.isNotBlank(taskToRun)) {
            String expectedScript = schedule.getBackupScriptPath().toString().toLowerCase(Locale.ROOT);
            String actualCommand = taskToRun.toLowerCase(Locale.ROOT);
            if (!actualCommand.contains(expectedScript)) {
                return "计划任务脚本路径异常";
            }
        }
        if (Boolean.TRUE.equals(schedulerStatus.getEnabled()) && schedulerStatus.getNextRunTime() == null) {
            return "下次运行时间缺失";
        }
        return null;
    }

    private String backupFreshnessBlockedReason(BackupPlanSchedule schedule,
                                                RuntimeControlBackupPointRespVO latestBackupPoint) {
        if (schedule.getMaxFreshnessHours() == null || schedule.getMaxFreshnessHours() <= 0) {
            return null;
        }
        if (latestBackupPoint == null) {
            return "最近成功备份点缺失";
        }
        if (latestBackupPoint.getCompletedAt() == null) {
            return "最近成功备份点 manifest completedAt 缺失或非法";
        }
        long ageHours = Duration.between(latestBackupPoint.getCompletedAt(), LocalDateTime.now()).toHours();
        if (ageHours > schedule.getMaxFreshnessHours()) {
            return "最近成功备份点 completedAt 超过 backup.maxFreshnessHours";
        }
        return null;
    }

    private BackupPlanSchedule normalizeSchedule(BackupPlanScheduleSaveReqVO reqVO) {
        String fullSchedule = normalizeFullSchedule(reqVO.getFullSchedule());
        String incrementalSchedule = normalizeTime(reqVO.getIncrementalSchedule(), "incrementalSchedule");
        BackupPlanSchedule schedule = readSchedule(true);
        schedule.setFullSchedule(fullSchedule);
        schedule.setIncrementalSchedule(incrementalSchedule);
        schedule.setRetentionSource(StrUtil.trim(reqVO.getRetentionSource()));
        schedule.setQualityApprovalRef(StrUtil.trim(reqVO.getQualityApprovalRef()));
        return schedule;
    }

    private String normalizeFullSchedule(String value) {
        String normalized = StrUtil.trimToEmpty(value).toUpperCase(Locale.ROOT);
        String[] parts = normalized.split("\\s+");
        if (parts.length != 2 || !WEEKDAYS.contains(parts[0])) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, "fullSchedule");
        }
        return parts[0] + " " + normalizeTime(parts[1], "fullSchedule");
    }

    private String normalizeTime(String value, String fieldName) {
        String normalized = StrUtil.trim(value);
        if (StrUtil.isBlank(normalized) || !normalized.matches("^(?:[01]\\d|2[0-3]):[0-5]\\d$")) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, fieldName);
        }
        return normalized;
    }

    private BackupPlanSchedule readSchedule(boolean strict) {
        Path configPath = resolveConfigPath();
        JsonNode root = readConfig(configPath);
        JsonNode backup = root.path("backup");
        BackupPlanSchedule schedule = baseSchedule();
        schedule.setFullSchedule(readRequiredText(backup, "fullSchedule", strict));
        schedule.setIncrementalSchedule(readRequiredText(backup, "incrementalSchedule", strict));
        schedule.setRepositoryEnvironment(readRepositoryEnvironment(backup, strict));
        schedule.setMaxFreshnessHours(readMaxFreshnessHours(backup, strict));
        schedule.setRetentionSource(readRequiredText(backup, "retentionSource", strict));
        schedule.setQualityApprovalRef(readRequiredText(backup, "qualityApprovalRef", strict));
        return schedule;
    }

    private String readRequiredText(JsonNode node, String fieldName, boolean strict) {
        String value = StrUtil.trim(node.path(fieldName).asText(null));
        if (StrUtil.isBlank(value) && strict) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, "backup." + fieldName);
        }
        return value;
    }

    private String readRepositoryEnvironment(JsonNode backup, boolean strict) {
        String repositoryEnvironment = StrUtil.trimToEmpty(backup.path("repositoryEnvironment").asText(null)).toLowerCase(Locale.ROOT);
        if (!"test".equals(repositoryEnvironment)) {
            if (strict) {
                throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, "backup.repositoryEnvironment");
            }
            return null;
        }
        return repositoryEnvironment;
    }

    private Integer readMaxFreshnessHours(JsonNode backup, boolean strict) {
        JsonNode node = backup.path("maxFreshnessHours");
        if (!node.isInt() || node.asInt() <= 0) {
            if (strict) {
                throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, "backup.maxFreshnessHours");
            }
            return null;
        }
        return node.asInt();
    }

    private BackupPlanSchedule baseSchedule() {
        Path repoRoot = resolveRepoRoot();
        BackupPlanSchedule schedule = new BackupPlanSchedule();
        schedule.setRepoRoot(repoRoot);
        schedule.setConfigPath(resolveConfigPath());
        schedule.setBackupScriptPath(repoRoot.resolve("script/backup-ops/scripts/backup-ops.ps1").normalize());
        schedule.setRegisterScriptPath(repoRoot.resolve("script/backup-ops/actions/Register-BackupOpsScheduledTasks.ps1").normalize());
        return schedule;
    }

    private void writeSchedule(BackupPlanSchedule schedule) {
        Path configPath = schedule.getConfigPath();
        JsonNode rootNode = readConfig(configPath);
        if (!(rootNode instanceof ObjectNode root)) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, "backup-ops.config.json root must be object");
        }
        JsonNode existingBackup = root.path("backup");
        ObjectNode backup = existingBackup instanceof ObjectNode existingObject
                ? existingObject
                : objectMapper.createObjectNode();
        backup.put("fullSchedule", schedule.getFullSchedule());
        backup.put("incrementalSchedule", schedule.getIncrementalSchedule());
        backup.put("retentionSource", schedule.getRetentionSource());
        backup.put("qualityApprovalRef", schedule.getQualityApprovalRef());
        root.set("backup", backup);
        try {
            Files.writeString(configPath, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root) + "\n",
                    StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw exception(RUNTIME_CONTROL_OPERATION_STORE_FAILED, ex.getMessage());
        }
    }

    private JsonNode readConfig(Path configPath) {
        if (!Files.isRegularFile(configPath)) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, "backupOps.configPath: " + configPath);
        }
        try {
            return objectMapper.readTree(Files.readString(configPath, StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw exception(RUNTIME_CONTROL_OPERATION_STORE_FAILED, ex.getMessage());
        }
    }

    private void assertBackupScriptsExist(BackupPlanSchedule schedule) {
        if (!Files.isRegularFile(schedule.getBackupScriptPath())) {
            throw exception(RUNTIME_CONTROL_SCRIPT_NOT_EXISTS, schedule.getBackupScriptPath().toString());
        }
        if (!Files.isRegularFile(schedule.getRegisterScriptPath())) {
            throw exception(RUNTIME_CONTROL_SCRIPT_NOT_EXISTS, schedule.getRegisterScriptPath().toString());
        }
    }

    private Path resolveRepoRoot() {
        try {
            return Path.of(properties.getRepoRoot()).toAbsolutePath().normalize();
        } catch (InvalidPathException ex) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, "repoRoot: " + ex.getInput());
        }
    }

    private Path resolveConfigPath() {
        Path configPath;
        try {
            configPath = Path.of(properties.getBackupOps().getConfigPath());
        } catch (InvalidPathException ex) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, "backupOps.configPath: " + ex.getInput());
        }
        return configPath.isAbsolute() ? configPath.normalize() : resolveRepoRoot().resolve(configPath).normalize();
    }
}
