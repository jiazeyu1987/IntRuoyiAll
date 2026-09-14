package cn.iocoder.yudao.module.erp.service.nastablesync;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.nastablesync.vo.ErpNasTableSyncPlanRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.nastablesync.vo.ErpNasTableSyncPlanSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.nastablesync.vo.ErpNasTableSyncRunOnceRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.nastablesync.vo.ErpNasTableSyncRunPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.nastablesync.vo.ErpNasTableSyncRunRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.nastablesync.vo.ErpNasTableSyncTestWriteReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.nastablesync.vo.ErpNasTableSyncTestWriteRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.nastablesync.vo.ErpNasTableSyncTypeRespVO;

import java.util.List;

public interface ErpNasTableSyncService {

    ErpNasTableSyncPlanRespVO getPlan();

    ErpNasTableSyncPlanRespVO savePlan(ErpNasTableSyncPlanSaveReqVO reqVO);

    List<ErpNasTableSyncTypeRespVO> getSyncTypes();

    ErpNasTableSyncTestWriteRespVO testNasWrite(ErpNasTableSyncTestWriteReqVO reqVO);

    ErpNasTableSyncRunOnceRespVO runOnce();

    PageResult<ErpNasTableSyncRunRespVO> getRunPage(ErpNasTableSyncRunPageReqVO pageReqVO);

    String executeAutoForCurrentTenant();
}
