package cn.iocoder.yudao.module.bpm.service.task;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskPageReqVO;
import org.flowable.engine.TaskService;
import org.flowable.engine.HistoryService;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.task.api.history.HistoricTaskInstanceQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BpmTaskServiceImplApprovalFilterTest {

    @Mock
    private TaskService taskService;
    @Mock
    private HistoryService historyService;
    @Mock
    private HistoricTaskInstanceQuery query;
    @Mock
    private TaskQuery taskQuery;
    @InjectMocks
    private BpmTaskServiceImpl service;

    @Test
    void getTaskDonePageAppliesProcessDefinitionKeyFilter() {
        when(historyService.createHistoricTaskInstanceQuery()).thenReturn(query);
        when(query.finished()).thenReturn(query);
        when(query.taskAssignee("100")).thenReturn(query);
        when(query.includeTaskLocalVariables()).thenReturn(query);
        when(query.orderByHistoricTaskInstanceEndTime()).thenReturn(query);
        when(query.desc()).thenReturn(query);
        when(query.count()).thenReturn(0L);

        BpmTaskPageReqVO reqVO = new BpmTaskPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);
        reqVO.setProcessDefinitionKey("dcc-controlled-file-approval");

        PageResult<HistoricTaskInstance> page = service.getTaskDonePage(100L, reqVO);

        assertEquals(0L, page.getTotal());
        verify(query).processDefinitionKey("dcc-controlled-file-approval");
    }

    @Test
    void getTaskTodoPageSkipsAssigneeFilterWhenLoginUserIsNull() {
        when(taskService.createTaskQuery()).thenReturn(taskQuery);
        when(taskQuery.active()).thenReturn(taskQuery);
        when(taskQuery.includeProcessVariables()).thenReturn(taskQuery);
        when(taskQuery.taskTenantId(org.mockito.ArgumentMatchers.anyString())).thenReturn(taskQuery);
        when(taskQuery.orderByTaskCreateTime()).thenReturn(taskQuery);
        when(taskQuery.desc()).thenReturn(taskQuery);
        when(taskQuery.count()).thenReturn(0L);

        BpmTaskPageReqVO reqVO = new BpmTaskPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);

        PageResult<org.flowable.task.api.Task> page = service.getTaskTodoPage(null, reqVO);

        assertEquals(0L, page.getTotal());
        verify(taskQuery, org.mockito.Mockito.never()).taskAssignee(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void getTaskDonePageSkipsAssigneeFilterWhenLoginUserIsNull() {
        when(historyService.createHistoricTaskInstanceQuery()).thenReturn(query);
        when(query.finished()).thenReturn(query);
        when(query.includeTaskLocalVariables()).thenReturn(query);
        when(query.orderByHistoricTaskInstanceEndTime()).thenReturn(query);
        when(query.desc()).thenReturn(query);
        when(query.count()).thenReturn(0L);

        BpmTaskPageReqVO reqVO = new BpmTaskPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);

        PageResult<HistoricTaskInstance> page = service.getTaskDonePage(null, reqVO);

        assertEquals(0L, page.getTotal());
        verify(query, org.mockito.Mockito.never()).taskAssignee(org.mockito.ArgumentMatchers.anyString());
    }
}
