package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrReleaseTransactionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionOriginMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptMapper;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderCompletionFlow6ReceiptPort;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.*;

/** Reads frozen owner receipts and the sealed trace chain; never reconstructs facts from current masters. */
@Service
public class MesProEdhrActiveOrderPrintSource {
    @Resource private MesProEdhrBatchExecutionOriginMapper originMapper;
    @Resource private MesProcessPoolActiveOrderCompletionReceiptMapper receiptMapper;
    @Resource private MesTeamLeaderActiveOrderCompletionFlow6ReceiptPort receiptPort;
    @Resource private MesProEdhrBatchTraceabilityService traceabilityService;

    public JSONObject requireSource(MesProEdhrBatchExecutionDO batch, MesProEdhrReleaseTransactionDO release) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        if (batch == null || !Objects.equals(batch.getTenantId(), tenantId)
                || !Objects.equals(batch.getStatus(), MesProEdhrBatchExecutionServiceImpl.BATCH_STATUS_ARCHIVED)
                || release == null || !Objects.equals(release.getTenantId(), tenantId)
                || !Objects.equals(release.getBatchExecutionId(), batch.getId())
                || !Objects.equals(release.getWorkOrderId(), batch.getWorkOrderId())
                || !"RELEASED".equals(release.getReleaseStatus())
                || release.getApprovedBy() == null || release.getApprovedAt() == null
                || release.getApprovalSignatureId() == null || release.getReleaseDecisionId() == null
                || StrUtil.isBlank(release.getApprovalSignoffEvidenceHash())
                || StrUtil.isBlank(release.getFinalizationPayloadHash())) {
            throw exception(PRO_EDHR_BATCH_PRINT_RELEASE_REQUIRED);
        }
        var origins = originMapper.selectListByBatchExecutionId(batch.getId());
        if (origins.size() != 1 || !MesProEdhrBatchTraceFormalSourceResolver
                .isActiveOrderEntryType(origins.get(0).getEntryType())
                || !"CAPTURED".equals(origins.get(0).getRelationStatus())) {
            throw exception(PRO_EDHR_BATCH_PRINT_SOURCE_INVALID, "缺少唯一的正式活跃订单来源");
        }
        var origin = origins.get(0);
        var receipt = receiptMapper.selectByIdAndTenantId(origin.getCompletionBackfillReceiptId(), tenantId);
        try {
            MesProEdhrReverseTraceReceiptValidator.validate(batch, origin, receipt);
        } catch (IllegalStateException failure) {
            throw exception(PRO_EDHR_BATCH_PRINT_SOURCE_INVALID, failure.getMessage());
        }
        // This also verifies the owner's original creation audit and all-part receipt bindings.
        var verified = receiptPort.requireVerifiedByReceiptId(receipt.getId(), tenantId);
        if (verified == null || !Objects.equals(verified.receipt().getReceiptHash(), receipt.getReceiptHash())) {
            throw exception(PRO_EDHR_BATCH_PRINT_SOURCE_INVALID, "完工创建证据与冻结回执不一致");
        }
        var traceability = traceabilityService.getTraceability(batch.getId());
        JSONObject formal = JSON.parseObject(receipt.getFormalSourceSnapshotJson());
        JSONObject production = formal.getJSONObject("productionFacts");
        if (production == null || production.getJSONArray("events") == null
                || production.getJSONArray("events").isEmpty() || production.getJSONArray("reviews") == null
                || formal.getJSONArray("snapshots") == null || formal.getJSONArray("completions") == null
                || formal.getJSONArray("pqcTasks") == null || formal.getJSONArray("pqcDetails") == null) {
            throw exception(PRO_EDHR_BATCH_PRINT_SOURCE_INVALID, "冻结生产、检验或签署事实不完整");
        }
        JSONObject source = new JSONObject(true);
        source.put("completionReceiptId", receipt.getId());
        source.put("completionReceiptHash", receipt.getReceiptHash());
        source.put("sourceSnapshotHash", receipt.getSourceSnapshotHash());
        source.put("formalSourceSnapshotJson", receipt.getFormalSourceSnapshotJson());
        source.put("signatureSnapshotJson", receipt.getSignatureSnapshotJson());
        source.put("lossConditionFactsJson", receipt.getLossConditionFactsJson());
        source.put("completionCreation", verified.creation());
        source.put("formalSourceSnapshot", formal);
        source.put("signatureSnapshot", JSON.parseObject(receipt.getSignatureSnapshotJson()));
        source.put("lossConditionFacts", JSON.parse(receipt.getLossConditionFactsJson()));
        source.put("traceability", traceability);
        return source;
    }
}
