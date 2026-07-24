package cn.iocoder.yudao.module.dcc.controller.admin.signature.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - DCC电子签名图片 Response VO")
@Data
public class DccElectronicSignatureImageRespVO {

    @Schema(description = "签名图片ID", example = "9001")
    private Long id;

    @Schema(description = "用户ID", example = "100")
    private Long userId;

    @Schema(description = "版本号", example = "2")
    private Integer versionNo;

    @Schema(description = "文件ID", example = "6001")
    private Long fileId;

    @Schema(description = "图片访问地址")
    private String fileUrl;

    @Schema(description = "原文件名", example = "signature.png")
    private String fileName;

    @Schema(description = "MIME类型", example = "image/png")
    private String contentType;

    @Schema(description = "文件大小", example = "10240")
    private Long fileSize;

    @Schema(description = "图片SHA-256")
    private String sha256;

    @Schema(description = "图片SHA-256短码")
    private String sha256Short;

    @Schema(description = "图片状态", example = "ACTIVE")
    private String imageStatus;

    @Schema(description = "是否当前启用")
    private Boolean active;

    @Schema(description = "上传人ID", example = "100")
    private Long uploadedBy;

    @Schema(description = "上传时间")
    private LocalDateTime uploadedAt;

    @Schema(description = "启用时间")
    private LocalDateTime enabledAt;

    @Schema(description = "停用时间")
    private LocalDateTime disabledAt;

    @Schema(description = "停用原因")
    private String disableReason;

    @Schema(description = "签名引用次数")
    private Integer referencedCount;
}
