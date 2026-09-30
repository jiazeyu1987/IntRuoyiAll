package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DccControlledFileAttachmentUploadTicketReqVO {

    @NotBlank(message = "attachment uploadTicket is required")
    private String uploadTicket;

    @NotBlank(message = "attachment sessionId is required")
    private String sessionId;

}
