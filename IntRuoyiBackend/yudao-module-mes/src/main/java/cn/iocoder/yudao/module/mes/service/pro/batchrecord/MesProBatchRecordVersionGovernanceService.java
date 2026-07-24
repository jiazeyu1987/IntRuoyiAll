package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordVersionDraftReuploadRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordVersionGovernanceImpactRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordVersionGovernanceInspectionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordVersionGovernanceMetricsRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordVersionGovernanceRollbackReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordVersionGovernanceSummaryRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordVersionMigrationConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordVersionMigrationConfirmRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordVersionMigrationDiffRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrUnifiedChangeRespVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MesProBatchRecordVersionGovernanceService {

    MesProBatchRecordVersionGovernanceSummaryRespVO getSummary(Long definitionId);

    MesProBatchRecordVersionGovernanceImpactRespVO getImpact(Long versionId);

    MesProBatchRecordVersionGovernanceInspectionRespVO getInspection(Long versionId);

    MesProBatchRecordVersionGovernanceMetricsRespVO getMetrics(Long versionId);

    MesProEdhrUnifiedChangeRespVO requestRollback(MesProBatchRecordVersionGovernanceRollbackReqVO reqVO);

    MesProBatchRecordVersionMigrationDiffRespVO getMigrationDiff(Long versionId);

    MesProBatchRecordVersionMigrationConfirmRespVO confirmMigrationItems(
            Long versionId, MesProBatchRecordVersionMigrationConfirmReqVO reqVO);

    MesProBatchRecordVersionDraftReuploadRespVO reuploadDraft(Long versionId, MultipartFile file,
                                                              List<String> productNames, String remark);
}
