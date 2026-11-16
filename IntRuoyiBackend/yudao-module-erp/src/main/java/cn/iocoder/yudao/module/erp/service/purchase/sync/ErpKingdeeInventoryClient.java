package cn.iocoder.yudao.module.erp.service.purchase.sync;

import java.time.LocalDateTime;
import java.util.List;

public interface ErpKingdeeInventoryClient {

    List<ErpKingdeeInventoryRow> fetchInventoryRows(ErpKingdeeProperties properties);

    /** 查询受影响物料在指定组织、仓库的全部批次，包含零库存。 */
    List<ErpKingdeeInventoryRow> fetchInventoryGroup(ErpKingdeeProperties properties,
            String materialNumber, String stockOrgNumber, String warehouseNumber);

    List<ErpKingdeeInventoryRow> fetchInventoryRowsModifiedBetween(ErpKingdeeProperties properties,
                                                                   LocalDateTime windowStart,
                                                                   LocalDateTime windowEnd);


    List<ErpKingdeeInventoryRow> fetchInventoryRowsByCode(ErpKingdeeProperties properties, String code);
}
