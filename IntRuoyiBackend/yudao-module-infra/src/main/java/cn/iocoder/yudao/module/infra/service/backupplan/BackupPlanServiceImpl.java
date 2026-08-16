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

    private static final String DAILY = "DAILY";
    private static final String WEEKLY = "WEEKLY";
    private static final List<String> WEEKDAYS = List.of("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN");

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
    public RuntimeControlOperationRespVO backupNow(Long loginUserId) {
        return operationGateway.backupNow(loginUserId);
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
        respVO.setFrequency(schedule.getFrequency());
        respVO.setTime(schedule.getTime());
        respVO.setWeekday(schedule.getWeekday());
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
        if (!"test".equals(repositoryEnvironment) && !"backup".equals(repositoryEnvironment)) {
            return "backup.repositoryEnvironment 必须显式配置为 test 或 backup";
        }
        if (schedule.getMaxFreshnessHours() == null || schedule.getMaxFreshnessHours() <= 0) {
            return "backup.maxFreshnessHours 必须配置为正整数";
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
        String frequency = StrUtil.trimToEmpty(reqVO.getFrequency()).toUpperCase(Locale.ROOT);
        if (!DAILY.equals(frequency) && !WEEKLY.equals(frequency)) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, "frequency");
        }
        String time = StrUtil.trim(reqVO.getTime());
        if (StrUtil.isBlank(time) || !time.matches("^(?:[01]\\d|2[0-3]):[0-5]\\d$")) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_INVALID, "time");
        }
        String weekday = StrUtil.trimToEmpty(reqVO.getWeekday()).toUpperCase(Locale.ROOT);
        if (WEEKLY.equals(frequency) && !WEEKDAYS.contains(weekday)) {
            throw exception(RUNTIME_CONTROL_ACTION_PARAMETER_REQUIRED, "weekday");
        }
        BackupPlanSchedule schedule = readSchedule(true);
        schedule.setFrequency(frequency);
        schedule.setTime(time);
        schedule.setWeekday(WEEKLY.equals(frequency) ? weekday : null);
        return schedule;
    }

    private BackupPlanSchedule readSchedule(boolean strict) {
        Path configPath = resolveConfigPath();
        JsonNode root = readConfig(configPath);
        JsonNode backup = root.path("backup");
        String frequency = StrUtil.blankToDefault(backup.path("frequency").asText(null), DAILY);
        String scheduleTime = StrUtil.blankToDefault(backup.path("schedule").asText(null), "01:30");
        String weekday = StrUtil.blankToDefault(backup.path("weekday").asText(null), "MON");
        BackupPlanSchedule schedule = baseSchedule();
        schedule.setFrequency(frequency);
        schedule.setTime(scheduleTime);
        schedule.setWeekday(weekday);
        schedule.setRepositoryEnvironment(readRepositoryEnvironment(backup, strict));
        schedule.setMaxFreshnessHours(readMaxFreshnessHours(backup, strict));
        return schedule;
    }

    private String readRepositoryEnvironment(JsonNode backup, boolean strict) {
        String repositoryEnvironment = StrUtil.trimToEmpty(backup.path("repositoryEnvironment").asText(null)).toLowerCase(Locale.ROOT);
        if (!List.of("test", "backup").contains(repositoryEnvironment)) {
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
        backup.put("frequency", schedule.getFrequency());
        backup.put("schedule", schedule.getTime());
        if (WEEKLY.equals(schedule.getFrequency())) {
            backup.put("weekday", schedule.getWeekday());
        } else {
            backup.put("weekday", "MON");
        }
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
