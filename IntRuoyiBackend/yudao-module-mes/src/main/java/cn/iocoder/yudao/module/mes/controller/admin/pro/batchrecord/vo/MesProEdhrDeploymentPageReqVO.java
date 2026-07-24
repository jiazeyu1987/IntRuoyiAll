package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - eDHR 部署授权接口证据分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MesProEdhrDeploymentPageReqVO extends PageParam {

    @Schema(description = "交付项目ID")
    private Long projectId;

    @Schema(description = "部署证据编号")
    private String deploymentCode;

    @Schema(description = "部署证据名称")
    private String deploymentName;

    @Schema(description = "部署状态")
    private String deploymentStatus;

    @Schema(description = "发布标签")
    private String releaseTag;

    @Schema(description = "目标环境")
    private String targetEnvironment;
}

