package cn.iocoder.yudao.module.mes.service.pro.frontline;

import java.time.LocalDate;
import java.util.List;

public record MesFrontlinePqcTaskOption(Long pqcTaskId,
                                        Long regulationVersionId,
                                        Boolean finalInspectionApplicable,
                                        String inspectionType,
                                        LocalDate businessDate,
                                        String shiftCode,
                                        Integer roundNo,
                                        Integer plannedInspectionQuantity,
                                        List<MesFrontlinePqcInspectionItem> inspectionItems) {
}
