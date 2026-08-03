package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DccNasUncontrolledImportLocalWriteResultReqVO {

    @NotBlank(message = "sourceSignature is required")
    private String sourceSignature;

    @NotBlank(message = "localRelativePath is required")
    private String localRelativePath;

    @NotBlank(message = "localWriteStatus is required")
    private String localWriteStatus;

    private String localWriteErrorCode;

    private String localWriteError;
}
