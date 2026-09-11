package cn.iocoder.yudao.module.dcc.dal.mysql.projectcode;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectAccessRuleDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface DccProjectAccessRuleMapper extends BaseMapperX<DccProjectAccessRuleDO> {

    default List<DccProjectAccessRuleDO> selectActiveRules(Long projectCodeId, LocalDateTime now) {
        return selectList(new LambdaQueryWrapperX<DccProjectAccessRuleDO>()
                .eq(DccProjectAccessRuleDO::getDccProjectCodeId, projectCodeId)
                .eq(DccProjectAccessRuleDO::getActive, Boolean.TRUE)
                .and(wrapper -> wrapper.isNull(DccProjectAccessRuleDO::getValidFrom)
                        .or().le(DccProjectAccessRuleDO::getValidFrom, now))
                .and(wrapper -> wrapper.isNull(DccProjectAccessRuleDO::getExpireTime)
                        .or().gt(DccProjectAccessRuleDO::getExpireTime, now))
                .orderByDesc(DccProjectAccessRuleDO::getId));
    }

}
