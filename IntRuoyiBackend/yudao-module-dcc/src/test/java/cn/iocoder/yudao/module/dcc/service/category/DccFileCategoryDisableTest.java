package cn.iocoder.yudao.module.dcc.service.category;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.FILE_CATEGORY_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DccFileCategoryDisableTest extends BaseMockitoUnitTest {
    @InjectMocks private DccFileCategoryAdminServiceImpl service;
    @Mock private DccFileCategoryMapper mapper;

    @BeforeEach void setTenant() { TenantContextHolder.setTenantId(122L); }
    @AfterEach void clearTenant() { TenantContextHolder.clear(); }

    @Test void legacyCategory_changesOnlyActiveFlag() {
        when(mapper.selectMatrixCategoryForUpdate(122L, 907249L)).thenReturn(
                DccFileCategoryDO.builder().id(907249L).name("其他").code("LEGACY")
                        .active(true).approvalSourceMode("PUBLIC_MATRIX").build());
        when(mapper.updateById(any(DccFileCategoryDO.class))).thenAnswer(invocation -> {
            DccFileCategoryDO update = invocation.getArgument(0);
            assertEquals(907249L, update.getId());
            assertFalse(update.getActive());
            assertNull(update.getCode());
            assertNull(update.getName());
            assertNull(update.getApprovalSourceMode());
            assertNull(update.getFileTypeTaxonomyId());
            assertNull(update.getDeleted());
            return 1;
        });
        service.disableCategory(907249L);
        verify(mapper).selectMatrixCategoryForUpdate(122L, 907249L);
        verify(mapper).updateById(any(DccFileCategoryDO.class));
        verifyNoMoreInteractions(mapper);
    }

    @Test void foreignOrMissingCategory_doesNotWrite() {
        assertServiceException(() -> service.disableCategory(907249L), FILE_CATEGORY_NOT_EXISTS);
        verify(mapper).selectMatrixCategoryForUpdate(122L, 907249L);
        verifyNoMoreInteractions(mapper);
    }

    @Test void alreadyDisabled_isIdempotent() {
        when(mapper.selectMatrixCategoryForUpdate(122L, 907249L)).thenReturn(
                DccFileCategoryDO.builder().id(907249L).active(false).build());
        service.disableCategory(907249L);
        verify(mapper).selectMatrixCategoryForUpdate(122L, 907249L);
        verifyNoMoreInteractions(mapper);
    }

    @Test void incompleteWrite_isNotReportedAsSuccess() {
        when(mapper.selectMatrixCategoryForUpdate(122L, 907249L)).thenReturn(
                DccFileCategoryDO.builder().id(907249L).active(true).build());
        when(mapper.updateById(any(DccFileCategoryDO.class))).thenReturn(0);
        assertServiceException(() -> service.disableCategory(907249L),
                DccFileCategoryAdminErrors.WRITE_INCOMPLETE, "停用类别");
    }
}
