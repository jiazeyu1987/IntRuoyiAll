package cn.iocoder.yudao.module.infra.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.infra.controller.admin.file.vo.file.FileCreateReqVO;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.framework.file.core.client.FileClient;
import cn.iocoder.yudao.module.infra.framework.file.core.client.StorageRetentionEvidence;
import cn.iocoder.yudao.module.infra.framework.file.core.client.StorageRetentionPolicy;
import cn.iocoder.yudao.module.infra.framework.file.core.client.db.DBFileClient;
import cn.iocoder.yudao.module.infra.framework.file.core.client.db.DBFileClientConfig;
import cn.iocoder.yudao.module.infra.service.file.access.BusinessFileAccessService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Import(FileServiceImpl.class)
class FileTenantWriteTest extends BaseDbUnitTest {
    @Resource private FileServiceImpl service;
    @Resource private FileMapper files;
    @Resource private javax.sql.DataSource dataSource;
    private JdbcTemplate jdbc;
    @MockitoBean private FileConfigService configs;
    @MockitoBean private BusinessFileAccessService access;

    @BeforeEach
    void prepareJdbc() {
        jdbc = new JdbcTemplate(dataSource);
    }

    @ParameterizedTest
    @ValueSource(longs = {186L, 122L})
    void allUploadFormsPersistTheActualTenant(Long tenant) throws Exception {
        TenantContextHolder.setTenantId(tenant);
        byte[] content = "SIMULATION registration certificate".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        FileClient client = mock(FileClient.class);
        when(configs.getMasterFileClient()).thenReturn(client);
        when(client.getId()).thenReturn(10L);
        when(client.upload(any(byte[].class), anyString(), anyString())).thenReturn("https://test/bytes");
        when(client.upload(any(java.io.InputStream.class), anyLong(), anyString(), anyString())).thenReturn("https://test/stream");
        when(client.upload(any(Path.class), anyLong(), anyString(), anyString())).thenReturn("https://test/path");

        assertTenant(service.createFileAndReturnId(content, "certificate.txt", "dcc/registration-certificate/upload/1", "text/plain"), tenant);
        assertTenant(service.createFileAndReturnId(new ByteArrayInputStream(content), content.length, "certificate.txt", "dcc", "text/plain"), tenant);
        Path path = Files.createTempFile("registration-tenant-write-", ".txt");
        try {
            Files.write(path, content);
            assertTenant(service.createFileAndReturnId(path, content.length, "certificate.txt", "dcc", "text/plain"), tenant);
        } finally {
            Files.delete(path);
        }

        when(client.uploadWithStorageRetention(any(), anyString(), anyString(), any()))
                .thenReturn(new StorageRetentionEvidence().setUrl("https://test/retained"));
        assertTenant(service.createFileWithStorageRetention(content, "certificate.txt", "dcc", "text/plain", new StorageRetentionPolicy()).getFileId(), tenant);

        FileCreateReqVO registration = new FileCreateReqVO().setConfigId(10L).setName("certificate.txt")
                .setPath("dcc/registered.txt").setUrl("https://test/registered").setType("text/plain").setSize((long) content.length);
        assertTenant(service.createFile(registration), tenant);
    }

    @Test
    void databaseStoragePersistsContentAndMetadataWithTheSameTenant() {
        TenantContextHolder.setTenantId(186L);
        DBFileClientConfig config = new DBFileClientConfig().setDomain("http://localhost");
        DBFileClient client = new DBFileClient(10L, config);
        client.init();
        when(configs.getMasterFileClient()).thenReturn(client);
        byte[] content = "SIMULATION CE ANNEX".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Long id = service.createFileAndReturnId(content, "annex.txt", "dcc/registration-certificate", "text/plain");
        assertTenant(id, 186L);
        FileDO file = files.selectById(id);
        assertEquals(186L, jdbc.queryForObject("SELECT tenant_id FROM infra_file_content WHERE path=?", Long.class, file.getPath()));
        assertArrayEquals(content, client.getContent(file.getPath()));
    }

    @Test
    void missingTenantIsRejectedBeforeUploadingOrRegisteringAFile() {
        TenantContextHolder.clear();
        assertThrows(RuntimeException.class, () -> service.createFileAndReturnId(new byte[]{1}, "certificate.txt", "dcc", "text/plain"));
        assertThrows(RuntimeException.class, () -> service.createFile(new FileCreateReqVO()));
        verifyNoInteractions(configs);
        assertEquals(0L, jdbc.queryForObject("SELECT COUNT(*) FROM infra_file", Long.class));
    }

    @Test
    void insertsOutsideFileServiceAlsoUseTheActualTenant() {
        TenantContextHolder.setTenantId(186L);
        FileDO file = new FileDO().setConfigId(10L).setName("certificate.txt").setSize(1L);
        files.insert(file);
        assertTenant(file.getId(), 186L);
    }

    private void assertTenant(Long id, Long expected) {
        assertEquals(expected, jdbc.queryForObject("SELECT tenant_id FROM infra_file WHERE id=?", Long.class, id));
    }
}
