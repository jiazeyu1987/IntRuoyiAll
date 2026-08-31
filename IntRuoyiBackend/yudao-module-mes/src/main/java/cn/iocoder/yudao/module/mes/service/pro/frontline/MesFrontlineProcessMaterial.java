package cn.iocoder.yudao.module.mes.service.pro.frontline;

import java.math.BigDecimal;
import java.util.List;

public record MesFrontlineProcessMaterial(Long materialId,
                                          String materialCode,
                                          String materialName,
                                          String materialSpecification,
                                          BigDecimal bomQuantity,
                                          List<String> batchCodes) {

    public MesFrontlineProcessMaterial(Long materialId, String materialCode, String materialName,
                                       String materialSpecification, BigDecimal bomQuantity) {
        this(materialId, materialCode, materialName, materialSpecification, bomQuantity, List.of());
    }
}
