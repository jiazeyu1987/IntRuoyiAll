package cn.iocoder.yudao.module.dcc.dal.mysql.directory;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.directory.DccFileDirectoryDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * DCC file directory mapper.
 */
@Mapper
public interface DccFileDirectoryMapper extends BaseMapperX<DccFileDirectoryDO> {
    @org.apache.ibatis.annotations.Select("SELECT * FROM dcc_file_directory WHERE tenant_id=#{tenant} AND id=#{id} AND deleted=0 FOR UPDATE")
    DccFileDirectoryDO selectTenantForUpdate(@org.apache.ibatis.annotations.Param("tenant") Long tenant,@org.apache.ibatis.annotations.Param("id") Long id);
    @org.apache.ibatis.annotations.Select("SELECT COUNT(*) FROM dcc_file_directory WHERE tenant_id=#{tenant} AND parent_id=#{id} AND active=1 AND deleted=0")
    long countActiveChildren(@org.apache.ibatis.annotations.Param("tenant") Long tenant,@org.apache.ibatis.annotations.Param("id") Long id);

    default List<DccFileDirectoryDO> selectEnabledList() {
        LambdaQueryWrapperX<DccFileDirectoryDO> query = new LambdaQueryWrapperX<>();
        query.eq(DccFileDirectoryDO::getActive, Boolean.TRUE);
        query.orderByAsc(DccFileDirectoryDO::getSort);
        query.orderByDesc(DccFileDirectoryDO::getId);
        return selectList(query);
    }

    default List<DccFileDirectoryDO> selectEnabledListByParentId(Long parentId) {
        LambdaQueryWrapperX<DccFileDirectoryDO> query = new LambdaQueryWrapperX<>();
        query.orderByAsc(DccFileDirectoryDO::getSort);
        query.orderByDesc(DccFileDirectoryDO::getId);
        if (parentId == null || parentId == 0L) {
            query.and(wrapper -> wrapper.isNull(DccFileDirectoryDO::getParentId)
                    .or()
                    .eq(DccFileDirectoryDO::getParentId, 0L));
        } else {
            query.eq(DccFileDirectoryDO::getParentId, parentId);
        }
        query.eq(DccFileDirectoryDO::getActive, Boolean.TRUE);
        return selectList(query);
    }

    default List<Long> selectEnabledParentIdsByParentIds(Collection<Long> parentIds) {
        if (parentIds == null || parentIds.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapperX<DccFileDirectoryDO> query = new LambdaQueryWrapperX<>();
        query.select(DccFileDirectoryDO::getParentId);
        query.in(DccFileDirectoryDO::getParentId, parentIds);
        query.eq(DccFileDirectoryDO::getActive, Boolean.TRUE);
        return selectList(query).stream()
                .map(DccFileDirectoryDO::getParentId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    default List<DccFileDirectoryDO> selectEnabledListByKeyword(String keyword, int limit) {
        LambdaQueryWrapperX<DccFileDirectoryDO> query = new LambdaQueryWrapperX<>();
        query.eq(DccFileDirectoryDO::getActive, Boolean.TRUE);
        query.and(wrapper -> wrapper.like(DccFileDirectoryDO::getName, keyword)
                .or()
                .like(DccFileDirectoryDO::getCode, keyword));
        query.orderByAsc(DccFileDirectoryDO::getSort);
        query.orderByDesc(DccFileDirectoryDO::getId);
        query.last("LIMIT " + limit);
        return selectList(query);
    }

}
