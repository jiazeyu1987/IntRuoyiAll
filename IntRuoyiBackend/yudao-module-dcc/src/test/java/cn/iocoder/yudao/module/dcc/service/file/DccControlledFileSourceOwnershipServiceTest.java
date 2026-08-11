package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSourceOwnershipDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSourceOwnershipMapper;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.dao.DuplicateKeyException;

import java.nio.charset.StandardCharsets;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SOURCE_OWNERSHIP_CONFLICT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccControlledFileSourceOwnershipServiceTest extends BaseMockitoUnitTest {

    @Mock
    private DccControlledFileMapper controlledFileMapper;
    @Mock
    private DccControlledFileSourceOwnershipMapper ownershipMapper;
    @Mock
    private FileMapper fileMapper;
    @Mock
    private FileService fileService;
    @InjectMocks
    private DccControlledFileSourceOwnershipService service;

    @BeforeEach
    void setTenant() {
        TenantContextHolder.setTenantId(31L);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void prepareSubmissionSource_unreferencedTicketSourceKeepsDedicatedFileRecord() throws Exception {
        byte[] content = "source-a".getBytes(StandardCharsets.UTF_8);
        stubFile(700L, "source-a.docx", "dcc/temp/source-a.docx", content);
        when(controlledFileMapper.countAllBySourceFileId(31L, 700L)).thenReturn(0L);
        when(ownershipMapper.selectBySourceFileId(31L, 700L)).thenReturn(null);

        DccControlledFilePreparedSource result = service.prepareSubmissionSource(700L, false);

        assertEquals(700L, result.sourceFileId());
        assertEquals(700L, result.originSourceFileId());
        assertFalse(result.isolatedCopy());
        verify(fileService, never()).createFileAndReturnId(any(byte[].class), any(), any(), any());
    }

    @Test
    void prepareSubmissionSource_rawReferenceCreatesAndVerifiesIndependentCopy() throws Exception {
        byte[] content = "source-b".getBytes(StandardCharsets.UTF_8);
        stubFile(701L, "source-b.docx", "nas/inbound/source-b.docx", content);
        when(fileService.createFileAndReturnId(content, "source-b.docx", "dcc/source-owned",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document")).thenReturn(1701L);
        stubFile(1701L, "source-b.docx", "dcc/source-owned/source-b.docx", content);

        DccControlledFilePreparedSource result = service.prepareSubmissionSource(701L, true);

        assertEquals(1701L, result.sourceFileId());
        assertEquals(701L, result.originSourceFileId());
        assertTrue(result.isolatedCopy());
        assertNotEquals("", result.sourceSha256());
    }

    @Test
    void prepareSubmissionSource_ticketSourceAlreadyReferencedCreatesIndependentCopy() throws Exception {
        byte[] content = "source-c".getBytes(StandardCharsets.UTF_8);
        stubFile(702L, "source-c.docx", "dcc/temp/source-c.docx", content);
        when(controlledFileMapper.countAllBySourceFileId(31L, 702L)).thenReturn(1L);
        when(fileService.createFileAndReturnId(content, "source-c.docx", "dcc/source-owned",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document")).thenReturn(1702L);
        stubFile(1702L, "source-c.docx", "dcc/source-owned/source-c.docx", content);

        DccControlledFilePreparedSource result = service.prepareSubmissionSource(702L, false);

        assertEquals(1702L, result.sourceFileId());
        assertTrue(result.isolatedCopy());
    }

    @Test
    void claimSubmissionSource_persistsTenantScopedOwnershipAndReportsConflict() {
        DccControlledFilePreparedSource prepared =
                new DccControlledFilePreparedSource(1703L, 703L, "abc123", true);

        service.claimSubmissionSource(900L, prepared, 120L, "SUBMISSION");

        ArgumentCaptor<DccControlledFileSourceOwnershipDO> captor =
                ArgumentCaptor.forClass(DccControlledFileSourceOwnershipDO.class);
        verify(ownershipMapper).insert(captor.capture());
        assertEquals(31L, captor.getValue().getTenantId());
        assertEquals(900L, captor.getValue().getControlledFileId());
        assertEquals(1703L, captor.getValue().getSourceFileId());
        assertEquals(703L, captor.getValue().getOriginSourceFileId());

        when(ownershipMapper.insert(any(DccControlledFileSourceOwnershipDO.class)))
                .thenThrow(new DuplicateKeyException("duplicate source owner"));
        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.claimSubmissionSource(901L, prepared, 120L, "SUBMISSION"));
        assertEquals(CONTROLLED_FILE_SOURCE_OWNERSHIP_CONFLICT.getCode(), ex.getCode());
    }

    private void stubFile(Long fileId, String name, String path, byte[] content) throws Exception {
        when(fileMapper.selectById(fileId)).thenReturn(FileDO.builder()
                .id(fileId).configId(8L).name(name).path(path)
                .type("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                .size((long) content.length).build());
        when(fileService.getFileContent(8L, path)).thenReturn(content);
    }
}
