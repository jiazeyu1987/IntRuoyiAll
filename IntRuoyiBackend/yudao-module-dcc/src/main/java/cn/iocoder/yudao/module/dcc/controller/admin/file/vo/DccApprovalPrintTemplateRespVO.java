package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DCC approval print template response.
 */
@Data
public class DccApprovalPrintTemplateRespVO {

    private Long id;
    private Long templateFileId;
    private String templateFileName;
    private String templateFileContentType;
    private Boolean active;
    private String remark;
    private List<String> requiredPlaceholders;
    private List<String> supportedPlaceholders;
    private LocalDateTime updateTime;

}
