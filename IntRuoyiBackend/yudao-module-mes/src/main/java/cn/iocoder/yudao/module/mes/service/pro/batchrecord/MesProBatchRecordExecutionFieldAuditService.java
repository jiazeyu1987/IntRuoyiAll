package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldAuditDetailReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldAuditDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldAuditExportReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldAuditExportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldAuditItemRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldAuditPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldAuditVerifyReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldAuditVerifyRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldResponsibilityExportReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldResponsibilityExportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldResponsibilityHistoryReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldResponsibilityHistoryRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldResponsibilitySummaryReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordExecutionFieldResponsibilitySummaryRespVO;

public interface MesProBatchRecordExecutionFieldAuditService {

    MesProBatchRecordExecutionFieldAuditSaveResult saveChanges(
            MesProBatchRecordExecutionFieldAuditSaveChangesCommand command);

    MesProBatchRecordExecutionFieldAuditSaveResult saveSystemCellLinkChanges(
            MesProBatchRecordExecutionFieldAuditSaveChangesCommand command);

    MesProBatchRecordExecutionFieldAuditHashVerification verifyChain(Long executionId);

    PageResult<MesProBatchRecordExecutionFieldAuditItemRespVO> getPage(
            MesProBatchRecordExecutionFieldAuditPageReqVO pageReqVO);

    MesProBatchRecordExecutionFieldAuditDetailRespVO getDetail(
            MesProBatchRecordExecutionFieldAuditDetailReqVO reqVO);

    MesProBatchRecordExecutionFieldAuditVerifyRespVO verifyChain(
            MesProBatchRecordExecutionFieldAuditVerifyReqVO reqVO);

    MesProBatchRecordExecutionFieldAuditExportRespVO export(
            MesProBatchRecordExecutionFieldAuditExportReqVO reqVO);

    MesProBatchRecordExecutionFieldResponsibilitySummaryRespVO getResponsibilitySummary(
            MesProBatchRecordExecutionFieldResponsibilitySummaryReqVO reqVO);

    MesProBatchRecordExecutionFieldResponsibilityHistoryRespVO getResponsibilityHistory(
            MesProBatchRecordExecutionFieldResponsibilityHistoryReqVO reqVO);

    MesProBatchRecordExecutionFieldResponsibilityExportRespVO exportResponsibility(
            MesProBatchRecordExecutionFieldResponsibilityExportReqVO reqVO);
}
