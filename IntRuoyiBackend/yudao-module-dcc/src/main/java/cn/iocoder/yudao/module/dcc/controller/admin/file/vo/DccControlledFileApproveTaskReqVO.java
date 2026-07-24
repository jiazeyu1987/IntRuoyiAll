package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - DCC 受控文件审批通过 Request VO")
@Data
public class DccControlledFileApproveTaskReqVO {

    @Schema(description = "BPM 任务编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "task-1")
    @NotBlank(message = "任务编号不能为空")
    private String taskId;

    @Schema(description = "当前登录密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "123456")
    @NotBlank(message = "密码不能为空")
    private String password;

    @Schema(description = "审批意见", example = "同意发布")
    private String reason;

    @Schema(description = "上传会话编号", example = "dcc-approve-session-1")
    private String sessionId;

    @Schema(description = "第四节点上传的受控章 PDF 上传凭证", example = "UT-20260528-0001")
    private String stampedPdfUploadTicket;

    @Schema(description = "第四节点确认的最终存入叶子目录编号", example = "1001")
    private Long confirmedDirectoryId;

    @JsonIgnore
    @Schema(hidden = true)
    private Long stampedPdfFileId;

    @JsonIgnore
    @Schema(hidden = true)
    private Long trainingRecordFileId;

    @Schema(description = "第四节点选择的文件下发范围与介质")
    private List<DistributionScope> selectedDistributionScopes;

    @Schema(description = "管理后台 - DCC 受控文件审批通过下发范围")
    @Data
    public static class DistributionScope {

        @Schema(description = "下发部门编号", example = "300")
        private Long departmentId;

        @Schema(description = "下发介质，PUBLIC_FOLDER=电子公共文件夹，PAPER=纸质", example = "PUBLIC_FOLDER")
        private String distributionMedium;

    }

}
