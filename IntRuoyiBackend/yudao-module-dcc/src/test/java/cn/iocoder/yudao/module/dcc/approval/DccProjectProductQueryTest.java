package cn.iocoder.yudao.module.dcc.approval;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.bpm.approval.core.*;
import cn.iocoder.yudao.module.bpm.approval.service.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectProductCreateRequestMapper;
import cn.iocoder.yudao.module.dcc.service.projectcode.productcreate.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class DccProjectProductQueryTest {
    @AfterEach void clearTenant() { TenantContextHolder.clear(); }
    @Test void roleReviewUsesFrozenRoleSqlBeforeCountWithoutLoadingRequests() {
        TenantContextHolder.setTenantId(123L);
        var requests=mock(DccProjectProductCreateRequestMapper.class);
        var actors=mock(DccProjectProductActorSupport.class);
        var roles=mock(DccProjectReviewerRoleService.class);
        var service=new DccProjectProductTaskDelegate(requests,actors,mock(DccProjectProductCreateService.class));
        ReflectionTestUtils.setField(service,"reviewerRoles",roles);
        when(actors.canQuery(7L)).thenReturn(true);
        when(roles.reviewableRoleId(7L)).thenReturn(55L);
        // Contract validation, presence of role tasks, review count, approval count.
        when(requests.selectCount(any())).thenReturn(0L,1L,2L,0L);
        var context=ApprovalTaskQueryContext.of(7L,ApprovalTaskViewType.TODO,ApprovalModuleCode.DCC,null,1,20)
                .setListFilters("[{\"key\":\"node\",\"operator\":\"contains\",\"value\":\"项目审核员\"}]")
                .withWindow(new ApprovalQueryWindow(0,0));
        var page=service.page(context);
        assertEquals(2L,page.getTotal());assertTrue(page.getList().isEmpty());
        @SuppressWarnings("rawtypes") var queries=ArgumentCaptor.forClass(QueryWrapper.class);
        verify(requests,times(4)).selectCount(queries.capture());
        var review=queries.getAllValues().get(2);String sql=review.getSqlSegment();
        assertTrue(sql.contains("review_contract='ROLE_SINGLE_REVIEW_V1'"));
        assertTrue(sql.contains("reviewer_role_id="));
        assertFalse(review.getParamNameValuePairs().containsValue(7L));
        assertTrue(review.getParamNameValuePairs().containsValue(55L));
        assertTrue(review.getParamNameValuePairs().containsValue("%项目审核员%"));
        assertTrue(queries.getAllValues().get(3).getSqlSegment().contains("1=0"));
        verify(requests,never()).selectNativeTaskRows();
        verify(requests,never()).selectList(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
    }
}
