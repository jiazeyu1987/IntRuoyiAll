package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 排产工单完成排产后创建 eDHR 执行批次的正式业务命令。
 */
@Data
@Accessors(chain = true)
public class EdhrScheduleCompletionCreateCommand {

    private Long scheduleOrderId;

    private String scheduleOrderCode;

    private Long workOrderId;

    private String batchCode;

    private Long productId;

    private Long routeId;

    private String remark;
}
