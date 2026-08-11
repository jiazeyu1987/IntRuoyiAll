package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - DCC 受控文件任务动作就绪度 Request VO")
@Data
public class DccControlledFileTaskReadinessReqVO {

    @Schema(description = "BPM 任务编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "task-1")
    @NotBlank(message = "任务编号不能为空")
    private String taskId;

    @Schema(description = "上传会话编号", example = "dcc-approve-session-1")
    private String sessionId;

    @Schema(description = "受控章 PDF 上传凭证", example = "UT-20260528-0001")
    private String stampedPdfUploadTicket;

    @Schema(description = "最终存入叶子目录编号", example = "1001")
    private Long confirmedDirectoryId;

    @Schema(description = "文件下发范围与介质")
    private List<DccControlledFileApproveTaskReqVO.DistributionScope> selectedDistributionScopes;

}
