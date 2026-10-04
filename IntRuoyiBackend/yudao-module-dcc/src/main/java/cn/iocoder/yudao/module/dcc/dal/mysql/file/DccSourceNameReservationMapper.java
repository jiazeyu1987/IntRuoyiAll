package cn.iocoder.yudao.module.dcc.dal.mysql.file;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccSourceNameReservationDO;
import org.apache.ibatis.annotations.*;
import java.time.LocalDateTime;
import java.util.List;
@Mapper
public interface DccSourceNameReservationMapper {
    @Select("SELECT * FROM dcc_source_name_reservation WHERE tenant_id=#{tenant} AND source_name_key=#{key} FOR UPDATE")
    DccSourceNameReservationDO lockName(@Param("tenant") Long tenant,@Param("key") byte[] key);
    @Select("SELECT * FROM dcc_source_name_reservation WHERE tenant_id=#{tenant} AND source_name_key=#{key}")
    DccSourceNameReservationDO readName(@Param("tenant") Long tenant,@Param("key") byte[] key);
    @Insert("INSERT INTO dcc_source_name_reservation(tenant_id,source_original_file_name,reservation_kind,modern_claim_id,modern_master_id,generation,active) VALUES(#{tenant},#{name},'MODERN',#{claim},#{master},1,1)")
    int insertModern(@Param("tenant") Long tenant,@Param("name") String name,@Param("claim") Long claim,@Param("master") Long master);
    @Update("UPDATE dcc_source_name_reservation SET reservation_kind='MODERN',verification_scope_id=NULL,modern_claim_id=#{claim},modern_master_id=#{master},generation=generation+1,active=1,update_time=CURRENT_TIMESTAMP(6) WHERE tenant_id=#{tenant} AND id=#{id} AND active=0 AND reservation_kind='NONE' AND generation=#{generation}")
    int acquireReleased(@Param("tenant") Long tenant,@Param("id") Long id,@Param("generation") Long generation,@Param("claim") Long claim,@Param("master") Long master);
    @Select("SELECT e.source_original_file_name FROM dcc_legacy_source_name_evidence e JOIN dcc_controlled_file_name_claim c ON c.tenant_id=e.tenant_id AND c.id=e.legacy_claim_id WHERE e.tenant_id=#{tenant} AND e.legacy_master_id=#{master} AND e.controlled_file_id=#{file} AND e.released_time IS NULL AND ("+("EXISTS(SELECT 1 FROM dcc_legacy_source_name_scope s WHERE s.id=e.verification_scope_id AND " + DccLegacySourceNameSql.VALID_SCOPE + ")")+")")
    List<String> verifiedNames(@Param("tenant") Long tenant,@Param("master") Long master,@Param("file") Long file);
    @Select("SELECT COUNT(*) FROM dcc_legacy_source_name_evidence e JOIN dcc_controlled_file_name_claim c ON c.tenant_id=e.tenant_id AND c.id=e.legacy_claim_id WHERE e.tenant_id=#{tenant} AND e.legacy_master_id=#{master} AND e.source_name_key=#{key} AND e.released_time IS NULL AND ("+("EXISTS(SELECT 1 FROM dcc_legacy_source_name_scope s WHERE s.id=e.verification_scope_id AND " + DccLegacySourceNameSql.VALID_SCOPE + ")")+")")
    long verifiedOwnerName(@Param("tenant") Long tenant,@Param("master") Long master,@Param("key") byte[] key);
    @Select("SELECT COUNT(*) FROM dcc_controlled_file_source_ownership o JOIN dcc_controlled_file f ON f.tenant_id=o.tenant_id AND f.id=o.controlled_file_id WHERE o.tenant_id=#{tenant} AND o.controlled_file_id=#{file} AND o.deleted=0 AND f.deleted=0 AND f.source_original_file_name IS NOT NULL AND o.source_file_id=f.source_file_id AND o.source_sha256=f.source_sha256")
    long currentVersionOwnsSource(@Param("tenant") Long tenant,@Param("file") Long file);
    @Select("SELECT COUNT(*) FROM dcc_controlled_file_name_claim WHERE tenant_id=#{tenant} AND master_id=#{master} AND source_original_file_name IS NULL AND deleted=0")
    long rawLegacyClaimCount(@Param("tenant") Long tenant,@Param("master") Long master);
    @Select("SELECT COUNT(*) FROM dcc_legacy_source_name_evidence e JOIN dcc_controlled_file_name_claim c ON c.tenant_id=e.tenant_id AND c.id=e.legacy_claim_id WHERE e.tenant_id=#{tenant} AND e.master_project_id=#{project} AND e.master_leaf_id=#{leaf} AND HEX(e.master_number)=#{numberHex} AND e.legacy_master_id<>#{master} AND e.released_time IS NULL AND ("+("EXISTS(SELECT 1 FROM dcc_legacy_source_name_scope s WHERE s.id=e.verification_scope_id AND " + DccLegacySourceNameSql.VALID_SCOPE + ")")+")")
    long activeLegacyNumberOwners(@Param("tenant") Long tenant,@Param("project") Long project,@Param("leaf") Long leaf,@Param("numberHex") String numberHex,@Param("master") Long master);
    @Select("SELECT r.* FROM dcc_source_name_reservation r WHERE r.tenant_id=#{tenant} AND EXISTS(SELECT 1 FROM dcc_legacy_source_name_evidence e JOIN dcc_controlled_file_name_claim c ON c.tenant_id=e.tenant_id AND c.id=e.legacy_claim_id WHERE e.tenant_id=r.tenant_id AND e.source_name_key=r.source_name_key AND e.verification_scope_id=r.verification_scope_id AND e.legacy_master_id=#{master} AND e.released_time IS NULL AND ("+("EXISTS(SELECT 1 FROM dcc_legacy_source_name_scope s WHERE s.id=e.verification_scope_id AND " + DccLegacySourceNameSql.VALID_SCOPE + ")")+")) ORDER BY r.source_name_key FOR UPDATE")
    List<DccSourceNameReservationDO> lockLegacyNames(@Param("tenant") Long tenant,@Param("master") Long master);
    @Select("SELECT COUNT(*) FROM dcc_legacy_source_name_evidence WHERE tenant_id=#{tenant} AND verification_scope_id=#{scope} AND legacy_master_id=#{master} AND released_time IS NULL")
    long activeLegacyEvidenceCount(@Param("tenant") Long tenant,@Param("scope") Long scope,@Param("master") Long master);
    @Update("UPDATE dcc_legacy_source_name_evidence SET obsolete_time=CASE WHEN obsolete_time IS NULL OR obsolete_time<#{obsolete} THEN #{obsolete} ELSE obsolete_time END,retain_until=CASE WHEN retain_until IS NULL OR retain_until<#{until} THEN #{until} ELSE retain_until END WHERE tenant_id=#{tenant} AND verification_scope_id=#{scope} AND legacy_master_id=#{master} AND released_time IS NULL")
    int retainLegacy(@Param("tenant") Long tenant,@Param("scope") Long scope,@Param("master") Long master,@Param("obsolete") LocalDateTime obsolete,@Param("until") LocalDateTime until);
    @Select("SELECT COUNT(*) FROM dcc_legacy_source_name_evidence WHERE tenant_id=#{tenant} AND verification_scope_id=#{scope} AND legacy_master_id=#{master} AND released_time IS NULL AND (obsolete_time IS NULL OR retain_until IS NULL OR retain_until>#{now})")
    long unexpiredLegacy(@Param("tenant") Long tenant,@Param("scope") Long scope,@Param("master") Long master,@Param("now") LocalDateTime now);
    @Update("UPDATE dcc_legacy_source_name_evidence SET released_time=#{now} WHERE tenant_id=#{tenant} AND verification_scope_id=#{scope} AND legacy_master_id=#{master} AND released_time IS NULL AND obsolete_time IS NOT NULL AND retain_until<=#{now}")
    int releaseLegacyOwner(@Param("tenant") Long tenant,@Param("scope") Long scope,@Param("master") Long master,@Param("now") LocalDateTime now);
    @Update("UPDATE dcc_source_name_reservation r SET reservation_kind='NONE',active=0,released_time=#{now},update_time=CURRENT_TIMESTAMP(6) WHERE r.tenant_id=#{tenant} AND r.id=#{id} AND r.reservation_kind='LEGACY_GROUP' AND NOT EXISTS(SELECT 1 FROM dcc_legacy_source_name_evidence e WHERE e.tenant_id=r.tenant_id AND e.source_name_key=r.source_name_key AND e.verification_scope_id=r.verification_scope_id AND e.released_time IS NULL)")
    int releaseEmptyGroup(@Param("tenant") Long tenant,@Param("id") Long id,@Param("now") LocalDateTime now);
    @Update("UPDATE dcc_source_name_reservation SET reservation_kind='NONE',active=0,released_time=#{now},update_time=CURRENT_TIMESTAMP(6) WHERE tenant_id=#{tenant} AND modern_master_id=#{master} AND reservation_kind='MODERN' AND active=1")
    int releaseModern(@Param("tenant") Long tenant,@Param("master") Long master,@Param("now") LocalDateTime now);
}
