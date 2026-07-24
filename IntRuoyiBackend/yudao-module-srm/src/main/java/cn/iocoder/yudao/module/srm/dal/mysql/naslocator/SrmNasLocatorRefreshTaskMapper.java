package cn.iocoder.yudao.module.srm.dal.mysql.naslocator;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.srm.dal.dataobject.naslocator.SrmNasLocatorRefreshTaskDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SrmNasLocatorRefreshTaskMapper extends BaseMapperX<SrmNasLocatorRefreshTaskDO> {

    default SrmNasLocatorRefreshTaskDO selectLatestTask(Long tenantId) {
        return selectOne(new LambdaQueryWrapperX<SrmNasLocatorRefreshTaskDO>()
                .eq(SrmNasLocatorRefreshTaskDO::getTenantId, tenantId)
                .orderByDesc(SrmNasLocatorRefreshTaskDO::getId)
                .last("LIMIT 1"));
    }

    default SrmNasLocatorRefreshTaskDO selectRunningTask(Long tenantId) {
        return selectOne(new LambdaQueryWrapperX<SrmNasLocatorRefreshTaskDO>()
                .eq(SrmNasLocatorRefreshTaskDO::getTenantId, tenantId)
                .eq(SrmNasLocatorRefreshTaskDO::getStatus, "RUNNING")
                .last("LIMIT 1"));
    }

    default SrmNasLocatorRefreshTaskDO selectLatestSuccessTask(Long tenantId) {
        return selectOne(new LambdaQueryWrapperX<SrmNasLocatorRefreshTaskDO>()
                .eq(SrmNasLocatorRefreshTaskDO::getTenantId, tenantId)
                .eq(SrmNasLocatorRefreshTaskDO::getStatus, "SUCCESS")
                .orderByDesc(SrmNasLocatorRefreshTaskDO::getFinishedTime)
                .orderByDesc(SrmNasLocatorRefreshTaskDO::getId)
                .last("LIMIT 1"));
    }

    default List<SrmNasLocatorRefreshTaskDO> selectSuccessTasksDesc(Long tenantId) {
        return selectList(new LambdaQueryWrapperX<SrmNasLocatorRefreshTaskDO>()
                .eq(SrmNasLocatorRefreshTaskDO::getTenantId, tenantId)
                .eq(SrmNasLocatorRefreshTaskDO::getStatus, "SUCCESS")
                .orderByDesc(SrmNasLocatorRefreshTaskDO::getFinishedTime)
                .orderByDesc(SrmNasLocatorRefreshTaskDO::getId));
    }

    default List<SrmNasLocatorRefreshTaskDO> selectFailedTasks(Long tenantId) {
        return selectList(new LambdaQueryWrapperX<SrmNasLocatorRefreshTaskDO>()
                .eq(SrmNasLocatorRefreshTaskDO::getTenantId, tenantId)
                .eq(SrmNasLocatorRefreshTaskDO::getStatus, "FAILED"));
    }
}
