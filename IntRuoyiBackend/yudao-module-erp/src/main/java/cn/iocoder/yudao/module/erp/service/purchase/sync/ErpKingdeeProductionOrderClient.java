package cn.iocoder.yudao.module.erp.service.purchase.sync;

import java.util.List;
import java.util.Collection;
import java.time.LocalDate;
import java.time.LocalDateTime;

public interface ErpKingdeeProductionOrderClient {

    List<ErpKingdeeProductionOrder> fetchProductionOrders(ErpKingdeeProperties properties);

    List<ErpKingdeeProductionOrder> fetchProductionOrdersByBillDateRange(ErpKingdeeProperties properties,
                                                                     LocalDate fromDate,
                                                                     LocalDate toDate);

    List<ErpKingdeeProductionOrder> fetchProductionOrdersModifiedBetween(ErpKingdeeProperties properties,
                                                                         LocalDateTime windowStart,
                                                                         LocalDateTime windowEnd);

    List<ErpKingdeeProductionOrder> fetchProductionOrdersByBillNos(ErpKingdeeProperties properties,
                                                                    Collection<String> billNos);

    ErpKingdeeProductionOrder getProductionOrderByBillNo(ErpKingdeeProperties properties, String billNo);

    /** 查询当前账套实际存在的订单编号，不按日期、物料或单据状态过滤。 */
    java.util.Set<String> findExistingProductionOrderBillNos(ErpKingdeeProperties properties,
                                                            Collection<String> billNos);

    ErpKingdeeProductionOrderCreateResult createAndSubmitProductionOrder(
            ErpKingdeeProperties properties, ErpKingdeeProductionOrderCreateRequest request);

}
