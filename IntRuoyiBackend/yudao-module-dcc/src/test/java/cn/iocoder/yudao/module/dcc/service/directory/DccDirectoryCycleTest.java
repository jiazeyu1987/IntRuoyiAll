package cn.iocoder.yudao.module.dcc.service.directory;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.dcc.controller.admin.directory.vo.DccDirectorySaveReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.directory.DccFileDirectoryDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.directory.DccFileDirectoryMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DccDirectoryCycleTest {
    private final DccFileDirectoryMapper mapper = mock(DccFileDirectoryMapper.class);
    private final DccDirectoryAdminServiceImpl service = new DccDirectoryAdminServiceImpl();

    private void hierarchy() {
        ReflectionTestUtils.setField(service, "directoryMapper", mapper);
        DccFileDirectoryDO a = DccFileDirectoryDO.builder().id(1L).parentId(null).name("A").active(true).build();
        DccFileDirectoryDO b = DccFileDirectoryDO.builder().id(2L).parentId(1L).name("B").active(true).build();
        lenient().when(mapper.selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(List.of(a, b));
        lenient().when(mapper.selectById(1L)).thenReturn(a);
        lenient().when(mapper.selectById(2L)).thenReturn(b);
    }

    @Test
    void rejectsSelfParentWithoutSaving() {
        hierarchy();
        var request = new DccDirectorySaveReqVO();
        request.setId(1L);
        request.setParentId(1L);
        assertThrows(ServiceException.class, () -> service.updateDirectory(request));
        verify(mapper, never()).updateById(any(DccFileDirectoryDO.class));
    }

    @Test
    void rejectsDescendantParentWithoutSaving() {
        hierarchy();
        var request = new DccDirectorySaveReqVO();
        request.setId(1L);
        request.setParentId(2L);
        assertThrows(ServiceException.class, () -> service.updateDirectory(request));
        verify(mapper, never()).updateById(any(DccFileDirectoryDO.class));
    }

    @Test
    void corruptedParentChainFailsExplicitlyBeforeUnboundedQueries() {
        ReflectionTestUtils.setField(service, "directoryMapper", mapper);
        ReflectionTestUtils.setField(service, "projectCodeAssignmentMapper",
                mock(cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeAssignmentMapper.class));
        var permissions = mock(DccDirectoryAccessPermissionService.class);
        ReflectionTestUtils.setField(service, "accessPermissionService", permissions);
        when(permissions.hasDirectoryManagementPermission(9L)).thenReturn(true);
        var a = DccFileDirectoryDO.builder().id(1L).parentId(2L).name("A").active(true).build();
        var b = DccFileDirectoryDO.builder().id(2L).parentId(1L).name("B").active(true).build();
        AtomicInteger queries = new AtomicInteger();
        when(mapper.selectById(any())).thenAnswer(call -> {
            if (queries.incrementAndGet() > 10) {
                throw new AssertionError("Cyclic hierarchy exceeded bounded query budget");
            }
            return Long.valueOf(1).equals(call.getArgument(0)) ? a : b;
        });
        assertThrows(ServiceException.class, () -> service.listVisibleChildDirectories(9L, 1L));
    }
}
