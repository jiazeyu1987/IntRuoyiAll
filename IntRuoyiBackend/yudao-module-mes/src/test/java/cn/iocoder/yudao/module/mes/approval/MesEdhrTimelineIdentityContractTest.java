package cn.iocoder.yudao.module.mes.approval;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrWorkTaskServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class MesEdhrTimelineIdentityContractTest {
    MesProEdhrWorkTaskMapper tasks;
    MesProEdhrWorkTaskServiceImpl service;
    MesProEdhrWorkTaskDO source;
    MockedStatic<SecurityFrameworkUtils> login;
    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(1L);
        login = mockStatic(SecurityFrameworkUtils.class);
        login.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(12L);
        tasks = mock(MesProEdhrWorkTaskMapper.class); service = new MesProEdhrWorkTaskServiceImpl();
        ReflectionTestUtils.setField(service,"workTaskMapper",tasks);
        source = new MesProEdhrWorkTaskDO().setId(177L).setExecutionId(880L).setAssigneeUserId(12L).setBatchExecutionId(92L);
        when(tasks.selectOne(any())).thenReturn(source);
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(new org.apache.ibatis.builder.MapperBuilderAssistant(
                new com.baomidou.mybatisplus.core.MybatisConfiguration(),"timeline-contract"),MesProEdhrWorkTaskDO.class);
    }
    @AfterEach void cleanup() { login.close(); TenantContextHolder.clear(); }

    @ParameterizedTest @ValueSource(booleans={false,true})
    void suppliedOtherExecutionIsRejectedForPersonalAndGlobalBeforeRangeRead(boolean global) {
        assertThrows(IllegalArgumentException.class,() -> service.getApprovalCenterTimelineTasks(177L,881L,global));
        assertThrows(IllegalArgumentException.class,() -> service.getApprovalTimelineTasks(177L,881L));
        verify(tasks,never()).selectTimelineListByExecutionId(anyLong());
        verify(tasks,never()).selectTimelineListByBatchExecutionId(anyLong());
    }
    @ParameterizedTest @ValueSource(booleans={false,true})
    void releaseSourceCannotBorrowExecutionButKeepsItsOwnBatchPath(boolean global) {
        source.setExecutionId(null);
        assertThrows(IllegalArgumentException.class,() -> service.getApprovalCenterTimelineTasks(177L,881L,global));
        assertThrows(IllegalArgumentException.class,() -> service.getApprovalTimelineTasks(177L,881L));
        verify(tasks,never()).selectTimelineListByExecutionId(anyLong());
        when(tasks.selectTimelineListByBatchExecutionId(92L)).thenReturn(List.of(source));
        assertEquals(List.of(source),service.getApprovalCenterTimelineTasks(177L,null,global));
        assertEquals(List.of(source),service.getApprovalTimelineTasks(177L,null));
    }
    @Test void exactAndDefaultExecutionUseOnlyAuthorizedSourceRelation() {
        when(tasks.selectTimelineListByExecutionId(880L)).thenReturn(List.of(source));
        assertEquals(List.of(source),service.getApprovalTimelineTasks(177L,880L));
        assertEquals(List.of(source),service.getApprovalTimelineTasks(177L,null));
        assertEquals(List.of(source),service.getApprovalCenterTimelineTasks(177L,880L,false));
        assertEquals(List.of(source),service.getApprovalCenterTimelineTasks(177L,null,true));
        verify(tasks,times(4)).selectTimelineListByExecutionId(880L);
    }
    @Test void inaccessibleSourceFailsBeforeRangeReadEvenWithCorrectExecution() {
        source.setAssigneeUserId(112L);
        assertThrows(IllegalStateException.class,() -> service.getApprovalCenterTimelineTasks(177L,880L,false));
        assertThrows(IllegalStateException.class,() -> service.getApprovalTimelineTasks(177L,880L));
        verify(tasks,never()).selectTimelineListByExecutionId(anyLong());
    }
    @Test void exactSourceQueryUsesTenantAndDeletionBeforeAnyObjectEnrichment() {
        when(tasks.selectOne(any())).thenReturn(null); // foreign tenant is absent from exact query
        assertThrows(IllegalStateException.class,() -> service.getApprovalCenterTimelineTasks(177L,880L,true));
        var capture = org.mockito.ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.Wrapper.class);
        verify(tasks).selectOne(capture.capture());
        String sql = capture.getValue().getSqlSegment();
        assertTrue(sql.contains("tenant_id =")); assertTrue(sql.contains("deleted =")); assertTrue(sql.contains("id ="));
        verify(tasks,never()).selectTimelineListByExecutionId(anyLong());
    }
}
