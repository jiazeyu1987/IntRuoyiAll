package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MesProEdhrUnifiedChangeImpactPageReqVO extends PageParam {

    @NotNull(message = "变更申请不能为空")
    private Long changeRequestId;

    private String impactType;

    private String impactObjectType;

    private String riskLevel;
}
