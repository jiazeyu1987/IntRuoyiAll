package cn.iocoder.yudao.module.mes.service.pro.schedule;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProScheduleIssueDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProTaskDependencyDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.schedule.MesProTaskScheduleExtDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.scheduleorder.MesProScheduleOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.task.MesProTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProScheduleIssueMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProTaskDependencyMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.schedule.MesProTaskScheduleExtMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.scheduleorder.MesProScheduleOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.task.MesProTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.enums.pro.MesProTaskStatusEnum;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.EdhrScheduleCompletionCreateCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionService;
import cn.iocoder.yudao.module.mes.service.pro.schedule.SchedulePlanner.ScheduleIssueDraft;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


@Component
public class ScheduleApplier {

    private static final String SCHEDULE_SOURCE_MANUAL = "MANUAL";
    private static final String RISK_STATUS_NONE = "NONE";

    @Resource
    private MesProTaskMapper taskMapper;
    @Resource
    private MesProTaskScheduleExtMapper taskScheduleExtMapper;
    @Resource
    private MesProTaskDependencyMapper taskDependencyMapper;
    @Resource
    private MesProScheduleIssueMapper scheduleIssueMapper;
    @Resource
    private MesProScheduleOrderMapper scheduleOrderMapper;
    @Resource
    private MesProWorkOrderMapper workOrderMapper;
    @Resource
    private MesProEdhrBatchExecutionService edhrBatchExecutionService;

    public ApplyResult deleteReplaceableTasks(ApplyCommand command) {
        Objects.requireNonNull(command, "apply command must not be null");
        List<Long> existingScopeTaskIds = command.getExistingScopeTaskIds();
        List<Long> deleteTaskIds = command.getDeleteTaskIds();
        taskDependencyMapper.deleteByTaskIds(existingScopeTaskIds);
        if (!deleteTaskIds.isEmpty()) {
            scheduleIssueMapper.deleteByTaskIds(deleteTaskIds);
            taskScheduleExtMapper.deleteByTaskIds(deleteTaskIds);
            for (Long taskId : deleteTaskIds) {
                taskMapper.deleteById(taskId);
            }
        }
        scheduleIssueMapper.deleteByWorkOrderIds(command.getWorkOrderIds());
        return new ApplyResult(deleteTaskIds);
    }

    public int insertDependencies(List<MesProTaskDependencyDO> dependencies) {
        Objects.requireNonNull(dependencies, "dependencies must not be null");
        if (dependencies.isEmpty()) {
            return 0;
        }
        taskDependencyMapper.insertBatch(dependencies);
        return dependencies.size();
    }

    public int insertIssues(List<ScheduleIssueDraft> issues) {
        Objects.requireNonNull(issues, "issues must not be null");
        if (issues.isEmpty()) {
            return 0;
        }
        List<MesProScheduleIssueDO> issueRecords = issues.stream()
                .map(issue -> Objects.requireNonNull(issue, "issue must not be null").toDO(null))
                .toList();
        scheduleIssueMapper.insertBatch(issueRecords);
        return issueRecords.size();
    }

    public Long insertTaskWithScheduleExt(MesProTaskDO task, MesProTaskScheduleExtDO ext) {
        Objects.requireNonNull(task, "task must not be null");
        Objects.requireNonNull(ext, "task schedule ext must not be null");
        taskMapper.insert(task);
        if (task.getId() == null) {
            throw new IllegalStateException("task id must be generated after insert");
        }
        ext.setTaskId(task.getId());
        taskScheduleExtMapper.insert(ext);
        return task.getId();
    }

    public int syncPreservedTaskScheduleRelations(List<PreservedTaskScheduleRelation> relations) {
        Objects.requireNonNull(relations, "preserved task schedule relations must not be null");
        int writes = 0;
        for (PreservedTaskScheduleRelation relation : relations) {
            Objects.requireNonNull(relation, "preserved task schedule relation must not be null");
            if (MesProTaskStatusEnum.isEndStatus(relation.getTaskStatus())) {
                continue;
            }
            MesProTaskScheduleExtDO existingExt = relation.getExistingExt();
            if (existingExt == null) {
                taskScheduleExtMapper.insert(MesProTaskScheduleExtDO.builder()
                        .taskId(relation.getTaskId())
                        .scheduleOrderId(relation.getScheduleOrderId())
                        .scheduleOrderProcessId(relation.getScheduleOrderProcessId())
                        .scheduleSource(SCHEDULE_SOURCE_MANUAL)
                        .locked(Boolean.FALSE)
                        .riskStatus(RISK_STATUS_NONE)
                        .remark("REPLAN_RELINK")
                        .build());
                writes++;
                continue;
            }
            if (Objects.equals(existingExt.getScheduleOrderId(), relation.getScheduleOrderId())
                    && Objects.equals(existingExt.getScheduleOrderProcessId(), relation.getScheduleOrderProcessId())) {
                continue;
            }
            taskScheduleExtMapper.updateById(new MesProTaskScheduleExtDO()
                    .setId(existingExt.getId())
                    .setScheduleOrderId(relation.getScheduleOrderId())
                    .setScheduleOrderProcessId(relation.getScheduleOrderProcessId()));
            writes++;
        }
        return writes;
    }

    public int syncQuantityScheduled(Collection<Long> workOrderIds) {
        Objects.requireNonNull(workOrderIds, "work order ids must not be null");
        if (workOrderIds.isEmpty()) {
            return 0;
        }
        List<MesProTaskDO> finalTasks = taskMapper.selectListByWorkOrderIds(workOrderIds);
        Map<Long, BigDecimal> quantityScheduledByWorkOrder = new LinkedHashMap<>();
        for (MesProTaskDO task : finalTasks) {
            if (Objects.equals(task.getStatus(), MesProTaskStatusEnum.CANCELED.getStatus())) {
                continue;
            }
            if (task.getWorkOrderId() == null || task.getQuantity() == null) {
                continue;
            }
            quantityScheduledByWorkOrder.merge(task.getWorkOrderId(), task.getQuantity(), BigDecimal::add);
        }
        int updates = 0;
        for (Long workOrderId : workOrderIds) {
            workOrderMapper.updateQuantityScheduled(workOrderId,
                    quantityScheduledByWorkOrder.getOrDefault(workOrderId, BigDecimal.ZERO));
            updates++;
        }
        return updates;
    }

    public int syncScheduleOrderPlanFields(List<ScheduleOrderPlanFieldUpdate> updates) {
        Objects.requireNonNull(updates, "schedule order plan field updates must not be null");
        if (updates.isEmpty()) {
            return 0;
        }
        int writes = 0;
        for (ScheduleOrderPlanFieldUpdate update : updates) {
            Objects.requireNonNull(update, "schedule order plan field update must not be null");
            scheduleOrderMapper.updateById(update.toDO());
            writes++;
        }
        return writes;
    }

    public List<ScheduleIssueDraft> createEdhrBatchExecutionsAfterScheduleCompletion(
            List<EdhrScheduleCompletionCreateCommand> commands) {
        Objects.requireNonNull(commands, "edhr schedule completion commands must not be null");
        if (commands.isEmpty()) {
            return List.of();
        }
        List<ScheduleIssueDraft> issues = new ArrayList<>();
        for (EdhrScheduleCompletionCreateCommand command : commands) {
            Objects.requireNonNull(command, "edhr schedule completion command must not be null");
            List<String> missingItems = Objects.requireNonNull(
                    edhrBatchExecutionService.getScheduleCompletionMissingItems(command),
                    "edhr schedule completion missing items must not be null");
            if (!missingItems.isEmpty()) {
                issues.add(edhrBatchCreationWarning(command, missingItems));
                continue;
            }
            edhrBatchExecutionService.openOrCreateFromScheduleCompletion(command);
        }
        return issues;
    }

    private ScheduleIssueDraft edhrBatchCreationWarning(EdhrScheduleCompletionCreateCommand command,
                                                        List<String> missingItems) {
        return ScheduleIssueDraft.warning("EDHR_BATCH_CREATION", command.getWorkOrderId(), null, null, null,
                "排产完成创建 eDHR 批次缺少前置条件：" + String.join("、", missingItems));
    }

    public static final class ApplyCommand {

        private final List<Long> existingScopeTaskIds;
        private final List<Long> deleteTaskIds;
        private final List<Long> workOrderIds;

        private ApplyCommand(List<Long> existingScopeTaskIds, List<Long> deleteTaskIds, List<Long> workOrderIds) {
            this.existingScopeTaskIds = List.copyOf(Objects.requireNonNull(existingScopeTaskIds,
                    "existing scope task ids must not be null"));
            this.deleteTaskIds = List.copyOf(Objects.requireNonNull(deleteTaskIds,
                    "delete task ids must not be null"));
            this.workOrderIds = List.copyOf(Objects.requireNonNull(workOrderIds,
                    "work order ids must not be null"));
        }

        public static ApplyCommand forReplaceableTaskCleanup(List<Long> existingScopeTaskIds,
                                                             List<Long> deleteTaskIds,
                                                             List<Long> workOrderIds) {
            return new ApplyCommand(existingScopeTaskIds, deleteTaskIds, workOrderIds);
        }

        public List<Long> getExistingScopeTaskIds() {
            return existingScopeTaskIds;
        }

        public List<Long> getDeleteTaskIds() {
            return deleteTaskIds;
        }

        public List<Long> getWorkOrderIds() {
            return workOrderIds;
        }

    }

    public static final class ApplyResult {

        private final List<Long> deletedTaskIds;

        private ApplyResult(List<Long> deletedTaskIds) {
            this.deletedTaskIds = Collections.unmodifiableList(deletedTaskIds);
        }

        public List<Long> getDeletedTaskIds() {
            return deletedTaskIds;
        }

    }

    public static final class ScheduleOrderPlanFieldUpdate {

        private final Long scheduleOrderId;
        private final Integer status;
        private final LocalDateTime plannedStartTime;
        private final LocalDateTime plannedEndTime;
        private final LocalDateTime latestStartTime;
        private final Boolean delayRiskFlag;
        private final Boolean startRiskFlag;

        private ScheduleOrderPlanFieldUpdate(Long scheduleOrderId, Integer status,
                                             LocalDateTime plannedStartTime,
                                             LocalDateTime plannedEndTime,
                                             LocalDateTime latestStartTime,
                                             Boolean delayRiskFlag,
                                             Boolean startRiskFlag) {
            this.scheduleOrderId = Objects.requireNonNull(scheduleOrderId, "schedule order id must not be null");
            this.status = status;
            this.plannedStartTime = plannedStartTime;
            this.plannedEndTime = plannedEndTime;
            this.latestStartTime = latestStartTime;
            this.delayRiskFlag = delayRiskFlag;
            this.startRiskFlag = startRiskFlag;
        }

        public static ScheduleOrderPlanFieldUpdate of(Long scheduleOrderId, Integer status,
                                                      LocalDateTime plannedStartTime,
                                                      LocalDateTime plannedEndTime,
                                                      LocalDateTime latestStartTime,
                                                      Boolean delayRiskFlag,
                                                      Boolean startRiskFlag) {
            return new ScheduleOrderPlanFieldUpdate(scheduleOrderId, status, plannedStartTime, plannedEndTime,
                    latestStartTime, delayRiskFlag, startRiskFlag);
        }

        private MesProScheduleOrderDO toDO() {
            MesProScheduleOrderDO updateObj = new MesProScheduleOrderDO();
            updateObj.setId(scheduleOrderId);
            updateObj.setStatus(status);
            updateObj.setPlannedStartTime(plannedStartTime);
            updateObj.setPlannedEndTime(plannedEndTime);
            updateObj.setLatestStartTime(latestStartTime);
            updateObj.setDelayRiskFlag(delayRiskFlag);
            updateObj.setStartRiskFlag(startRiskFlag);
            return updateObj;
        }

    }

    public static final class PreservedTaskScheduleRelation {

        private final Long taskId;
        private final Integer taskStatus;
        private final Long scheduleOrderId;
        private final Long scheduleOrderProcessId;
        private final MesProTaskScheduleExtDO existingExt;

        private PreservedTaskScheduleRelation(Long taskId, Integer taskStatus, Long scheduleOrderId,
                                              Long scheduleOrderProcessId,
                                              MesProTaskScheduleExtDO existingExt) {
            this.taskId = Objects.requireNonNull(taskId, "task id must not be null");
            this.taskStatus = taskStatus;
            this.scheduleOrderId = Objects.requireNonNull(scheduleOrderId, "schedule order id must not be null");
            this.scheduleOrderProcessId = Objects.requireNonNull(scheduleOrderProcessId,
                    "schedule order process id must not be null");
            this.existingExt = existingExt;
        }

        public static PreservedTaskScheduleRelation of(Long taskId, Integer taskStatus, Long scheduleOrderId,
                                                       Long scheduleOrderProcessId,
                                                       MesProTaskScheduleExtDO existingExt) {
            return new PreservedTaskScheduleRelation(taskId, taskStatus, scheduleOrderId, scheduleOrderProcessId,
                    existingExt);
        }

        public Long getTaskId() {
            return taskId;
        }

        public Integer getTaskStatus() {
            return taskStatus;
        }

        public Long getScheduleOrderId() {
            return scheduleOrderId;
        }

        public Long getScheduleOrderProcessId() {
            return scheduleOrderProcessId;
        }

        public MesProTaskScheduleExtDO getExistingExt() {
            return existingExt;
        }

    }

}
