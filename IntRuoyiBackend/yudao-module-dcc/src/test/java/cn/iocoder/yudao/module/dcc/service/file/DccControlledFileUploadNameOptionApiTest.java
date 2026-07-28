package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.controller.admin.file.DccControlledFileController;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileUploadNameOptionRespVO;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccControlledFileUploadNameOptionApiTest extends BaseMockitoUnitTest {

    @Mock
    private DccControlledFileWorkflowService workflowService;
    @Mock
    private DccControlledFileFinalizationService finalizationService;
    @Mock
    private DccControlledFileUploadService uploadService;
    @Mock
    private DccControlledFileQueryService queryService;
    @Mock
    private DccControlledFileObsoleteService obsoleteService;
    @Mock
    private DccTrainingAssignmentAckService trainingAssignmentAckService;

    @InjectMocks
    private DccControlledFileController controller;

    @Test
    void getUploadNameOptions_delegatesToQueryService() {
        when(queryService.listUploadNameOptions(124L, 30L)).thenReturn(List.of(
                DccControlledFileUploadNameOptionRespVO.builder()
                        .fileName("SOP-001")
                        .currentVersionNo("1.0")
                        .controlledFileId(901L)
                        .fileNumber("SOP-001-NO")
                        .build()));

        CommonResult<List<DccControlledFileUploadNameOptionRespVO>> result =
                controller.getUploadNameOptions(124L, 30L);

        assertEquals(1, result.getData().size());
        assertEquals("SOP-001", result.getData().get(0).getFileName());
        assertEquals("1.0", result.getData().get(0).getCurrentVersionNo());
        assertEquals(901L, result.getData().get(0).getControlledFileId());
        assertEquals("SOP-001-NO", result.getData().get(0).getFileNumber());
        verify(queryService).listUploadNameOptions(124L, 30L);
    }
}
