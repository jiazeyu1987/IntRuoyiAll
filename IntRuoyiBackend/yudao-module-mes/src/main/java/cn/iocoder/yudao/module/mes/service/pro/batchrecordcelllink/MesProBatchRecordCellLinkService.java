package cn.iocoder.yudao.module.mes.service.pro.batchrecordcelllink;

import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordCellLinkFormCellsRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordCellLinkPrefillRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordCellLinkRulesSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordCellLinkRulesSaveRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordCellLinkWorkbenchContextRespVO;

public interface MesProBatchRecordCellLinkService {

    BatchRecordCellLinkWorkbenchContextRespVO getWorkbenchContext(Long routeId, Long definitionId,
                                                                  Long versionId, String sourceReportId);

    BatchRecordCellLinkFormCellsRespVO getFormCells(String reportId, Long versionId);

    BatchRecordCellLinkRulesSaveRespVO saveRules(BatchRecordCellLinkRulesSaveReqVO reqVO);

    BatchRecordCellLinkPrefillRespVO getPrefill(Long targetExecutionId, Long workTaskId);
}
