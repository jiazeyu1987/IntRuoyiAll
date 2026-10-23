package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.dcc.controller.admin.file.DccControlledFileController;
import cn.iocoder.yudao.module.dcc.dal.dataobject.category.DccFileCategoryDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper;
import cn.iocoder.yudao.module.dcc.service.upload.*;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;

import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

/** Actual Controller binding/UploadService/source context + real H2 cancelled native state.
 * Ticket issuance/watermark/size policy are explicit isolated ports; no fake browser success. */
class DccWithdrawnCheckinMultipartContextDatabaseTest extends DccWithdrawnNativeRevisionReworkDatabaseTest {
    @Test
    void currentCancelledNativeHeldLockAcceptsFormalMultipartSourceContext() throws Exception {
        long file=cancelledRevision();checkout(file);
        var category=mock(DccFileCategoryMapper.class);
        when(category.selectById(2L)).thenReturn(DccFileCategoryDO.builder().id(2L).active(true).lifecycleStage("OUTPUT").build());
        wire(workflow,"categoryMapper",category,"queryService",query);
        var uploads=new DccControlledFileUploadServiceImpl();defaults(uploads);
        wire(uploads,"workflowService",workflow,"categoryMapper",category,"permissionSupport",permissions,
                "fileService",storage,"fileMapper",infraFiles,"onlyOfficePreviewProperties",new DccOnlyOfficePreviewProperties());
        var tickets=mock(DccUploadTicketService.class);
        when(tickets.createTicket(any())).thenAnswer(call->{
            DccUploadTicketCreateCommand cmd=call.getArgument(0);
            return new DccUploadTicketCreated("G69-TICKET",cmd.sessionId(),cmd.purpose(),"AVAILABLE",
                    LocalDateTime.now().plusMinutes(10),cmd.storageFileId(),cmd.originalFileName(),cmd.contentType(),cmd.fileSize());
        });
        wire(uploads,"uploadTicketService",tickets);
        var watermarks=mock(DccControlledPreviewWatermarkService.class);
        when(watermarks.build(anyLong(),anyString(),anyString())).thenReturn(DccControlledPreviewWatermarkRespVO.builder()
                .label("隔离预览").text("隔离预览").actorName("测试申请人").actorAccount("isolated.actor")
                .timestamp("2026-10-06 08:00:00").purpose("preview")
                .overlay(DccControlledPreviewWatermarkOverlayRespVO.builder().textColor("#6b7280").opacity(0.18D)
                        .rotationDeg(-24).gapX(260).gapY(180).fontSize(18).build()).build());
        wire(uploads,"watermarkService",watermarks);
        var controller=new DccControlledFileController();
        ReflectionTestUtils.setField(controller,"uploadService",transactionProxy(uploads));
        var bytes=new ByteArrayOutputStream();try(var document=new PDDocument()){document.addPage(new PDPage());document.save(bytes);}
        var upload=new MockMultipartFile("files","SOP.pdf","application/pdf",bytes.toByteArray());
        try(var login=mockStatic(SecurityFrameworkUtils.class)) {
            login.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(99L);
            MockMvcBuilders.standaloneSetup(controller).build().perform(multipart("/dcc/controlled-files/upload-preview")
                    .file(upload).param("categoryId","2").param("sessionId","g69-current-session")
                    .param("purpose","SOURCE").param("uploadContext","CHECKIN").param("controlledFileId",String.valueOf(file))
                    .header("tenant-id","1").header("X-DCC-Request-Id","G69-ACTUAL-MULTIPART").header("User-Agent","G69-MockMvc"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.uploadTicket").value("G69-TICKET"));
        }
        var command=org.mockito.ArgumentCaptor.forClass(DccUploadTicketCreateCommand.class);
        verify(tickets).createTicket(command.capture());
        assertEquals(DccSourceUploadSession.scope(DccSourceUploadSession.checkinPrefix(file),"g69-current-session"),command.getValue().sessionId());
        assertEquals(99L,checkouts.selectActiveByMasterIdForRead(1L,10L).getActorId());
        assertEquals(file,checkouts.selectActiveByMasterIdForRead(1L,10L).getBaseIterationId());
        assertEquals("WITHDRAWN",files.selectById(file).getStatus());
    }

    @Test
    void currentCancelledUploadContextRejectsAnotherActorWrongBaseAndForeignCategoryWithoutChangingTheLock() {
        long file=cancelledRevision();checkout(file);
        var category=mock(DccFileCategoryMapper.class);
        when(category.selectById(2L)).thenReturn(DccFileCategoryDO.builder().id(2L).active(true).lifecycleStage("OUTPUT").build());
        when(category.selectById(3L)).thenReturn(DccFileCategoryDO.builder().id(3L).active(true).lifecycleStage("OUTPUT").build());
        wire(workflow,"categoryMapper",category,"queryService",query);
        var req=new DccControlledFileUploadPreviewReqVO();req.setControlledFileId(file);req.setCategoryId(2L);req.setUploadContext("CHECKIN");
        assertThrows(RuntimeException.class,()->workflow.validateSourceUploadContext(88L,req));
        req.setCategoryId(3L);assertThrows(RuntimeException.class,()->workflow.validateSourceUploadContext(99L,req));req.setCategoryId(2L);
        jdbc.update("UPDATE dcc_controlled_file_checkout SET base_iteration_id=20 WHERE tenant_id=1 AND master_id=10 AND status='ACTIVE'");
        assertThrows(RuntimeException.class,()->workflow.validateSourceUploadContext(99L,req));
        assertEquals(99L,checkouts.selectActiveByMasterIdForRead(1L,10L).getActorId());
        assertEquals("WITHDRAWN",files.selectById(file).getStatus());
    }
}
