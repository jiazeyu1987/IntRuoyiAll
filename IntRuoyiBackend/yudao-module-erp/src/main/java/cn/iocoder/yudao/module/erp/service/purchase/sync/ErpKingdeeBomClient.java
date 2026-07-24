package cn.iocoder.yudao.module.erp.service.purchase.sync;

import java.time.LocalDateTime;
import java.util.List;

public interface ErpKingdeeBomClient {

    List<ErpKingdeeBomLine> fetchApprovedBomByParentMaterialNumber(ErpKingdeeProperties properties,
                                                                   String parentMaterialNumber);

    List<ErpKingdeeBomLine> fetchBomLines(ErpKingdeeProperties properties);

    List<ErpKingdeeBomLine> fetchBomLinesModifiedBetween(ErpKingdeeProperties properties,
                                                         LocalDateTime windowStart,
                                                         LocalDateTime windowEnd);

}
