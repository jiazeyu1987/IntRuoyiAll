package cn.iocoder.yudao.module.erp.service.purchase;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.number.MoneyUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductUnitDO;
import cn.iocoder.yudao.module.erp.service.product.ErpProductUnitService;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeOrderAmounts;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseOrderItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseOrderMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseReturnMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import cn.iocoder.yudao.module.erp.service.sync.ErpKingdeeOrderLineIdentity;
import cn.iocoder.yudao.module.erp.service.sync.ErpKingdeeOrderLineIdentity.Line;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseReturnItemMapper;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.*;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;

// TODO 芋艿：记录操作日志

/**
 * ERP 采购订单 Service 实现类
 *
 * @author 瑛泰源码
 */
@Service
@Validated
public class ErpPurchaseOrderServiceImpl implements ErpPurchaseOrderService {
    @Override
    public void lockOrder(Long id) {
        if (purchaseOrderMapper.selectByIdForUpdate(id) == null) throw exception(PURCHASE_ORDER_NOT_EXISTS);
    }


    @Resource
    private ErpPurchaseOrderMapper purchaseOrderMapper;
    @Resource
    private ErpPurchaseOrderItemMapper purchaseOrderItemMapper;
    @Resource
    private ErpPurchaseInItemMapper purchaseInItemMapper;
    @Resource
    private ErpPurchaseReturnItemMapper purchaseReturnItemMapper;
    @Resource
    private ErpPurchaseInMapper purchaseInMapper;
    @Resource
    private ErpPurchaseReturnMapper purchaseReturnMapper;

    @Resource
    private ErpNoRedisDAO noRedisDAO;

    @Resource
    private ErpProductService productService;
    @Resource
    private ErpProductUnitService productUnitService;
    @Resource
    private ErpSupplierService supplierService;
    @Resource
    private ErpAccountService accountService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPurchaseOrder(ErpPurchaseOrderSaveReqVO createReqVO) {
        // 1.1 校验订单项的有效性
        List<ErpPurchaseOrderItemDO> purchaseOrderItems = validatePurchaseOrderItems(createReqVO);
        // 1.2 校验供应商
        if (createReqVO.getKingdeeAmounts() == null || createReqVO.getSupplierId() != null) {
            supplierService.validateSupplier(createReqVO.getSupplierId());
        }
        // 1.3 校验结算账户
        if (createReqVO.getAccountId() != null) {
            accountService.validateAccount(createReqVO.getAccountId());
        }
        // 1.4 生成订单号，并校验唯一性
        String no = noRedisDAO.generate(ErpNoRedisDAO.PURCHASE_ORDER_NO_PREFIX);
        if (purchaseOrderMapper.selectByNo(no) != null) {
            throw exception(PURCHASE_ORDER_NO_EXISTS);
        }

        // 2.1 插入订单
        ErpPurchaseOrderDO purchaseOrder = BeanUtils.toBean(createReqVO, ErpPurchaseOrderDO.class, in -> in
                .setNo(no).setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setInCount(BigDecimal.ZERO).setReturnCount(BigDecimal.ZERO));
        calculateTotalPrice(purchaseOrder, purchaseOrderItems, createReqVO.getKingdeeAmounts());
        purchaseOrderMapper.insert(purchaseOrder);
        // 2.2 插入订单项
        purchaseOrderItems.forEach(o -> o.setOrderId(purchaseOrder.getId())
                .setInCount(BigDecimal.ZERO).setReturnCount(BigDecimal.ZERO));
        purchaseOrderItemMapper.insertBatch(purchaseOrderItems);
        return purchaseOrder.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePurchaseOrder(ErpPurchaseOrderSaveReqVO updateReqVO) {
        updateOrder(updateReqVO, null, List.of());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePurchaseOrderFromKingdee(ErpPurchaseOrderSaveReqVO req, Integer status,
            List<ErpPurchaseOrderSaveReqVO.Item> previousItems) {
        if (req.getKingdeeAmounts() == null || !(ErpAuditStatus.APPROVE.getStatus().equals(status)
                || ErpAuditStatus.PROCESS.getStatus().equals(status))) {
            throw new IllegalArgumentException("ERP同步缺少可信金额快照或审核状态无效");
        }
        updateOrder(req, status, previousItems);
    }

    private void updateOrder(ErpPurchaseOrderSaveReqVO updateReqVO, Integer sourceStatus,
            List<ErpPurchaseOrderSaveReqVO.Item> previousItems) {
        // 1.1 校验存在
        ErpPurchaseOrderDO purchaseOrder = validatePurchaseOrderExists(updateReqVO.getId());
        if (sourceStatus == null && ErpAuditStatus.APPROVE.getStatus().equals(purchaseOrder.getStatus())) {
            throw exception(PURCHASE_ORDER_UPDATE_FAIL_APPROVE, purchaseOrder.getNo());
        }
        // 1.2 校验供应商
        if (updateReqVO.getKingdeeAmounts() == null || updateReqVO.getSupplierId() != null) {
            supplierService.validateSupplier(updateReqVO.getSupplierId());
        }
        // 1.3 校验结算账户
        if (updateReqVO.getAccountId() != null) {
            accountService.validateAccount(updateReqVO.getAccountId());
        }
        // 1.4 校验订单项的有效性
        List<ErpPurchaseOrderItemDO> purchaseOrderItems = validatePurchaseOrderItems(updateReqVO);

        // 2.1 更新订单
        ErpPurchaseOrderDO updateObj = BeanUtils.toBean(updateReqVO, ErpPurchaseOrderDO.class);
        calculateTotalPrice(updateObj, purchaseOrderItems, updateReqVO.getKingdeeAmounts());
        if (sourceStatus != null) {
            reconcileSourceItems(purchaseOrder, updateReqVO, purchaseOrderItems, previousItems, sourceStatus, updateObj);
            updateObj.setStatus(sourceStatus);
        }
        if (sourceStatus == null) {
            purchaseOrderMapper.updateById(updateObj);
        } else if (purchaseOrderMapper.updateByIdAndStatus(updateReqVO.getId(), purchaseOrder.getStatus(), updateObj) != 1) {
            throw new IllegalStateException("ERP订单更新期间状态已变化，请重新同步");
        }
        if (updateReqVO.getKingdeeAmounts() != null && updateReqVO.getSupplierId() == null) {
            // MyBatis normally ignores null fields; an ERP source clearing its supplier must clear it locally.
            purchaseOrderMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ErpPurchaseOrderDO>()
                    .eq(ErpPurchaseOrderDO::getId, updateReqVO.getId()).set(ErpPurchaseOrderDO::getSupplierId, null));
        }
        // 2.2 更新订单项
        updatePurchaseOrderItemList(updateReqVO.getId(), purchaseOrderItems, sourceStatus != null);
    }


    private void reconcileSourceItems(ErpPurchaseOrderDO order, ErpPurchaseOrderSaveReqVO req,
            List<ErpPurchaseOrderItemDO> incoming, List<ErpPurchaseOrderSaveReqVO.Item> previous,
            Integer status, ErpPurchaseOrderDO update) {
        var stored = purchaseOrderItemMapper.selectListByOrderId(order.getId());
        var documents = purchaseInMapper.selectListByOrderId(order.getId());
        var returns = purchaseReturnMapper.selectListByOrderId(order.getId());
        var quantities = purchaseInItemMapper.selectOrderItemCountSumMapByInIds(
                documents.stream().map(o -> o.getId()).toList());
        var returnQuantities = purchaseReturnItemMapper.selectOrderItemCountSumMapByReturnIds(
                returns.stream().map(o -> o.getId()).toList());
        BigDecimal documentTotal = documents.stream().map(o -> requireDocumentCount(o.getTotalCount(), o.getNo()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal returnTotal = returns.stream().map(o -> requireDocumentCount(o.getTotalCount(), o.getNo()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (quantities.values().stream().anyMatch(Objects::isNull)
                || returnQuantities.values().stream().anyMatch(Objects::isNull)) {
            throw new IllegalStateException("关联出入库单明细数量为空：" + order.getNo());
        }
        if (documentTotal.compareTo(quantities.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add)) != 0
                || returnTotal.compareTo(returnQuantities.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add)) != 0) {
            throw new IllegalStateException("关联出入库单汇总与明细数量不一致：" + order.getNo());
        }
        var storedIds = stored.stream().map(o -> o.getId()).collect(java.util.stream.Collectors.toSet());
        if (!storedIds.containsAll(quantities.keySet()) || !storedIds.containsAll(returnQuantities.keySet())) {
            throw new IllegalStateException("关联出入库单指向不存在的订单明细，请核对订单=" + order.getNo());
        }
        // Unidentified legacy rows without downstream references are replaced from the source snapshot.
        // Only referenced rows require adoption of their old IDs; never guess between identical source lines.
        var ids = ErpKingdeeOrderLineIdentity.reconcile(stored.stream()
                .filter(o -> StrUtil.isNotBlank(o.getSourceEntryId()) || quantities.containsKey(o.getId())
                        || returnQuantities.containsKey(o.getId()))
                .map(o -> new Line(o.getId(), o.getSourceEntryId(), o.getProductId(), o.getProductUnitId(),
                        o.getProductPrice(), o.getCount(), o.getTaxPercent(), o.getRemark())).toList(),
                incoming.stream().map(o -> new Line(null, o.getSourceEntryId(), o.getProductId(), o.getProductUnitId(),
                        o.getProductPrice(), o.getCount(), o.getTaxPercent(), o.getRemark())).toList(),
                previous.stream().map(o -> new Line(null, o.getSourceEntryId(), o.getProductId(), o.getProductUnitId(),
                        o.getProductPrice(), o.getCount(), o.getTaxPercent(), o.getRemark())).toList());
        incoming.forEach(o -> o.setId(ids.get(o.getSourceEntryId())));
        boolean linked = !documents.isEmpty() || !returns.isEmpty();
        if (linked && (!Objects.equals(order.getSupplierId(), req.getSupplierId())
                || !ErpAuditStatus.APPROVE.getStatus().equals(status))) {
            throw new IllegalStateException("已有出入库关联的ERP订单不能更换往来单位或反审核：" + order.getNo());
        }
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal totalReturn = BigDecimal.ZERO;
        for (var old : stored) {
            BigDecimal count = ErpKingdeeOrderLineIdentity.quantity(quantities, old.getId(), old.getInCount());
            BigDecimal returned = ErpKingdeeOrderLineIdentity.quantity(returnQuantities, old.getId(), old.getReturnCount());
            if (returned.compareTo(count) > 0) throw new IllegalStateException("退货数量超过出入库数量，明细ID=" + old.getId());
            var next = incoming.stream().filter(o -> old.getId().equals(o.getId())).findFirst().orElse(null);
            boolean referenced = quantities.containsKey(old.getId()) || returnQuantities.containsKey(old.getId());
            if (referenced && (next == null || !Objects.equals(old.getProductId(), next.getProductId())
                    || !Objects.equals(old.getProductUnitId(), next.getProductUnitId()))) {
                throw new IllegalStateException("ERP更新不能删除或更换已关联出入库的物料/单位，明细ID=" + old.getId());
            }
            if (next != null) {
                if (next.getCount() == null ? referenced : next.getCount().compareTo(count) < 0) {
                    throw new IllegalStateException("ERP订单数量小于已出入库数量，明细ID=" + old.getId());
                }
                next.setInCount(count).setReturnCount(returned);
            }
            total = total.add(count);
            totalReturn = totalReturn.add(returned);
        }
        ErpKingdeeOrderLineIdentity.quantity(java.util.Map.of(order.getId(), total), order.getId(), order.getInCount());
        ErpKingdeeOrderLineIdentity.quantity(java.util.Map.of(order.getId(), totalReturn), order.getId(), order.getReturnCount());
        update.setInCount(total).setReturnCount(totalReturn);
    }

    private void calculateTotalPrice(ErpPurchaseOrderDO purchaseOrder, List<ErpPurchaseOrderItemDO> purchaseOrderItems, ErpKingdeeOrderAmounts source) {
        purchaseOrder.setTotalCount(getSumValue(purchaseOrderItems, ErpPurchaseOrderItemDO::getCount, BigDecimal::add));
        purchaseOrder.setTotalProductPrice(getSumValue(purchaseOrderItems, ErpPurchaseOrderItemDO::getTotalPrice, BigDecimal::add, BigDecimal.ZERO));
        purchaseOrder.setTotalTaxPrice(getSumValue(purchaseOrderItems, ErpPurchaseOrderItemDO::getTaxPrice, BigDecimal::add, BigDecimal.ZERO));
        purchaseOrder.setTotalPrice(purchaseOrder.getTotalProductPrice().add(purchaseOrder.getTotalTaxPrice()));
        if (source != null) {
            source.validateSourceSnapshot(purchaseOrderItems.size());
            purchaseOrder.setTotalProductPrice(source.getProductAmount());
            purchaseOrder.setTotalTaxPrice(source.getTaxAmount());
            purchaseOrder.setTotalPrice(source.getTotalAmount());
            purchaseOrder.setDiscountPrice(source.getDiscountAmount());
            purchaseOrder.setDiscountPercent(source.discountPercent());
            return;
        }
        // 计算优惠价格
        if (purchaseOrder.getDiscountPercent() == null) {
            purchaseOrder.setDiscountPercent(BigDecimal.ZERO);
        }
        purchaseOrder.setDiscountPrice(MoneyUtils.priceMultiplyPercent(purchaseOrder.getTotalPrice(), purchaseOrder.getDiscountPercent()));
        purchaseOrder.setTotalPrice(purchaseOrder.getTotalPrice().subtract(purchaseOrder.getDiscountPrice()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePurchaseOrderStatus(Long id, Integer status) {
        boolean approve = ErpAuditStatus.APPROVE.getStatus().equals(status);
        // 1.1 校验存在
        ErpPurchaseOrderDO purchaseOrder = validatePurchaseOrderExists(id);
        // 1.2 校验状态
        if (purchaseOrder.getStatus().equals(status)) {
            throw exception(approve ? PURCHASE_ORDER_APPROVE_FAIL : PURCHASE_ORDER_PROCESS_FAIL);
        }
        if (!approve) {
            // 历史订单的汇总可能未初始化，必须同时核对实际关联单据，不能把空值当成没有入库。
            boolean hasIn = purchaseInMapper.selectListByOrderId(id).stream().anyMatch(in ->
                    requireDocumentCount(in.getTotalCount(), in.getNo()).signum() > 0);
            if (hasIn || (purchaseOrder.getInCount() != null && purchaseOrder.getInCount().signum() > 0)) {
                throw exception(PURCHASE_ORDER_PROCESS_FAIL_EXISTS_IN);
            }
            boolean hasReturn = purchaseReturnMapper.selectListByOrderId(id).stream().anyMatch(returns ->
                    requireDocumentCount(returns.getTotalCount(), returns.getNo()).signum() > 0);
            if (hasReturn || (purchaseOrder.getReturnCount() != null && purchaseOrder.getReturnCount().signum() > 0)) {
                throw exception(PURCHASE_ORDER_PROCESS_FAIL_EXISTS_RETURN);
            }
        }

        // 2. 更新状态
        int updateCount = purchaseOrderMapper.updateByIdAndStatus(id, purchaseOrder.getStatus(),
                new ErpPurchaseOrderDO().setStatus(status));
        if (updateCount == 0) {
            throw exception(approve ? PURCHASE_ORDER_APPROVE_FAIL : PURCHASE_ORDER_PROCESS_FAIL);
        }
    }

    private BigDecimal requireDocumentCount(BigDecimal count, String no) {
        if (count == null) {
            throw new IllegalStateException("采购关联单据数量为空，无法核实是否允许反审核：" + no);
        }
        return count;
    }

    private List<ErpPurchaseOrderItemDO> validatePurchaseOrderItems(ErpPurchaseOrderSaveReqVO req) {
        List<ErpPurchaseOrderSaveReqVO.Item> list = req.getItems();
        if (req.getKingdeeAmounts() != null) {
            ErpKingdeeOrderLineIdentity.reconcile(List.of(), list.stream().map(o -> new Line(null,
                    o.getSourceEntryId(), o.getProductId(), o.getProductUnitId(), o.getProductPrice(),
                    o.getCount(), o.getTaxPercent(), o.getRemark())).toList(), List.of());
        }
        if (req.getKingdeeAmounts() != null) { req.getKingdeeAmounts().validateSourceSnapshot(list.size()); }
        java.util.Set<Long> unitIds = list.stream().map(ErpPurchaseOrderSaveReqVO.Item::getProductUnitId)
                .filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        // Source drafts may contain no selected unit; an empty IN query is invalid SQL.
        Map<Long, ErpProductUnitDO> units = unitIds.isEmpty()
                ? Collections.emptyMap() : productUnitService.getProductUnitMap(unitIds);
        for (ErpPurchaseOrderSaveReqVO.Item item : list) {
            if (req.getKingdeeAmounts() != null && item.getProductUnitId() == null) {
                continue;
            }
            if (item.getProductUnitId() == null || !units.containsKey(item.getProductUnitId())) {
                throw exception(PRODUCT_UNIT_NOT_EXISTS);
            }
        }
        // 1. 校验产品存在
        if (req.getKingdeeAmounts() == null) {
            productService.validProductList(convertSet(list, ErpPurchaseOrderSaveReqVO.Item::getProductId));
        } else {
            // ERP 历史快照可以引用已停用物料，导入不改变物料状态。
            for (Long productId : convertSet(list, ErpPurchaseOrderSaveReqVO.Item::getProductId)) {
                if (productId == null) {
                    continue; // Preserve an unselected material on a trusted ERP source line.
                }
                if (productService.getProduct(productId) == null) {
                    throw exception(PRODUCT_NOT_EXISTS);
                }
            }
        }
        // 2. 转化为 ErpPurchaseOrderItemDO 列表
        List<ErpPurchaseOrderItemDO> result = convertList(list, o -> BeanUtils.toBean(o, ErpPurchaseOrderItemDO.class, item -> {
            item.setProductUnitId(o.getProductUnitId());
            item.setTotalPrice(MoneyUtils.priceMultiply(item.getProductPrice(), item.getCount()));
            if (item.getTotalPrice() == null) {
                return;
            }
            if (item.getTaxPercent() != null) {
                item.setTaxPrice(MoneyUtils.priceMultiplyPercent(item.getTotalPrice(), item.getTaxPercent()));
            }
        }));
        if (req.getKingdeeAmounts() != null) {
            for (int i = 0; i < result.size(); i++) {
                var amount = req.getKingdeeAmounts().getLines().get(i);
                result.get(i).setTotalPrice(amount.getNetAmount());
                result.get(i).setTaxPrice(amount.getTaxAmount());
            }
        }
        return result;
    }

    private void updatePurchaseOrderItemList(Long id, List<ErpPurchaseOrderItemDO> newList, boolean sourceUpdate) {
        // 第一步，对比新老数据，获得添加、修改、删除的列表
        List<ErpPurchaseOrderItemDO> oldList = purchaseOrderItemMapper.selectListByOrderId(id);
        var ownedIds = oldList.stream().map(item -> item.getId()).collect(java.util.stream.Collectors.toSet());
        if (newList.stream().anyMatch(item -> item.getId() != null && !ownedIds.contains(item.getId()))) {
            throw new IllegalArgumentException("订单明细ID不属于当前订单");
        }
        List<List<ErpPurchaseOrderItemDO>> diffList = diffList(oldList, newList, // id 不同，就认为是不同的记录
                (oldVal, newVal) -> oldVal.getId().equals(newVal.getId()));

        // 第二步，批量添加、修改、删除
        if (CollUtil.isNotEmpty(diffList.get(0))) {
            diffList.get(0).forEach(o -> o.setOrderId(id)
                    .setInCount(BigDecimal.ZERO).setReturnCount(BigDecimal.ZERO));
            purchaseOrderItemMapper.insertBatch(diffList.get(0));
        }
        if (CollUtil.isNotEmpty(diffList.get(1))) {
            if (sourceUpdate) {
                for (var item : diffList.get(1)) {
                    if (purchaseOrderItemMapper.updateSourceItem(item, id) != 1) {
                        throw new IllegalStateException("ERP明细更新期间已变化，明细ID=" + item.getId());
                    }
                }
            } else {
                purchaseOrderItemMapper.updateBatch(diffList.get(1));
            }
        }
        if (CollUtil.isNotEmpty(diffList.get(2))) {
            purchaseOrderItemMapper.deleteByIds(convertList(diffList.get(2), ErpPurchaseOrderItemDO::getId));
        }
    }

    @Override
    public void updatePurchaseOrderInCount(Long id, Map<Long, BigDecimal> inCountMap) {
        List<ErpPurchaseOrderItemDO> orderItems = purchaseOrderItemMapper.selectListByOrderId(id);
        // 1. 更新每个采购订单项
        orderItems.forEach(item -> {
            BigDecimal inCount = inCountMap.getOrDefault(item.getId(), BigDecimal.ZERO);
            if (inCount.equals(item.getInCount())) {
                return;
            }
            if (inCount.compareTo(item.getCount()) > 0) {
                throw exception(PURCHASE_ORDER_ITEM_IN_FAIL_PRODUCT_EXCEED,
                        productService.getProduct(item.getProductId()).getName(), item.getCount());
            }
            purchaseOrderItemMapper.updateById(new ErpPurchaseOrderItemDO().setId(item.getId()).setInCount(inCount));
        });
        // 2. 更新采购订单
        BigDecimal totalInCount = getSumValue(inCountMap.values(), value -> value, BigDecimal::add, BigDecimal.ZERO);
        purchaseOrderMapper.updateById(new ErpPurchaseOrderDO().setId(id).setInCount(totalInCount));
    }

    @Override
    public void updatePurchaseOrderReturnCount(Long orderId, Map<Long, BigDecimal> returnCountMap) {
        List<ErpPurchaseOrderItemDO> orderItems = purchaseOrderItemMapper.selectListByOrderId(orderId);
        // 1. 更新每个采购订单项
        orderItems.forEach(item -> {
            BigDecimal returnCount = returnCountMap.getOrDefault(item.getId(), BigDecimal.ZERO);
            if (returnCount.equals(item.getReturnCount())) {
                return;
            }
            if (item.getInCount() == null) {
                throw new IllegalStateException("订单出入库数量为空，请先核对关联单据，明细ID=" + item.getId());
            }
            if (returnCount.compareTo(item.getInCount()) > 0) {
                throw exception(PURCHASE_ORDER_ITEM_RETURN_FAIL_IN_EXCEED,
                        productService.getProduct(item.getProductId()).getName(), item.getInCount());
            }
            purchaseOrderItemMapper.updateById(new ErpPurchaseOrderItemDO().setId(item.getId()).setReturnCount(returnCount));
        });
        // 2. 更新采购订单
        BigDecimal totalReturnCount = getSumValue(returnCountMap.values(), value -> value, BigDecimal::add, BigDecimal.ZERO);
        purchaseOrderMapper.updateById(new ErpPurchaseOrderDO().setId(orderId).setReturnCount(totalReturnCount));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePurchaseOrder(List<Long> ids) {
        // 1. 校验不处于已审批
        List<ErpPurchaseOrderDO> purchaseOrders = ids.stream().distinct().sorted()
                .map(this::validatePurchaseOrderExists).toList();
        if (CollUtil.isEmpty(purchaseOrders)) {
            return;
        }
        purchaseOrders.forEach(purchaseOrder -> {
            if (ErpAuditStatus.APPROVE.getStatus().equals(purchaseOrder.getStatus())) {
                throw exception(PURCHASE_ORDER_DELETE_FAIL_APPROVE, purchaseOrder.getNo());
            }
        });

        // 2. 遍历删除，并记录操作日志
        purchaseOrders.forEach(purchaseOrder -> {
            // 2.1 删除订单
            purchaseOrderMapper.deleteById(purchaseOrder.getId());
            // 2.2 删除订单项
            purchaseOrderItemMapper.deleteByOrderId(purchaseOrder.getId());
        });
    }

    private ErpPurchaseOrderDO validatePurchaseOrderExists(Long id) {
        ErpPurchaseOrderDO purchaseOrder = org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()
                ? purchaseOrderMapper.selectByIdForUpdate(id) : purchaseOrderMapper.selectById(id);
        if (purchaseOrder == null) {
            throw exception(PURCHASE_ORDER_NOT_EXISTS);
        }
        return purchaseOrder;
    }

    @Override
    public ErpPurchaseOrderDO getPurchaseOrder(Long id) {
        return purchaseOrderMapper.selectById(id);
    }

    @Override
    public ErpPurchaseOrderDO validatePurchaseOrder(Long id) {
        ErpPurchaseOrderDO purchaseOrder = validatePurchaseOrderExists(id);
        if (ObjectUtil.notEqual(purchaseOrder.getStatus(), ErpAuditStatus.APPROVE.getStatus())) {
            throw exception(PURCHASE_ORDER_NOT_APPROVE);
        }
        return purchaseOrder;
    }

    @Override
    public PageResult<ErpPurchaseOrderDO> getPurchaseOrderPage(ErpPurchaseOrderPageReqVO pageReqVO) {
        return purchaseOrderMapper.selectPage(pageReqVO);
    }

    // ==================== 订单项 ====================

    @Override
    public List<ErpPurchaseOrderItemDO> getPurchaseOrderItemListByOrderId(Long orderId) {
        return purchaseOrderItemMapper.selectListByOrderId(orderId);
    }

    @Override
    public List<ErpPurchaseOrderItemDO> getPurchaseOrderItemListByOrderIds(Collection<Long> orderIds) {
        if (CollUtil.isEmpty(orderIds)) {
            return Collections.emptyList();
        }
        return purchaseOrderItemMapper.selectListByOrderIds(orderIds);
    }

}
