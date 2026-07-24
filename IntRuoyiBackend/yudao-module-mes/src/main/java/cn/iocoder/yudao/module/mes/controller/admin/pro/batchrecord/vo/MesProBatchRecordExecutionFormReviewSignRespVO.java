package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProBatchRecordExecutionFormReviewSignRespVO {

    private Long executionId;

    private Integer status;

    private Long signatureId;

    private String actionType;

    private String meaningText;

    private String cellValuesHash;

    private Long fieldAuditRevision;

    private String fieldAuditHeadHash;
}
