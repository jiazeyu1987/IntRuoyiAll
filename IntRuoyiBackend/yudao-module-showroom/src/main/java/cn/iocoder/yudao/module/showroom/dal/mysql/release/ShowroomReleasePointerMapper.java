package cn.iocoder.yudao.module.showroom.dal.mysql.release;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.showroom.dal.dataobject.release.ShowroomReleasePointerDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ShowroomReleasePointerMapper extends BaseMapperX<ShowroomReleasePointerDO> {

    default ShowroomReleasePointerDO selectByPointerKey(String pointerKey) {
        return selectOne(new LambdaQueryWrapperX<ShowroomReleasePointerDO>()
                .eq(ShowroomReleasePointerDO::getPointerKey, pointerKey)
                .last("LIMIT 1"));
    }

    default ShowroomReleasePointerDO selectByPointerScope(Long tenantId, String siteKey, String stage,
                                                          String pointerKey) {
        return selectOne(new LambdaQueryWrapperX<ShowroomReleasePointerDO>()
                .eq(ShowroomReleasePointerDO::getTenantId, tenantId)
                .eq(ShowroomReleasePointerDO::getSiteKey, siteKey)
                .eq(ShowroomReleasePointerDO::getStage, stage)
                .eq(ShowroomReleasePointerDO::getPointerKey, pointerKey)
                .last("LIMIT 1"));
    }
}
