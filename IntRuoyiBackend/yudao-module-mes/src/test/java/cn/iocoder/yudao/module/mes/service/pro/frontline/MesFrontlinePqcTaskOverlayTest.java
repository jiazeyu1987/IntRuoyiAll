package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MesFrontlinePqcTaskOverlayTest {

    private static final long ACTIVE_ORDER_ID = 5001L;
    private static final long REGULATION_VERSION_ID = 8001L;
    private static final long QA_PROCESS_ID = 9001L;
    private static final LocalDate BUSINESS_DATE = LocalDate.of(2026, 8, 12);

    @Test
    void shouldMatchPendingTaskByActiveOrderRegulationQaProcessAndRuleKey() {
        List<MesPqcInspectionTaskDO> tasks = List.of(
                task(1001L, ACTIVE_ORDER_ID + 1, REGULATION_VERSION_ID, QA_PROCESS_ID, "FIRST", "FIRST"),
                task(1002L, ACTIVE_ORDER_ID, REGULATION_VERSION_ID + 1, QA_PROCESS_ID, "FIRST", "FIRST"),
                task(1003L, ACTIVE_ORDER_ID, REGULATION_VERSION_ID, QA_PROCESS_ID + 1, "FIRST", "FIRST"),
                task(1004L, ACTIVE_ORDER_ID, REGULATION_VERSION_ID, QA_PROCESS_ID, "PATROL", "PATROL_AM"),
                task(1005L, ACTIVE_ORDER_ID, REGULATION_VERSION_ID, QA_PROCESS_ID, "FIRST", "FIRST"));

        MesFrontlinePqcTaskOverlay overlay = MesFrontlinePqcTaskOverlay.fromExpectedTask(
                expected(QA_PROCESS_ID, "FIRST", "FIRST"), tasks);

        assertEquals(MesFrontlinePqcTaskOverlay.STATUS_PENDING, overlay.status());
        assertEquals(1005L, overlay.pqcTaskOption().pqcTaskId());
        assertEquals(REGULATION_VERSION_ID, overlay.pqcTaskOption().regulationVersionId());
        assertEquals(QA_PROCESS_ID, overlay.pqcTaskOption().qaProcessId());
    }

    @Test
    void shouldReturnNotCreatedWhenNoPendingTaskMatchesOverlayIdentity() {
        MesFrontlinePqcTaskOverlay overlay = MesFrontlinePqcTaskOverlay.fromExpectedTask(
                expected(QA_PROCESS_ID, "FINAL", "FINAL"),
                List.of(task(1001L, ACTIVE_ORDER_ID, REGULATION_VERSION_ID, QA_PROCESS_ID, "FIRST", "FIRST")));

        assertEquals(MesFrontlinePqcTaskOverlay.STATUS_NOT_CREATED, overlay.status());
        assertNull(overlay.pqcTaskOption());
    }

    @Test
    void shouldKeepFirstPatrolAmPatrolPmAndFinalTaskOptionsSeparate() {
        List<MesFrontlinePqcTaskOverlay.ExpectedTaskIdentity> expectedTasks = List.of(
                expected(QA_PROCESS_ID, "FIRST", "FIRST"),
                expected(QA_PROCESS_ID, "PATROL_AM", "PATROL"),
                expected(QA_PROCESS_ID, "PATROL_PM", "PATROL"),
                expected(QA_PROCESS_ID, "FINAL", "FINAL"));
        List<MesPqcInspectionTaskDO> tasks = List.of(
                task(1001L, ACTIVE_ORDER_ID, REGULATION_VERSION_ID, QA_PROCESS_ID, "FIRST", "FIRST"),
                task(1002L, ACTIVE_ORDER_ID, REGULATION_VERSION_ID, QA_PROCESS_ID, "PATROL", "PATROL_AM"),
                task(1003L, ACTIVE_ORDER_ID, REGULATION_VERSION_ID, QA_PROCESS_ID, "PATROL", "PATROL_PM"),
                task(1004L, ACTIVE_ORDER_ID, REGULATION_VERSION_ID, QA_PROCESS_ID, "FINAL", "FINAL"));

        List<MesFrontlinePqcTaskOverlay> overlays = MesFrontlinePqcTaskOverlay.fromExpectedTasks(expectedTasks, tasks);

        assertEquals(List.of("FIRST", "PATROL_AM", "PATROL_PM", "FINAL"),
                overlays.stream().map(MesFrontlinePqcTaskOverlay::inspectionRuleKey).toList());
        assertEquals(List.of(1001L, 1002L, 1003L, 1004L),
                overlays.stream().map(overlay -> overlay.pqcTaskOption().pqcTaskId()).toList());
    }

    @Test
    void shouldSortTaskOptionsByBusinessDateRuleRoundAndTaskId() {
        LocalDate previousDay = BUSINESS_DATE.minusDays(1);
        List<MesFrontlinePqcTaskOverlay.ExpectedTaskIdentity> expectedTasks = List.of(
                expected(QA_PROCESS_ID, "FINAL", "FINAL", BUSINESS_DATE, 1),
                expected(QA_PROCESS_ID, "PATROL_PM", "PATROL", BUSINESS_DATE, 1),
                expected(QA_PROCESS_ID, "FIRST", "FIRST", previousDay, 1),
                expected(QA_PROCESS_ID, "PATROL_AM", "PATROL", BUSINESS_DATE, 1));
        List<MesPqcInspectionTaskDO> tasks = List.of(
                task(1004L, ACTIVE_ORDER_ID, REGULATION_VERSION_ID, QA_PROCESS_ID, "FINAL", "FINAL",
                        BUSINESS_DATE, 1),
                task(1003L, ACTIVE_ORDER_ID, REGULATION_VERSION_ID, QA_PROCESS_ID, "PATROL", "PATROL_PM",
                        BUSINESS_DATE, 1),
                task(1001L, ACTIVE_ORDER_ID, REGULATION_VERSION_ID, QA_PROCESS_ID, "FIRST", "FIRST",
                        previousDay, 1),
                task(1002L, ACTIVE_ORDER_ID, REGULATION_VERSION_ID, QA_PROCESS_ID, "PATROL", "PATROL_AM",
                        BUSINESS_DATE, 1));

        List<MesFrontlinePqcTaskOverlay> overlays = MesFrontlinePqcTaskOverlay.fromExpectedTasks(expectedTasks, tasks);

        assertEquals(List.of(1001L, 1002L, 1003L, 1004L),
                overlays.stream().map(overlay -> overlay.pqcTaskOption().pqcTaskId()).toList());
    }

    private static MesFrontlinePqcTaskOverlay.ExpectedTaskIdentity expected(long qaProcessId,
                                                                            String inspectionRuleKey,
                                                                            String inspectionType) {
        return expected(qaProcessId, inspectionRuleKey, inspectionType, BUSINESS_DATE, 1);
    }

    private static MesFrontlinePqcTaskOverlay.ExpectedTaskIdentity expected(long qaProcessId,
                                                                            String inspectionRuleKey,
                                                                            String inspectionType,
                                                                            LocalDate businessDate,
                                                                            int roundNo) {
        return new MesFrontlinePqcTaskOverlay.ExpectedTaskIdentity(ACTIVE_ORDER_ID, REGULATION_VERSION_ID,
                qaProcessId, inspectionRuleKey, inspectionType, businessDate, "DAY", roundNo,
                true, 5, List.of());
    }

    private static MesPqcInspectionTaskDO task(long id, long activeOrderId, long regulationVersionId,
                                               long qaProcessId, String inspectionType,
                                               String inspectionRuleKey) {
        return task(id, activeOrderId, regulationVersionId, qaProcessId, inspectionType, inspectionRuleKey,
                BUSINESS_DATE, 1);
    }

    private static MesPqcInspectionTaskDO task(long id, long activeOrderId, long regulationVersionId,
                                               long qaProcessId, String inspectionType,
                                               String inspectionRuleKey,
                                               LocalDate businessDate,
                                               int roundNo) {
        return MesPqcInspectionTaskDO.builder()
                .id(id)
                .activeOrderId(activeOrderId)
                .regulationVersionId(regulationVersionId)
                .qaProcessId(qaProcessId)
                .inspectionType(inspectionType)
                .inspectionRuleKey(inspectionRuleKey)
                .businessDate(businessDate)
                .shiftCode("DAY")
                .roundNo(roundNo)
                .plannedInspectionQuantity(5)
                .taskStatus(MesPqcInspectionTaskDO.TASK_STATUS_PENDING)
                .build();
    }
}
