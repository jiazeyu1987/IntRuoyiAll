package cn.iocoder.yudao.module.mes.service.pro.workorder.sync;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.IdUtil;
import cn.iocoder.yudao.module.erp.service.config.ErpKingdeeConfigService;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProductionOrder;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProductionOrderClient;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProductionOrderCreateRequest;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProductionOrderCreateResult;
import cn.iocoder.yudao.module.erp.service.purchase.sync.ErpKingdeeProperties;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.unitmeasure.MesMdUnitMeasureDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesKingdeeProductionOrderSyncRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesKingdeeProductionOrderSyncRecordMapper;
import cn.iocoder.yudao.module.mes.enums.pro.MesProWorkOrderStatusEnum;
import cn.iocoder.yudao.module.mes.enums.pro.MesProWorkOrderTypeEnum;
import cn.iocoder.yudao.module.mes.service.md.item.MesMdItemService;
import cn.iocoder.yudao.module.mes.service.md.unitmeasure.MesMdUnitMeasureService;
import cn.iocoder.yudao.module.mes.service.pro.workorder.MesProWorkOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_WORK_ORDER_CREATE_ERP_DATA_MISSING;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_WORK_ORDER_CREATE_ERP_DUPLICATE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_WORK_ORDER_CREATE_ERP_STATUS_INVALID;

@Service
@Validated
@RequiredArgsConstructor
public class MesKingdeeProductionOrderCreateServiceImpl implements MesKingdeeProductionOrderCreateService {

    private final ErpKingdeeProductionOrderClient productionOrderClient;
    private final ErpKingdeeConfigService kingdeeConfigService;
    private final MesProWorkOrderService workOrderService;
    private final MesMdItemService itemService;
    private final MesMdUnitMeasureService unitMeasureService;
    private final MesKingdeeProductionOrderSyncRecordMapper syncRecordMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesKingdeeProductionOrderCreateResult createAndSubmitProductionOrder(Long workOrderId) {
        kingdeeConfigService.assertExternalWriteEnabled();
        MesProWorkOrderDO workOrder = workOrderService.validateWorkOrderExists(workOrderId);
        validateWorkOrderEligible(workOrder);
        MesMdItemDO product = validateProduct(workOrder);
        MesMdUnitMeasureDO unitMeasure = validateUnitMeasure(product);
        ErpKingdeeProperties kingdeeProperties = kingdeeConfigService.getEffectiveProperties();
        kingdeeProperties.validateProductionOrderCreateConfig();
        String testBillNo = generateTestBillNo(workOrder);
        BigDecimal testQuantity = generateTestQuantity();

        ErpKingdeeProductionOrder existingOrder =
                productionOrderClient.getProductionOrderByBillNo(kingdeeProperties, testBillNo);
        if (existingOrder != null) {
            throw exception(PRO_WORK_ORDER_CREATE_ERP_DUPLICATE, testBillNo);
        }

        ErpKingdeeProductionOrderCreateResult erpResult = productionOrderClient.createAndSubmitProductionOrder(
                kingdeeProperties, buildCreateRequest(kingdeeProperties, workOrder, product, unitMeasure, testBillNo, testQuantity));
        return MesKingdeeProductionOrderCreateResult.builder()
                .workOrderId(workOrderId)
                .erpFid(erpResult.getErpFid())
                .erpBillNo(erpResult.getErpBillNo())
                .saved(erpResult.getSaved())
                .submitted(erpResult.getSubmitted())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesKingdeeProductionOrderCreateResult createAndSubmitProductionOrder(
            Long workOrderId, MesKingdeeProductionOrderDeterministicCreateCommand command) {
        validateDeterministicCommand(command);
        kingdeeConfigService.assertExternalWriteEnabled();
        MesProWorkOrderDO workOrder = workOrderService.validateWorkOrderExists(workOrderId);
        validateWorkOrderEligible(workOrder);
        MesMdItemDO product = validateProduct(workOrder);
        MesMdUnitMeasureDO unitMeasure = validateUnitMeasure(product);
        ErpKingdeeProperties kingdeeProperties = kingdeeConfigService.getEffectiveProperties();
        kingdeeProperties.validateProductionOrderCreateConfig();
        String testBillNo = command.getRunId().trim() + "-" + command.getSlot().trim();
        ErpKingdeeProductionOrder existingOrder =
                productionOrderClient.getProductionOrderByBillNo(kingdeeProperties, testBillNo);
        if (existingOrder != null) {
            throw exception(PRO_WORK_ORDER_CREATE_ERP_DUPLICATE, testBillNo);
        }
        ErpKingdeeProductionOrderCreateResult erpResult = productionOrderClient.createAndSubmitProductionOrder(
                kingdeeProperties, buildCreateRequest(kingdeeProperties, workOrder, product, unitMeasure,
                        testBillNo, command.getQuantity(), command.getBatchNumber()));
        return MesKingdeeProductionOrderCreateResult.builder()
                .workOrderId(workOrderId)
                .erpFid(erpResult.getErpFid())
                .erpBillNo(erpResult.getErpBillNo())
                .saved(erpResult.getSaved())
                .submitted(erpResult.getSubmitted())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesKingdeeProductionOrderCreateResult createAndSubmitProductionOrder(
            MesKingdeeProductionOrderDeterministicCreateCommand command) {
        validateDeterministicCommand(command);
        kingdeeConfigService.assertExternalWriteEnabled();
        ErpKingdeeProperties kingdeeProperties = kingdeeConfigService.getEffectiveProperties();
        kingdeeProperties.validateProductionOrderCreateConfig();
        String templateBillNo = kingdeeProperties.getProductionOrder().getTemplateBillNo().trim();
        ErpKingdeeProductionOrder templateOrder =
                productionOrderClient.getProductionOrderByBillNo(kingdeeProperties, templateBillNo);
        validateTemplateProductionOrder(templateOrder, templateBillNo);
        String testBillNo = command.getRunId().trim() + "-" + command.getSlot().trim();
        ErpKingdeeProductionOrder existingOrder =
                productionOrderClient.getProductionOrderByBillNo(kingdeeProperties, testBillNo);
        if (existingOrder != null) {
            throw exception(PRO_WORK_ORDER_CREATE_ERP_DUPLICATE, testBillNo);
        }
        ErpKingdeeProductionOrderCreateResult erpResult = productionOrderClient.createAndSubmitProductionOrder(
                kingdeeProperties, buildCreateRequest(kingdeeProperties, templateOrder, testBillNo,
                        command.getQuantity(), command.getBatchNumber()));
        return MesKingdeeProductionOrderCreateResult.builder()
                .erpFid(erpResult.getErpFid())
                .erpBillNo(erpResult.getErpBillNo())
                .saved(erpResult.getSaved())
                .submitted(erpResult.getSubmitted())
                .build();
    }

    private void validateDeterministicCommand(MesKingdeeProductionOrderDeterministicCreateCommand command) {
        if (command == null || StrUtil.isBlank(command.getRunId()) || StrUtil.isBlank(command.getSlot())
                || StrUtil.isBlank(command.getBatchNumber()) || command.getQuantity() == null
                || command.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(PRO_WORK_ORDER_CREATE_ERP_DATA_MISSING,
                    "AI E2E runId、slot、batchNumber和正数quantity均不能为空");
        }
        String slot = command.getSlot().trim();
        if (!slot.matches("O0[1-5]")) {
            throw exception(PRO_WORK_ORDER_CREATE_ERP_DATA_MISSING, "AI E2E slot必须为O01至O05");
        }
        requireNoSingleQuote(command.getRunId().trim(), "AI E2E runId");
        requireNoSingleQuote(slot, "AI E2E slot");
        requireNoSingleQuote(command.getBatchNumber().trim(), "AI E2E batchNumber");
    }

    private void validateWorkOrderEligible(MesProWorkOrderDO workOrder) {
        if (!MesProWorkOrderStatusEnum.CONFIRMED.getStatus().equals(workOrder.getStatus())) {
            throw exception(PRO_WORK_ORDER_CREATE_ERP_STATUS_INVALID, "仅已确认工单允许创建 ERP 生产订单");
        }
        if (!MesProWorkOrderTypeEnum.SELF.getType().equals(workOrder.getType())) {
            throw exception(PRO_WORK_ORDER_CREATE_ERP_STATUS_INVALID, "仅自行生产工单允许创建 ERP 生产订单");
        }
        if (Boolean.TRUE.equals(workOrder.getTemporaryFrozen())) {
            throw exception(PRO_WORK_ORDER_CREATE_ERP_STATUS_INVALID, "冻结工单不允许创建 ERP 生产订单");
        }
        requireText(workOrder.getCode(), "工单编码");
        requireNoSingleQuote(workOrder.getCode(), "工单编码");
        if (workOrder.getProductId() == null) {
            throw exception(PRO_WORK_ORDER_CREATE_ERP_DATA_MISSING, "产品");
        }
        if (workOrder.getQuantity() == null || workOrder.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(PRO_WORK_ORDER_CREATE_ERP_DATA_MISSING, "工单数量");
        }
        if (workOrder.getRequestDate() == null) {
            throw exception(PRO_WORK_ORDER_CREATE_ERP_DATA_MISSING, "需求日期");
        }
        if (StrUtil.isNotBlank(workOrder.getOrderSourceCode())) {
            requireNoSingleQuote(workOrder.getOrderSourceCode(), "来源单据编号");
        }
    }

    private MesMdItemDO validateProduct(MesProWorkOrderDO workOrder) {
        MesMdItemDO product = itemService.getItem(workOrder.getProductId());
        if (product == null) {
            throw exception(PRO_WORK_ORDER_CREATE_ERP_DATA_MISSING, "产品");
        }
        requireText(product.getCode(), "产品编码");
        requireNoSingleQuote(product.getCode(), "产品编码");
        if (product.getUnitMeasureId() == null) {
            throw exception(PRO_WORK_ORDER_CREATE_ERP_DATA_MISSING, "产品计量单位");
        }
        return product;
    }

    private MesMdUnitMeasureDO validateUnitMeasure(MesMdItemDO product) {
        MesMdUnitMeasureDO unitMeasure = unitMeasureService.getUnitMeasure(product.getUnitMeasureId());
        if (unitMeasure == null) {
            throw exception(PRO_WORK_ORDER_CREATE_ERP_DATA_MISSING, "产品计量单位");
        }
        requireText(unitMeasure.getCode(), "计量单位编码");
        requireNoSingleQuote(unitMeasure.getCode(), "计量单位编码");
        return unitMeasure;
    }

    private void validateTemplateProductionOrder(ErpKingdeeProductionOrder templateOrder, String templateBillNo) {
        if (templateOrder == null) {
            throw exception(PRO_WORK_ORDER_CREATE_ERP_DATA_MISSING, "ERP生产订单模板：" + templateBillNo);
        }
        requireText(templateOrder.getMaterialNumber(), "ERP生产订单模板物料编码");
        requireNoSingleQuote(templateOrder.getMaterialNumber(), "ERP生产订单模板物料编码");
        if (StrUtil.isNotBlank(templateOrder.getUnitCode())) {
            requireNoSingleQuote(templateOrder.getUnitCode(), "ERP生产订单模板计量单位编码");
        }
        if (templateOrder.getPlannedStartDate() == null || templateOrder.getPlannedEndDate() == null) {
            throw exception(PRO_WORK_ORDER_CREATE_ERP_DATA_MISSING, "ERP生产订单模板计划日期");
        }
        if (StrUtil.isNotBlank(templateOrder.getSourceBillNo())) {
            requireNoSingleQuote(templateOrder.getSourceBillNo(), "ERP生产订单模板来源单据编号");
        }
    }

    String generateTestBillNo(MesProWorkOrderDO workOrder) {
        String suffix = IdUtil.fastSimpleUUID().substring(0, 12).toUpperCase(Locale.ROOT);
        return "TESTERP" + suffix;
    }

    BigDecimal generateTestQuantity() {
        return BigDecimal.valueOf(ThreadLocalRandom.current().nextInt(10, 1001));
    }

    private ErpKingdeeProductionOrderCreateRequest buildCreateRequest(ErpKingdeeProperties kingdeeProperties,
                                                                      MesProWorkOrderDO workOrder,
                                                                      MesMdItemDO product,
                                                                      MesMdUnitMeasureDO unitMeasure,
                                                                      String billNo,
                                                                      BigDecimal quantity) {
        return buildCreateRequest(kingdeeProperties, workOrder, product, unitMeasure, billNo, quantity, null);
    }

    private ErpKingdeeProductionOrderCreateRequest buildCreateRequest(ErpKingdeeProperties kingdeeProperties,
                                                                      MesProWorkOrderDO workOrder,
                                                                      MesMdItemDO product,
                                                                      MesMdUnitMeasureDO unitMeasure,
                                                                      String billNo,
                                                                      BigDecimal quantity,
                                                                      String batchNumber) {
        return ErpKingdeeProductionOrderCreateRequest.builder()
                .billNo(billNo)
                .templateBillNo(kingdeeProperties.getProductionOrder().getTemplateBillNo())
                .materialNumber(product.getCode())
                .unitNumber(unitMeasure.getCode())
                .quantity(quantity)
                .plannedStartDate(workOrder.getRequestDate())
                .plannedFinishDate(workOrder.getRequestDate())
                .sourceBillNo(workOrder.getOrderSourceCode())
                .batchNumber(batchNumber)
                .build();
    }

    private ErpKingdeeProductionOrderCreateRequest buildCreateRequest(ErpKingdeeProperties kingdeeProperties,
                                                                      ErpKingdeeProductionOrder templateOrder,
                                                                      String billNo,
                                                                      BigDecimal quantity,
                                                                      String batchNumber) {
        String unitCode = StrUtil.isBlank(templateOrder.getUnitCode()) ? null : templateOrder.getUnitCode().trim();
        return ErpKingdeeProductionOrderCreateRequest.builder()
                .billNo(billNo)
                .templateBillNo(kingdeeProperties.getProductionOrder().getTemplateBillNo())
                .materialNumber(templateOrder.getMaterialNumber())
                .productionOrgNumber(StrUtil.trimToNull(templateOrder.getProductionOrgNumber()))
                .unitNumber(unitCode)
                .useTemplateEntryUnit(unitCode == null ? Boolean.TRUE : null)
                .quantity(quantity)
                .plannedStartDate(templateOrder.getPlannedStartDate())
                .plannedFinishDate(templateOrder.getPlannedEndDate())
                .sourceBillNo(templateOrder.getSourceBillNo())
                .batchNumber(batchNumber)
                .build();
    }

    private void requireText(String value, String fieldName) {
        if (StrUtil.isBlank(value)) {
            throw exception(PRO_WORK_ORDER_CREATE_ERP_DATA_MISSING, fieldName);
        }
    }

    private void requireNoSingleQuote(String value, String fieldName) {
        if (value.contains("'")) {
            throw exception(PRO_WORK_ORDER_CREATE_ERP_DATA_MISSING, fieldName + "不能包含单引号");
        }
    }

}
