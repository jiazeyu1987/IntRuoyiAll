package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductDO;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_DCC_ITEM_PRODUCT_MASTER_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_DCC_PRODUCT_MASTER_AMBIGUOUS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_DCC_PRODUCT_MASTER_MISMATCH;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_DCC_PROJECT_PRODUCT_MASTER_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_DCC_ROUTE_ITEM_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_DCC_ROUTE_PRODUCT_REQUIRED;

public final class MesRouteDccProductMasterInvariant {

    private MesRouteDccProductMasterInvariant() {
    }

    public static void requireMatching(Long routeId, List<MesProRouteProductDO> routeProducts,
                                       Map<Long, MesMdItemDO> itemsById, DccProjectCodeDO projectCode) {
        if (routeProducts == null || routeProducts.isEmpty()) {
            throw exception(PRO_ROUTE_DCC_ROUTE_PRODUCT_REQUIRED, routeId);
        }
        List<Long> itemIds = routeProducts.stream()
                .map(MesProRouteProductDO::getItemId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (itemIds.isEmpty() || routeProducts.stream().anyMatch(product -> product.getItemId() == null)
                || itemsById.size() != itemIds.size()) {
            throw exception(PRO_ROUTE_DCC_ROUTE_ITEM_INVALID, routeId, itemIds);
        }
        Set<Long> productMasterIds = new LinkedHashSet<>();
        for (Long itemId : itemIds) {
            MesMdItemDO item = itemsById.get(itemId);
            if (item == null) {
                throw exception(PRO_ROUTE_DCC_ROUTE_ITEM_INVALID, routeId, itemIds);
            }
            if (item.getProductMasterId() == null) {
                throw exception(PRO_ROUTE_DCC_ITEM_PRODUCT_MASTER_REQUIRED, routeId, itemId);
            }
            productMasterIds.add(item.getProductMasterId());
        }
        if (productMasterIds.size() != 1) {
            throw exception(PRO_ROUTE_DCC_PRODUCT_MASTER_AMBIGUOUS, routeId, productMasterIds);
        }
        if (projectCode.getProductMasterId() == null) {
            throw exception(PRO_ROUTE_DCC_PROJECT_PRODUCT_MASTER_REQUIRED, projectCode.getId());
        }
        Long routeProductMasterId = productMasterIds.iterator().next();
        if (!Objects.equals(routeProductMasterId, projectCode.getProductMasterId())) {
            throw exception(PRO_ROUTE_DCC_PRODUCT_MASTER_MISMATCH,
                    routeId, routeProductMasterId, projectCode.getProductMasterId());
        }
    }
}
