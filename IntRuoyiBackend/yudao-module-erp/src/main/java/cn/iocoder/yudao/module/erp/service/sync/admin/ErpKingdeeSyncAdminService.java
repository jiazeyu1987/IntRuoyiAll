package cn.iocoder.yudao.module.erp.service.sync.admin;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.sync.vo.ErpKingdeeFullSyncRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sync.vo.ErpKingdeeSyncRunPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sync.vo.ErpKingdeeSyncRunRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sync.vo.ErpKingdeeSyncWatermarkRespVO;

import java.util.List;

public interface ErpKingdeeSyncAdminService {

    PageResult<ErpKingdeeSyncRunRespVO> getRunPage(ErpKingdeeSyncRunPageReqVO pageReqVO);

    List<ErpKingdeeSyncWatermarkRespVO> getWatermarks();

    ErpKingdeeFullSyncRespVO runFullSync(String syncType);
}