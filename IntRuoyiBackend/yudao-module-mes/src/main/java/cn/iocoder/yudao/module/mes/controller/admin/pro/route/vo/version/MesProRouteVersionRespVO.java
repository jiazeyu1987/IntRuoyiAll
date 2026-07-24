package cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.version;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - MES 工艺路线版本 Response VO")
@Data
public class MesProRouteVersionRespVO {

    @Schema(description = "编号")
    private Long id;

    @Schema(description = "工艺路线编号")
    private Long routeId;

    @Schema(description = "版本号")
    private String versionNo;

    @Schema(description = "是否当前生效")
    private Boolean active;

    @Schema(description = "生命周期状态")
    private String lifecycleStatus;

    @Schema(description = "来源路线版本编号")
    private Long sourceRouteVersionId;

    @Schema(description = "提交人")
    private Long submittedBy;

    @Schema(description = "提交时间")
    private LocalDateTime submittedTime;

    @Schema(description = "审批流程实例编号")
    private String approvalProcessInstanceId;

    @Schema(description = "发布人")
    private Long publishedBy;

    @Schema(description = "发布时间")
    private LocalDateTime publishedTime;

    @Schema(description = "备注")
    private String remark;
}
