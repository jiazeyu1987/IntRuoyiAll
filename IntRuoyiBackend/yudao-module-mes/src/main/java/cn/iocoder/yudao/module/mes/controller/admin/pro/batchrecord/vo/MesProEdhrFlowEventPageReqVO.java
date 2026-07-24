package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MesProEdhrFlowEventPageReqVO extends PageParam {

    @NotBlank(message = "业务对象类型不能为空")
    private String businessObjectType;

    @NotBlank(message = "业务对象不能为空")
    private String businessObjectId;

    private String flowInstanceId;

    private String eventType;
}
