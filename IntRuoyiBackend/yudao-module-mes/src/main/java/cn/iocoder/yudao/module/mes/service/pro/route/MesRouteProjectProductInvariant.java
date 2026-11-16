package cn.iocoder.yudao.module.mes.service.pro.route;

import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProductDO;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_DCC_ROUTE_ITEM_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_ROUTE_DCC_ROUTE_PRODUCT_REQUIRED;

public final class MesRouteProjectProductInvariant {

    private MesRouteProjectProductInvariant() {
    }

    public static void requireProducts(Long routeId, List<MesProRouteProductDO> routeProducts,
                                       Map<Long, MesMdItemDO> itemsById) {
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
        // The route's explicit DCC project binding is the production identity.
        // MDM belongs to showroom data and must not determine route eligibility.
        for (Long itemId : itemIds) {
            if (itemsById.get(itemId) == null) {
                throw exception(PRO_ROUTE_DCC_ROUTE_ITEM_INVALID, routeId, itemIds);
            }
        }
    }
}
