package cn.iocoder.yudao.module.erp.service.production.kingdee;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.production.vo.ErpProductionPickListPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.production.vo.ErpProductionPickListRespVO;
import cn.iocoder.yudao.module.erp.service.production.sync.ErpKingdeeProductionPickListSyncResult;

import java.time.LocalDateTime;

public interface ErpKingdeeProductionPickListService {

    PageResult<ErpProductionPickListRespVO> getPage(
            ErpProductionPickListPageReqVO pageReqVO);

    ErpKingdeeProductionPickListSyncResult syncAll();

    ErpKingdeeProductionPickListSyncResult syncAllSkipExisting();

    ErpKingdeeProductionPickListSyncResult syncModifiedBetween(
            LocalDateTime windowStart, LocalDateTime windowEnd);

}
