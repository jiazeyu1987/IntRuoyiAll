package cn.iocoder.yudao.module.infra.service.file;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.ObjectUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.test.core.util.AssertUtils;
import cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FilePageReqVO;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.framework.file.core.client.FileClient;
import cn.iocoder.yudao.module.infra.framework.file.core.client.StorageRetentionEvidence;
import cn.iocoder.yudao.module.infra.framework.file.core.client.StorageRetentionPolicy;
import cn.iocoder.yudao.module.infra.framework.file.core.client.s3.S3FileClient;
import cn.iocoder.yudao.module.infra.service.file.access.FileDirectLinkAccessContext;
import cn.iocoder.yudao.module.infra.service.file.access.FileDirectLinkAccessGuard;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static cn.iocoder.yudao.framework.common.util.date.LocalDateTimeUtils.buildTime;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.*;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.FILE_DIRECT_LINK_BLOCKED_BY_DCC;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.FILE_NOT_EXISTS;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.FILE_PROTECTED_SHOWROOM_MEDIA;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

@Import({FileServiceImpl.class})
public class FileServiceImplTest extends BaseDbUnitTest {

    @Resource
    private FileServiceImpl fileService;

    @Resource
    private FileMapper fileMapper;

    @MockitoBean
    private FileConfigService fileConfigService;
    @MockitoBean
    private FileDirectLinkAccessGuard fileDirectLinkAccessGuard;

    @BeforeEach
    public void setUp() {
        FileServiceImpl.PATH_PREFIX_DATE_ENABLE = true;
        FileServiceImpl.PATH_SUFFIX_TIMESTAMP_ENABLE = true;
        FileServiceImpl.PATH_SUFFIX_AS_DIRECTORY = true;
    }

    @Test
    public void testGetFilePage() {
        // mock 数据
        FileDO dbFile = randomPojo(FileDO.class, o -> { // 等会查询到
            o.setPath("yunai");
            o.setType("image/jpg");
            o.setCreateTime(buildTime(2021, 1, 15));
        });
        fileMapper.insert(dbFile);
        // 测试 path 不匹配
        fileMapper.insert(ObjectUtils.cloneIgnoreId(dbFile, o -> o.setPath("tudou")));
        // 测试 type 不匹配
        fileMapper.insert(ObjectUtils.cloneIgnoreId(dbFile, o -> {
            o.setType("image/png");
        }));
        // 测试 createTime 不匹配
        fileMapper.insert(ObjectUtils.cloneIgnoreId(dbFile, o -> {
            o.setCreateTime(buildTime(2020, 1, 15));
        }));
        // 准备参数
        FilePageReqVO reqVO = new FilePageReqVO();
        reqVO.setPath("yunai");
        reqVO.setType("jp");
        reqVO.setCreateTime((new LocalDateTime[]{buildTime(2021, 1, 10), buildTime(2021, 1, 20)}));

        // 调用
        PageResult<FileDO> pageResult = fileService.getFilePage(reqVO);
        // 断言
        assertEquals(1, pageResult.getTotal());
        assertEquals(1, pageResult.getList().size());
        AssertUtils.assertPojoEquals(dbFile, pageResult.getList().get(0));
    }

    /**
     * content、name、directory、type 都非空
     */
    @Test
    public void testCreateFile_success_01() throws Exception {
        // 准备参数
        byte[] content = ResourceUtil.readBytes("file/erweima.jpg");
        String name = "单测文件名";
        String directory = randomString();
        String type = "image/jpeg";
        // mock Master 文件客户端
        FileClient client = mock(FileClient.class);
        when(fileConfigService.getMasterFileClient()).thenReturn(client);
        String url = randomString();
        AtomicReference<String> pathRef = new AtomicReference<>();
        when(client.upload(same(content), argThat(path -> {
            assertTrue(path.matches(directory + "/\\d{8}/\\d+/" + name + ".jpg"));
            pathRef.set(path);
            return true;
        }), eq(type))).thenReturn(url);
        when(client.getId()).thenReturn(10L);
        // 调用
        String result = fileService.createFile(content, name, directory, type);
        // 断言
        assertEquals(result, url);
        // 校验数据
        FileDO file = fileMapper.selectOne(FileDO::getUrl, url);
        assertEquals(10L, file.getConfigId());
        assertEquals(pathRef.get(), file.getPath());
        assertEquals(url, file.getUrl());
        assertEquals(type, file.getType());
        assertEquals(content.length, file.getSize());
    }

    /**
     * content 非空，其它都空
     */
    @Test
    public void testCreateFile_success_02() throws Exception {
        // 准备参数
        byte[] content = ResourceUtil.readBytes("file/erweima.jpg");
        // mock Master 文件客户端
        String type = "image/jpeg";
        FileClient client = mock(FileClient.class);
        when(fileConfigService.getMasterFileClient()).thenReturn(client);
        String url = randomString();
        AtomicReference<String> pathRef = new AtomicReference<>();
        when(client.upload(same(content), argThat(path -> {
            assertTrue(path.matches("\\d{8}/\\d+/6318848e882d8a7e7e82789d87608f684ee52d41966bfc8cad3ce15aad2b970e\\.jpg"));
            pathRef.set(path);
            return true;
        }), eq(type))).thenReturn(url);
        when(client.getId()).thenReturn(10L);
        // 调用
        String result = fileService.createFile(content, null, null, null);
        // 断言
        assertEquals(result, url);
        // 校验数据
        FileDO file = fileMapper.selectOne(FileDO::getUrl, url);
        assertEquals(10L, file.getConfigId());
        assertEquals(pathRef.get(), file.getPath());
        assertEquals(url, file.getUrl());
        assertEquals(type, file.getType());
        assertEquals(content.length, file.getSize());
    }

    @Test
    public void testCreateFileAndReturnId_success() throws Exception {
        byte[] content = ResourceUtil.readBytes("file/erweima.jpg");
        String name = "showroom-audio.wav";
        String directory = "showroom/narration";
        String type = "audio/wav";
        FileClient client = mock(FileClient.class);
        when(fileConfigService.getMasterFileClient()).thenReturn(client);
        String url = randomString();
        AtomicReference<String> pathRef = new AtomicReference<>();
        when(client.upload(same(content), argThat(path -> {
            assertTrue(path.matches(directory + "/\\d{8}/\\d+/" + name));
            pathRef.set(path);
            return true;
        }), eq(type))).thenReturn(url);
        when(client.getId()).thenReturn(11L);

        Long fileId = fileService.createFileAndReturnId(content, name, directory, type);

        FileDO file = fileMapper.selectById(fileId);
        assertNotNull(file);
        assertEquals(11L, file.getConfigId());
        assertEquals(pathRef.get(), file.getPath());
        assertEquals(url, file.getUrl());
        assertEquals(type, file.getType());
        assertEquals(content.length, file.getSize());
    }

    @Test
    public void testCreateFileAndReturnId_streamZeroByte_success() throws Exception {
        String name = "非精准分类.txt";
        String directory = "dcc/original";
        String type = "text/plain";
        InputStream content = new ByteArrayInputStream(new byte[0]);
        FileClient client = mock(FileClient.class);
        when(fileConfigService.getMasterFileClient()).thenReturn(client);
        String url = randomString();
        AtomicReference<String> pathRef = new AtomicReference<>();
        when(client.upload(same(content), eq(0L), argThat(path -> {
            assertTrue(path.matches(directory + "/\\d{8}/\\d+/" + name));
            pathRef.set(path);
            return true;
        }), eq(type))).thenReturn(url);
        when(client.getId()).thenReturn(12L);

        Long fileId = fileService.createFileAndReturnId(content, 0L, name, directory, type);

        FileDO file = fileMapper.selectById(fileId);
        assertNotNull(file);
        assertEquals(12L, file.getConfigId());
        assertEquals(pathRef.get(), file.getPath());
        assertEquals(url, file.getUrl());
        assertEquals(type, file.getType());
        assertEquals(0L, file.getSize());
    }

    @Test
    public void testCreateFileAndReturnId_path_success() throws Exception {
        String name = "Manual.pdf";
        String directory = "dcc/original";
        String type = "application/pdf";
        byte[] content = "manual-content".getBytes(StandardCharsets.UTF_8);
        Path contentPath = Files.createTempFile("file-service-path-upload-", ".pdf");
        Files.write(contentPath, content);
        try {
            FileClient client = mock(FileClient.class);
            when(fileConfigService.getMasterFileClient()).thenReturn(client);
            String url = randomString();
            AtomicReference<String> pathRef = new AtomicReference<>();
            when(client.upload(same(contentPath), eq((long) content.length), argThat(path -> {
                assertTrue(path.matches(directory + "/\\d{8}/\\d+/" + name));
                pathRef.set(path);
                return true;
            }), eq(type))).thenReturn(url);
            when(client.getId()).thenReturn(13L);

            Long fileId = fileService.createFileAndReturnId(contentPath, content.length, name, directory, type);

            FileDO file = fileMapper.selectById(fileId);
            assertNotNull(file);
            assertEquals(13L, file.getConfigId());
            assertEquals(pathRef.get(), file.getPath());
            assertEquals(url, file.getUrl());
            assertEquals(type, file.getType());
            assertEquals(content.length, file.getSize());
        } finally {
            Files.deleteIfExists(contentPath);
        }
    }

    @Test
    public void testDeleteFile_success() throws Exception {
        // mock 数据
        FileDO dbFile = randomPojo(FileDO.class, o -> o.setConfigId(10L).setPath("tudou.jpg"));
        fileMapper.insert(dbFile);// @Sql: 先插入出一条存在的数据
        // mock Master 文件客户端
        FileClient client = mock(FileClient.class);
        when(fileConfigService.getFileClient(eq(10L))).thenReturn(client);
        // 准备参数
        Long id = dbFile.getId();

        // 调用
        fileService.deleteFile(id);
        // 校验数据不存在了
        assertNull(fileMapper.selectById(id));
        // 校验调用
        verify(client).delete(eq("tudou.jpg"));
    }

    @Test
    public void testDeleteFile_whenProtectedShowroomMedia_failFast() {
        FileDO dbFile = randomPojo(FileDO.class, o -> o.setConfigId(28L)
                .setPath("showroom/product/cover/product-001.png"));
        fileMapper.insert(dbFile);

        assertServiceException(() -> fileService.deleteFile(dbFile.getId()),
                FILE_PROTECTED_SHOWROOM_MEDIA, dbFile.getId());
        assertNotNull(fileMapper.selectById(dbFile.getId()));
        verify(fileConfigService, never()).getFileClient(any());
    }

    @Test
    public void testMoveFile_successMovesStorageObjectAndUpdatesRow() throws Exception {
        FileDO dbFile = randomPojo(FileDO.class, o -> o.setConfigId(10L)
                .setName("SOP-001.pdf")
                .setPath("dcc/stamped/SOP-001.pdf")
                .setUrl("https://old.example.com/SOP-001.pdf")
                .setType("application/pdf"));
        fileMapper.insert(dbFile);
        FileClient client = mock(FileClient.class);
        when(fileConfigService.getFileClient(eq(10L))).thenReturn(client);
        when(client.move(eq("dcc/stamped/SOP-001.pdf"),
                eq("dcc/stamped/作废文件/" + dbFile.getId() + "/SOP-001.pdf"),
                eq("application/pdf"))).thenReturn("https://new.example.com/SOP-001.pdf");

        FileDO result = fileService.moveFile(dbFile.getId(),
                "dcc/stamped/作废文件/" + dbFile.getId() + "/SOP-001.pdf");

        assertEquals("dcc/stamped/作废文件/" + dbFile.getId() + "/SOP-001.pdf", result.getPath());
        assertEquals("https://new.example.com/SOP-001.pdf", result.getUrl());
        FileDO updated = fileMapper.selectById(dbFile.getId());
        assertEquals(result.getPath(), updated.getPath());
        assertEquals(result.getUrl(), updated.getUrl());
        verify(client).move(eq("dcc/stamped/SOP-001.pdf"),
                eq("dcc/stamped/作废文件/" + dbFile.getId() + "/SOP-001.pdf"),
                eq("application/pdf"));
    }

    @Test
    public void testMoveFile_whenPreviousStorageMoveAlreadyCompleted_updatesRowAfterVerifyingTarget() throws Exception {
        FileDO dbFile = randomPojo(FileDO.class, o -> o.setConfigId(10L)
                .setName("SOP-001.pdf")
                .setPath("dcc/stamped/SOP-001.pdf")
                .setUrl("https://old.example.com/SOP-001.pdf")
                .setType("application/pdf"));
        fileMapper.insert(dbFile);
        String targetPath = "dcc/stamped/作废文件/" + dbFile.getId() + "/SOP-001.pdf";
        FileClient client = mock(FileClient.class);
        when(fileConfigService.getFileClient(eq(10L))).thenReturn(client);
        when(client.move(eq("dcc/stamped/SOP-001.pdf"), eq(targetPath), eq("application/pdf")))
                .thenThrow(new IllegalStateException("source object missing after previous copy"));
        when(client.getContent(eq(targetPath))).thenReturn("moved".getBytes());
        when(client.getContent(eq("dcc/stamped/SOP-001.pdf"))).thenThrow(NoSuchKeyException.builder().build());
        when(client.presignGetUrl(eq(targetPath), isNull())).thenReturn("https://new.example.com/SOP-001.pdf");

        FileDO result = fileService.moveFile(dbFile.getId(), targetPath);

        assertEquals(targetPath, result.getPath());
        assertEquals("https://new.example.com/SOP-001.pdf", result.getUrl());
        FileDO updated = fileMapper.selectById(dbFile.getId());
        assertEquals(targetPath, updated.getPath());
        assertEquals("https://new.example.com/SOP-001.pdf", updated.getUrl());
    }

    @Test
    public void testMoveFile_whenPreviousLocalStorageMoveReturnsNullForMissingSource_updatesRow() throws Exception {
        FileDO dbFile = randomPojo(FileDO.class, o -> o.setConfigId(10L)
                .setName("SOP-001.pdf")
                .setPath("dcc/stamped/SOP-001.pdf")
                .setUrl("https://old.example.com/SOP-001.pdf")
                .setType("application/pdf"));
        fileMapper.insert(dbFile);
        String targetPath = "dcc/stamped/作废文件/" + dbFile.getId() + "/SOP-001.pdf";
        FileClient client = mock(FileClient.class);
        when(fileConfigService.getFileClient(eq(10L))).thenReturn(client);
        when(client.move(eq("dcc/stamped/SOP-001.pdf"), eq(targetPath), eq("application/pdf")))
                .thenThrow(new IllegalStateException("local source missing after previous move"));
        when(client.getContent(eq(targetPath))).thenReturn("moved".getBytes());
        when(client.getContent(eq("dcc/stamped/SOP-001.pdf"))).thenReturn(null);
        when(client.presignGetUrl(eq(targetPath), isNull())).thenReturn("https://new.example.com/SOP-001.pdf");

        FileDO result = fileService.moveFile(dbFile.getId(), targetPath);

        assertEquals(targetPath, result.getPath());
        assertEquals("https://new.example.com/SOP-001.pdf", result.getUrl());
        FileDO updated = fileMapper.selectById(dbFile.getId());
        assertEquals(targetPath, updated.getPath());
        assertEquals("https://new.example.com/SOP-001.pdf", updated.getUrl());
    }

    @Test
    public void testMoveFile_whenPreviousStorageMoveLeftSourcePresent_failsFast() throws Exception {
        FileDO dbFile = randomPojo(FileDO.class, o -> o.setConfigId(10L)
                .setName("SOP-001.pdf")
                .setPath("dcc/stamped/SOP-001.pdf")
                .setUrl("https://old.example.com/SOP-001.pdf")
                .setType("application/pdf"));
        fileMapper.insert(dbFile);
        String targetPath = "dcc/stamped/作废文件/" + dbFile.getId() + "/SOP-001.pdf";
        FileClient client = mock(FileClient.class);
        when(fileConfigService.getFileClient(eq(10L))).thenReturn(client);
        when(client.move(eq("dcc/stamped/SOP-001.pdf"), eq(targetPath), eq("application/pdf")))
                .thenThrow(new IllegalStateException("delete source failed"));
        when(client.getContent(eq(targetPath))).thenReturn("moved".getBytes());
        when(client.getContent(eq("dcc/stamped/SOP-001.pdf"))).thenReturn("source".getBytes());

        assertThrows(IllegalStateException.class, () -> fileService.moveFile(dbFile.getId(), targetPath));

        FileDO updated = fileMapper.selectById(dbFile.getId());
        assertEquals("dcc/stamped/SOP-001.pdf", updated.getPath());
        assertEquals("https://old.example.com/SOP-001.pdf", updated.getUrl());
    }

    @Test
    public void testMoveFile_whenProtectedShowroomMedia_failFastBeforeStorageMove() {
        FileDO dbFile = randomPojo(FileDO.class, o -> o.setConfigId(28L)
                .setPath("showroom/product/cover/product-001.png"));
        fileMapper.insert(dbFile);

        assertServiceException(() -> fileService.moveFile(dbFile.getId(),
                        "showroom/product/cover/作废文件/" + dbFile.getId() + "/product-001.png"),
                FILE_PROTECTED_SHOWROOM_MEDIA, dbFile.getId());
        assertEquals("showroom/product/cover/product-001.png", fileMapper.selectById(dbFile.getId()).getPath());
        verify(fileConfigService, never()).getFileClient(any());
    }

    @Test
    public void testDeleteFileList_whenMultipleFiles_deletesStorageConcurrentlyAndDeletesRows() throws Exception {
        FileDO first = randomPojo(FileDO.class, o -> o.setConfigId(10L).setPath("bulk/a.pdf"));
        FileDO second = randomPojo(FileDO.class, o -> o.setConfigId(10L).setPath("bulk/b.pdf"));
        fileMapper.insert(first);
        fileMapper.insert(second);
        FileClient client = mock(FileClient.class);
        when(fileConfigService.getFileClient(eq(10L))).thenReturn(client);
        CountDownLatch bothDeletesStarted = new CountDownLatch(2);
        AtomicInteger inFlight = new AtomicInteger();
        AtomicInteger maxInFlight = new AtomicInteger();
        doAnswer(invocation -> {
            int current = inFlight.incrementAndGet();
            maxInFlight.accumulateAndGet(current, Math::max);
            bothDeletesStarted.countDown();
            bothDeletesStarted.await(500, TimeUnit.MILLISECONDS);
            inFlight.decrementAndGet();
            return null;
        }).when(client).delete(anyString());

        fileService.deleteFileList(List.of(first.getId(), second.getId()));

        assertTrue(maxInFlight.get() > 1, "deleteFileList should delete storage objects concurrently");
        assertNull(fileMapper.selectById(first.getId()));
        assertNull(fileMapper.selectById(second.getId()));
        verify(client).delete(eq("bulk/a.pdf"));
        verify(client).delete(eq("bulk/b.pdf"));
    }

    @Test
    public void testDeleteFileList_whenContainsProtectedShowroomMedia_failFastBeforeStorageDelete() {
        FileDO ordinary = randomPojo(FileDO.class, o -> o.setConfigId(10L).setPath("bulk/a.pdf"));
        FileDO protectedShowroom = randomPojo(FileDO.class, o -> o.setConfigId(28L)
                .setPath("showroom/narration/company-1-zh.wav"));
        fileMapper.insert(ordinary);
        fileMapper.insert(protectedShowroom);

        assertServiceException(() -> fileService.deleteFileList(List.of(ordinary.getId(), protectedShowroom.getId())),
                FILE_PROTECTED_SHOWROOM_MEDIA, protectedShowroom.getId());
        assertNotNull(fileMapper.selectById(ordinary.getId()));
        assertNotNull(fileMapper.selectById(protectedShowroom.getId()));
        verify(fileConfigService, never()).getFileClient(any());
    }

    @Test
    public void testDeleteFile_notExists() {
        // 准备参数
        Long id = randomLongId();

        // 调用, 并断言异常
        assertServiceException(() -> fileService.deleteFile(id), FILE_NOT_EXISTS);
    }

    @Test
    public void testGetFileContent() throws Exception {
        // 准备参数
        Long configId = 10L;
        String path = "tudou.jpg";
        // mock 方法
        FileClient client = mock(FileClient.class);
        when(fileConfigService.getFileClient(eq(10L))).thenReturn(client);
        byte[] content = new byte[]{};
        when(client.getContent(eq("tudou.jpg"))).thenReturn(content);

        // 调用
        byte[] result = fileService.getFileContent(configId, path);
        // 断言
        assertSame(result, content);
    }

    @Test
    public void testGetFileContent_whenS3NoSuchKey_refreshesClientAndRetriesOnce() throws Exception {
        Long configId = 10L;
        String path = "showroom/product/cover/20260530/product-product_001-cover.png";
        FileClient staleClient = mock(FileClient.class);
        FileClient refreshedClient = mock(FileClient.class);
        byte[] content = "image".getBytes();
        when(fileConfigService.getFileClient(eq(configId))).thenReturn(staleClient, refreshedClient);
        when(staleClient.getContent(eq(path))).thenThrow(NoSuchKeyException.builder().message("missing").build());
        when(refreshedClient.getContent(eq(path))).thenReturn(content);

        byte[] result = fileService.getFileContent(configId, path);

        assertSame(content, result);
        verify(fileConfigService).clearFileClientCache(configId);
        verify(staleClient).getContent(path);
        verify(refreshedClient).getContent(path);
    }

    @Test
    public void testGetFileContent_whenRetryStillNoSuchKey_usesFreshS3ClientClone() throws Exception {
        Long configId = 10L;
        String path = "showroom/product/cover/20260530/product-product_001-cover.png";
        FileClient staleClient = mock(FileClient.class);
        S3FileClient refreshedClient = mock(S3FileClient.class);
        byte[] content = "image".getBytes();
        when(fileConfigService.getFileClient(eq(configId))).thenReturn(staleClient, refreshedClient);
        when(staleClient.getContent(eq(path))).thenThrow(NoSuchKeyException.builder().message("missing").build());
        when(refreshedClient.getContent(eq(path))).thenThrow(NoSuchKeyException.builder().message("missing-again").build());
        when(refreshedClient.getContentWithFreshClient(eq(path))).thenReturn(content);

        byte[] result = fileService.getFileContent(configId, path);

        assertSame(content, result);
        verify(fileConfigService).clearFileClientCache(configId);
        verify(staleClient).getContent(path);
        verify(refreshedClient).getContent(path);
        verify(refreshedClient).getContentWithFreshClient(path);
    }

    @Test
    public void testValidateDirectLinkAllowed_whenDccControlledFile_failClosed() {
        FileDO dbFile = randomPojo(FileDO.class, o -> o.setConfigId(10L).setPath("quality/spec.pdf"));
        fileMapper.insert(dbFile);
        doThrow(new FileDirectLinkAccessGuard.ControlledFileDirectLinkBlockedException(dbFile.getId()))
                .when(fileDirectLinkAccessGuard).assertAllowed(eq(dbFile), any(FileDirectLinkAccessContext.class));

        assertServiceException(() -> fileService.validateDirectLinkAllowed(10L, "quality/spec.pdf",
                        directLinkContext()),
                FILE_DIRECT_LINK_BLOCKED_BY_DCC, dbFile.getId());
    }

    @Test
    public void testGetFileContent_whenDccControlledFile_internalReadNotBlockedByDirectLinkGuard() throws Exception {
        FileDO dbFile = randomPojo(FileDO.class, o -> o.setConfigId(10L).setPath("quality/internal.pdf"));
        fileMapper.insert(dbFile);
        doThrow(new FileDirectLinkAccessGuard.ControlledFileDirectLinkBlockedException(dbFile.getId()))
                .when(fileDirectLinkAccessGuard).assertAllowed(eq(dbFile), any(FileDirectLinkAccessContext.class));
        FileClient client = mock(FileClient.class);
        byte[] content = "controlled".getBytes();
        when(fileConfigService.getFileClient(eq(10L))).thenReturn(client);
        when(client.getContent(eq("quality/internal.pdf"))).thenReturn(content);

        byte[] result = fileService.getFileContent(10L, "quality/internal.pdf");

        assertSame(content, result);
        verify(fileDirectLinkAccessGuard, never()).assertAllowed(any(), any());
        verify(client).getContent("quality/internal.pdf");
    }

    @Test
    public void testValidateDirectLinkAllowed_whenOrdinaryFile_allows() {
        FileDO dbFile = randomPojo(FileDO.class, o -> o.setConfigId(10L).setPath("ordinary/spec.pdf"));
        fileMapper.insert(dbFile);

        fileService.validateDirectLinkAllowed(10L, "ordinary/spec.pdf", directLinkContext());

        verify(fileDirectLinkAccessGuard).assertAllowed(eq(dbFile), any(FileDirectLinkAccessContext.class));
    }

    private FileDirectLinkAccessContext directLinkContext() {
        return new FileDirectLinkAccessContext("10.0.0.7", "Playwright-E2E", "REQ-DIRECT-001");
    }

    @Test
    public void testGetFileContentWithStorageRetentionByFileId_delegatesToFileClient() throws Exception {
        // mock 数据
        FileDO dbFile = randomPojo(FileDO.class, o -> o.setConfigId(20L).setPath("archive/eDHR.pdf"));
        fileMapper.insert(dbFile);
        StorageRetentionPolicy policy = buildRetentionPolicy();
        byte[] content = new byte[]{1, 2, 3};
        // mock 方法
        FileClient client = mock(FileClient.class);
        when(fileConfigService.getFileClient(eq(20L))).thenReturn(client);
        when(client.getContentWithStorageRetention(eq("archive/eDHR.pdf"), same(policy))).thenReturn(content);

        // 调用
        byte[] result = fileService.getFileContentWithStorageRetention(dbFile.getId(), policy);

        // 断言
        assertSame(content, result);
        verify(client).getContentWithStorageRetention(eq("archive/eDHR.pdf"), same(policy));
    }

    @Test
    public void testCreateFileWithStorageRetentionByConfigId_usesSpecifiedClientAndPersistsConfigId() throws Exception {
        // 准备参数
        Long configId = -1_040_750_314L;
        byte[] content = "sealed-pdf-content".getBytes(StandardCharsets.UTF_8);
        String name = "archive.pdf";
        String directory = "mes/edhr/archive";
        String type = "application/pdf";
        StorageRetentionPolicy policy = buildRetentionPolicy();
        StorageRetentionEvidence evidence = new StorageRetentionEvidence()
                .setClientId(configId)
                .setBucket("edhr-lock-bucket")
                .setPath("mes/edhr/archive/archive.pdf")
                .setUrl("http://127.0.0.1:9000/edhr-lock-bucket/mes/edhr/archive/archive.pdf")
                .setObjectVersionId("version-1")
                .setRetentionMode("COMPLIANCE")
                .setLegalHoldStatus("ON");
        // mock 指定文件客户端
        FileClient client = mock(FileClient.class);
        when(fileConfigService.getFileClient(eq(configId))).thenReturn(client);
        when(client.getId()).thenReturn(configId);
        AtomicReference<String> pathRef = new AtomicReference<>();
        when(client.uploadWithStorageRetention(same(content), argThat(path -> {
            assertTrue(path.matches("mes/edhr/archive/\\d{8}/\\d+/archive\\.pdf"));
            pathRef.set(path);
            return true;
        }), eq(type), same(policy))).thenReturn(evidence);

        // 调用
        StorageRetentionEvidence result = fileService.createFileWithStorageRetention(configId, content, name,
                directory, type, policy);

        // 断言
        assertSame(evidence, result);
        assertNotNull(result.getFileId());
        FileDO file = fileMapper.selectById(result.getFileId());
        assertEquals(configId, file.getConfigId());
        assertEquals(pathRef.get(), file.getPath());
        assertEquals(evidence.getUrl(), file.getUrl());
        assertEquals(type, file.getType());
        assertEquals(content.length, file.getSize());
        verify(fileConfigService, never()).getMasterFileClient();
    }

    @Test
    public void testFileClientDefaultGetContentWithStorageRetention_unsupportedFailFast() {
        FileClient client = new FileClient() {

            @Override
            public Long getId() {
                return 20L;
            }

            @Override
            public String upload(byte[] content, String path, String type) {
                return path;
            }

            @Override
            public void delete(String path) {
            }

            @Override
            public byte[] getContent(String path) {
                return new byte[0];
            }
        };

        assertThrows(UnsupportedOperationException.class,
                () -> client.getContentWithStorageRetention("archive/eDHR.pdf", buildRetentionPolicy()));
    }

    @Test
    public void testFileClientDefaultMove_unsupportedFailFast() {
        FileClient client = new FileClient() {

            @Override
            public Long getId() {
                return 20L;
            }

            @Override
            public String upload(byte[] content, String path, String type) {
                return path;
            }

            @Override
            public void delete(String path) {
            }

            @Override
            public byte[] getContent(String path) {
                return new byte[0];
            }
        };

        assertThrows(UnsupportedOperationException.class,
                () -> client.move("archive/current.pdf", "archive/作废文件/current.pdf", "application/pdf"));
    }

    @Test
    public void testRequireStorageRetentionEvidenceByFileId_delegatesToFileClient() throws Exception {
        // mock 数据
        FileDO dbFile = randomPojo(FileDO.class, o -> o.setConfigId(20L).setPath("archive/eDHR.pdf"));
        fileMapper.insert(dbFile);
        StorageRetentionPolicy policy = buildRetentionPolicy();
        StorageRetentionEvidence evidence = new StorageRetentionEvidence()
                .setClientId(20L).setBucket("edhr-archive").setPath("archive/eDHR.pdf")
                .setObjectVersionId("version-1").setRetentionMode("COMPLIANCE")
                .setLegalHoldStatus("ON");
        // mock 方法
        FileClient client = mock(FileClient.class);
        when(fileConfigService.getFileClient(eq(20L))).thenReturn(client);
        when(client.requireStorageRetentionEvidence(eq("archive/eDHR.pdf"), same(policy))).thenReturn(evidence);

        // 调用
        StorageRetentionEvidence result = fileService.requireStorageRetentionEvidence(dbFile.getId(), policy);

        // 断言
        assertSame(evidence, result);
        assertEquals(dbFile.getId(), result.getFileId());
        verify(client).requireStorageRetentionEvidence(eq("archive/eDHR.pdf"), same(policy));
    }

    @Test
    public void testRequireStorageRetentionEvidenceByConfigPath_unsupportedExceptionPassThrough() throws Exception {
        // 准备参数
        Long configId = 11L;
        String path = "archive/no-retention.pdf";
        StorageRetentionPolicy policy = buildRetentionPolicy();
        UnsupportedOperationException unsupported = new UnsupportedOperationException("当前文件客户端不支持存储保留证据");
        // mock 方法
        FileClient client = mock(FileClient.class);
        when(fileConfigService.getFileClient(eq(configId))).thenReturn(client);
        when(client.requireStorageRetentionEvidence(eq(path), same(policy))).thenThrow(unsupported);

        // 调用，并断言异常透出
        UnsupportedOperationException result = assertThrows(UnsupportedOperationException.class,
                () -> fileService.requireStorageRetentionEvidence(configId, path, policy));
        assertSame(unsupported, result);
    }

    private StorageRetentionPolicy buildRetentionPolicy() {
        return new StorageRetentionPolicy()
                .setObjectLockRequired(true)
                .setRetentionMode("COMPLIANCE")
                .setRetentionDays(365)
                .setLegalHoldRequired(true)
                .setObjectVersionId("version-1")
                .setChecksumSha256("sha256");
    }

    @Test
    public void testGenerateUploadPath_AllEnabled() {
        // 准备参数
        String name = "test.jpg";
        String directory = "avatar";
        FileServiceImpl.PATH_PREFIX_DATE_ENABLE = true;
        FileServiceImpl.PATH_SUFFIX_TIMESTAMP_ENABLE = true;

        // 调用
        String path = fileService.generateUploadPath(name, directory);

        // 断言
        // 格式为：avatar/yyyyMMdd/{时间戳+随机数}/test.jpg
        assertTrue(path.startsWith(directory + "/"));
        // 包含日期格式：8 位数字，如 20240517
        assertTrue(path.matches(directory + "/\\d{8}/\\d+/test\\.jpg"));
    }

    @Test
    public void testGenerateUploadPath_PrefixEnabled_SuffixDisabled() {
        // 准备参数
        String name = "test.jpg";
        String directory = "avatar";
        FileServiceImpl.PATH_PREFIX_DATE_ENABLE = true;
        FileServiceImpl.PATH_SUFFIX_TIMESTAMP_ENABLE = false;

        // 调用
        String path = fileService.generateUploadPath(name, directory);

        // 断言
        // 格式为：avatar/yyyyMMdd/test.jpg
        assertTrue(path.startsWith(directory + "/"));
        // 包含日期格式：8 位数字，如 20240517
        assertTrue(path.matches(directory + "/\\d{8}/test\\.jpg"));
    }

    @Test
    public void testGenerateUploadPath_PrefixDisabled_SuffixEnabled() {
        // 准备参数
        String name = "test.jpg";
        String directory = "avatar";
        FileServiceImpl.PATH_PREFIX_DATE_ENABLE = false;
        FileServiceImpl.PATH_SUFFIX_TIMESTAMP_ENABLE = true;

        // 调用
        String path = fileService.generateUploadPath(name, directory);

        // 断言
        // 格式为：avatar/{时间戳+随机数}/test.jpg
        assertTrue(path.startsWith(directory + "/"));
        assertTrue(path.matches(directory + "/\\d+/test\\.jpg"));
    }

    @Test
    public void testGenerateUploadPath_AllDisabled() {
        // 准备参数
        String name = "test.jpg";
        String directory = "avatar";
        FileServiceImpl.PATH_PREFIX_DATE_ENABLE = false;
        FileServiceImpl.PATH_SUFFIX_TIMESTAMP_ENABLE = false;

        // 调用
        String path = fileService.generateUploadPath(name, directory);

        // 断言
        // 格式为：avatar/test.jpg
        assertEquals(directory + "/" + name, path);
    }

    @Test
    public void testGenerateUploadPath_NoExtension() {
        // 准备参数
        String name = "test";
        String directory = "avatar";
        FileServiceImpl.PATH_PREFIX_DATE_ENABLE = true;
        FileServiceImpl.PATH_SUFFIX_TIMESTAMP_ENABLE = true;

        // 调用
        String path = fileService.generateUploadPath(name, directory);

        // 断言
        // 格式为：avatar/yyyyMMdd/{时间戳+随机数}/test
        assertTrue(path.startsWith(directory + "/"));
        assertTrue(path.matches(directory + "/\\d{8}/\\d+/test"));
    }

    @Test
    public void testGenerateUploadPath_DirectoryNull() {
        // 准备参数
        String name = "test.jpg";
        String directory = null;
        FileServiceImpl.PATH_PREFIX_DATE_ENABLE = true;
        FileServiceImpl.PATH_SUFFIX_TIMESTAMP_ENABLE = true;

        // 调用
        String path = fileService.generateUploadPath(name, directory);

        // 断言
        // 格式为：yyyyMMdd/{时间戳+随机数}/test.jpg
        assertTrue(path.matches("\\d{8}/\\d+/test\\.jpg"));
    }

    @Test
    public void testGenerateUploadPath_SuffixAsName_AllEnabled() {
        // 准备参数
        String name = "test.jpg";
        String directory = "avatar";
        FileServiceImpl.PATH_PREFIX_DATE_ENABLE = true;
        FileServiceImpl.PATH_SUFFIX_TIMESTAMP_ENABLE = true;
        FileServiceImpl.PATH_SUFFIX_AS_DIRECTORY = false;

        // 调用
        String path = fileService.generateUploadPath(name, directory);

        // 断言
        // 格式为：avatar/yyyyMMdd/test_{时间戳+随机数}.jpg
        assertTrue(path.matches(directory + "/\\d{8}/test_\\d+\\.jpg"));
    }

    @Test
    public void testGenerateUploadPath_SuffixAsName_PrefixDisabled() {
        // 准备参数
        String name = "test.jpg";
        String directory = "avatar";
        FileServiceImpl.PATH_PREFIX_DATE_ENABLE = false;
        FileServiceImpl.PATH_SUFFIX_TIMESTAMP_ENABLE = true;
        FileServiceImpl.PATH_SUFFIX_AS_DIRECTORY = false;

        // 调用
        String path = fileService.generateUploadPath(name, directory);

        // 断言
        // 格式为：avatar/test_{时间戳+随机数}.jpg
        assertTrue(path.matches(directory + "/test_\\d+\\.jpg"));
    }

    @Test
    public void testGenerateUploadPath_SuffixAsName_NoExtension() {
        // 准备参数
        String name = "test";
        String directory = "avatar";
        FileServiceImpl.PATH_PREFIX_DATE_ENABLE = true;
        FileServiceImpl.PATH_SUFFIX_TIMESTAMP_ENABLE = true;
        FileServiceImpl.PATH_SUFFIX_AS_DIRECTORY = false;

        // 调用
        String path = fileService.generateUploadPath(name, directory);

        // 断言
        // 格式为：avatar/yyyyMMdd/test_{时间戳+随机数}
        assertTrue(path.matches(directory + "/\\d{8}/test_\\d+"));
    }

    @Test
    public void testGenerateUploadPath_DirectoryEmpty() {
        // 准备参数
        String name = "test.jpg";
        String directory = "";
        FileServiceImpl.PATH_PREFIX_DATE_ENABLE = true;
        FileServiceImpl.PATH_SUFFIX_TIMESTAMP_ENABLE = true;

        // 调用
        String path = fileService.generateUploadPath(name, directory);

        // 断言
        // 格式为：yyyyMMdd/{时间戳+随机数}/test.jpg
        assertTrue(path.matches("\\d{8}/\\d+/test\\.jpg"));
    }

}
