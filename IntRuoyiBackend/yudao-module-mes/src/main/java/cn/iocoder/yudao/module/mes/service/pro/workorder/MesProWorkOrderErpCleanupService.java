package cn.iocoder.yudao.module.mes.service.pro.workorder;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.service.config.ErpKingdeeConfigService;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProductionOrderClient;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProperties;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProWorkOrderErpCleanupPreviewRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.scheduleorder.vo.MesProWorkOrderErpCleanupReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesKingdeeProductionOrderSyncRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesKingdeeProductionOrderSyncRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderBomMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderErpCleanupMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.enums.pro.MesProWorkOrderStatusEnum;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception0;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_WORK_ORDER_ERP_CLEANUP_BLOCKED;

@Service
@RequiredArgsConstructor
public class MesProWorkOrderErpCleanupService {
    private final ErpKingdeeProductionOrderClient productionOrderClient;
    private final ErpKingdeeConfigService configService;
    private final MesProWorkOrderMapper workOrderMapper;
    private final MesKingdeeProductionOrderSyncRecordMapper syncRecordMapper;
    private final MesProWorkOrderErpCleanupMapper cleanupMapper;
    private final MesProWorkOrderBomMapper bomMapper;
    private final GxpAuditService gxpAuditService;

    public MesProWorkOrderErpCleanupPreviewRespVO analyze() {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        ErpKingdeeProperties properties = configService.getEffectiveProperties();
        properties.validateBaseConfig();
        List<MesProWorkOrderDO> workOrders = workOrderMapper.selectListAll();
        Map<Long, MesKingdeeProductionOrderSyncRecordDO> records = new LinkedHashMap<>();
        Map<Long, MesKingdeeProductionOrderSyncRecordDO> allRecords = new LinkedHashMap<>();
        for (MesKingdeeProductionOrderSyncRecordDO record : syncRecordMapper.selectList()) {
            if (allRecords.putIfAbsent(record.getWorkOrderId(), record) != null) {
                throw blocked("同一工单存在多条 ERP 同步关联，无法确定正式来源");
            }
        }
        int unverifiableCount = 0;
        for (MesProWorkOrderDO workOrder : workOrders) {
            MesKingdeeProductionOrderSyncRecordDO record = allRecords.get(workOrder.getId());
            if (!hasIdentity(record)) {
                unverifiableCount++;
                continue;
            }
            records.put(workOrder.getId(), record);
        }
        Set<String> existing = productionOrderClient.findExistingProductionOrderBillNos(properties,
                records.values().stream().map(MesKingdeeProductionOrderSyncRecordDO::getSourceBillNo).distinct().toList());
        List<MesProWorkOrderErpCleanupPreviewRespVO.Item> missing = new ArrayList<>();
        for (MesProWorkOrderDO workOrder : workOrders) {
            MesKingdeeProductionOrderSyncRecordDO record = records.get(workOrder.getId());
            if (record == null || existing.contains(record.getSourceBillNo())) {
                continue;
            }
            MesProWorkOrderErpCleanupPreviewRespVO.Item item = new MesProWorkOrderErpCleanupPreviewRespVO.Item();
            item.setWorkOrderId(workOrder.getId());
            item.setWorkOrderCode(workOrder.getCode());
            item.setSyncRecordId(record.getId());
            item.setSourceFid(record.getSourceFid());
            item.setSourceBillNo(record.getSourceBillNo());
            item.setSourceMaterialNumber(record.getSourceMaterialNumber());
            String retainReason = retainReason(tenantId, workOrder);
            item.setDeletable(retainReason == null);
            item.setRetainReason(retainReason);
            missing.add(item);
        }
        MesProWorkOrderErpCleanupPreviewRespVO result = new MesProWorkOrderErpCleanupPreviewRespVO();
        result.setEnvironmentKey(environmentKey(properties));
        result.setAnalyzedCount(records.size());
        result.setUnverifiableCount(unverifiableCount);
        result.setMissingWorkOrders(missing);
        return result;
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public int deleteMissing(MesProWorkOrderErpCleanupReqVO request) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        ErpKingdeeProperties properties = configService.getEffectiveProperties();
        properties.validateBaseConfig();
        if (!Objects.equals(request.getEnvironmentKey(), environmentKey(properties))) {
            throw blocked("ERP 环境已变化，请重新分析后确认删除");
        }
        if (request.getItems() == null || request.getItems().isEmpty()
                || request.getItems().stream().anyMatch(Objects::isNull)
                || request.getItems().stream().anyMatch(item -> item.getWorkOrderId() == null)
                || request.getItems().stream().map(MesProWorkOrderErpCleanupReqVO.Item::getWorkOrderId).distinct().count()
                != request.getItems().size()) {
            throw blocked("清理请求中的工单编号不能为空或重复");
        }
        // 与同步及生产写入共用审计账本锁；锁工单后再复核关联，整批校验通过后才删除。
        gxpAuditService.acquireLedgerLock();
        List<MesProWorkOrderErpCleanupReqVO.Item> items = request.getItems().stream()
                .sorted(java.util.Comparator.comparing(MesProWorkOrderErpCleanupReqVO.Item::getWorkOrderId)).toList();
        for (MesProWorkOrderErpCleanupReqVO.Item item : items) {
            MesProWorkOrderDO workOrder = workOrderMapper.selectByIdForUpdate(item.getWorkOrderId());
            if (workOrder == null) {
                throw blocked("工单已变化或已删除，请重新分析");
            }
            MesKingdeeProductionOrderSyncRecordDO record = syncRecordMapper.selectByWorkOrderId(workOrder.getId());
            if (!hasIdentity(record) || !Objects.equals(record.getId(), item.getSyncRecordId())
                    || !Objects.equals(record.getSourceFid(), item.getSourceFid())
                    || !Objects.equals(record.getSourceBillNo(), item.getSourceBillNo())
                    || !Objects.equals(record.getSourceMaterialNumber(), item.getSourceMaterialNumber())) {
                throw blocked("工单 " + workOrder.getCode() + " 的 ERP 同步关联已变化，请重新分析");
            }
            String reason = retainReason(tenantId, workOrder);
            if (reason != null) {
                throw blocked("工单 " + workOrder.getCode() + "：" + reason + "，请重新分析");
            }
        }
        Set<String> existing = productionOrderClient.findExistingProductionOrderBillNos(properties,
                items.stream().map(MesProWorkOrderErpCleanupReqVO.Item::getSourceBillNo).distinct().toList());
        if (!existing.isEmpty()) {
            throw blocked("ERP 中已存在以下订单，未执行删除，请重新分析：" + String.join("、", existing));
        }
        for (MesProWorkOrderErpCleanupReqVO.Item item : items) {
            bomMapper.deleteByWorkOrderId(item.getWorkOrderId());
            cleanupMapper.deleteMaterialSnapshots(tenantId, item.getWorkOrderId());
            cleanupMapper.deleteScheduleIssues(tenantId, item.getWorkOrderId());
            if (cleanupMapper.deleteSyncRecords(tenantId, item.getWorkOrderId()) < 1
                    || workOrderMapper.deleteById(item.getWorkOrderId()) != 1) {
                throw blocked("清理工单发生并发冲突，删除已回滚，请重新分析");
            }
        }
        return items.size();
    }

    private String retainReason(Long tenantId, MesProWorkOrderDO workOrder) {
        if (Boolean.TRUE.equals(workOrder.getTemporaryFrozen())) return "工单已冻结，保留工单";
        if (positive(workOrder.getQuantityProduced()) || positive(workOrder.getQuantityScheduled())) {
            return "已有生产或排产数量，保留工单";
        }
        if (Objects.equals(workOrder.getStatus(), MesProWorkOrderStatusEnum.FINISHED.getStatus())
                || workOrder.getFinishDate() != null || workOrder.getReleasedAt() != null
                || workOrder.getReleaseDecisionId() != null) {
            return "已有完工或放行记录，保留工单";
        }
        return cleanupMapper.selectRetainReason(tenantId, workOrder.getId());
    }

    private boolean hasIdentity(MesKingdeeProductionOrderSyncRecordDO record) {
        return record != null && record.getId() != null && StrUtil.isNotBlank(record.getSourceFid())
                && StrUtil.isNotBlank(record.getSourceBillNo()) && StrUtil.isNotBlank(record.getSourceMaterialNumber());
    }

    private boolean positive(BigDecimal quantity) { return quantity != null && quantity.signum() > 0; }

    private String environmentKey(ErpKingdeeProperties properties) {
        return DigestUtil.sha256Hex(properties.getBaseUrl() + "\n" + properties.getAcctId() + "\n" + properties.getUsername());
    }

    private RuntimeException blocked(String message) {
        return exception0(PRO_WORK_ORDER_ERP_CLEANUP_BLOCKED.getCode(), message);
    }
}
