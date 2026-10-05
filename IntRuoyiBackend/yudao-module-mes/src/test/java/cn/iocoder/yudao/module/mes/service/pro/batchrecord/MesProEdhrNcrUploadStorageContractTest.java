package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.framework.file.core.client.FileClient;
import cn.iocoder.yudao.module.infra.service.file.FileConfigService;
import cn.iocoder.yudao.module.infra.service.file.FileServiceImpl;
import cn.iocoder.yudao.module.infra.service.file.FileUploadSecurityPolicy;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.MesProEdhrNonconformanceReviewController;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrNonconformanceReviewDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrNonconformanceReviewMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MesProEdhrNcrUploadStorageContractTest {
    @Test
    void uploadRejectsRecordFromAnotherUploadOfSameReview() {
        assertReturnedRecordRejected("directory");
    }

    @Test
    void uploadRejectsChangedOriginalName() {
        assertReturnedRecordRejected("name");
    }

    @Test
    void uploadRejectsChangedContentLength() {
        assertReturnedRecordRejected("size");
    }

    private void assertReturnedRecordRejected(String mismatch) {
        var files = mock(cn.iocoder.yudao.module.infra.service.file.FileService.class);
        var reviews = mock(MesProEdhrNonconformanceReviewMapper.class);
        when(reviews.selectById(1001L)).thenReturn(new MesProEdhrNonconformanceReviewDO()
                .setId(1001L).setReviewStatus("pending_review"));
        byte[] bytes = "%PDF-1.7 current".getBytes(StandardCharsets.UTF_8);
        FileDO record = new FileDO().setId(70001L).setConfigId(7L)
                .setName("current.pdf").setSize((long) bytes.length).setUrl("https://storage.example/current");
        when(files.createFileAndReturnId(any(byte[].class), anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> {
                    String directory = invocation.getArgument(2);
                    record.setPath(("directory".equals(mismatch)
                            ? "mes/edhr-ncr/reviews/1001/another-upload" : directory) + "/20260929/current.pdf");
                    if ("name".equals(mismatch)) record.setName("other.pdf");
                    if ("size".equals(mismatch)) record.setSize((long) bytes.length + 1);
                    return record.getId();
                });
        when(files.getFile(70001L)).thenReturn(record);
        var service = new MesProEdhrNonconformanceReviewServiceImpl();
        { org.springframework.test.util.ReflectionTestUtils.setField(service, "handoffService", org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.service.pro.handoff.MesActiveOrderHandoffService.class)); }
        ReflectionTestUtils.setField(service, "reviewMapper", reviews);
        ReflectionTestUtils.setField(service, "fileService", files);
        ReflectionTestUtils.setField(service, "fileUploadSecurityPolicy", new FileUploadSecurityPolicy());
        assertThrows(IllegalStateException.class,
                () -> service.uploadMaterial(1001L, "current.pdf", "application/pdf", bytes));
        verify(reviews, never()).updateById(any(MesProEdhrNonconformanceReviewDO.class));
    }

    @Test
    void sameNameUploadsKeepBothPhysicalContentsThroughRealFileService() throws Exception {
        Map<String, byte[]> storage = new HashMap<>();
        Map<Long, FileDO> records = new HashMap<>();
        AtomicLong sequence = new AtomicLong(9007199254740992L);
        FileClient client = mock(FileClient.class);
        FileConfigService configuration = mock(FileConfigService.class);
        FileMapper fileMapper = mock(FileMapper.class);
        when(configuration.getMasterFileClient()).thenReturn(client);
        when(client.getId()).thenReturn(7L);
        when(client.upload(any(byte[].class), anyString(), anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(1);
            storage.put(key, ((byte[]) invocation.getArgument(0)).clone());
            return "https://storage.example/" + key + "?signature=local-test";
        });
        when(fileMapper.insert(any(FileDO.class))).thenAnswer(invocation -> {
            FileDO file = invocation.getArgument(0);
            file.setId(sequence.incrementAndGet());
            records.put(file.getId(), file);
            return 1;
        });
        when(fileMapper.selectById(anyLong())).thenAnswer(invocation -> records.get(invocation.getArgument(0)));
        FileServiceImpl files = new FileServiceImpl();
        ReflectionTestUtils.setField(files, "fileConfigService", configuration);
        ReflectionTestUtils.setField(files, "fileMapper", fileMapper);
        MesProEdhrNonconformanceReviewMapper reviews = mock(MesProEdhrNonconformanceReviewMapper.class);
        when(reviews.selectById(1001L)).thenReturn(new MesProEdhrNonconformanceReviewDO()
                .setId(1001L).setReviewStatus("pending_review"));
        MesProEdhrNonconformanceReviewServiceImpl service = new MesProEdhrNonconformanceReviewServiceImpl();
        { org.springframework.test.util.ReflectionTestUtils.setField(service, "handoffService", org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.service.pro.handoff.MesActiveOrderHandoffService.class)); }
        ReflectionTestUtils.setField(service, "reviewMapper", reviews);
        ReflectionTestUtils.setField(service, "fileService", files);
        ReflectionTestUtils.setField(service, "fileUploadSecurityPolicy", new FileUploadSecurityPolicy());
        MesProEdhrNonconformanceReviewController controller = new MesProEdhrNonconformanceReviewController();
        ReflectionTestUtils.setField(controller, "nonconformanceReviewService", service);
        String name = "  原件+100%25.pdf  ";
        byte[] firstBytes = "%PDF-1.7 first".getBytes(StandardCharsets.UTF_8);
        byte[] secondBytes = "%PDF-1.7 second".getBytes(StandardCharsets.UTF_8);
        var first = controller.uploadMaterial(1001L,
                new MockMultipartFile("file", name, "application/pdf", firstBytes)).getData();
        var second = controller.uploadMaterial(1001L,
                new MockMultipartFile("file", name, "application/pdf", secondBytes)).getData();
        assertNotEquals(first.getFileId(), second.getFileId());
        assertNotEquals(first.getPath(), second.getPath(), "independent evidence needs independent storage keys");
        assertArrayEquals(firstBytes, storage.get(first.getPath()));
        assertArrayEquals(secondBytes, storage.get(second.getPath()));
        assertEquals(name, first.getFileName());
        assertEquals(name, second.getFileName());
        assertEquals(records.get(first.getFileId()).getUrl(), first.getUrl());
        assertEquals(2, storage.size());
    }
}
