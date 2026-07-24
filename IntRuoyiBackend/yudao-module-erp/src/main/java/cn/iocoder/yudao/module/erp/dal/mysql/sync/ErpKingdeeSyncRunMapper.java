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
                .eq(ErpKingdeeSyncRunDO::getStatus, ErpKingdeeSyncRunStatusEnum.RUNNING.getStatus()));
    }

    default PageResult<ErpKingdeeSyncRunDO> selectPage(ErpKingdeeSyncRunPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpKingdeeSyncRunDO>()
                .eqIfPresent(ErpKingdeeSyncRunDO::getSyncType, reqVO.getSyncType())
                .eqIfPresent(ErpKingdeeSyncRunDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(ErpKingdeeSyncRunDO::getStartedAt, reqVO.getStartedAt())
                .orderByDesc(ErpKingdeeSyncRunDO::getId));
    }

}
