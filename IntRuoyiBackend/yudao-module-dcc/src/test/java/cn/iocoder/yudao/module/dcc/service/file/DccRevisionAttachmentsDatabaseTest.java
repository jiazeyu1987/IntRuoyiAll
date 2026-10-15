package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.config.TenantProperties;
import cn.iocoder.yudao.framework.tenant.core.db.TenantDatabaseInterceptor;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileAttachmentUploadTicketReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileAttachmentDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.DccControlledFileTemporaryFileMapper;
import cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import cn.iocoder.yudao.module.dcc.service.upload.*;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.service.file.FileConfigService;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.infra.framework.file.core.client.FileClient;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** A08: private H2 schema, real SQL/ticket/attachment/source-copy services and Spring transactions.
 * Account/project/category/directory and physical storage are explicit external test ports.
 * Query/Revision/Workflow/BPM and real-page integration are not claimed by this phase. */
public class DccRevisionAttachmentsDatabaseTest {
    Fixture f;
    @BeforeEach void setup() throws Exception { f = new Fixture(); }
    @AfterEach void close() { f.close(); }

    @Test void bindsTwoIndependentTicketsToExactWorkingVersionWithRealUploadFacts() {
        f.bind();
        assertEquals(2, f.rows(21L).size());
        assertEquals(List.of(111L,112L), f.rows(21L).stream().map(DccControlledFileAttachmentDO::getStorageFileId).toList());
        for (var row : f.rows(21L)) {
            assertEquals(99L,row.getUploadedBy()); assertEquals("original",row.getUploadedUsernameSnapshot());
            assertNotNull(row.getUploadedAt()); assertNull(row.getApplicationProcessInstanceId());
            assertEquals("ATTACHMENT",row.getAttachmentPurpose());
        }
        assertEquals(2,f.jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_temporary_file WHERE status='BOUND' AND bound_controlled_file_id=21",Integer.class));
        assertTrue(f.rows(20L).isEmpty());
    }

    @Test void secondAttachmentDatabaseFailureRollsBackBothBindingsAndCanRetry() {
        f.jdbc.execute("ALTER TABLE dcc_controlled_file_attachment ADD CONSTRAINT fail_second CHECK(storage_file_id<>112)");
        String before=f.snapshot();
        assertThrows(RuntimeException.class,f::bind);
        assertEquals(before,f.snapshot());
        f.jdbc.execute("ALTER TABLE dcc_controlled_file_attachment DROP CONSTRAINT fail_second");
        f.bind(); assertEquals(2,f.rows(21L).size());
    }

    @Test void ticketWriteFailureRollsBackPriorTicketAndCreatesNoAttachment() {
        f.jdbc.execute("ALTER TABLE dcc_controlled_file_temporary_file ADD CONSTRAINT fail_bind CHECK(upload_ticket<>'ticket-b' OR status<>'BOUND')");
        String before=f.snapshot(); assertThrows(RuntimeException.class,f::bind); assertEquals(before,f.snapshot());
        f.jdbc.execute("ALTER TABLE dcc_controlled_file_temporary_file DROP CONSTRAINT fail_bind");
        f.bind(); assertEquals(2,f.rows(21L).size());
    }

    @Test void invalidSecondTicketCannotPartiallySaveFirst() {
        for (String update : List.of("purpose='SOURCE'","uploader_id=77","tenant_id=2","category_id=3",
                "session_id='dcc-checkin:19:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa'","expire_time=TIMESTAMP '2000-01-01 00:00:00'",
                "status='FAILED'","storage_file_id=999")) {
            try (var local=new Fixture()) {
                local.jdbc.update("UPDATE dcc_controlled_file_temporary_file SET "+update+" WHERE upload_ticket='ticket-b'");
                String before=local.snapshot(); assertThrows(RuntimeException.class,local::bind,update); assertEquals(before,local.snapshot(),update);
            } catch(Exception e) { throw new RuntimeException(e); }
            TenantContextHolder.setTenantId(1L);
        }
    }

    @Test void duplicateTicketOrSessionAndNullListRejectWithoutBinding() {
        for(var tickets : Arrays.asList(null,List.of(f.ticket("ticket-a",f.sessionA),f.ticket("ticket-a",f.sessionA)),
                List.of(f.ticket("ticket-a",f.sessionA),f.ticket("ticket-b",f.sessionA)))) {
            String before=f.snapshot();
            assertThrows(RuntimeException.class,()->f.tx.execute(s->{f.invoke("bindCheckinAttachments",99L,20L,21L,tickets);return null;}));
            assertEquals(before,f.snapshot());
        }
    }

    @Test void invalidCheckoutActorTargetIdentityDirectoryOrPermissionRejectsBeforeTickets() {
        for(String update : List.of("actor_id=77","base_iteration_id=19","status='CANCELLED'")) {
            try(var local=new Fixture()) {
                local.jdbc.update("UPDATE dcc_controlled_file_checkout SET "+update); String before=local.snapshot();
                assertThrows(RuntimeException.class,local::bind); assertEquals(before,local.snapshot());
            } catch(Exception e) {throw new RuntimeException(e);}
            TenantContextHolder.setTenantId(1L);
        }
        for(String update : List.of("master_id=11","category_id=3","dcc_project_code_id=6","directory_id=8",
                "predecessor_controlled_file_id=19","requester_id=77","version_no='A/1'","process_instance_id='old-round'","status='ACTIVE'")) {
            try(var local=new Fixture()) {
                local.jdbc.update("UPDATE dcc_controlled_file SET "+update+" WHERE id=21"); String before=local.snapshot();
                assertThrows(RuntimeException.class,local::bind); assertEquals(before,local.snapshot());
            } catch(Exception e) {throw new RuntimeException(e);}
            TenantContextHolder.setTenantId(1L);
        }
        when(f.permission.hasCategoryPermission(anyLong(),anyLong(),eq(DccFileCategoryPermissionActionEnum.UPLOAD))).thenReturn(false);
        String before=f.snapshot();assertThrows(RuntimeException.class,f::bind);assertEquals(before,f.snapshot());
        when(f.permission.hasCategoryPermission(anyLong(),anyLong(),eq(DccFileCategoryPermissionActionEnum.UPLOAD))).thenReturn(true);
        doThrow(new IllegalArgumentException("invalid formal folder")).when(f.placement).lockSourceLocation(99L,20L);
        assertThrows(RuntimeException.class,f::bind);assertEquals(before,f.snapshot());
    }

    @Test void freezesTwoOrdinaryAttachmentsAsIndependentBytesAndPreservesOriginalUploaderForOwner() {
        f.bind();f.training(); f.candidate();
        String original=f.jdbc.queryForObject("SELECT CONCAT(original_file_name,'|',uploaded_by,'|',application_process_instance_id) FROM dcc_controlled_file_attachment WHERE attachment_purpose='TRAINING_RECORD'",String.class);
        f.freeze();
        var source=f.rows(21L).stream().filter(r->"ATTACHMENT".equals(r.getAttachmentPurpose())).toList();var target=f.rows(30L);
        assertEquals(2,target.size());
        for(int i=0;i<2;i++) {
            assertNotEquals(source.get(i).getId(),target.get(i).getId());
            assertNotEquals(source.get(i).getStorageFileId(),target.get(i).getStorageFileId());
            assertEquals(source.get(i).getFileSha256(),target.get(i).getFileSha256());
            assertEquals(source.get(i).getUploadedAt(),target.get(i).getUploadedAt());
            assertEquals(99L,target.get(i).getUploadedBy()); assertNull(target.get(i).getApplicationProcessInstanceId());
            assertArrayEquals(f.content(source.get(i).getStorageFileId()),f.content(target.get(i).getStorageFileId()));
        }
        assertEquals(original,f.jdbc.queryForObject("SELECT CONCAT(original_file_name,'|',uploaded_by,'|',application_process_instance_id) FROM dcc_controlled_file_attachment WHERE attachment_purpose='TRAINING_RECORD'",String.class));
        assertEquals(2,f.jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_temporary_file WHERE bound_controlled_file_id=21",Integer.class));
        f.tx.execute(s->{f.jdbc.update("UPDATE dcc_controlled_file SET process_instance_id='new-round' WHERE id=30");f.attachments.bindApplicationRound(30L,"new-round");return null;});
        assertTrue(f.rows(30L).stream().allMatch(r->"new-round".equals(r.getApplicationProcessInstanceId())));
    }

    @Test void secondFrozenInsertFailureRollsBackRowsAndPhysicalCopiesThenCompleteRetry() {
        f.bind(); f.candidate();String before=f.snapshot();int bytesBefore=f.bytes.size();
        f.jdbc.execute("ALTER TABLE dcc_controlled_file_attachment ADD CONSTRAINT fail_freeze CHECK(controlled_file_id<>30 OR sort_no<>1)");
        assertThrows(RuntimeException.class,f::freeze);assertEquals(before,f.snapshot());assertEquals(bytesBefore,f.bytes.size());
        f.jdbc.execute("ALTER TABLE dcc_controlled_file_attachment DROP CONSTRAINT fail_freeze");
        f.freeze();assertEquals(2,f.rows(30L).size());
    }

    @Test void callerLateFailureRollsBackCandidateAttachmentsAndBytesThenSameIntentRetries() {
        f.bind();String before=f.snapshot();int bytesBefore=f.bytes.size();
        assertThrows(RuntimeException.class,()->f.tx.execute(s->{f.candidate();f.freezeInside();throw new IllegalStateException("later BPM write failed");}));
        assertEquals(before,f.snapshot());assertEquals(bytesBefore,f.bytes.size());
        f.candidate();f.freeze();assertEquals(2,f.rows(30L).size());
    }

    @Test void exactFreezeReplayAddsNoRowsOrCopiesAndChangedManifestRejects() {
        f.bind();f.candidate();f.freeze();String before=f.snapshot();int bytesBefore=f.bytes.size();
        f.freeze();assertEquals(before,f.snapshot());assertEquals(bytesBefore,f.bytes.size());
        assertThrows(RuntimeException.class,()->f.tx.execute(s->{f.invoke("freezeOrdinaryAttachments",77L,21L,30L,List.of(f.ids().get(0)));return null;}));
        assertEquals(before,f.snapshot());
        f.jdbc.update("UPDATE dcc_controlled_file_attachment SET uploaded_nickname_snapshot='changed' WHERE controlled_file_id=30");
        assertThrows(RuntimeException.class,f::freeze);
    }

    @Test void missingOrChangedSourceBytesAndWrongExpectedVersionRejectBeforeFreeze() {
        f.bind();f.candidate();String before=f.snapshot();
        assertThrows(RuntimeException.class,()->f.tx.execute(s->{f.invoke("freezeOrdinaryAttachments",77L,21L,30L,List.of(999L));return null;}));
        assertEquals(before,f.snapshot());
        f.bytes.put("own/111",new byte[]{9});assertThrows(RuntimeException.class,f::freeze);assertEquals(before,f.snapshot());
        f.bytes.remove("own/111");assertThrows(RuntimeException.class,f::freeze);assertEquals(before,f.snapshot());
    }

    @Test void ownerAndTenantTargetGuardsRunEvenWithNoOrdinaryAttachments() {
        f.candidate();doThrow(new IllegalArgumentException("no OWNER")).when(f.access).assertProjectOwner(77L,5L);
        String before=f.snapshot();assertThrows(RuntimeException.class,f::freeze);assertEquals(before,f.snapshot());
        reset(f.access);TenantContextHolder.setIgnore(true);assertThrows(RuntimeException.class,f::freeze);TenantContextHolder.setIgnore(false);
        assertEquals(before,f.snapshot());
    }

    @Test void mandatoryTransactionAndExplicitEmptySelectionDoNotInventAttachments() {
        assertThrows(RuntimeException.class,()->f.invoke("bindCheckinAttachments",99L,20L,21L,List.of()));
        f.tx.execute(s->{f.invoke("bindCheckinAttachments",99L,20L,21L,List.of());return null;});
        assertEquals(0,f.rows(21L).size());f.candidate();f.freeze();assertEquals(0,f.rows(30L).size());
    }

    @Test void versionMustAdvanceWithinSameFormalBodyAndUnknownSourceStateRejects() {
        for(String update:List.of("version_no='B/1-1'","version_no='A/1-1'")) {
            try(var local=new Fixture()) {
                if(update.equals("version_no='A/1-1'"))local.jdbc.update("UPDATE dcc_controlled_file SET version_no='A/1-2' WHERE id=20");
                local.jdbc.update("UPDATE dcc_controlled_file SET "+update+" WHERE id=21");
                String before=local.snapshot();assertThrows(RuntimeException.class,local::bind);assertEquals(before,local.snapshot());
            } catch(Exception e){throw new RuntimeException(e);}
            TenantContextHolder.setTenantId(1L);
        }
        f.jdbc.update("UPDATE dcc_controlled_file SET status='OBSOLETE' WHERE id=20");
        String before=f.snapshot();assertThrows(RuntimeException.class,f::bind);assertEquals(before,f.snapshot());
    }

    @Test void physicalCopyFailureMustRollbackEarlierCopyAndKeepSourceFacts() throws Exception {
        f.bind();f.candidate();String before=f.snapshot();int bytesBefore=f.bytes.size();
        when(f.storage.createFileAndReturnId(any(byte[].class),eq("Two.pdf"),anyString(),anyString())).thenThrow(new IllegalStateException("second physical copy failed"));
        assertThrows(RuntimeException.class,f::freeze);assertEquals(before,f.snapshot());assertEquals(bytesBefore,f.bytes.size());
    }

    @Test void expectedOrderAndFrozenBytesCannotBeReplacedDuringReplay() {
        f.bind();f.candidate();String before=f.snapshot();var ids=new ArrayList<>(f.ids());Collections.reverse(ids);
        assertThrows(RuntimeException.class,()->f.tx.execute(s->{f.invoke("freezeOrdinaryAttachments",77L,21L,30L,ids);return null;}));assertEquals(before,f.snapshot());
        f.freeze();f.bytes.put("own/"+f.rows(30L).get(0).getStorageFileId(),new byte[]{8});
        assertThrows(RuntimeException.class,f::freeze);
    }

    @Test void formalCandidateRequiresExactSelectedPredecessorAndBaselineDerivedVersion() {
        for(String update:List.of("selected_iteration_controlled_file_id=20","predecessor_controlled_file_id=20",
                "selected_iteration_version_no='A/1-99'","revision_source_controlled_file_id=19",
                "revision_base_active_controlled_file_id=19","revision_source_version_no='A/2'","version_no='C/1'")) {
            try(var local=new Fixture()) {
                local.bind();local.candidate();local.jdbc.update("UPDATE dcc_controlled_file SET "+update+" WHERE id=30");
                String before=local.snapshot();int bytes=local.bytes.size();assertThrows(RuntimeException.class,local::freeze,update);
                assertEquals(before,local.snapshot());assertEquals(bytes,local.bytes.size());
            }catch(Exception e){throw new RuntimeException(e);}
            TenantContextHolder.setTenantId(1L);
        }
    }

    @Test void sourceBodyAndAllTicketsJoinAttachmentFailureRollbackAndFullRetry() throws Exception {
        f.store(110L,"Source.docx",new byte[]{10,11,12},"application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        f.seedTicket("source-ticket","dcc-checkin:20:"+"c".repeat(32),110L);f.jdbc.update("UPDATE dcc_controlled_file_temporary_file SET purpose='SOURCE',session_id=? WHERE upload_ticket='source-ticket'",f.sessionA);
        String before=f.snapshot();int bytes=f.bytes.size();
        f.jdbc.execute("ALTER TABLE dcc_controlled_file_attachment ADD CONSTRAINT fail_body_attach CHECK(storage_file_id<>112)");
        assertThrows(RuntimeException.class,()->f.tx.execute(s->{f.bindBodyAndAttachments();return null;}));
        assertEquals(before,f.snapshot());assertEquals(bytes,f.bytes.size());
        f.jdbc.execute("ALTER TABLE dcc_controlled_file_attachment DROP CONSTRAINT fail_body_attach");
        f.tx.execute(s->{f.bindBodyAndAttachments();return null;});
        assertEquals(3,f.jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_temporary_file WHERE status='BOUND' AND bound_controlled_file_id=21",Integer.class));
        assertEquals(1,f.jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_source_ownership WHERE controlled_file_id=21",Integer.class));
        assertEquals(2,f.rows(21L).size());assertEquals(110L,f.jdbc.queryForObject("SELECT origin_source_file_id FROM dcc_controlled_file_source_ownership WHERE controlled_file_id=21",Long.class));
    }

    @Test void partialApplicantTargetUsesFormalPolicyAndKeepsRecordedBaselineFields() {
        f.bind();f.candidate();f.jdbc.update("UPDATE dcc_controlled_file SET requester_id=99,revision_change_type='PARTIAL',version_no='A/2' WHERE id=30");
        f.tx.execute(s->{f.invoke("freezeOrdinaryAttachments",99L,21L,30L,f.ids());return null;});
        assertEquals(2,f.rows(30L).size());var target=f.files.selectByIdAndTenantForUpdate(1L,30L);
        assertEquals(20L,target.getRevisionSourceControlledFileId());assertEquals(20L,target.getRevisionBaseActiveControlledFileId());
        assertEquals("A/1",target.getRevisionSourceVersionNo());assertEquals(21L,target.getSelectedIterationControlledFileId());
        assertEquals(21L,target.getPredecessorControlledFileId());assertEquals("A/1-1",target.getSelectedIterationVersionNo());
        verify(f.access,never()).assertProjectOwner(anyLong(),anyLong());
    }

    @Test void initialCandidateHasExactSelectedFormalIdentityAndNoInventedRevisionBaseline() {
        f.bind();f.candidate();f.jdbc.update("UPDATE dcc_controlled_file SET revision_base_active_controlled_file_id=NULL WHERE id=21");
        f.jdbc.update("UPDATE dcc_controlled_file SET requester_id=99,revision_change_type='INITIAL',version_no='A/1',revision_base_active_controlled_file_id=NULL,revision_source_controlled_file_id=NULL,revision_source_version_no=NULL WHERE id=30");
        f.tx.execute(s->{f.invoke("freezeOrdinaryAttachments",99L,21L,30L,f.ids());return null;});assertEquals(2,f.rows(30L).size());
        assertNull(f.files.selectByIdAndTenantForUpdate(1L,30L).getRevisionSourceControlledFileId());
    }

    @Test void a20_queryActuallyBindsSourceAndTwoAttachmentsInOneCommittedCheckout() throws Exception {
        var query=f.configureQuery();var input=f.checkinRequest();
        var result=f.tx.execute(s->query.checkinControlledFile(99L,20L,input));
        assertEquals("A/1-1",result.getVersionNo());assertEquals(2,result.getAttachments().size());
        assertNotNull(result.getOrdinaryAttachmentManifestHash());assertTrue(result.getOrdinaryAttachmentManifestHash().matches("[a-f0-9]{64}"));
        assertEquals(3,f.jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_temporary_file WHERE bound_controlled_file_id=? AND status='BOUND'",Integer.class,result.getId()));
        assertEquals(List.of(111L,112L),f.rows(result.getId()).stream().map(DccControlledFileAttachmentDO::getStorageFileId).toList());
        var checkout=f.checkouts.selectLatestByBaseIterationId(1L,20L);assertEquals("CHECKED_IN",checkout.getStatus());
        assertEquals(result.getId(),checkout.getCheckinIterationId());
        var hash=f.jdbc.queryForObject("SELECT checkin_payload_hash FROM dcc_controlled_file_checkout WHERE id=1",String.class);
        assertNotNull(hash);assertTrue(hash.matches("[a-f0-9]{64}"));
        String expected=cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(Arrays.asList("DCC_CHECKIN_PAYLOAD_V1",1L,99L,20L,"MINOR",null,"query-source-ticket",null,"dcc-checkin:20:"+"c".repeat(32),"Query two attachments","new remark",false,List.of(List.of("ticket-a",f.sessionA),List.of("ticket-b",f.sessionB))));
        assertEquals(DigestUtil.sha256Hex(expected),hash);
        assertEquals(1,f.jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_source_ownership WHERE controlled_file_id=?",Integer.class,result.getId()));
    }

    @Test void a20_committedBoundReplayReturnsExactManifestWithoutAnotherWriteOrCopy() throws Exception {
        var query=f.configureQuery();var input=f.checkinRequest();var first=f.tx.execute(s->query.checkinControlledFile(99L,20L,input));
        String before=f.snapshot();int bytes=f.bytes.size();var replay=f.tx.execute(s->query.checkinControlledFile(99L,20L,input));
        assertEquals(first.getId(),replay.getId());assertEquals(2,replay.getAttachments().size());assertEquals(before,f.snapshot());assertEquals(bytes,f.bytes.size());
    }

    @Test void a20_sameSourceWithEmptyMissingReorderedOrChangedAttachmentSessionCannotReplay() throws Exception {
        var query=f.configureQuery();var input=f.checkinRequest();f.tx.execute(s->query.checkinControlledFile(99L,20L,input));
        String before=f.snapshot();
        for(var list:Arrays.asList(null,List.<DccControlledFileAttachmentUploadTicketReqVO>of(),
                List.of(f.ticket("ticket-b",f.sessionB),f.ticket("ticket-a",f.sessionA)),
                List.of(f.ticket("ticket-a",f.sessionA)),
                List.of(f.ticket("ticket-a",f.sessionA),f.ticket("ticket-b","dcc-checkin:20:"+"d".repeat(32))))) {
            var changed=f.checkinRequest();changed.setAttachmentUploadTickets(list);
            assertThrows(RuntimeException.class,()->f.tx.execute(s->query.checkinControlledFile(99L,20L,changed)));
            assertEquals(before,f.snapshot());
        }
    }

    @Test void a20_boundFactTamperingAndLegacyNullRequestHashRefuseReplay() throws Exception {
        var query=f.configureQuery();var input=f.checkinRequest();var result=f.tx.execute(s->query.checkinControlledFile(99L,20L,input));
        f.jdbc.update("UPDATE dcc_controlled_file_attachment SET file_sha256=? WHERE controlled_file_id=? AND sort_no=1","d".repeat(64),result.getId());
        String before=f.snapshot();assertThrows(RuntimeException.class,()->f.tx.execute(s->query.checkinControlledFile(99L,20L,input)));assertEquals(before,f.snapshot());
        f.jdbc.update("UPDATE dcc_controlled_file_attachment SET file_sha256=? WHERE controlled_file_id=? AND sort_no=1",DigestUtil.sha256Hex(new byte[]{4,5}),result.getId());
        f.jdbc.update("UPDATE dcc_controlled_file_checkout SET checkin_payload_hash=NULL WHERE id=1");
        assertThrows(RuntimeException.class,()->f.tx.execute(s->query.checkinControlledFile(99L,20L,input)));
    }

    @Test void a20_realQueryLateAttachmentFailureRollsBackBodyTicketsCheckoutAndRetries() throws Exception {
        var query=f.configureQuery();var input=f.checkinRequest();String before=f.snapshot();int bytes=f.bytes.size();
        f.jdbc.execute("ALTER TABLE dcc_controlled_file_attachment ADD CONSTRAINT fail_query_second CHECK(storage_file_id<>112)");
        assertThrows(RuntimeException.class,()->f.tx.execute(s->query.checkinControlledFile(99L,20L,input)));assertEquals(before,f.snapshot());assertEquals(bytes,f.bytes.size());
        f.jdbc.execute("ALTER TABLE dcc_controlled_file_attachment DROP CONSTRAINT fail_query_second");
        assertEquals(2,f.tx.execute(s->query.checkinControlledFile(99L,20L,input)).getAttachments().size());
    }

    @Test void a20_queryRequiresExplicitTrainingAndReadManifestRatherThanInferringEmpty() throws Exception {
        var query=f.configureQuery();String before=f.snapshot();var input=f.checkinRequest();input.setNeedTraining(null);
        assertThrows(RuntimeException.class,()->f.tx.execute(s->query.checkinControlledFile(99L,20L,input)));assertEquals(before,f.snapshot());
        input.setNeedTraining(false);input.setAttachmentUploadTickets(null);
        assertThrows(RuntimeException.class,()->f.tx.execute(s->query.checkinControlledFile(99L,20L,input)));assertEquals(before,f.snapshot());
    }

    @Test void a20_remarkNullVersusExplicitClearAndChangedTrainingCannotReplay() throws Exception {
        var query=f.configureQuery();var input=f.checkinRequest();input.setRemark(null);f.tx.execute(s->query.checkinControlledFile(99L,20L,input));String before=f.snapshot();
        input.setRemark("");assertThrows(RuntimeException.class,()->f.tx.execute(s->query.checkinControlledFile(99L,20L,input)));assertEquals(before,f.snapshot());
        input.setRemark(null);input.setNeedTraining(true);assertThrows(RuntimeException.class,()->f.tx.execute(s->query.checkinControlledFile(99L,20L,input)));assertEquals(before,f.snapshot());
    }

    @Test void a20_equivalentSavedWhitespaceReplaysWhileChangedDiagramTicketCannot() throws Exception {
        var query=f.configureQuery();var input=f.checkinRequest();var saved=f.tx.execute(s->query.checkinControlledFile(99L,20L,input));String before=f.snapshot();
        input.setChangeDescription("Query two attachments");input.setRemark("new remark");assertEquals(saved.getId(),f.tx.execute(s->query.checkinControlledFile(99L,20L,input)).getId());assertEquals(before,f.snapshot());
        input.setDrawingPdfUploadTicket("not-the-saved-diagram");assertThrows(RuntimeException.class,()->f.tx.execute(s->query.checkinControlledFile(99L,20L,input)));assertEquals(before,f.snapshot());
    }

    @Test void a20_realQueryAfterCheckoutUpdateFailureRollsBackSourceCopiesAndEveryTicket() throws Exception {
        var query=f.configureQuery();var input=f.checkinRequest();
        // A real existing source ownership forces the uploaded body to be copied by the actual service.
        f.jdbc.update("INSERT INTO dcc_controlled_file_source_ownership(tenant_id,controlled_file_id,source_file_id,origin_source_file_id,source_sha256,ownership_type,claimed_by) VALUES(1,999,113,113,?,'PREVIOUS',99)",DigestUtil.sha256Hex(f.content(113L)));
        String before=f.snapshot();int bytes=f.bytes.size();
        f.jdbc.execute("ALTER TABLE dcc_controlled_file_checkout ADD CONSTRAINT fail_checkout_hash CHECK(checkin_payload_hash IS NULL)");
        assertThrows(RuntimeException.class,()->f.tx.execute(s->query.checkinControlledFile(99L,20L,input)));assertEquals(before,f.snapshot());assertEquals(bytes,f.bytes.size());
        f.jdbc.execute("ALTER TABLE dcc_controlled_file_checkout DROP CONSTRAINT fail_checkout_hash");
        var saved=f.tx.execute(s->query.checkinControlledFile(99L,20L,input));assertNotEquals(113L,f.files.selectById(saved.getId()).getSourceFileId());assertEquals(2,saved.getAttachments().size());
        var owned=f.bodyOwnership.selectByControlledFileId(1L,saved.getId());assertEquals(113L,owned.getOriginSourceFileId());assertEquals(3,f.jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_temporary_file WHERE bound_controlled_file_id=?",Integer.class,saved.getId()));
    }

    @Test void a20_callerFailureAfterCompleteQuerySaveRollsBackCommittedIntentAndCopiedBytes() throws Exception {
        var query=f.configureQuery();var input=f.checkinRequest();f.jdbc.update("INSERT INTO dcc_controlled_file_source_ownership(tenant_id,controlled_file_id,source_file_id,origin_source_file_id,source_sha256,ownership_type,claimed_by) VALUES(1,999,113,113,?,'PREVIOUS',99)",DigestUtil.sha256Hex(f.content(113L)));
        String before=f.snapshot();int bytes=f.bytes.size();
        assertThrows(RuntimeException.class,()->f.tx.execute(s->{query.checkinControlledFile(99L,20L,input);throw new IllegalStateException("late caller audit failure");}));
        assertEquals(before,f.snapshot());assertEquals(bytes,f.bytes.size());assertEquals(2,f.tx.execute(s->query.checkinControlledFile(99L,20L,input)).getAttachments().size());
    }

    @Test void a23_actualRevisionFreezesExactSelectedManifestAndStableFactsIntoCandidateHash() throws Exception {
        var query=f.configureQuery();long selected=f.tx.execute(s->query.checkinControlledFile(99L,20L,f.checkinRequest())).getId();
        var revisions=f.configureRevision(selected);var request=f.revisionRequest();
        var target=f.tx.execute(s->revisions.createRevision(77L,20L,selected,request));
        assertEquals("B/1",target.getVersionNo());assertEquals(selected,target.getPredecessorControlledFileId());assertEquals(selected,target.getSelectedIterationControlledFileId());assertEquals(2,f.rows(target.getId()).size());
        assertNotEquals(f.rows(selected).get(0).getStorageFileId(),f.rows(target.getId()).get(0).getStorageFileId());
        assertEquals(99L,f.rows(target.getId()).get(0).getUploadedBy());String before=f.snapshot();int bytes=f.bytes.size();
        assertEquals(target.getId(),f.tx.execute(s->revisions.createRevision(77L,20L,selected,request)).getId());assertEquals(before,f.snapshot());assertEquals(bytes,f.bytes.size());
        f.jdbc.update("UPDATE dcc_controlled_file_attachment SET uploaded_nickname_snapshot='updated fact' WHERE controlled_file_id=? AND sort_no=1",selected);
        String changed=f.snapshot();assertThrows(RuntimeException.class,()->f.tx.execute(s->revisions.createRevision(77L,20L,selected,request)));assertEquals(changed,f.snapshot());
    }

    @Test void a23_actualRevisionSecondFrozenInsertFailureRollsBackCandidateBodyAndAllAttachmentBytesThenRetries() throws Exception {
        var query=f.configureQuery();long selected=f.tx.execute(s->query.checkinControlledFile(99L,20L,f.checkinRequest())).getId();var revisions=f.configureRevision(selected);var request=f.revisionRequest();
        String before=f.snapshot();int bytes=f.bytes.size();
        f.jdbc.execute("ALTER TABLE dcc_controlled_file_attachment ADD CONSTRAINT fail_revision_second CHECK(controlled_file_id="+selected+" OR sort_no<>1)");
        assertThrows(RuntimeException.class,()->f.tx.execute(s->revisions.createRevision(77L,20L,selected,request)));assertEquals(before,f.snapshot());assertEquals(bytes,f.bytes.size());
        f.jdbc.execute("ALTER TABLE dcc_controlled_file_attachment DROP CONSTRAINT fail_revision_second");
        assertEquals(2,f.rows(f.tx.execute(s->revisions.createRevision(77L,20L,selected,request)).getId()).size());
    }

    @Test void a23_delayedStageUsesExactSourceAndStoredFourFactsWithoutOpeningDailyBody() throws Exception {
        var query=f.configureQuery();var file=f.files.selectById(20L);file.setStatus("PENDING_DELAYED_EFFECTIVE_CONFIRM");
        file.setProcessInstanceId("exact-delayed-round");
        file.setDelayedEffectiveTime(LocalDateTime.of(2026,10,30,9,1));file.setDelayedEffectiveConfirmedAt(LocalDateTime.of(2026,10,15,8,2));file.setDelayedEffectiveSignatureId(9001L);file.setDelayedEffectiveProcessInstanceId("exact-delayed-round");
        Long dailySource=ReflectionTestUtils.invokeMethod(query,"resolvePreviewReferenceId",file);assertNull(dailySource);
        @SuppressWarnings("unchecked") var readScope=(ThreadLocal<DccControlledFileBodyReadContext>)ReflectionTestUtils.getField(query,"bodyReadContext");
        readScope.set(new DccControlledFileBodyReadContext(DccControlledFileBodyReadContext.Intent.APPROVAL_TASK,"20","A/1","UPLOAD","exact-delayed-round","exact-task"));
        Long sourceId=ReflectionTestUtils.invokeMethod(query,"resolvePreviewReferenceId",file);assertEquals(Long.valueOf(110L),sourceId);readScope.remove();
        var projected=new cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileRespVO();
        ReflectionTestUtils.invokeMethod(query,"fillApplicationSelectionProjection",projected,file);
        assertEquals(file.getDelayedEffectiveTime(),projected.getDelayedEffectiveTime());assertEquals(file.getDelayedEffectiveConfirmedAt(),projected.getDelayedEffectiveConfirmedAt());assertEquals(9001L,projected.getDelayedEffectiveSignatureId());assertEquals("exact-delayed-round",projected.getDelayedEffectiveProcessInstanceId());
        var guard=new DccControlledFileDetailAuthorizationGuard();mockDependenciesForGuard(guard);
        assertThrows(RuntimeException.class,()->guard.requireBodyRead(99L,file,DccControlledFileBodyReadContext.daily(20L)));
    }
    @Test void a23_manifestHasStableOrderAndByteFactsAndRejectsAbsentServerHash() {
        f.bind();var manifest=f.tx.execute(s->f.attachments.readOrdinaryAttachmentManifest(99L,21L));
        assertEquals(f.ids(),manifest.attachmentIds());assertEquals(List.of(0,1),manifest.facts().stream().map(DccControlledFileAttachmentService.OrdinaryAttachmentFact::sortNo).toList());
        assertEquals(DigestUtil.sha256Hex(new byte[]{1,2,3}),manifest.facts().get(0).fileSha256());
        assertThrows(UnsupportedOperationException.class,()->manifest.facts().clear());
        assertThrows(IllegalArgumentException.class,()->new DccControlledFileAttachmentService.OrdinaryAttachmentManifest(List.of(),null));
    }
    @Test void a29_nonUploaderMetadataViewerAndTaskReviewerDoNotNeedUploadOrReadSourceBytesForDetails() throws Exception {
        var query=f.configureQuery();var guard=(DccControlledFileDetailAuthorizationGuard)ReflectionTestUtils.getField(query,"detailAuthorizationGuard");
        when(guard.isAllowed(eq(66L),any(),anyBoolean(),nullable(Map.class))).thenReturn(true);
        when(f.permission.hasCategoryPermission(anyLong(),eq(66L),eq(DccFileCategoryPermissionActionEnum.UPLOAD))).thenReturn(false);
        clearInvocations(f.storage);
        var view=f.tx.execute(s->query.getControlledFile(66L,20L));assertEquals(20L,view.getId());assertNull(view.getOrdinaryAttachmentManifestHash());
        verify(f.storage,never()).getFileContent(anyLong(),anyString());
        f.jdbc.update("UPDATE dcc_controlled_file SET status='PENDING_DELAYED_EFFECTIVE_CONFIRM',process_instance_id='exact-review' WHERE id=20");
        var reviewed=f.tx.execute(s->query.getControlledFile(66L,20L));assertEquals(20L,reviewed.getId());assertNull(reviewed.getOrdinaryAttachmentManifestHash());
        verify(f.storage,never()).getFileContent(anyLong(),anyString());
    }
    @Test void a29_actualRevisionRejectsHashlessOrWrongOrderedExpectedManifestBeforeCandidateOrCopy() throws Exception {
        var query=f.configureQuery();long selected=f.tx.execute(s->query.checkinControlledFile(99L,20L,f.checkinRequest())).getId();var revisions=f.configureRevision(selected);
        String before=f.snapshot();int bytes=f.bytes.size();
        var missing=f.revisionRequest();missing.setExpectedOrdinaryAttachmentManifestHash(null);
        assertThrows(IllegalArgumentException.class,()->f.tx.execute(s->revisions.createRevision(77L,20L,selected,missing)));assertEquals(before,f.snapshot());assertEquals(bytes,f.bytes.size());
        var empty=f.revisionRequest();empty.setExpectedOrdinaryAttachmentIds(List.of());
        assertThrows(IllegalArgumentException.class,()->f.tx.execute(s->revisions.createRevision(77L,20L,selected,empty)));assertEquals(before,f.snapshot());assertEquals(bytes,f.bytes.size());
        var wrongHash=f.revisionRequest();wrongHash.setExpectedOrdinaryAttachmentManifestHash("e".repeat(64));
        assertThrows(IllegalArgumentException.class,()->f.tx.execute(s->revisions.createRevision(77L,20L,selected,wrongHash)));assertEquals(before,f.snapshot());assertEquals(bytes,f.bytes.size());
    }
    @Test void a29_requesterWithViewOnlyProjectStillReadsMetadataWithoutHashOrSourceBytes() throws Exception {
        var query=f.configureQuery();when(f.access.hasProjectEditorOrOwner(99L,5L)).thenReturn(false);
        doThrow(new IllegalArgumentException("project is VIEW only")).when(f.access).assertProjectEditorOrOwner(99L,5L);
        clearInvocations(f.storage);
        var response=f.tx.execute(s->query.getControlledFile(99L,20L));assertEquals(20L,response.getId());assertNull(response.getOrdinaryAttachmentManifestHash());
        verify(f.storage,never()).getFileContent(anyLong(),anyString());
    }
    static void mockDependenciesForGuard(Object guard){Fixture.mockDependencies(guard);}

    /** Explicit source-ticket port for pre-existing body/round suites; no permissive resolve or hash matcher. */
    static void a24BoundSourcePort(DccControlledFileQueryServiceImpl query, DccUploadTicketService tickets,
            cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileCheckinReqVO request,
            Long categoryId, Long storageId, String name, String type, Long size, String sha256) {
        Object target=query;
        try { if(query instanceof org.springframework.aop.framework.Advised advised)target=advised.getTargetSource().getTarget(); }
        catch(Exception failure){throw new IllegalStateException(failure);}
        var formal=cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileTemporaryFileDO.builder()
                .uploadTicket(request.getUploadTicket()).sessionId(request.getSessionId()).categoryId(categoryId).uploaderId(99L)
                .purpose("SOURCE").storageFileId(storageId).originalFileName(name).contentType(type).fileSize(size).fileSha256(sha256)
                .status("AVAILABLE").cleanupStatus("ACTIVE").build();formal.setTenantId(TenantContextHolder.getRequiredTenantId());formal.setCreateTime(LocalDateTime.now().withNano(0));
        doAnswer(call->{DccUploadTicketMarkBoundCommand command=call.getArgument(0);
            assertEquals(request.getUploadTicket(),command.uploadTicket());assertEquals(request.getSessionId(),command.sessionId());assertEquals("SOURCE",command.purpose());assertEquals(categoryId,command.categoryId());assertEquals(99L,command.userId());
            formal.setBoundControlledFileId(command.controlledFileId());formal.setStatus("BOUND");formal.setCleanupStatus("BOUND");return null;
        }).when(tickets).markBound(any(DccUploadTicketMarkBoundCommand.class));
        var resolve=new DccUploadTicketResolveCommand(request.getUploadTicket(),99L,categoryId,request.getSessionId(),"SOURCE");
        when(tickets.resolveBoundFile(eq(resolve),anyLong())).thenAnswer(call->{assertEquals(formal.getBoundControlledFileId(),call.getArgument(1));assertEquals("BOUND",formal.getStatus());return new DccUploadTicketBoundFile(request.getUploadTicket(),storageId,name,type,size);});
        var ticketRows=mock(DccControlledFileTemporaryFileMapper.class);
        when(ticketRows.selectOne(org.mockito.ArgumentMatchers.<com.baomidou.mybatisplus.core.toolkit.support.SFunction<cn.iocoder.yudao.module.dcc.dal.dataobject.protection.DccControlledFileTemporaryFileDO,?>>any(),eq(request.getUploadTicket()))).thenReturn(formal);
        ReflectionTestUtils.setField(target,"checkinTemporaryFileMapper",ticketRows);
        var sources=(DccControlledFileSourceOwnershipService)ReflectionTestUtils.getField(target,"sourceOwnershipService");
        if (mockingDetails(sources).isMock() && !mockingDetails(sources).isSpy()) {
            var ownershipRows=(DccControlledFileSourceOwnershipMapper)ReflectionTestUtils.getField(target,"sourceOwnershipMapper");
            when(sources.inspectSource(storageId)).thenReturn(new DccControlledFilePreparedSource(storageId,storageId,sha256,false));
            var fixtureFiles=(DccControlledFileMapper)ReflectionTestUtils.getField(target,"controlledFileMapper");
            doAnswer(call->{Long id=call.getArgument(0);DccControlledFilePreparedSource prepared=call.getArgument(1);
                Long exactOrigin=storageId;
                if ("BOUND".equals(formal.getStatus())) {
                    var next=fixtureFiles.selectById(id);var immediateSource=fixtureFiles.selectById(next.getPredecessorControlledFileId());
                    exactOrigin=immediateSource.getSourceFileId();
                }
                assertEquals(exactOrigin,prepared.originSourceFileId());assertEquals(sha256,prepared.sourceSha256());
                when(ownershipRows.selectByControlledFileId(formal.getTenantId(),id)).thenReturn(
                        cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileSourceOwnershipDO.builder()
                                .tenantId(formal.getTenantId()).controlledFileId(id).sourceFileId(prepared.sourceFileId())
                                .originSourceFileId(prepared.originSourceFileId()).sourceSha256(sha256).ownershipType(call.getArgument(3)).claimedBy(call.getArgument(2)).build());
                return null;
            }).when(sources).claimSubmissionSource(anyLong(),any(DccControlledFilePreparedSource.class),eq(99L),eq("CHECKIN"));
        }
    }

    /** All schema columns are from current migrations/mapper fixtures, never written to shared test schema. */
    public static final class Fixture implements AutoCloseable {
        public final JdbcTemplate jdbc;
        public final TransactionTemplate tx;
        public final DccControlledFileAttachmentServiceImpl attachments=new DccControlledFileAttachmentServiceImpl();
        public final DccUploadTicketServiceImpl tickets=new DccUploadTicketServiceImpl();
        public final DccControlledFileSourceOwnershipService ownership=new DccControlledFileSourceOwnershipService();
        public final DccControlledFileMapper files;
        public final DccControlledFileAttachmentMapper attachmentMapper;
        public final DccControlledFileTemporaryFileMapper ticketMapper;
        public final DccControlledFileCheckoutMapper checkouts;
        public final FileMapper physical;
        public final DccControlledFileSourceOwnershipMapper bodyOwnership;
        public final FileService storage=mock(FileService.class);
        public final DccPublicUploadPlacementService placement=mock(DccPublicUploadPlacementService.class);
        public final DccProjectAccessService access=mock(DccProjectAccessService.class);
        public final DccControlledFileCategoryPermissionSupport permission=mock(DccControlledFileCategoryPermissionSupport.class);
        public final Map<String,byte[]> bytes=new HashMap<>();
        public final String sessionA="dcc-checkin:20:"+"a".repeat(32), sessionB="dcc-checkin:20:"+"b".repeat(32);
        AtomicLong sequence=new AtomicLong(1000);
        org.apache.ibatis.session.SqlSessionFactory sessions;
        Long revisionSelectedForRequest;
        public Fixture() throws Exception {
            TenantContextHolder.setTenantId(1L);TenantContextHolder.setIgnore(false);
            var ds=new DriverManagerDataSource("jdbc:h2:mem:g06_a08_"+UUID.randomUUID()+";MODE=MYSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1","sa","");
            try(var connection=ds.getConnection()){assertTrue(connection.getMetaData().getURL().startsWith("jdbc:h2:mem:"));}
            jdbc=new JdbcTemplate(ds);tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
            String base="creator VARCHAR(64),create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,updater VARCHAR(64),update_time TIMESTAMP,deleted TINYINT DEFAULT 0";
            jdbc.execute("CREATE TABLE dcc_controlled_file(id BIGINT PRIMARY KEY,tenant_id BIGINT,master_id BIGINT,category_id BIGINT,dcc_project_code_id BIGINT,file_type_taxonomy_id BIGINT,directory_id BIGINT,project_folder_id BIGINT,file_number VARCHAR(128),version_no VARCHAR(64),status VARCHAR(64),requester_id BIGINT,predecessor_controlled_file_id BIGINT,selected_iteration_controlled_file_id BIGINT,selected_iteration_version_no VARCHAR(64),revision_base_active_controlled_file_id BIGINT,revision_source_controlled_file_id BIGINT,revision_source_version_no VARCHAR(64),controlled_time TIMESTAMP,revision_change_type VARCHAR(32),process_instance_id VARCHAR(128),source_file_id BIGINT,drawing_pdf_file_id BIGINT,training_record_file_id BIGINT,"+base+")");
            jdbc.execute("CREATE TABLE dcc_controlled_file_source_ownership(id BIGINT AUTO_INCREMENT PRIMARY KEY,tenant_id BIGINT,controlled_file_id BIGINT,source_file_id BIGINT,origin_source_file_id BIGINT,source_sha256 VARCHAR(64),ownership_type VARCHAR(32),claimed_by BIGINT,claimed_time TIMESTAMP,"+base+",UNIQUE(tenant_id,controlled_file_id),UNIQUE(tenant_id,source_file_id))");
            jdbc.execute("CREATE TABLE dcc_controlled_file_checkout(id BIGINT PRIMARY KEY,tenant_id BIGINT,master_id BIGINT,base_iteration_id BIGINT,actor_id BIGINT,status VARCHAR(32),"+base+")");
            jdbc.execute("CREATE TABLE infra_file(id BIGINT PRIMARY KEY,config_id BIGINT,name VARCHAR(512),path VARCHAR(1024),url VARCHAR(1024),type VARCHAR(255),size BIGINT,"+base+")");
            jdbc.execute("CREATE TABLE dcc_controlled_file_temporary_file(id BIGINT AUTO_INCREMENT PRIMARY KEY,tenant_id BIGINT,upload_ticket VARCHAR(128),session_id VARCHAR(128),purpose VARCHAR(32),category_id BIGINT,uploader_id BIGINT,original_file_name VARCHAR(512),content_type VARCHAR(255),file_size BIGINT,file_sha256 VARCHAR(64),storage_file_id BIGINT,status VARCHAR(32),expire_time TIMESTAMP,bound_controlled_file_id BIGINT,bound_time TIMESTAMP,cleanup_status VARCHAR(32),cleanup_reason VARCHAR(64),cleanup_time TIMESTAMP,request_id VARCHAR(128),"+base+",active_slot_flag INT GENERATED ALWAYS AS(CASE WHEN status='AVAILABLE' AND cleanup_status IN ('ACTIVE','CLEANING') AND bound_controlled_file_id IS NULL AND deleted=0 THEN 1 ELSE NULL END),UNIQUE(tenant_id,uploader_id,session_id,purpose,active_slot_flag))");
            jdbc.execute("CREATE TABLE dcc_controlled_file_attachment(id BIGINT AUTO_INCREMENT PRIMARY KEY,tenant_id BIGINT,controlled_file_id BIGINT,storage_file_id BIGINT,original_file_name VARCHAR(512),content_type VARCHAR(255),file_size BIGINT,file_sha256 VARCHAR(64),sort_no INT,attachment_purpose VARCHAR(32),application_process_instance_id VARCHAR(128),uploaded_by BIGINT,uploaded_username_snapshot VARCHAR(128),uploaded_nickname_snapshot VARCHAR(128),uploaded_at TIMESTAMP,"+base+")");
            var factory=new MybatisSqlSessionFactoryBean();factory.setDataSource(ds);
            var config=new MybatisConfiguration();config.setMapUnderscoreToCamelCase(true);factory.setConfiguration(config);
            factory.setGlobalConfig(new GlobalConfig().setDbConfig(new GlobalConfig.DbConfig().setIdType(IdType.AUTO)));
            var tenant=new TenantProperties();tenant.setIgnoreTables(new HashSet<>(Set.of("infra_file")));
            var interceptors=new MybatisPlusInterceptor();interceptors.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantDatabaseInterceptor(tenant)));factory.setPlugins(interceptors);
            sessions=factory.getObject();for(var type:List.of(DccControlledFileMapper.class,DccControlledFileAttachmentMapper.class,DccControlledFileTemporaryFileMapper.class,DccControlledFileCheckoutMapper.class,DccControlledFileSourceOwnershipMapper.class,FileMapper.class))sessions.getConfiguration().addMapper(type);
            var sql=new SqlSessionTemplate(sessions);files=sql.getMapper(DccControlledFileMapper.class);attachmentMapper=sql.getMapper(DccControlledFileAttachmentMapper.class);ticketMapper=sql.getMapper(DccControlledFileTemporaryFileMapper.class);checkouts=sql.getMapper(DccControlledFileCheckoutMapper.class);physical=sql.getMapper(FileMapper.class);
            bodyOwnership=sql.getMapper(DccControlledFileSourceOwnershipMapper.class);
            mockDependencies(attachments);mockDependencies(tickets);mockDependencies(ownership);
            wire(tickets,"temporaryFileMapper",ticketMapper,"fileMapper",physical,"fileService",storage);
            var accounts=mock(AdminUserApi.class);when(accounts.getUser(99L)).thenReturn(new AdminUserRespDTO().setId(99L).setTenantId(1L).setStatus(0).setUsername("original").setNickname("原上传人"));
            when(accounts.getUser(77L)).thenReturn(new AdminUserRespDTO().setId(77L).setTenantId(1L).setStatus(0).setUsername("owner").setNickname("项目负责人"));
            wire(attachments,"attachmentMapper",attachmentMapper,"controlledFileMapper",files,"temporaryFileMapper",ticketMapper,"fileMapper",physical,"users",accounts);
            // API-specific dependencies are injected once they are added by GREEN; old RED remains executable.
            optional(attachments,"uploadTicketService",tickets);optional(attachments,"checkoutMapper",checkouts);optional(attachments,"permissionSupport",permission);optional(attachments,"projectAccessService",access);optional(attachments,"publicUploadPlacementService",placement);optional(attachments,"sourceOwnershipService",ownership);
            optional(attachments,"versionPolicy",DccControlledFileVersionPolicy.defaultPolicy());
            var scope=mock(DccControlledFileAssignmentScopeService.class);when(scope.isWithinAssignedFileScope(anyLong(),anyLong())).thenReturn(true);optional(attachments,"assignmentScopeService",scope);
            var configs=mock(FileConfigService.class);var client=mock(FileClient.class);when(configs.getFileClient(1L)).thenReturn(client);
            doAnswer(call->{bytes.remove(call.getArgument(0));return null;}).when(client).delete(anyString());
            wire(ownership,"fileMapper",physical,"fileService",storage,"fileConfigService",configs,"ownershipMapper",bodyOwnership,"controlledFileMapper",files);
            when(permission.hasCategoryPermission(anyLong(),anyLong(),eq(DccFileCategoryPermissionActionEnum.UPLOAD))).thenReturn(true);
            when(storage.getFileContent(eq(1L),anyString())).thenAnswer(call->{var value=bytes.get(call.getArgument(1));if(value==null)throw new IllegalStateException("missing bytes");return value;});
            when(storage.createFileAndReturnId(any(byte[].class),anyString(),anyString(),anyString())).thenAnswer(call->{long id=sequence.incrementAndGet();store(id,call.getArgument(1),call.getArgument(0),call.getArgument(3));return id;});
            doAnswer(call->{Long id=call.getArgument(0);var file=physical.selectById(id);if(file!=null){bytes.remove(file.getPath());jdbc.update("DELETE FROM infra_file WHERE id=?",id);}return null;}).when(storage).deleteFile(anyLong());
            jdbc.update("INSERT INTO dcc_controlled_file(id,tenant_id,master_id,category_id,dcc_project_code_id,file_type_taxonomy_id,directory_id,project_folder_id,file_number,version_no,status,requester_id,source_file_id) VALUES(20,1,10,2,5,6,7,8,'F1','A/1','ACTIVE',99,110)");
            jdbc.update("INSERT INTO dcc_controlled_file(id,tenant_id,master_id,category_id,dcc_project_code_id,file_type_taxonomy_id,directory_id,project_folder_id,file_number,version_no,status,requester_id,predecessor_controlled_file_id,source_file_id) VALUES(21,1,10,2,5,6,7,8,'F1','A/1-1','WORKING',99,20,110)");
            jdbc.update("UPDATE dcc_controlled_file SET controlled_time=CURRENT_TIMESTAMP WHERE id=20");jdbc.update("UPDATE dcc_controlled_file SET revision_base_active_controlled_file_id=20 WHERE id=21");
            jdbc.update("INSERT INTO dcc_controlled_file_checkout(id,tenant_id,master_id,base_iteration_id,actor_id,status) VALUES(1,1,10,20,99,'ACTIVE')");
            store(111L,"One.pdf",new byte[]{1,2,3},"application/pdf");store(112L,"Two.pdf",new byte[]{4,5},"application/pdf");
            seedTicket("ticket-a",sessionA,111L);seedTicket("ticket-b",sessionB,112L);
        }
        void seedTicket(String ticket,String session,long id) {
            var file=physical.selectById(id);jdbc.update("INSERT INTO dcc_controlled_file_temporary_file(tenant_id,upload_ticket,session_id,purpose,category_id,uploader_id,original_file_name,content_type,file_size,file_sha256,storage_file_id,status,expire_time,cleanup_status) VALUES(1,?,?,'ATTACHMENT',2,99,?,?,?,?,?,'AVAILABLE',?,'ACTIVE')",ticket,session,file.getName(),file.getType(),file.getSize(),DigestUtil.sha256Hex(content(id)),id,LocalDateTime.now().plusHours(1));
        }
        public void store(long id,String name,byte[] data,String mime) {jdbc.update("INSERT INTO infra_file(id,config_id,name,path,type,size) VALUES(?,1,?,?,?,?)",id,name,"own/"+id,mime,data.length);bytes.put("own/"+id,data.clone());}
        public byte[] content(Long id){return bytes.get("own/"+id);}
        public void candidate(){jdbc.update("INSERT INTO dcc_controlled_file(id,tenant_id,master_id,category_id,dcc_project_code_id,file_type_taxonomy_id,directory_id,project_folder_id,file_number,version_no,status,requester_id,predecessor_controlled_file_id,selected_iteration_controlled_file_id,selected_iteration_version_no,revision_base_active_controlled_file_id,revision_source_controlled_file_id,revision_source_version_no,revision_change_type,source_file_id) VALUES(30,1,10,2,5,6,7,8,'F1','B/1','WORKING',77,21,21,'A/1-1',20,20,'A/1','REPLACEMENT',110)");}
        void training(){jdbc.update("INSERT INTO dcc_controlled_file_attachment(tenant_id,controlled_file_id,storage_file_id,original_file_name,attachment_purpose,application_process_instance_id,uploaded_by,sort_no) VALUES(1,21,199,'History-training.pdf','TRAINING_RECORD','history-round',99,999)");}
        public List<DccControlledFileAttachmentDO> rows(long id){return attachmentMapper.selectListByControlledFileId(id);}
        public List<Long> ids(){return rows(21L).stream().filter(r->"ATTACHMENT".equals(r.getAttachmentPurpose())).map(DccControlledFileAttachmentDO::getId).toList();}
        public void bind(){tx.execute(s->{invoke("bindCheckinAttachments",99L,20L,21L,List.of(ticket("ticket-a",sessionA),ticket("ticket-b",sessionB)));return null;});}
        public void freeze(){tx.execute(s->{freezeInside();return null;});}
        public void freezeInside(){invoke("freezeOrdinaryAttachments",77L,21L,30L,ids());}
        void bindBodyAndAttachments(){
            var body=ownership.createVerifiedCopy(110L);DccControlledFileVersionSourceRollback.enlist(ownership,body);
            jdbc.update("UPDATE dcc_controlled_file SET source_file_id=? WHERE id=21",body.sourceFileId());
            ownership.claimSubmissionSource(21L,body,99L,"CHECKIN");
            tickets.markBound(new DccUploadTicketMarkBoundCommand("source-ticket",99L,2L,sessionA,"SOURCE",21L));
            invoke("bindCheckinAttachments",99L,20L,21L,List.of(ticket("ticket-a",sessionA),ticket("ticket-b",sessionB)));
        }
        public cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileCheckinReqVO checkinRequest(){
            var input=new cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileCheckinReqVO();input.setUploadTicket("query-source-ticket");input.setSessionId("dcc-checkin:20:"+"c".repeat(32));input.setChangeDescription(" Query two attachments ");input.setRemark(" new remark ");input.setNeedTraining(false);
            input.setAttachmentUploadTickets(List.of(ticket("ticket-a",sessionA),ticket("ticket-b",sessionB)));return input;
        }
        public DccControlledFileQueryServiceImpl configureQuery() throws Exception {
            // Read the current shared schema as input only; every table is installed into this fixture's private H2 URL.
            String schema=java.nio.file.Files.readString(requireRepositoryRoot().resolve("IntRuoyiBackend/yudao-module-dcc/src/test/resources/sql/create_tables.sql"));
            jdbc.execute("DROP TABLE dcc_controlled_file");
            for(String table:List.of("dcc_controlled_file","dcc_controlled_file_master","dcc_project_code")){
                String marker="CREATE TABLE IF NOT EXISTS `"+table+"`";int start=schema.indexOf(marker);assertTrue(start>=0);
                jdbc.execute(schema.substring(start,schema.indexOf(";",start)+1));
            }
            jdbc.execute("ALTER TABLE dcc_controlled_file_checkout ADD COLUMN checkin_payload_hash CHAR(64)");
            for(String column:List.of("reason VARCHAR(500)","base_source_sha256 VARCHAR(64)","checkin_upload_ticket VARCHAR(128)","checkin_iteration_id BIGINT","checkin_source_file_id BIGINT","checkin_source_sha256 VARCHAR(64)","checked_in_time TIMESTAMP"))jdbc.execute("ALTER TABLE dcc_controlled_file_checkout ADD COLUMN "+column);
            var sql=new SqlSessionTemplate(sessions);
            sessions.getConfiguration().addMapper(DccControlledFileMasterMapper.class);sessions.getConfiguration().addMapper(cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper.class);
            var masters=sql.getMapper(DccControlledFileMasterMapper.class);var projects=sql.getMapper(cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper.class);
            jdbc.update("INSERT INTO dcc_project_code(id,tenant_id,project_code,project_name,status,deleted) VALUES(5,1,'G06-A20','G06-A20','ENABLE',0)");
            jdbc.update("INSERT INTO dcc_controlled_file_master(id,tenant_id,category_id,directory_id,file_name,file_number,dcc_project_code_id,file_type_taxonomy_leaf_id,normalized_file_number,status) VALUES(10,1,2,7,'Source','F1',5,6,'F1','ACTIVE_CHAIN')");
            store(110L,"Source.docx",new byte[]{10,11,12},"application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            store(113L,"Source.docx",new byte[]{13,14,15},"application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            var base=cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO.builder().id(20L).tenantId(1L).masterId(10L).categoryId(2L).directoryId(7L).dccProjectCodeId(5L).fileTypeTaxonomyId(6L).fileNumber("F1").fileName("Source").title("Source")
                .versionNo("A/1").status("WORKING").requesterId(99L).submitterId(99L).changeType("NEW").processType("CONTROLLED_FILE").needTraining(false).sourceFileId(110L).originalFileId(110L).sourceOriginalFileName("Source.docx").sourceSha256(DigestUtil.sha256Hex(content(110L))).checkedOutBy(99L).checkedOutTime(LocalDateTime.now()).remark("old remark").build();files.insert(base);
            jdbc.update("UPDATE dcc_controlled_file_checkout SET reason='real edit',base_source_sha256=? WHERE id=1",base.getSourceSha256());
            seedTicket("query-source-ticket","dcc-checkin:20:"+"c".repeat(32),113L);jdbc.update("UPDATE dcc_controlled_file_temporary_file SET purpose='SOURCE' WHERE upload_ticket='query-source-ticket'");
            var query=new DccControlledFileQueryServiceImpl();mockDependencies(query);
            var scope=mock(DccControlledFileAssignmentScopeService.class);when(scope.isWithinAssignedFileScope(anyLong(),anyLong())).thenReturn(true);
            var names=mock(DccControlledFileNameClaimService.class);when(names.requireSourceName(any())).thenReturn("Source.docx");
            var execution=mock(DccApplicationExecutionContractService.class);when(execution.inheritPrepared(anyLong(),anyLong(),eq(20L),eq(0))).thenReturn(new DccApplicationExecutionContractService.Prepared("LEGACY","exact-definition","d".repeat(64),"dcc-upload"));
            when(execution.readInheritedDraftFacts(anyLong(),anyLong(),eq(20L),eq(0))).thenReturn(new DccApplicationExecutionContractService.DraftFacts(true,false,"{}","[]","AFTER_STANDARDIZATION_SIGNATURE",null,"[]","[]",false,null,null,null));
            var guard=mock(DccControlledFileDetailAuthorizationGuard.class);when(guard.isAllowed(eq(99L),any(),anyBoolean(),nullable(Map.class))).thenReturn(true);
            wire(query,"controlledFileMapper",files,"controlledFileMasterMapper",masters,"projectCodeMapper",projects,"checkoutMapper",checkouts,"versionPolicy",DccControlledFileVersionPolicy.defaultPolicy(),"sourceOwnershipMapper",bodyOwnership,"sourceOwnershipService",ownership,"fileMapper",physical,"fileService",storage,"uploadTicketService",tickets,"attachmentService",attachments,"permissionSupport",permission,"projectAccessService",access,"assignmentScopeService",scope,"nameClaimService",names,"publicUploadPlacementService",placement,"executionContracts",execution,"detailAuthorizationGuard",guard,"downloadPolicyService",new cn.iocoder.yudao.module.dcc.service.download.DccDownloadPolicyService());
            optional(query,"checkinAttachmentMapper",attachmentMapper);optional(query,"checkinTemporaryFileMapper",ticketMapper);
            return query;
        }
        static java.nio.file.Path requireRepositoryRoot() {
            String explicit=System.getProperty("dcc.repository.root");
            java.nio.file.Path root;
            if (explicit!=null) {
                if (explicit.isBlank())throw new IllegalStateException("dcc.repository.root must be an explicit absolute repository path");
                root=java.nio.file.Path.of(explicit).normalize();
                if (!root.isAbsolute())throw new IllegalStateException("dcc.repository.root must be absolute");
            } else {
                String basedir=System.getProperty("basedir");
                if (basedir==null || basedir.isBlank())throw new IllegalStateException("Maven basedir is required; standalone runners must set dcc.repository.root");
                var module=java.nio.file.Path.of(basedir).normalize();
                if (!module.isAbsolute() || module.getFileName()==null || !"yudao-module-dcc".equals(module.getFileName().toString())
                        || module.getParent()==null || module.getParent().getFileName()==null
                        || !"IntRuoyiBackend".equals(module.getParent().getFileName().toString())
                        || module.getParent().getParent()==null || !java.nio.file.Files.isRegularFile(module.resolve("pom.xml")))
                    throw new IllegalStateException("Maven basedir must identify the real IntRuoyiBackend/yudao-module-dcc module");
                root=module.getParent().getParent();
            }
            if (!java.nio.file.Files.isRegularFile(root.resolve("AGENTS.md"))
                    || !java.nio.file.Files.isRegularFile(root.resolve("IntRuoyiBackend/pom.xml"))
                    || !java.nio.file.Files.isRegularFile(root.resolve("IntRuoyiBackend/yudao-module-dcc/pom.xml")))
                throw new IllegalStateException("DCC fixture repository root is missing its formal repository/module markers: "+root);
            if (!java.nio.file.Files.isRegularFile(root.resolve("IntRuoyiBackend/yudao-module-dcc/src/test/resources/sql/create_tables.sql")))
                throw new IllegalStateException("Current formal DCC test schema is missing under the verified repository root");
            return root;
        }
        public cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileSubmitIterationReqVO revisionRequest(){
            var request=new cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileSubmitIterationReqVO();request.setIdempotencyKey("g06-exact-revision");request.setRevisionChangeType("REPLACEMENT");request.setChangeDescription("formal attachment revision");request.setNeedTraining(false);
            request.setEffectiveMode("AFTER_STANDARDIZATION_SIGNATURE");request.setSelectedDistributionDepartmentIds(List.of());request.setSelectedSignoffDepartmentIds(List.of(10L));request.setSelectedSignoffUserIds(List.of());
            request.setProjectAttributes(new cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributes(List.of("CE"),null,"Y","N","N",null));request.setExecutionContract("LEGACY");request.setExpectedProcessDefinitionId("exact-definition");request.setExpectedDefinitionDigest("d".repeat(64));
            var manifest=tx.execute(s->attachments.readOrdinaryAttachmentManifest(77L,revisionSelectedForRequest));
            request.setExpectedOrdinaryAttachmentIds(manifest.attachmentIds());request.setExpectedOrdinaryAttachmentManifestHash(manifest.payloadHash());return request;
        }
        public DccControlledFileRevisionServiceImpl configureRevision(long selected){
            revisionSelectedForRequest=selected;
            jdbc.update("UPDATE dcc_controlled_file SET status='ACTIVE',controlled_time=CURRENT_TIMESTAMP WHERE id=20");jdbc.update("UPDATE dcc_controlled_file SET change_type='REVISION',revision_base_active_controlled_file_id=20 WHERE id=?",selected);
            jdbc.update("UPDATE dcc_controlled_file_master SET latest_controlled_file_id=20,current_active_controlled_file_id=20 WHERE id=10");
            var sql=new SqlSessionTemplate(sessions);var revisions=new DccControlledFileRevisionServiceImpl();mockDependencies(revisions);
            var scope=mock(DccControlledFileAssignmentScopeService.class);when(scope.isWithinAssignedFileScope(anyLong(),anyLong())).thenReturn(true);
            var names=mock(DccControlledFileNameClaimService.class);when(names.requireSourceName(any())).thenReturn("Source.docx");
            var execution=mock(DccApplicationExecutionContractService.class);var prepared=new DccApplicationExecutionContractService.Prepared("LEGACY","exact-definition","d".repeat(64),"dcc-revision");
            when(execution.inheritPrepared(eq(77L),anyLong(),eq(selected),eq(0))).thenReturn(prepared);when(execution.verifyInheritedPrepared(eq(77L),anyLong(),eq(selected),eq(0))).thenReturn(prepared);
            when(execution.readInheritedDraftFacts(eq(77L),anyLong(),eq(selected),eq(0))).thenReturn(new DccApplicationExecutionContractService.DraftFacts(true,false,"{}","[]","AFTER_STANDARDIZATION_SIGNATURE",null,"[10]","[]",false,null,null,null));
            var dates=new DccWorkflowDatePolicy();dates.setZoneId("Asia/Shanghai");
            wire(revisions,"fileMapper",files,"masterMapper",sql.getMapper(DccControlledFileMasterMapper.class),"checkoutMapper",checkouts,"projectAccessService",access,"assignmentScopeService",scope,"permissionSupport",permission,"sourceOwnershipService",ownership,"nameClaimService",names,"versionPolicy",DccControlledFileVersionPolicy.defaultPolicy(),"datePolicy",dates,"executionContracts",execution,"publicUploadPlacementService",placement);
            optional(revisions,"attachmentService",attachments);return revisions;
        }
        public DccControlledFileAttachmentUploadTicketReqVO ticket(String name,String session){var r=new DccControlledFileAttachmentUploadTicketReqVO();r.setUploadTicket(name);r.setSessionId(session);return r;}
        public void invoke(String method,Long actor,Long source,Long target,List<?> entries){
            try{attachments.getClass().getMethod(method,Long.class,Long.class,Long.class,List.class).invoke(attachments,actor,source,target,entries);}
            catch(NoSuchMethodException e){throw new AssertionError("G06 production attachment API missing: "+method,e);}
            catch(InvocationTargetException e){if(e.getCause() instanceof RuntimeException r)throw r;if(e.getCause() instanceof Error r)throw r;throw new RuntimeException(e.getCause());}
            catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
        }
        public String snapshot(){var all=new ArrayList<Object>();for(String table:List.of("dcc_controlled_file","dcc_controlled_file_checkout","dcc_controlled_file_temporary_file","dcc_controlled_file_attachment","dcc_controlled_file_source_ownership","infra_file")){var rows=jdbc.queryForList("SELECT * FROM "+table+" ORDER BY id");for(var row:rows)row.replaceAll((key,value)->value instanceof byte[] bytes?java.util.HexFormat.of().formatHex(bytes):value);all.add(rows);}return all.toString();}
        public static void mockDependencies(Object bean){for(var field:bean.getClass().getDeclaredFields())if(field.getAnnotation(jakarta.annotation.Resource.class)!=null)ReflectionTestUtils.setField(bean,field.getName(),mock(field.getType()));}
        public static void wire(Object bean,Object... pairs){for(int i=0;i<pairs.length;i+=2)ReflectionTestUtils.setField(bean,(String)pairs[i],pairs[i+1]);}
        static void optional(Object bean,String field,Object value){if(Arrays.stream(bean.getClass().getDeclaredFields()).anyMatch(f->f.getName().equals(field)))ReflectionTestUtils.setField(bean,field,value);}
        @Override public void close(){jdbc.execute("SHUTDOWN");TenantContextHolder.clear();}
    }
}
