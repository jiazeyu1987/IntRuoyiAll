package cn.iocoder.yudao.module.dcc.service.directory;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.dcc.dal.dataobject.directory.DccDirectoryAccessRuleDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.directory.DccDirectoryAccessRuleMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DccDirectoryAccessSubjectQueryTest extends BaseDbUnitTest {
    @Resource private DccDirectoryAccessRuleMapper mapper;

    @Test
    void returnsOnlyEnabledRulesForExactUserAndCurrentDepartmentRoleAndPositionSubjects() {
        rule(1, "USER", 99, true);
        rule(2, "1", 99, true);
        rule(3, "dept", 10, true);
        rule(4, "2", 11, true);
        rule(5, "role", 30, true);
        rule(6, "3", 31, true);
        rule(7, "position", 20, true);
        rule(8, "4", 21, true);
        rule(9, "USER", 98, true);
        rule(10, "DEPT", 12, true);
        rule(11, "ROLE", 99, true); // Same id in a different subject type must not match the user.
        rule(12, "POSITION", 22, true);
        rule(13, "USER", 99, false);
        var removed = rule(14, "USER", 99, true);
        mapper.deleteById(removed.getId());

        var result = mapper.selectActiveForSubjects(99L, Set.of(10L, 11L),
                Set.of(30L, 31L), Set.of(20L, 21L));

        assertEquals(Set.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L), result.stream()
                .map(DccDirectoryAccessRuleDO::getDirectoryId).collect(Collectors.toSet()));
    }

    @Test
    void noOtherMembershipsStillMatchesOnlyTheUser() {
        rule(1, "USER", 99, true);
        rule(2, "DEPT", 99, true);
        rule(3, "ROLE", 99, true);
        rule(4, "POSITION", 99, true);

        var result = mapper.selectActiveForSubjects(99L, Set.of(), Set.of(), Set.of());

        assertEquals(Set.of(1L), result.stream().map(DccDirectoryAccessRuleDO::getDirectoryId)
                .collect(Collectors.toSet()));
    }

    private DccDirectoryAccessRuleDO rule(long directory, String type, long subject, boolean active) {
        var rule = DccDirectoryAccessRuleDO.builder().directoryId(directory).subjectType(type)
                .subjectId(subject).canQuery(true).canPreview(false).canDownload(false).active(active).build();
        mapper.insert(rule);
        return rule;
    }
}
