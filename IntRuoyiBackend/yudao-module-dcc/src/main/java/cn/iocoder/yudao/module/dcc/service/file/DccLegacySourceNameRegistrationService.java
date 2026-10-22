package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileNameClaimMapper;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.service.gxpaudit.*;
import jakarta.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.*;

/** Bounded internal registration; no controller, runner, client MATCH flag, or default actor. */
@Service
public class DccLegacySourceNameRegistrationService {
    public static final String OPERATION="dcc.controlled-file.legacy-name-occupancy.activate";
    private final JdbcTemplate jdbc;
    public DccLegacySourceNameRegistrationService(javax.sql.DataSource source){this.jdbc=new JdbcTemplate(Objects.requireNonNull(source));}
    @Resource private DccLegacyRegistrationSourceEnvironment sourceEnvironment;
    @Resource private PermissionApi permissions;
    @Resource private AdminUserApi users;
    @Resource private FileService fileService;
    @Resource private DccControlledFileNameClaimMapper claims;
    @Resource private GxpAuditService audit;
    public record Receipt(Long scopeId,Long eventId,Long sequence,String eventHash,boolean replay) { }

    @GxpWriteOperation(operationId="dcc.controlled-file.legacy-name-occupancy.activate")
    @Transactional(rollbackFor=Exception.class)
    public Receipt activateVerifiedScope(DccLegacyNameVerifiedScope sealed) {
        Long actor=authorize();
        if(!TransactionSynchronizationManager.isActualTransactionActive())throw new IllegalStateException("registration requires real transaction");
        var m=Objects.requireNonNull(sealed,"server sealed artifact required").manifest;
        Long tenant=TenantContextHolder.getRequiredTenantId();
        if(!Objects.equals(tenant,m.tenantId()))throw new IllegalArgumentException("sealed tenant mismatch");
        assertDatabaseIdentity(m);
        var evidence=m.evidence().stream().sorted(Comparator.comparing(DccLegacyNameVerifiedScope.Evidence::fileId)).toList();
        var claimIds=evidence.stream().map(DccLegacyNameVerifiedScope.Evidence::claimId).collect(java.util.stream.Collectors.toCollection(TreeSet::new));
        var masters=evidence.stream().map(DccLegacyNameVerifiedScope.Evidence::masterId).collect(java.util.stream.Collectors.toCollection(TreeSet::new));
        var fileIds=evidence.stream().map(DccLegacyNameVerifiedScope.Evidence::fileId).collect(java.util.stream.Collectors.toCollection(TreeSet::new));
        var existing=jdbc.queryForList("SELECT * FROM dcc_legacy_source_name_scope WHERE tenant_id=? AND scope_id=? FOR UPDATE",tenant,m.scopeId());
        boolean replay=!existing.isEmpty();
        if(replay && (existing.size()!=1 || !sealed.manifestSha256.equals(existing.get(0).get("manifest_sha256")) || !"VERIFIED".equals(existing.get(0).get("status"))))
            throw new IllegalArgumentException("registration scope conflicts with immutable original manifest");
        for(var master:masters)one("SELECT * FROM dcc_controlled_file_master WHERE tenant_id=? AND id=? AND deleted=0 FOR UPDATE",tenant,master);
        for(var claim:claimIds)one("SELECT * FROM dcc_controlled_file_name_claim WHERE tenant_id=? AND id=? AND deleted=0 FOR UPDATE",tenant,claim);
        if(!replay)assertExactLiveScope(tenant,claimIds,masters,fileIds);
        for(var e:evidence)verifyCurrentEvidence(tenant,e,!replay);
        var names=evidence.stream().map(DccLegacyNameVerifiedScope.Evidence::sourceName).distinct()
            .sorted(Comparator.comparing(name->java.util.HexFormat.of().formatHex(name.getBytes(java.nio.charset.StandardCharsets.UTF_8)))).toList();
        for(var name:names) {
            var occupied=jdbc.queryForList("SELECT * FROM dcc_source_name_reservation WHERE tenant_id=? AND source_name_key=? FOR UPDATE",tenant,name.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            if(!replay && !occupied.isEmpty())throw new IllegalStateException("legacy name already has a registry owner");
            if(!replay && jdbc.queryForObject("SELECT COUNT(*) FROM dcc_controlled_file_name_claim WHERE tenant_id=? AND source_name_key=? AND deleted=0",Long.class,tenant,name.getBytes(java.nio.charset.StandardCharsets.UTF_8))!=0)
                throw new IllegalStateException("legacy name already has a modern owner");
        }
        long scope;
        if(replay) {
            scope=((Number)existing.get(0).get("id")).longValue();
            var header=existing.get(0);
            require(header,"facts_sha256",m.factsSha256());require(header,"bytes_receipt_sha256",m.bytesReceiptSha256());require(header,"user_decision_sha256",m.userDecisionSha256());require(header,"scope_identity_sha256",m.scopeIdentitySha256());
            require(header,"claim_count",claimIds.size());require(header,"version_count",evidence.size());require(header,"source_count",evidence.size());
            require(header,"edge_count",evidence.stream().map(e->e.masterId()+":"+HexFormat.of().formatHex(e.sourceName().getBytes(java.nio.charset.StandardCharsets.UTF_8))).distinct().count());require(header,"name_count",names.size());
            require(header,"reason",m.reason());require(header,"request_id",m.requestId());
            if(!(header.get("verified_at") instanceof java.sql.Timestamp date) || !m.verifiedAt().equals(date.toLocalDateTime()))throw new IllegalArgumentException("sealed verification time changed");
            verifyPersistedScope(tenant,scope,evidence);
        } else {
            int edges=(int)evidence.stream().map(e->e.masterId()+":"+HexFormat.of().formatHex(e.sourceName().getBytes(java.nio.charset.StandardCharsets.UTF_8))).distinct().count();
            scope=insertScope(tenant,actor,sealed,claimIds.size(),evidence.size(),edges,names.size());
            for(var e:evidence)insertEvidence(tenant,scope,e,(int)evidence.stream().filter(x->x.claimId().equals(e.claimId())).count());
            for(var name:names)if(jdbc.update("INSERT INTO dcc_source_name_reservation(tenant_id,source_original_file_name,reservation_kind,verification_scope_id,generation,active,actor_id,reason) VALUES(?,?,'LEGACY_GROUP',?,1,1,?,?)",tenant,name,scope,actor,m.reason())!=1)
                throw new IllegalStateException("legacy name registration incomplete");
            if(jdbc.update("UPDATE dcc_legacy_source_name_scope SET status='VERIFIED',activated_at=? WHERE tenant_id=? AND id=? AND status='PREPARED' AND manifest_sha256=?",LocalDateTime.now(),tenant,scope,sealed.manifestSha256)!=1)
                throw new IllegalStateException("legacy scope activation identity changed");
            verifyPersistedScope(tenant,scope,evidence);
        }
        var state=JsonUtils.toJsonString(Map.of("scopeId",m.scopeId(),"manifestSha256",sealed.manifestSha256,"scopeIdentitySha256",m.scopeIdentitySha256(),"claims",claimIds.stream().map(String::valueOf).toList(),"versions",fileIds.stream().map(String::valueOf).toList()));
        var appended=audit.append(GxpAuditCommand.builder().operationId(OPERATION).subjectId("DCC_LEGACY_SOURCE_NAME_SCOPE:"+scope)
            .subjectVersion(sealed.manifestSha256).reason(m.reason()).requestId(m.requestId()).source(DccLegacySourceNameRegistrationService.class.getName()+"#activateVerifiedScope")
            .idempotencyKey("DCC:LEGACY:"+m.scopeId()+":"+sealed.manifestSha256)
            .beforeState(GxpAuditStateEnvelope.builder().state("ABSENT").canonicalJson("{}").build())
            .afterState(GxpAuditStateEnvelope.builder().state("PRESENT").objectVersion(sealed.manifestSha256).canonicalJson(state).build()).build());
        if(appended==null || appended.eventId()==null || appended.ledgerSequence()==null || appended.eventHash()==null)
            throw new IllegalStateException("actual configuration audit receipt missing");
        return new Receipt(scope,appended.eventId(),appended.ledgerSequence(),appended.eventHash(),replay);
    }

    private Long authorize() {
        var login=SecurityFrameworkUtils.getLoginUser();Long tenant=TenantContextHolder.getRequiredTenantId();
        if(login==null || login.getId()==null || !Objects.equals(tenant,login.getTenantId())
            || !Objects.equals(UserTypeEnum.ADMIN.getValue(),login.getUserType()) || login.getVisitTenantId()!=null && !Objects.equals(tenant,login.getVisitTenantId()))
            throw new IllegalArgumentException("actual authenticated same-tenant admin context required");
        var user=users.getUser(login.getId());
        if(user==null || !Objects.equals(user.getId(),login.getId()) || !Objects.equals(user.getTenantId(),tenant) || !Integer.valueOf(0).equals(user.getStatus())
            || user.getUsername()==null || user.getUsername().isBlank() || user.getNickname()==null || user.getNickname().isBlank()
            || login.getInfo()==null || !user.getUsername().equals(login.getInfo().get("username")) || !user.getNickname().equals(login.getInfo().get("nickname"))
            || !permissions.hasAnyRoles(login.getId(),"doc_control") || !permissions.hasAnyPermissions(login.getId(),"dcc:controlled-file:update"))
            throw new IllegalArgumentException("enabled actual doc_control and update permission required");
        return login.getId();
    }
    private void assertDatabaseIdentity(DccLegacyNameVerifiedScope.RootManifest m) {
        var row=sourceEnvironment.identity();
        if(!m.database().equals(row.get("database_name")) || !m.serverUuid().equals(row.get("server_uuid")))throw new IllegalArgumentException("actual database identity differs from sealed source");
    }
    private void assertExactLiveScope(Long tenant,Set<Long> claimIds,Set<Long> masters,Set<Long> fileIds) {
        var liveClaims=new TreeSet<>(jdbc.queryForList("SELECT id FROM dcc_controlled_file_name_claim WHERE tenant_id=? AND source_original_file_name IS NULL AND deleted=0 FOR UPDATE",Long.class,tenant));
        if(!claimIds.equals(liveClaims))throw new IllegalArgumentException("sealed legacy claim ID scope is incomplete");
        var ids=masters.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
        var liveVersions=new TreeSet<>(jdbc.queryForList("SELECT id FROM dcc_controlled_file WHERE tenant_id=? AND deleted=0 AND master_id IN ("+ids+") FOR UPDATE",Long.class,tenant));
        if(!fileIds.equals(liveVersions))throw new IllegalArgumentException("sealed original version ID scope changed");
    }
    private void verifyCurrentEvidence(Long tenant,DccLegacyNameVerifiedScope.Evidence e,boolean checkPreimage) {
        var claim=one("SELECT * FROM dcc_controlled_file_name_claim WHERE tenant_id=? AND id=? AND deleted=0",tenant,e.claimId());
        var master=one("SELECT * FROM dcc_controlled_file_master WHERE tenant_id=? AND id=? AND deleted=0",tenant,e.masterId());
        var file=one("SELECT * FROM dcc_controlled_file WHERE tenant_id=? AND id=? AND deleted=0 FOR UPDATE",tenant,e.fileId());
        var storage=one("SELECT * FROM infra_file WHERE id=? AND deleted=0",e.sourceFileId());
        var config=one("SELECT storage,config FROM infra_file_config WHERE id=? AND deleted=0",e.configId());
        var owner=one("SELECT * FROM dcc_controlled_file_source_ownership WHERE tenant_id=? AND controlled_file_id=? AND deleted=0",tenant,e.fileId());
        require(owner,"source_file_id",e.sourceFileId());require(owner,"source_sha256",e.sourceSha256());
        if(checkPreimage && (!e.claimPreimageSha256().equals(DccLegacyNameVerifiedScope.rowHash(claim)) || !e.masterPreimageSha256().equals(DccLegacyNameVerifiedScope.rowHash(master))
            || !e.filePreimageSha256().equals(DccLegacyNameVerifiedScope.rowHash(file)) || !e.storagePreimageSha256().equals(DccLegacyNameVerifiedScope.rowHash(storage))))
            throw new IllegalArgumentException("sealed original row preimage changed");
        require(claim,"master_id",e.masterId());require(claim,"normalized_name",e.claimName());require(claim,"source_original_file_name",null);
        require(claim,"dcc_project_code_id",null);require(claim,"file_type_taxonomy_leaf_id",null);require(claim,"normalized_file_number",null);
        require(master,"dcc_project_code_id",e.masterProjectId());require(master,"file_type_taxonomy_leaf_id",e.masterLeafId());require(master,"normalized_file_number",e.masterNumber());
        require(file,"master_id",e.masterId());require(file,"source_file_id",e.sourceFileId());require(file,"version_no",e.versionNo());require(file,"source_sha256",e.sourceSha256());
        require(file,"process_instance_id",e.processInstanceId());require(file,"source_original_file_name",null);
        require(storage,"config_id",e.configId());require(storage,"name",e.sourceName());require(storage,"path",e.sourcePath());require(storage,"size",e.sourceSize());require(config,"storage",e.storageType());
        var cfg=JsonUtils.parseObject(String.valueOf(config.get("config")),Map.class);
        require(cfg,"endpoint",e.storageEndpoint());require(cfg,"bucket",e.storageBucket());require(cfg,"region",e.storageRegion());require(cfg,"enablePathStyleAccess",e.storagePathStyle());
        try {
            var body=fileService.getFileContent(e.configId(),e.sourcePath());
            if(body==null || body.length!=e.sourceSize() || !DigestUtil.sha256Hex(body).equals(e.sourceSha256()))throw new IllegalArgumentException("actual source bytes do not match sealed scope");
        } catch(java.io.IOException failure){throw new IllegalStateException("actual source bytes unavailable",failure);}
        catch(Exception failure){if(failure instanceof RuntimeException r)throw r;throw new IllegalStateException("actual source bytes verification failed",failure);}
    }
    private void verifyPersistedScope(Long tenant,long scope,List<DccLegacyNameVerifiedScope.Evidence> evidence) {
        if(claims.countUnresolvedNames(tenant)!=0 || jdbc.queryForObject("SELECT COUNT(*) FROM dcc_legacy_source_name_evidence WHERE tenant_id=? AND verification_scope_id=?",Long.class,tenant,scope)!=evidence.size())
            throw new IllegalStateException("persisted scope does not have full verified source coverage");
        for(var e:evidence) {
            var saved=one("SELECT * FROM dcc_legacy_source_name_evidence WHERE tenant_id=? AND verification_scope_id=? AND controlled_file_id=?",tenant,scope,e.fileId());
            require(saved,"legacy_claim_id",e.claimId());require(saved,"legacy_master_id",e.masterId());require(saved,"source_file_id",e.sourceFileId());require(saved,"source_original_file_name",e.sourceName());
            require(saved,"config_id",e.configId());require(saved,"source_path",e.sourcePath());require(saved,"version_no",e.versionNo());
            require(saved,"storage_type",e.storageType());require(saved,"storage_endpoint",e.storageEndpoint());require(saved,"storage_bucket",e.storageBucket());
            require(saved,"storage_region",e.storageRegion());require(saved,"storage_path_style",e.storagePathStyle()?1:0);
            require(saved,"expected_sha256",e.sourceSha256());require(saved,"actual_sha256",e.sourceSha256());require(saved,"expected_size",e.sourceSize());require(saved,"actual_size",e.sourceSize());require(saved,"bytes_status","MATCH");
            require(saved,"expected_version_count",evidence.stream().filter(x->x.claimId().equals(e.claimId())).count());
            require(saved,"claim_normalized_name",e.claimName());require(saved,"claim_project_id",e.claimProjectId());require(saved,"claim_leaf_id",e.claimLeafId());require(saved,"claim_number",e.claimNumber());
            require(saved,"master_project_id",e.masterProjectId());require(saved,"master_leaf_id",e.masterLeafId());require(saved,"master_number",e.masterNumber());
            require(saved,"metadata_identity_sha256",e.storagePreimageSha256());require(saved,"preimage_sha256",DigestUtil.sha256Hex(e.filePreimageSha256()+e.masterPreimageSha256()+e.claimPreimageSha256()));
            require(saved,"proof_row_sha256",DigestUtil.sha256Hex(JsonUtils.toJsonString(e)));
        }
    }
    private long insertScope(Long tenant,Long actor,DccLegacyNameVerifiedScope sealed,int claimCount,int versions,int edges,int names) {
        var m=sealed.manifest;
        var keys=new GeneratedKeyHolder();
        int changed=jdbc.update(connection->{var statement=connection.prepareStatement("INSERT INTO dcc_legacy_source_name_scope(tenant_id,scope_id,manifest_sha256,facts_sha256,bytes_receipt_sha256,user_decision_sha256,scope_identity_sha256,status,claim_count,version_count,source_count,edge_count,name_count,verified_at,actor_id,reason,request_id) VALUES(?,?,?,?,?,?,?,'PREPARED',?,?,?,?,?,?,?,?,?)",new String[]{"id"});
            Object[] values={tenant,m.scopeId(),sealed.manifestSha256,m.factsSha256(),m.bytesReceiptSha256(),m.userDecisionSha256(),m.scopeIdentitySha256(),claimCount,versions,versions,edges,names,m.verifiedAt(),actor,m.reason(),m.requestId()};
            for(int i=0;i<values.length;i++)statement.setObject(i+1,values[i]);return statement;},keys);
        if(changed!=1 || keys.getKey()==null)throw new IllegalStateException("scope INSERT incomplete");return keys.getKey().longValue();
    }
    private void insertEvidence(Long tenant,long scope,DccLegacyNameVerifiedScope.Evidence e,int versionCount) {
        String sql="INSERT INTO dcc_legacy_source_name_evidence(tenant_id,verification_scope_id,legacy_claim_id,legacy_master_id,controlled_file_id,source_file_id,config_id,source_path,storage_type,storage_endpoint,storage_bucket,storage_region,storage_path_style,version_no,source_original_file_name,expected_sha256,actual_sha256,expected_size,actual_size,bytes_status,expected_version_count,claim_normalized_name,claim_project_id,claim_leaf_id,claim_number,master_project_id,master_leaf_id,master_number,metadata_identity_sha256,preimage_sha256,proof_row_sha256) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'MATCH',?,?,?,?,?,?,?,?,?,?,?)";
        if(jdbc.update(sql,tenant,scope,e.claimId(),e.masterId(),e.fileId(),e.sourceFileId(),e.configId(),e.sourcePath(),e.storageType(),e.storageEndpoint(),e.storageBucket(),e.storageRegion(),e.storagePathStyle()?1:0,e.versionNo(),e.sourceName(),e.sourceSha256(),e.sourceSha256(),e.sourceSize(),e.sourceSize(),versionCount,e.claimName(),e.claimProjectId(),e.claimLeafId(),e.claimNumber(),e.masterProjectId(),e.masterLeafId(),e.masterNumber(),e.storagePreimageSha256(),DigestUtil.sha256Hex(e.filePreimageSha256()+e.masterPreimageSha256()+e.claimPreimageSha256()),DigestUtil.sha256Hex(JsonUtils.toJsonString(e)))!=1)
            throw new IllegalStateException("evidence INSERT incomplete");
    }
    private Map<String,Object> one(String sql,Object...args){var rows=jdbc.queryForList(sql,args);if(rows.size()!=1)throw new IllegalArgumentException("exact current database row missing or ambiguous");return rows.get(0);}
    private void require(Map<String,?> row,String key,Object expected){var actual=row.get(key);if(!Objects.equals(actual==null?null:String.valueOf(actual),expected==null?null:String.valueOf(expected)))throw new IllegalArgumentException("sealed identity differs at "+key);}
}
