package cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 表单中心模板池分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class FormCenterTemplatePoolPageReqVO extends PageParam {

    @Schema(description = "租户编号")
    private Long tenantId;

    @Schema(description = "模板名称")
    private String templateName;

    @Schema(description = "状态")
    private String status;

}
