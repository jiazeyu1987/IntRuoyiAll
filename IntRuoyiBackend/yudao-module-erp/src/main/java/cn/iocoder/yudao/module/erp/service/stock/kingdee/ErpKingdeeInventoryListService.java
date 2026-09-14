package cn.iocoder.yudao.module.erp.service.stock.kingdee;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.kingdee.ErpKingdeeInventoryListPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.kingdee.ErpKingdeeInventoryListRespVO;

import java.time.LocalDateTime;

public interface ErpKingdeeInventoryListService {

    PageResult<ErpKingdeeInventoryListRespVO> getPage(ErpKingdeeInventoryListPageReqVO pageReqVO);

    int syncAll();

    ErpKingdeeInventoryListSyncResult syncAllSkipExisting();

    int syncModifiedBetween(LocalDateTime windowStart, LocalDateTime windowEnd);

}
