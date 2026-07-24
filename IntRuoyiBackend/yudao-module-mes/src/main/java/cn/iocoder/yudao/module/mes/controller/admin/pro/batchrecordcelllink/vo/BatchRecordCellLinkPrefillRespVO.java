package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class BatchRecordCellLinkPrefillRespVO {

    private Long targetExecutionId;
    private List<BatchRecordCellLinkPrefillItemVO> prefills;
    private List<BatchRecordCellLinkPrefillItemVO> conflicts;
}
