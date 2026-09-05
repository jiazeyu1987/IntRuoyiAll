package cn.iocoder.yudao.module.erp.service.production.kingdee;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.production.vo.ErpProductionReplenishmentListPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.production.vo.ErpProductionReplenishmentListRespVO;
import cn.iocoder.yudao.module.erp.service.production.sync.ErpKingdeeProductionReplenishmentListSyncResult;

import java.time.LocalDateTime;

public interface ErpKingdeeProductionReplenishmentListService {

    PageResult<ErpProductionReplenishmentListRespVO> getPage(
            ErpProductionReplenishmentListPageReqVO pageReqVO);

    ErpKingdeeProductionReplenishmentListSyncResult syncAll(
            LocalDateTime windowStart, LocalDateTime windowEnd);

    ErpKingdeeProductionReplenishmentListSyncResult syncAllSkipExisting(
            LocalDateTime windowStart, LocalDateTime windowEnd);

    ErpKingdeeProductionReplenishmentListSyncResult syncModifiedBetween(
            LocalDateTime windowStart, LocalDateTime windowEnd);

}
