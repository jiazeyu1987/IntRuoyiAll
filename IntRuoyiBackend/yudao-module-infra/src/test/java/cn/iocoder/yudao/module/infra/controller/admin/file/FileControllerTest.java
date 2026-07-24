package cn.iocoder.yudao.module.infra.controller.admin.file;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FileUploadReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FileNasConfigRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FileNasConfigSaveReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FileNasConfigTestRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FileNasListRespVO;
import cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FileNasDirectoryTreeReqVO;
import cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FileNasDirectoryTreeRespVO;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.infra.service.file.access.FileDirectLinkAccessContext;
import cn.iocoder.yudao.module.infra.service.file.NasConnectionConfig;
import cn.iocoder.yudao.module.infra.service.file.NasBrowserService;
import cn.iocoder.yudao.module.infra.service.file.NasDirectoryService;
import cn.iocoder.yudao.module.infra.service.file.NasSettingsService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.infra.enums.ErrorCodeConstants.FILE_DIRECT_LINK_BLOCKED_BY_DCC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private FileController fileController;

    @Mock
    private FileService fileService;
    @Mock
    private NasBrowserService nasBrowserService;
    @Mock
    private NasDirectoryService nasDirectoryService;
    @Mock
    private NasSettingsService nasSettingsService;

    @Test
    void testGetNasDirectoryTree() {
        FileNasDirectoryTreeReqVO reqVO = new FileNasDirectoryTreeReqVO();
        reqVO.setPath("\\\\nas\\share\\quality");
        FileNasDirectoryTreeRespVO respVO = new FileNasDirectoryTreeRespVO()
                .setRootPath("\\\\nas\\share\\quality")
                .setRootName("quality")
                .setDirectoryCount(2)
                .setChildren(List.of(
                        new FileNasDirectoryTreeRespVO.Node()
                                .setName("SOP")
                                .setPath("\\\\nas\\share\\quality\\SOP")
                                .setChildren(List.of())
                ));
        when(nasDirectoryService.getNasDirectoryTree(eq(reqVO.getPath()))).thenReturn(respVO);

        CommonResult<FileNasDirectoryTreeRespVO> result = fileController.getNasDirectoryTree(reqVO);

        assertEquals(0, result.getCode());
        assertEquals(respVO, result.getData());
    }

    @Test
    void testListNasFiles() {
        FileNasListRespVO respVO = new FileNasListRespVO()
                .setCurrentPath("QMS")
                .setParentPath("")
                .setRootPath("\\\\172.30.30.4\\it共享")
                .setItems(List.of(
                        new FileNasListRespVO.Item()
                                .setName("1.QMS documents")
                                .setPath("QMS/1.QMS documents")
                                .setDir(true)
                                .setSize(0L)
                                .setModifiedAt(1L)
                ));
        when(nasBrowserService.listFiles(eq("QMS"))).thenReturn(respVO);

        CommonResult<FileNasListRespVO> result = fileController.listNasFiles("QMS");

        assertEquals(0, result.getCode());
        assertEquals(respVO, result.getData());
    }

    @Test
    void testGetNasTree() {
        FileNasDirectoryTreeRespVO respVO = new FileNasDirectoryTreeRespVO()
                .setRootName("it共享")
                .setRootPath("\\\\172.30.30.4\\it共享")
                .setDirectoryCount(3)
                .setChildren(List.of(
                        new FileNasDirectoryTreeRespVO.Node()
                                .setName("1.QMS documents")
                                .setPath("1.QMS documents")
                                .setChildren(List.of(
                                        new FileNasDirectoryTreeRespVO.Node()
                                                .setName("child")
                                                .setPath("1.QMS documents/child")
                                                .setChildren(List.of())
                                ))
                ))
                .setSkipped(List.of(
                        new FileNasDirectoryTreeRespVO.SkippedNode()
                                .setPath("#recycle")
                                .setReason("access_denied")
                ));
        when(nasBrowserService.getDirectoryTree()).thenReturn(respVO);

        CommonResult<FileNasDirectoryTreeRespVO> result = fileController.getNasTree();

        assertEquals(0, result.getCode());
        assertEquals(respVO, result.getData());
    }

    @Test
    void testGetNasConfig() {
        FileNasConfigRespVO respVO = new FileNasConfigRespVO()
                .setServer("172.30.30.4")
                .setShare("it共享")
                .setUsername("int")
                .setPassword("Kdlyx123");
        when(nasSettingsService.getNasConfig()).thenReturn(respVO);

        CommonResult<FileNasConfigRespVO> result = fileController.getNasConfig();

        assertEquals(0, result.getCode());
        assertEquals(respVO, result.getData());
    }

    @Test
    void testSaveNasConfig() {
        FileNasConfigSaveReqVO reqVO = new FileNasConfigSaveReqVO();
        reqVO.setServer("172.30.30.4");
        reqVO.setShare("it共享");
        reqVO.setUsername("int");
        reqVO.setPassword("Kdlyx123");

        CommonResult<Boolean> result = fileController.saveNasConfig(reqVO);

        verify(nasSettingsService).saveNasConfig(eq(reqVO));
        assertEquals(Boolean.TRUE, result.getData());
    }

    @Test
    void testTestNasConfig() {
        FileNasConfigSaveReqVO reqVO = new FileNasConfigSaveReqVO();
        reqVO.setServer("172.30.30.4");
        reqVO.setShare("it共享");
        reqVO.setUsername("int");
        reqVO.setPassword("Kdlyx123");
        NasConnectionConfig config = new NasConnectionConfig("172.30.30.4", null, "it共享", null, "int", "Kdlyx123");
        FileNasConfigTestRespVO respVO = new FileNasConfigTestRespVO()
                .setRootPath("\\\\172.30.30.4\\it共享")
                .setItemCount(3)
                .setMessage("NAS 连接成功");
        when(nasSettingsService.toConnectionConfig(eq(reqVO))).thenReturn(config);
        when(nasBrowserService.testConnection(eq(config))).thenReturn(respVO);

        CommonResult<FileNasConfigTestRespVO> result = fileController.testNasConfig(reqVO);

        assertEquals(0, result.getCode());
        assertEquals(respVO, result.getData());
    }

    @Test
    void uploadFileShouldReturnAdminProxyUrlInsteadOfStorageDirectUrl() throws Exception {
        FileUploadReqVO reqVO = new FileUploadReqVO();
        reqVO.setFile(new MockMultipartFile("file", "company cover.png", "image/png", "png".getBytes()));
        reqVO.setDirectory("showroom/company");
        when(fileService.createFileAndReturnId(eq("png".getBytes()), eq("company cover.png"),
                eq("showroom/company"), eq("image/png"))).thenReturn(88L);
        when(fileService.getFile(eq(88L))).thenReturn(FileDO.builder()
                .id(88L)
                .configId(28L)
                .path("showroom/company/20260521/company cover.png")
                .url("http://127.0.0.1:9000/yudao/showroom/company/20260521/company cover.png")
                .type("image/png")
                .build());

        CommonResult<String> result = fileController.uploadFile(reqVO);

        assertEquals(0, result.getCode());
        assertEquals("/admin-api/infra/file/28/get/showroom/company/20260521/company%20cover.png",
                result.getData());
        assertTrue(result.getData().startsWith("/admin-api/infra/file/28/get/"));
    }

    @Test
    void getFileContent_whenDccControlledFile_failClosedBeforeServiceRead() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET",
                "/admin-api/infra/file/10/get/quality/spec.pdf");
        request.addHeader("User-Agent", "Playwright-E2E");
        request.addHeader("X-DCC-Request-Id", "REQ-DIRECT-001");
        MockHttpServletResponse response = new MockHttpServletResponse();
        doThrow(exception(FILE_DIRECT_LINK_BLOCKED_BY_DCC, 700L))
                .when(fileService).validateDirectLinkAllowed(eq(10L), eq("quality/spec.pdf"),
                        argThat(context -> "Playwright-E2E".equals(context.userAgent())
                                && "REQ-DIRECT-001".equals(context.requestId())));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> fileController.getFileContent(request, response, 10L));

        assertEquals(FILE_DIRECT_LINK_BLOCKED_BY_DCC.getCode(), ex.getCode());
        verify(fileService).validateDirectLinkAllowed(eq(10L), eq("quality/spec.pdf"),
                any(FileDirectLinkAccessContext.class));
        verify(fileService, never()).getFileContent(any(), any());
    }

    @Test
    void getFileContent_whenOrdinaryFile_writesContentAsBefore() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET",
                "/admin-api/infra/file/10/get/ordinary/spec.txt");
        request.addHeader("User-Agent", "Playwright-E2E");
        request.addHeader("X-DCC-Request-Id", "REQ-DIRECT-002");
        MockHttpServletResponse response = new MockHttpServletResponse();
        byte[] content = "ordinary".getBytes(StandardCharsets.UTF_8);
        when(fileService.getFileContent(10L, "ordinary/spec.txt")).thenReturn(content);

        fileController.getFileContent(request, response, 10L);

        verify(fileService).validateDirectLinkAllowed(eq(10L), eq("ordinary/spec.txt"),
                any(FileDirectLinkAccessContext.class));
        verify(fileService).getFileContent(10L, "ordinary/spec.txt");
        assertArrayEquals(content, response.getContentAsByteArray());
    }

    @Test
    void getFileContent_whenChinesePathPercentEncoded_passesUtf8DecodedPathToService() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET",
                "/admin-api/infra/file/28/get/20260521/%E5%BC%80%E5%9B%AD%E6%B4%BB%E5%8A%A8%E5%9B%BE-%E5%8E%8B%E7%BC%A9%E7%89%88.jpg");
        request.addHeader("User-Agent", "Playwright-E2E");
        request.addHeader("X-DCC-Request-Id", "REQ-DIRECT-CHINESE-001");
        MockHttpServletResponse response = new MockHttpServletResponse();
        byte[] content = "jpeg".getBytes(StandardCharsets.UTF_8);
        when(fileService.getFileContent(28L, "20260521/开园活动图-压缩版.jpg")).thenReturn(content);

        fileController.getFileContent(request, response, 28L);

        verify(fileService).validateDirectLinkAllowed(eq(28L), eq("20260521/开园活动图-压缩版.jpg"),
                any(FileDirectLinkAccessContext.class));
        verify(fileService).getFileContent(28L, "20260521/开园活动图-压缩版.jpg");
        assertArrayEquals(content, response.getContentAsByteArray());
    }

    @Test
    void getFileContent_whenChinesePathAlreadyMojibake_repairsUtf8PathBeforeServiceRead() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET",
                "/admin-api/infra/file/28/get/20260521/å¼€å›­æ´»åŠ¨å›¾-åŽ‹ç¼©ç‰ˆ.jpg");
        request.addHeader("User-Agent", "Playwright-E2E");
        request.addHeader("X-DCC-Request-Id", "REQ-DIRECT-CHINESE-002");
        MockHttpServletResponse response = new MockHttpServletResponse();
        byte[] content = "jpeg".getBytes(StandardCharsets.UTF_8);
        when(fileService.getFileContent(28L, "20260521/开园活动图-压缩版.jpg")).thenReturn(content);

        fileController.getFileContent(request, response, 28L);

        verify(fileService).validateDirectLinkAllowed(eq(28L), eq("20260521/开园活动图-压缩版.jpg"),
                any(FileDirectLinkAccessContext.class));
        verify(fileService).getFileContent(28L, "20260521/开园活动图-压缩版.jpg");
        assertArrayEquals(content, response.getContentAsByteArray());
    }
}
