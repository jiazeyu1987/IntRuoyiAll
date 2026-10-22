package cn.iocoder.yudao.module.dcc.service.projectcode;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO;
import cn.iocoder.yudao.module.dcc.service.file.DccControlledFileWorkflowService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.*;

class DccProjectUploadOptionsServiceTest {
    @Test void allProjectsAreSortedReadyFirstAndExpectedErrorsBecomeVisibleReasons() {
        var service = new DccProjectUploadOptionsService();
        var projects = mock(DccProjectCodeService.class);
        var workflow = mock(DccControlledFileWorkflowService.class);
        ReflectionTestUtils.setField(service,"projectCodeService",projects);
        ReflectionTestUtils.setField(service,"workflowService",workflow);
        var ready = project(1L,"IDI");
        var incomplete = project(2L,"AAA"); incomplete.setDefaultAttributesJson("{}");
        var denied = project(3L,"BBB");
        when(projects.getReadableProjectCodePage(eq(99L),any())).thenReturn(new PageResult<>(List.of(incomplete,denied,ready),3L));
        when(workflow.previewProjectProduct(99L,3L)).thenThrow(exception(DCC_PROJECT_ACCESS_DENIED));
        var rows=service.list(99L,null);
        assertEquals(List.of("IDI","AAA","BBB"),rows.stream().map(DccProjectUploadOptionsService.Option::projectCode).toList());
        assertTrue(rows.get(0).uploadSelectable());
        assertFalse(rows.get(1).uploadSelectable());
        assertTrue(rows.get(1).uploadIssues().toString().contains("目标市场"));
        assertTrue(rows.get(2).uploadIssues().toString().contains("权限"));
        var captor=org.mockito.ArgumentCaptor.forClass(cn.iocoder.yudao.module.dcc.controller.admin.projectcode.vo.DccProjectCodePageReqVO.class);
        verify(projects).getReadableProjectCodePage(eq(99L),captor.capture());
        assertEquals(-1,captor.getValue().getPageSize());
        when(workflow.previewProjectProduct(99L,1L)).thenThrow(new IllegalStateException("dependency unavailable"));
        assertThrows(IllegalStateException.class,()->service.list(99L,null));
    }
    @Test void malformedDefaultsAndProductIdentityAreBlockedRatherThanGreen() {
        var service=new DccProjectUploadOptionsService();
        var projects=mock(DccProjectCodeService.class);
        var workflow=mock(DccControlledFileWorkflowService.class);
        ReflectionTestUtils.setField(service,"projectCodeService",projects);
        ReflectionTestUtils.setField(service,"workflowService",workflow);
        var badJson=project(1L,"IDI"); badJson.setDefaultAttributesJson("{");
        var badProduct=project(2L,"XYZ");
        when(projects.getReadableProjectCodePage(eq(99L),any())).thenReturn(new PageResult<>(List.of(badJson,badProduct),2L));
        when(workflow.previewProjectProduct(99L,2L)).thenThrow(exception(CONTROLLED_FILE_SUBMIT_REQUIRED_METADATA_MISSING));
        var rows=service.list(99L,null);
        assertTrue(rows.stream().noneMatch(DccProjectUploadOptionsService.Option::uploadSelectable));
        assertTrue(rows.get(0).uploadIssues().toString().contains("格式"));
        assertTrue(rows.get(1).uploadIssues().toString().contains("产品"));
    }
    private static DccProjectCodeDO project(Long id,String code) {
        var p=new DccProjectCodeDO();p.setId(id);p.setProjectCode(code);p.setProjectName("名称不用于下拉");p.setStatus("ENABLE");
        p.setDefaultAttributesJson("{\"targetMarkets\":[\"NA\"],\"licenseHolder\":\"NA\",\"actualManufacturer\":\"NA\",\"documentTransfer\":\"N\"}");return p;
    }
}
