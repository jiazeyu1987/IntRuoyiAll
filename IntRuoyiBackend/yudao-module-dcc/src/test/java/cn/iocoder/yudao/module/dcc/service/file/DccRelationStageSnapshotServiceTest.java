package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccRelationStageSnapshotMapper;
import cn.iocoder.yudao.module.dcc.service.file.*;
import cn.iocoder.yudao.module.dcc.service.file.relations.*;
import cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationContracts.*;
import cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationStageContracts.*;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditService;
import org.h2.jdbcx.JdbcDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.*;
import java.util.*;
import java.nio.file.*;
import static org.mockito.Mockito.*;

/** Real production service/SQL/Spring transaction against task-private H2. External permission ports are explicit mocks. */
public class DccRelationStageSnapshotServiceTest {
    JdbcTemplate jdbc; TransactionTemplate tx; DccRelationStageSnapshotService service;
    DccControlledFileQueryService query; DccLatestControlledFileResolver files; DccRelationAccessPolicy access;
    GxpAuditService audit; int passed;
    DccRelationRemediationService remediation;
    DccApplicationRoundService rounds;
    LocalDateTime signedAt=LocalDateTime.of(2026,10,11,12,0);
    DccRelationStageSnapshotServiceTest() throws Exception {
        var ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1");
        jdbc=new JdbcTemplate(ds);tx=new TransactionTemplate(new DataSourceTransactionManager(ds));
        jdbc.execute("CREATE TABLE dcc_controlled_file_master(id BIGINT PRIMARY KEY,tenant_id BIGINT,deleted INT)");
        jdbc.update("INSERT INTO dcc_controlled_file_master VALUES(10,1,0),(20,1,0),(30,1,0),(40,1,0)");
        jdbc.execute("CREATE TABLE dcc_project_code(id BIGINT PRIMARY KEY,tenant_id BIGINT,deleted INT)");
        jdbc.update("INSERT INTO dcc_project_code VALUES(2,1,0)");
        jdbc.execute("CREATE TABLE dcc_controlled_file(id BIGINT PRIMARY KEY,tenant_id BIGINT,master_id BIGINT,dcc_project_code_id BIGINT,process_instance_id VARCHAR(128),deleted INT)");
        jdbc.update("INSERT INTO dcc_controlled_file VALUES(100,1,10,2,'round-1',0)");
        jdbc.execute("CREATE TABLE dcc_controlled_file_related_file(id BIGINT PRIMARY KEY,tenant_id BIGINT,controlled_file_id BIGINT,related_controlled_file_id BIGINT,related_master_id BIGINT,project_code_id BIGINT,related_file_number_snapshot VARCHAR(128),related_file_name_snapshot VARCHAR(256),related_version_no_snapshot VARCHAR(64),relation_source VARCHAR(32),deleted INT)");
        jdbc.update("INSERT INTO dcc_controlled_file_related_file VALUES(1,1,100,200,20,2,'A','原A','A/1','UPLOAD',0)");
        jdbc.execute("CREATE TABLE dcc_controlled_file_signature(id BIGINT PRIMARY KEY,controlled_file_id BIGINT,task_id VARCHAR(128),process_instance_id VARCHAR(128),actor_id BIGINT,signed_at DATETIME,evidence_status VARCHAR(32),evidence_hash VARCHAR(256),action_type VARCHAR(32),comment VARCHAR(4096),deleted INT)");
        for(String statement:Files.readString(Path.of("../sql/mysql/20260930_dcc_d_relations.sql")).replaceAll("(?m)^--.*$","").split(";"))
            if(!statement.isBlank())jdbc.execute(statement);
        // H2 exercises the same explicit columns/keys. MySQL metadata procedures have a separate source contract.
        String sql=Files.readString(Path.of("../sql/mysql/20261011_dcc_relation_stage_snapshots.sql"));
        var matcher=java.util.regex.Pattern.compile("CREATE TABLE IF NOT EXISTS dcc_relation_stage_(?:snapshot|member) \\(.*?\\) ENGINE=InnoDB[^;]*;",java.util.regex.Pattern.DOTALL).matcher(sql);
        int tables=0;while(matcher.find()) {
            String ddl=matcher.group().replaceAll(" ENGINE=InnoDB[^;]*;",";")
                    .replaceAll(" CHARACTER SET utf8mb4 COLLATE utf8mb4_bin","")
                    .replaceAll("UNIQUE KEY ","CONSTRAINT ");
            ddl=ddl.replaceAll("CONSTRAINT (\\w+) \\(","CONSTRAINT $1 UNIQUE (");
            ddl=ddl.replaceAll(",\\s*KEY \\w+ \\([^)]*\\)","");
            jdbc.execute(ddl);tables++;
        }
        check(tables==2,"actual new migration must supply both tables");
        DccRelationStageH2Schema.addNullableColumns(jdbc,"20261011_dcc_relation_stage_application_identity.sql");
        jdbc.execute("CREATE TABLE dcc_application_round_link(id BIGINT AUTO_INCREMENT PRIMARY KEY,tenant_id BIGINT,project_id BIGINT,application_type VARCHAR(16),application_id BIGINT,bpm_round VARCHAR(64),attribute_round INT,UNIQUE(tenant_id,application_type,application_id,attribute_round),UNIQUE(tenant_id,application_type,application_id,bpm_round))");
        DccRelationStageH2Schema.addNullableColumns(jdbc,"20261011_dcc_application_round_relation_source.sql");
        DccRelationStageH2Schema.addNullableColumns(jdbc,"20261012_dcc_obsolete_form_draft_round.sql");
        jdbc.update("INSERT INTO dcc_application_round_link(tenant_id,project_id,application_type,application_id,bpm_round,attribute_round) VALUES(1,2,'REVISION',100,'round-1',1)");
        jdbc.execute("CREATE TABLE bpm_form_action_instance(id BIGINT PRIMARY KEY,tenant_id BIGINT,system_code VARCHAR(64),object_type VARCHAR(64),action_code VARCHAR(64),object_id VARCHAR(128),object_version VARCHAR(128),status VARCHAR(32),bpm_process_instance_id VARCHAR(128),deleted INT)");
        query=mock(DccControlledFileQueryService.class);files=mock(DccLatestControlledFileResolver.class);
        when(files.resolveSelectedForUpdate(any())).thenAnswer(call->files.resolveSelected(call.getArgument(0)));
        access=mock(DccRelationAccessPolicy.class);audit=mock(GxpAuditService.class);
        when(files.resolveSelected(100L)).thenReturn(file(100,10,2,"PENDING_MATRIX_REVIEW",false,false,false));
        for(long id: new long[]{200,300,400}) {
            when(files.resolveSelected(id)).thenReturn(file(id,id/10,2,"ACTIVE",true,false,true));
            when(query.getRelationPermissions(7L,id)).thenReturn(permission(id,id/10,id));
        }
        service=new DccRelationStageSnapshotService(new DccRelationStore(jdbc,audit),new DccRelationStageSnapshotMapper(jdbc),query,files,access);
        rounds=new DccApplicationRoundService();
        var roundFiles=mock(cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper.class);
        when(roundFiles.selectById(any())).thenAnswer(call->{Long id=call.getArgument(0);var rows=jdbc.queryForList("SELECT id,tenant_id,master_id,dcc_project_code_id FROM dcc_controlled_file WHERE id=?",id);
            if(rows.isEmpty())return null;var row=rows.get(0);return cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO.builder().id(id).tenantId(((Number)row.get("TENANT_ID")).longValue()).masterId(((Number)row.get("MASTER_ID")).longValue()).dccProjectCodeId(((Number)row.get("DCC_PROJECT_CODE_ID")).longValue()).build();});
        var roundProjects=mock(cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper.class);
        var project=cn.iocoder.yudao.module.dcc.dal.dataobject.projectcode.DccProjectCodeDO.builder().id(2L).status("ENABLE").build();project.setTenantId(1L);
        when(roundProjects.selectByIdForUpdate(2L)).thenReturn(project);
        when(roundProjects.selectById(2L)).thenReturn(project);
        org.springframework.test.util.ReflectionTestUtils.setField(rounds,"jdbc",jdbc);
        org.springframework.test.util.ReflectionTestUtils.setField(rounds,"files",roundFiles);
        org.springframework.test.util.ReflectionTestUtils.setField(rounds,"projects",roundProjects);
        org.springframework.test.util.ReflectionTestUtils.setField(service,"rounds",rounds);
        remediation=new DccRelationRemediationService(new DccRelationStore(jdbc,audit),files,access,mock(DccRelationNotificationPostCommitScheduler.class));
        TenantContextHolder.setTenantId(1L);
        var login=new cn.iocoder.yudao.framework.security.core.LoginUser().setId(7L).setTenantId(1L).setUserType(2)
                .setInfo(Map.of("username","real-fixture-human",cn.iocoder.yudao.framework.security.core.LoginUser.INFO_KEY_NICKNAME,"真实测试操作者"));
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(login,null));
    }
    FileVersion file(long id,long master,long project,String status,boolean control,boolean pending,boolean executable){
        return new FileVersion(1L,id,master,project,"N"+id,"文件"+id,"A/1",status,control,pending,executable);
    }
    DccFileRelationPermissions permission(long id,long master,long current){
        return new DccFileRelationPermissions(id,false,true,1L,master,2L,"项目",null,null,"N"+id,"文件"+id,"A/1","ACTIVE",true,false,true,true,current,current);
    }
    ValidatedTaskIdentity task(String task,String obligation,String action){return new ValidatedTaskIdentity(1L,"round-1",task,obligation,"MATRIX_REVIEW","node-"+task,5L,action,"REVISION",1,null);}
    Prepared prepare(String task,String obligation,List<Long> extras,String key,String digest,List<Arrangement> arrangements) {
        return tx.execute(s->service.prepare(7L,100L,task(task,obligation,"ASSIGN"),extras,arrangements,key,digest,"节点理由"));
    }
    SignatureIdentity signature(Prepared p,long id){
        jdbc.update("INSERT INTO dcc_controlled_file_signature VALUES(?,?,?,?,?,?, 'VALID',? ,?,?,0)",id,100L,p.task().taskId(),"round-1",7L,signedAt,"evidence-"+id,p.task().actionType(),"节点理由；"+p.signedReasonMarker());
        return new SignatureIdentity(id,signedAt,"evidence-"+id);
    }
    Saved freeze(Prepared p,long signature){return tx.execute(s->service.freeze(p,signature(p,signature)));}
    int count(String table){return jdbc.queryForObject("SELECT COUNT(*) FROM "+table,Integer.class);}
    static void check(boolean result,String message){if(!result)throw new AssertionError(message);}
    static void rejects(Runnable run,String name){try{run.run();}catch(RuntimeException expected){return;}throw new AssertionError("Expected rejection: "+name);}
    @org.junit.jupiter.api.Test
    void completeAndImmutable() {
        var p=prepare("t1","dept-5",List.of(300L),"command-1",null,List.of(new Arrangement(30L,8L,signedAt.plusDays(2))));
        var saved=freeze(p,1);
        check(saved.members().size()==2,"complete set must contain upload A plus supplement B");
        check(saved.members().stream().filter(m->m.supplemental()).count()==1,"only B is newly supplemented");
        check(saved.members().get(0).selectedControlledFileId()==200L,"upload stays exact A/1");
        var bytes=jdbc.queryForList("SELECT * FROM dcc_relation_stage_member WHERE snapshot_id=?",saved.header().id());
        freeze(prepare("t2","dept-6",List.of(400L),"command-2",null,List.of()),2);
        check(count("dcc_relation_stage_snapshot")==2,"same actor different obligation must retain independent nodes");
        check(bytes.equals(jdbc.queryForList("SELECT * FROM dcc_relation_stage_member WHERE snapshot_id=?",saved.header().id())),"later supplement may not change prior signed complete set");
        check(jdbc.queryForObject("SELECT related_controlled_file_id FROM dcc_controlled_file_related_file",Long.class)==200L,"original upload unchanged");
    }
    @org.junit.jupiter.api.Test
    void replayAndConflicts() {
        var p=prepare("t1","dept-5",List.of(300L),"command-1",null,List.of());var first=freeze(p,1);
        when(files.resolveSelected(300L)).thenReturn(file(300,30,2,"OBSOLETE",false,false,false));
        var replay=prepare("t1","dept-5",List.of(300L),"command-1",p.expectedDigest(),List.of());
        var saved=tx.execute(s->service.freeze(replay,new SignatureIdentity(1L,signedAt,"evidence-1")));
        check(saved.equals(first),"exact replay returns complete original immutable evidence despite target obsolete");
        check(count("dcc_relation_stage_snapshot")==1 && count("dcc_relation_stage_member")==2,"replay zero duplicate writes");
        verify(audit,times(1)).append(any());
        rejects(()->prepare("t1","dept-5",List.of(400L),"command-1",p.expectedDigest(),List.of()),"changed supplement");
        rejects(()->tx.execute(s->service.prepare(8L,100L,p.task(),List.of(300L),List.of(),"command-1",p.expectedDigest(),"节点理由")),"different actor");
        rejects(()->tx.execute(s->service.prepare(7L,100L,p.task(),List.of(300L),List.of(),"command-1",p.expectedDigest(),"changed reason")),"changed reason");
        rejects(()->tx.execute(s->service.freeze(p,new SignatureIdentity(1L,signedAt.plusSeconds(1),"evidence-1"))),"signed time cannot change");
        check(count("dcc_relation_stage_snapshot")==1,"conflicts leave old head unchanged");
    }
    @org.junit.jupiter.api.Test
    void staleDigestRejectsBeforeWrite() {
        var stale=prepare("t2","dept-6",List.of(400L),"command-2",null,List.of());
        freeze(prepare("t1","dept-5",List.of(300L),"command-1",null,List.of()),1);
        rejects(()->prepare("t2","dept-6",List.of(400L),"command-2",stale.expectedDigest(),List.of()),"stale prior head digest");
        rejects(()->freeze(stale,2),"stale prepared at final freeze");
        check(count("dcc_relation_stage_snapshot")==1 && count("dcc_controlled_file_signature")==1,"stale signing rolls back new signature");
    }
    @org.junit.jupiter.api.Test
    void candidateRulesReject() {
        when(query.getRelationPermissions(7L,300L)).thenReturn(permission(300,30,400));
        rejects(()->prepare("t1","dept-5",List.of(300L),"c1",null,List.of()),"not current");
        when(query.getRelationPermissions(7L,300L)).thenReturn(permission(300,30,300));
        for(var invalid:List.of(file(300,30,3,"ACTIVE",true,false,true),file(300,30,2,"CONTROLLED_PENDING_EFFECTIVE",true,true,false),
                new FileVersion(2L,300L,30L,2L,"N300","文件300","A/1","ACTIVE",true,false,true))) {
            when(files.resolveSelected(300L)).thenReturn(invalid);
            rejects(()->prepare("t1","dept-5",List.of(300L),"c1",null,List.of()),"cross project/pending/tenant");
        }
        when(files.resolveSelected(300L)).thenReturn(file(300,30,2,"ACTIVE",true,false,true));
        rejects(()->prepare("t1","dept-5",List.of(100L),"c1",null,List.of()),"self");
        rejects(()->prepare("t1","dept-5",List.of(300L,300L),"c1",null,List.of()),"duplicate id");
        rejects(()->prepare("t1","dept-5",List.of(200L),"c1",null,List.of()),"already present master");
        check(count("dcc_relation_stage_snapshot")==0,"invalid choices no header writes");
    }
    @org.junit.jupiter.api.Test
    void signatureIdentityRejects() {
        var p=prepare("t1","dept-5",List.of(300L),"c1",null,List.of());
        rejects(()->tx.execute(s->service.freeze(p,new SignatureIdentity(999L,signedAt,"missing"))),"missing signature");
        rejects(()->tx.execute(s->{var sig=signature(p,1);jdbc.update("UPDATE dcc_controlled_file_signature SET process_instance_id='other' WHERE id=1");return service.freeze(p,sig);}),"wrong round");
        rejects(()->tx.execute(s->{var sig=signature(p,2);jdbc.update("UPDATE dcc_controlled_file_signature SET comment='unsigned payload' WHERE id=2");return service.freeze(p,sig);}),"unsigned digest");
        rejects(()->tx.execute(s->{var sig=signature(p,3);jdbc.update("UPDATE dcc_controlled_file_signature SET actor_id=8 WHERE id=3");return service.freeze(p,sig);}),"wrong signer");
        check(count("dcc_relation_stage_snapshot")==0 && count("dcc_controlled_file_signature")==0,"signature failure entire transaction rollback");
    }
    @org.junit.jupiter.api.Test
    void lateBpmAndAuditRollback() {
        var p=prepare("t1","dept-5",List.of(300L),"c1",null,List.of());
        rejects(()->tx.execute(s->{service.freeze(p,signature(p,1));throw new IllegalStateException("late BPM failure");}),"late BPM");
        check(count("dcc_relation_stage_snapshot")==0 && count("dcc_relation_stage_member")==0 && count("dcc_controlled_file_signature")==0,"late failure rolls all rows back");
        doThrow(new IllegalStateException("audit failure")).when(audit).append(any());
        rejects(()->freeze(p,2),"audit failure");
        check(count("dcc_relation_stage_snapshot")==0 && count("dcc_relation_stage_member")==0 && count("dcc_controlled_file_signature")==0,"audit failure rolls all rows back");
        check(count("dcc_controlled_file_related_file")==1,"upload history preserved");
    }
    @org.junit.jupiter.api.Test
    void memberInsertRollback() {
        var p=prepare("t1","dept-5",List.of(300L),"c1",null,List.of());
        jdbc.execute("ALTER TABLE dcc_relation_stage_member ADD CONSTRAINT intentional_member_failure CHECK(related_master_id<>30)");
        rejects(()->freeze(p,1),"second member failure");
        check(count("dcc_relation_stage_snapshot")==0 && count("dcc_relation_stage_member")==0 && count("dcc_controlled_file_signature")==0,"second member failure rolls head/member/signature back");
        verifyNoInteractions(audit);
    }
    @org.junit.jupiter.api.Test
    void actualReviewSnapshotDoesNotMutateEarlierSupervisor() {
        var supervisor=new ValidatedTaskIdentity(1L,"round-1","supervisor","supervisor-1","DOC_CONTROL_REVIEW","supervisor-node",null,"APPROVE","REVISION",1,null);
        var p=tx.execute(s->service.prepare(7L,100L,supervisor,List.of(),List.of(),"supervisor-command",null,"主管已看原A"));
        var signed=freeze(p,1);var original=jdbc.queryForList("SELECT * FROM dcc_relation_stage_member WHERE snapshot_id=?",signed.header().id());
        freeze(prepare("t1","dept-5",List.of(300L),"c1",null,List.of()),2);
        check(original.equals(jdbc.queryForList("SELECT * FROM dcc_relation_stage_member WHERE snapshot_id=?",signed.header().id())),"supervisor stays original A");
        check(signed.members().size()==1,"supervisor complete original set only");
        rejects(()->tx.execute(s->service.prepare(7L,100L,supervisor,List.of(300L),List.of(),"supervisor-command",p.expectedDigest(),"主管已看原A")),"supervisor cannot supplement");
    }
    @org.junit.jupiter.api.Test
    void normalizedHashIncludesObligationAndArrangements() {
        var a=prepare("t1","dept-5",List.of(400L,300L),"c1",null,List.of(new Arrangement(30L,8L,signedAt.plusDays(1))));
        var reordered=prepare("t1","dept-5",List.of(300L,400L),"c1",a.expectedDigest(),List.of(new Arrangement(30L,8L,signedAt.plusDays(1))));
        check(a.equals(reordered),"normalized input order deterministic");
        var different=prepare("t1","dept-6",List.of(300L,400L),"c1",a.expectedDigest(),List.of(new Arrangement(30L,8L,signedAt.plusDays(1))));
        check(!a.payloadHash().equals(different.payloadHash()),"obligation enters signature hash");
        var changed=prepare("t1","dept-5",List.of(300L,400L),"c1",a.expectedDigest(),List.of(new Arrangement(30L,9L,signedAt.plusDays(1))));
        check(!a.payloadHash().equals(changed.payloadHash()),"arrangement assignee enters hash");
        rejects(()->prepare("t1","dept-5",List.of(300L),"c1",null,List.of(new Arrangement(40L,8L,signedAt))),"arrangement master absent");
    }
    @org.junit.jupiter.api.Test
    void mandatoryAndExplicitCollections() {
        rejects(()->service.prepare(7L,100L,task("t1","dept-5","ASSIGN"),List.of(),List.of(),"c1",null,"节点理由"),"mandatory transaction");
        rejects(()->tx.execute(s->service.prepare(7L,100L,task("t1","dept-5","ASSIGN"),null,List.of(),"c1",null,"节点理由")),"supplements must be explicit");
        rejects(()->tx.execute(s->service.prepare(7L,100L,task("t1","dept-5","ASSIGN"),List.of(),null,"c1",null,"节点理由")),"arrangements must be explicit");
        check(count("dcc_relation_stage_snapshot")==0,"validation zero write");
    }
    @org.junit.jupiter.api.Test
    void signatureMarkerRequiresExactBoundDigest() {
        var p=prepare("t1","dept-5",List.of(300L),"c1",null,List.of());
        rejects(()->tx.execute(s->{var sig=signature(p,1);jdbc.update("UPDATE dcc_controlled_file_signature SET comment=? WHERE id=1",
                "dccRelationPayloadHash="+p.payloadHash()+"extra");return service.freeze(p,sig);}),"marker with forged suffix");
        check(count("dcc_relation_stage_snapshot")==0 && count("dcc_controlled_file_signature")==0,"bad marker zero write");
    }
    @org.junit.jupiter.api.Test
    void controlledRoundCannotAcceptNewNodeButAllowsOriginalReplay() {
        var p=prepare("t1","dept-5",List.of(300L),"c1",null,List.of());var saved=freeze(p,1);
        jdbc.update("INSERT INTO dcc_relation_controlled_event(tenant_id,master_id,controlled_file_id,application_round,event_key,controlled_at) VALUES(1,10,100,'round-1','controlled-100',?)",signedAt);
        rejects(()->prepare("t2","dept-6",List.of(400L),"c2",null,List.of()),"already controlled round");
        var replay=prepare("t1","dept-5",List.of(300L),"c1",p.expectedDigest(),List.of());
        check(tx.execute(s->service.freeze(replay,new SignatureIdentity(1L,signedAt,"evidence-1"))).equals(saved),"existing replay after control");
    }
    @org.junit.jupiter.api.Test
    void signedSupplementCanArrangeAndNotifyOnceWithoutChangingUpload() {
        var arrangement=new Arrangement(30L,8L,signedAt.plusDays(2));
        var p=prepare("t1","dept-5",List.of(300L),"c1",null,List.of(arrangement));
        tx.executeWithoutResult(s->{service.freeze(p,signature(p,1));remediation.saveArrangements(7L,100L,"round-1",List.of(arrangement),"指派整改");});
        check(count("dcc_controlled_file_related_file")==1,"no supplement inserted into original upload history");
        check(count("dcc_relation_arrangement")==1 && count("dcc_relation_notification_outbox")==0,"freeze+arrange before control has no notice");
        when(files.resolveSelected(100L)).thenReturn(file(100,10,2,"ACTIVE",true,false,true));
        when(files.resolveLatestForUpdate(10L)).thenReturn(file(100,10,2,"ACTIVE",true,false,true));
        var event=new ControlledEvent(1L,10L,100L,"round-1","controlled-100",signedAt);
        tx.executeWithoutResult(s->remediation.recordControlled(event));tx.executeWithoutResult(s->remediation.recordControlled(event));
        check(count("dcc_relation_remediation_task")==1 && count("dcc_relation_notification_outbox")==1,"one exact remediation obligation/outbox on repeated CONTROLLED");
        check(jdbc.queryForObject("SELECT related_master_id FROM dcc_relation_notification_outbox",Long.class)==30L,"new signed supplement drives notice");
        check(count("dcc_current_file_relation")==2,"control relation projection includes upload and signed supplement");
    }
    @org.junit.jupiter.api.Test
    void unsignedWrongRoundOrChangedArrangementCannotAuthorizeSupplement() {
        var arrangement=new Arrangement(30L,8L,signedAt.plusDays(2));
        freeze(prepare("t1","dept-5",List.of(300L),"c1",null,List.of(arrangement)),1);
        rejects(()->tx.executeWithoutResult(s->remediation.saveArrangements(7L,100L,"other-round",List.of(arrangement),"wrong round")),"other round");
        rejects(()->tx.executeWithoutResult(s->remediation.saveArrangements(7L,100L,"round-1",List.of(new Arrangement(30L,9L,arrangement.dueAt())),"changed assignee")),"unsigned arrangement");
        check(count("dcc_relation_arrangement")==0,"no unsigned arrangement persisted");
    }
    @org.junit.jupiter.api.Test
    void historicalEvidenceRetainsInvalidSignatureAndUnrecordedFacts() {
        var p=prepare("t1","dept-5",List.of(300L),"c1",null,List.of());freeze(p,1);
        jdbc.update("UPDATE dcc_controlled_file_signature SET evidence_status='INVALID' WHERE id=1");
        var evidence=service.readRoundEvidence(7L,100L,"REVISION","round-1");
        check(evidence.recorded() && evidence.stages().size()==1,"historical head still recorded when signature invalid");
        check("INVALID".equals(evidence.stages().get(0).signatureEvidenceStatus()),"independent current signature status exposed");
        check(evidence.completeSelections().size()==2,"history keeps upload plus originally signed supplement");
        check(evidence.stages().get(0).snapshot().members().get(1).selectedControlledFileId()==300L,"historical exact selected ID");
        rejects(()->prepare("t2","dept-6",List.of(),"c2",null,List.of()),"invalid signature cannot seed new snapshot");
    }
    @org.junit.jupiter.api.Test
    void pristineHistoricalRoundDoesNotInventAnApprovedSet() {
        var evidence=service.readRoundEvidence(7L,100L,"REVISION","round-1");
        check(!evidence.recorded() && evidence.stages().isEmpty(),"legacy no head is unrecorded");
        check(evidence.originalUploadSelections().size()==1,"original upload facts can remain separately visible");
        check(evidence.unavailableReason()!=null && !evidence.unavailableReason().isBlank(),"missing signed history stated");
        rejects(()->service.readRoundEvidence(7L,100L,"REVISION","foreign-round"),"unbound history");
    }
    @org.junit.jupiter.api.Test
    void exactRoundInheritanceKeepsObsoleteSelectionsAndSourceHistory() {
        freeze(prepare("t1","dept-5",List.of(300L),"c1",null,List.of()),1);
        var evidence=service.readRoundEvidence(7L,100L,"REVISION","round-1");
        var old=jdbc.queryForList("SELECT * FROM dcc_relation_stage_member");
        jdbc.update("INSERT INTO dcc_controlled_file VALUES(101,1,10,2,NULL,0)");
        when(files.resolveSelected(101L)).thenReturn(file(101,10,2,"WORKING",false,false,false));
        jdbc.update("INSERT INTO dcc_application_round_link(tenant_id,project_id,application_type,application_id,bpm_round,attribute_round) VALUES(1,2,'REVISION',101,NULL,1)");
        when(files.resolveSelected(300L)).thenReturn(file(300,30,2,"OBSOLETE",false,false,false));
        var source=tx.execute(s->service.inheritRoundSelections(7L,100L,"REVISION","round-1",101L,"REVISION",1,evidence.relationDigest()));
        var replay=tx.execute(s->service.inheritRoundSelections(7L,100L,"REVISION","round-1",101L,"REVISION",1,evidence.relationDigest()));
        check(source.equals(replay),"same inheritance precise replay");
        check(old.equals(jdbc.queryForList("SELECT * FROM dcc_relation_stage_member")),"source signed history unchanged");
        check(count("dcc_controlled_file_related_file")==1,"no rewrite/copy source upload snapshot");
        jdbc.update("UPDATE dcc_controlled_file SET process_instance_id='round-2' WHERE id=101");
        jdbc.update("UPDATE dcc_application_round_link SET bpm_round='round-2' WHERE application_id=101");
        var task=new ValidatedTaskIdentity(1L,"round-2","t2","dept-6","MATRIX_REVIEW","node-t2",5L,"ASSIGN","REVISION",1,null);
        var prepared=tx.execute(s->service.prepare(7L,101L,task,List.of(),List.of(),"c2",null,"原轮关系继承"));
        check(prepared.complete().size()==2 && prepared.complete().get(1).selectedControlledFileId()==300L,"target baseline keeps exact obsolete selected body");
        check("原A".equals(prepared.complete().get(0).fileName()),"original name stays frozen");
        verify(files,never()).resolveLatest(any());
        rejects(()->tx.execute(s->rounds.bindRelationSource(2L,"REVISION",101L,1,"REVISION",100L,"round-1","0".repeat(64))),"digest conflict");
    }
    @org.junit.jupiter.api.Test
    void obsoleteTaskUsesFormCenterRoundWithoutReplacingUploadBpm() {
        jdbc.update("INSERT INTO dcc_application_round_link(tenant_id,project_id,application_type,application_id,bpm_round,attribute_round) VALUES(1,2,'OBSOLETE',100,'obsolete-round',2)");
        jdbc.update("INSERT INTO bpm_form_action_instance VALUES(99,1,'DCC','CONTROLLED_FILE','OBSOLETE','100','A/1','IN_APPROVAL','obsolete-round',0)");
        var obsolete=new ValidatedTaskIdentity(1L,"obsolete-round","ot1","form-99:5","MATRIX_REVIEW","obsolete-node",5L,"ASSIGN","OBSOLETE",2,99L);
        var prepared=tx.execute(s->service.prepare(7L,100L,obsolete,List.of(),List.of(),"oc1",null,"正式作废指派"));
        jdbc.update("INSERT INTO dcc_controlled_file_signature VALUES(2,100,'ot1','obsolete-round',7,?,'VALID','obsolete-evidence','ASSIGN',?,0)",signedAt,"正式作废指派；"+prepared.signedReasonMarker());
        var saved=tx.execute(s->service.freeze(prepared,new SignatureIdentity(2L,signedAt,"obsolete-evidence")));
        check("OBSOLETE".equals(saved.header().applicationType()) && saved.header().formActionInstanceId()==99L,"new header stores real form identity");
        check("obsolete-round".equals(saved.header().processInstanceId()),"head process independent obsolete");
        check("round-1".equals(jdbc.queryForObject("SELECT process_instance_id FROM dcc_controlled_file WHERE id=100",String.class)),"File original upload BPM unchanged");
        check(service.readRoundEvidence(7L,100L,"OBSOLETE","obsolete-round").recorded(),"mapped obsolete history readable");
        rejects(()->tx.execute(s->service.prepare(7L,100L,obsolete,List.of(300L),List.of(),"oc1",null,"正式作废指派")),"obsolete no supplementation");
    }
    @org.junit.jupiter.api.Test
    void sourceBindingRequiresAllocatedTargetBoundSourceAndExactDigest() {
        jdbc.update("INSERT INTO dcc_controlled_file VALUES(101,1,10,2,NULL,0)");
        jdbc.update("INSERT INTO dcc_application_round_link(tenant_id,project_id,application_type,application_id,bpm_round,attribute_round) VALUES(1,2,'REVISION',101,NULL,1)");
        String digest="a".repeat(64);var before=jdbc.queryForList("SELECT * FROM dcc_application_round_link WHERE application_id=100");
        var saved=tx.execute(s->rounds.bindRelationSource(2L,"REVISION",101L,1,"REVISION",100L,"round-1",digest));
        var after=jdbc.queryForList("SELECT * FROM dcc_application_round_link WHERE application_id=101");
        check(saved.equals(tx.execute(s->rounds.bindRelationSource(2L,"REVISION",101L,1,"REVISION",100L,"round-1",digest))),"exact source binding replay");
        check(after.equals(jdbc.queryForList("SELECT * FROM dcc_application_round_link WHERE application_id=101")),"replay source row unchanged");
        check(before.equals(jdbc.queryForList("SELECT * FROM dcc_application_round_link WHERE application_id=100")),"original source mapping unchanged");
        rejects(()->tx.execute(s->rounds.bindRelationSource(2L,"REVISION",101L,1,"REVISION",100L,"round-1","b".repeat(64))),"different digest");
        rejects(()->tx.execute(s->rounds.bindRelationSource(2L,"REVISION",101L,2,"REVISION",100L,"round-1",digest)),"target not reserved");
        rejects(()->tx.execute(s->rounds.bindRelationSource(2L,"REVISION",101L,1,"REVISION",100L,"unknown",digest)),"source not bound");
        jdbc.update("INSERT INTO dcc_application_round_link(tenant_id,project_id,application_type,application_id,bpm_round,attribute_round,source_application_type) VALUES(1,2,'REVISION',101,NULL,2,'REVISION')");
        rejects(()->rounds.readRelationSource(2L,"REVISION",101L,2),"partial source cannot become no source");
    }
    @org.junit.jupiter.api.Test
    void inheritanceLateCallerAndAuditFailureRollBackTargetSourceOnly() {
        var evidence=service.readRoundEvidence(7L,100L,"REVISION","round-1");
        jdbc.update("INSERT INTO dcc_controlled_file VALUES(101,1,10,2,NULL,0)");
        when(files.resolveSelected(101L)).thenReturn(file(101,10,2,"WORKING",false,false,false));
        jdbc.update("INSERT INTO dcc_application_round_link(tenant_id,project_id,application_type,application_id,bpm_round,attribute_round) VALUES(1,2,'REVISION',101,NULL,1)");
        rejects(()->tx.execute(s->{service.inheritRoundSelections(7L,100L,"REVISION","round-1",101L,"REVISION",1,evidence.relationDigest());throw new IllegalStateException("late outer failure");}),"source binding caller rollback");
        check(rounds.readRelationSource(2L,"REVISION",101L,1)==null,"source CAS rolled back");
        doThrow(new IllegalStateException("inheritance audit failed")).when(audit).append(any());
        rejects(()->tx.execute(s->service.inheritRoundSelections(7L,100L,"REVISION","round-1",101L,"REVISION",1,evidence.relationDigest())),"source audit rollback");
        check(rounds.readRelationSource(2L,"REVISION",101L,1)==null && count("dcc_controlled_file_related_file")==1,"source facts preserved/target binding rollback");
    }
    @org.junit.jupiter.api.Test
    void obsoleteFormMismatchAndNativeWrongRoundRejectBeforeWriting() {
        jdbc.update("INSERT INTO dcc_application_round_link(tenant_id,project_id,application_type,application_id,bpm_round,attribute_round) VALUES(1,2,'OBSOLETE',100,'obsolete-round',2)");
        jdbc.update("INSERT INTO bpm_form_action_instance VALUES(99,1,'DCC','CONTROLLED_FILE','OBSOLETE','100','A/1','IN_APPROVAL','obsolete-round',0)");
        var obsolete=new ValidatedTaskIdentity(1L,"obsolete-round","ot1","form-99:5","MATRIX_REVIEW","on",5L,"ASSIGN","OBSOLETE",2,99L);
        for(String mutation:List.of("object_id='999'","object_version='B/1'","action_code='UPDATE'","tenant_id=2","status='REJECTED'","bpm_process_instance_id='other'")) {
            jdbc.update("UPDATE bpm_form_action_instance SET "+mutation+" WHERE id=99");
            rejects(()->tx.execute(s->service.prepare(7L,100L,obsolete,List.of(),List.of(),"oc",null,"作废事实")),"wrong form "+mutation);
            jdbc.update("UPDATE bpm_form_action_instance SET object_id='100',object_version='A/1',action_code='OBSOLETE',tenant_id=1,status='IN_APPROVAL',bpm_process_instance_id='obsolete-round' WHERE id=99");
        }
        rejects(()->tx.execute(s->service.prepare(7L,100L,obsolete,List.of(),List.of(new Arrangement(20L,8L,signedAt)),"oc",null,"作废事实")),"obsolete no remediation");
        var wrong=new ValidatedTaskIdentity(1L,"round-1","t1","d5","MATRIX_REVIEW","n",5L,"ASSIGN","REVISION",2,null);
        rejects(()->tx.execute(s->service.prepare(7L,100L,wrong,List.of(),List.of(),"c",null,"节点")),"wrong attribute identity");
        check(count("dcc_relation_stage_snapshot")==0,"identity failures zero writes");
    }
    @org.junit.jupiter.api.Test
    void historyKeepsOriginalHeadAfterFormRoundEndsAndSourceMetadataChanges() {
        freeze(prepare("t1","dept-5",List.of(300L),"c1",null,List.of()),1);
        jdbc.update("UPDATE dcc_relation_stage_snapshot SET application_type=NULL,attribute_round=NULL,form_action_instance_id=NULL");
        // The migration preserves an actually older head's absent identity; read does not invent its type on the head.
        var evidence=service.readRoundEvidence(7L,100L,"REVISION","round-1");
        check(evidence.stages().get(0).snapshot().header().applicationType()==null,"old head nullable identity retained");
        when(files.resolveSelected(300L)).thenReturn(file(300,30,2,"OBSOLETE",false,false,false));
        check(evidence.completeSelections().get(1).selectedControlledFileId()==300L,"metadata change cannot replace historical selected ID");
        verify(files,never()).resolveLatest(any());
    }
    @org.junit.jupiter.api.Test
    void obsoleteKeepsTheFormalNativeRoundSupplementsWithoutGuessingLatest() {
        freeze(prepare("t1","dept-5",List.of(300L),"c1",null,List.of()),1);
        jdbc.update("INSERT INTO dcc_application_round_link(tenant_id,project_id,application_type,application_id,bpm_round,attribute_round) VALUES(1,2,'OBSOLETE',100,'obsolete-round',2)");
        jdbc.update("INSERT INTO bpm_form_action_instance VALUES(99,1,'DCC','CONTROLLED_FILE','OBSOLETE','100','A/1','IN_APPROVAL','obsolete-round',0)");
        var obsolete=new ValidatedTaskIdentity(1L,"obsolete-round","ot1","form99-d5","MATRIX_REVIEW","on",5L,"ASSIGN","OBSOLETE",2,99L);
        var prepared=tx.execute(s->service.prepare(7L,100L,obsolete,List.of(),List.of(),"oc",null,"作废冻结"));
        check(prepared.complete().size()==2 && prepared.complete().get(1).selectedControlledFileId()==300L,"obsolete complete set must retain formal native supplement B");
        check(prepared.complete().stream().noneMatch(Selection::supplemental),"obsolete does not label native saved facts as new supplement");
        verify(files,never()).resolveLatest(any());
    }
    @org.junit.jupiter.api.Test
    void concurrentSourceBindingReturnsOneExactReceipt() throws Exception {
        jdbc.update("INSERT INTO dcc_controlled_file VALUES(101,1,10,2,NULL,0)");
        jdbc.update("INSERT INTO dcc_application_round_link(tenant_id,project_id,application_type,application_id,bpm_round,attribute_round) VALUES(1,2,'REVISION',101,NULL,1)");
        var pool=java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            java.util.concurrent.Callable<DccApplicationRoundService.RelationSource> call=()->{
                TenantContextHolder.setTenantId(1L);
                try{return tx.execute(s->rounds.bindRelationSource(2L,"REVISION",101L,1,"REVISION",100L,"round-1","a".repeat(64)));}
                finally{TenantContextHolder.clear();}
            };
            var first=pool.submit(call);var second=pool.submit(call);
            check(first.get(10,java.util.concurrent.TimeUnit.SECONDS).equals(second.get(10,java.util.concurrent.TimeUnit.SECONDS)),"one precise concurrent source result");
        }finally{pool.shutdownNow();}
        check(jdbc.queryForObject("SELECT COUNT(*) FROM dcc_application_round_link WHERE application_id=101 AND source_application_id=100",Integer.class)==1,"source once on one target row");
    }
    @org.junit.jupiter.api.Test
    void actualEvidenceJsonKeepsEveryLongIdentityStringAndServerSecondTime() {
        freeze(prepare("t1","dept-5",List.of(300L),"c1",null,List.of(new Arrangement(30L,8L,signedAt.plusDays(2)))),1);
        var evidence=service.readRoundEvidence(7L,100L,"REVISION","round-1");
        var json=cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(evidence);
        var tree=cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseTree(json);
        for(var field:List.of("tenantId","fileId","masterId","projectId"))check(tree.get(field).isTextual(),"round Long serializer "+field);
        var saved=tree.get("stages").get(0).get("snapshot");var head=saved.get("header");
        for(var field:List.of("id","tenantId","sourceFileId","sourceMasterId","projectId","departmentId","actorId","signatureId"))
            check(head.get(field).isTextual(),"Saved/header Long serializer "+field);
        check("2026-10-11 12:00:00".equals(head.get("signedAt").asText()),"real server signedAt seconds format");
        for(var row:saved.get("members")) {
            for(var field:List.of("tenantId","snapshotId","relatedMasterId","selectedControlledFileId","projectId"))check(row.get(field).isTextual(),"member Long serializer "+field);
            if(row.hasNonNull("assigneeUserId"))check(row.get("assigneeUserId").isTextual(),"arrangement assignee identity string");
        }
        for(var row:tree.get("completeSelections"))for(var field:List.of("relatedMasterId","selectedControlledFileId","projectId"))
            check(row.get(field).isTextual(),"Selection Long serializer "+field);
    }
    @org.junit.jupiter.api.Test
    void selfownedAndSharedH2DdlRemainIdenticalAfterRepeatedCreation() throws Exception {
        var ds=new JdbcDataSource();ds.setURL("jdbc:h2:mem:ddl-"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1");
        var privateJdbc=new JdbcTemplate(ds);
        String shared=Files.readString(Path.of("src/test/resources/sql/create_tables.sql"));
        var tables=java.util.regex.Pattern.compile("CREATE TABLE IF NOT EXISTS (dcc_application_round_link|dcc_relation_stage_snapshot|dcc_relation_stage_member) \\(.*?\\);",java.util.regex.Pattern.DOTALL).matcher(shared);
        int created=0;while(tables.find()){privateJdbc.execute(tables.group());created++;}
        check(created==3,"actual shared H2 has all 3 formal relation tables");
        var adds=java.util.regex.Pattern.compile("ALTER TABLE (dcc_application_round_link|dcc_relation_stage_snapshot) ADD COLUMN IF NOT EXISTS [^;]+;").matcher(shared);
        while(adds.find())privateJdbc.execute(adds.group());
        var beforeColumns=privateJdbc.queryForList("SELECT TABLE_NAME,COLUMN_NAME,DATA_TYPE,IS_NULLABLE,CHARACTER_MAXIMUM_LENGTH FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME IN ('DCC_APPLICATION_ROUND_LINK','DCC_RELATION_STAGE_SNAPSHOT','DCC_RELATION_STAGE_MEMBER') ORDER BY TABLE_NAME,ORDINAL_POSITION");
        var beforeKeys=privateJdbc.queryForList("SELECT TABLE_NAME,CONSTRAINT_NAME,COLUMN_NAME,ORDINAL_POSITION FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE WHERE TABLE_NAME IN ('DCC_APPLICATION_ROUND_LINK','DCC_RELATION_STAGE_SNAPSHOT','DCC_RELATION_STAGE_MEMBER') AND CONSTRAINT_NAME LIKE 'UK_DCC%' ORDER BY TABLE_NAME,CONSTRAINT_NAME,ORDINAL_POSITION");
        DccRelationStageH2Schema.create(privateJdbc);DccRelationStageH2Schema.createRoundTable(privateJdbc);
        DccRelationStageH2Schema.create(privateJdbc);DccRelationStageH2Schema.createRoundTable(privateJdbc);
        check(beforeColumns.equals(privateJdbc.queryForList("SELECT TABLE_NAME,COLUMN_NAME,DATA_TYPE,IS_NULLABLE,CHARACTER_MAXIMUM_LENGTH FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME IN ('DCC_APPLICATION_ROUND_LINK','DCC_RELATION_STAGE_SNAPSHOT','DCC_RELATION_STAGE_MEMBER') ORDER BY TABLE_NAME,ORDINAL_POSITION")),"repeated actual helper leaves same columns/types/nullability");
        check(beforeKeys.equals(privateJdbc.queryForList("SELECT TABLE_NAME,CONSTRAINT_NAME,COLUMN_NAME,ORDINAL_POSITION FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE WHERE TABLE_NAME IN ('DCC_APPLICATION_ROUND_LINK','DCC_RELATION_STAGE_SNAPSHOT','DCC_RELATION_STAGE_MEMBER') AND CONSTRAINT_NAME LIKE 'UK_DCC%' ORDER BY TABLE_NAME,CONSTRAINT_NAME,ORDINAL_POSITION")),"repeated helpers preserve named real unique keys");
    }
    @org.junit.jupiter.api.Test
    void signedStageAuditUsesRealProjectionNamespaceHashTimeAndStableKey() {
        var p=prepare("t1","dept-5",List.of(300L),"c1",null,List.of());freeze(p,1);
        var captor=org.mockito.ArgumentCaptor.forClass(cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditCommand.class);
        verify(audit).append(captor.capture());var command=captor.getValue();
        check("DCC_CONTROLLED_FILE_SIGNATURE:1".equals(command.getSignatureRecordId()),"signed audit must reference actual DCC projection namespace");
        check("evidence-1".equals(command.getSignatureContentHash()),"signed audit hash must be actual projection evidence hash");
        check(command.getIdempotencyKey().startsWith("DCC_RELATION_STAGE:"),"stable signed audit command scope key");
        check(command.getAfterState().getCanonicalJson().contains("2026-10-11 12:00:00"),"signed audit carries actual time not new client time");
        check(command.getAfterState().getCanonicalJson().contains("DCC_CONTROLLED_FILE_SIGNATURE"),"signed after facts namespace explicit");
        var replay=prepare("t1","dept-5",List.of(300L),"c1",p.expectedDigest(),List.of());
        tx.execute(s->service.freeze(replay,new SignatureIdentity(1L,signedAt,"evidence-1")));
        verify(audit,times(1)).append(any());
    }
    @org.junit.jupiter.api.Test
    void unsignedSourceAuditRejectsAbsentOrForeignLoginAndRollsBackBinding() {
        var source=service.readRoundEvidence(7L,100L,"REVISION","round-1");
        jdbc.update("INSERT INTO dcc_controlled_file VALUES(101,1,10,2,NULL,0)");
        when(files.resolveSelected(101L)).thenReturn(file(101,10,2,"WORKING",false,false,false));
        jdbc.update("INSERT INTO dcc_application_round_link(tenant_id,project_id,application_type,application_id,bpm_round,attribute_round) VALUES(1,2,'REVISION',101,NULL,1)");
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        rejects(()->tx.execute(s->service.inheritRoundSelections(7L,100L,"REVISION","round-1",101L,"REVISION",1,source.relationDigest())),"must not turn missing human actor into system audit");
        check(rounds.readRelationSource(2L,"REVISION",101L,1)==null,"no-login audit rejects and source CAS rollback");
        var other=new cn.iocoder.yudao.framework.security.core.LoginUser().setId(8L).setTenantId(1L).setUserType(2);
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(other,null));
        rejects(()->tx.execute(s->service.inheritRoundSelections(7L,100L,"REVISION","round-1",101L,"REVISION",1,source.relationDigest())),"foreign actor cannot audit another requester source");
        check(rounds.readRelationSource(2L,"REVISION",101L,1)==null,"foreign actor source binding rollback");
    }
    @org.junit.jupiter.api.Test
    void reverseBoundRoundReadReturnsOnlyTheExplicitFormalSourceIdentity() {
        check("round-1".equals(rounds.resolveBoundBpm("REVISION",100L,1)),"exact source attribute round resolves original BPM");
        jdbc.update("INSERT INTO dcc_application_round_link(tenant_id,project_id,application_type,application_id,bpm_round,attribute_round) VALUES(1,2,'REVISION',100,'later-round',2)");
        check("round-1".equals(rounds.resolveBoundBpm("REVISION",100L,1)),"newer bound round cannot replace explicit source");
        rejects(()->rounds.resolveBoundBpm("REVISION",100L,3),"unallocated source");
        jdbc.update("INSERT INTO dcc_application_round_link(tenant_id,project_id,application_type,application_id,bpm_round,attribute_round) VALUES(1,2,'REVISION',100,NULL,3)");
        rejects(()->rounds.resolveBoundBpm("REVISION",100L,3),"reserved but unbound source");
        rejects(()->rounds.resolveBoundBpm("UPLOAD",100L,1),"wrong source type cannot select another existing mapping");
    }
    @org.junit.jupiter.api.AfterEach void clearTenant(){TenantContextHolder.clear();org.springframework.security.core.context.SecurityContextHolder.clearContext();}
    public static void main(String[] args) throws Exception {
        String[] scenarios={"completeAndImmutable","replayAndConflicts","staleDigestRejectsBeforeWrite","candidateRulesReject",
                "signatureIdentityRejects","lateBpmAndAuditRollback","memberInsertRollback","actualReviewSnapshotDoesNotMutateEarlierSupervisor",
                "normalizedHashIncludesObligationAndArrangements","mandatoryAndExplicitCollections","signatureMarkerRequiresExactBoundDigest",
                "controlledRoundCannotAcceptNewNodeButAllowsOriginalReplay","signedSupplementCanArrangeAndNotifyOnceWithoutChangingUpload",
                "unsignedWrongRoundOrChangedArrangementCannotAuthorizeSupplement","historicalEvidenceRetainsInvalidSignatureAndUnrecordedFacts",
                "pristineHistoricalRoundDoesNotInventAnApprovedSet","exactRoundInheritanceKeepsObsoleteSelectionsAndSourceHistory",
                "obsoleteTaskUsesFormCenterRoundWithoutReplacingUploadBpm","sourceBindingRequiresAllocatedTargetBoundSourceAndExactDigest",
                "inheritanceLateCallerAndAuditFailureRollBackTargetSourceOnly","obsoleteFormMismatchAndNativeWrongRoundRejectBeforeWriting",
                "historyKeepsOriginalHeadAfterFormRoundEndsAndSourceMetadataChanges","obsoleteKeepsTheFormalNativeRoundSupplementsWithoutGuessingLatest",
                "concurrentSourceBindingReturnsOneExactReceipt","actualEvidenceJsonKeepsEveryLongIdentityStringAndServerSecondTime",
                "selfownedAndSharedH2DdlRemainIdenticalAfterRepeatedCreation","signedStageAuditUsesRealProjectionNamespaceHashTimeAndStableKey",
                "unsignedSourceAuditRejectsAbsentOrForeignLoginAndRollsBackBinding","reverseBoundRoundReadReturnsOnlyTheExplicitFormalSourceIdentity"};
        int passed=0,failed=0;
        for(var scenario:scenarios) {
            var contract=new DccRelationStageSnapshotServiceTest();
            try{DccRelationStageSnapshotServiceTest.class.getDeclaredMethod(scenario).invoke(contract);passed++;System.out.println("PASS "+scenario);}
            catch(java.lang.reflect.InvocationTargetException e){failed++;System.out.println("FAIL "+scenario);e.getCause().printStackTrace(System.out);}
            finally{TenantContextHolder.clear();}
        }
        System.out.println("IC07 PRIVATE H2 tests="+scenarios.length+" passed="+passed+" failed="+failed);
        if(failed>0)System.exit(1);
    }
}
