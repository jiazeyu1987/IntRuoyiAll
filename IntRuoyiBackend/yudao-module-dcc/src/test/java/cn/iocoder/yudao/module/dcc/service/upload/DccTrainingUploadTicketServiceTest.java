package cn.iocoder.yudao.module.dcc.service.upload;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileTemporaryFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileTemporaryFileMapper;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import java.time.LocalDateTime;
import java.util.function.Consumer;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_UPLOAD_TICKET_INVALID;

class DccTrainingUploadTicketServiceTest extends BaseMockitoUnitTest {
    @Mock private DccControlledFileTemporaryFileMapper temporaryFileMapper;
    @Mock private FileMapper fileMapper;
    @Mock private FileService fileService;
    @InjectMocks private DccUploadTicketServiceImpl service;
    private static final String HASH="0123456789abcdef".repeat(4);
    @BeforeEach void tenant(){TenantContextHolder.setTenantId(31L);}
    @AfterEach void clear(){TenantContextHolder.clear();}
    private DccUploadTicketResolveCommand command(){return new DccUploadTicketResolveCommand("UT-TRAIN",99L,10L,"session-1","TRAINING_RECORD");}
    private DccControlledFileTemporaryFileDO ticket(){
        var row=DccControlledFileTemporaryFileDO.builder().id(9001L).uploadTicket("UT-TRAIN").sessionId("session-1")
            .purpose("TRAINING_RECORD").categoryId(10L).uploaderId(99L).originalFileName("training.pdf")
            .contentType("application/pdf").fileSize(128L).fileSha256(HASH).storageFileId(700L)
            .status("AVAILABLE").cleanupStatus("ACTIVE").expireTime(LocalDateTime.now().plusMinutes(20)).build();
        row.setTenantId(31L);return row;
    }
    private void given(DccControlledFileTemporaryFileDO row){when(temporaryFileMapper.selectOne(
        org.mockito.ArgumentMatchers.<SFunction<DccControlledFileTemporaryFileDO,?>>any(),eq("UT-TRAIN"))).thenReturn(row);}
    @Test void sameValidatedRowSuppliesOriginalDigestAndAllMetadataInOneReadWithoutBinding(){
        var row=ticket();given(row);
        when(fileMapper.selectById(700L)).thenReturn(FileDO.builder().id(700L).name("training.pdf").type("application/pdf").build());
        DccUploadTicketService api=service;
        DccTrainingUploadTicketBoundFile result=api.resolveTrainingForBinding(command());
        assertEquals(HASH,result.fileSha256());assertEquals(new DccUploadTicketBoundFile("UT-TRAIN",700L,"training.pdf","application/pdf",128L),result.file());
        verify(temporaryFileMapper,times(1)).selectOne(org.mockito.ArgumentMatchers.<SFunction<DccControlledFileTemporaryFileDO,?>>any(),eq("UT-TRAIN"));
        verify(fileMapper,times(1)).selectById(700L);verifyNoMoreInteractions(temporaryFileMapper,fileMapper);verifyNoInteractions(fileService);
        assertEquals("AVAILABLE",row.getStatus());assertNull(row.getBoundControlledFileId());assertEquals(HASH,row.getFileSha256());
    }
    @Test void absentOrInvalidOriginalDigestNeverReadsMetadataOrMakesUpAHash(){
        for(String hash:new String[]{null,""," ","a".repeat(63),"a".repeat(65),"A".repeat(64),"g".repeat(64)}){
            var row=ticket();row.setFileSha256(hash);given(row);
            var error=assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->service.resolveTrainingForBinding(command()));
            assertEquals(CONTROLLED_FILE_UPLOAD_TICKET_INVALID.getCode(),error.getCode());assertEquals(hash,row.getFileSha256());
        }
        verifyNoInteractions(fileMapper,fileService);
    }
    @Test void wrongPurposeCannotUseTheTrainingResolver(){
        for(String purpose:new String[]{"SOURCE","DRAWING_PDF","ATTACHMENT","APPROVAL_PDF","EXTERNAL_REVIEW_OUTPUT",null})
            assertThrows(RuntimeException.class,()->service.resolveTrainingForBinding(new DccUploadTicketResolveCommand("UT-TRAIN",99L,10L,"session-1",purpose)));
        verifyNoInteractions(temporaryFileMapper,fileMapper,fileService);
    }
    @Test void originalTicketValidityAndExactTenantAndTicketIdentityRemainMandatory(){
        List<Consumer<DccControlledFileTemporaryFileDO>> changes=List.of(row->row.setUploaderId(100L),row->row.setCategoryId(11L),
            row->row.setSessionId("other"),row->row.setPurpose("SOURCE"),row->row.setStatus("BOUND"),row->row.setCleanupStatus("CLEANING"),
            row->row.setBoundControlledFileId(88L),row->row.setExpireTime(LocalDateTime.now().minusSeconds(1)),row->row.setExpireTime(null),
            row->row.setStorageFileId(null),row->row.setTenantId(32L),row->row.setUploadTicket("UT-OTHER"));
        for(var change:changes){var row=ticket();change.accept(row);given(row);assertThrows(RuntimeException.class,()->service.resolveTrainingForBinding(command()));}
        verifyNoInteractions(fileMapper,fileService);
    }
    @Test void missingTenantOrIgnoredTenantCannotAuthorizeTheTrainingRead(){
        TenantContextHolder.clear();assertThrows(RuntimeException.class,()->service.resolveTrainingForBinding(command()));
        TenantContextHolder.setTenantId(31L);TenantContextHolder.setIgnore(true);
        assertThrows(RuntimeException.class,()->service.resolveTrainingForBinding(command()));verifyNoInteractions(temporaryFileMapper,fileMapper,fileService);
    }
    @Test void missingOrMismatchedStorageMetadataCannotBecomeTrainingEvidence(){
        given(ticket());assertThrows(RuntimeException.class,()->service.resolveTrainingForBinding(command()));
        when(fileMapper.selectById(700L)).thenReturn(FileDO.builder().id(701L).name("foreign.pdf").type("application/pdf").build());
        assertThrows(RuntimeException.class,()->service.resolveTrainingForBinding(command()));verifyNoInteractions(fileService);
    }
    @Test void storageReadTechnicalFailurePropagatesExactlyWithoutMarkingBound(){
        var row=ticket();given(row);var failure=new IllegalStateException("actual storage metadata read failed");
        when(fileMapper.selectById(700L)).thenThrow(failure);
        assertSame(failure,assertThrows(IllegalStateException.class,()->service.resolveTrainingForBinding(command())));
        assertEquals("AVAILABLE",row.getStatus());verifyNoInteractions(fileService);
    }
}
