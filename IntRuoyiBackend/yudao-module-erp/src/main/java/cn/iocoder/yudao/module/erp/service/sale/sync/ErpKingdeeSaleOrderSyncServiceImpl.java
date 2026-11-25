package cn.iocoder.yudao.module.erp.service.sale.sync;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.order.ErpSaleOrderSaveReqVO;
import cn.iocoder.yudao.module.erp.service.config.ErpKingdeeConfigService;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpKingdeeCustomerSyncRecordDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpKingdeeSaleOrderSyncRecordDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductCategoryDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductUnitDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpCustomerMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpKingdeeCustomerSyncRecordMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpKingdeeSaleOrderSyncRecordMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOrderMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductCategoryMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductUnitMapper;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeMaterialDetail;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProperties;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeOrderAmounts;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeePurchaseOrderClient;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeSaleOrder;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeSaleOrderClient;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import cn.iocoder.yudao.module.erp.service.sync.runtime.*;
import jakarta.annotation.Resource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID;

@Service
@Validated
@RequiredArgsConstructor
public class ErpKingdeeSaleOrderSyncServiceImpl implements ErpKingdeeSaleOrderSyncService {

    private final ErpKingdeeSaleOrderClient saleOrderClient;
    private final ErpKingdeePurchaseOrderClient materialDetailClient;
    private final ErpKingdeeConfigService kingdeeConfigService;
    private final ErpKingdeeSaleOrderSyncRecordMapper syncRecordMapper;
    private final ErpSaleOrderMapper saleOrderMapper;
    private final ErpSaleOrderService saleOrderService;
    private final ErpCustomerMapper customerMapper;
    private final ErpKingdeeCustomerSyncRecordMapper customerSyncRecordMapper;
    private final ErpProductMapper productMapper;
    private final ErpProductCategoryMapper productCategoryMapper;
    private final ErpProductUnitMapper productUnitMapper;

    @Resource
    private PlatformTransactionManager transactionManager;
    @Resource
    private ErpKingdeeSyncRuntimeTransactionService runtimeTransactionService;

    @Override
    public ErpKingdeeSaleOrderSyncResult syncSaleOrders() {
        ErpKingdeeProperties kingdeeProperties = kingdeeConfigService.getEffectiveProperties();
        kingdeeProperties.validateBaseConfig();
        List<ErpKingdeeSaleOrder> saleOrders = saleOrderClient.fetchSaleOrders(kingdeeProperties);
        return syncSaleOrders(kingdeeProperties, saleOrders, false);
    }

    @Override
    public ErpKingdeeSaleOrderSyncResult syncAllWithUpdates() {
        ErpKingdeeProperties properties = kingdeeConfigService.getEffectiveProperties();
        properties.validateBaseConfig();
        return syncSaleOrders(properties, saleOrderClient.fetchAllSaleOrders(properties), false);
    }

    @Override
    public int syncByCode(String billNo) {
        ErpKingdeeProperties properties = kingdeeConfigService.getEffectiveProperties();
        properties.validateBaseConfig();
        ErpKingdeeSaleOrderSyncResult result = syncSaleOrders(properties,
                saleOrderClient.fetchSaleOrdersByBillNo(properties, billNo), false);
        return result.getCreatedCount() + result.getUpdatedCount() + result.getSkippedCount();
    }

    @Override
    public ErpKingdeeSaleOrderSyncResult syncSaleOrdersFullSkipExisting() {
        return syncSaleOrdersFullSkipExisting(null, null);
    }

    @Override
    public ErpKingdeeSaleOrderSyncResult syncSaleOrdersFullSkipExisting(LocalDateTime windowStart, LocalDateTime windowEnd) {
        ErpKingdeeProperties kingdeeProperties = kingdeeConfigService.getEffectiveProperties();
        kingdeeProperties.validateBaseConfig();
        List<ErpKingdeeSaleOrder> saleOrders = (windowStart == null ? saleOrderClient.fetchSaleOrders(kingdeeProperties)
                : saleOrderClient.fetchSaleOrdersModifiedBetween(kingdeeProperties, windowStart, windowEnd));
        return syncSaleOrders(kingdeeProperties, saleOrders, true);
    }

    @Override
    public ErpKingdeeSaleOrderSyncResult syncSaleOrdersModifiedBetween(LocalDateTime windowStart,
                                                                       LocalDateTime windowEnd) {
        ErpKingdeeProperties kingdeeProperties = kingdeeConfigService.getEffectiveProperties();
        kingdeeProperties.validateBaseConfig();
        List<ErpKingdeeSaleOrder> saleOrders = saleOrderClient.fetchSaleOrdersModifiedBetween(
                kingdeeProperties, windowStart, windowEnd);
        return syncSaleOrders(kingdeeProperties, saleOrders, false);
    }

    private ErpKingdeeSaleOrderSyncResult syncSaleOrders(ErpKingdeeProperties kingdeeProperties,
                                                         List<ErpKingdeeSaleOrder> saleOrders,
                                                         boolean skipExisting) {
        Map<String, ErpKingdeeMaterialDetail> materialDetails = new HashMap<>();
            ErpKingdeeSaleOrderSyncResult result = new ErpKingdeeSaleOrderSyncResult();
        List<ErpKingdeeOrderSyncFailure> failures = new ArrayList<>();
        for (ErpKingdeeSaleOrder saleOrder : saleOrders) {
            if (Thread.currentThread().isInterrupted()) { throw new java.util.concurrent.CancellationException("ERP同步已停止"); }
            String[] step = {"读取本地同步关联"};
            try {
                TransactionTemplate transaction = new TransactionTemplate(transactionManager);
                transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                ErpKingdeeSaleOrderSyncResult single = transaction.execute(status -> {
                ErpKingdeeSaleOrderSyncResult itemResult = new ErpKingdeeSaleOrderSyncResult();
                ErpKingdeeSaleOrderSyncRecordDO syncRecord =
                        syncRecordMapper.selectBySourceKey(ErpKingdeeSaleOrder.FORM_ID, saleOrder.getFid());
                if (syncRecord != null) {
                    if (skipExisting) {
                        itemResult.addSkipped(saleOrder.getFid());
                        recordProgress(itemResult);
                        return itemResult;
                    }
                    step[0] = "更新已有订单及明细";
                    updateExistingSaleOrder(kingdeeProperties, saleOrder, syncRecord, materialDetails);
                    itemResult.addUpdated(syncRecord.getSaleOrderId());
                } else {
                    step[0] = "构建并新增订单";
                    Long saleOrderId = saleOrderService.createSaleOrder(buildCreateReqVO(kingdeeProperties, saleOrder, materialDetails));
                    step[0] = "写入源状态及同步记录";
                    updateKingdeeSourceStatus(saleOrderId, saleOrder);
                    if (isApproved(saleOrder)) {
                        saleOrderService.updateSaleOrderStatus(saleOrderId, ErpAuditStatus.APPROVE.getStatus());
                    }
                    syncRecordMapper.insert(new ErpKingdeeSaleOrderSyncRecordDO()
                            .setSourceFormId(ErpKingdeeSaleOrder.FORM_ID)
                            .setSourceFid(saleOrder.getFid())
                            .setSourceBillNo(saleOrder.getBillNo())
                            .setSaleOrderId(saleOrderId)
                            .setRawPayload(JsonUtils.toJsonString(saleOrder)));
                    itemResult.addCreated(saleOrderId);
                }
                recordProgress(itemResult);
                return itemResult;
                });
                single.getCreatedSaleOrderIds().forEach(result::addCreated);
                single.getUpdatedSaleOrderIds().forEach(result::addUpdated);
                single.getSkippedSourceFids().forEach(result::addSkipped);
            } catch (java.util.concurrent.CancellationException ex) {
                throw ex;
            } catch (RuntimeException ex) {
                org.slf4j.LoggerFactory.getLogger(ErpKingdeeSaleOrderSyncServiceImpl.class).error(
                        "ERP订单同步失败 form={} fid={} billNo={} step={} errorType={}",
                        ErpKingdeeSaleOrder.FORM_ID, saleOrder.getFid(), saleOrder.getBillNo(),
                        step[0], ex.getClass().getSimpleName());
                failures.add(new ErpKingdeeOrderSyncFailure(
                        "SaleOrder", saleOrder.getFid(), saleOrder.getBillNo(), step[0], saleOrders.size(), ex));
            }
        }
        if (!failures.isEmpty()) { throw new ErpKingdeeOrderBatchSyncFailure(failures); }
        return result;
    }

    private void recordProgress(ErpKingdeeSaleOrderSyncResult result) {
        Long runId = ErpKingdeeOrderSyncContext.getRunId();
        if (runId != null) {
            runtimeTransactionService.recordOrderProgress(runId, ErpKingdeeSyncRunResult.success(null,
                    result.getCreatedCount(), result.getUpdatedCount(), result.getSkippedCount(), 0));
        }
    }

    private void updateExistingSaleOrder(ErpKingdeeProperties kingdeeProperties,
                                         ErpKingdeeSaleOrder saleOrder,
                                         ErpKingdeeSaleOrderSyncRecordDO syncRecord,
                                         Map<String, ErpKingdeeMaterialDetail> materialDetails) {
        Long saleOrderId = syncRecord.getSaleOrderId();
        saleOrderService.lockOrder(saleOrderId);
        ErpSaleOrderDO localOrder = saleOrderService.getSaleOrder(saleOrderId);
        if (localOrder == null) {
            throw exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID,
                    "local sale order is missing for source fid " + saleOrder.getFid());
        }
        ErpSaleOrderSaveReqVO updateReqVO = buildCreateReqVO(kingdeeProperties, saleOrder, materialDetails);
        updateReqVO.setId(saleOrderId);
        List<ErpSaleOrderSaveReqVO.Item> previousItems = List.of();
        if (saleOrderService.getSaleOrderItemListByOrderId(saleOrderId).stream()
                .anyMatch(item -> StrUtil.isBlank(item.getSourceEntryId()))) {
            if (StrUtil.isBlank(syncRecord.getRawPayload())) {
                throw new IllegalStateException("历史订单缺少原始ERP快照，无法绑定明细EntryID");
            }
            previousItems = buildItems(kingdeeProperties,
                    JsonUtils.parseObject(syncRecord.getRawPayload(), ErpKingdeeSaleOrder.class), materialDetails);
        }
        saleOrderService.updateSaleOrderFromKingdee(updateReqVO,
                isApproved(saleOrder) ? ErpAuditStatus.APPROVE.getStatus() : ErpAuditStatus.PROCESS.getStatus(), previousItems);
        updateKingdeeSourceStatus(saleOrderId, saleOrder);

        syncRecordMapper.updateById(new ErpKingdeeSaleOrderSyncRecordDO()
                .setId(syncRecord.getId())
                .setSourceBillNo(saleOrder.getBillNo())
                .setRawPayload(JsonUtils.toJsonString(saleOrder)));
    }

    private boolean isApproved(ErpKingdeeSaleOrder saleOrder) {
        return "C".equalsIgnoreCase(saleOrder.getDocumentStatus());
    }

    private void updateKingdeeSourceStatus(Long saleOrderId, ErpKingdeeSaleOrder saleOrder) {
        saleOrderMapper.updateById(new ErpSaleOrderDO()
                .setId(saleOrderId)
                .setKingdeeCloseStatus(saleOrder.getCloseStatus())
                .setKingdeeCancelStatus(saleOrder.getCancelStatus()));
    }

    private ErpSaleOrderSaveReqVO buildCreateReqVO(ErpKingdeeProperties kingdeeProperties,
                                                   ErpKingdeeSaleOrder saleOrder,
                                                   Map<String, ErpKingdeeMaterialDetail> materialDetails) {
        ErpSaleOrderSaveReqVO reqVO = new ErpSaleOrderSaveReqVO();
        reqVO.setCustomerId(resolveCustomerId(saleOrder));
        reqVO.setOrderTime(saleOrder.getBillDate());
        reqVO.setDiscountPercent(BigDecimal.ZERO);
        reqVO.setDepositPrice(saleOrder.getDepositAmount());
        reqVO.setRemark(saleOrder.getRemark());
        reqVO.setItems(buildItems(kingdeeProperties, saleOrder, materialDetails));
        ErpKingdeeOrderAmounts amounts = new ErpKingdeeOrderAmounts();
        amounts.setProductAmount(saleOrder.getTotalProductAmount());
        amounts.setTaxAmount(saleOrder.getTotalTaxAmount());
        amounts.setTotalAmount(saleOrder.getTotalAmount());
        amounts.setDiscountAmount(saleOrder.getDiscountAmount());
        amounts.setLines(new ArrayList<>());
        for (var line : saleOrder.getLines()) {
            var amount = new ErpKingdeeOrderAmounts.Line();
            amount.setNetAmount(line.getNetAmount());
            amount.setTaxAmount(line.getTaxAmount());
            amount.setTotalAmount(line.getTotalAmount());
            amounts.getLines().add(amount);
        }
        amounts.validate(reqVO.getItems().size());
        reqVO.setDiscountPercent(amounts.discountPercent());
        reqVO.setKingdeeAmounts(amounts);
        return reqVO;
    }

    private List<ErpSaleOrderSaveReqVO.Item> buildItems(ErpKingdeeProperties kingdeeProperties,
                                                        ErpKingdeeSaleOrder saleOrder,
                                                        Map<String, ErpKingdeeMaterialDetail> materialDetails) {
        if (CollUtil.isEmpty(saleOrder.getLines())) {
            throw exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID, "sale order lines are empty");
        }
        List<ErpSaleOrderSaveReqVO.Item> items = new ArrayList<>(saleOrder.getLines().size());
        for (ErpKingdeeSaleOrder.Line line : saleOrder.getLines()) {
            ErpSaleOrderSaveReqVO.Item item = new ErpSaleOrderSaveReqVO.Item();
            item.setSourceEntryId(line.getEntryId());
            Long productId = resolveProductId(kingdeeProperties, line, materialDetails);
            item.setProductId(productId);
            item.setProductUnitId(ensureOrderUnit(line.getUnitName()));
            // Historical ERP rows can omit the pricing unit. Retain their price without inventing a conversion.
            item.setProductPrice(StrUtil.isBlank(line.getPriceUnitName()) ? line.getPrice()
                    : ErpKingdeeOrderAmounts.transactionUnitPrice(line.getPrice(), line.getQuantity(),
                    line.getPriceUnitQuantity(), line.getUnitName(), line.getPriceUnitName()));
            item.setCount(line.getQuantity());
            item.setTaxPercent(line.getTaxPercent());
            item.setRemark(line.getRemark());
            items.add(item);
        }
        return items;
    }

    private Long resolveCustomerId(ErpKingdeeSaleOrder saleOrder) {
        ErpKingdeeCustomerSyncRecordDO syncRecord =
                customerSyncRecordMapper.selectBySourceCustomerNumber(saleOrder.getCustomerNumber());
        if (syncRecord != null) {
            return syncRecord.getCustomerId();
        }
        ErpCustomerDO customer = customerMapper.selectByName(saleOrder.getCustomerName());
        if (customer == null) {
            customer = new ErpCustomerDO();
            customer.setName(saleOrder.getCustomerName());
            customer.setStatus(CommonStatusEnum.ENABLE.getStatus());
            customer.setSort(0);
            customerMapper.insert(customer);
        }
        customerSyncRecordMapper.insert(new ErpKingdeeCustomerSyncRecordDO()
                .setSourceCustomerNumber(saleOrder.getCustomerNumber())
                .setSourceCustomerName(saleOrder.getCustomerName())
                .setCustomerId(customer.getId()));
        return customer.getId();
    }

    private Long resolveProductId(ErpKingdeeProperties kingdeeProperties,
                                  ErpKingdeeSaleOrder.Line line,
                                  Map<String, ErpKingdeeMaterialDetail> materialDetails) {
        ErpProductDO existingProduct = productMapper.selectByBarCode(line.getMaterialNumber());
        if (existingProduct != null) {
            return existingProduct.getId();
        }
        ErpKingdeeMaterialDetail detail = materialDetails.computeIfAbsent(
                line.getMaterialNumber(),
                key -> fetchMaterialDetail(kingdeeProperties, key));
        if (detail == null) {
            throw exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID,
                    "material detail is missing for material " + line.getMaterialNumber());
        }
        ErpProductDO product = new ErpProductDO();
        product.setName(StrUtil.blankToDefault(detail.getMaterialName(), line.getMaterialName()));
        product.setBarCode(line.getMaterialNumber());
        product.setCategoryId(ensureProductCategory(detail));
        product.setUnitId(ensureProductUnit(detail));
        product.setStatus(CommonStatusEnum.ENABLE.getStatus());
        product.setStandard(detail.getSpecification());
        product.setSalePrice(line.getTaxPrice());
        product.setMinPrice(line.getTaxPrice());
        productMapper.insert(product);
        return product.getId();
    }

    private ErpKingdeeMaterialDetail fetchMaterialDetail(ErpKingdeeProperties kingdeeProperties, String materialNumber) {
        return materialDetailClient.fetchMaterialDetails(kingdeeProperties, List.of(materialNumber)).get(materialNumber);
    }

    private Long ensureProductCategory(ErpKingdeeMaterialDetail detail) {
        ErpProductCategoryDO category = productCategoryMapper.selectByCode(detail.getCategoryCode());
        if (category != null) {
            return category.getId();
        }
        category = new ErpProductCategoryDO();
        category.setParentId(ErpProductCategoryDO.PARENT_ID_ROOT);
        category.setName(detail.getCategoryName());
        category.setCode(detail.getCategoryCode());
        category.setSort(0);
        category.setStatus(CommonStatusEnum.ENABLE.getStatus());
        productCategoryMapper.insert(category);
        return category.getId();
    }

    private Long ensureOrderUnit(String name) {
        if (StrUtil.isBlank(name)) { throw exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID, "ERP order unit is blank"); }
        ErpProductUnitDO unit = productUnitMapper.selectByName(name);
        if (unit == null) {
            unit = new ErpProductUnitDO();
            unit.setName(name);
            unit.setStatus(CommonStatusEnum.ENABLE.getStatus());
            productUnitMapper.insert(unit);
        }
        return unit.getId();
    }

    private Long ensureProductUnit(ErpKingdeeMaterialDetail detail) {
        ErpProductUnitDO unit = productUnitMapper.selectByName(detail.getUnitName());
        if (unit != null) {
            return unit.getId();
        }
        unit = new ErpProductUnitDO();
        unit.setName(detail.getUnitName());
        unit.setStatus(CommonStatusEnum.ENABLE.getStatus());
        productUnitMapper.insert(unit);
        return unit.getId();
    }

}
