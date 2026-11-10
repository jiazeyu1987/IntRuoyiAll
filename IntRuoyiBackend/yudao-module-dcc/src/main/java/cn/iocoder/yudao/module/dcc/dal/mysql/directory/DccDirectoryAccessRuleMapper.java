package cn.iocoder.yudao.module.dcc.dal.mysql.directory;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.dcc.dal.dataobject.directory.DccDirectoryAccessRuleDO;
import cn.iocoder.yudao.module.dcc.enums.DccAccessSubjectTypeEnum;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * DCC directory access rule mapper.
 */
@Mapper
public interface DccDirectoryAccessRuleMapper extends BaseMapperX<DccDirectoryAccessRuleDO> {

    default List<DccDirectoryAccessRuleDO> selectActiveForSubjects(Long userId, Set<Long> departmentIds,
            Set<Long> roleIds, Set<Long> positionIds) {
        var query = new LambdaQueryWrapper<DccDirectoryAccessRuleDO>()
                .eq(DccDirectoryAccessRuleDO::getActive, true);
        // Filter before materializing rules: a tenant can have hundreds of thousands of other subjects' grants.
        query.and(subjects -> {
            subjects.nested(user -> subject(user, DccAccessSubjectTypeEnum.USER, List.of(userId)));
            if (!departmentIds.isEmpty())
                subjects.or(dept -> subject(dept, DccAccessSubjectTypeEnum.DEPT, departmentIds));
            if (!roleIds.isEmpty())
                subjects.or(role -> subject(role, DccAccessSubjectTypeEnum.ROLE, roleIds));
            if (!positionIds.isEmpty())
                subjects.or(position -> subject(position, DccAccessSubjectTypeEnum.POSITION, positionIds));
        });
        return selectList(query);
    }

    private static void subject(LambdaQueryWrapper<DccDirectoryAccessRuleDO> query,
            DccAccessSubjectTypeEnum type, Collection<Long> ids) {
        query.apply("UPPER(subject_type) IN ({0}, {1})", String.valueOf(type.getType()), type.name())
                .in(DccDirectoryAccessRuleDO::getSubjectId, ids);
    }

    default java.util.List<DccDirectoryAccessRuleDO> selectListByDirectoryId(Long directoryId) {
        return selectList(DccDirectoryAccessRuleDO::getDirectoryId, directoryId);
    }
}
