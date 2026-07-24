package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MesProEdhrReleaseCheckItemPageReqVO extends PageParam {

    @NotNull(message = "放行事务不能为空")
    private Long releaseTransactionId;

    private String checkCategory;

    private String checkResult;

    private String itemStatus;

    private String sourceObjectCode;
}
