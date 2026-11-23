package cn.iocoder.yudao.module.dcc.service.category;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccFileCategorySaveReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class DccFileCategoryDuplicateWriteTest {
    @Test void concurrentCodeConflictIsTranslatedButOtherUniqueFailuresArePreserved() {
        TenantContextHolder.setTenantId(1L);
        try {
            var mapper = mock(DccFileCategoryMapper.class);
            var taxonomy = mock(DccFileTypeTaxonomyAdminService.class);
            var service = new DccFileCategoryAdminServiceImpl();
            ReflectionTestUtils.setField(service, "categoryMapper", mapper);
            ReflectionTestUtils.setField(service, "fileTypeTaxonomyAdminService", taxonomy);
            when(taxonomy.resolveActivePath(8801L)).thenReturn(
                    new DccFileTypeTaxonomyPath(8801L, "技术文档", "设计和开发策划阶段", "测试", null, null));
            var request = new DccFileCategorySaveReqVO();
            request.setCode("RACE"); request.setName("并发类别"); request.setActive(true);
            request.setApprovalSourceMode("PUBLIC_MATRIX"); request.setFileTypeTaxonomyId(8801L);
            var failure = new DuplicateKeyException("unique constraint");
            when(mapper.insert(any(DccFileCategoryDO.class))).thenThrow(failure);
            when(mapper.selectReservedCodeForUpdate(1L, "RACE", null)).thenReturn(List.of(22L));
            var error = assertThrows(ServiceException.class, () -> service.createCategory(request));
            assertEquals(DccFileCategoryAdminErrors.CODE_EXISTS.getCode(), error.getCode());
            when(mapper.selectReservedCodeForUpdate(1L, "RACE", null)).thenReturn(List.of());
            assertSame(failure, assertThrows(DuplicateKeyException.class, () -> service.createCategory(request)));
        } finally { TenantContextHolder.clear(); }
    }
}
