package cn.iocoder.yudao.module.mes.service.pro.frontline;

import java.math.BigDecimal;
import java.util.List;

public record MesFrontlinePqcInspectionItem(String itemCode,
                                            String itemName,
                                            String inspectionMethod,
                                            String standardText,
                                            String inspectionTool,
                                            String samplingPlanText,
                                            BigDecimal standardLowerLimit,
                                            BigDecimal standardUpperLimit,
                                            String standardUnit,
                                            Integer standardPrecision,
                                            Boolean equipmentRequired,
                                            String resultType,
                                            List<EquipmentOption> equipmentOptions) {

    public record EquipmentOption(Long equipmentId,
                                  String equipmentCode,
                                  String equipmentName,
                                  String equipmentNumber,
                                  Boolean defaultFlag,
                                  Integer sort) {
    }
}
