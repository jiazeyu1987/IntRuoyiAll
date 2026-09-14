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

    private String workOrderCode;

    private String batchCode;

    private String processName;

    private List<String> nodeTypes;

    private Long batchExecutionId;
}
