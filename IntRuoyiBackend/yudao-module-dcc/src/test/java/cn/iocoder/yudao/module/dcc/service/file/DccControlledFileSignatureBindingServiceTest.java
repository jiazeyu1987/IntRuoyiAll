package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSignatureBindingDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSignatureDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSignatureBindingMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileSignatureMapper;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SIGNATURE_BINDING_FAILED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DccControlledFileSignatureBindingServiceTest extends BaseMockitoUnitTest {

    @Mock
    private DccControlledFileSignatureBindingMapper bindingMapper;
    @Mock
    private DccControlledFileSignatureMapper signatureMapper;
    @Mock
    private FileService fileService;
    @InjectMocks
    private DccControlledFileSignatureBindingService service;

    private final byte[] publishedPdf = "%PDF-1.7-controlled-copy".getBytes(StandardCharsets.UTF_8);

    @BeforeEach
    void setUpTenant() {
        TenantContextHolder.setTenantId(1L);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void bindPublishedCopy_createsImmutableBindingForEverySignatureWithoutUpdatingOriginalEvidence() throws Exception {
        DccControlledFileDO file = publishedFile();
        List<DccControlledFileSignatureDO> signatures = List.of(signature(1001L, "evidence-1"),
                signature(1002L, "evidence-2"), signature(1003L, "evidence-3"),
                signature(1004L, "evidence-4"));
        when(signatureMapper.selectListByControlledFileId(900L)).thenReturn(signatures);
        when(fileService.getFile(800L)).thenReturn(publishedFileRecord());
        when(fileService.getFileContent(7L, "dcc/published/900.pdf")).thenReturn(publishedPdf);

        service.bindPublishedCopy(file, 800L, 99L, "process-900");

        ArgumentCaptor<DccControlledFileSignatureBindingDO> captor =
                ArgumentCaptor.forClass(DccControlledFileSignatureBindingDO.class);
        verify(bindingMapper, org.mockito.Mockito.times(4)).insert(captor.capture());
        assertEquals(List.of(1001L, 1002L, 1003L, 1004L),
                captor.getAllValues().stream().map(DccControlledFileSignatureBindingDO::getSignatureId).toList());
        assertEquals(List.of("evidence-1", "evidence-2", "evidence-3", "evidence-4"),
                captor.getAllValues().stream().map(DccControlledFileSignatureBindingDO::getOriginalEvidenceHash).toList());
        assertTrue(captor.getAllValues().stream().allMatch(binding -> binding.getControlledCopyFileId().equals(800L)));
        assertTrue(captor.getAllValues().stream().allMatch(binding ->
                "dcc/published/900.pdf".equals(binding.getControlledCopyObjectKey())));
        assertTrue(captor.getAllValues().stream().allMatch(binding -> binding.getControlledCopySha256().length() == 64));
        assertTrue(captor.getAllValues().stream().allMatch(binding -> binding.getBindingHash().length() == 64));
        verify(signatureMapper, never()).updateById(any(DccControlledFileSignatureDO.class));
    }

    @Test
    void bindPublishedCopy_existingDifferentCopyFailsWithoutOverwrite() throws Exception {
        DccControlledFileDO file = publishedFile();
        DccControlledFileSignatureDO signature = signature(1001L, "evidence-1");
        when(signatureMapper.selectListByControlledFileId(900L)).thenReturn(List.of(signature));
        when(fileService.getFile(800L)).thenReturn(publishedFileRecord());
        when(fileService.getFileContent(7L, "dcc/published/900.pdf")).thenReturn(publishedPdf);
        when(bindingMapper.selectBySignatureId(1001L)).thenReturn(DccControlledFileSignatureBindingDO.builder()
                .id(2001L).signatureId(1001L).controlledFileId(900L).originalEvidenceHash("evidence-1")
                .controlledCopyFileId(801L).controlledCopySha256("different").build());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.bindPublishedCopy(file, 800L, 99L, "process-900"));

        assertEquals(CONTROLLED_FILE_SIGNATURE_BINDING_FAILED.getCode(), ex.getCode());
        verify(bindingMapper, never()).insert(any(DccControlledFileSignatureBindingDO.class));
        verify(bindingMapper, never()).updateById(any(DccControlledFileSignatureBindingDO.class));
    }

    @Test
    void bindPublishedCopy_sameExistingBindingIsIdempotent() throws Exception {
        DccControlledFileDO file = publishedFile();
        DccControlledFileSignatureDO signature = signature(1001L, "evidence-1");
        DccControlledFileSignatureBindingDO existing = service.createBindingEvent(signature, file, 800L,
                "dcc/published/900.pdf", publishedPdf, 99L, "dcc-final-approval:900:task-4");
        when(signatureMapper.selectListByControlledFileId(900L)).thenReturn(List.of(signature));
        when(fileService.getFile(800L)).thenReturn(publishedFileRecord());
        when(fileService.getFileContent(7L, "dcc/published/900.pdf")).thenReturn(publishedPdf);
        when(bindingMapper.selectBySignatureId(1001L)).thenReturn(existing);

        service.bindPublishedCopy(file, 800L, 99L, "process-900");

        verify(bindingMapper, never()).insert(any(DccControlledFileSignatureBindingDO.class));
        verify(bindingMapper, never()).updateById(any(DccControlledFileSignatureBindingDO.class));
    }

    @Test
    void verifyPublishedCopyBinding_detectsChangedPdfContent() throws Exception {
        DccControlledFileDO file = publishedFile();
        DccControlledFileSignatureDO signature = signature(1001L, "evidence-1");
        DccControlledFileSignatureBindingDO binding = service.createBindingEvent(signature, file, 800L,
                "dcc/published/900.pdf", publishedPdf, 99L, "process-900");
        when(bindingMapper.selectBySignatureId(1001L)).thenReturn(binding);
        when(fileService.getFile(800L)).thenReturn(publishedFileRecord());
        when(fileService.getFileContent(7L, "dcc/published/900.pdf"))
                .thenReturn("%PDF-tampered".getBytes(StandardCharsets.UTF_8));

        DccControlledFileSignatureBindingVerification result =
                service.verifyPublishedCopyBinding(signature, file);

        assertFalse(result.valid());
        assertEquals("CONTROLLED_COPY_HASH_MISMATCH", result.reasonCode());
    }

    private DccControlledFileDO publishedFile() {
        return DccControlledFileDO.builder().id(900L).publishedFileId(800L).stampedFileId(800L).build();
    }

    private FileDO publishedFileRecord() {
        return FileDO.builder().id(800L).configId(7L).path("dcc/published/900.pdf").build();
    }

    private DccControlledFileSignatureDO signature(Long id, String evidenceHash) {
        return DccControlledFileSignatureDO.builder().id(id).controlledFileId(900L)
                .evidenceHash(evidenceHash).build();
    }

}
