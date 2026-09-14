package cn.iocoder.yudao.module.mes.service.pro.batchrecordcelllink;

import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordCellLinkFormCellsRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordCellLinkPrefillRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordCellLinkRulesSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordCellLinkRulesSaveRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordCellLinkWorkbenchContextRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordRepeatRowGroupSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordRepeatRowGroupSaveRespVO;

import java.util.Map;

public interface MesProBatchRecordCellLinkService {

    BatchRecordCellLinkWorkbenchContextRespVO getWorkbenchContext(Long routeId, Long definitionId,
                                                                  Long versionId, String sourceReportId,
                                                                         Long templateId, String versionNo,
                                                                         Long routeProcessId, Long qaProcessId,
                                                                         Long dccProjectCodeId);

    BatchRecordCellLinkFormCellsRespVO getFormCells(String reportId, Long versionId);

    BatchRecordCellLinkRulesSaveRespVO saveRules(BatchRecordCellLinkRulesSaveReqVO reqVO);

    BatchRecordRepeatRowGroupSaveRespVO saveRepeatRowGroup(BatchRecordRepeatRowGroupSaveReqVO reqVO);

    BatchRecordCellLinkPrefillRespVO getPrefill(Long targetExecutionId, Long workTaskId);

    Map<String, Object> buildFormTemplateVersionPrefillData(Long templateVersionId, Long workOrderId,
                                                            String executionBatchCode,
                                                            Map<String, Object> formData);
}
