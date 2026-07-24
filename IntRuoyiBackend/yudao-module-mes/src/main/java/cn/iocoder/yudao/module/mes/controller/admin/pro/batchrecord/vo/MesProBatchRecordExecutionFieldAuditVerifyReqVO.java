package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProBatchRecordExecutionFieldAuditVerifyReqVO {

    @NotNull(message = "executionId 不能为空")
    private Long executionId;
    private Long fromFieldAuditRevision;
    private Long toFieldAuditRevision;
    private String expectedFieldAuditHeadHash;
    private String expectedCellValuesHash;
    private Boolean includeBrokenItem;
}
