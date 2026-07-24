package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MesProEdhrUnifiedChangePageReqVO extends PageParam {

    private String controlledObjectType;

    private String controlledObjectId;

    private String controlledObjectCode;

    private String changeType;

    private String changeStatus;

    private String riskLevel;
}
