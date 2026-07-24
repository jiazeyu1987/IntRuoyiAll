package cn.iocoder.yudao.module.infra.controller.admin.runtimecontrol.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.module.infra.service.runtimecontrol.RuntimeControlSiteMessageStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 运行控制台告警分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class RuntimeControlAlertPageReqVO extends PageParam {

    @Schema(description = "环境", example = "prod")
    private String environment;

    @Schema(description = "动作或异常类型", example = "backup-failed")
    private String action;

    @Schema(description = "站内信发送状态")
    private RuntimeControlSiteMessageStatus siteMessageStatus;
}
