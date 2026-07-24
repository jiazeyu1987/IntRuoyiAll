package cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 表单中心生效失败待处理分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class FormEffectPendingPageReqVO extends PageParam {

    @Schema(description = "租户编号")
    private Long tenantId;

    @Schema(description = "表单实例编号")
    private Long instanceId;

}
