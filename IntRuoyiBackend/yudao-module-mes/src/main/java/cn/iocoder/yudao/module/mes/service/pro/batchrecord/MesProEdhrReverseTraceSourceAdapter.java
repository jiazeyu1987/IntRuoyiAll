package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReverseTraceModels.Category;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReverseTraceModels.CatalogItem;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReverseTraceModels.Condition;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReverseTraceModels.EvidenceItem;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReverseTraceModels.EvidenceResult;

import java.util.List;

/**
 * P2 adapter boundary. P3/P4 provide formal source readers; P2 never fabricates
 * a hit when a source adapter is absent.
 */
public interface MesProEdhrReverseTraceSourceAdapter {

    Category category();

    boolean supports(String evidenceKey);

    CatalogResult readCatalog(MesProEdhrBatchExecutionDO batch);

    EvaluationResult evaluate(MesProEdhrBatchExecutionDO batch, List<Condition> conditions);

    EvidenceResult readEvidence(MesProEdhrBatchExecutionDO batch, List<Condition> conditions);

    ValidationResult validateCondition(CatalogItem item, Condition condition);

    record CatalogResult(String sourceVersion, String sourceIdentity, String status, String reasonCode, String reason,
                         List<CatalogItem> items) {
    }

    record EvaluationResult(String sourceVersion, String sourceIdentity, String status, String reasonCode, String reason,
                            List<ConditionMatch> matches) {
    }

    record ConditionMatch(String conditionId, boolean matched, String summary, String sourceRef) {
    }

    record EvidenceResult(String sourceVersion, String sourceIdentity, String targetBatchExecutionId,
                          List<EvidenceItem> items) {
    }

    record ValidationResult(boolean valid, String reasonCode, String reason) {
    }
}
