package cn.iocoder.yudao.module.erp.dal.mysql.nastablesync;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.nastablesync.vo.ErpNasTableSyncRunPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.nastablesync.ErpNasTableSyncRunDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ErpNasTableSyncRunMapper extends BaseMapperX<ErpNasTableSyncRunDO> {

    default PageResult<ErpNasTableSyncRunDO> selectPage(ErpNasTableSyncRunPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpNasTableSyncRunDO>()
                .eqIfPresent(ErpNasTableSyncRunDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(ErpNasTableSyncRunDO::getStartedAt, reqVO.getStartedAt())
                .orderByDesc(ErpNasTableSyncRunDO::getId));
    }

    default ErpNasTableSyncRunDO selectLatestByPlanId(Long planId) {
        return selectOne(new LambdaQueryWrapperX<ErpNasTableSyncRunDO>()
                .eq(ErpNasTableSyncRunDO::getPlanId, planId)
                .orderByDesc(ErpNasTableSyncRunDO::getId)
                .last("LIMIT 1"));
    }
}
