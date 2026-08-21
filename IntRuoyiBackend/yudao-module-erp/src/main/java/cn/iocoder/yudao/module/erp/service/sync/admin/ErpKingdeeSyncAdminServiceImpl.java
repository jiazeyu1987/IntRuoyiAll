package cn.iocoder.yudao.module.erp.service.sync.admin;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.sync.vo.ErpKingdeeFullSyncRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sync.vo.ErpKingdeeSyncRunPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sync.vo.ErpKingdeeSyncRunRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sync.vo.ErpKingdeeSyncWatermarkRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sync.ErpKingdeeSyncRunDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sync.ErpKingdeeSyncRunMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sync.ErpKingdeeSyncWatermarkMapper;
import cn.iocoder.yudao.module.erp.enums.kingdeeautosync.ErpKingdeeTableAutoSyncTypeEnum;
import cn.iocoder.yudao.module.erp.service.config.ErpKingdeeConfigService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.KINGDEE_TABLE_AUTO_SYNC_JOB_HANDLER_INVALID;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.KINGDEE_TABLE_AUTO_SYNC_JOB_HANDLER_MISSING;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.KINGDEE_TABLE_AUTO_SYNC_TYPE_UNSUPPORTED;

@Service
@Validated
public class ErpKingdeeSyncAdminServiceImpl implements ErpKingdeeSyncAdminService {

    @Resource
    private ErpKingdeeSyncRunMapper runMapper;
    @Resource
    private ErpKingdeeSyncWatermarkMapper watermarkMapper;
    @Resource
    private ErpKingdeeConfigService kingdeeConfigService;

    @Override
    public PageResult<ErpKingdeeSyncRunRespVO> getRunPage(ErpKingdeeSyncRunPageReqVO pageReqVO) {
        PageResult<ErpKingdeeSyncRunDO> pageResult = runMapper.selectPage(pageReqVO);
        return new PageResult<>(BeanUtils.toBean(pageResult.getList(), ErpKingdeeSyncRunRespVO.class),
                pageResult.getTotal());
    }

    @Override
    public List<ErpKingdeeSyncWatermarkRespVO> getWatermarks() {
        return BeanUtils.toBean(watermarkMapper.selectListOrderBySyncType(), ErpKingdeeSyncWatermarkRespVO.class);
    }

    @Override
    public ErpKingdeeFullSyncRespVO runFullSync(String syncType) {
        kingdeeConfigService.getActiveConnection();
        ErpKingdeeTableAutoSyncTypeEnum type = resolveSyncType(syncType);
        Object bean;
        try {
            bean = SpringUtil.getBean(type.getHandlerName());
        } catch (Exception ex) {
            throw exception(KINGDEE_TABLE_AUTO_SYNC_JOB_HANDLER_MISSING, type.getHandlerName());
        }
        if (!(bean instanceof ErpKingdeeFullSyncHandler handler)) {
            throw exception(KINGDEE_TABLE_AUTO_SYNC_JOB_HANDLER_INVALID, type.getHandlerName());
        }
        return new ErpKingdeeFullSyncRespVO(type.getSyncType(), type.getHandlerName(), handler.executeFullSync());
    }

    private ErpKingdeeTableAutoSyncTypeEnum resolveSyncType(String syncType) {
        try {
            return ErpKingdeeTableAutoSyncTypeEnum.requiredOf(StrUtil.trim(syncType));
        } catch (IllegalArgumentException ex) {
            throw exception(KINGDEE_TABLE_AUTO_SYNC_TYPE_UNSUPPORTED, syncType);
        }
    }
}