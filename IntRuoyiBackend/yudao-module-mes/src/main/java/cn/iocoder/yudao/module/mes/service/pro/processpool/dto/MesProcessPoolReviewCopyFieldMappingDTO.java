package cn.iocoder.yudao.module.mes.service.pro.processpool.dto;

import cn.iocoder.yudao.module.mes.service.pro.processpool.MesProcessPoolFragmentOriginalField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MesProcessPoolReviewCopyFieldMappingDTO {

    private String fieldCode;
    private String fieldName;
    private BigDecimal lowerLimit;
    private BigDecimal upperLimit;
    private String valueType;
    private Boolean affectsAllocation;
    private MesProcessPoolFragmentOriginalField allocationField;
    private Long sourceQuantityFragmentId;
    private String templateFieldMetadataJson;
}
