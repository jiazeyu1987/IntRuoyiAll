package cn.iocoder.yudao.module.erp.service.purchase.sync;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderSaveReqVO;
import cn.iocoder.yudao.module.erp.service.config.ErpKingdeeConfigService;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductCategoryDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductUnitDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpKingdeePurchaseOrderSyncRecordDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpKingdeeSupplierSyncRecordDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductCategoryMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.product.ErpProductUnitMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpKingdeePurchaseOrderSyncRecordMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpKingdeeSupplierSyncRecordMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseOrderMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpSupplierMapper;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
public class ErpKingdeePurchaseOrderSyncServiceImpl implements ErpKingdeePurchaseOrderSyncService {

    private final ErpKingdeePurchaseOrderClient kingdeePurchaseOrderClient;
    private final ErpKingdeeConfigService kingdeeConfigService;
    private final ErpKingdeePurchaseOrderSyncRecordMapper syncRecordMapper;
    private final ErpPurchaseOrderMapper purchaseOrderMapper;
    private final ErpPurchaseOrderService purchaseOrderService;
    private final ErpSupplierMapper supplierMapper;
    private final ErpKingdeeSupplierSyncRecordMapper supplierSyncRecordMapper;
    private final ErpProductMapper productMapper;
    private final ErpProductCategoryMapper productCategoryMapper;
    private final ErpProductUnitMapper productUnitMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpKingdeePurchaseOrderSyncResult syncPurchaseOrders() {
        ErpKingdeeProperties kingdeeProperties = kingdeeConfigService.getEffectiveProperties();
        kingdeeProperties.validatePurchaseOrderSyncConfig();
        List<ErpKingdeePurchaseOrder> purchaseOrders = kingdeePurchaseOrderClient.fetchPurchaseOrders(kingdeeProperties);
        return syncPurchaseOrders(kingdeeProperties, purchaseOrders, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpKingdeePurchaseOrderSyncResult syncPurchaseOrdersFullSkipExisting() {
        ErpKingdeeProperties kingdeeProperties = kingdeeConfigService.getEffectiveProperties();
        kingdeeProperties.validatePurchaseOrderSyncConfig();
        List<ErpKingdeePurchaseOrder> purchaseOrders = kingdeePurchaseOrderClient.fetchPurchaseOrders(kingdeeProperties);
        return syncPurchaseOrders(kingdeeProperties, purchaseOrders, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpKingdeePurchaseOrderSyncResult syncPurchaseOrdersModifiedBetween(LocalDateTime windowStart,
                                                                               LocalDateTime windowEnd) {
        ErpKingdeeProperties kingdeeProperties = kingdeeConfigService.getEffectiveProperties();
        kingdeeProperties.validatePurchaseOrderSyncConfig();
        List<ErpKingdeePurchaseOrder> purchaseOrders = kingdeePurchaseOrderClient.fetchPurchaseOrdersModifiedBetween(
                kingdeeProperties, windowStart, windowEnd);
        return syncPurchaseOrders(kingdeeProperties, purchaseOrders, false);
    }

    private ErpKingdeePurchaseOrderSyncResult syncPurchaseOrders(ErpKingdeeProperties kingdeeProperties,
                                                                 List<ErpKingdeePurchaseOrder> purchaseOrders,
                                                                 boolean skipExisting) {
        Map<String, ErpKingdeeMaterialDetail> materialDetails = new HashMap<>();

        ErpKingdeePurchaseOrderSyncResult result = new ErpKingdeePurchaseOrderSyncResult();
        for (ErpKingdeePurchaseOrder purchaseOrder : purchaseOrders) {
            ErpKingdeePurchaseOrderSyncRecordDO syncRecord =
                    syncRecordMapper.selectBySourceKey(ErpKingdeePurchaseOrder.FORM_ID, purchaseOrder.getFid());
            if (syncRecord != null) {
                if (skipExisting) {
                    result.addSkipped(purchaseOrder.getFid());
                    continue;
                }
                updateExistingPurchaseOrder(kingdeeProperties, purchaseOrder, syncRecord, materialDetails);
                result.addUpdated(syncRecord.getPurchaseOrderId());
            } else {
                ErpPurchaseOrderSaveReqVO createReqVO = buildCreateReqVO(kingdeeProperties, purchaseOrder, materialDetails);
                Long purchaseOrderId = purchaseOrderService.createPurchaseOrder(createReqVO);
                updateKingdeeSourceStatus(purchaseOrderId, purchaseOrder);
                if (isApproved(purchaseOrder)) {
                    purchaseOrderService.updatePurchaseOrderStatus(purchaseOrderId, ErpAuditStatus.APPROVE.getStatus());
                }
                syncRecordMapper.insert(buildSyncRecord(purchaseOrder, purchaseOrderId));
                result.addCreated(purchaseOrderId);
            }
        }
        return result;
    }

    private void updateExistingPurchaseOrder(ErpKingdeeProperties kingdeeProperties,
                                             ErpKingdeePurchaseOrder purchaseOrder,
                                             ErpKingdeePurchaseOrderSyncRecordDO syncRecord,
                                             Map<String, ErpKingdeeMaterialDetail> materialDetails) {
        Long purchaseOrderId = syncRecord.getPurchaseOrderId();
        ErpPurchaseOrderDO localOrder = purchaseOrderService.getPurchaseOrder(purchaseOrderId);
        if (localOrder == null) {
            throw exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID,
                    "local purchase order is missing for source fid " + purchaseOrder.getFid());
        }
        boolean wasApproved = ErpAuditStatus.APPROVE.getStatus().equals(localOrder.getStatus());
        if (wasApproved) {
            purchaseOrderService.updatePurchaseOrderStatus(purchaseOrderId, ErpAuditStatus.PROCESS.getStatus());
        }

        ErpPurchaseOrderSaveReqVO updateReqVO = buildCreateReqVO(kingdeeProperties, purchaseOrder, materialDetails);
        updateReqVO.setId(purchaseOrderId);
        purchaseOrderService.updatePurchaseOrder(updateReqVO);
        updateKingdeeSourceStatus(purchaseOrderId, purchaseOrder);

        if (isApproved(purchaseOrder)) {
            purchaseOrderService.updatePurchaseOrderStatus(purchaseOrderId, ErpAuditStatus.APPROVE.getStatus());
        }
        syncRecordMapper.updateById(new ErpKingdeePurchaseOrderSyncRecordDO()
                .setId(syncRecord.getId())
                .setSourceBillNo(purchaseOrder.getBillNo())
                .setSyncStatus(ErpKingdeePurchaseOrderSyncRecordDO.SYNC_STATUS_SUCCESS)
                .setFailureMessage(null)
                .setRawPayload(JsonUtils.toJsonString(purchaseOrder)));
    }

    private boolean isApproved(ErpKingdeePurchaseOrder purchaseOrder) {
        return "C".equalsIgnoreCase(purchaseOrder.getDocumentStatus());
    }

    private void updateKingdeeSourceStatus(Long purchaseOrderId, ErpKingdeePurchaseOrder purchaseOrder) {
        purchaseOrderMapper.updateById(new ErpPurchaseOrderDO()
                .setId(purchaseOrderId)
                .setKingdeeCloseStatus(purchaseOrder.getCloseStatus())
                .setKingdeeCancelStatus(purchaseOrder.getCancelStatus()));
    }

    private ErpPurchaseOrderSaveReqVO buildCreateReqVO(ErpKingdeeProperties kingdeeProperties,
                                                       ErpKingdeePurchaseOrder kingdeeOrder,
                                                       Map<String, ErpKingdeeMaterialDetail> materialDetails) {
        ErpPurchaseOrderSaveReqVO createReqVO = new ErpPurchaseOrderSaveReqVO();
        createReqVO.setSupplierId(resolveSupplierId(kingdeeProperties, kingdeeOrder));
        createReqVO.setOrderTime(kingdeeOrder.getBillDate());
        createReqVO.setDiscountPercent(BigDecimal.ZERO);
        createReqVO.setRemark("Kingdee K3Cloud采购订单：" + kingdeeOrder.getBillNo());
        createReqVO.setItems(buildItems(kingdeeProperties, kingdeeOrder, materialDetails));
        return createReqVO;
    }

    private List<ErpPurchaseOrderSaveReqVO.Item> buildItems(ErpKingdeeProperties kingdeeProperties,
                                                            ErpKingdeePurchaseOrder kingdeeOrder,
                                                            Map<String, ErpKingdeeMaterialDetail> materialDetails) {
        if (CollUtil.isEmpty(kingdeeOrder.getLines())) {
            throw exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID, "purchase order lines are empty");
        }
        List<ErpPurchaseOrderSaveReqVO.Item> items = new ArrayList<>(kingdeeOrder.getLines().size());
        for (ErpKingdeePurchaseOrder.Line line : kingdeeOrder.getLines()) {
            ErpPurchaseOrderSaveReqVO.Item item = new ErpPurchaseOrderSaveReqVO.Item();
            item.setProductId(resolveProductId(kingdeeProperties, line, materialDetails));
            item.setProductPrice(line.getPrice());
            item.setCount(line.getQuantity());
            item.setTaxPercent(line.getTaxPercent());
            item.setRemark(line.getRemark());
            items.add(item);
        }
        return items;
    }

    private Long resolveSupplierId(ErpKingdeeProperties kingdeeProperties, ErpKingdeePurchaseOrder kingdeeOrder) {
        Long supplierId = kingdeeProperties.getSupplierId(kingdeeOrder.getSupplierNumber());
        if (supplierId != null) {
            return supplierId;
        }
        ErpKingdeeSupplierSyncRecordDO syncRecord =
                supplierSyncRecordMapper.selectBySourceSupplierNumber(kingdeeOrder.getSupplierNumber());
        if (syncRecord != null) {
            return syncRecord.getSupplierId();
        }
        if (StrUtil.isBlank(kingdeeOrder.getSupplierName())) {
            throw exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID,
                    "supplier name is blank for supplier " + kingdeeOrder.getSupplierNumber());
        }

        ErpSupplierDO supplier = new ErpSupplierDO();
        supplier.setName(kingdeeOrder.getSupplierName());
        supplier.setStatus(CommonStatusEnum.ENABLE.getStatus());
        supplier.setSort(0);
        supplierMapper.insert(supplier);

        ErpKingdeeSupplierSyncRecordDO supplierSyncRecord = new ErpKingdeeSupplierSyncRecordDO();
        supplierSyncRecord.setSourceSupplierNumber(kingdeeOrder.getSupplierNumber());
        supplierSyncRecord.setSourceSupplierName(kingdeeOrder.getSupplierName());
        supplierSyncRecord.setSupplierId(supplier.getId());
        supplierSyncRecordMapper.insert(supplierSyncRecord);
        return supplier.getId();
    }

    private Long resolveProductId(ErpKingdeeProperties kingdeeProperties,
                                  ErpKingdeePurchaseOrder.Line line,
                                  Map<String, ErpKingdeeMaterialDetail> materialDetails) {
        Long productId = kingdeeProperties.getProductId(line.getMaterialNumber());
        if (productId != null) {
            return productId;
        }
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
        product.setName(resolveProductName(detail, line));
        product.setBarCode(line.getMaterialNumber());
        product.setCategoryId(ensureProductCategory(detail));
        product.setUnitId(ensureProductUnit(detail));
        product.setStatus(CommonStatusEnum.ENABLE.getStatus());
        product.setStandard(detail.getSpecification());
        product.setPurchasePrice(line.getPrice());
        productMapper.insert(product);
        return product.getId();
    }

    private ErpKingdeeMaterialDetail fetchMaterialDetail(ErpKingdeeProperties kingdeeProperties, String materialNumber) {
        Map<String, ErpKingdeeMaterialDetail> details =
                kingdeePurchaseOrderClient.fetchMaterialDetails(kingdeeProperties, List.of(materialNumber));
        return details.get(materialNumber);
    }

    private String resolveProductName(ErpKingdeeMaterialDetail detail, ErpKingdeePurchaseOrder.Line line) {
        String productName = StrUtil.blankToDefault(detail.getMaterialName(), line.getMaterialName());
        if (StrUtil.isBlank(productName)) {
            throw exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID,
                    "material name is blank for material " + line.getMaterialNumber());
        }
        return productName;
    }

    private Long ensureProductCategory(ErpKingdeeMaterialDetail detail) {
        if (StrUtil.isBlank(detail.getCategoryCode()) || StrUtil.isBlank(detail.getCategoryName())) {
            throw exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID,
                    "material category is blank for material " + detail.getMaterialNumber());
        }
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

    private Long ensureProductUnit(ErpKingdeeMaterialDetail detail) {
        if (StrUtil.isBlank(detail.getUnitName())) {
            throw exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID,
                    "material unit is blank for material " + detail.getMaterialNumber());
        }
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

    private ErpKingdeePurchaseOrderSyncRecordDO buildSyncRecord(ErpKingdeePurchaseOrder kingdeeOrder,
                                                                Long purchaseOrderId) {
        ErpKingdeePurchaseOrderSyncRecordDO record = new ErpKingdeePurchaseOrderSyncRecordDO();
        record.setSourceFormId(ErpKingdeePurchaseOrder.FORM_ID);
        record.setSourceFid(kingdeeOrder.getFid());
        record.setSourceBillNo(kingdeeOrder.getBillNo());
        record.setPurchaseOrderId(purchaseOrderId);
        record.setSyncStatus(ErpKingdeePurchaseOrderSyncRecordDO.SYNC_STATUS_SUCCESS);
        record.setRawPayload(JsonUtils.toJsonString(kingdeeOrder));
        return record;
    }

}
