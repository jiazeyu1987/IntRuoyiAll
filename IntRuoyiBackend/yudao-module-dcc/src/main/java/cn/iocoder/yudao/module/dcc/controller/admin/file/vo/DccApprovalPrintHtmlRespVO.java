package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import lombok.Data;

import java.util.List;

/**
 * DCC approval print HTML response.
 */
@Data
public class DccApprovalPrintHtmlRespVO {

    private Long templateId;
    private String templateFileName;
    private String html;
    private List<String> requiredPlaceholders;

}
