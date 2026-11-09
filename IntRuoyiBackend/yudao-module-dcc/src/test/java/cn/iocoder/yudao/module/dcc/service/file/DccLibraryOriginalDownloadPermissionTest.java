package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.controller.admin.file.DccControlledFileController;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DccLibraryOriginalDownloadPermissionTest {
    @AfterEach void clearTenant() { TenantContextHolder.clear(); }

    @Test void editableEndpointAcceptsOnlyTheActualTenantLibraryEditorWithoutGlobalDownloadPermission() throws Exception {
        TenantContextHolder.setTenantId(1L);
        var files = mock(DccControlledFileMapper.class);
        var projects = mock(DccProjectCodeMapper.class);
        var access = mock(DccProjectAccessService.class);
        var permissions = new DccControlledFileCategoryPermissionSupport();
        ReflectionTestUtils.setField(permissions, "projectMapper", projects);
        ReflectionTestUtils.setField(permissions, "projectAccessService", access);
        var service = new DccControlledFileQueryServiceImpl();
        ReflectionTestUtils.setField(service, "controlledFileMapper", files);
        ReflectionTestUtils.setField(service, "permissionSupport", permissions);
        var project = new DccProjectCodeDO();
        project.setId(7L); project.setTenantId(1L); project.setLibraryScope("QMS");
        var file = DccControlledFileDO.builder().id(10L).dccProjectCodeId(7L).status("ACTIVE").build();
        file.setTenantId(1L);
        when(files.selectById(10L)).thenReturn(file);
        when(projects.selectById(7L)).thenReturn(project);

        var expression = new SpelExpressionParser().parseExpression(DccControlledFileController.class
                .getMethod("downloadEditableControlledFile", Long.class, Boolean.class, String.class, HttpServletRequest.class)
                .getAnnotation(PreAuthorize.class).value());
        var context = new StandardEvaluationContext();
        context.setVariable("id", 10L);
        context.setBeanResolver((ignored, name) -> switch (name) {
            case "dccControlledFileQueryServiceImpl" -> service;
            case "ss" -> new NoGlobalDownloadPermission();
            default -> throw new IllegalArgumentException(name);
        });
        try (var security = mockStatic(SecurityFrameworkUtils.class)) {
            security.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            // VIEW and an absent rule must not satisfy the same endpoint used by EDIT.
            when(access.listReadableProjectIds(99L)).thenReturn(java.util.List.of(7L));
            assertFalse(expression.getValue(context, Boolean.class));
            when(access.hasProjectEditorOrOwner(99L, 7L)).thenReturn(true);
            assertTrue(expression.getValue(context, Boolean.class));
            project.setLibraryScope("GENERAL");
            assertTrue(expression.getValue(context, Boolean.class));
            project.setLibraryScope("PROJECT");
            assertFalse(expression.getValue(context, Boolean.class));
            project.setLibraryScope("QMS");
            file.setTenantId(2L);
            assertFalse(expression.getValue(context, Boolean.class));
            file.setTenantId(1L); file.setStatus("OBSOLETE");
            assertFalse(expression.getValue(context, Boolean.class));
            file.setStatus("ACTIVE"); file.setDeleted(true);
            assertFalse(expression.getValue(context, Boolean.class));
            file.setDeleted(false); TenantContextHolder.setIgnore(true);
            assertFalse(expression.getValue(context, Boolean.class));
        }
    }

    public static class NoGlobalDownloadPermission {
        public boolean hasAnyPermissions(String... permissions) { return false; }
    }
}
