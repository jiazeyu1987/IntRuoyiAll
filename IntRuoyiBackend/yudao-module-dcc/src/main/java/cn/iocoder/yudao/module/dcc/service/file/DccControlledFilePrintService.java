package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePrintCreateReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePrintHtmlRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePrintRecordRespVO;

import java.util.List;

/**
 * DCC controlled file print service.
 */
public interface DccControlledFilePrintService {

    DccControlledFilePrintRecordRespVO createPrintRecord(Long userId, Long controlledFileId,
                                                         DccControlledFilePrintCreateReqVO reqVO, DccRequestAuditContext auditContext);

    List<DccControlledFilePrintRecordRespVO> getPrintRecords(Long userId, Long controlledFileId, DccRequestAuditContext auditContext);

    DccControlledFilePrintHtmlRespVO getPrintHtml(Long userId, Long controlledFileId, Long printRecordId, DccRequestAuditContext auditContext);

}
