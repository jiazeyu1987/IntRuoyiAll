package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.*;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrOperationAuditEventMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.service.pro.frontline.*;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureQueryService;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionFieldAuditHasher.*;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_SOURCE_MISSING;

/** Explicit, signed association of an ERP replenishment line to an order's frozen process.
 * The original route inputs and signed production records are never amended by this operation. */
@Service
@RequiredArgsConstructor
public class MesActiveOrderReplenishmentBindingService {
    private static final String OBJECT = "ACTIVE_ORDER_REPLENISHMENT";
    private static final String OPERATION = "REPLENISHMENT_PROCESS_BIND";
    private final MesProcessPoolActiveOrderMapper orders;
    private final MesProcessPoolActiveOrderProcessSnapshotMapper processes;
    private final MesProcessPoolActiveOrderCompletionReceiptMapper receipts;
    private final MesProWorkOrderMapper workOrders;
    private final ErpKingdeeProductionReplenishmentListItemMapper items;
    private final ErpKingdeeProductionReplenishmentListMapper headers;
    private final MesLocalTestRunSourceGuard localSources;
    private final MesFrontlineProcessMaterialService materials;
    private final MesProEdhrOperationAuditEventMapper events;
    private final MesProEdhrOperationAuditService audit;
    private final MesProBatchRecordExecutionSignatureService signing;
    private final ElectronicSignatureQueryService signatures;
    private final GxpAuditService gxpAudit;

    public record BindRequest(@NotNull @Positive Long activeOrderId, @NotNull Integer expectedVersion,
                              @NotNull @Positive Long itemId, @NotNull @Positive Long routeProcessId,
                              @NotBlank String sourceHash, @NotBlank @Size(max = 500) String reason,
                              @NotBlank @Size(max = 200) String password) {}
    public record ProcessOption(String routeProcessId, String name) {}
    public record Line(String itemId, String billNo, String materialCode, String materialName,
                       String lotNumber, BigDecimal actualQuantity, String sourceHash,
                       List<String> frozenRouteProcessIds, String boundRouteProcessId, String signatureId) {}
    public record Page(Integer version, boolean editable, List<ProcessOption> processes, List<Line> lines) {}
    public record Evidence(Long tenantId, Long activeOrderId, Long workOrderId, Long routeVersionId,
                           Long itemId, String sourceHash, Long routeProcessId, String reason,
                           Long signatureId, String challengeHash) {}
    public record Binding(Long eventId, Long routeProcessId, Long signatureId, String challengeHash) {}

    @Transactional(readOnly = true)
    public Page get(Long actorId, Long orderId) {
        var order = requireOrder(orderId, false);
        require(orderId, Objects.equals(order.getLeaderUserId(), actorId), "仅当前订单生产组长可维护补料工序关联");
        var workOrder = workOrders.selectById(order.getWorkOrderId());
        require(orderId, workOrder != null && workOrder.getCode() != null, "生产工单不存在");
        boolean local = localSources.validate(workOrder, false);
        var snapshots = processes.selectListByActiveOrderId(orderId);
        var sourceItems = items.selectListByProductionOrderNo(workOrder.getCode());
        var bindings = read(orderId);
        var lines = new ArrayList<Line>();
        for (var item : sourceItems) {
            var header = headers.selectById(item.getProductionReplenishmentListId());
            validateSource(orderId, workOrder.getCode(), local, item, header);
            var frozenOwners = snapshots.stream().filter(p -> materials.listFrozenMaterials(orderId, order.getRouteId(),
                    p.getRouteProcessId(), p.getProcessId()).stream().anyMatch(m ->
                    MesFrontlineProcessMaterial.ROLE_INPUT.equals(m.materialRole())
                            && Objects.equals(item.getMaterialNumber().trim(), m.materialCode().trim())))
                    .map(p -> p.getRouteProcessId().toString()).toList();
            var binding = bindings.get(item.getId());
            lines.add(new Line(item.getId().toString(), header.getSourceBillNo(), item.getMaterialNumber(),
                    item.getMaterialName(), item.getLotNumber(), item.getActualQuantity(), sourceHash(item, header),
                    frozenOwners, binding == null ? null : binding.routeProcessId().toString(),
                    binding == null ? null : binding.signatureId().toString()));
        }
        return new Page(order.getVersion(), "ACTIVE".equals(order.getActiveStatus())
                && receipts.selectByActiveOrderId(orderId) == null,
                snapshots.stream().map(p -> new ProcessOption(p.getRouteProcessId().toString(), p.getProcessNameSnapshot())).toList(), lines);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long bind(Long actorId, BindRequest request) {
        Long orderId = request.activeOrderId();
        gxpAudit.acquireLedgerLock();
        var order = requireOrder(orderId, true);
        require(orderId, Objects.equals(actorId, order.getLeaderUserId()), "仅当前订单生产组长可签名关联补料工序");
        require(orderId, "ACTIVE".equals(order.getActiveStatus()) && receipts.selectByActiveOrderId(orderId) == null
                && order.getReleaseDecisionId() == null, "订单已完工或已放行，不能修改补料关联");
        require(orderId, Objects.equals(order.getVersion(), request.expectedVersion()), "订单版本已变更，请刷新后重试");
        var page = get(actorId, orderId);
        var line = page.lines().stream().filter(l -> l.itemId().equals(request.itemId().toString())).findFirst().orElse(null);
        require(orderId, line != null && Objects.equals(line.sourceHash(), request.sourceHash()), "补料来源已变化，请刷新后核对");
        require(orderId, page.processes().stream().anyMatch(p -> p.routeProcessId().equals(request.routeProcessId().toString())),
                "所选工序不属于订单冻结路线");
        require(orderId, line.frozenRouteProcessIds().isEmpty(), "补料已在冻结输入物料中配置，不允许重复补正");
        require(orderId, line.boundRouteProcessId() == null, "该补料已有签名工序关联，不允许覆盖");
        var unsigned = new Evidence(TenantContextHolder.getRequiredTenantId(), orderId, order.getWorkOrderId(),
                order.getRouteVersionId(), request.itemId(), request.sourceHash(), request.routeProcessId(),
                request.reason().trim(), null, null);
        String challenge = challenge(unsigned);
        var signed = signing.recordFieldChangeSignature(new MesProBatchRecordExecutionFieldAuditSignatureCommand()
                .setExecutionId(0L).setPassword(request.password()).setReasonCategory("REPLENISHMENT_PROCESS_BINDING")
                .setReasonText(unsigned.reason()).setSignatureChallengeHash(challenge));
        require(orderId, signed != null && signed.getSignatureId() != null && signed.getSignedAt() != null
                && Objects.equals(actorId, signed.getActorId()), "补料关联签名无效");
        var evidence = new Evidence(unsigned.tenantId(), orderId, unsigned.workOrderId(), unsigned.routeVersionId(),
                unsigned.itemId(), unsigned.sourceHash(), unsigned.routeProcessId(), unsigned.reason(), signed.getSignatureId(), challenge);
        verifySignature(evidence, actorId);
        var result = audit.recordInCallerTransaction(new MesProEdhrOperationAuditCommand()
                .setRequestId("REPLENISHMENT_BIND:" + orderId + ":" + request.itemId())
                .setObjectType(OBJECT).setObjectId(orderId.toString()).setRouteId(order.getRouteId())
                .setRouteProcessId(request.routeProcessId()).setOperationType(OPERATION).setActionName("补料工序关联")
                .setActorUserId(actorId).setActorUsername(signed.getActorName())
                .setPermissionCode("mes:pro-process-pool-team-leader:maintain").setPermissionDecision("ALLOW")
                .setResultStatus("SUCCESS").setBeforeSummaryHash(sha256("UNBOUND:" + request.sourceHash()))
                .setAfterSummaryHash(challenge).setMetadataJson(JsonUtils.toJsonString(evidence)).setOccurredAt(signed.getSignedAt()));
        require(orderId, result != null && result.getId() != null, "补料关联审计写入失败");
        return result.getId();
    }

    /** Read formal associations only; any changed source or signature fails closed. */
    public Map<Long, Binding> read(Long orderId) {
        var order = requireOrder(orderId, false);
        Map<Long, Binding> result = new LinkedHashMap<>();
        for (var event : events.selectListByObject(OBJECT, orderId.toString())) {
            require(orderId, OPERATION.equals(event.getOperationType()) && "SUCCESS".equals(event.getResultStatus()), "补料关联审计状态无效");
            var e = JsonUtils.parseObject(event.getMetadataJson(), Evidence.class);
            require(orderId, e != null && Objects.equals(e.tenantId(), TenantContextHolder.getRequiredTenantId())
                    && Objects.equals(e.activeOrderId(), orderId) && Objects.equals(e.workOrderId(), order.getWorkOrderId())
                    && Objects.equals(e.routeVersionId(), order.getRouteVersionId())
                    && Objects.equals(e.routeProcessId(), event.getRouteProcessId())
                    && Objects.equals(event.getRouteId(), order.getRouteId())
                    && Objects.equals(e.challengeHash(), challenge(e))
                    && Objects.equals(e.challengeHash(), event.getAfterSummaryHash()), "补料关联冻结身份或摘要不一致");
            var item = items.selectById(e.itemId());
            var header = item == null ? null : headers.selectById(item.getProductionReplenishmentListId());
            require(orderId, item != null && header != null && Objects.equals(e.sourceHash(), sourceHash(item, header)),
                    "已签名补料来源发生变化，请核对正式补料单");
            require(orderId, processes.selectListByActiveOrderId(orderId).stream()
                    .anyMatch(p -> Objects.equals(e.routeProcessId(), p.getRouteProcessId())), "补料关联工序不属于冻结订单");
            verifySignature(e, event.getActorUserId());
            require(orderId, result.put(e.itemId(), new Binding(event.getId(), e.routeProcessId(), e.signatureId(), e.challengeHash())) == null,
                    "补料存在重复签名关联");
        }
        return result;
    }

    private MesProcessPoolActiveOrderDO requireOrder(Long id, boolean lock) {
        var order = lock ? orders.selectByIdForUpdate(id) : orders.selectById(id);
        require(id, order != null && Objects.equals(order.getTenantId(), TenantContextHolder.getRequiredTenantId()), "当前租户订单不存在");
        return order;
    }

    private void verifySignature(Evidence e, Long actorId) {
        var signature = signatures.getById(e.signatureId());
        String subject = MesBatchRecordSignatureSubjectAdapter.encodeSubjectId(0L, "FIELD_CHANGE", null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, e.challengeHash());
        require(e.activeOrderId(), signature != null && Objects.equals(actorId, signature.actorId())
                && "MES".equals(signature.moduleCode()) && "FIELD_CHANGE".equals(signature.actionCode())
                && "MES_BATCH_RECORD".equals(signature.subjectType()) && Objects.equals(subject, signature.subjectId())
                && Objects.equals(sha256(subject), signature.subjectVersion()) && Objects.equals(e.reason(), signature.reason()),
                "补料关联签名与业务正文不一致");
        var verified = signatures.verifyEvidence(e.signatureId());
        require(e.activeOrderId(), verified != null && "VALID".equals(verified.verificationStatus())
                && Objects.equals(e.signatureId(), verified.signatureId())
                && Objects.equals(signature.contentHash(), verified.calculatedContentHash())
                && Objects.equals(signature.evidenceHash(), verified.calculatedEvidenceHash()), "补料关联签名完整性核验失败");
    }

    private void validateSource(Long orderId, String workCode, boolean local,
                                ErpKingdeeProductionReplenishmentListItemDO item, ErpKingdeeProductionReplenishmentListDO header) {
        require(orderId, item != null && header != null && item.getId() != null
                && Objects.equals(TenantContextHolder.getRequiredTenantId(), item.getTenantId())
                && Objects.equals(TenantContextHolder.getRequiredTenantId(), header.getTenantId())
                && Objects.equals(workCode, item.getProductionOrderNo())
                && header.getSourceBillNo() != null && !header.getSourceBillNo().isBlank()
                && item.getMaterialNumber() != null && !item.getMaterialNumber().isBlank()
                && item.getMaterialName() != null && !item.getMaterialName().isBlank()
                && item.getActualQuantity() != null && item.getActualQuantity().signum() >= 0
                && (local ? MesLocalTestRunSourceGuard.STATUS.equals(header.getDocumentStatus())
                    && MesLocalTestRunSourceGuard.SOURCE.equals(header.getSourceFormId())
                    : "C".equals(header.getDocumentStatus()) && !MesLocalTestRunSourceGuard.SOURCE.equals(header.getSourceFormId())
                    && !MesLocalTestRunSourceGuard.SOURCE.equals(item.getSourceFormId())), "补料单未审核或正式来源不完整");
    }

    static String sourceHash(ErpKingdeeProductionReplenishmentListItemDO item, ErpKingdeeProductionReplenishmentListDO header) {
        // Persist business fields, excluding synchronization timestamps and raw ERP payload.
        var source = new LinkedHashMap<String, Object>();
        source.put("tenantId", item.getTenantId()); source.put("itemId", item.getId());
        source.put("headerId", header.getId()); source.put("billNo", header.getSourceBillNo());
        source.put("status", header.getDocumentStatus()); source.put("sourceFormId", header.getSourceFormId());
        source.put("lineKey", item.getSourceLineKey()); source.put("productionOrderNo", item.getProductionOrderNo());
        source.put("materialCode", item.getMaterialNumber()); source.put("materialName", item.getMaterialName());
        source.put("specification", item.getMaterialSpecification()); source.put("unit", item.getUnitName());
        source.put("lot", item.getLotNumber()); source.put("actualQuantity", item.getActualQuantity());
        source.put("baseActualQuantity", item.getBaseActualQuantity()); source.put("requestedQuantity", item.getRequestedQuantity());
        return sha256(canonicalizeJsonString(JsonUtils.toJsonString(source)));
    }

    static String challenge(Evidence e) {
        return sha256("REPLENISHMENT_PROCESS_BIND_V1:" + canonicalizeJsonString(JsonUtils.toJsonString(
                new Evidence(e.tenantId(), e.activeOrderId(), e.workOrderId(), e.routeVersionId(), e.itemId(),
                        e.sourceHash(), e.routeProcessId(), e.reason(), null, null))));
    }

    private static void require(Long id, boolean valid, String reason) {
        if (!valid) throw exception(PRO_PROCESS_POOL_ACTIVE_ORDER_COMPLETION_SOURCE_MISSING, id, reason);
    }
}
