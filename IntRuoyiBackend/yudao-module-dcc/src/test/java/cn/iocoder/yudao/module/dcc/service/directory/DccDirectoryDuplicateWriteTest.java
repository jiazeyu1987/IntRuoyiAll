package cn.iocoder.yudao.module.dcc.service.directory;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.dcc.controller.admin.directory.vo.DccDirectorySaveReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.directory.DccFileDirectoryDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.directory.DccFileDirectoryMapper;
import cn.iocoder.yudao.module.dcc.service.file.DccStorageMappingMutationGuard;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class DccDirectoryDuplicateWriteTest {
    @Test void concurrentCodeConflictIsTranslatedButOtherUniqueFailuresArePreserved() {
        TenantContextHolder.setTenantId(1L);
        try {
            var mapper = mock(DccFileDirectoryMapper.class);
            var service = new DccDirectoryAdminServiceImpl();
            ReflectionTestUtils.setField(service, "directoryMapper", mapper);
            ReflectionTestUtils.setField(service, "storageMappingGuard", mock(DccStorageMappingMutationGuard.class));
            var request = new DccDirectorySaveReqVO();
            request.setCode("RACE"); request.setName("Directory"); request.setActive(true); request.setParentId(0L);
            var failure = new DuplicateKeyException("unique constraint");
            when(mapper.insert(any(DccFileDirectoryDO.class))).thenThrow(failure);
            when(mapper.selectReservedCodeForUpdate(0L, "RACE", null)).thenReturn(List.of(), List.of(22L));
            var error = assertThrows(ServiceException.class, () -> service.createDirectory(request));
            assertEquals(DccDirectoryAdminErrors.CODE_EXISTS.getCode(), error.getCode());
            when(mapper.selectReservedCodeForUpdate(0L, "RACE", null)).thenReturn(List.of());
            assertSame(failure, assertThrows(DuplicateKeyException.class, () -> service.createDirectory(request)));
        } finally { TenantContextHolder.clear(); }
    }
}
