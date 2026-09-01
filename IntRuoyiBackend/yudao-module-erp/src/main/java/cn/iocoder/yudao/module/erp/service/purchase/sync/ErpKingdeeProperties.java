package cn.iocoder.yudao.module.erp.service.purchase.sync;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.KINGDEE_PRODUCTION_ORDER_CONFIG_INVALID;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.KINGDEE_PRODUCTION_ORDER_CONFIG_MISSING;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.KINGDEE_PURCHASE_ORDER_CONFIG_INVALID;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.KINGDEE_PURCHASE_ORDER_CONFIG_MISSING;

@Data
@Component
@ConfigurationProperties(prefix = "yudao.erp.kingdee")
public class ErpKingdeeProperties {

    private String baseUrl;
    private String acctId;
    private String username;
    private String password;
    private String appId;
    private String signedData;
    private String timestamp;
    private Integer lcid;
    private ProductProperties product = new ProductProperties();
    private BomProperties bom = new BomProperties();
    private ProductionOrderProperties productionOrder = new ProductionOrderProperties();
    private PurchaseOrderProperties purchaseOrder = new PurchaseOrderProperties();
    private SaleOrderProperties saleOrder = new SaleOrderProperties();

    public void validateBaseConfig() {
        requireNotBlank(baseUrl, "yudao.erp.kingdee.base-url");
        requireNotBlank(acctId, "yudao.erp.kingdee.acct-id");
        requireNotBlank(username, "yudao.erp.kingdee.username");
        requireNotBlank(password, "yudao.erp.kingdee.password");
        if (lcid == null) {
            throw exception(KINGDEE_PURCHASE_ORDER_CONFIG_MISSING, "yudao.erp.kingdee.lcid");
        }
    }

    public void validatePurchaseOrderSyncConfig() {
        validateBaseConfig();
        if (purchaseOrder == null) {
            throw exception(KINGDEE_PURCHASE_ORDER_CONFIG_MISSING, "yudao.erp.kingdee.purchase-order");
        }
        requireNotBlank(purchaseOrder.getPurchaseOrgNumber(),
                "yudao.erp.kingdee.purchase-order.purchase-org-number");
        requireNoSingleQuote(purchaseOrder.getPurchaseOrgNumber(),
                "yudao.erp.kingdee.purchase-order.purchase-org-number");
        requirePositive(purchaseOrder.getQueryDays(), "yudao.erp.kingdee.purchase-order.query-days");
        requirePositive(purchaseOrder.getQueryLimit(), "yudao.erp.kingdee.purchase-order.query-limit");
    }

    public void validateProductionOrderSyncConfig() {
        validateBaseConfig();
        if (productionOrder == null) {
            throw exception(KINGDEE_PRODUCTION_ORDER_CONFIG_MISSING, "yudao.erp.kingdee.production-order");
        }
        requireProductionPositive(productionOrder.getQueryLimit(), "yudao.erp.kingdee.production-order.query-limit");
    }

    public void validateProductionOrderCreateConfig() {
        validateProductionOrderSyncConfig();
        requireProductionNotBlank(productionOrder.getTemplateBillNo(),
                "yudao.erp.kingdee.production-order.template-bill-no");
        requireProductionNoSingleQuote(productionOrder.getTemplateBillNo(),
                "yudao.erp.kingdee.production-order.template-bill-no");
    }

    public void validateBomSyncConfig() {
        validateBaseConfig();
        if (bom == null) {
            throw exception(KINGDEE_PURCHASE_ORDER_CONFIG_MISSING, "yudao.erp.kingdee.bom");
        }
        requirePositive(bom.getQueryLimit(), "yudao.erp.kingdee.bom.query-limit");
    }

    public void validateProductSyncConfig() {
        validateBaseConfig();
        if (product == null) {
            throw exception(KINGDEE_PURCHASE_ORDER_CONFIG_MISSING, "yudao.erp.kingdee.product");
        }
        requirePositive(product.getQueryLimit(), "yudao.erp.kingdee.product.query-limit");
    }

    public void validateSaleOrderSyncConfig() {
        validateBaseConfig();
        if (saleOrder == null) {
            throw exception(KINGDEE_PURCHASE_ORDER_CONFIG_MISSING, "yudao.erp.kingdee.sale-order");
        }
        requirePositive(saleOrder.getQueryDays(), "yudao.erp.kingdee.sale-order.query-days");
        requirePositive(saleOrder.getQueryLimit(), "yudao.erp.kingdee.sale-order.query-limit");
    }

    public Long getSupplierId(String supplierNumber) {
        Map<String, Long> supplierMappings = purchaseOrder.getSupplierMappings();
        return supplierMappings == null ? null : supplierMappings.get(supplierNumber);
    }

    public Long getProductId(String materialNumber) {
        Map<String, Long> materialMappings = purchaseOrder.getMaterialMappings();
        return materialMappings == null ? null : materialMappings.get(materialNumber);
    }

    private static void requireNotBlank(String value, String propertyName) {
        if (StrUtil.isBlank(value)) {
            throw exception(KINGDEE_PURCHASE_ORDER_CONFIG_MISSING, propertyName);
        }
    }

    private static void requirePositive(Integer value, String propertyName) {
        if (value == null) {
            throw exception(KINGDEE_PURCHASE_ORDER_CONFIG_MISSING, propertyName);
        }
        if (value <= 0) {
            throw exception(KINGDEE_PURCHASE_ORDER_CONFIG_INVALID, propertyName);
        }
    }

    private static void requireNoSingleQuote(String value, String propertyName) {
        if (value.contains("'")) {
            throw exception(KINGDEE_PURCHASE_ORDER_CONFIG_INVALID, propertyName);
        }
    }

    private static void requireProductionNotBlank(String value, String propertyName) {
        if (StrUtil.isBlank(value)) {
            throw exception(KINGDEE_PRODUCTION_ORDER_CONFIG_MISSING, propertyName);
        }
    }

    private static void requireProductionPositive(Integer value, String propertyName) {
        if (value == null) {
            throw exception(KINGDEE_PRODUCTION_ORDER_CONFIG_MISSING, propertyName);
        }
        if (value <= 0) {
            throw exception(KINGDEE_PRODUCTION_ORDER_CONFIG_INVALID, propertyName);
        }
    }

    private static void requireProductionNoSingleQuote(String value, String propertyName) {
        if (value.contains("'")) {
            throw exception(KINGDEE_PRODUCTION_ORDER_CONFIG_INVALID, propertyName);
        }
    }

    @Data
    public static class PurchaseOrderProperties {

        private String purchaseOrgNumber;
        private Integer queryDays;
        private Integer queryLimit;
        private Map<String, Long> supplierMappings = new HashMap<>();
        private Map<String, Long> materialMappings = new HashMap<>();

    }

    @Data
    public static class ProductionOrderProperties {

        private Integer queryLimit;
        private String templateBillNo;

    }

    @Data
    public static class SaleOrderProperties {

        private Integer queryDays;
        private Integer queryLimit;

    }

    @Data
    public static class ProductProperties {

        private Integer queryLimit;

    }

    @Data
    public static class BomProperties {

        private Integer queryLimit;

    }

}
