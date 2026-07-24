package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class MesProBatchRecordExecutionFieldAuditPageReqVO extends PageParam {

    private Long executionId;
    private Long auditBatchId;
    private String fieldPath;
    private String fieldKey;
    private Long actorId;
    private String actorName;
    private String reasonCategory;
    private String reasonKeyword;
    private LocalDateTime changedAtStart;
    private LocalDateTime changedAtEnd;
}
