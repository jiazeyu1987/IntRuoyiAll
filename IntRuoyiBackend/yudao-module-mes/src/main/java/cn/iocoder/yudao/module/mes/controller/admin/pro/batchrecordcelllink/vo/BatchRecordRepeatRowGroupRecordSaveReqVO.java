package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class BatchRecordRepeatRowGroupRecordSaveReqVO {

    @NotNull(message = "重复记录序号不能为空")
    private Integer recordSequence;
    @NotNull(message = "重复记录开始行不能为空")
    private Integer startRowIndex;
    @NotNull(message = "重复记录结束行不能为空")
    private Integer endRowIndex;
}