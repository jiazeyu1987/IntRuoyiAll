package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import lombok.Data;

/**
 * DCC controlled print HTML response.
 */
@Data
public class DccControlledFilePrintHtmlRespVO {

    private Long printRecordId;
    private String printNo;
    private String html;

}
