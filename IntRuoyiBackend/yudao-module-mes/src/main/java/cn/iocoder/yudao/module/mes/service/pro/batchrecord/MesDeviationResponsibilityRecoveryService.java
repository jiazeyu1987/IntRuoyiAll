package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkService;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesDeviationReceiverAssignReqVO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrDeviationMapper;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import com.alibaba.fastjson.JSON;
import cn.hutool.crypto.digest.DigestUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrBatchExecutionErrorCodeConstants.*;

/** Explicit recovery of an unassigned historical deviation; never infer an owner. */
@Service
public class MesDeviationResponsibilityRecoveryService {
    @Resource private MesProEdhrDeviationMapper deviationMapper;
    @Resource private MesDeviationAdministratorService administratorService;
    @Resource private SecurityFrameworkService securityFrameworkService;
    @Resource private GxpAuditService gxpAuditService;
    @Resource private MesProEdhrOperationAuditService auditService;
    @Resource private MesProEdhrWorkTaskService workTaskService;

    @Transactional(rollbackFor = Exception.class)
    public void assignReceiver(Long actor, MesDeviationReceiverAssignReqVO req) {
        if (actor == null || actor <= 0 || !securityFrameworkService.hasPermission("mes:pro-edhr-deviation:handle")
                || req.getReason() == null || req.getReason().isBlank()) {
            throw exception(PRO_EDHR_DEVIATION_HANDLING_PERMISSION_DENIED, "补录接收人及原因");
        }
        Long tenant = TenantContextHolder.getRequiredTenantId();
        gxpAuditService.acquireLedgerLock();
        administratorService.requireReceiver(actor);
        var target = administratorService.requireReceiver(req.getReceiverId());
        var deviation = deviationMapper.selectByTenantAndIdForUpdate(tenant, req.getDeviationId());
        if (deviation == null || !Objects.equals(tenant, deviation.getTenantId())) {
            throw exception(PRO_EDHR_DEVIATION_HANDLING_NOT_EXISTS, req.getDeviationId());
        }
        if (!"OPEN".equals(deviation.getStatus())) throw exception(PRO_EDHR_DEVIATION_HANDLING_CLOSED, deviation.getId());
        if (deviation.getReceiverId() != null) throw exception(PRO_EDHR_DEVIATION_HANDLING_PERMISSION_DENIED, "仅补录缺失接收人");
        var before = new TreeMap<String, Object>();
        before.put("deviationId", deviation.getId());
        before.put("receiverId", null);
        before.put("receiverName", deviation.getReceiverName());
        before.put("version", deviation.getVersion());
        if (!Objects.equals(deviation.getVersion(), req.getExpectedDeviationVersion())
                || deviationMapper.assignMissingReceiver(tenant, deviation.getId(), req.getExpectedDeviationVersion(),
                    target.getId(), target.getNickname(), actor) != 1) {
            throw exception(PRO_EDHR_DEVIATION_HANDLING_VERSION_CONFLICT, deviation.getId());
        }
        var after = new TreeMap<String, Object>(before);
        after.put("receiverId", target.getId());
        after.put("receiverName", target.getNickname());
        after.put("version", req.getExpectedDeviationVersion() + 1);
        deviation.setReceiverId(target.getId()).setReceiverName(target.getNickname());
        workTaskService.assignDeviationTask(deviation, MesProEdhrWorkTaskService.TASK_TYPE_DEVIATION_RECEIVE,
                target.getId(), "补录历史偏差接收人：" + req.getReason().trim());
        auditService.recordInCallerTransaction(new MesProEdhrOperationAuditCommand()
                .setRequestId("EDHR_DEVIATION_RECEIVER_ASSIGN:" + deviation.getId() + ":" + req.getExpectedDeviationVersion())
                .setObjectType("EDHR_DEVIATION").setObjectId(deviation.getId().toString())
                .setBatchExecutionId(deviation.getBatchExecutionId()).setRecordCategory("EDHR_DEVIATION_HANDLING")
                .setOperationType("UPDATE").setActionName("补录历史偏差接收人").setActorUserId(actor)
                .setPermissionDecision("ALLOW").setResultStatus("SUCCESS")
                .setBeforeSummaryHash(DigestUtil.sha256Hex(JSON.toJSONString(before)))
                .setAfterSummaryHash(DigestUtil.sha256Hex(JSON.toJSONString(after)))
                .setMetadataJson(JSON.toJSONString(Map.of("reason", req.getReason().trim(),
                        "receiverId", target.getId(), "beforeVersion", req.getExpectedDeviationVersion(),
                        "afterVersion", req.getExpectedDeviationVersion() + 1,
                        "beforeContent", before, "afterContent", after))));
    }
}
