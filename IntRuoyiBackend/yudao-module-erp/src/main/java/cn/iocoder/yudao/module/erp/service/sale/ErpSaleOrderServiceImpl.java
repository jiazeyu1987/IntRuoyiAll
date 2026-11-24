package cn.iocoder.yudao.module.erp.service.sale;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.number.MoneyUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.order.ErpSaleOrderPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.order.ErpSaleOrderSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductUnitDO;
import cn.iocoder.yudao.module.erp.service.product.ErpProductUnitService;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeOrderAmounts;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOrderItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOrderItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOrderMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.service.finance.ErpAccountService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import cn.iocoder.yudao.module.erp.service.sync.ErpKingdeeOrderLineIdentity;
import cn.iocoder.yudao.module.erp.service.sync.ErpKingdeeOrderLineIdentity.Line;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleReturnItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleReturnMapper;

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
 * ERP 销售订单 Service 实现类
 *
 * @author 瑛泰源码
 */
@Service
@Validated
public class ErpSaleOrderServiceImpl implements ErpSaleOrderService {
    @Override
    public void lockOrder(Long id) {
        if (saleOrderMapper.selectByIdForUpdate(id) == null) throw exception(SALE_ORDER_NOT_EXISTS);
    }


    @Resource
    private ErpSaleOrderMapper saleOrderMapper;
    @Resource
    private ErpSaleOrderItemMapper saleOrderItemMapper;
    @Resource
    private ErpSaleOutItemMapper saleOutItemMapper;
    @Resource
    private ErpSaleReturnItemMapper saleReturnItemMapper;
    @Resource
    private ErpSaleOutMapper saleOutMapper;
    @Resource
    private ErpSaleReturnMapper saleReturnMapper;

    @Resource
    private ErpNoRedisDAO noRedisDAO;

    @Resource
    private ErpProductService productService;
    @Resource
    private ErpProductUnitService productUnitService;
    @Resource
    private ErpCustomerService customerService;
    @Resource
    private ErpAccountService accountService;

    @Resource
    private AdminUserApi adminUserApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSaleOrder(ErpSaleOrderSaveReqVO createReqVO) {
        // 1.1 校验订单项的有效性
        List<ErpSaleOrderItemDO> saleOrderItems = validateSaleOrderItems(createReqVO);
        // 1.2 校验客户
        customerService.validateCustomer(createReqVO.getCustomerId());
        // 1.3 校验结算账户
        if (createReqVO.getAccountId() != null) {
            accountService.validateAccount(createReqVO.getAccountId());
        }
        // 1.4 校验销售人员
        if (createReqVO.getSaleUserId() != null) {
            adminUserApi.validateUser(createReqVO.getSaleUserId());
        }
        // 1.5 生成订单号，并校验唯一性
        String no = noRedisDAO.generate(ErpNoRedisDAO.SALE_ORDER_NO_PREFIX);
        if (saleOrderMapper.selectByNo(no) != null) {
            throw exception(SALE_ORDER_NO_EXISTS);
        }

        // 2.1 插入订单
        ErpSaleOrderDO saleOrder = BeanUtils.toBean(createReqVO, ErpSaleOrderDO.class, in -> in
                .setNo(no).setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setOutCount(BigDecimal.ZERO).setReturnCount(BigDecimal.ZERO));
        calculateTotalPrice(saleOrder, saleOrderItems, createReqVO.getKingdeeAmounts());
        saleOrderMapper.insert(saleOrder);
        // 2.2 插入订单项
        saleOrderItems.forEach(o -> o.setOrderId(saleOrder.getId()).setOutCount(BigDecimal.ZERO).setReturnCount(BigDecimal.ZERO));
        saleOrderItemMapper.insertBatch(saleOrderItems);
        return saleOrder.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSaleOrder(ErpSaleOrderSaveReqVO updateReqVO) {
        updateOrder(updateReqVO, null, List.of());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSaleOrderFromKingdee(ErpSaleOrderSaveReqVO req, Integer status,
            List<ErpSaleOrderSaveReqVO.Item> previousItems) {
        if (req.getKingdeeAmounts() == null || !(ErpAuditStatus.APPROVE.getStatus().equals(status)
                || ErpAuditStatus.PROCESS.getStatus().equals(status))) {
            throw new IllegalArgumentException("ERP同步缺少可信金额快照或审核状态无效");
        }
        updateOrder(req, status, previousItems);
    }

    private void updateOrder(ErpSaleOrderSaveReqVO updateReqVO, Integer sourceStatus,
            List<ErpSaleOrderSaveReqVO.Item> previousItems) {
        // 1.1 校验存在
        ErpSaleOrderDO saleOrder = validateSaleOrderExists(updateReqVO.getId());
        if (sourceStatus == null && ErpAuditStatus.APPROVE.getStatus().equals(saleOrder.getStatus())) {
            throw exception(SALE_ORDER_UPDATE_FAIL_APPROVE, saleOrder.getNo());
        }
        // 1.2 校验客户
        customerService.validateCustomer(updateReqVO.getCustomerId());
        // 1.3 校验结算账户
        if (updateReqVO.getAccountId() != null) {
            accountService.validateAccount(updateReqVO.getAccountId());
        }
        // 1.4 校验销售人员
        if (updateReqVO.getSaleUserId() != null) {
            adminUserApi.validateUser(updateReqVO.getSaleUserId());
        }
        // 1.5 校验订单项的有效性
        List<ErpSaleOrderItemDO> saleOrderItems = validateSaleOrderItems(updateReqVO);

        // 2.1 更新订单
        ErpSaleOrderDO updateObj = BeanUtils.toBean(updateReqVO, ErpSaleOrderDO.class);
        calculateTotalPrice(updateObj, saleOrderItems, updateReqVO.getKingdeeAmounts());
        if (sourceStatus != null) {
            reconcileSourceItems(saleOrder, updateReqVO, saleOrderItems, previousItems, sourceStatus, updateObj);
            updateObj.setStatus(sourceStatus);
        }
        if (sourceStatus == null) {
            saleOrderMapper.updateById(updateObj);
        } else if (saleOrderMapper.updateByIdAndStatus(updateReqVO.getId(), saleOrder.getStatus(), updateObj) != 1) {
            throw new IllegalStateException("ERP订单更新期间状态已变化，请重新同步");
        }
        // 2.2 更新订单项
        updateSaleOrderItemList(updateReqVO.getId(), saleOrderItems, sourceStatus != null);
    }


    private void reconcileSourceItems(ErpSaleOrderDO order, ErpSaleOrderSaveReqVO req,
            List<ErpSaleOrderItemDO> incoming, List<ErpSaleOrderSaveReqVO.Item> previous,
            Integer status, ErpSaleOrderDO update) {
        var stored = saleOrderItemMapper.selectListByOrderId(order.getId());
        var ids = ErpKingdeeOrderLineIdentity.reconcile(stored.stream().map(o -> new Line(o.getId(),
                o.getSourceEntryId(), o.getProductId(), o.getProductUnitId(), o.getProductPrice(),
                o.getCount(), o.getTaxPercent(), o.getRemark())).toList(), incoming.stream().map(o -> new Line(null,
                o.getSourceEntryId(), o.getProductId(), o.getProductUnitId(), o.getProductPrice(),
                o.getCount(), o.getTaxPercent(), o.getRemark())).toList(), previous.stream().map(o -> new Line(null,
                o.getSourceEntryId(), o.getProductId(), o.getProductUnitId(), o.getProductPrice(),
                o.getCount(), o.getTaxPercent(), o.getRemark())).toList());
        incoming.forEach(o -> o.setId(ids.get(o.getSourceEntryId())));
        var documents = saleOutMapper.selectListByOrderId(order.getId());
        var returns = saleReturnMapper.selectListByOrderId(order.getId());
        var quantities = saleOutItemMapper.selectOrderItemCountSumMapByOutIds(
                documents.stream().map(o -> o.getId()).toList());
        var returnQuantities = saleReturnItemMapper.selectOrderItemCountSumMapByReturnIds(
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
        boolean linked = !documents.isEmpty() || !returns.isEmpty();
        if (linked && (!Objects.equals(order.getCustomerId(), req.getCustomerId())
                || !ErpAuditStatus.APPROVE.getStatus().equals(status))) {
            throw new IllegalStateException("已有出入库关联的ERP订单不能更换往来单位或反审核：" + order.getNo());
        }
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal totalReturn = BigDecimal.ZERO;
        for (var old : stored) {
            BigDecimal count = ErpKingdeeOrderLineIdentity.quantity(quantities, old.getId(), old.getOutCount());
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
                next.setOutCount(count).setReturnCount(returned);
            }
            total = total.add(count);
            totalReturn = totalReturn.add(returned);
        }
        ErpKingdeeOrderLineIdentity.quantity(java.util.Map.of(order.getId(), total), order.getId(), order.getOutCount());
        ErpKingdeeOrderLineIdentity.quantity(java.util.Map.of(order.getId(), totalReturn), order.getId(), order.getReturnCount());
        update.setOutCount(total).setReturnCount(totalReturn);
    }

    private void calculateTotalPrice(ErpSaleOrderDO saleOrder, List<ErpSaleOrderItemDO> saleOrderItems, ErpKingdeeOrderAmounts source) {
        saleOrder.setTotalCount(getSumValue(saleOrderItems, ErpSaleOrderItemDO::getCount, BigDecimal::add));
        saleOrder.setTotalProductPrice(getSumValue(saleOrderItems, ErpSaleOrderItemDO::getTotalPrice, BigDecimal::add, BigDecimal.ZERO));
        saleOrder.setTotalTaxPrice(getSumValue(saleOrderItems, ErpSaleOrderItemDO::getTaxPrice, BigDecimal::add, BigDecimal.ZERO));
        saleOrder.setTotalPrice(saleOrder.getTotalProductPrice().add(saleOrder.getTotalTaxPrice()));
        if (source != null) {
            source.validate(saleOrderItems.size());
            saleOrder.setTotalProductPrice(source.getProductAmount());
            saleOrder.setTotalTaxPrice(source.getTaxAmount());
            saleOrder.setTotalPrice(source.getTotalAmount());
            saleOrder.setDiscountPrice(source.getDiscountAmount());
            saleOrder.setDiscountPercent(source.discountPercent());
            return;
        }
        // 计算优惠价格
        if (saleOrder.getDiscountPercent() == null) {
            saleOrder.setDiscountPercent(BigDecimal.ZERO);
        }
        saleOrder.setDiscountPrice(MoneyUtils.priceMultiplyPercent(saleOrder.getTotalPrice(), saleOrder.getDiscountPercent()));
        saleOrder.setTotalPrice(saleOrder.getTotalPrice().subtract(saleOrder.getDiscountPrice()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSaleOrderStatus(Long id, Integer status) {
        boolean approve = ErpAuditStatus.APPROVE.getStatus().equals(status);
        // 1.1 校验存在
        ErpSaleOrderDO saleOrder = validateSaleOrderExists(id);
        // 1.2 校验状态
        if (saleOrder.getStatus().equals(status)) {
            throw exception(approve ? SALE_ORDER_APPROVE_FAIL : SALE_ORDER_PROCESS_FAIL);
        }
        if (!approve) {
            boolean hasOut = saleOutMapper.selectListByOrderId(id).stream().anyMatch(document ->
                    requireDocumentCount(document.getTotalCount(), document.getNo()).signum() > 0);
            if (hasOut || (saleOrder.getOutCount() != null && saleOrder.getOutCount().signum() > 0)) {
                throw exception(SALE_ORDER_PROCESS_FAIL_EXISTS_OUT);
            }
            boolean hasReturn = saleReturnMapper.selectListByOrderId(id).stream().anyMatch(document ->
                    requireDocumentCount(document.getTotalCount(), document.getNo()).signum() > 0);
            if (hasReturn || (saleOrder.getReturnCount() != null && saleOrder.getReturnCount().signum() > 0)) {
                throw exception(SALE_ORDER_PROCESS_FAIL_EXISTS_RETURN);
            }
        }

        // 2. 更新状态
        int updateCount = saleOrderMapper.updateByIdAndStatus(id, saleOrder.getStatus(),
                new ErpSaleOrderDO().setStatus(status));
        if (updateCount == 0) {
            throw exception(approve ? SALE_ORDER_APPROVE_FAIL : SALE_ORDER_PROCESS_FAIL);
        }
    }

    private static BigDecimal requireDocumentCount(BigDecimal value, String no) {
        if (value == null) throw new IllegalStateException("销售关联单据数量为空，无法核实是否允许反审核：" + no);
        return value;
    }

    private List<ErpSaleOrderItemDO> validateSaleOrderItems(ErpSaleOrderSaveReqVO req) {
        List<ErpSaleOrderSaveReqVO.Item> list = req.getItems();
        if (req.getKingdeeAmounts() != null) {
            ErpKingdeeOrderLineIdentity.reconcile(List.of(), list.stream().map(o -> new Line(null,
                    o.getSourceEntryId(), o.getProductId(), o.getProductUnitId(), o.getProductPrice(),
                    o.getCount(), o.getTaxPercent(), o.getRemark())).toList(), List.of());
        }
        if (req.getKingdeeAmounts() != null) { req.getKingdeeAmounts().validate(list.size()); }
        Map<Long, ErpProductUnitDO> units =
                productUnitService.getProductUnitMap(convertSet(list, ErpSaleOrderSaveReqVO.Item::getProductUnitId));
        for (ErpSaleOrderSaveReqVO.Item item : list) {
            if (item.getProductUnitId() == null || !units.containsKey(item.getProductUnitId())) {
                throw exception(PRODUCT_UNIT_NOT_EXISTS);
            }
        }
        // 1. 校验产品存在
        if (req.getKingdeeAmounts() == null) {
            productService.validProductList(convertSet(list, ErpSaleOrderSaveReqVO.Item::getProductId));
        } else {
            // ERP 历史快照可以引用已停用物料，导入不改变物料状态。
            for (Long productId : convertSet(list, ErpSaleOrderSaveReqVO.Item::getProductId)) {
                if (productService.getProduct(productId) == null) {
                    throw exception(PRODUCT_NOT_EXISTS);
                }
            }
        }
        // 2. 转化为 ErpSaleOrderItemDO 列表
        List<ErpSaleOrderItemDO> result = convertList(list, o -> BeanUtils.toBean(o, ErpSaleOrderItemDO.class, item -> {
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

    private void updateSaleOrderItemList(Long id, List<ErpSaleOrderItemDO> newList, boolean sourceUpdate) {
        // 第一步，对比新老数据，获得添加、修改、删除的列表
        List<ErpSaleOrderItemDO> oldList = saleOrderItemMapper.selectListByOrderId(id);
        var ownedIds = oldList.stream().map(item -> item.getId()).collect(java.util.stream.Collectors.toSet());
        if (newList.stream().anyMatch(item -> item.getId() != null && !ownedIds.contains(item.getId()))) {
            throw new IllegalArgumentException("订单明细ID不属于当前订单");
        }
        List<List<ErpSaleOrderItemDO>> diffList = diffList(oldList, newList, // id 不同，就认为是不同的记录
                (oldVal, newVal) -> oldVal.getId().equals(newVal.getId()));

        // 第二步，批量添加、修改、删除
        if (CollUtil.isNotEmpty(diffList.get(0))) {
            diffList.get(0).forEach(o -> o.setOrderId(id).setOutCount(BigDecimal.ZERO).setReturnCount(BigDecimal.ZERO));
            saleOrderItemMapper.insertBatch(diffList.get(0));
        }
        if (CollUtil.isNotEmpty(diffList.get(1))) {
            if (sourceUpdate) {
                for (var item : diffList.get(1)) {
                    if (saleOrderItemMapper.updateSourceItem(item, id) != 1) {
                        throw new IllegalStateException("ERP明细更新期间已变化，明细ID=" + item.getId());
                    }
                }
            } else {
                saleOrderItemMapper.updateBatch(diffList.get(1));
            }
        }
        if (CollUtil.isNotEmpty(diffList.get(2))) {
            saleOrderItemMapper.deleteByIds(convertList(diffList.get(2), ErpSaleOrderItemDO::getId));
        }
    }

    @Override
    public void updateSaleOrderOutCount(Long id, Map<Long, BigDecimal> outCountMap) {
        List<ErpSaleOrderItemDO> orderItems = saleOrderItemMapper.selectListByOrderId(id);
        // 1. 更新每个销售订单项
        orderItems.forEach(item -> {
            BigDecimal outCount = outCountMap.getOrDefault(item.getId(), BigDecimal.ZERO);
            if (outCount.equals(item.getOutCount())) {
                return;
            }
            if (outCount.compareTo(item.getCount()) > 0) {
                throw exception(SALE_ORDER_ITEM_OUT_FAIL_PRODUCT_EXCEED,
                        productService.getProduct(item.getProductId()).getName(), item.getCount());
            }
            saleOrderItemMapper.updateById(new ErpSaleOrderItemDO().setId(item.getId()).setOutCount(outCount));
        });
        // 2. 更新销售订单
        BigDecimal totalOutCount = getSumValue(outCountMap.values(), value -> value, BigDecimal::add, BigDecimal.ZERO);
        saleOrderMapper.updateById(new ErpSaleOrderDO().setId(id).setOutCount(totalOutCount));
    }

    @Override
    public void updateSaleOrderReturnCount(Long orderId, Map<Long, BigDecimal> returnCountMap) {
        List<ErpSaleOrderItemDO> orderItems = saleOrderItemMapper.selectListByOrderId(orderId);
        // 1. 更新每个销售订单项
        orderItems.forEach(item -> {
            BigDecimal returnCount = returnCountMap.getOrDefault(item.getId(), BigDecimal.ZERO);
            if (returnCount.equals(item.getReturnCount())) {
                return;
            }
            if (item.getOutCount() == null) {
                throw new IllegalStateException("订单出入库数量为空，请先核对关联单据，明细ID=" + item.getId());
            }
            if (returnCount.compareTo(item.getOutCount()) > 0) {
                throw exception(SALE_ORDER_ITEM_RETURN_FAIL_OUT_EXCEED,
                        productService.getProduct(item.getProductId()).getName(), item.getOutCount());
            }
            saleOrderItemMapper.updateById(new ErpSaleOrderItemDO().setId(item.getId()).setReturnCount(returnCount));
        });
        // 2. 更新销售订单
        BigDecimal totalReturnCount = getSumValue(returnCountMap.values(), value -> value, BigDecimal::add, BigDecimal.ZERO);
        saleOrderMapper.updateById(new ErpSaleOrderDO().setId(orderId).setReturnCount(totalReturnCount));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSaleOrder(List<Long> ids) {
        // 1. 校验不处于已审批
        List<ErpSaleOrderDO> saleOrders = ids.stream().distinct().sorted()
                .map(this::validateSaleOrderExists).toList();
        if (CollUtil.isEmpty(saleOrders)) {
            return;
        }
        saleOrders.forEach(saleOrder -> {
            if (ErpAuditStatus.APPROVE.getStatus().equals(saleOrder.getStatus())) {
                throw exception(SALE_ORDER_DELETE_FAIL_APPROVE, saleOrder.getNo());
            }
        });

        // 2. 遍历删除，并记录操作日志
        saleOrders.forEach(saleOrder -> {
            // 2.1 删除订单
            saleOrderMapper.deleteById(saleOrder.getId());
            // 2.2 删除订单项
            saleOrderItemMapper.deleteByOrderId(saleOrder.getId());
        });
    }

    private ErpSaleOrderDO validateSaleOrderExists(Long id) {
        ErpSaleOrderDO saleOrder = org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()
                ? saleOrderMapper.selectByIdForUpdate(id) : saleOrderMapper.selectById(id);
        if (saleOrder == null) {
            throw exception(SALE_ORDER_NOT_EXISTS);
        }
        return saleOrder;
    }

    @Override
    public ErpSaleOrderDO getSaleOrder(Long id) {
        return saleOrderMapper.selectById(id);
    }

    @Override
    public ErpSaleOrderDO validateSaleOrder(Long id) {
        ErpSaleOrderDO saleOrder = validateSaleOrderExists(id);
        if (ObjectUtil.notEqual(saleOrder.getStatus(), ErpAuditStatus.APPROVE.getStatus())) {
            throw exception(SALE_ORDER_NOT_APPROVE);
        }
        return saleOrder;
    }

    @Override
    public PageResult<ErpSaleOrderDO> getSaleOrderPage(ErpSaleOrderPageReqVO pageReqVO) {
        return saleOrderMapper.selectPage(pageReqVO);
    }

    // ==================== 订单项 ====================

    @Override
    public List<ErpSaleOrderItemDO> getSaleOrderItemListByOrderId(Long orderId) {
        return saleOrderItemMapper.selectListByOrderId(orderId);
    }

    @Override
    public List<ErpSaleOrderItemDO> getSaleOrderItemListByOrderIds(Collection<Long> orderIds) {
        if (CollUtil.isEmpty(orderIds)) {
            return Collections.emptyList();
        }
        return saleOrderItemMapper.selectListByOrderIds(orderIds);
    }

}
