package cn.iocoder.yudao.module.srm.service.purchaseorder;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.srm.controller.admin.procurementplan.vo.SrmProcurementPlanRespVO;
import cn.iocoder.yudao.module.srm.controller.admin.purchaseorder.vo.SrmPurchaseOrderChangeReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.purchaseorder.vo.SrmPurchaseOrderConfirmReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.purchaseorder.vo.SrmPurchaseOrderCreateReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.purchaseorder.vo.SrmPurchaseOrderPageReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.purchaseorder.vo.SrmPurchaseOrderRejectChangeReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.purchaseorder.vo.SrmPurchaseOrderRespVO;
import cn.iocoder.yudao.module.srm.controller.admin.purchaseorder.vo.SrmPurchaseOrderWithdrawChangeReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.supplierportal.vo.SrmSupplierPortalApplicationRespVO;
import cn.iocoder.yudao.module.srm.dal.dataobject.procurement.SrmProcurementPlanLineDO;
import cn.iocoder.yudao.module.srm.dal.dataobject.purchaseorder.SrmPurchaseOrderChangeDO;
import cn.iocoder.yudao.module.srm.dal.dataobject.purchaseorder.SrmPurchaseOrderChangeLineDO;
import cn.iocoder.yudao.module.srm.dal.dataobject.purchaseorder.SrmPurchaseOrderDO;
import cn.iocoder.yudao.module.srm.dal.dataobject.purchaseorder.SrmPurchaseOrderLineDO;
import cn.iocoder.yudao.module.srm.dal.mysql.procurement.SrmProcurementPlanLineMapper;
import cn.iocoder.yudao.module.srm.dal.mysql.purchaseorder.SrmPurchaseOrderChangeLineMapper;
import cn.iocoder.yudao.module.srm.dal.mysql.purchaseorder.SrmPurchaseOrderChangeMapper;
import cn.iocoder.yudao.module.srm.dal.mysql.purchaseorder.SrmPurchaseOrderLineMapper;
import cn.iocoder.yudao.module.srm.dal.mysql.purchaseorder.SrmPurchaseOrderMapper;
import cn.iocoder.yudao.module.srm.enums.coderule.SrmCodeRuleTargetFormEnum;
import cn.iocoder.yudao.module.srm.enums.procurement.SrmProcurementPlanStatusEnum;
import cn.iocoder.yudao.module.srm.enums.procurement.SrmPurchaseOrderChangeStatusEnum;
import cn.iocoder.yudao.module.srm.enums.procurement.SrmPurchaseOrderStatusEnum;
import cn.iocoder.yudao.module.srm.service.coderule.SrmCodeRuleService;
import cn.iocoder.yudao.module.srm.service.procurement.SrmProcurementPlanService;
import cn.iocoder.yudao.module.srm.service.supplier.SrmSupplierAccessRiskService;
import cn.iocoder.yudao.module.srm.service.supplier.SrmSupplierPortalApplicationService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.srm.enums.ErrorCodeConstants.*;

@Service
@Validated
public class SrmPurchaseOrderServiceImpl implements SrmPurchaseOrderService {

    @Resource
    private SrmCodeRuleService codeRuleService;
    @Resource
    private SrmProcurementPlanService procurementPlanService;
    @Resource
    private SrmProcurementPlanLineMapper procurementPlanLineMapper;
    @Resource
    private SrmPurchaseOrderMapper purchaseOrderMapper;
    @Resource
    private SrmPurchaseOrderChangeMapper purchaseOrderChangeMapper;
    @Resource
    private SrmPurchaseOrderChangeLineMapper purchaseOrderChangeLineMapper;
    @Resource
    private SrmPurchaseOrderLineMapper purchaseOrderLineMapper;
    @Resource
    private SrmSupplierAccessRiskService supplierAccessRiskService;
    @Resource
    private SrmSupplierPortalApplicationService supplierPortalApplicationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createFromPlan(SrmPurchaseOrderCreateReqVO createReqVO) {
        if (createReqVO.getSupplierId() == null) {
            throw exception(PURCHASE_ORDER_SUPPLIER_REQUIRED);
        }
        SrmProcurementPlanRespVO plan = procurementPlanService.getProcurementPlan(createReqVO.getSourcePlanId());
        if (!Objects.equals(plan.getPlanStatus(), SrmProcurementPlanStatusEnum.APPROVED.getStatus())
                && !Objects.equals(plan.getPlanStatus(), SrmProcurementPlanStatusEnum.GENERATED.getStatus())) {
            throw exception(PURCHASE_ORDER_SOURCE_PLAN_NOT_APPROVED);
        }
        supplierAccessRiskService.validateSupplierEligible(createReqVO.getSupplierId());
        if (purchaseOrderMapper.selectBySourcePlanIdAndSupplierId(getRequiredTenantId(),
                createReqVO.getSourcePlanId(), createReqVO.getSupplierId()) != null) {
            throw exception(PURCHASE_ORDER_DUPLICATE);
        }

        SrmPurchaseOrderDO order = SrmPurchaseOrderDO.builder()
                .orderNo(codeRuleService.generateCode(SrmCodeRuleTargetFormEnum.PURCHASE_ORDER.getTargetForm()))
                .sourcePlanId(plan.getId())
                .sourcePlanNo(plan.getPlanNo())
                .supplierId(createReqVO.getSupplierId())
                .supplierName(resolveSupplierName(createReqVO.getSupplierId()))
                .orderStatus(SrmPurchaseOrderStatusEnum.PENDING_CONFIRM.getStatus())
                .orderRemark(createReqVO.getOrderRemark())
                .build();
        order.setTenantId(getRequiredTenantId());
        purchaseOrderMapper.insert(order);

        for (SrmProcurementPlanLineDO sourceLine : procurementPlanLineMapper.selectListByPlanId(plan.getId())) {
            SrmPurchaseOrderLineDO line = SrmPurchaseOrderLineDO.builder()
                    .orderId(order.getId())
                    .lineNo(codeRuleService.generateCode(SrmCodeRuleTargetFormEnum.PURCHASE_ORDER_LINE.getTargetForm()))
                    .sourcePlanLineId(sourceLine.getId())
                    .materialId(sourceLine.getMaterialId())
                    .materialCode(sourceLine.getMaterialCode())
                    .materialName(sourceLine.getMaterialName())
                    .requestedQuantity(sourceLine.getQuantity())
                    .unit(sourceLine.getUnit())
                    .requestedDeliveryDate(sourceLine.getRequiredDate())
                    .build();
            line.setTenantId(getRequiredTenantId());
            purchaseOrderLineMapper.insert(line);
        }
        return order.getId();
    }

    @Override
    public SrmPurchaseOrderRespVO getPurchaseOrder(Long id) {
        return buildPurchaseOrderResp(validateOrder(id));
    }

    @Override
    public PageResult<SrmPurchaseOrderRespVO> getPurchaseOrderPage(SrmPurchaseOrderPageReqVO pageReqVO) {
        PageResult<SrmPurchaseOrderDO> pageResult = purchaseOrderMapper.selectPage(pageReqVO);
        return new PageResult<>(pageResult.getList().stream().map(this::buildPurchaseOrderResp).toList(), pageResult.getTotal());
    }

    @Override
    public PageResult<SrmPurchaseOrderRespVO> getMyPurchaseOrderPage(SrmPurchaseOrderPageReqVO pageReqVO) {
        Long supplierId = getRequiredCurrentSupplierId();
        PageResult<SrmPurchaseOrderDO> pageResult = purchaseOrderMapper.selectMyPage(getRequiredTenantId(), supplierId, pageReqVO);
        return new PageResult<>(pageResult.getList().stream().map(this::buildPurchaseOrderResp).toList(), pageResult.getTotal());
    }

    @Override
    public SrmPurchaseOrderRespVO getMyPurchaseOrder(Long id) {
        SrmPurchaseOrderDO order = validateOrder(id);
        if (!Objects.equals(order.getSupplierId(), getRequiredCurrentSupplierId())) {
            throw exception(PURCHASE_ORDER_CONFIRM_FORBIDDEN);
        }
        return buildPurchaseOrderResp(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmMyPurchaseOrder(SrmPurchaseOrderConfirmReqVO reqVO) {
        SrmPurchaseOrderDO order = validateOrder(reqVO.getId());
        Long currentSupplierId = getRequiredCurrentSupplierId();
        if (!Objects.equals(order.getSupplierId(), currentSupplierId)) {
            throw exception(PURCHASE_ORDER_CONFIRM_FORBIDDEN);
        }
        if (!Objects.equals(order.getOrderStatus(), SrmPurchaseOrderStatusEnum.PENDING_CONFIRM.getStatus())
                && !Objects.equals(order.getOrderStatus(), SrmPurchaseOrderStatusEnum.CHANGE_PENDING.getStatus())) {
            throw exception(PURCHASE_ORDER_STATUS_INVALID, SrmPurchaseOrderStatusEnum.getLabel(order.getOrderStatus()));
        }
        if (reqVO.getLines() == null || reqVO.getLines().isEmpty()) {
            throw exception(PURCHASE_ORDER_CONFIRM_LINE_REQUIRED);
        }

        List<SrmPurchaseOrderLineDO> currentLines = purchaseOrderLineMapper.selectListByOrderId(order.getId());
        SrmPurchaseOrderChangeDO pendingChange = null;
        if (Objects.equals(order.getOrderStatus(), SrmPurchaseOrderStatusEnum.CHANGE_PENDING.getStatus())) {
            pendingChange = validateLatestPendingChange(order.getId());
        }
        for (SrmPurchaseOrderConfirmReqVO.Line confirmLine : reqVO.getLines()) {
            SrmPurchaseOrderLineDO line = currentLines.stream()
                    .filter(item -> Objects.equals(item.getId(), confirmLine.getOrderLineId()))
                    .findFirst()
                    .orElseThrow(() -> exception(PURCHASE_ORDER_CONFIRM_LINE_INVALID));
            if (confirmLine.getConfirmedQuantity() == null || confirmLine.getConfirmedQuantity().compareTo(BigDecimal.ZERO) <= 0
                    || confirmLine.getConfirmedDeliveryDate() == null) {
                throw exception(PURCHASE_ORDER_CONFIRM_LINE_INVALID);
            }
            line.setConfirmedQuantity(confirmLine.getConfirmedQuantity());
            line.setConfirmedDeliveryDate(confirmLine.getConfirmedDeliveryDate());
            line.setSupplierRemark(confirmLine.getSupplierRemark());
            line.setPendingChangedQuantity(null);
            line.setPendingChangedDeliveryDate(null);
            line.setPendingChangedRemark(null);
            purchaseOrderLineMapper.updateById(line);
        }
        order.setOrderStatus(SrmPurchaseOrderStatusEnum.CONFIRMED.getStatus());
        order.setConfirmedBy(getRequiredLoginUserId());
        order.setConfirmedName(getRequiredLoginUserNickname());
        order.setConfirmedTime(LocalDateTime.now());
        order.setConfirmRemark(reqVO.getConfirmRemark());
        purchaseOrderMapper.updateById(order);

        if (pendingChange != null) {
            pendingChange.setChangeStatus(SrmPurchaseOrderChangeStatusEnum.CONFIRMED.getStatus());
            pendingChange.setConfirmedBy(getRequiredLoginUserId());
            pendingChange.setConfirmedName(getRequiredLoginUserNickname());
            pendingChange.setConfirmedTime(LocalDateTime.now());
            pendingChange.setConfirmRemark(reqVO.getConfirmRemark());
            purchaseOrderChangeMapper.updateById(pendingChange);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submitOrderChange(SrmPurchaseOrderChangeReqVO reqVO) {
        if (StrUtil.isBlank(reqVO.getChangeReason())) {
            throw exception(PURCHASE_ORDER_CHANGE_REMARK_REQUIRED);
        }
        if (reqVO.getLines() == null || reqVO.getLines().isEmpty()) {
            throw exception(PURCHASE_ORDER_CHANGE_LINE_REQUIRED);
        }
        SrmPurchaseOrderDO order = validateOrder(reqVO.getOrderId());
        if (!Objects.equals(order.getOrderStatus(), SrmPurchaseOrderStatusEnum.CONFIRMED.getStatus())) {
            throw exception(PURCHASE_ORDER_STATUS_INVALID, SrmPurchaseOrderStatusEnum.getLabel(order.getOrderStatus()));
        }
        if (purchaseOrderChangeMapper.selectLatestPendingByOrderId(getRequiredTenantId(), order.getId()) != null) {
            throw exception(PURCHASE_ORDER_STATUS_INVALID, SrmPurchaseOrderStatusEnum.CHANGE_PENDING.getLabel());
        }
        List<SrmPurchaseOrderLineDO> orderLines = purchaseOrderLineMapper.selectListByOrderId(order.getId());
        SrmPurchaseOrderChangeDO change = SrmPurchaseOrderChangeDO.builder()
                .changeNo(codeRuleService.generateCode(SrmCodeRuleTargetFormEnum.PURCHASE_ORDER_CHANGE.getTargetForm()))
                .orderId(order.getId())
                .orderNo(order.getOrderNo())
                .supplierId(order.getSupplierId())
                .supplierName(order.getSupplierName())
                .changeStatus(SrmPurchaseOrderChangeStatusEnum.PENDING_CONFIRM.getStatus())
                .changeReason(reqVO.getChangeReason())
                .changeRemark(reqVO.getChangeRemark())
                .submittedBy(getRequiredLoginUserId())
                .submittedName(getRequiredLoginUserNickname())
                .submittedTime(LocalDateTime.now())
                .build();
        change.setTenantId(getRequiredTenantId());
        purchaseOrderChangeMapper.insert(change);

        for (SrmPurchaseOrderChangeReqVO.Line reqLine : reqVO.getLines()) {
            SrmPurchaseOrderLineDO orderLine = orderLines.stream()
                    .filter(item -> Objects.equals(item.getId(), reqLine.getOrderLineId()))
                    .findFirst()
                    .orElseThrow(() -> exception(PURCHASE_ORDER_CHANGE_LINE_INVALID));
            if (reqLine.getChangedQuantity() == null || reqLine.getChangedQuantity().compareTo(BigDecimal.ZERO) <= 0
                    || reqLine.getChangedDeliveryDate() == null
                    || orderLine.getConfirmedQuantity() == null
                    || orderLine.getConfirmedDeliveryDate() == null) {
                throw exception(PURCHASE_ORDER_CHANGE_LINE_INVALID);
            }
            purchaseOrderChangeLineMapper.insert(buildChangeLine(change.getId(), orderLine, reqLine));
            orderLine.setPendingChangedQuantity(reqLine.getChangedQuantity());
            orderLine.setPendingChangedDeliveryDate(reqLine.getChangedDeliveryDate());
            orderLine.setPendingChangedRemark(reqLine.getChangedSupplierRemark());
            purchaseOrderLineMapper.updateById(orderLine);
        }
        order.setOrderStatus(SrmPurchaseOrderStatusEnum.CHANGE_PENDING.getStatus());
        purchaseOrderMapper.updateById(order);
        return change.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectMyPurchaseOrderChange(SrmPurchaseOrderRejectChangeReqVO reqVO) {
        if (StrUtil.isBlank(reqVO.getRejectRemark())) {
            throw exception(PURCHASE_ORDER_CHANGE_REJECT_REMARK_REQUIRED);
        }
        SrmPurchaseOrderChangeDO change = validateChange(reqVO.getChangeId());
        SrmPurchaseOrderDO order = validateOrder(change.getOrderId());
        if (!Objects.equals(order.getSupplierId(), getRequiredCurrentSupplierId())) {
            throw exception(PURCHASE_ORDER_CONFIRM_FORBIDDEN);
        }
        if (!Objects.equals(change.getChangeStatus(), SrmPurchaseOrderChangeStatusEnum.PENDING_CONFIRM.getStatus())) {
            throw exception(PURCHASE_ORDER_STATUS_INVALID, SrmPurchaseOrderChangeStatusEnum.getLabel(change.getChangeStatus()));
        }
        clearPendingChangeFromOrder(order);
        change.setChangeStatus(SrmPurchaseOrderChangeStatusEnum.REJECTED.getStatus());
        change.setRejectedBy(getRequiredLoginUserId());
        change.setRejectedName(getRequiredLoginUserNickname());
        change.setRejectedTime(LocalDateTime.now());
        change.setRejectRemark(reqVO.getRejectRemark());
        purchaseOrderChangeMapper.updateById(change);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdrawOrderChange(SrmPurchaseOrderWithdrawChangeReqVO reqVO) {
        if (StrUtil.isBlank(reqVO.getWithdrawRemark())) {
            throw exception(PURCHASE_ORDER_CHANGE_WITHDRAW_REMARK_REQUIRED);
        }
        SrmPurchaseOrderChangeDO change = validateChange(reqVO.getChangeId());
        if (!Objects.equals(change.getChangeStatus(), SrmPurchaseOrderChangeStatusEnum.PENDING_CONFIRM.getStatus())) {
            throw exception(PURCHASE_ORDER_STATUS_INVALID, SrmPurchaseOrderChangeStatusEnum.getLabel(change.getChangeStatus()));
        }
        SrmPurchaseOrderDO order = validateOrder(change.getOrderId());
        clearPendingChangeFromOrder(order);
        change.setChangeStatus(SrmPurchaseOrderChangeStatusEnum.WITHDRAWN.getStatus());
        change.setWithdrawnBy(getRequiredLoginUserId());
        change.setWithdrawnName(getRequiredLoginUserNickname());
        change.setWithdrawnTime(LocalDateTime.now());
        change.setWithdrawRemark(reqVO.getWithdrawRemark());
        purchaseOrderChangeMapper.updateById(change);
    }

    private SrmPurchaseOrderDO validateOrder(Long id) {
        SrmPurchaseOrderDO order = purchaseOrderMapper.selectById(id);
        if (order == null || !Objects.equals(order.getTenantId(), getRequiredTenantId())) {
            throw exception(PURCHASE_ORDER_NOT_EXISTS);
        }
        return order;
    }

    private SrmPurchaseOrderRespVO buildPurchaseOrderResp(SrmPurchaseOrderDO order) {
        SrmPurchaseOrderRespVO respVO = new SrmPurchaseOrderRespVO();
        respVO.setId(order.getId());
        respVO.setOrderNo(order.getOrderNo());
        respVO.setSourcePlanId(order.getSourcePlanId());
        respVO.setSourcePlanNo(order.getSourcePlanNo());
        respVO.setSupplierId(order.getSupplierId());
        respVO.setSupplierName(order.getSupplierName());
        respVO.setOrderStatus(order.getOrderStatus());
        respVO.setOrderStatusLabel(SrmPurchaseOrderStatusEnum.getLabel(order.getOrderStatus()));
        respVO.setOrderRemark(order.getOrderRemark());
        respVO.setConfirmedBy(order.getConfirmedBy());
        respVO.setConfirmedName(order.getConfirmedName());
        respVO.setConfirmedTime(order.getConfirmedTime());
        respVO.setConfirmRemark(order.getConfirmRemark());
        respVO.setCreateTime(order.getCreateTime());
        SrmPurchaseOrderChangeDO latestChange = purchaseOrderChangeMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SrmPurchaseOrderChangeDO>()
                .eq(SrmPurchaseOrderChangeDO::getTenantId, getRequiredTenantId())
                .eq(SrmPurchaseOrderChangeDO::getOrderId, order.getId())
                .orderByDesc(SrmPurchaseOrderChangeDO::getId)
                .last("LIMIT 1"));
        if (latestChange != null) {
            SrmPurchaseOrderRespVO.Change change = new SrmPurchaseOrderRespVO.Change();
            change.setId(latestChange.getId());
            change.setChangeNo(latestChange.getChangeNo());
            change.setChangeStatus(latestChange.getChangeStatus());
            change.setChangeStatusLabel(SrmPurchaseOrderChangeStatusEnum.getLabel(latestChange.getChangeStatus()));
            change.setChangeReason(latestChange.getChangeReason());
            change.setChangeRemark(latestChange.getChangeRemark());
            change.setConfirmRemark(latestChange.getConfirmRemark());
            change.setRejectRemark(latestChange.getRejectRemark());
            change.setWithdrawRemark(latestChange.getWithdrawRemark());
            change.setSubmittedTime(latestChange.getSubmittedTime());
            change.setConfirmedTime(latestChange.getConfirmedTime());
            change.setRejectedTime(latestChange.getRejectedTime());
            change.setWithdrawnTime(latestChange.getWithdrawnTime());
            respVO.setLatestChange(change);
        }
        respVO.setLines(purchaseOrderLineMapper.selectListByOrderId(order.getId()).stream()
                .map(this::buildLineResp)
                .collect(Collectors.toList()));
        return respVO;
    }

    private SrmPurchaseOrderRespVO.Line buildLineResp(SrmPurchaseOrderLineDO line) {
        SrmPurchaseOrderRespVO.Line respVO = new SrmPurchaseOrderRespVO.Line();
        respVO.setId(line.getId());
        respVO.setLineNo(line.getLineNo());
        respVO.setSourcePlanLineId(line.getSourcePlanLineId());
        respVO.setMaterialId(line.getMaterialId());
        respVO.setMaterialCode(line.getMaterialCode());
        respVO.setMaterialName(line.getMaterialName());
        respVO.setRequestedQuantity(line.getRequestedQuantity());
        respVO.setUnit(line.getUnit());
        respVO.setRequestedDeliveryDate(line.getRequestedDeliveryDate());
        respVO.setConfirmedQuantity(line.getConfirmedQuantity());
        respVO.setConfirmedDeliveryDate(line.getConfirmedDeliveryDate());
        respVO.setSupplierRemark(line.getSupplierRemark());
        respVO.setPendingChangedQuantity(line.getPendingChangedQuantity());
        respVO.setPendingChangedDeliveryDate(line.getPendingChangedDeliveryDate());
        respVO.setPendingChangedRemark(line.getPendingChangedRemark());
        return respVO;
    }

    private SrmPurchaseOrderChangeLineDO buildChangeLine(Long changeId, SrmPurchaseOrderLineDO orderLine,
                                                         SrmPurchaseOrderChangeReqVO.Line reqLine) {
        SrmPurchaseOrderChangeLineDO line = SrmPurchaseOrderChangeLineDO.builder()
                .changeId(changeId)
                .orderLineId(orderLine.getId())
                .materialId(orderLine.getMaterialId())
                .materialCode(orderLine.getMaterialCode())
                .materialName(orderLine.getMaterialName())
                .beforeQuantity(orderLine.getConfirmedQuantity())
                .beforeDeliveryDate(orderLine.getConfirmedDeliveryDate())
                .beforeSupplierRemark(orderLine.getSupplierRemark())
                .changedQuantity(reqLine.getChangedQuantity())
                .changedDeliveryDate(reqLine.getChangedDeliveryDate())
                .changedSupplierRemark(reqLine.getChangedSupplierRemark())
                .build();
        line.setTenantId(getRequiredTenantId());
        return line;
    }

    private void clearPendingChangeFromOrder(SrmPurchaseOrderDO order) {
        purchaseOrderLineMapper.clearPendingChangeFieldsByOrderId(order.getId());
        order.setOrderStatus(SrmPurchaseOrderStatusEnum.CONFIRMED.getStatus());
        purchaseOrderMapper.updateById(order);
    }

    private SrmPurchaseOrderChangeDO validateChange(Long changeId) {
        SrmPurchaseOrderChangeDO change = purchaseOrderChangeMapper.selectById(changeId);
        if (change == null || !Objects.equals(change.getTenantId(), getRequiredTenantId())) {
            throw exception(PURCHASE_ORDER_CHANGE_NOT_EXISTS);
        }
        return change;
    }

    private SrmPurchaseOrderChangeDO validateLatestPendingChange(Long orderId) {
        SrmPurchaseOrderChangeDO change = purchaseOrderChangeMapper.selectLatestPendingByOrderId(getRequiredTenantId(), orderId);
        if (change == null) {
            throw exception(PURCHASE_ORDER_CHANGE_NOT_EXISTS);
        }
        return change;
    }

    private Long getRequiredCurrentSupplierId() {
        SrmSupplierPortalApplicationRespVO currentApplication = supplierPortalApplicationService.getCurrentApplication();
        if (currentApplication == null || currentApplication.getSupplierId() == null) {
            throw exception(PURCHASE_ORDER_SUPPLIER_CONTEXT_MISSING);
        }
        return currentApplication.getSupplierId();
    }

    private String resolveSupplierName(Long supplierId) {
        return supplierAccessRiskService.getSupplierProfile(supplierId).getSupplierName();
    }

    private Long getRequiredLoginUserId() {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId == null) {
            throw exception(PURCHASE_ORDER_SUPPLIER_CONTEXT_MISSING);
        }
        return userId;
    }

    private String getRequiredLoginUserNickname() {
        String nickname = SecurityFrameworkUtils.getLoginUserNickname();
        if (StrUtil.isBlank(nickname)) {
            throw exception(PURCHASE_ORDER_SUPPLIER_CONTEXT_MISSING);
        }
        return nickname;
    }

    private Long getRequiredTenantId() {
        return TenantContextHolder.getRequiredTenantId();
    }
}
