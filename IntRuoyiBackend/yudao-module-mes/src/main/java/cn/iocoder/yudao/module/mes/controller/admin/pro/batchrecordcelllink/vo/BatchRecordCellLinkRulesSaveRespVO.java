package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class BatchRecordCellLinkRulesSaveRespVO {

    private Integer savedCount;
    private Long ruleVersion;
    private List<BatchRecordCellLinkRuleVO> rules;
}
