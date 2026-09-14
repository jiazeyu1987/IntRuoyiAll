package cn.iocoder.yudao.module.erp.service.kingdeeautosync;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.kingdeeautosync.vo.ErpKingdeeTableAutoSyncPlanRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.kingdeeautosync.vo.ErpKingdeeTableAutoSyncPlanSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.kingdeeautosync.vo.ErpKingdeeTableAutoSyncRunOnceRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.kingdeeautosync.vo.ErpKingdeeTableAutoSyncTypeRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sync.vo.ErpKingdeeSyncRunPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sync.vo.ErpKingdeeSyncRunRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sync.vo.ErpKingdeeSyncWatermarkRespVO;

import java.util.List;

public interface ErpKingdeeTableAutoSyncService {

    ErpKingdeeTableAutoSyncPlanRespVO getPlan();

    ErpKingdeeTableAutoSyncPlanRespVO savePlan(ErpKingdeeTableAutoSyncPlanSaveReqVO reqVO);

    List<ErpKingdeeTableAutoSyncTypeRespVO> getSyncTypes();

    ErpKingdeeTableAutoSyncRunOnceRespVO runOnce();

    PageResult<ErpKingdeeSyncRunRespVO> getRunPage(ErpKingdeeSyncRunPageReqVO pageReqVO);

    List<ErpKingdeeSyncWatermarkRespVO> getWatermarks();

    String executeAutoForCurrentTenant();
}
