package cn.iocoder.yudao.module.dcc.dal.mysql.projectcode;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeAliasMappingDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DccProjectCodeAliasMappingMapper extends BaseMapperX<DccProjectCodeAliasMappingDO> {

    String STATUS_CONFIRMED = "CONFIRMED";

    default List<DccProjectCodeAliasMappingDO> selectConfirmedActiveList() {
        return selectList(new LambdaQueryWrapperX<DccProjectCodeAliasMappingDO>()
                .eq(DccProjectCodeAliasMappingDO::getStatus, STATUS_CONFIRMED)
                .eq(DccProjectCodeAliasMappingDO::getActive, true)
                .orderByAsc(DccProjectCodeAliasMappingDO::getId));
    }
}
