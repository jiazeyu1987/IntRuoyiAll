package cn.iocoder.yudao.module.mes.controller.admin.pro.route.vo.flowconfig;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Schema(description = "管理后台 - MES 工艺路线批记录附件负责人 Response VO")
@Data
@Accessors(chain = true)
public class MesProRouteBatchRecordAttachmentOwnerRespVO {

    @Schema(description = "附件配置编码", example = "INCOMING_INSPECTION_REPORT")
    private String attachmentCode;

    @Schema(description = "附件配置名称", example = "来料检报告")
    private String attachmentName;

    @Schema(description = "默认角色编码", example = "BATCH_ATTACHMENT_INCOMING_REPORT_UPLOAD_1")
    private String defaultRoleCode;

    @Schema(description = "默认角色名称", example = "来料检报告上传1")
    private String defaultRoleName;

    @Schema(description = "候选来源类型：USERS/ROLE", example = "ROLE")
    private String candidateSourceType;

    @Schema(description = "候选来源 ID 列表")
    private List<Long> candidateSourceIds;

    @Schema(description = "候选来源名称快照")
    private List<String> candidateSourceNames;

    @Schema(description = "默认角色已授权的当前租户启用用户 ID 列表")
    private List<Long> assignedUserIds;

    @Schema(description = "默认角色已授权的当前租户启用用户名称快照")
    private List<String> assignedUserNames;

    @Schema(description = "排序", example = "1")
    private Integer sort;

    @Schema(description = "备注")
    private String remark;
}
