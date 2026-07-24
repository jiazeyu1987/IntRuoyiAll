package cn.iocoder.yudao.module.erp.service.purchase.sync;

import java.time.LocalDateTime;
import java.util.List;

public interface ErpKingdeeSaleOrderClient {

    List<ErpKingdeeSaleOrder> fetchSaleOrders(ErpKingdeeProperties properties);

    List<ErpKingdeeSaleOrder> fetchSaleOrdersModifiedBetween(ErpKingdeeProperties properties,
                                                             LocalDateTime windowStart,
                                                             LocalDateTime windowEnd);

}
