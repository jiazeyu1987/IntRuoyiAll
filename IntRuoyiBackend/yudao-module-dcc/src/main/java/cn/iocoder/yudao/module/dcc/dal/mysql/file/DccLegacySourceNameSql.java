package cn.iocoder.yudao.module.dcc.dal.mysql.file;

/** Registered evidence is immutable configuration; every live source identity is rechecked. */
public final class DccLegacySourceNameSql {
    private DccLegacySourceNameSql() { }
    public static final String VALID_SCOPE = """
        s.tenant_id=c.tenant_id AND s.status='VERIFIED' AND s.activated_at IS NOT NULL
        AND s.claim_count>0 AND s.version_count>0 AND s.source_count>0 AND s.edge_count>0 AND s.name_count>0
        AND LENGTH(s.manifest_sha256)=64 AND LENGTH(s.facts_sha256)=64 AND LENGTH(s.bytes_receipt_sha256)=64
        AND LENGTH(s.user_decision_sha256)=64 AND LENGTH(s.scope_identity_sha256)=64
        AND s.manifest_sha256 NOT REGEXP '[^0-9a-f]' AND s.facts_sha256 NOT REGEXP '[^0-9a-f]'
        AND s.bytes_receipt_sha256 NOT REGEXP '[^0-9a-f]' AND s.user_decision_sha256 NOT REGEXP '[^0-9a-f]'
        AND s.scope_identity_sha256 NOT REGEXP '[^0-9a-f]'
        AND (SELECT COUNT(*) FROM dcc_legacy_source_name_evidence x WHERE x.tenant_id=s.tenant_id AND x.verification_scope_id=s.id)=s.version_count
        AND (SELECT COUNT(DISTINCT x.legacy_claim_id) FROM dcc_legacy_source_name_evidence x WHERE x.tenant_id=s.tenant_id AND x.verification_scope_id=s.id)=s.claim_count
        AND (SELECT COUNT(DISTINCT x.source_file_id) FROM dcc_legacy_source_name_evidence x WHERE x.tenant_id=s.tenant_id AND x.verification_scope_id=s.id)=s.source_count
        AND (SELECT COUNT(*) FROM (SELECT DISTINCT tenant_id,verification_scope_id,source_name_key,legacy_master_id FROM dcc_legacy_source_name_evidence) x WHERE x.tenant_id=s.tenant_id AND x.verification_scope_id=s.id)=s.edge_count
        AND (SELECT COUNT(DISTINCT x.source_name_key) FROM dcc_legacy_source_name_evidence x WHERE x.tenant_id=s.tenant_id AND x.verification_scope_id=s.id)=s.name_count
        AND NOT EXISTS (
          SELECT 1 FROM dcc_legacy_source_name_evidence e
          LEFT JOIN dcc_controlled_file_name_claim lc ON lc.id=e.legacy_claim_id AND lc.tenant_id=e.tenant_id
          LEFT JOIN dcc_controlled_file_master m ON m.id=e.legacy_master_id AND m.tenant_id=e.tenant_id
          LEFT JOIN dcc_controlled_file f ON f.id=e.controlled_file_id AND f.tenant_id=e.tenant_id
          LEFT JOIN infra_file i ON i.id=e.source_file_id
          LEFT JOIN infra_file_config fc ON fc.id=e.config_id
          LEFT JOIN dcc_source_name_reservation r ON r.tenant_id=e.tenant_id AND r.source_name_key=e.source_name_key
          WHERE e.tenant_id=s.tenant_id AND e.verification_scope_id=s.id AND (
            lc.id IS NULL OR lc.deleted<>0 OR lc.source_original_file_name IS NOT NULL OR lc.master_id<>e.legacy_master_id
            OR HEX(lc.normalized_name)<>HEX(e.claim_normalized_name)
            OR ((lc.dcc_project_code_id IS NULL AND e.claim_project_id IS NOT NULL) OR (lc.dcc_project_code_id IS NOT NULL AND e.claim_project_id IS NULL) OR (lc.dcc_project_code_id IS NOT NULL AND e.claim_project_id IS NOT NULL AND lc.dcc_project_code_id<>e.claim_project_id)) OR ((lc.file_type_taxonomy_leaf_id IS NULL AND e.claim_leaf_id IS NOT NULL) OR (lc.file_type_taxonomy_leaf_id IS NOT NULL AND e.claim_leaf_id IS NULL) OR (lc.file_type_taxonomy_leaf_id IS NOT NULL AND e.claim_leaf_id IS NOT NULL AND lc.file_type_taxonomy_leaf_id<>e.claim_leaf_id))
            OR ((lc.normalized_file_number IS NULL AND e.claim_number IS NOT NULL) OR (lc.normalized_file_number IS NOT NULL AND e.claim_number IS NULL) OR (lc.normalized_file_number IS NOT NULL AND e.claim_number IS NOT NULL AND HEX(lc.normalized_file_number)<>HEX(e.claim_number)))
            OR m.id IS NULL OR m.deleted<>0 OR ((m.dcc_project_code_id IS NULL AND e.master_project_id IS NOT NULL) OR (m.dcc_project_code_id IS NOT NULL AND e.master_project_id IS NULL) OR (m.dcc_project_code_id IS NOT NULL AND e.master_project_id IS NOT NULL AND m.dcc_project_code_id<>e.master_project_id))
            OR ((m.file_type_taxonomy_leaf_id IS NULL AND e.master_leaf_id IS NOT NULL) OR (m.file_type_taxonomy_leaf_id IS NOT NULL AND e.master_leaf_id IS NULL) OR (m.file_type_taxonomy_leaf_id IS NOT NULL AND e.master_leaf_id IS NOT NULL AND m.file_type_taxonomy_leaf_id<>e.master_leaf_id))
            OR ((m.normalized_file_number IS NULL AND e.master_number IS NOT NULL) OR (m.normalized_file_number IS NOT NULL AND e.master_number IS NULL) OR (m.normalized_file_number IS NOT NULL AND e.master_number IS NOT NULL AND HEX(m.normalized_file_number)<>HEX(e.master_number)))
            OR f.id IS NULL OR f.deleted<>0 OR f.master_id IS NULL OR f.source_file_id IS NULL OR f.version_no IS NULL
            OR f.master_id<>e.legacy_master_id OR f.source_file_id<>e.source_file_id
            OR HEX(f.version_no)<>HEX(e.version_no) OR f.source_sha256 IS NULL
            OR LOWER(f.source_sha256)<>e.expected_sha256
            OR (f.source_original_file_name IS NOT NULL AND HEX(f.source_original_file_name)<>HEX(e.source_original_file_name))
            OR i.id IS NULL OR i.deleted<>0 OR i.config_id IS NULL OR i.name IS NULL OR i.size IS NULL OR i.config_id<>e.config_id OR i.size<>e.expected_size
            OR fc.id IS NULL OR fc.deleted<>0 OR fc.storage IS NULL OR fc.storage<>e.storage_type OR fc.config IS NULL
            OR JSON_UNQUOTE(JSON_EXTRACT(fc.config,'$.endpoint')) IS NULL
            OR HEX(JSON_UNQUOTE(JSON_EXTRACT(fc.config,'$.endpoint')))<>HEX(e.storage_endpoint)
            OR JSON_UNQUOTE(JSON_EXTRACT(fc.config,'$.bucket')) IS NULL
            OR HEX(JSON_UNQUOTE(JSON_EXTRACT(fc.config,'$.bucket')))<>HEX(e.storage_bucket)
            OR ((JSON_UNQUOTE(JSON_EXTRACT(fc.config,'$.region')) IS NULL AND e.storage_region IS NOT NULL)
                OR (JSON_UNQUOTE(JSON_EXTRACT(fc.config,'$.region')) IS NOT NULL AND e.storage_region IS NULL)
                OR (JSON_UNQUOTE(JSON_EXTRACT(fc.config,'$.region')) IS NOT NULL AND e.storage_region IS NOT NULL AND HEX(JSON_UNQUOTE(JSON_EXTRACT(fc.config,'$.region')))<>HEX(e.storage_region)))
            OR JSON_UNQUOTE(JSON_EXTRACT(fc.config,'$.enablePathStyleAccess')) IS NULL
            OR JSON_UNQUOTE(JSON_EXTRACT(fc.config,'$.enablePathStyleAccess'))<>CASE WHEN e.storage_path_style=1 THEN 'true' ELSE 'false' END
            OR i.path IS NULL OR HEX(i.path)<>HEX(e.source_path)
            OR HEX(i.name)<>HEX(e.source_original_file_name) OR e.expected_size<0 OR e.expected_size<>e.actual_size
            OR e.bytes_status<>'MATCH' OR e.actual_sha256<>e.expected_sha256 OR LENGTH(e.expected_sha256)<>64
            OR e.expected_sha256 REGEXP '[^0-9a-f]' OR e.actual_sha256 REGEXP '[^0-9a-f]'
            OR LENGTH(e.metadata_identity_sha256)<>64 OR LENGTH(e.preimage_sha256)<>64 OR LENGTH(e.proof_row_sha256)<>64
            OR r.id IS NULL OR (e.released_time IS NULL AND (r.active<>1 OR r.reservation_kind<>'LEGACY_GROUP' OR r.verification_scope_id IS NULL OR r.verification_scope_id<>s.id))
            OR (e.released_time IS NOT NULL AND (e.obsolete_time IS NULL OR e.retain_until IS NULL OR e.released_time<e.retain_until))
          )
        )
        """;
    public static final String RESOLVED_CLAIM = """
        EXISTS (SELECT 1 FROM dcc_legacy_source_name_scope s WHERE
        """ + VALID_SCOPE + """
          AND EXISTS (SELECT 1 FROM dcc_legacy_source_name_evidence e WHERE e.tenant_id=c.tenant_id
            AND e.verification_scope_id=s.id AND e.legacy_claim_id=c.id AND e.legacy_master_id=c.master_id)
          AND NOT EXISTS (SELECT 1 FROM dcc_legacy_source_name_evidence e WHERE e.tenant_id=c.tenant_id
            AND e.verification_scope_id=s.id AND e.legacy_claim_id=c.id
            AND e.expected_version_count<>(SELECT COUNT(*) FROM dcc_legacy_source_name_evidence x
                WHERE x.tenant_id=c.tenant_id AND x.verification_scope_id=s.id AND x.legacy_claim_id=c.id)))
        """;
    public static final String COUNT_UNRESOLVED = "SELECT COUNT(*) FROM dcc_controlled_file_name_claim c WHERE c.tenant_id=#{tenantId} AND c.deleted=0 AND c.source_original_file_name IS NULL AND NOT (" + RESOLVED_CLAIM + ")";
}
