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
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeePurchaseOrderClient;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeSaleOrder;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeSaleOrderClient;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleOrderService;
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpKingdeeSaleOrderSyncResult syncSaleOrders() {
        ErpKingdeeProperties kingdeeProperties = kingdeeConfigService.getEffectiveProperties();
        kingdeeProperties.validateBaseConfig();
        List<ErpKingdeeSaleOrder> saleOrders = saleOrderClient.fetchSaleOrders(kingdeeProperties);
        return syncSaleOrders(kingdeeProperties, saleOrders);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpKingdeeSaleOrderSyncResult syncSaleOrdersModifiedBetween(LocalDateTime windowStart,
                                                                       LocalDateTime windowEnd) {
        ErpKingdeeProperties kingdeeProperties = kingdeeConfigService.getEffectiveProperties();
        kingdeeProperties.validateBaseConfig();
        List<ErpKingdeeSaleOrder> saleOrders = saleOrderClient.fetchSaleOrdersModifiedBetween(
                kingdeeProperties, windowStart, windowEnd);
        return syncSaleOrders(kingdeeProperties, saleOrders);
    }

    private ErpKingdeeSaleOrderSyncResult syncSaleOrders(ErpKingdeeProperties kingdeeProperties,
                                                         List<ErpKingdeeSaleOrder> saleOrders) {
        Map<String, ErpKingdeeMaterialDetail> materialDetails = new HashMap<>();
            ErpKingdeeSaleOrderSyncResult result = new ErpKingdeeSaleOrderSyncResult();
        for (ErpKingdeeSaleOrder saleOrder : saleOrders) {
            ErpKingdeeSaleOrderSyncRecordDO syncRecord =
                    syncRecordMapper.selectBySourceKey(ErpKingdeeSaleOrder.FORM_ID, saleOrder.getFid());
            if (syncRecord != null) {
                updateExistingSaleOrder(kingdeeProperties, saleOrder, syncRecord, materialDetails);
                result.addUpdated(syncRecord.getSaleOrderId());
            } else {
                Long saleOrderId = saleOrderService.createSaleOrder(buildCreateReqVO(kingdeeProperties, saleOrder, materialDetails));
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
                result.addCreated(saleOrderId);
            }
        }
        return result;
    }

    private void updateExistingSaleOrder(ErpKingdeeProperties kingdeeProperties,
                                         ErpKingdeeSaleOrder saleOrder,
                                         ErpKingdeeSaleOrderSyncRecordDO syncRecord,
                                         Map<String, ErpKingdeeMaterialDetail> materialDetails) {
        Long saleOrderId = syncRecord.getSaleOrderId();
        ErpSaleOrderDO localOrder = saleOrderService.getSaleOrder(saleOrderId);
        if (localOrder == null) {
            throw exception(KINGDEE_PURCHASE_ORDER_RESPONSE_INVALID,
                    "local sale order is missing for source fid " + saleOrder.getFid());
        }
        boolean wasApproved = ErpAuditStatus.APPROVE.getStatus().equals(localOrder.getStatus());
        if (wasApproved) {
            saleOrderService.updateSaleOrderStatus(saleOrderId, ErpAuditStatus.PROCESS.getStatus());
        }

        ErpSaleOrderSaveReqVO updateReqVO = buildCreateReqVO(kingdeeProperties, saleOrder, materialDetails);
        updateReqVO.setId(saleOrderId);
        saleOrderService.updateSaleOrder(updateReqVO);
        updateKingdeeSourceStatus(saleOrderId, saleOrder);

        if (isApproved(saleOrder)) {
            saleOrderService.updateSaleOrderStatus(saleOrderId, ErpAuditStatus.APPROVE.getStatus());
        }
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
        reqVO.setDepositPrice(BigDecimal.ZERO);
        reqVO.setRemark("Kingdee K3Cloud sale order: " + saleOrder.getBillNo());
        reqVO.setItems(buildItems(kingdeeProperties, saleOrder, materialDetails));
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
            Long productId = resolveProductId(kingdeeProperties, line, materialDetails);
            ErpProductDO product = productMapper.selectById(productId);
            item.setProductId(productId);
            item.setProductUnitId(product.getUnitId());
            item.setProductPrice(line.getPrice());
            item.setCount(line.getQuantity());
            item.setTaxPercent(line.getTaxPercent());
            item.setRemark(null);
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
