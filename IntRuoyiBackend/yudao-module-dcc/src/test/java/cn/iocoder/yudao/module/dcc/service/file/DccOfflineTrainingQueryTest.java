package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.approval.core.*;
import cn.iocoder.yudao.module.bpm.approval.service.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class DccOfflineTrainingQueryTest {
    @AfterEach void clearTenant() { TenantContextHolder.clear(); }

    @Test void countOnlyAppliesActorAclAndColumnPredicatesWithoutLoadingAllCandidates() {
        TenantContextHolder.setTenantId(123L);
        var files=mock(DccControlledFileMapper.class);
        var users=mock(AdminUserApi.class);
        var permissions=mock(PermissionApi.class);
        var categories=mock(DccControlledFileCategoryPermissionSupport.class);
        var fileScope=mock(DccControlledFileAssignmentScopeService.class);
        var projects=mock(DccProjectAccessService.class);
        var account=new AdminUserRespDTO();account.setId(7L);account.setTenantId(123L);account.setStatus(0);
        when(users.getUser(7L)).thenReturn(account);
        when(permissions.hasAnyPermissions(7L,"dcc:controlled-file:query")).thenReturn(true);
        when(categories.exactCategoryIds(7L,DccFileCategoryPermissionActionEnum.TRAINING_RECORD)).thenReturn(Set.of(2L));
        when(categories.editableCategoryIds(7L)).thenReturn(Set.of(3L));
        when(fileScope.resolveActiveAssignedControlledFileIds(7L)).thenReturn(Set.of(30L));
        when(projects.editorScope(7L)).thenReturn(new DccProjectAccessService.EditorScope(false,Set.of(10L),true,false));
        when(files.selectCount(any())).thenReturn(70L);
        var service=new DccOfflineTrainingRecordService();
        ReflectionTestUtils.setField(service,"files",files);ReflectionTestUtils.setField(service,"users",users);
        ReflectionTestUtils.setField(service,"permissions",permissions);ReflectionTestUtils.setField(service,"categories",categories);
        ReflectionTestUtils.setField(service,"fileScope",fileScope);ReflectionTestUtils.setField(service,"projectAccess",projects);
        var context=ApprovalTaskQueryContext.of(7L,ApprovalTaskViewType.TODO,ApprovalModuleCode.DCC,null,1,20)
                .setListFilters("[{\"key\":\"source\",\"operator\":\"contains\",\"value\":\"文控' OR 1=1\"}]")
                .withWindow(new ApprovalQueryWindow(0,0));
        var result=service.pageForActor(context);
        assertEquals(70L,result.getTotal());assertTrue(result.getList().isEmpty());
        @SuppressWarnings("rawtypes") var captured=ArgumentCaptor.forClass(QueryWrapper.class);
        verify(files).selectCount(captured.capture());
        var query=captured.getValue();String sql=query.getSqlSegment();
        assertTrue(sql.contains("dcc_controlled_file.requester_id=7"));
        assertTrue(sql.contains("dcc_controlled_file.file_owner_user_id=7"));
        assertTrue(sql.contains("dcc_controlled_file.id IN (30)"));
        assertTrue(sql.contains("ep.tenant_id=123"));
        assertFalse(sql.contains("文控' OR 1=1"));
        assertTrue(query.getParamNameValuePairs().containsValue("%文控' or 1=1%"));
        verify(files,never()).selectOfflineTrainingCandidates(anyLong());
        verify(files,never()).selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
    }
}
