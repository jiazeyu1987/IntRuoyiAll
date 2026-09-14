package cn.iocoder.yudao.module.mes.service.pro.frontline;

import java.time.LocalDate;
import java.util.List;

public record MesFrontlinePqcProcessCandidate(Long routeId,
                                              String routeCode,
                                              String routeName,
                                              Long dccProjectCodeId,
                                              Long regulationId,
                                              Long regulationVersionId,
                                              Long qaProcessId,
                                              String qaProcessCode,
                                              String qaProcessName,
                                              Integer qaProcessSort,
                                              Long activeOrderId,
                                              Long pqcTaskId,
                                              Boolean finalInspectionApplicable,
                                              String inspectionType,
                                              LocalDate businessDate,
                                              String shiftCode,
                                              Integer roundNo,
                                              Integer plannedInspectionQuantity,
                                              List<MesFrontlinePqcInspectionItem> inspectionItems,
                                              List<MesFrontlinePqcTaskOption> pqcTaskOptions) {
}
