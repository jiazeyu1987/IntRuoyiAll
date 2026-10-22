package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MesProEdhrDeviationBatchOptionRespVO {

    private Long batchExecutionId;
    private String batchExecutionCode;
    private Long workOrderId;
    private String workOrderCode;
    private String batchCode;
    private String productCode;
    private String productName;
    private String routeName;
    private Integer batchStatus;
}
