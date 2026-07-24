package cn.iocoder.yudao.module.mes.service.pro.schedule;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
import cn.iocoder.yudao.module.mes.enums.pro.MesProScheduleOrderStatusEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProTaskStatusEnum;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.EdhrScheduleCompletionCreateCommand;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionService;
import cn.iocoder.yudao.module.mes.service.pro.schedule.SchedulePlanner.ScheduleIssueDraft;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleApplierTest {

    @InjectMocks
    private ScheduleApplier scheduleApplier;

    @Mock
    private MesProTaskMapper taskMapper;
    @Mock
    private MesProTaskScheduleExtMapper taskScheduleExtMapper;
    @Mock
    private MesProTaskDependencyMapper taskDependencyMapper;
    @Mock
    private MesProScheduleIssueMapper scheduleIssueMapper;
    @Mock
    private MesProScheduleOrderMapper scheduleOrderMapper;
    @Mock
    private MesProWorkOrderMapper workOrderMapper;
    @Mock
    private MesProEdhrBatchExecutionService edhrBatchExecutionService;

    @Test
    void applier_shouldExposePersistenceBoundaryWithoutPlanningDependencies() {
        assertNotNull(ScheduleApplier.ApplyCommand.class);
        assertNotNull(ScheduleApplier.ApplyResult.class);

        Set<String> dependencyTypes = Arrays.stream(ScheduleApplier.class.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Resource.class))
                .map(Field::getType)
                .map(Class::getSimpleName)
                .collect(Collectors.toSet());

        assertTrue(dependencyTypes.contains("MesProTaskMapper"));
        assertTrue(dependencyTypes.contains("MesProTaskScheduleExtMapper"));
        assertTrue(dependencyTypes.contains("MesProTaskDependencyMapper"));
        assertTrue(dependencyTypes.contains("MesProScheduleIssueMapper"));
        assertTrue(dependencyTypes.contains("MesProScheduleOrderMapper"));
        assertTrue(dependencyTypes.contains("MesProWorkOrderMapper"));
        assertFalse(dependencyTypes.contains("SchedulePlanner"));
        assertFalse(dependencyTypes.contains("ScheduleInputAssembler"));
        assertFalse(dependencyTypes.contains("RouteSnapshotResolver"));
    }

    @Test
    void deleteReplaceableTasks_shouldClearRelatedPersistenceScope() {
        ScheduleApplier.ApplyCommand command = ScheduleApplier.ApplyCommand.forReplaceableTaskCleanup(
                List.of(10L, 11L), List.of(11L), List.of(100L));

        ScheduleApplier.ApplyResult result = scheduleApplier.deleteReplaceableTasks(command);

        assertEquals(List.of(11L), result.getDeletedTaskIds());
        InOrder inOrder = inOrder(taskDependencyMapper, scheduleIssueMapper, taskScheduleExtMapper, taskMapper);
        inOrder.verify(taskDependencyMapper).deleteByTaskIds(List.of(10L, 11L));
        inOrder.verify(scheduleIssueMapper).deleteByTaskIds(List.of(11L));
        inOrder.verify(taskScheduleExtMapper).deleteByTaskIds(List.of(11L));
        inOrder.verify(taskMapper).deleteById(11L);
        verify(scheduleIssueMapper).deleteByWorkOrderIds(List.of(100L));
    }

    @Test
    void deleteReplaceableTasks_shouldKeepScopeDependencyCleanupWhenNoTasksDeleted() {
        ScheduleApplier.ApplyCommand command = ScheduleApplier.ApplyCommand.forReplaceableTaskCleanup(
                List.of(10L), List.of(), List.of(100L));

        ScheduleApplier.ApplyResult result = scheduleApplier.deleteReplaceableTasks(command);

        assertEquals(List.of(), result.getDeletedTaskIds());
        verify(taskDependencyMapper).deleteByTaskIds(List.of(10L));
        verify(scheduleIssueMapper, never()).deleteByTaskIds(List.of());
        verify(taskScheduleExtMapper, never()).deleteByTaskIds(List.of());
        verify(taskMapper, never()).deleteById(10L);
        verify(scheduleIssueMapper).deleteByWorkOrderIds(List.of(100L));
    }

    @Test
    void insertDependencies_shouldPersistNonEmptyDependencyPlansOnly() {
        MesProTaskDependencyDO dependency = MesProTaskDependencyDO.builder()
                .sourceTaskId(10L)
                .targetTaskId(11L)
                .sourceProcessId(300L)
                .targetProcessId(301L)
                .dependencyType("0")
                .enabled(Boolean.TRUE)
                .build();

        int inserted = scheduleApplier.insertDependencies(List.of(dependency));

        assertEquals(1, inserted);
        verify(taskDependencyMapper).insertBatch(List.of(dependency));
    }

    @Test
    void insertDependencies_shouldNotPersistEmptyDependencyPlans() {
        int inserted = scheduleApplier.insertDependencies(List.of());

        assertEquals(0, inserted);
        verify(taskDependencyMapper, never()).insertBatch(List.of());
    }

    @Test
    void insertIssues_shouldConvertDraftsAndPersistNonEmptyIssuesOnly() {
        ScheduleIssueDraft issue = ScheduleIssueDraft.warning("CAPACITY", 100L, 300L,
                30L, null, "capacity warning");

        int inserted = scheduleApplier.insertIssues(List.of(issue));

        assertEquals(1, inserted);
        ArgumentCaptor<List<MesProScheduleIssueDO>> issueCaptor = ArgumentCaptor.captor();
        verify(scheduleIssueMapper).insertBatch(issueCaptor.capture());
        assertEquals("CAPACITY", issueCaptor.getValue().get(0).getIssueType());
        assertEquals("WARNING", issueCaptor.getValue().get(0).getSeverity());
        assertEquals(100L, issueCaptor.getValue().get(0).getWorkOrderId());
        assertEquals(300L, issueCaptor.getValue().get(0).getProcessId());
        assertEquals("capacity warning", issueCaptor.getValue().get(0).getMessage());
    }

    @Test
    void insertIssues_shouldNotPersistEmptyIssueDrafts() {
        int inserted = scheduleApplier.insertIssues(List.of());

        assertEquals(0, inserted);
        verify(scheduleIssueMapper, never()).insertBatch(List.of());
    }

    @Test
    void insertTaskWithScheduleExt_shouldBindGeneratedTaskIdBeforePersistingExt() {
        MesProTaskDO task = MesProTaskDO.builder()
                .code("TASK-001")
                .name("auto task")
                .workOrderId(100L)
                .processId(300L)
                .build();
        MesProTaskScheduleExtDO ext = MesProTaskScheduleExtDO.builder()
                .scheduleOrderId(200L)
                .scheduleOrderProcessId(201L)
                .scheduleSource("AUTO")
                .build();
        doAnswer(invocation -> {
            task.setId(10L);
            return 1;
        }).when(taskMapper).insert(task);

        Long taskId = scheduleApplier.insertTaskWithScheduleExt(task, ext);

        assertEquals(10L, taskId);
        assertEquals(10L, ext.getTaskId());
        InOrder inOrder = inOrder(taskMapper, taskScheduleExtMapper);
        inOrder.verify(taskMapper).insert(task);
        inOrder.verify(taskScheduleExtMapper).insert(ext);
    }

    @Test
    void insertTaskWithScheduleExt_shouldFailFastWhenTaskIdIsNotGenerated() {
        MesProTaskDO task = MesProTaskDO.builder()
                .code("TASK-002")
                .name("auto task")
                .build();
        MesProTaskScheduleExtDO ext = MesProTaskScheduleExtDO.builder()
                .scheduleSource("AUTO")
                .build();

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> scheduleApplier.insertTaskWithScheduleExt(task, ext));

        assertEquals("task id must be generated after insert", error.getMessage());
        verify(taskMapper).insert(task);
        verify(taskScheduleExtMapper, never()).insert(ext);
    }

    @Test
    void syncPreservedTaskScheduleRelations_shouldInsertMissingExtForActiveTask() {
        ScheduleApplier.PreservedTaskScheduleRelation command =
                ScheduleApplier.PreservedTaskScheduleRelation.of(10L,
                        MesProTaskStatusEnum.PREPARE.getStatus(), 200L, 201L, null);

        int writes = scheduleApplier.syncPreservedTaskScheduleRelations(List.of(command));

        assertEquals(1, writes);
        ArgumentCaptor<MesProTaskScheduleExtDO> extCaptor = ArgumentCaptor.forClass(MesProTaskScheduleExtDO.class);
        verify(taskScheduleExtMapper).insert(extCaptor.capture());
        MesProTaskScheduleExtDO ext = extCaptor.getValue();
        assertEquals(10L, ext.getTaskId());
        assertEquals(200L, ext.getScheduleOrderId());
        assertEquals(201L, ext.getScheduleOrderProcessId());
        assertEquals("MANUAL", ext.getScheduleSource());
        assertFalse(ext.getLocked());
        assertEquals("NONE", ext.getRiskStatus());
        assertEquals("REPLAN_RELINK", ext.getRemark());
    }

    @Test
    void syncPreservedTaskScheduleRelations_shouldUpdateChangedExtRelationOnly() {
        MesProTaskScheduleExtDO existingExt = MesProTaskScheduleExtDO.builder()
                .id(30L)
                .taskId(10L)
                .scheduleOrderId(199L)
                .scheduleOrderProcessId(198L)
                .scheduleSource("MANUAL")
                .riskStatus("LOCKED")
                .build();
        ScheduleApplier.PreservedTaskScheduleRelation command =
                ScheduleApplier.PreservedTaskScheduleRelation.of(10L,
                        MesProTaskStatusEnum.PREPARE.getStatus(), 200L, 201L, existingExt);

        int writes = scheduleApplier.syncPreservedTaskScheduleRelations(List.of(command));

        assertEquals(1, writes);
        ArgumentCaptor<MesProTaskScheduleExtDO> extCaptor = ArgumentCaptor.forClass(MesProTaskScheduleExtDO.class);
        verify(taskScheduleExtMapper).updateById(extCaptor.capture());
        MesProTaskScheduleExtDO update = extCaptor.getValue();
        assertEquals(30L, update.getId());
        assertEquals(200L, update.getScheduleOrderId());
        assertEquals(201L, update.getScheduleOrderProcessId());
        assertEquals(null, update.getScheduleSource());
        assertEquals(null, update.getRiskStatus());
    }

    @Test
    void syncPreservedTaskScheduleRelations_shouldSkipMatchedAndEndedTasks() {
        MesProTaskScheduleExtDO matchedExt = MesProTaskScheduleExtDO.builder()
                .id(30L)
                .taskId(10L)
                .scheduleOrderId(200L)
                .scheduleOrderProcessId(201L)
                .build();
        ScheduleApplier.PreservedTaskScheduleRelation matched =
                ScheduleApplier.PreservedTaskScheduleRelation.of(10L,
                        MesProTaskStatusEnum.PREPARE.getStatus(), 200L, 201L, matchedExt);
        ScheduleApplier.PreservedTaskScheduleRelation ended =
                ScheduleApplier.PreservedTaskScheduleRelation.of(11L,
                        MesProTaskStatusEnum.FINISHED.getStatus(), 200L, 202L, null);

        int writes = scheduleApplier.syncPreservedTaskScheduleRelations(List.of(matched, ended));

        assertEquals(0, writes);
        verify(taskScheduleExtMapper, never()).insert(matchedExt);
        verify(taskScheduleExtMapper, never()).updateById(matchedExt);
    }

    @Test
    void syncQuantityScheduled_shouldAggregateNonCanceledTaskQuantitiesAndResetEmptyWorkOrders() {
        MesProTaskDO first = MesProTaskDO.builder()
                .workOrderId(100L)
                .quantity(new BigDecimal("2.0000"))
                .status(MesProTaskStatusEnum.PREPARE.getStatus())
                .build();
        MesProTaskDO second = MesProTaskDO.builder()
                .workOrderId(100L)
                .quantity(new BigDecimal("3.0000"))
                .status(MesProTaskStatusEnum.IN_PROGRESS.getStatus())
                .build();
        MesProTaskDO canceled = MesProTaskDO.builder()
                .workOrderId(100L)
                .quantity(new BigDecimal("9.0000"))
                .status(MesProTaskStatusEnum.CANCELED.getStatus())
                .build();
        MesProTaskDO missingQuantity = MesProTaskDO.builder()
                .workOrderId(101L)
                .status(MesProTaskStatusEnum.PREPARE.getStatus())
                .build();
        when(taskMapper.selectListByWorkOrderIds(List.of(100L, 101L)))
                .thenReturn(List.of(first, second, canceled, missingQuantity));

        int updates = scheduleApplier.syncQuantityScheduled(List.of(100L, 101L));

        assertEquals(2, updates);
        verify(workOrderMapper).updateQuantityScheduled(100L, new BigDecimal("5.0000"));
        verify(workOrderMapper).updateQuantityScheduled(101L, BigDecimal.ZERO);
    }

    @Test
    void syncQuantityScheduled_shouldSkipEmptyWorkOrderScope() {
        int updates = scheduleApplier.syncQuantityScheduled(List.of());

        assertEquals(0, updates);
        verify(taskMapper, never()).selectListByWorkOrderIds(List.of());
        verify(workOrderMapper, never()).updateQuantityScheduled(100L, BigDecimal.ZERO);
    }

    @Test
    void syncScheduleOrderPlanFields_shouldPersistPlanFieldUpdatesOnly() {
        LocalDateTime plannedStart = LocalDateTime.of(2026, 5, 14, 8, 0);
        LocalDateTime plannedEnd = LocalDateTime.of(2026, 5, 14, 16, 0);
        LocalDateTime latestStart = LocalDateTime.of(2026, 5, 13, 8, 0);
        ScheduleApplier.ScheduleOrderPlanFieldUpdate update =
                ScheduleApplier.ScheduleOrderPlanFieldUpdate.of(501L,
                        MesProScheduleOrderStatusEnum.SCHEDULED.getStatus(),
                        plannedStart, plannedEnd, latestStart, Boolean.TRUE, Boolean.TRUE);

        int writes = scheduleApplier.syncScheduleOrderPlanFields(List.of(update));

        assertEquals(1, writes);
        ArgumentCaptor<MesProScheduleOrderDO> updateCaptor = ArgumentCaptor.forClass(MesProScheduleOrderDO.class);
        verify(scheduleOrderMapper).updateById(updateCaptor.capture());
        MesProScheduleOrderDO updateObj = updateCaptor.getValue();
        assertEquals(501L, updateObj.getId());
        assertEquals(MesProScheduleOrderStatusEnum.SCHEDULED.getStatus(), updateObj.getStatus());
        assertEquals(plannedStart, updateObj.getPlannedStartTime());
        assertEquals(plannedEnd, updateObj.getPlannedEndTime());
        assertEquals(latestStart, updateObj.getLatestStartTime());
        assertEquals(Boolean.TRUE, updateObj.getDelayRiskFlag());
        assertEquals(Boolean.TRUE, updateObj.getStartRiskFlag());
    }

    @Test
    void syncScheduleOrderPlanFields_shouldSkipEmptyUpdates() {
        int writes = scheduleApplier.syncScheduleOrderPlanFields(List.of());

        assertEquals(0, writes);
        verify(scheduleOrderMapper, never()).updateById(new MesProScheduleOrderDO());
    }

    @Test
    void createEdhrBatchExecutionsAfterScheduleCompletion_shouldReturnWarningAndSkipCreationWhenPrerequisiteMissing() {
        EdhrScheduleCompletionCreateCommand command = buildEdhrCompletionCommand();
        when(edhrBatchExecutionService.getScheduleCompletionMissingItems(command))
                .thenReturn(List.of("批次号", "批记录模板"));

        List<ScheduleIssueDraft> issues =
                scheduleApplier.createEdhrBatchExecutionsAfterScheduleCompletion(List.of(command));

        assertEquals(1, issues.size());
        MesProScheduleIssueDO issue = issues.get(0).toDO(null);
        assertEquals("EDHR_BATCH_CREATION", issue.getIssueType());
        assertEquals("WARNING", issue.getSeverity());
        assertEquals(100L, issue.getWorkOrderId());
        assertTrue(issue.getMessage().contains("批次号"));
        assertTrue(issue.getMessage().contains("批记录模板"));
        verify(edhrBatchExecutionService, never()).openOrCreateFromScheduleCompletion(command);
    }

    @Test
    void createEdhrBatchExecutionsAfterScheduleCompletion_shouldOpenOrCreateWhenPrerequisitesReady() {
        EdhrScheduleCompletionCreateCommand command = buildEdhrCompletionCommand();
        when(edhrBatchExecutionService.getScheduleCompletionMissingItems(command)).thenReturn(List.of());

        List<ScheduleIssueDraft> issues =
                scheduleApplier.createEdhrBatchExecutionsAfterScheduleCompletion(List.of(command));

        assertEquals(0, issues.size());
        verify(edhrBatchExecutionService).openOrCreateFromScheduleCompletion(command);
    }

    private EdhrScheduleCompletionCreateCommand buildEdhrCompletionCommand() {
        return new EdhrScheduleCompletionCreateCommand()
                .setScheduleOrderId(501L)
                .setScheduleOrderCode("SO-501")
                .setWorkOrderId(100L)
                .setBatchCode("BATCH-100")
                .setProductId(200L)
                .setRouteId(300L)
                .setRemark("排产完成自动创建");
    }

}
