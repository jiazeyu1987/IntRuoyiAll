package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventRevisionDiffDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collections;
import java.util.List;

@Mapper
public interface MesProProcessPoolEventRevisionDiffMapper
        extends BaseMapperX<MesProProcessPoolEventRevisionDiffDO> {

    default List<MesProProcessPoolEventRevisionDiffDO> selectListByRevisionIds(List<Long> revisionIds) {
        if (revisionIds == null || revisionIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<MesProProcessPoolEventRevisionDiffDO>()
                .in(MesProProcessPoolEventRevisionDiffDO::getRevisionId, revisionIds)
                .orderByAsc(MesProProcessPoolEventRevisionDiffDO::getId));
    }
}
