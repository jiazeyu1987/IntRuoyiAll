package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MesFrontlinePqcTaskOverlayTest {

    private static final long ACTIVE_ORDER_ID = 5001L;
    private static final long REGULATION_VERSION_ID = 8001L;
    private static final long QA_PROCESS_ID = 9001L;
    private static final long ROUTE_PROCESS_ID = 7001L;
    private static final long PROCESS_ID = 6001L;
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

    @Test
    void shouldKeepSameQaIdentityAcrossProductionProcessesSeparate() {
        MesFrontlinePqcTaskOverlay.ExpectedTaskIdentity first = expected(
                ROUTE_PROCESS_ID, PROCESS_ID, QA_PROCESS_ID, "PATROL_AM", "PATROL", BUSINESS_DATE, "DAY", 1);
        MesFrontlinePqcTaskOverlay.ExpectedTaskIdentity second = expected(
                ROUTE_PROCESS_ID + 1, PROCESS_ID + 1, QA_PROCESS_ID, "PATROL_AM", "PATROL", BUSINESS_DATE, "DAY", 1);

        List<MesFrontlinePqcTaskOverlay> overlays = MesFrontlinePqcTaskOverlay.fromExpectedTasks(
                List.of(first, second), List.of(
                        task(1101L, ACTIVE_ORDER_ID, ROUTE_PROCESS_ID, PROCESS_ID, REGULATION_VERSION_ID,
                                QA_PROCESS_ID, "PATROL", "PATROL_AM", BUSINESS_DATE, "DAY", 1),
                        task(1102L, ACTIVE_ORDER_ID, ROUTE_PROCESS_ID + 1, PROCESS_ID + 1,
                                REGULATION_VERSION_ID, QA_PROCESS_ID, "PATROL", "PATROL_AM", BUSINESS_DATE, "DAY", 1)));

        assertEquals(List.of(1101L, 1102L), overlays.stream()
                .map(overlay -> overlay.pqcTaskOption().pqcTaskId()).toList());
    }

    @Test
    void shouldKeepSameQaIdentityAcrossBusinessRoundsSeparate() {
        MesFrontlinePqcTaskOverlay.ExpectedTaskIdentity first = expected(
                ROUTE_PROCESS_ID, PROCESS_ID, QA_PROCESS_ID, "PATROL_AM", "PATROL", BUSINESS_DATE, "DAY", 1);
        MesFrontlinePqcTaskOverlay.ExpectedTaskIdentity second = expected(
                ROUTE_PROCESS_ID, PROCESS_ID, QA_PROCESS_ID, "PATROL_AM", "PATROL", BUSINESS_DATE.plusDays(1), "NIGHT", 2);

        List<MesFrontlinePqcTaskOverlay> overlays = MesFrontlinePqcTaskOverlay.fromExpectedTasks(
                List.of(first, second), List.of(
                        task(1201L, ACTIVE_ORDER_ID, ROUTE_PROCESS_ID, PROCESS_ID, REGULATION_VERSION_ID,
                                QA_PROCESS_ID, "PATROL", "PATROL_AM", BUSINESS_DATE, "DAY", 1),
                        task(1202L, ACTIVE_ORDER_ID, ROUTE_PROCESS_ID, PROCESS_ID, REGULATION_VERSION_ID,
                                QA_PROCESS_ID, "PATROL", "PATROL_AM", BUSINESS_DATE.plusDays(1), "NIGHT", 2)));

        assertEquals(List.of(1201L, 1202L), overlays.stream()
                .map(overlay -> overlay.pqcTaskOption().pqcTaskId()).toList());
    }

    @Test
    void shouldRejectDuplicateTasksWithCompleteOverlayIdentity() {
        MesFrontlinePqcTaskOverlay.ExpectedTaskIdentity expected = expected(
                ROUTE_PROCESS_ID, PROCESS_ID, QA_PROCESS_ID, "PATROL_AM", "PATROL", BUSINESS_DATE, "DAY", 1);
        assertThrows(IllegalStateException.class, () -> MesFrontlinePqcTaskOverlay.fromExpectedTask(expected,
                List.of(
                        task(1301L, ACTIVE_ORDER_ID, ROUTE_PROCESS_ID, PROCESS_ID, REGULATION_VERSION_ID,
                                QA_PROCESS_ID, "PATROL", "PATROL_AM", BUSINESS_DATE, "DAY", 1),
                        task(1302L, ACTIVE_ORDER_ID, ROUTE_PROCESS_ID, PROCESS_ID, REGULATION_VERSION_ID,
                                QA_PROCESS_ID, "PATROL", "PATROL_AM", BUSINESS_DATE, "DAY", 1))));
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
        return expected(ROUTE_PROCESS_ID, PROCESS_ID, qaProcessId, inspectionRuleKey, inspectionType,
                businessDate, "DAY", roundNo);
    }

    private static MesFrontlinePqcTaskOverlay.ExpectedTaskIdentity expected(long routeProcessId, long processId,
                                                                             long qaProcessId, String inspectionRuleKey,
                                                                             String inspectionType, LocalDate businessDate,
                                                                             String shiftCode, int roundNo) {
        return new MesFrontlinePqcTaskOverlay.ExpectedTaskIdentity(ACTIVE_ORDER_ID, routeProcessId, processId,
                REGULATION_VERSION_ID, qaProcessId, itemCode(inspectionType), inspectionRuleKey, inspectionType,
                businessDate, shiftCode, roundNo, true, 5, List.of());
    }

    private static MesPqcInspectionTaskDO task(long id, long activeOrderId, long regulationVersionId,
                                               long qaProcessId, String inspectionType,
                                               String inspectionRuleKey) {
        return task(id, activeOrderId, ROUTE_PROCESS_ID, PROCESS_ID, regulationVersionId, qaProcessId,
                inspectionType, inspectionRuleKey, BUSINESS_DATE, "DAY", 1);
    }

    private static MesPqcInspectionTaskDO task(long id, long activeOrderId, long regulationVersionId,
                                               long qaProcessId, String inspectionType,
                                               String inspectionRuleKey,
                                               LocalDate businessDate,
                                               int roundNo) {
        return task(id, activeOrderId, ROUTE_PROCESS_ID, PROCESS_ID, regulationVersionId, qaProcessId,
                inspectionType, inspectionRuleKey, businessDate, "DAY", roundNo);
    }

    private static MesPqcInspectionTaskDO task(long id, long activeOrderId, long routeProcessId, long processId,
                                               long regulationVersionId, long qaProcessId, String inspectionType,
                                               String inspectionRuleKey, LocalDate businessDate, String shiftCode,
                                               int roundNo) {
        return MesPqcInspectionTaskDO.builder()
                .id(id)
                .activeOrderId(activeOrderId)
                .routeProcessId(routeProcessId)
                .processId(processId)
                .regulationVersionId(regulationVersionId)
                .qaProcessId(qaProcessId)
                .qaItemCode(itemCode(inspectionType))
                .inspectionType(inspectionType)
                .inspectionRuleKey(inspectionRuleKey)
                .businessDate(businessDate)
                .shiftCode(shiftCode)
                .roundNo(roundNo)
                .plannedInspectionQuantity(5)
                .taskStatus(MesPqcInspectionTaskDO.TASK_STATUS_PENDING)
                .build();
    }

    private static String itemCode(String inspectionType) {
        return "FINAL".equals(inspectionType) ? "" : "ITEM-001";
    }
}
