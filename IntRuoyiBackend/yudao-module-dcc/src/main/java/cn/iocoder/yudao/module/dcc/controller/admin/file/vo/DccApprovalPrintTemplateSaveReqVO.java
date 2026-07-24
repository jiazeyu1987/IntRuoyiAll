package cn.iocoder.yudao.module.dcc.controller.admin.file.vo;

import lombok.Data;

/**
 * DCC approval print template save request.
 */
@Data
public class DccApprovalPrintTemplateSaveReqVO {

    private Long templateFileId;

    private String templateFileUrl;

    private String remark;

}
