package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo;

import jakarta.validation.Valid;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class BatchRecordCellLinkRulesSaveReqVO {

    private String scopeType;
    private Long scopeId;
    private Long routeId;
    private Long batchRecordDefinitionId;
    private Long batchRecordVersionId;
    @Valid
    private List<BatchRecordCellLinkRuleSaveItemReqVO> rules;
}
