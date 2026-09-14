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
                                                         DccControlledFilePrintCreateReqVO reqVO);

    List<DccControlledFilePrintRecordRespVO> getPrintRecords(Long userId, Long controlledFileId);

    DccControlledFilePrintHtmlRespVO getPrintHtml(Long userId, Long controlledFileId, Long printRecordId);

}
