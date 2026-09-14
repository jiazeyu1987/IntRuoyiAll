package cn.iocoder.yudao.module.mes.service.pro.simulation.stage6;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordDomainTraceDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordDomainTracePageRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordDomainTracePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProBatchRecordDomainTraceVerifyReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrBatchTraceabilityRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordDomainTraceService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchTraceabilityService;
import cn.iocoder.yudao.module.mes.service.pro.simulation.stage5.MesStage5FinalReleaseSimulationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class MesStage6IdiSimulationServiceImpl implements MesStage6IdiSimulationService {

    private final MesStage5FinalReleaseSimulationService stage5Service;
    private final MesProEdhrBatchTraceabilityService batchTraceabilityService;
    private final MesProBatchRecordDomainTraceService domainTraceService;
    private final MesProBatchRecordExecutionMapper executionMapper;

    public MesStage6IdiSimulationServiceImpl(
            MesStage5FinalReleaseSimulationService stage5Service,
            MesProEdhrBatchTraceabilityService batchTraceabilityService,
            MesProBatchRecordDomainTraceService domainTraceService,
            MesProBatchRecordExecutionMapper executionMapper) {
        this.stage5Service = stage5Service;
        this.batchTraceabilityService = batchTraceabilityService;
        this.domainTraceService = domainTraceService;
        this.executionMapper = executionMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesStage6IdiSimulationResult simulate(MesStage6IdiSimulationCommand command) {
        MesStage6IdiSimulationCommand validated = MesStage6IdiSimulationCommand.validate(
                command == null ? null : command.getSimulationRunId(),
                command == null ? null : command.getStage5SimulationRunId(),
                command == null ? null : command.getBatchExecutionId());

        Map<String, Object> releaseSnapshot = stage5Service.getReleaseSnapshot(
                validated.getStage5SimulationRunId(), validated.getBatchExecutionId());
        MesStage6TraceabilityContractValidator.validateReleaseSnapshot(releaseSnapshot);

        Long batchExecutionId = requireLong(releaseSnapshot.get("batchExecutionId"), "batchExecutionId");
        Long releaseTransactionId = requireLong(releaseSnapshot.get("releaseReceiptId"), "releaseReceiptId");
        Long releaseDecisionId = requireLong(releaseSnapshot.get("releaseDecisionId"), "releaseDecisionId");
        MesProEdhrBatchTraceabilityRespVO batchTraceability =
                batchTraceabilityService.getTraceability(batchExecutionId);
        if (batchTraceability == null || batchTraceability.getLatestManifest() == null
                || !Objects.equals(batchExecutionId, batchTraceability.getLatestManifest().getBatchExecutionId())) {
            throw new IllegalStateException("STAGE6_TRACE_MANIFEST_REQUIRED");
        }

        MesProBatchRecordExecutionDO execution = findExecution(batchExecutionId);
        if (execution == null || execution.getId() == null) {
            throw new IllegalStateException("STAGE6_TRACE_EXECUTION_REQUIRED");
        }

        MesProBatchRecordDomainTracePageReqVO pageRequest = new MesProBatchRecordDomainTracePageReqVO();
        pageRequest.setPageNo(1);
        pageRequest.setPageSize(100);
        pageRequest.setExecutionId(execution.getId());
        PageResult<MesProBatchRecordDomainTracePageRespVO> page =
                domainTraceService.getTracePage(pageRequest);
        if (page == null || page.getList() == null || page.getList().stream()
                .noneMatch(row -> Objects.equals(row.getExecutionId(), execution.getId()))) {
            throw new IllegalStateException("STAGE6_TRACE_PAGE_RESULT_REQUIRED");
        }

        MesProBatchRecordDomainTraceDetailRespVO detail = domainTraceService.getTraceDetail(execution.getId());
        if (detail == null || !Objects.equals(detail.getExecutionId(), execution.getId())) {
            throw new IllegalStateException("STAGE6_TRACE_DETAIL_RESULT_REQUIRED");
        }
        MesProBatchRecordDomainTraceDetailRespVO verified = domainTraceService.verify(
                new MesProBatchRecordDomainTraceVerifyReqVO()
                        .setExecutionId(execution.getId())
                        .setExpectedDomainTraceHash(detail.getDomainTraceHash()));
        if (verified == null || !Objects.equals(verified.getExecutionId(), execution.getId())) {
            throw new IllegalStateException("STAGE6_TRACE_VERIFY_RESULT_REQUIRED");
        }

        Map<String, Object> traceSnapshot = buildTraceSnapshot(
                validated.getSimulationRunId(), batchExecutionId, releaseTransactionId,
                releaseSnapshot, batchTraceability, execution, page, verified);
        MesStage6TraceabilityContractValidator.validateTraceabilitySnapshot(traceSnapshot);

        return new MesStage6IdiSimulationResult()
                .setSimulationRunId(validated.getSimulationRunId())
                .setReleasePreparationStatus(String.valueOf(releaseSnapshot.get("releaseStatus")))
                .setTraceEntryPath("/mes/pro/feedback/edhr-form-trace?tab=release&autoOpenBatchExecutionId="
                        + batchExecutionId)
                .setBatchExecutionId(batchExecutionId)
                .setExecutionId(execution.getId())
                .setReleaseTransactionId(releaseTransactionId)
                .setReleaseDecisionId(releaseDecisionId)
                .setReleaseReceiptId(String.valueOf(releaseSnapshot.get("releaseReceiptId")))
                .setReleaseSnapshot(releaseSnapshot)
                .setTraceabilitySnapshot(traceSnapshot);
    }

    private MesProBatchRecordExecutionDO findExecution(Long batchExecutionId) {
        List<MesProBatchRecordExecutionDO> executions = executionMapper.selectList(
                new LambdaQueryWrapper<MesProBatchRecordExecutionDO>()
                        .eq(MesProBatchRecordExecutionDO::getBatchExecutionId, batchExecutionId)
                        .orderByDesc(MesProBatchRecordExecutionDO::getId));
        return executions.stream().findFirst().orElse(null);
    }

    private Map<String, Object> buildTraceSnapshot(
            String runId, Long batchExecutionId, Long releaseTransactionId,
            Map<String, Object> releaseSnapshot,
            MesProEdhrBatchTraceabilityRespVO batchTraceability,
            MesProBatchRecordExecutionDO execution,
            PageResult<MesProBatchRecordDomainTracePageRespVO> page,
            MesProBatchRecordDomainTraceDetailRespVO verified) {
        List<Map<String, Object>> items = new ArrayList<>();
        if (verified.getItems() != null) {
            for (MesProBatchRecordDomainTraceDetailRespVO.Item item : verified.getItems()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("itemType", item.getItemType());
                row.put("itemKey", item.getItemKey());
                row.put("sourceId", item.getSourceId());
                row.put("sourceCode", item.getSourceCode());
                row.put("sourceVersion", item.getSourceVersion());
                row.put("snapshotHash", item.getSnapshotHash());
                row.put("status", item.getStatus());
                items.add(row);
            }
        }
        List<?> blockers = verified.getBlockers() == null ? List.of() : verified.getBlockers();
        Map<String, Object> frontendEntry = new LinkedHashMap<>();
        frontendEntry.put("formTraceRoute", "/mes/pro/feedback/edhr-form-trace?tab=release"
                + "&autoOpenBatchExecutionId=" + batchExecutionId);
        frontendEntry.put("autoOpenBatchExecutionId", batchExecutionId);
        frontendEntry.put("releaseListAction", "放行追溯列表行操作：追溯");
        frontendEntry.put("traceDrawerComponent", "BatchExecutionTraceDrawer");
        frontendEntry.put("traceDrawerTabs", List.of("批记录表单", "单元责任", "操作审计", "电子签名", "放行事件"));
        frontendEntry.put("domainTraceDetailRoute",
                "/mes/pro/feedback/edhr-domain-trace/detail?executionId=" + execution.getId());
        frontendEntry.put("domainTraceDetailComponent", "DomainTraceDetailPage");
        frontendEntry.put("pageAssertions", Map.of(
                "releaseListDataSource", "RELEASED",
                "drawerDisplaysBatchRecordForm", true,
                "drawerDisplaysOperationAudit", true,
                "drawerDisplaysElectronicSignature", true,
                "drawerDisplaysReleaseEvents", true,
                "domainTraceDetailDisplaysItems", true));

        Map<String, Object> backendSummary = new LinkedHashMap<>();
        backendSummary.put("status", verified.getStatus());
        backendSummary.put("blockers", blockers);
        backendSummary.put("executionId", execution.getId());
        backendSummary.put("domainTraceHash", verified.getDomainTraceHash());
        backendSummary.put("batchTraceabilityCaptured",
                batchTraceability != null && batchTraceability.getLatestManifest() != null);
        backendSummary.put("releaseSnapshotVersion", releaseSnapshot.get("version"));

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("contractName", MesStage6TraceabilityContractValidator.TRACEABILITY_SNAPSHOT_CONTRACT_NAME);
        snapshot.put("contractVersion", MesStage6TraceabilityContractValidator.TRACEABILITY_SNAPSHOT_CONTRACT_VERSION);
        snapshot.put("simulationRunId", runId);
        snapshot.put("batchExecutionId", batchExecutionId);
        snapshot.put("releaseTransactionId", releaseTransactionId);
        snapshot.put("releaseReceiptId", releaseSnapshot.get("releaseReceiptId"));
        snapshot.put("releaseStatus", releaseSnapshot.get("releaseStatus"));
        snapshot.put("tracePageRequest", requestSnapshot("GET",
                "/mes/pro/batch-record-execution/domain-trace/page", execution.getId(), page.getList().size()));
        snapshot.put("traceDetailRequest", requestSnapshot("GET",
                "/mes/pro/batch-record-execution/domain-trace/detail", execution.getId(),
                verified.getItems() == null ? 0 : verified.getItems().size()));
        snapshot.put("traceVerifyRequest", requestSnapshot("POST",
                "/mes/pro/batch-record-execution/domain-trace/verify", execution.getId(),
                verified.getItems() == null ? 0 : verified.getItems().size()));
        snapshot.put("items", items);
        snapshot.put("blockers", blockers);
        snapshot.put("complete", "VERIFIED".equals(verified.getStatus()) && blockers.isEmpty());
        snapshot.put("frontendDisplayEntry", frontendEntry);
        snapshot.put("backendTraceabilitySummary", backendSummary);
        return snapshot;
    }

    private Map<String, Object> requestSnapshot(String method, String path, Long executionId, int resultCount) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("method", method);
        request.put("path", path);
        request.put("statusCode", 200);
        request.put("executionId", executionId);
        request.put("resultCount", resultCount);
        return request;
    }

    private Long requireLong(Object value, String field) {
        if (value == null) {
            throw new IllegalStateException("STAGE6_RELEASE_SNAPSHOT_" + field.toUpperCase() + "_REQUIRED");
        }
        try {
            long parsed = Long.parseLong(String.valueOf(value));
            if (parsed <= 0) {
                throw new NumberFormatException();
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("STAGE6_RELEASE_SNAPSHOT_" + field.toUpperCase() + "_INVALID");
        }
    }
}
