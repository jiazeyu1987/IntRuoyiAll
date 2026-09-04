package cn.iocoder.yudao.module.mes.service.pro.frontline;

import java.math.BigDecimal;
import java.util.List;

public record MesFrontlineProcessMaterial(Long materialId,
                                          String materialCode,
                                          String materialName,
                                          String materialSpecification,
                                          String materialRole,
                                          BigDecimal bomQuantity,
                                          List<String> batchCodes,
                                          BigDecimal requestedQuantity,
                                          BigDecimal actualQuantity,
                                          BigDecimal baseActualQuantity,
                                          List<Long> sourcePickListIds,
                                          List<String> sourcePickListNos,
                                          List<Long> sourcePickListItemIds,
                                          String sourceSnapshotHash) {

    public static final String ROLE_INPUT = "INPUT";
    public static final String ROLE_OUTPUT = "OUTPUT";

    public MesFrontlineProcessMaterial(Long materialId, String materialCode, String materialName,
                                       String materialSpecification, BigDecimal bomQuantity,
                                       List<String> batchCodes) {
        this(materialId, materialCode, materialName, materialSpecification, ROLE_OUTPUT,
                bomQuantity, batchCodes, null, null, null, List.of(), List.of(), List.of(), null);
    }

    public MesFrontlineProcessMaterial(Long materialId, String materialCode, String materialName,
                                       String materialSpecification, BigDecimal bomQuantity) {
        this(materialId, materialCode, materialName, materialSpecification, ROLE_OUTPUT,
                bomQuantity, List.of(), null, null, null, List.of(), List.of(), List.of(), null);
    }
}
