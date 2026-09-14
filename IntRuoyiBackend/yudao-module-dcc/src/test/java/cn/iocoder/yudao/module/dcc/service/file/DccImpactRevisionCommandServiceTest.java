package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileMajorRevisionReqVO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class DccImpactRevisionCommandServiceTest extends BaseMockitoUnitTest {

    @Mock private DccRelatedFileImpactAssessmentService impactService;
    @Mock private DccControlledFileWorkflowService workflowService;

    @InjectMocks
    private DccImpactRevisionCommandService service;

    @Test
    void createAndLinkMajorRevision_reusesExistingWorkflowThenLinksConcreteRevision() {
        when(workflowService.createMajorRevision(any(), any(DccControlledFileMajorRevisionReqVO.class)))
                .thenReturn(501L);

        Long result = service.createAndLinkMajorRevision(99L, 10L, 2, 200L, "同步关联文件");

        assertEquals(501L, result);
        ArgumentCaptor<DccControlledFileMajorRevisionReqVO> requestCaptor =
                ArgumentCaptor.forClass(DccControlledFileMajorRevisionReqVO.class);
        verify(workflowService).createMajorRevision(org.mockito.ArgumentMatchers.eq(99L), requestCaptor.capture());
        assertEquals(200L, requestCaptor.getValue().getSourceControlledFileId());
        assertEquals("同步关联文件", requestCaptor.getValue().getReason());
        InOrder order = inOrder(impactService, workflowService);
        order.verify(impactService).assertRevisionCreationAllowed(99L, 10L, 2, 200L, "同步关联文件");
        order.verify(workflowService).createMajorRevision(org.mockito.ArgumentMatchers.eq(99L), any());
        order.verify(impactService).linkExistingMajorRevision(99L, 10L, 2, 501L, "同步关联文件");
    }

    @Test
    void createAndLinkMajorRevision_existingWorkflowOpenRevisionConflictDoesNotLinkAnotherVersion() {
        RuntimeException conflict = new IllegalStateException("existing open major revision");
        when(workflowService.createMajorRevision(any(), any(DccControlledFileMajorRevisionReqVO.class)))
                .thenThrow(conflict);

        org.junit.jupiter.api.Assertions.assertSame(conflict, org.junit.jupiter.api.Assertions.assertThrows(
                RuntimeException.class,
                () -> service.createAndLinkMajorRevision(99L, 10L, 2, 200L, "同步关联文件")));

        verify(impactService, never()).linkExistingMajorRevision(any(), any(), any(), any(), any());
    }
}
