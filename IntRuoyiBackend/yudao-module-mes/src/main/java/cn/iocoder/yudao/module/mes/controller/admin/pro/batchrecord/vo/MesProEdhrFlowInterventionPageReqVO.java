package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MesProEdhrFlowInterventionPageReqVO extends PageParam {

    private String businessObjectType;

    private String businessObjectId;

    private String businessObjectCode;

    private String flowInstanceId;

    private String interventionAction;

    private String interventionStatus;
}
