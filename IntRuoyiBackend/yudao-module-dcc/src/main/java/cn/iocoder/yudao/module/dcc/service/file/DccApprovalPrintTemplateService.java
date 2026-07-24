package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccApprovalPrintHtmlRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccApprovalPrintTemplateRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccApprovalPrintTemplateSaveReqVO;

/**
 * DCC approval print template service.
 */
public interface DccApprovalPrintTemplateService {

    DccApprovalPrintTemplateRespVO saveActiveTemplate(Long userId, DccApprovalPrintTemplateSaveReqVO reqVO);

    DccApprovalPrintTemplateRespVO getActiveTemplate();

    DccApprovalPrintRenderedWord exportApprovalWord(Long userId, Long controlledFileId);

    DccApprovalPrintHtmlRespVO getApprovalPrintHtml(Long userId, Long controlledFileId);
}
