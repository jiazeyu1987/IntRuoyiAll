package cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 运行控制台事故分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class RuntimeControlIncidentPageReqVO extends PageParam {

    @Schema(description = "环境")
    private String environment;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "来源类型")
    private String sourceType;
}
