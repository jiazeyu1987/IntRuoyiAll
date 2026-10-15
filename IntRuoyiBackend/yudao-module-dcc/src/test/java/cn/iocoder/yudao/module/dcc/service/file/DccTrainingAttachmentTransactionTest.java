package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketBoundFile;
import cn.iocoder.yudao.module.dcc.service.upload.DccTrainingUploadTicketBoundFile;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Task-private H2 schema, actual attachment Mapper and Spring transaction. No shared fixture modifications. */
class DccTrainingAttachmentTransactionTest {
    private JdbcTemplate jdbc;
    private TransactionTemplate tx;
    private DccControlledFileAttachmentServiceImpl service;
    private DccControlledFileAttachmentMapper mapper;
    private DccOfflineTrainingRecordService training;
    private AdminUserApi users;
    private DccUploadTicketBoundFile genericBound=new DccUploadTicketBoundFile("formal-ticket",111L,"Training.pdf","application/pdf",12L);
    private DccTrainingUploadTicketBoundFile bound=new DccTrainingUploadTicketBoundFile(genericBound,"a".repeat(64));
    @BeforeEach void setup() throws Exception {
        TenantContextHolder.setTenantId(1L);
        var ds=new DriverManagerDataSource("jdbc:h2:mem:w06_attach_"+UUID.randomUUID()+";MODE=MYSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1","sa","");
        jdbc=new JdbcTemplate(ds);assertNotNull(jdbc.queryForObject("SELECT H2VERSION()",String.class));
        jdbc.execute("""
            CREATE TABLE dcc_controlled_file_attachment(
              id BIGINT AUTO_INCREMENT PRIMARY KEY,tenant_id BIGINT NOT NULL,controlled_file_id BIGINT NOT NULL,
              storage_file_id BIGINT NOT NULL,original_file_name VARCHAR(512),content_type VARCHAR(255),file_size BIGINT,
              file_sha256 VARCHAR(64),sort_no INT,attachment_purpose VARCHAR(32),application_process_instance_id VARCHAR(128),
              uploaded_by BIGINT,uploaded_username_snapshot VARCHAR(128),uploaded_nickname_snapshot VARCHAR(128),uploaded_at DATETIME,
              creator VARCHAR(64),create_time DATETIME,updater VARCHAR(64),update_time DATETIME,deleted TINYINT DEFAULT 0,
              training_record_round_key VARCHAR(128) GENERATED ALWAYS AS
                (CASE WHEN attachment_purpose='TRAINING_RECORD' AND deleted=0 THEN application_process_instance_id ELSE NULL END),
              UNIQUE(tenant_id,controlled_file_id,training_record_round_key))
            """);
        jdbc.execute("CREATE TABLE w06_file(id BIGINT PRIMARY KEY,tenant_id BIGINT,round VARCHAR(128),requester_id BIGINT,training_record_file_id BIGINT,status VARCHAR(64))");
        jdbc.execute("CREATE TABLE w06_ticket(id BIGINT PRIMARY KEY,status VARCHAR(32))");
        jdbc.execute("CREATE TABLE w06_progress(id BIGINT AUTO_INCREMENT PRIMARY KEY,round VARCHAR(128))");
        jdbc.update("INSERT INTO w06_file VALUES(20,1,'round-1',99,NULL,'PENDING_APPLICANT_TRAINING_RECORD')");
        jdbc.update("INSERT INTO w06_ticket VALUES(1,'AVAILABLE')");
        var factory=new MybatisSqlSessionFactoryBean();factory.setDataSource(ds);
        var configuration=new MybatisConfiguration();configuration.setMapUnderscoreToCamelCase(true);
        factory.setConfiguration(configuration);factory.setGlobalConfig(new GlobalConfig().setDbConfig(new GlobalConfig.DbConfig().setIdType(IdType.AUTO)));
        var sessions=factory.getObject();sessions.getConfiguration().addMapper(DccControlledFileAttachmentMapper.class);
        mapper=new SqlSessionTemplate(sessions).getMapper(DccControlledFileAttachmentMapper.class);
        tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        service=new DccControlledFileAttachmentServiceImpl();var files=mock(DccControlledFileMapper.class);
        when(files.selectByIdAndTenantForUpdate(anyLong(),anyLong())).thenAnswer(call->{
            var rows=jdbc.query("SELECT * FROM w06_file WHERE tenant_id=? AND id=? FOR UPDATE",(rs,n)->
                DccControlledFileDO.builder().id(rs.getLong("id")).tenantId(rs.getLong("tenant_id")).requesterId(rs.getLong("requester_id"))
                    .categoryId(2L).dccProjectCodeId(4L).needTraining(true).processInstanceId(rs.getString("round"))
                    .status(rs.getString("status")).trainingRecordFileId(rs.getObject("training_record_file_id",Long.class)).build(),
                    call.getArgument(0),call.getArgument(1));return rows.isEmpty()?null:rows.get(0);
        });
        training=mock(DccOfflineTrainingRecordService.class);when(training.requireConfiguredRecipients(any())).thenReturn(List.of(99L));
        when(training.requireUpload(eq(99L),any())).thenAnswer(call->new DccOfflineTrainingRecordService.Waiting(call.getArgument(1),"round-1","receive-1"));
        users=mock(AdminUserApi.class);when(users.getUser(99L)).thenReturn(new AdminUserRespDTO().setId(99L).setTenantId(1L).setStatus(0)
                .setUsername("uploader").setNickname("上传时姓名"));
        var storage=mock(FileMapper.class);when(storage.selectById(anyLong())).thenAnswer(call->FileDO.builder().id(call.getArgument(0))
                .name("Training.pdf").type("application/pdf").size(12L).configId(1L).path("owned/training.pdf").build());
        ReflectionTestUtils.setField(service,"attachmentMapper",mapper);ReflectionTestUtils.setField(service,"controlledFileMapper",files);
        ReflectionTestUtils.setField(service,"offlineTraining",training);ReflectionTestUtils.setField(service,"users",users);ReflectionTestUtils.setField(service,"fileMapper",storage);
    }
    @AfterEach void clear() {if(jdbc!=null)jdbc.execute("SHUTDOWN");TenantContextHolder.clear();}
    @Test void committedReplayPreservesOneAttachmentTimeAndAccountSnapshotAfterProgression() {
        Long id=tx.execute(s->service.bindTrainingRecord(99L,20L,"round-1",bound));
        var original=jdbc.queryForMap("SELECT * FROM dcc_controlled_file_attachment WHERE id=?",id);
        assertEquals("a".repeat(64),original.get("file_sha256"));
        jdbc.update("UPDATE w06_file SET training_record_file_id=111,status='ACTIVE' WHERE id=20");
        users.getUser(99L).setNickname("以后改名");
        assertEquals(id,tx.execute(s->service.bindTrainingRecord(99L,20L,"round-1",bound)));
        assertEquals(original,jdbc.queryForMap("SELECT * FROM dcc_controlled_file_attachment WHERE id=?",id));
        assertEquals(1,count());verify(training,times(1)).requireUpload(eq(99L),any());
        assertNotEquals(111L,id);
    }
    @Test void conflictingBodyAndRoundCannotOverwriteOriginal() {
        tx.execute(s->service.bindTrainingRecord(99L,20L,"round-1",bound));
        var original=jdbc.queryForList("SELECT * FROM dcc_controlled_file_attachment");
        var changed=new DccTrainingUploadTicketBoundFile(new DccUploadTicketBoundFile("other-ticket",112L,"Training.pdf","application/pdf",12L),"a".repeat(64));
        assertThrows(IllegalArgumentException.class,()->tx.execute(s->service.bindTrainingRecord(99L,20L,"round-1",changed)));
        assertThrows(IllegalArgumentException.class,()->tx.execute(s->service.bindTrainingRecord(99L,20L,"round-2",bound)));
        assertEquals(original,jdbc.queryForList("SELECT * FROM dcc_controlled_file_attachment"));
    }
    @Test void laterProcessFailureRollsBackAttachmentTicketFileAndProgressTogether() {
        jdbc.update("INSERT INTO dcc_controlled_file_attachment(tenant_id,controlled_file_id,storage_file_id,original_file_name) VALUES(1,20,88,'Historical.pdf')");
        assertThrows(IllegalStateException.class,()->tx.executeWithoutResult(s->{
            service.bindTrainingRecord(99L,20L,"round-1",bound);
            jdbc.update("UPDATE w06_ticket SET status='BOUND' WHERE id=1");
            jdbc.update("UPDATE w06_file SET training_record_file_id=111 WHERE id=20");
            jdbc.update("INSERT INTO w06_progress(round) VALUES('round-1')");
            throw new IllegalStateException("actual downstream failure injection");
        }));
        assertEquals(1,count());assertEquals("Historical.pdf",jdbc.queryForObject("SELECT original_file_name FROM dcc_controlled_file_attachment",String.class));
        assertEquals("AVAILABLE",jdbc.queryForObject("SELECT status FROM w06_ticket",String.class));
        assertNull(jdbc.queryForObject("SELECT training_record_file_id FROM w06_file",Long.class));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM w06_progress",Integer.class));
    }
    @Test void responseKeepsHistoricalNullsAndUsesStringIdsAndServerTimeFormat() throws Exception {
        tx.execute(s->service.bindTrainingRecord(99L,20L,"round-1",bound));
        jdbc.update("INSERT INTO dcc_controlled_file_attachment(tenant_id,controlled_file_id,storage_file_id,original_file_name) VALUES(1,20,88,'Historical.pdf')");
        var rows=service.listAttachments(20L);var legacy=rows.stream().filter(row->"Historical.pdf".equals(row.getFileName())).findFirst().orElseThrow();
        assertNull(legacy.getUploadedAt());assertNull(legacy.getUploadedBy());assertNull(legacy.getAttachmentPurpose());
        var current=rows.stream().filter(row->"TRAINING_RECORD".equals(row.getAttachmentPurpose())).findFirst().orElseThrow();
        var json=new com.fasterxml.jackson.databind.ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule()).valueToTree(current);
        assertTrue(json.get("attachmentId").isTextual());assertTrue(json.get("controlledFileId").isTextual());assertEquals("99",json.get("uploadedBy").asText());
        assertTrue(json.get("uploadedAt").asText().matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"));
    }
    @Test void concurrentSameRoundBindingReturnsOneCommittedAttachment() throws Exception {
        var executor=java.util.concurrent.Executors.newFixedThreadPool(2);
        var start=new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.Callable<Long> bind=()->{TenantContextHolder.setTenantId(1L);
            try {start.await();return tx.execute(s->service.bindTrainingRecord(99L,20L,"round-1",bound));}
            finally {TenantContextHolder.clear();}};
        try {var a=executor.submit(bind);var b=executor.submit(bind);start.countDown();
            assertEquals(a.get(10,java.util.concurrent.TimeUnit.SECONDS),b.get(10,java.util.concurrent.TimeUnit.SECONDS));
            assertEquals(1,count());
        } finally {executor.shutdownNow();assertTrue(executor.awaitTermination(10,java.util.concurrent.TimeUnit.SECONDS));}
    }
    @Test void generatedUniquenessConfinementLeavesHistoricalOrdinaryAttachmentsUnconstrained() {
        jdbc.update("INSERT INTO dcc_controlled_file_attachment(tenant_id,controlled_file_id,storage_file_id,original_file_name) VALUES(1,20,88,'Old1.pdf')");
        jdbc.update("INSERT INTO dcc_controlled_file_attachment(tenant_id,controlled_file_id,storage_file_id,original_file_name) VALUES(1,20,89,'Old2.pdf')");
        tx.execute(s->service.bindTrainingRecord(99L,20L,"round-1",bound));assertEquals(3,count());
        assertThrows(org.springframework.dao.DuplicateKeyException.class,()->jdbc.update("""
            INSERT INTO dcc_controlled_file_attachment(tenant_id,controlled_file_id,storage_file_id,original_file_name,attachment_purpose,application_process_instance_id)
            VALUES(1,20,112,'Conflicting.pdf','TRAINING_RECORD','round-1')
            """));
        assertEquals(3,count());
    }
    @Test void ordinaryBatchInsertFailureRollsBackWholeBatchWithTrueMapper() {
        jdbc.update("UPDATE w06_file SET status='WORKING' WHERE id=20");
        var ticketMapper=mock(cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileTemporaryFileMapper.class);
        when(ticketMapper.selectOne(org.mockito.ArgumentMatchers.<com.baomidou.mybatisplus.core.toolkit.support.SFunction<cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileTemporaryFileDO,?>>any(),any()))
                .thenAnswer(c->{var key=c.<String>getArgument(1);boolean second="formal-ticket-2".equals(key);
                    var row=cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileTemporaryFileDO.builder()
                            .uploadTicket(key).uploaderId(99L).categoryId(2L).purpose("ATTACHMENT").status("BOUND")
                            .boundControlledFileId(20L).storageFileId(second?112L:111L).originalFileName(second?"Second.pdf":"Training.pdf")
                            .contentType("application/pdf").fileSize(12L).fileSha256("a".repeat(64)).build();
                    row.setTenantId(1L);row.setCreateTime(java.time.LocalDateTime.of(2026,10,14,19,0));return row;});
        ReflectionTestUtils.setField(service,"temporaryFileMapper",ticketMapper);
        var storage=(FileMapper)ReflectionTestUtils.getField(service,"fileMapper");
        when(storage.selectById(112L)).thenReturn(FileDO.builder().id(112L).name("Second.pdf").type("application/pdf").size(12L).configId(1L).path("own/second.pdf").build());
        jdbc.execute("ALTER TABLE dcc_controlled_file_attachment ADD CONSTRAINT w06_attachment_valid_test_storage CHECK(storage_file_id<>112)");
        var second=new DccUploadTicketBoundFile("formal-ticket-2",112L,"Second.pdf","application/pdf",12L);
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                ()->tx.executeWithoutResult(s->service.bindAttachments(20L,List.of(genericBound,second))));
        assertEquals(0,count());
        tx.executeWithoutResult(s->service.bindAttachments(20L,List.of(genericBound)));
        assertEquals("ATTACHMENT",jdbc.queryForObject("SELECT attachment_purpose FROM dcc_controlled_file_attachment",String.class));
        assertEquals(1L,jdbc.queryForObject("SELECT tenant_id FROM dcc_controlled_file_attachment",Long.class));
    }
    @Test void sameMetadataButDifferentDigestRejectsReplayWithoutAnyRowMutation(){
        tx.execute(s->service.bindTrainingRecord(99L,20L,"round-1",bound));
        var before=jdbc.queryForList("SELECT * FROM dcc_controlled_file_attachment");
        assertThrows(IllegalArgumentException.class,()->tx.execute(s->service.bindTrainingRecord(99L,20L,"round-1",
                new DccTrainingUploadTicketBoundFile(genericBound,"b".repeat(64)))));
        assertEquals(before,jdbc.queryForList("SELECT * FROM dcc_controlled_file_attachment"));
    }
    @Test void historicalTrainingRowWithoutDigestCannotBeFilledByReplay(){
        tx.execute(s->service.bindTrainingRecord(99L,20L,"round-1",bound));
        jdbc.update("UPDATE dcc_controlled_file_attachment SET file_sha256=NULL");
        var before=jdbc.queryForList("SELECT * FROM dcc_controlled_file_attachment");
        assertThrows(IllegalArgumentException.class,()->tx.execute(s->service.bindTrainingRecord(99L,20L,"round-1",bound)));
        assertEquals(before,jdbc.queryForList("SELECT * FROM dcc_controlled_file_attachment"));
    }
    private int count(){return jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_attachment",Integer.class);}
}
