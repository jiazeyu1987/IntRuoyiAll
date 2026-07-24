package cn.iocoder.yudao.module.dcc.controller.admin.signature.governance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 电子签名统一签名记录 Response VO")
@Data
public class SignatureGovernanceRecordRespVO {

    @Schema(description = "全局记录 ID", example = "FILE-1001")
    private String globalId;

    @Schema(description = "来源编码", example = "FILE")
    private String sourceCode;

    @Schema(description = "来源中文", example = "文件")
    private String sourceLabel;

    @Schema(description = "来源表", example = "dcc_controlled_file_signature")
    private String sourceTable;

    @Schema(description = "来源记录 ID", example = "1001")
    private Long sourceRecordId;

    @Schema(description = "业务记录 ID", example = "88001")
    private Long businessRecordId;

    @Schema(description = "业务记录编号", example = "BR-20260714-001")
    private String businessRecordCode;

    @Schema(description = "业务记录名称", example = "批记录执行")
    private String businessRecordName;

    @Schema(description = "签名人用户 ID", example = "101")
    private Long signerUserId;

    @Schema(description = "签名人")
    private String signerName;

    @Schema(description = "账号快照")
    private String actorUsernameSnapshot;

    @Schema(description = "昵称快照")
    private String actorNicknameSnapshot;

    @Schema(description = "部门快照")
    private String actorDeptNameSnapshot;

    @Schema(description = "岗位快照")
    private String actorPostNamesSnapshot;

    @Schema(description = "角色快照")
    private String actorRoleNamesSnapshot;

    @Schema(description = "动作编码")
    private String actionCode;

    @Schema(description = "动作显示文本")
    private String actionLabel;

    @Schema(description = "含义编码")
    private String meaningCode;

    @Schema(description = "含义显示文本")
    private String meaningLabel;

    @Schema(description = "签名意见")
    private String comment;

    @Schema(description = "签名时间")
    private LocalDateTime signedAt;

    @Schema(description = "证据 Hash")
    private String evidenceHash;

    @Schema(description = "证据状态")
    private String evidenceStatus;

    @Schema(description = "签名图片 ID")
    private Long signatureImageId;

    @Schema(description = "签名图片版本号")
    private Integer signatureImageVersionNo;

    @Schema(description = "签名图片文件 ID")
    private Long signatureImageFileId;

    @Schema(description = "签名图片文件 URL")
    private String signatureImageFileUrl;

    @Schema(description = "签名图片 SHA-256")
    private String signatureImageSha256;

    @Schema(description = "签名图片内容类型")
    private String signatureImageContentType;

    @Schema(description = "签名图片文件大小")
    private Long signatureImageFileSize;

    @Schema(description = "签名图片状态快照")
    private String signatureImageStatusSnapshot;

    @Schema(description = "签名图片校验状态")
    private String signatureImageVerifiedStatus;

    @Schema(description = "详情路由名")
    private String detailRouteName;

    @Schema(description = "详情路径")
    private String detailPath;

}
