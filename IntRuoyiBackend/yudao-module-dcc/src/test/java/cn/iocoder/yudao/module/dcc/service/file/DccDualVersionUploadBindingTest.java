package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileSubmitReqVO;
import cn.iocoder.yudao.module.dcc.service.upload.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DccDualVersionUploadBindingTest {
    private final DccUploadTicketService tickets = mock(DccUploadTicketService.class);
    private final DccControlledFileWorkflowServiceImpl workflow = new DccControlledFileWorkflowServiceImpl();

    private DccControlledFileSubmitReqVO request() {
        ReflectionTestUtils.setField(workflow, "uploadTicketService", tickets);
        var request = new DccControlledFileSubmitReqVO();
        request.setCategoryId(10L);
        request.setProcessType("CONTROLLED_FILE");
        request.setDccProjectCodeId(20L);
        request.setFileTypeTaxonomyId(30L);
        request.setFileName("Procedure.pdf");
        request.setReadOnlyUploadTicket("pdf-ticket");
        request.setSessionId(DccSourceUploadSession.scope(
                DccSourceUploadSession.newUploadPrefix(20L, 30L, "Procedure.pdf"), "client-session"));
        return request;
    }

    @Test void dualVersionUsesScopedPdfAndEditableTicketsWithDistinctPurposes() {
        var request = request();
        request.setEditableUploadTicket("editable-ticket");
        var pdf = new DccUploadTicketResolveCommand("pdf-ticket", 99L, 10L, request.getSessionId(), "READ_ONLY_VIEW");
        var editable = new DccUploadTicketResolveCommand("editable-ticket", 99L, 10L, request.getSessionId(), "EDITABLE_SOURCE");
        when(tickets.resolveForBinding(pdf)).thenReturn(new DccUploadTicketBoundFile("pdf-ticket", 101L, "Procedure.pdf", "application/pdf", 10L));
        when(tickets.resolveForBinding(editable)).thenReturn(new DccUploadTicketBoundFile("editable-ticket", 102L, "Procedure.docx", "application/octet-stream", 20L));
        Object result = ReflectionTestUtils.invokeMethod(workflow, "resolveSubmitFiles", 99L, request, true);
        assertEquals(101L, ReflectionTestUtils.getField(result, "readOnlyFileId"));
        assertEquals(102L, ReflectionTestUtils.getField(result, "editableFileId"));
        assertEquals(102L, ReflectionTestUtils.getField(result, "sourceFileId"));
        verify(tickets).resolveForBinding(pdf);
        verify(tickets).resolveForBinding(editable);
        verifyNoMoreInteractions(tickets);
    }

    @Test void pdfOnlyDoesNotRequireOrResolveAnEditableTicket() {
        var request = request();
        var pdf = new DccUploadTicketResolveCommand("pdf-ticket", 99L, 10L, request.getSessionId(), "READ_ONLY_VIEW");
        when(tickets.resolveForBinding(pdf)).thenReturn(new DccUploadTicketBoundFile("pdf-ticket", 101L, "Procedure.pdf", "application/pdf", 10L));
        Object result = ReflectionTestUtils.invokeMethod(workflow, "resolveSubmitFiles", 99L, request, true);
        assertEquals(101L, ReflectionTestUtils.getField(result, "readOnlyFileId"));
        assertNull(ReflectionTestUtils.getField(result, "editableFileId"));
        assertEquals(101L, ReflectionTestUtils.getField(result, "sourceFileId"));
        verify(tickets).resolveForBinding(pdf);
        verifyNoMoreInteractions(tickets);
    }

    @Test void changedProjectCannotReuseAnExistingSessionOrResolveItsTickets() {
        var request = request();
        request.setDccProjectCodeId(21L);
        assertThrows(RuntimeException.class, () -> ReflectionTestUtils.invokeMethod(workflow, "resolveSubmitFiles", 99L, request, true));
        verifyNoInteractions(tickets);
    }
}
