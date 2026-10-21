package cn.iocoder.yudao.module.mes.service.pro.productionrelease.pqc;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesDossierUploadIntentDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesDossierUploadIntentMapper;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.framework.file.core.client.FileClient;
import cn.iocoder.yudao.module.infra.service.file.FileConfigService;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.LocalDateTime;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real H2 commit/rollback boundaries; only storage and mapper adapters are boundary doubles. */
class MesDossierUploadStagingServiceTest {
    JdbcTemplate db;
    DataSourceTransactionManager transactions;
    MesDossierUploadIntentMapper intents;
    FileMapper files;
    FileClient client;
    MesDossierUploadStagingService service;
    Map<String,byte[]> objects;
    @BeforeEach void setup() throws Exception {
        TenantContextHolder.setTenantId(71L);
        var ds=new DriverManagerDataSource("jdbc:h2:mem:"+UUID.randomUUID()+";DB_CLOSE_DELAY=-1","sa","");
        transactions=new DataSourceTransactionManager(ds); db=new JdbcTemplate(ds);
        db.execute("CREATE TABLE intent(id BIGINT PRIMARY KEY, tenant BIGINT, actor BIGINT, order_id BIGINT, config BIGINT, path VARCHAR(512), file_id BIGINT, state VARCHAR(24), expires TIMESTAMP, error VARCHAR(256))");
        db.execute("CREATE TABLE stored_file(id BIGINT PRIMARY KEY)");
        intents=mock(MesDossierUploadIntentMapper.class);files=mock(FileMapper.class);
        var configurations=mock(FileConfigService.class);client=mock(FileClient.class);
        when(configurations.getMasterFileClient()).thenReturn(client);when(configurations.getFileClient(1L)).thenReturn(client);
        when(client.getId()).thenReturn(1L);objects=new HashMap<>();
        when(client.upload(any(byte[].class),anyString(),anyString())).thenAnswer(c->{
            assertEquals("UPLOADING",db.queryForObject("SELECT state FROM intent WHERE path=?",String.class,c.getArgument(1,String.class)));
            assertTrue(c.getArgument(1,String.class).endsWith(".txt"), "Keep the extension used by local download MIME detection");
            objects.put(c.getArgument(1),c.getArgument(0));return "http://storage/"+c.getArgument(1);
        });
        doAnswer(c->{objects.remove(c.getArgument(0));return null;}).when(client).delete(anyString());
        when(intents.insert(any(MesDossierUploadIntentDO.class))).thenAnswer(c->{
            MesDossierUploadIntentDO row=c.getArgument(0);row.setId(1L);
            return db.update("INSERT INTO intent VALUES(?,?,?,?,?,?,?,?,?,?)",row.getId(),row.getTenantId(),row.getActorId(),row.getActiveOrderId(),row.getConfigId(),row.getStoragePath(),null,row.getState(),row.getExpiresAt(),null);
        });
        when(intents.lock(anyLong())).thenAnswer(c->db.query("SELECT * FROM intent WHERE id=? FOR UPDATE",(rs,n)->{
            var row=MesDossierUploadIntentDO.builder().id(rs.getLong("id")).actorId(rs.getLong("actor")).activeOrderId(rs.getLong("order_id"))
                    .configId(rs.getLong("config")).storagePath(rs.getString("path")).fileId((Long)rs.getObject("file_id"))
                    .state(rs.getString("state")).expiresAt(rs.getTimestamp("expires").toLocalDateTime()).lastError(rs.getString("error")).build();
            row.setTenantId(rs.getLong("tenant"));return row;
        },c.getArgument(0,Long.class)).stream().findFirst().orElse(null));
        when(intents.updateById(any(MesDossierUploadIntentDO.class))).thenAnswer(c->{var row=c.getArgument(0,MesDossierUploadIntentDO.class);
            return db.update("UPDATE intent SET state=?,file_id=?,expires=?,error=? WHERE id=?",row.getState(),row.getFileId(),row.getExpiresAt(),row.getLastError(),row.getId());});
        when(intents.expired()).thenAnswer(c->db.query("SELECT id FROM intent WHERE state IN ('UPLOADING','READY','CLEANING') AND expires<CURRENT_TIMESTAMP",(rs,n)->MesDossierUploadIntentDO.builder().id(rs.getLong(1)).build()).stream()
                .peek(row->row.setTenantId(71L)).toList());
        when(files.insert(any(FileDO.class))).thenAnswer(c->{c.getArgument(0,FileDO.class).setId(9L);return db.update("INSERT INTO stored_file VALUES(9)");});
        when(files.deleteById(anyLong())).thenAnswer(c->db.update("DELETE FROM stored_file WHERE id=?",c.getArgument(0,Long.class)));
        service=new MesDossierUploadStagingService(intents,configurations,files,transactions);
    }
    @AfterEach void clear(){TenantContextHolder.clear();}
    MesDossierUploadStagingService.Staged upload(){return service.upload(2L,3L,"OTHER_FILE","evidence.txt","text/plain",new byte[]{1,2});}
    String state(){return db.queryForObject("SELECT state FROM intent WHERE id=1",String.class);}
    void expire(){db.update("UPDATE intent SET expires=?",LocalDateTime.now().minusDays(1));}
    @Test void failedStorageWriteLeavesDurableIntentEvenAfterCallerFailure() throws Exception {
        when(client.upload(any(byte[].class),anyString(),anyString())).thenThrow(new IllegalStateException("storage down"));
        assertThrows(IllegalStateException.class,this::upload);
        assertEquals("UPLOADING",state());assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM stored_file",Integer.class));
        expire();service.cleanExpired();assertEquals("CLEANED",state());
    }
    @Test void auditRollbackRestoresReadyIntentAndRetainsFileUntilCleanup() {
        var staged=upload();
        assertThrows(IllegalStateException.class,()->new TransactionTemplate(transactions).executeWithoutResult(tx->{
            service.bind(staged,2L,3L);throw new IllegalStateException("audit rejected");
        }));
        assertEquals("READY",state());assertEquals(1,objects.size());
        expire();service.cleanExpired();assertEquals("CLEANED",state());assertTrue(objects.isEmpty());
        assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM stored_file",Integer.class));
    }
    @Test void committedEvidenceIsNeverReclaimedByExpiredIntentWorker() {
        var staged=upload();new TransactionTemplate(transactions).executeWithoutResult(tx->service.bind(staged,2L,3L));
        expire();service.cleanExpired();assertEquals("BOUND",state());assertEquals(1,objects.size());
    }
    @Test void expiredOrWrongTenantUploadCannotBeBound() {
        var staged=upload();TenantContextHolder.setTenantId(72L);
        assertThrows(IllegalStateException.class,()->new TransactionTemplate(transactions).executeWithoutResult(tx->service.bind(staged,2L,3L)));
        TenantContextHolder.setTenantId(71L);expire();
        assertThrows(IllegalStateException.class,()->new TransactionTemplate(transactions).executeWithoutResult(tx->service.bind(staged,2L,3L)));
    }
    @Test void storageDeletionFailureRemainsVisibleAndCanBeRetried() throws Exception {
        upload();expire();doThrow(new IllegalStateException("unavailable")).when(client).delete(anyString());
        service.cleanExpired();assertEquals("CLEANING",state());assertNotNull(db.queryForObject("SELECT error FROM intent",String.class));
        assertEquals(1,objects.size());expire();doAnswer(c->{objects.clear();return null;}).when(client).delete(anyString());
        service.cleanExpired();assertEquals("CLEANED",state());assertTrue(objects.isEmpty());
    }
}
