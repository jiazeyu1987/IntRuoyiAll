package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.feedback.vo.frontline.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProductionQuantityDraftDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.*;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.*;
import cn.iocoder.yudao.module.system.service.gxpaudit.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

@Service
@RequiredArgsConstructor
public class MesProductionQuantityDraftService {
    private final MesProductionQuantityDraftMapper drafts;
    private final MesProcessPoolActiveOrderMapper orders;
    private final MesTeamLeaderScopeService scopes;
    private final MesProductionQuantityCapacityService capacity;
    private final MesProBatchRecordExecutionSignatureService signatures;
    private final GxpAuditService audit;
    private final MesProEdhrNonconformanceReviewService nonconformance;
    private final cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper workOrders;

    @Data
    public static class Review {
        @NotNull private Long id;
        @NotNull private Integer expectedVersion;
        @NotBlank private String decision;
        @NotBlank private String reason;
        @NotBlank private String signaturePassword;
    }

    public List<MesProductionQuantityDraftDO> own() { return drafts.own(login()); }

    public List<MesProductionQuantityDraftDO> leader() {
        return drafts.leader(login()).stream().filter(row -> {
            var order = orders.selectById(row.getActiveOrderId());
            return order != null && Objects.equals(order.getLeaderUserId(), login());
        }).toList();
    }

    /** A blocked request remains blocked even if the user changes its idempotency key. */
    public MesProductionQuantityDraftDO requireRetry(MesProFrontlineFeedbackSubmitReqVO request) {
        MesProductionQuantityDraftDO row;
        if (request.getQuantityDraftId() != null) {
            row = drafts.lock(request.getQuantityDraftId());
            if (row == null || !Objects.equals(row.getOperatorUserId(), login())
                    || !Objects.equals(row.getEmployeeId(), request.getActualEmployeeId())
                    || !Objects.equals(row.getIdentityDomain(), request.getSignatureIdentityDomain())) {
                throw invalidParamException("超限草稿不属于当前账号和签名员工");
            }
            if (!List.of("BLOCKED", "READY").contains(row.getStatus())) {
                throw invalidParamException("超限草稿已提交或已失效，请核对原提交回执");
            }
        } else {
            row = drafts.own(login()).stream().filter(item ->
                    Objects.equals(item.getEmployeeId(), request.getActualEmployeeId())
                    && Objects.equals(item.getIdentityDomain(), request.getSignatureIdentityDomain())
                    && Objects.equals(item.getActiveOrderId(), request.getProcessPoolContext().getActiveOrderId())
                    && Objects.equals(item.getRouteProcessId(), request.getProcessPoolContext().getRouteProcessId()))
                    .findFirst().orElse(null);
            if (row != null) row = drafts.lock(row.getId());
        }
        if (row != null && "READY".equals(row.getStatus())) {
            var original = orders.selectByIdForUpdate(row.getActiveOrderId());
            var target = orders.selectByIdForUpdate(request.getProcessPoolContext().getActiveOrderId());
            if (original == null || target == null || !Objects.equals(original.getLeaderUserId(), row.getLeaderUserId())
                    || !Objects.equals(target.getLeaderUserId(), row.getLeaderUserId())
                    || !Objects.equals(original.getRouteId(), target.getRouteId())
                    || !Objects.equals(original.getRouteVersionId(), target.getRouteVersionId())
                    || !Objects.equals(row.getProcessId(), request.getProcessPoolContext().getProcessId())) {
                throw invalidParamException("草稿更正目标须属于原组长及同一冻结工艺、工序");
            }
            if ("HISTORY_ERROR".equals(row.getDecision())
                    && !Objects.equals(row.getActiveOrderId(), target.getId())) {
                throw invalidParamException("历史报工更正后的重提须使用原订单");
            }
            if (!Objects.equals(original.getWorkOrderId(), target.getWorkOrderId())) {
                var sourceWork = workOrders.selectById(original.getWorkOrderId());
                var targetWork = workOrders.selectById(target.getWorkOrderId());
                if (sourceWork == null || targetWork == null || sourceWork.getProductId() == null
                        || !Objects.equals(sourceWork.getProductId(), targetWork.getProductId())
                        || !Objects.equals(sourceWork.getMaterialSpecification(), targetWork.getMaterialSpecification())
                        || !Objects.equals(original.getDccProjectCodeId(), target.getDccProjectCodeId())) {
                    throw invalidParamException("草稿更正目标须为同产品、规格及项目的订单");
                }
            }
        }
        return row;
    }

    public MesProductionQuantityDraftDO capture(MesProFrontlineFeedbackSubmitReqVO request,
            String requestHash, MesProductionQuantityCapacityService.Capacity limit,
            MesProductionQuantityDraftDO retry) {
        var keyHash = DigestUtil.sha256Hex(request.getProcessPoolSubmissionIdempotencyKey());
        var row = retry == null ? drafts.findRequest(login(), keyHash) : retry;
        if (row != null) {
            // Preserve the original draft and its signed review. A failed retry never overwrites them.
            return row;
        }
        row = new MesProductionQuantityDraftDO();
        row.setOperatorUserId(login()); row.setEmployeeId(request.getActualEmployeeId());
        row.setIdentityDomain(request.getSignatureIdentityDomain());
        row.setLeaderUserId(limit.order().getLeaderUserId());
        row.setActiveOrderId(limit.order().getId()); row.setWorkOrderId(limit.order().getWorkOrderId());
        row.setRouteProcessId(request.getProcessPoolContext().getRouteProcessId());
        row.setProcessId(request.getProcessPoolContext().getProcessId());
        row.setSubmitKeyHash(keyHash); row.setRequestHash(requestHash);
        row.setRequestJson(sanitize(JsonUtils.parseTree(JsonUtils.toJsonString(request))).toString());
        row.setRequestedQuantity(limit.requested()); row.setPreviousQuantity(limit.previous());
        row.setUpperQuantity(limit.upper()); row.setStatus("BLOCKED"); row.setVersion(0);
        if (drafts.insert(row) != 1) throw new IllegalStateException("超限草稿保存失败，未产生正式报工");
        appendAudit(row, "capture", "保留超限填写，等待生产组长核对", null, "ABSENT");
        return row;
    }

    public MesProFrontlineFeedbackSubmitRespVO blocked(MesProductionQuantityDraftDO row, String message) {
        return new MesProFrontlineFeedbackSubmitRespVO().setSubmissionStatus("QUANTITY_BLOCKED")
                .setQuantityDraftId(row.getId()).setQuantityBlockMessage(message);
    }

    @Transactional(rollbackFor = Exception.class)
    public void review(Review command) {
        audit.acquireLedgerLock();
        var row = drafts.lock(command.getId());
        if (row == null || !Objects.equals(row.getLeaderUserId(), login())) {
            throw invalidParamException("无权处理该超限草稿");
        }
        var order = orders.selectByIdForUpdate(row.getActiveOrderId());
        if (order == null || !Objects.equals(order.getLeaderUserId(), login())
                || !"ACTIVE".equals(order.getActiveStatus()) || !"ACTIVE".equals(order.getBusinessStatus())) {
            throw invalidParamException("草稿所属订单已转交或结束生产");
        }
        nonconformance.ensureWorkOrderNotFrozen(order.getWorkOrderId(), "超限草稿处理");
        scopes.assertCanAccessEmployee(login(), "PRODUCTION", row.getEmployeeId());
        if (!"BLOCKED".equals(row.getStatus()) || !Objects.equals(row.getVersion(), command.getExpectedVersion())) {
            throw invalidParamException("草稿处理状态已变化，请刷新后核对");
        }
        if (!List.of("CURRENT_ERROR", "HISTORY_ERROR").contains(command.getDecision())
                || command.getReason().isBlank() || command.getReason().length() > 1000) {
            throw invalidParamException("请选择本次填写有误或历史归属有误，并填写1000字以内的处理说明");
        }
        if ("HISTORY_ERROR".equals(command.getDecision())) {
            var original = JsonUtils.parseObject(row.getRequestJson(), MesProFrontlineFeedbackSubmitReqVO.class);
            if (capacity.evaluate(original).exceeded()) {
                throw invalidParamException("历史报工重新分配后，本次填写仍会超限；请先完成历史归属更正");
            }
        }
        String before = JsonUtils.toJsonString(row);
        String challenge = DigestUtil.sha256Hex(JsonUtils.toJsonString(Map.of(
                "draftId", row.getId(), "version", row.getVersion(), "requestHash", row.getRequestHash(),
                "decision", command.getDecision(), "reason", command.getReason().trim())));
        var signature = signatures.recordFieldChangeSignature(new MesProBatchRecordExecutionFieldAuditSignatureCommand()
                .setExecutionId(0L).setPassword(command.getSignaturePassword())
                .setReasonCategory("PRODUCTION_QUANTITY_REVIEW").setReasonText(command.getReason().trim())
                .setSignatureChallengeHash(challenge));
        if (signature == null || signature.getSignatureId() == null || !Objects.equals(signature.getActorId(), login())) {
            throw new IllegalStateException("草稿处理签名身份不匹配");
        }
        row.setDecision(command.getDecision()); row.setReviewReason(command.getReason().trim());
        row.setReviewSignatureId(signature.getSignatureId()); row.setReviewedAt(signature.getSignedAt());
        row.setStatus("READY"); row.setVersion(row.getVersion() + 1);
        if (drafts.updateById(row) != 1) throw new IllegalStateException("草稿处理保存失败");
        appendAudit(row, "review", command.getReason().trim(), signature.getSignatureId(), before);
    }

    public void submitted(MesProductionQuantityDraftDO row, Long eventId, String requestHash) {
        if (row == null) return;
        if (!"READY".equals(row.getStatus()) || row.getReviewSignatureId() == null) {
            throw new IllegalStateException("超限草稿尚未由生产组长签署处理");
        }
        String before = JsonUtils.toJsonString(row);
        row.setStatus("SUBMITTED"); row.setSubmittedEventId(eventId); row.setResubmittedRequestHash(requestHash);
        row.setVersion(row.getVersion() + 1);
        if (drafts.updateById(row) != 1) throw new IllegalStateException("草稿与正式报工关联失败");
        appendAudit(row, "resubmit", "按组长处理结果重新提交并通过最新数量校验", row.getReviewSignatureId(), before);
    }

    private void appendAudit(MesProductionQuantityDraftDO row, String action, String reason, Long signatureId, String before) {
        audit.append(GxpAuditCommand.builder().eventSchemaVersion(2)
                .operationId("mes.production.quantity-draft." + action)
                .subjectId("MES_PRODUCTION_QUANTITY_DRAFT:" + row.getId()).subjectVersion(String.valueOf(row.getVersion()))
                .reason(reason).reasonCode("PRODUCTION_QUANTITY_" + action.toUpperCase()).reasonSource("USER")
                .beforeState(GxpAuditStateEnvelope.builder().state("ABSENT".equals(before) ? "ABSENT" : "PRESENT")
                        .canonicalJson("ABSENT".equals(before) ? "{}" : before).build())
                .afterState(GxpAuditStateEnvelope.builder().state("PRESENT").canonicalJson(JsonUtils.toJsonString(row)).build())
                .idempotencyKey("QTY-DRAFT:" + row.getId() + ":" + row.getVersion() + ":" + action)
                .requestId("MES-QTY:" + row.getId() + ":" + row.getVersion()).resultStatus("SUCCESS")
                .sourceType("SERVICE_METHOD").sourceLocator(getClass().getName() + "#" + action)
                .signatureRecordId(signatureId == null ? null : String.valueOf(signatureId))
                .links(List.of(new GxpAuditRelation("SOURCE", "ACTIVE_ORDER", String.valueOf(row.getActiveOrderId()), null, null)))
                .build());
    }

    private static JsonNode sanitize(JsonNode node) {
        if (node.isObject()) {
            ObjectNode object = (ObjectNode) node;
            var names = new java.util.ArrayList<String>(); object.fieldNames().forEachRemaining(names::add);
            for (String name : names) {
                if (name.matches("(?i).*(password|credential|secret|token|authorization|cookie).*")) object.remove(name);
                else sanitize(object.get(name));
            }
        } else if (node.isArray()) node.forEach(MesProductionQuantityDraftService::sanitize);
        return node;
    }

    private static Long login() {
        Long id = SecurityFrameworkUtils.getLoginUserId();
        if (id == null) throw new IllegalStateException("请先登录");
        return id;
    }
}
