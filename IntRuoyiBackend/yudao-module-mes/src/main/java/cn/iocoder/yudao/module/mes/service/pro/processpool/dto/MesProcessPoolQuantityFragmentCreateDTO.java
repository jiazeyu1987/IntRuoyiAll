package cn.iocoder.yudao.module.mes.service.pro.processpool.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesProcessPoolQuantityFragmentCreateDTO {

    private String sourceQuantityType;
    private String qualityStatus;
    private BigDecimal totalQuantity;
    private String rawPayload;
    private Boolean simulated;
    private String simulationStage;
    private String simulationRunId;
}
