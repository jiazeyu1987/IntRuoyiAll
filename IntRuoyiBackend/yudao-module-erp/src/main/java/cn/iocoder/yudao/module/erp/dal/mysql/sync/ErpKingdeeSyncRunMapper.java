package cn.iocoder.yudao.module.erp.dal.mysql.sync;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.sync.vo.ErpKingdeeSyncRunPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sync.ErpKingdeeSyncRunDO;
import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncRunStatusEnum;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ErpKingdeeSyncRunMapper extends BaseMapperX<ErpKingdeeSyncRunDO> {

    default ErpKingdeeSyncRunDO selectRunningBySyncType(String syncType) {
        return selectOne(new LambdaQueryWrapperX<ErpKingdeeSyncRunDO>()
                .eq(ErpKingdeeSyncRunDO::getSyncType, syncType)
                .in(ErpKingdeeSyncRunDO::getStatus, 10, 40));
    }

    default int addProductionMaterialListProgress(Long runId, int created, int updated, int skipped) {
        return update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ErpKingdeeSyncRunDO>()
                .eq(ErpKingdeeSyncRunDO::getId, runId)
                .eq(ErpKingdeeSyncRunDO::getSyncType,
                        cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTypeEnum.PRODUCTION_MATERIAL_LIST.getType())
                .eq(ErpKingdeeSyncRunDO::getStatus, ErpKingdeeSyncRunStatusEnum.RUNNING.getStatus())
                .setSql("created_count = created_count + {0}", created)
                .setSql("updated_count = updated_count + {0}", updated)
                .setSql("skipped_count = skipped_count + {0}", skipped));
    }

    default PageResult<ErpKingdeeSyncRunDO> selectPage(ErpKingdeeSyncRunPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpKingdeeSyncRunDO> query = new LambdaQueryWrapperX<ErpKingdeeSyncRunDO>()
                .eqIfPresent(ErpKingdeeSyncRunDO::getSyncType, reqVO.getSyncType())
                .betweenIfPresent(ErpKingdeeSyncRunDO::getStartedAt, reqVO.getStartedAt())
                .orderByDesc(ErpKingdeeSyncRunDO::getId);
        if (Integer.valueOf(10).equals(reqVO.getStatus())) query.in(ErpKingdeeSyncRunDO::getStatus, 10, 40);
        else query.eqIfPresent(ErpKingdeeSyncRunDO::getStatus, reqVO.getStatus());
        return selectPage(reqVO, query);
    }

}
