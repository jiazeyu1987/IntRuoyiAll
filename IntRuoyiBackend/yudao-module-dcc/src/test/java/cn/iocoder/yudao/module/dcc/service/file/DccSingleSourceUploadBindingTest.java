package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileSubmitReqVO;
import cn.iocoder.yudao.module.dcc.service.upload.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DccSingleSourceUploadBindingTest {
    private final DccUploadTicketService tickets = mock(DccUploadTicketService.class);
    private final DccControlledFileWorkflowServiceImpl workflow = new DccControlledFileWorkflowServiceImpl();

    private DccControlledFileSubmitReqVO request() {
        ReflectionTestUtils.setField(workflow, "uploadTicketService", tickets);
        var dates = new DccWorkflowDatePolicy();
        dates.setZoneId("Asia/Shanghai");
        ReflectionTestUtils.setField(workflow, "workflowDatePolicy", dates);
        var request = new DccControlledFileSubmitReqVO();
        request.setCategoryId(10L);
        request.setProcessType("CONTROLLED_FILE");
        request.setDccProjectCodeId(20L);
        request.setFileTypeTaxonomyId(30L);
        request.setFileName("Procedure.pdf");
        request.setFileNumber("SOP-001");
        request.setChangeType("NEW");
        request.setVersionNo("A/1");
        request.setDirectoryId(40L);
        request.setEffectiveMode("AFTER_STANDARDIZATION_SIGNATURE");
        request.setSelectedDistributionDepartmentIds(java.util.List.of());
        request.setOriginalUploadTicket("source-ticket");
        request.setSourceUploadTicket("source-ticket");
        request.setSessionId(DccSourceUploadSession.scope(
                DccSourceUploadSession.newUploadPrefix(20L, 30L, "Procedure.pdf"), "client-session"));
        return request;
    }

    @Test void originalTicketPassesBothSubmissionAndDraftEntryValidation() {
        var request = request();
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(workflow,
                "validateSubmitRequest", request, true, true, true));
        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(workflow,
                "validateSubmitRequest", request, true, true, false));
    }

    @Test void readOnlyTicketCannotReplaceRequiredOriginalAtEntry() {
        var request = request();
        request.setOriginalUploadTicket(null);
        request.setSourceUploadTicket(null);
        request.setReadOnlyUploadTicket("old-read-only-ticket");
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> ReflectionTestUtils.invokeMethod(workflow,
                        "validateSubmitRequest", request, true, true, true));
        assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> ReflectionTestUtils.invokeMethod(workflow,
                        "validateSubmitRequest", request, true, true, false));
    }

    @Test void officeOriginalBindsOnceWithoutSeparateReadOnlyOrEditableUpload() {
        var request = request();
        request.setFileName("Procedure.docx");
        request.setSessionId(DccSourceUploadSession.scope(
                DccSourceUploadSession.newUploadPrefix(20L, 30L, "Procedure.docx"), "client-session"));
        var source = new DccUploadTicketResolveCommand("source-ticket", 99L, 10L, request.getSessionId(), "SOURCE");
        when(tickets.resolveForBinding(source)).thenReturn(new DccUploadTicketBoundFile("source-ticket", 102L, "Procedure.docx", "application/octet-stream", 20L));
        Object result = ReflectionTestUtils.invokeMethod(workflow, "resolveSubmitFiles", 99L, request, true);
        assertNull(ReflectionTestUtils.getField(result, "readOnlyFileId"));
        assertEquals(102L, ReflectionTestUtils.getField(result, "editableFileId"));
        assertEquals(102L, ReflectionTestUtils.getField(result, "originalFileId"));
        assertEquals(102L, ReflectionTestUtils.getField(result, "sourceFileId"));
        assertEquals(1, ((java.util.List<?>) ReflectionTestUtils.getField(result, "ticketBindings")).size());
        verify(tickets).resolveForBinding(source);
        verifyNoMoreInteractions(tickets);
    }

    @Test void pdfOriginalUsesTheSameSourcePurpose() {
        var request = request();
        var pdf = new DccUploadTicketResolveCommand("source-ticket", 99L, 10L, request.getSessionId(), "SOURCE");
        when(tickets.resolveForBinding(pdf)).thenReturn(new DccUploadTicketBoundFile("source-ticket", 101L, "Procedure.pdf", "application/pdf", 10L));
        Object result = ReflectionTestUtils.invokeMethod(workflow, "resolveSubmitFiles", 99L, request, true);
        assertNull(ReflectionTestUtils.getField(result, "readOnlyFileId"));
        assertEquals(101L, ReflectionTestUtils.getField(result, "editableFileId"));
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

    @Test void mismatchedOriginalAndSourceTicketsAreRejected() {
        var request = request();
        request.setSourceUploadTicket("another-file-ticket");
        assertThrows(RuntimeException.class, () -> ReflectionTestUtils.invokeMethod(workflow, "resolveSubmitFiles", 99L, request, true));
        verifyNoInteractions(tickets);
    }

    @Test void externalReviewKeepsOneSourceTicketAndItsIndependentSessionScope() {
        var request = request();
        request.setProcessType("EXTERNAL_REVIEW");
        request.setSessionId(DccSourceUploadSession.scope(DccSourceUploadSession.externalPrefix(), "external-session"));
        var source = new DccUploadTicketResolveCommand("source-ticket", 99L, 10L, request.getSessionId(), "SOURCE");
        when(tickets.resolveForBinding(source)).thenReturn(new DccUploadTicketBoundFile("source-ticket", 101L, "Procedure.pdf", "application/pdf", 10L));
        Object result = ReflectionTestUtils.invokeMethod(workflow, "resolveSubmitFiles", 99L, request, true);
        assertEquals(101L, ReflectionTestUtils.getField(result, "sourceFileId"));
        assertNull(ReflectionTestUtils.getField(result, "editableFileId"));
        verify(tickets).resolveForBinding(source);
        verifyNoMoreInteractions(tickets);
    }

    @Test void attachmentsAndDrawingRetainTheirOwnPurposeAndSession() {
        var request = request();
        request.setDrawingPdfUploadTicket("drawing-ticket");
        var attachment = new cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileAttachmentUploadTicketReqVO();
        attachment.setUploadTicket("attachment-ticket");
        attachment.setSessionId(DccSourceUploadSession.scope(
                DccSourceUploadSession.newUploadPrefix(20L, 30L, "Procedure.pdf"), "attachment-session"));
        request.setAttachmentUploadTickets(java.util.List.of(attachment));
        for (var binding : java.util.List.of(
                new DccUploadTicketResolveCommand("source-ticket", 99L, 10L, request.getSessionId(), "SOURCE"),
                new DccUploadTicketResolveCommand("drawing-ticket", 99L, 10L, request.getSessionId(), "DRAWING_PDF"),
                new DccUploadTicketResolveCommand("attachment-ticket", 99L, 10L, attachment.getSessionId(), "ATTACHMENT"))) {
            when(tickets.resolveForBinding(binding)).thenReturn(new DccUploadTicketBoundFile(binding.uploadTicket(),
                    101L, "Procedure.pdf", "application/pdf", 10L));
        }
        Object result = ReflectionTestUtils.invokeMethod(workflow, "resolveSubmitFiles", 99L, request, true);
        assertEquals(3, ((java.util.List<?>) ReflectionTestUtils.getField(result, "ticketBindings")).size());
        assertEquals(1, ((java.util.List<?>) ReflectionTestUtils.getField(result, "attachmentFiles")).size());
    }
}
