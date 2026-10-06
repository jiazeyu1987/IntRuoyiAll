package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MesProEdhrWorkTaskPageReqVO extends PageParam {

    private String taskType;

    private String status;

    /** 个人工作台同时查询待处理和逾期任务；不可与单状态条件同时使用。 */
    private Boolean includeOverdue;

    private String workOrderCode;

    private String batchCode;

    private String processName;

    private List<String> nodeTypes;

    private Long batchExecutionId;
}
