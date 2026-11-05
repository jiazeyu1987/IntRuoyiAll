package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMasterDO;
import cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DccDraftEditPermissionTest {
    @Test void everyWorkingIterationUsesEditGrantInsteadOfOriginalAuthor() {
        var query = new DccControlledFileQueryServiceImpl();
        var permissions = mock(DccControlledFileCategoryPermissionSupport.class);
        ReflectionTestUtils.setField(query, "permissionSupport", permissions);
        ReflectionTestUtils.setField(query, "versionPolicy", DccControlledFileVersionPolicy.defaultPolicy());
        var master = DccControlledFileMasterDO.builder().id(100L).latestControlledFileId(10L).build();
        for (String version : new String[]{"A/1", "A/1-1", "A/1-2"}) {
            var file = DccControlledFileDO.builder().id(11L).masterId(100L).requesterId(88L)
                    .status("WORKING").versionNo(version).build();
            when(permissions.hasFilePermission(file, 99L, DccFileCategoryPermissionActionEnum.EDIT)).thenReturn(true);
            assertTrue(Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(query, "canCheckoutLockedVersion", 99L, file, master)));
            when(permissions.hasFilePermission(file, 99L, DccFileCategoryPermissionActionEnum.EDIT)).thenReturn(false);
            assertFalse(Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(query, "canCheckoutLockedVersion", 99L, file, master)));
            when(permissions.hasFilePermission(file, 99L, DccFileCategoryPermissionActionEnum.EDIT)).thenReturn(true);
            file.setStatus("PENDING_SUPERVISOR_REVIEW");
            assertFalse(Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(query, "canCheckoutLockedVersion", 99L, file, master)));
            file.setStatus("WORKING"); file.setRevisionChangeType("PARTIAL");
            assertFalse(Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(query, "canCheckoutLockedVersion", 99L, file, master)));
        }
    }
}
