package cn.iocoder.yudao.module.erp.service.purchase.sync;

import java.util.List;
import java.util.Collection;
import java.time.LocalDate;
import java.time.LocalDateTime;

public interface ErpKingdeeProductionOrderClient {

    List<ErpKingdeeProductionOrder> fetchProductionOrders(ErpKingdeeProperties properties);

    List<ErpKingdeeProductionOrder> fetchUnfinishedProductionOrders(ErpKingdeeProperties properties,
                                                                     LocalDate fromDate,
                                                                     LocalDate toDate);

    List<ErpKingdeeProductionOrder> fetchProductionOrdersModifiedBetween(ErpKingdeeProperties properties,
                                                                         LocalDateTime windowStart,
                                                                         LocalDateTime windowEnd);

    List<ErpKingdeeProductionOrder> fetchProductionOrdersByBillNos(ErpKingdeeProperties properties,
                                                                    Collection<String> billNos);

    ErpKingdeeProductionOrder getProductionOrderByBillNo(ErpKingdeeProperties properties, String billNo);

    ErpKingdeeProductionOrderCreateResult createAndSubmitProductionOrder(
            ErpKingdeeProperties properties, ErpKingdeeProductionOrderCreateRequest request);

}
