package cn.iocoder.yudao.module.infra.service.file;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.*;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileContentDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileContentMapper;
import cn.iocoder.yudao.module.infra.framework.file.core.client.local.*;
import cn.iocoder.yudao.module.infra.service.file.access.*;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.nio.file.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Import({FileServiceImpl.class, TenantFileConfigurationIsolationTest.SqlConfig.class})
class TenantFileOwnershipIsolationTest extends BaseDbUnitTest {
    @Resource FileServiceImpl service;
    @Resource FileContentMapper contents;
    @Resource cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper files;
    @MockitoBean FileConfigService configs;
    @MockitoBean BusinessFileAccessService business;
    @TempDir Path storage;

    @Test void sharedPhysicalStorageStillSeparatesTenantFilesAndRejectsForeignBatchBeforeDeleting() throws Exception {
        var client = new LocalFileClient(10L, new LocalFileClientConfig()
                .setBasePath(storage.toString()).setDomain("https://files.invalid"));
        client.init();
        when(configs.getMasterFileClient()).thenReturn(client);
        when(configs.getFileClient(10L)).thenReturn(client);
        TenantContextHolder.setTenantId(11L);
        Long aId = service.createFileAndReturnId("A".getBytes(), "same.txt", "docs", "text/plain");
        var a = service.getFile(aId);
        assertTrue(a.getPath().startsWith("tenants/11/docs/"));
        TenantContextHolder.setTenantId(22L);
        Long bId = service.createFileAndReturnId("B".getBytes(), "same.txt", "docs", "text/plain");
        var b = service.getFile(bId);
        assertTrue(b.getPath().startsWith("tenants/22/docs/"));
        TenantContextHolder.setIgnore(true);
        assertEquals(1L, service.getFilePage(new FilePageReqVO()).getTotal());
        assertThrows(RuntimeException.class, () -> service.getFile(aId));
        assertThrows(RuntimeException.class, () -> service.getFileContent(10L, a.getPath()));
        assertThrows(RuntimeException.class, () -> service.presignGetUrl(a.getUrl(), 60));
        assertThrows(RuntimeException.class, () -> service.deleteFileList(List.of(bId, aId)));
        assertEquals("A", Files.readString(storage.resolve(a.getPath())));
        assertEquals("B", Files.readString(storage.resolve(b.getPath())));
        assertThrows(RuntimeException.class, () -> service.moveFile(bId,"tenants/11/overwritten.txt"));
        var moved = service.moveFile(bId,"docs/moved.txt");
        assertEquals("tenants/22/docs/moved.txt",moved.getPath());
        assertArrayEquals("B".getBytes(), service.getFileContent(10L,moved.getPath()));
        service.deleteFileList(List.of(bId));
        assertFalse(Files.exists(storage.resolve(moved.getPath())));
        assertEquals("A", Files.readString(storage.resolve(a.getPath())));
    }

    @Test void databaseContentsWithIdenticalConfigAndPathRemainSeparate() {
        TenantContextHolder.setTenantId(11L);
        contents.insert(new FileContentDO().setConfigId(10L).setPath("same.txt").setContent("A".getBytes()));
        TenantContextHolder.setTenantId(22L);
        contents.insert(new FileContentDO().setConfigId(10L).setPath("same.txt").setContent("B".getBytes()));
        TenantContextHolder.setIgnore(true);
        assertArrayEquals("B".getBytes(),contents.selectListByConfigIdAndPath(10L,"same.txt").get(0).getContent());
        contents.deleteByConfigIdAndPath(10L,"same.txt");
        TenantContextHolder.setTenantId(11L);
        assertArrayEquals("A".getBytes(),contents.selectListByConfigIdAndPath(10L,"same.txt").get(0).getContent());
        TenantContextHolder.clear();
        assertThrows(RuntimeException.class, () -> contents.selectListByConfigIdAndPath(10L,"same.txt"));
    }

    @Test void foreignRegistrationAndPathTraversalFailBeforeStorageIsContacted() {
        TenantContextHolder.setTenantId(22L);
        var req = new FileCreateReqVO();
        req.setConfigId(10L);
        for (String path : List.of("tenants/11/a.txt","tenants/22/../11/a.txt","tenants/22/%2e%2e/a",
                "tenants/22/a/../../11/a.txt","tenants/22/a\\..\\b","/tenants/22/a")) {
            req.setPath(path);
            assertThrows(IllegalArgumentException.class, () -> service.createFile(req));
        }
        for (String directory : List.of("../11", "docs/../../11", "tenants/11", "C:/data")) {
            assertThrows(IllegalArgumentException.class, () -> service.generateUploadPath("a.txt",directory));
        }
        assertThrows(IllegalArgumentException.class, () -> service.generateUploadPath("../a.txt","docs"));
        verifyNoInteractions(configs);
    }

    @Test void namespaceDoesNotBypassExistingShowroomAndDossierRetentionRules() {
        TenantContextHolder.setTenantId(22L);
        var file = new cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO()
                .setConfigId(28L).setPath("tenants/22/showroom/protected.jpg").setSize(1L);
        files.insert(file);
        assertThrows(RuntimeException.class,()->service.deleteFile(file.getId()));
        assertThrows(RuntimeException.class,()->service.generateUploadPath("a.pdf","mes/active-order-dossier"));
        assertThrows(RuntimeException.class,()->service.generateUploadPath("a.pdf","tenants/22/mes/active-order-dossier"));
        verifyNoInteractions(configs);
    }
}
