package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MesProEdhrInitManifestUploadReqVO {

    @NotNull(message = "初始化批次不能为空")
    private Long initBatchId;

    @NotBlank(message = "包类型不能为空")
    private String packageType;

    @NotBlank(message = "manifestHash 不能为空")
    private String manifestHash;

    @NotBlank(message = "源文件不能为空")
    private String sourceFileName;

    private String sourceFileUrl;

    private Long fileSize;

    private String checksumJson;

    @NotBlank(message = "manifestJson 不能为空")
    private String manifestJson;
}
