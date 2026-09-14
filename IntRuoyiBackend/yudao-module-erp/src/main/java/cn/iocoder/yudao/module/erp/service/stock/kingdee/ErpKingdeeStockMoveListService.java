package cn.iocoder.yudao.module.erp.service.stock.kingdee;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.kingdee.move.ErpKingdeeStockMovePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.kingdee.move.ErpKingdeeStockMoveRespVO;
import cn.iocoder.yudao.module.erp.service.stock.sync.ErpKingdeeStockMoveSyncResult;

import java.time.LocalDateTime;

public interface ErpKingdeeStockMoveListService {

    PageResult<ErpKingdeeStockMoveRespVO> getPage(ErpKingdeeStockMovePageReqVO pageReqVO);

    ErpKingdeeStockMoveSyncResult syncAll();

    ErpKingdeeStockMoveSyncResult syncAllSkipExisting();

    ErpKingdeeStockMoveSyncResult syncModifiedBetween(LocalDateTime windowStart, LocalDateTime windowEnd);

}
