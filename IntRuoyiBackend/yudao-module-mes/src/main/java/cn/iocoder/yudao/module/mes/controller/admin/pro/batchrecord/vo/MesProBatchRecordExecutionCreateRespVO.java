package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProBatchRecordExecutionCreateRespVO {

    private Long id;

    private String executionCode;

    private Integer status;
}
