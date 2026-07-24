package cn.iocoder.yudao.module.showroom.dal.mysql.release;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.showroom.dal.dataobject.release.ShowroomPublicSiteBindingDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ShowroomPublicSiteBindingMapper extends BaseMapperX<ShowroomPublicSiteBindingDO> {

    default ShowroomPublicSiteBindingDO selectEnabledBySiteStage(String siteKey, String stage) {
        return selectOne(new LambdaQueryWrapperX<ShowroomPublicSiteBindingDO>()
                .eq(ShowroomPublicSiteBindingDO::getSiteKey, siteKey)
                .eq(ShowroomPublicSiteBindingDO::getStage, stage)
                .eq(ShowroomPublicSiteBindingDO::getEnabled, true)
                .last("LIMIT 1"));
    }

    default ShowroomPublicSiteBindingDO selectAnyBySiteStage(String siteKey, String stage) {
        return selectOne(new LambdaQueryWrapperX<ShowroomPublicSiteBindingDO>()
                .eq(ShowroomPublicSiteBindingDO::getSiteKey, siteKey)
                .eq(ShowroomPublicSiteBindingDO::getStage, stage)
                .orderByAsc(ShowroomPublicSiteBindingDO::getId)
                .last("LIMIT 1"));
    }

    default List<ShowroomPublicSiteBindingDO> selectEnabledByTenantId(Long tenantId) {
        return selectList(new LambdaQueryWrapperX<ShowroomPublicSiteBindingDO>()
                .eq(ShowroomPublicSiteBindingDO::getTenantId, tenantId)
                .eq(ShowroomPublicSiteBindingDO::getEnabled, true)
                .orderByAsc(ShowroomPublicSiteBindingDO::getSiteKey)
                .orderByAsc(ShowroomPublicSiteBindingDO::getStage)
                .orderByAsc(ShowroomPublicSiteBindingDO::getId));
    }
}
