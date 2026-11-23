package cn.iocoder.yudao.module.system.service.user;

import cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO;
import cn.iocoder.yudao.module.system.service.dept.DeptService;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserDepartmentPathTest {
    @Test
    void responsiblePathsRequireAnUnambiguousStringArray() {
        assertEquals(List.of("集团 / 中心", "集团 / 中心 / QA"), UserDepartmentPath.parseLeaderPaths(
                "[\"集团 / 中心\",\"集团 / 中心 / QA\"]"));
        assertTrue(UserDepartmentPath.parseLeaderPaths(null).isEmpty());
        for (String invalid : List.of("集团 / 中心", "{}", "[1]", "[null]", "[\"\"]",
                "[\"集团 / \"]", "[\"集团\",\"集团\"]", "[] {}")) {
            assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                    () -> UserDepartmentPath.parseLeaderPaths(invalid), invalid);
        }
    }

    @Test
    void exportsAncestorsAndFailsForBrokenOrCyclicHierarchy() {
        var service = mock(DeptService.class);
        var leaf = new DeptDO().setId(7L).setName("质量体系中心").setParentId(1L);
        var root = new DeptDO().setId(1L).setName("瑛泰医疗").setParentId(0L);
        when(service.getDept(1L)).thenReturn(root);
        var cache = new HashMap<>(Map.of(7L, leaf));
        assertEquals("瑛泰医疗 / 质量体系中心", UserDepartmentPath.exportPath(7L, cache, service));
        root.setParentId(7L);
        assertThrows(RuntimeException.class, () -> UserDepartmentPath.exportPath(7L, cache, service));
        assertThrows(RuntimeException.class, () -> UserDepartmentPath.exportPath(99L, cache, service));
    }

    @Test
    void importsHierarchyAndDistinguishesSameNamedDepartmentsByParent() {
        var service = mock(DeptService.class);
        var departments = new HashMap<String, List<DeptDO>>();
        departments.put("中心", new ArrayList<>(List.of(new DeptDO().setId(50L).setName("中心").setParentId(0L))));
        when(service.createDept(any())).thenReturn(80L, 81L);
        assertNull(UserDepartmentPath.resolve("公司 / 中心", "中心", departments, service, false));
        verifyNoInteractions(service);
        assertEquals(81L, UserDepartmentPath.resolve("公司 / 中心", "中心", departments, service, true));
        assertEquals(81L, UserDepartmentPath.resolve("公司 / 中心", "中心", departments, service, true));
        var requests = org.mockito.ArgumentCaptor.forClass(
                cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptSaveReqVO.class);
        verify(service, times(2)).createDept(requests.capture());
        assertEquals(List.of(0L, 80L), requests.getAllValues().stream().map(r -> r.getParentId()).toList());
        assertThrows(RuntimeException.class, () -> UserDepartmentPath.resolve("公司 / 中心", "错误", departments, service, true));
    }
}
