package cn.iocoder.yudao.module.mes.service.pro.frontline;

import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public record MesFrontlinePqcTaskOverlay(Long activeOrderId,
                                         Long regulationVersionId,
                                         Long qaProcessId,
                                         String inspectionRuleKey,
                                         String inspectionType,
                                         String status,
                                         MesFrontlinePqcTaskOption pqcTaskOption) {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_NOT_CREATED = "NOT_CREATED";

    public static MesFrontlinePqcTaskOverlay fromExpectedTask(ExpectedTaskIdentity expectedTask,
                                                              Collection<MesPqcInspectionTaskDO> tasks) {
        Objects.requireNonNull(expectedTask, "expectedTask");
        List<MesPqcInspectionTaskDO> matches = selectMatchingPendingTasks(expectedTask, tasks);
        if (matches.isEmpty()) {
            return new MesFrontlinePqcTaskOverlay(expectedTask.activeOrderId(), expectedTask.regulationVersionId(),
                    expectedTask.qaProcessId(), expectedTask.inspectionRuleKey(), expectedTask.inspectionType(),
                    STATUS_NOT_CREATED, null);
        }
        if (matches.size() > 1) {
            throw new IllegalStateException("duplicate PQC task overlay identity: activeOrderId="
                    + expectedTask.activeOrderId() + ", regulationVersionId=" + expectedTask.regulationVersionId()
                    + ", qaProcessId=" + expectedTask.qaProcessId()
                    + ", inspectionRuleKey=" + expectedTask.inspectionRuleKey());
        }
        MesPqcInspectionTaskDO task = matches.get(0);
        MesFrontlinePqcTaskOption option = new MesFrontlinePqcTaskOption(task.getId(),
                task.getRegulationVersionId(), task.getQaProcessId(), expectedTask.finalInspectionApplicable(),
                task.getInspectionType(), task.getBusinessDate(), task.getShiftCode(), task.getRoundNo(),
                task.getPlannedInspectionQuantity(), List.copyOf(expectedTask.inspectionItems()));
        return new MesFrontlinePqcTaskOverlay(expectedTask.activeOrderId(), expectedTask.regulationVersionId(),
                expectedTask.qaProcessId(), expectedTask.inspectionRuleKey(), expectedTask.inspectionType(),
                STATUS_PENDING, option);
    }

    public static List<MesFrontlinePqcTaskOverlay> fromExpectedTasks(Collection<ExpectedTaskIdentity> expectedTasks,
                                                                     Collection<MesPqcInspectionTaskDO> tasks) {
        if (expectedTasks == null || expectedTasks.isEmpty()) {
            return List.of();
        }
        List<OverlaySortEntry> overlays = new ArrayList<>();
        for (ExpectedTaskIdentity expectedTask : expectedTasks) {
            overlays.add(new OverlaySortEntry(expectedTask, fromExpectedTask(expectedTask, tasks)));
        }
        return overlays.stream()
                .sorted(Comparator.comparing((OverlaySortEntry entry) -> requireBusinessDate(entry.expectedTask()))
                        .thenComparingInt(entry -> ruleSort(entry.expectedTask().inspectionRuleKey()))
                        .thenComparingInt(entry -> requireRoundNo(entry.expectedTask()))
                        .thenComparingLong(entry -> taskIdSort(entry.overlay())))
                .map(OverlaySortEntry::overlay)
                .toList();
    }

    private static List<MesPqcInspectionTaskDO> selectMatchingPendingTasks(ExpectedTaskIdentity expectedTask,
                                                                           Collection<MesPqcInspectionTaskDO> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return List.of();
        }
        return tasks.stream()
                .filter(Objects::nonNull)
                .filter(task -> MesPqcInspectionTaskDO.TASK_STATUS_PENDING.equals(task.getTaskStatus()))
                .filter(task -> Objects.equals(expectedTask.activeOrderId(), task.getActiveOrderId()))
                .filter(task -> Objects.equals(expectedTask.regulationVersionId(), task.getRegulationVersionId()))
                .filter(task -> Objects.equals(expectedTask.qaProcessId(), task.getQaProcessId()))
                .filter(task -> Objects.equals(expectedTask.inspectionRuleKey(), task.getInspectionRuleKey()))
                .filter(task -> Objects.equals(expectedTask.inspectionType(), task.getInspectionType()))
                .toList();
    }

    private static LocalDate requireBusinessDate(ExpectedTaskIdentity expectedTask) {
        if (expectedTask.businessDate() == null) {
            throw new IllegalArgumentException("businessDate is required for PQC task overlay sorting");
        }
        return expectedTask.businessDate();
    }

    private static int requireRoundNo(ExpectedTaskIdentity expectedTask) {
        if (expectedTask.roundNo() == null) {
            throw new IllegalArgumentException("roundNo is required for PQC task overlay sorting");
        }
        return expectedTask.roundNo();
    }

    private static int ruleSort(String inspectionRuleKey) {
        return switch (inspectionRuleKey) {
            case "FIRST" -> 10;
            case "PATROL_AM" -> 20;
            case "PATROL_PM" -> 30;
            case "FINAL" -> 40;
            default -> throw new IllegalArgumentException("unsupported inspectionRuleKey for PQC task overlay sorting: "
                    + inspectionRuleKey);
        };
    }

    private static long taskIdSort(MesFrontlinePqcTaskOverlay overlay) {
        return overlay.pqcTaskOption() == null ? Long.MAX_VALUE : overlay.pqcTaskOption().pqcTaskId();
    }

    private record OverlaySortEntry(ExpectedTaskIdentity expectedTask,
                                    MesFrontlinePqcTaskOverlay overlay) {
    }

    public record ExpectedTaskIdentity(Long activeOrderId,
                                       Long regulationVersionId,
                                       Long qaProcessId,
                                       String inspectionRuleKey,
                                       String inspectionType,
                                       LocalDate businessDate,
                                       String shiftCode,
                                       Integer roundNo,
                                       Boolean finalInspectionApplicable,
                                       Integer plannedInspectionQuantity,
                                       List<MesFrontlinePqcInspectionItem> inspectionItems) {

        public ExpectedTaskIdentity {
            inspectionItems = inspectionItems == null ? List.of() : List.copyOf(inspectionItems);
        }
    }
}
