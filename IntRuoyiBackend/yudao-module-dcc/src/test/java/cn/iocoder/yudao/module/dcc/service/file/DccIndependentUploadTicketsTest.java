package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileAttachmentUploadTicketReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileSubmitReqVO;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketBoundFile;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketResolveCommand;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DccIndependentUploadTicketsTest {
    private final DccUploadTicketService tickets = mock(DccUploadTicketService.class);
    private final DccControlledFileWorkflowServiceImpl workflow = new DccControlledFileWorkflowServiceImpl();

    private DccControlledFileSubmitReqVO request() {
        ReflectionTestUtils.setField(workflow, "uploadTicketService", tickets);
        var req = new DccControlledFileSubmitReqVO();
        req.setCategoryId(10L);
        req.setProcessType("CONTROLLED_FILE");
        req.setDccProjectCodeId(30L);
        req.setFileTypeTaxonomyId(20L);
        req.setFileName("part.dwg");
        req.setSessionId(DccSourceUploadSession.scope(DccSourceUploadSession.newUploadPrefix(30L, 20L, "part.dwg"), "source"));
        req.setOriginalUploadTicket("source-ticket");
        req.setDrawingPdfUploadTicket("drawing-ticket");
        req.setDrawingPdfSessionId(DccSourceUploadSession.scope(DccSourceUploadSession.auxiliaryPrefix(30L, 20L), "drawing"));
        var attachment = new DccControlledFileAttachmentUploadTicketReqVO();
        attachment.setUploadTicket("attachment-ticket");
        attachment.setSessionId(DccSourceUploadSession.scope(DccSourceUploadSession.auxiliaryPrefix(30L, 20L), "attachment"));
        req.setAttachmentUploadTickets(List.of(attachment));
        return req;
    }

    @Test void resolvesIndependentTicketsWithTheirOwnSessionAndPurpose() {
        var req = request();
        when(tickets.resolveForBinding(any())).thenAnswer(invocation -> {
            DccUploadTicketResolveCommand cmd = invocation.getArgument(0);
            return new DccUploadTicketBoundFile(cmd.uploadTicket(), 100L, "file.pdf", "application/pdf", 4L);
        });
        Object resolved = ReflectionTestUtils.invokeMethod(workflow, "resolveSubmitFiles", 99L, req, true);
        assertNotNull(resolved);
        verify(tickets).resolveForBinding(new DccUploadTicketResolveCommand("source-ticket", 99L, 10L, req.getSessionId(), "SOURCE"));
        verify(tickets).resolveForBinding(new DccUploadTicketResolveCommand("drawing-ticket", 99L, 10L, req.getDrawingPdfSessionId(), "DRAWING_PDF"));
        verify(tickets).resolveForBinding(new DccUploadTicketResolveCommand("attachment-ticket", 99L, 10L,
                req.getAttachmentUploadTickets().get(0).getSessionId(), "ATTACHMENT"));
        List<?> bindings = ReflectionTestUtils.invokeMethod(resolved, "ticketBindings");
        assertEquals(req.getDrawingPdfSessionId(), ReflectionTestUtils.invokeMethod(bindings.get(1), "sessionId"));
        assertEquals(req.getAttachmentUploadTickets().get(0).getSessionId(), ReflectionTestUtils.invokeMethod(bindings.get(2), "sessionId"));
    }

    @Test void rejectsDrawingAndAttachmentSessionsFromOtherProjectsOrFileTypes() {
        var req = request();
        when(tickets.resolveForBinding(any())).thenReturn(new DccUploadTicketBoundFile("source-ticket", 100L, "part.dwg", "application/octet-stream", 4L));
        req.setDrawingPdfSessionId(DccSourceUploadSession.scope(DccSourceUploadSession.auxiliaryPrefix(31L, 20L), "drawing"));
        assertThrows(RuntimeException.class, () -> ReflectionTestUtils.invokeMethod(workflow, "resolveSubmitFiles", 99L, req, true));
        req.setDrawingPdfSessionId(DccSourceUploadSession.scope(DccSourceUploadSession.auxiliaryPrefix(30L, 20L), "drawing"));
        req.getAttachmentUploadTickets().get(0).setSessionId(DccSourceUploadSession.scope(DccSourceUploadSession.auxiliaryPrefix(30L, 21L), "attachment"));
        assertThrows(RuntimeException.class, () -> ReflectionTestUtils.invokeMethod(workflow, "resolveSubmitFiles", 99L, req, true));
    }

    @Test void rejectsDrawingTicketWithoutItsSession() {
        var req = request();
        when(tickets.resolveForBinding(any())).thenReturn(new DccUploadTicketBoundFile("source-ticket", 100L, "part.dwg", "application/octet-stream", 4L));
        req.setDrawingPdfSessionId(null);
        assertThrows(RuntimeException.class, () -> ReflectionTestUtils.invokeMethod(workflow, "resolveSubmitFiles", 99L, req, true));
    }
}
