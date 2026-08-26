package cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordCellLinkPrefillRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecordcelllink.vo.BatchRecordCellLinkWorkbenchContextRespVO;
import cn.iocoder.yudao.module.mes.service.pro.batchrecordcelllink.MesProBatchRecordCellLinkService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesProBatchRecordCellLinkControllerTest {

    @Mock
    private MesProBatchRecordCellLinkService cellLinkService;

    @InjectMocks
    private MesProBatchRecordCellLinkController controller;

    @Test
    void getPrefillDelegatesWorkTaskContextForAssignedFillWorkspace() {
        BatchRecordCellLinkPrefillRespVO expected = new BatchRecordCellLinkPrefillRespVO()
                .setTargetExecutionId(10L)
                .setPrefills(List.of())
                .setConflicts(List.of());
        when(cellLinkService.getPrefill(10L, 31L)).thenReturn(expected);

        CommonResult<BatchRecordCellLinkPrefillRespVO> response = controller.getPrefill(10L, 31L);

        assertSame(expected, response.getData());
        verify(cellLinkService).getPrefill(10L, 31L);
    }

    @Test
    void getWorkbenchContextDelegatesFormTemplateVersionScope() {
        BatchRecordCellLinkWorkbenchContextRespVO expected = new BatchRecordCellLinkWorkbenchContextRespVO()
                .setScopeType("FORM_TEMPLATE_VERSION")
                .setScopeId(7001L);
        when(cellLinkService.getWorkbenchContext(null, null, null, null, 1001L, "V3.0", null, null))
                .thenReturn(expected);

        CommonResult<BatchRecordCellLinkWorkbenchContextRespVO> response =
                controller.getWorkbenchContext(null, null, null, null, 1001L, "V3.0", null, null);

        assertSame(expected, response.getData());
        verify(cellLinkService).getWorkbenchContext(null, null, null, null, 1001L, "V3.0", null, null);
    }

    @Test
    void getPrefillAllowsStaticConfigQueryOrValidatedWorkTaskContext() throws Exception {
        Method method = MesProBatchRecordCellLinkController.class
                .getDeclaredMethod("getPrefill", Long.class, Long.class);

        assertArrayEquals(new String[]{"/prefill"}, method.getAnnotation(GetMapping.class).value());
        assertEquals(
                "@ss.hasPermission('mes:pro-batch-record-cell-link:query') "
                        + "or #workTaskId != null",
                method.getAnnotation(PreAuthorize.class).value());
    }
}
