package cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql;

import cn.iocoder.yudao.module.dcc.registrationcertificate.service.query.DccRegistrationCertificatePageQuery;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.query.DccRegistrationCertificateQueryRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;

import java.util.List;

@Mapper
public interface DccRegistrationCertificateQueryMapper {

    @SelectProvider(type = SqlProvider.class, method = "countPage")
    long countPage(@Param("tenantId") Long tenantId,
                   @Param("companyIds") List<Long> companyIds,
                   @Param("query") DccRegistrationCertificatePageQuery query);

    @SelectProvider(type = SqlProvider.class, method = "selectPage")
    List<DccRegistrationCertificateQueryRecord> selectPage(
            @Param("tenantId") Long tenantId,
            @Param("companyIds") List<Long> companyIds,
            @Param("query") DccRegistrationCertificatePageQuery query,
            @Param("limit") int limit,
            @Param("offset") int offset);

    @SelectProvider(type = SqlProvider.class, method = "selectDetail")
    DccRegistrationCertificateQueryRecord selectDetail(
            @Param("tenantId") Long tenantId,
            @Param("companyIds") List<Long> companyIds,
            @Param("certificateId") Long certificateId,
            @Param("versionId") Long versionId);

    @SelectProvider(type = SqlProvider.class, method = "countOldIndex")
    long countOldIndex(@Param("tenantId") Long tenantId,
                       @Param("companyIds") List<Long> companyIds,
                       @Param("query") DccRegistrationCertificatePageQuery query);

    @SelectProvider(type = SqlProvider.class, method = "selectOldIndexPage")
    List<DccRegistrationCertificateQueryRecord> selectOldIndexPage(
            @Param("tenantId") Long tenantId,
            @Param("companyIds") List<Long> companyIds,
            @Param("query") DccRegistrationCertificatePageQuery query,
            @Param("limit") int limit,
            @Param("offset") int offset);

    final class SqlProvider {

        private SqlProvider() {
        }

        public static String countPage() {
            return script("SELECT COUNT(*) " + from(), currentWhere() + filters(), "");
        }

        public static String selectPage() {
            return script(select(), currentWhere() + filters(),
                    currentOrderBy()
                            + " LIMIT #{limit} OFFSET #{offset}");
        }

        public static String selectDetail() {
            String latestSnapshotId = latestSnapshotId();
            return script(select(), commonWhere()
                            + " AND c.id = #{certificateId}"
                            + " <choose>"
                            + " <when test=\"versionId != null\">"
                            + " AND v.id = #{versionId}"
                            + " AND ("
                            + " (v.id = c.current_version_id AND s.id = c.current_snapshot_id)"
                            + " OR (v.id != c.current_version_id AND s.id = " + latestSnapshotId + ")"
                            + " OR (c.current_version_id IS NULL AND s.id = " + latestSnapshotId + ")"
                            + " )"
                            + " </when>"
                            + " <otherwise>"
                            + " AND ("
                            + " (v.id = c.current_version_id AND s.id = c.current_snapshot_id)"
                            + " OR (v.id = c.pending_version_id AND s.id = " + latestSnapshotId + ")"
                            + " OR (c.current_version_id IS NULL AND c.pending_version_id IS NULL"
                            + " AND s.id = " + latestSnapshotId + ")"
                            + " )"
                            + " </otherwise>"
                            + " </choose>",
                    " ORDER BY CASE v.status"
                            + " WHEN 'PENDING_EFFECTIVE' THEN 1"
                            + " WHEN 'CURRENT' THEN 2"
                            + " WHEN 'OLD' THEN 3"
                            + " ELSE 4 END, v.version_no DESC LIMIT 1");
        }

        public static String countOldIndex() {
            return script("SELECT COUNT(*) " + from(),
                    commonWhere() + " AND v.status = 'OLD' AND s.id = " + latestSnapshotId() + oldIndexFilters(), "");
        }

        public static String selectOldIndexPage() {
            return script(select(), commonWhere() + " AND v.status = 'OLD' AND s.id = " + latestSnapshotId() + oldIndexFilters(),
                    oldIndexOrderBy()
                            + " LIMIT #{limit} OFFSET #{offset}");
        }

        private static final String SORT_ORDER_ASC = "asc";
        private static final String SORT_ORDER_DESC = "desc";
        private static final String SORT_FIELD_CERTIFICATE_NO = "certificateNo";
        private static final String SORT_FIELD_OWNER_COMPANY_NAME = "ownerCompanyName";
        private static final String SORT_FIELD_PRODUCT_NAME = "productName";
        private static final String SORT_FIELD_CLASSIFICATION = "classification";
        private static final String SORT_FIELD_PROJECT_CODE = "projectCode";
        private static final String SORT_FIELD_VERSION_NO = "versionNo";
        private static final String SORT_FIELD_STATUS = "status";
        private static final String SORT_FIELD_HAS_PROJECT_CODE = "hasProjectCode";
        private static final String SORT_FIELD_HAS_REGISTRATION_FILE = "hasRegistrationFile";
        private static final String SORT_FIELD_APPROVAL_DATE = "approvalDate";
        private static final String SORT_FIELD_EFFECTIVE_DATE = "effectiveDate";
        private static final String SORT_FIELD_EXPIRY_DATE = "expiryDate";
        private static final String SORT_FIELD_REMINDER = "reminder";
        private static final String SORT_FIELD_REMARK = "remark";
        private static final String NO_EMPTY_RANK = "CASE WHEN 1 = 1 THEN 0 ELSE 0 END";
        private static final String OWNER_COMPANY_NAME_SORT_EXPRESSION =
                "(SELECT MIN(e.name) FROM mdm_enterprise e"
                        + " WHERE e.tenant_id = c.tenant_id"
                        + " AND e.id = c.owner_company_id"
                        + " AND e.deleted = 0"
                        + " AND e.type = 'OWNED_COMPANY')";
        private static final String PROJECT_CODE_EXISTS_SORT_EXPRESSION =
                "CASE WHEN c.project_code_id IS NULL THEN 0 ELSE 1 END";
        private static final String REGISTRATION_FILE_EXISTS_SORT_EXPRESSION =
                "CASE WHEN EXISTS ("
                        + "SELECT 1 FROM dcc_registration_certificate_file f"
                        + " WHERE f.tenant_id = c.tenant_id"
                        + " AND f.owner_type = 'VERSION'"
                        + " AND f.owner_id = v.id"
                        + " AND f.file_kind = 'REGISTRATION_CERTIFICATE'"
                        + " AND f.status = 'BOUND'"
                        + " AND f.deleted = 0"
                        + ") THEN 1 ELSE 0 END";
        private static final String REMINDER_CLEARED_EXISTS_EXPRESSION =
                "EXISTS (SELECT 1 FROM dcc_registration_certificate_supporting_document sd"
                        + " WHERE sd.tenant_id = c.tenant_id"
                        + " AND sd.certificate_id = c.id"
                        + " AND sd.document_type IN ('RENEWAL_ACCEPTANCE_RECEIPT', 'RENEWAL_SUPPLEMENT_NOTICE')"
                        + " AND sd.status = 'EFFECTIVE'"
                        + " AND sd.deleted = 0)";
        private static final String REMINDER_STATE_EXPRESSION = reminderStateExpression();
        private static final String REMINDER_PRIORITY_SORT_EXPRESSION = reminderPrioritySortExpression();

        private static String currentOrderBy() {
            String stableOrder = ", c.owner_company_id ASC, v.expiry_date ASC, c.id ASC, v.version_no ASC";
            return "<choose>"
                    + sortWhen(SORT_FIELD_CERTIFICATE_NO, SORT_ORDER_ASC, "v.certificate_no",
                    textBlankLast("v.certificate_no"), stableOrder)
                    + sortWhen(SORT_FIELD_CERTIFICATE_NO, SORT_ORDER_DESC, "v.certificate_no",
                    textBlankLast("v.certificate_no"), stableOrder)
                    + sortWhen(SORT_FIELD_OWNER_COMPANY_NAME, SORT_ORDER_ASC, OWNER_COMPANY_NAME_SORT_EXPRESSION,
                    textBlankLast(OWNER_COMPANY_NAME_SORT_EXPRESSION), stableOrder)
                    + sortWhen(SORT_FIELD_OWNER_COMPANY_NAME, SORT_ORDER_DESC, OWNER_COMPANY_NAME_SORT_EXPRESSION,
                    textBlankLast(OWNER_COMPANY_NAME_SORT_EXPRESSION), stableOrder)
                    + sortWhen(SORT_FIELD_PRODUCT_NAME, SORT_ORDER_ASC, "s.product_name",
                    textBlankLast("s.product_name"), stableOrder)
                    + sortWhen(SORT_FIELD_PRODUCT_NAME, SORT_ORDER_DESC, "s.product_name",
                    textBlankLast("s.product_name"), stableOrder)
                    + sortWhen(SORT_FIELD_CLASSIFICATION, SORT_ORDER_ASC, "v.classification",
                    textBlankLast("v.classification"), stableOrder)
                    + sortWhen(SORT_FIELD_CLASSIFICATION, SORT_ORDER_DESC, "v.classification",
                    textBlankLast("v.classification"), stableOrder)
                    + sortWhen(SORT_FIELD_PROJECT_CODE, SORT_ORDER_ASC, "pc.project_code",
                    textBlankLast("pc.project_code"), stableOrder)
                    + sortWhen(SORT_FIELD_PROJECT_CODE, SORT_ORDER_DESC, "pc.project_code",
                    textBlankLast("pc.project_code"), stableOrder)
                    + sortWhen(SORT_FIELD_VERSION_NO, SORT_ORDER_ASC, "v.version_no",
                    nullLast("v.version_no"), stableOrder)
                    + sortWhen(SORT_FIELD_VERSION_NO, SORT_ORDER_DESC, "v.version_no",
                    nullLast("v.version_no"), stableOrder)
                    + sortWhen(SORT_FIELD_STATUS, SORT_ORDER_ASC, "v.status",
                    textBlankLast("v.status"), stableOrder)
                    + sortWhen(SORT_FIELD_STATUS, SORT_ORDER_DESC, "v.status",
                    textBlankLast("v.status"), stableOrder)
                    + sortWhen(SORT_FIELD_HAS_PROJECT_CODE, SORT_ORDER_ASC, PROJECT_CODE_EXISTS_SORT_EXPRESSION,
                    nullLast(PROJECT_CODE_EXISTS_SORT_EXPRESSION), stableOrder)
                    + sortWhen(SORT_FIELD_HAS_PROJECT_CODE, SORT_ORDER_DESC, PROJECT_CODE_EXISTS_SORT_EXPRESSION,
                    nullLast(PROJECT_CODE_EXISTS_SORT_EXPRESSION), stableOrder)
                    + sortWhen(SORT_FIELD_HAS_REGISTRATION_FILE, SORT_ORDER_ASC,
                    REGISTRATION_FILE_EXISTS_SORT_EXPRESSION,
                    nullLast(REGISTRATION_FILE_EXISTS_SORT_EXPRESSION), stableOrder)
                    + sortWhen(SORT_FIELD_HAS_REGISTRATION_FILE, SORT_ORDER_DESC,
                    REGISTRATION_FILE_EXISTS_SORT_EXPRESSION,
                    nullLast(REGISTRATION_FILE_EXISTS_SORT_EXPRESSION), stableOrder)
                    + sortWhen(SORT_FIELD_APPROVAL_DATE, SORT_ORDER_ASC, "v.approval_date",
                    nullLast("v.approval_date"), stableOrder)
                    + sortWhen(SORT_FIELD_APPROVAL_DATE, SORT_ORDER_DESC, "v.approval_date",
                    nullLast("v.approval_date"), stableOrder)
                    + sortWhen(SORT_FIELD_EFFECTIVE_DATE, SORT_ORDER_ASC, "v.effective_date",
                    nullLast("v.effective_date"), stableOrder)
                    + sortWhen(SORT_FIELD_EFFECTIVE_DATE, SORT_ORDER_DESC, "v.effective_date",
                    nullLast("v.effective_date"), stableOrder)
                    + sortWhen(SORT_FIELD_EXPIRY_DATE, SORT_ORDER_ASC, "v.expiry_date",
                    nullLast("v.expiry_date"), stableOrder)
                    + sortWhen(SORT_FIELD_EXPIRY_DATE, SORT_ORDER_DESC, "v.expiry_date",
                    nullLast("v.expiry_date"), stableOrder)
                    + sortWhen(SORT_FIELD_REMINDER, SORT_ORDER_ASC, REMINDER_PRIORITY_SORT_EXPRESSION,
                    NO_EMPTY_RANK, stableOrder)
                    + sortWhen(SORT_FIELD_REMINDER, SORT_ORDER_DESC, REMINDER_PRIORITY_SORT_EXPRESSION,
                    NO_EMPTY_RANK, stableOrder)
                    + sortWhen(SORT_FIELD_REMARK, SORT_ORDER_ASC, "v.remark",
                    textBlankLast("v.remark"), stableOrder)
                    + sortWhen(SORT_FIELD_REMARK, SORT_ORDER_DESC, "v.remark",
                    textBlankLast("v.remark"), stableOrder)
                    + "<otherwise> ORDER BY c.owner_company_id ASC, v.expiry_date ASC, c.id ASC, v.version_no ASC</otherwise>"
                    + "</choose>";
        }

        private static String oldIndexOrderBy() {
            String stableOrder = ", v.expiry_date DESC, c.id ASC, v.version_no DESC";
            return "<choose>"
                    + sortWhen(SORT_FIELD_CERTIFICATE_NO, SORT_ORDER_ASC, "v.certificate_no",
                    textBlankLast("v.certificate_no"), stableOrder)
                    + sortWhen(SORT_FIELD_CERTIFICATE_NO, SORT_ORDER_DESC, "v.certificate_no",
                    textBlankLast("v.certificate_no"), stableOrder)
                    + sortWhen(SORT_FIELD_OWNER_COMPANY_NAME, SORT_ORDER_ASC, OWNER_COMPANY_NAME_SORT_EXPRESSION,
                    textBlankLast(OWNER_COMPANY_NAME_SORT_EXPRESSION), stableOrder)
                    + sortWhen(SORT_FIELD_OWNER_COMPANY_NAME, SORT_ORDER_DESC, OWNER_COMPANY_NAME_SORT_EXPRESSION,
                    textBlankLast(OWNER_COMPANY_NAME_SORT_EXPRESSION), stableOrder)
                    + sortWhen(SORT_FIELD_PRODUCT_NAME, SORT_ORDER_ASC, "s.product_name",
                    textBlankLast("s.product_name"), stableOrder)
                    + sortWhen(SORT_FIELD_PRODUCT_NAME, SORT_ORDER_DESC, "s.product_name",
                    textBlankLast("s.product_name"), stableOrder)
                    + sortWhen(SORT_FIELD_CLASSIFICATION, SORT_ORDER_ASC, "v.classification",
                    textBlankLast("v.classification"), stableOrder)
                    + sortWhen(SORT_FIELD_CLASSIFICATION, SORT_ORDER_DESC, "v.classification",
                    textBlankLast("v.classification"), stableOrder)
                    + sortWhen(SORT_FIELD_VERSION_NO, SORT_ORDER_ASC, "v.version_no",
                    nullLast("v.version_no"), stableOrder)
                    + sortWhen(SORT_FIELD_VERSION_NO, SORT_ORDER_DESC, "v.version_no",
                    nullLast("v.version_no"), stableOrder)
                    + sortWhen(SORT_FIELD_STATUS, SORT_ORDER_ASC, "v.status",
                    textBlankLast("v.status"), stableOrder)
                    + sortWhen(SORT_FIELD_STATUS, SORT_ORDER_DESC, "v.status",
                    textBlankLast("v.status"), stableOrder)
                    + sortWhen(SORT_FIELD_EXPIRY_DATE, SORT_ORDER_ASC, "v.expiry_date",
                    nullLast("v.expiry_date"), stableOrder)
                    + sortWhen(SORT_FIELD_EXPIRY_DATE, SORT_ORDER_DESC, "v.expiry_date",
                    nullLast("v.expiry_date"), stableOrder)
                    + "<otherwise> ORDER BY v.expiry_date DESC, c.id ASC, v.version_no DESC</otherwise>"
                    + "</choose>";
        }

        private static String sortWhen(
                String field, String order, String expression, String emptyRankExpression, String stableOrder) {
            String direction = SORT_ORDER_ASC.equals(order) ? "ASC" : "DESC";
            return "<when test=\"query != null and query.sortField == '" + field
                    + "' and query.sortOrder == '" + order + "'\">"
                    + " ORDER BY " + emptyRankExpression + " ASC, " + expression + " " + direction + stableOrder
                    + "</when>";
        }

        private static String textBlankLast(String expression) {
            return "CASE WHEN " + expression + " IS NULL OR TRIM(" + expression + ") = '' THEN 1 ELSE 0 END";
        }

        private static String nullLast(String expression) {
            return "CASE WHEN " + expression + " IS NULL THEN 1 ELSE 0 END";
        }

        private static String reminderStateExpression() {
            return "CASE"
                    + " WHEN " + reminderDateCrossed(1) + " THEN 'T_1'"
                    + " WHEN " + reminderDateCrossed(2) + " THEN 'T_2'"
                    + " WHEN " + reminderDateCrossed(8) + " AND " + REMINDER_CLEARED_EXISTS_EXPRESSION
                    + " THEN 'CLEARED'"
                    + " WHEN " + reminderDateCrossed(8) + " THEN 'T_8'"
                    + " WHEN " + reminderDateCrossed(30) + " THEN 'T_30'"
                    + " ELSE 'NONE' END";
        }

        private static String reminderPrioritySortExpression() {
            return "CASE"
                    + " WHEN " + reminderDateCrossed(1) + " THEN 4"
                    + " WHEN " + reminderDateCrossed(2) + " THEN 3"
                    + " WHEN " + reminderDateCrossed(8) + " AND " + REMINDER_CLEARED_EXISTS_EXPRESSION
                    + " THEN 0"
                    + " WHEN " + reminderDateCrossed(8) + " THEN 2"
                    + " WHEN " + reminderDateCrossed(30) + " THEN 1"
                    + " ELSE 0 END";
        }

        private static String reminderDateCrossed(int months) {
            return "#{query.businessDate} >= TIMESTAMPADD(MONTH, -" + months + ", v.expiry_date)";
        }

        private static String script(String select, String where, String suffix) {
            return "<script>" + select + where + suffix + "</script>";
        }

        private static String select() {
            return """
                    SELECT c.id AS certificate_id,
                           c.row_version,
                           v.id AS version_id,
                           s.id AS snapshot_id,
                           s.revision_no AS snapshot_revision,
                           c.owner_company_id,
                           c.product_master_id,
                           c.project_code_id,
                           pc.project_code,
                           CASE
                             WHEN c.pending_version_id IS NOT NULL
                               OR EXISTS (
                                 SELECT 1
                                   FROM dcc_registration_certificate_access_request r
                                  WHERE r.tenant_id = c.tenant_id
                                    AND r.certificate_id = c.id
                                    AND r.request_type = 'UPLOAD_CERTIFICATE'
                                    AND r.status IN ('SUBMITTED', 'BPM_BOUND')
                                    AND r.deleted = 0
                               )
                             THEN TRUE ELSE FALSE
                           END AS has_pending_renewal,
                           c.first_obtained_date,
                           s.product_name,
                           v.certificate_no,
                           v.version_no,
                           v.status,
                           v.approval_date,
                           v.effective_date,
                           v.expiry_date,
                           v.classification,
                           v.remark,
                           CASE WHEN pending_change.certificate_id IS NULL THEN FALSE ELSE TRUE END AS has_pending_change,
                           s.registrant_name,
                           s.model_specification,
                           s.structure_composition,
                           s.intended_use,
                           s.technical_requirements,
                           s.residence_address,
                           s.production_address,
                           s.entrusted_production,
                           s.self_production,
                           s.entrusted_enterprises_json,
                           (SELECT f.id
                              FROM dcc_registration_certificate_file f
                             WHERE f.tenant_id = c.tenant_id
                               AND f.owner_type = 'VERSION'
                               AND f.owner_id = v.id
                               AND f.file_kind = 'REGISTRATION_CERTIFICATE'
                               AND f.status = 'BOUND'
                               AND f.deleted = 0
                             ORDER BY f.id ASC
                             LIMIT 1) AS registration_file_id,
                           (SELECT f.original_name
                              FROM dcc_registration_certificate_file f
                             WHERE f.tenant_id = c.tenant_id
                               AND f.owner_type = 'VERSION'
                               AND f.owner_id = v.id
                               AND f.file_kind = 'REGISTRATION_CERTIFICATE'
                               AND f.status = 'BOUND'
                               AND f.deleted = 0
                             ORDER BY f.id ASC
                             LIMIT 1) AS registration_file_name
                    """ + from();
        }

        private static String from() {
            return """
                      FROM dcc_registration_certificate c
                      JOIN dcc_registration_certificate_version v
                        ON v.tenant_id = c.tenant_id
                       AND v.certificate_id = c.id
                       AND v.deleted = 0
                       AND v.status != 'DRAFT'
                      JOIN dcc_registration_certificate_snapshot s
                        ON s.tenant_id = c.tenant_id
                       AND s.version_id = v.id
                       AND s.deleted = 0
                      LEFT JOIN dcc_project_code pc
                        ON pc.tenant_id = c.tenant_id
                       AND pc.id = c.project_code_id
                       AND pc.deleted = 0
                      LEFT JOIN (
                        SELECT tenant_id, certificate_id
                          FROM dcc_registration_certificate_change
                         WHERE status = 'PENDING_APPROVAL'
                           AND deleted = 0
                         GROUP BY tenant_id, certificate_id
                      ) pending_change
                        ON pending_change.tenant_id = c.tenant_id
                       AND pending_change.certificate_id = c.id
                    """;
        }

        private static String commonWhere() {
            return """
                     WHERE c.tenant_id = #{tenantId}
                       AND c.deleted = 0
                       AND c.owner_company_id IN
                       <foreach collection="companyIds" item="companyId" open="(" separator="," close=")">
                         #{companyId}
                       </foreach>
                    """;
        }

        private static String currentWhere() {
            return commonWhere()
                    + " AND c.status IN ('ACTIVE', 'PENDING_FIRST_EFFECTIVE')"
                    + " AND v.id = COALESCE(c.pending_version_id, c.current_version_id)"
                    + " AND v.status IN ('CURRENT', 'PENDING_EFFECTIVE')"
                    + " AND ("
                    + " (v.id = c.current_version_id AND s.id = c.current_snapshot_id)"
                    + " OR (v.id = c.pending_version_id AND s.id = " + latestSnapshotId() + ")"
                    + " )";
        }

        private static String latestSnapshotId() {
            return "(SELECT s2.id FROM dcc_registration_certificate_snapshot s2"
                    + " WHERE s2.tenant_id = c.tenant_id"
                    + " AND s2.version_id = v.id"
                    + " AND s2.deleted = 0"
                    + " ORDER BY s2.revision_no DESC, s2.id DESC LIMIT 1)";
        }

        private static String filters() {
            return """
                    <if test="query != null">
                      <if test="query.ownerCompanyId != null">
                        AND c.owner_company_id = #{query.ownerCompanyId}
                      </if>
                      <if test="query.productMasterId != null">
                        AND c.product_master_id = #{query.productMasterId}
                      </if>
                      <if test="query.projectCodeId != null">
                        AND c.project_code_id = #{query.projectCodeId}
                      </if>
                      <if test="query.status != null and query.status != ''">
                        AND v.status = #{query.status}
                      </if>
                      <if test="query.reminderState != null and query.reminderState == 'NORMAL'">
                        AND
                    """ + REMINDER_STATE_EXPRESSION + """
                        IN ('NONE', 'CLEARED')
                      </if>
                      <if test="query.reminderState != null and query.reminderState != 'NORMAL'">
                        AND
                    """ + REMINDER_STATE_EXPRESSION + """
                        = #{query.reminderState}
                      </if>
                      <if test="query.certificateNo != null and query.certificateNo != ''">
                        AND v.certificate_no LIKE CONCAT('%', #{query.certificateNo}, '%')
                      </if>
                      <if test="query.ownerCompanyName != null and query.ownerCompanyName != ''">
                        AND EXISTS (SELECT 1 FROM mdm_enterprise e WHERE e.tenant_id = c.tenant_id AND e.id = c.owner_company_id AND e.deleted = 0 AND e.type = 'OWNED_COMPANY' AND e.name LIKE CONCAT('%', #{query.ownerCompanyName}, '%'))
                      </if>
                      <if test="query.productName != null and query.productName != ''">
                        AND s.product_name LIKE CONCAT('%', #{query.productName}, '%')
                      </if>
                      <if test="query.classification != null and query.classification != ''">
                        AND v.classification LIKE CONCAT('%', #{query.classification}, '%')
                      </if>
                      <if test="query.registrantName != null and query.registrantName != ''">
                        AND s.registrant_name LIKE CONCAT('%', #{query.registrantName}, '%')
                      </if>
                      <if test="query.modelSpecification != null and query.modelSpecification != ''">
                        AND s.model_specification LIKE CONCAT('%', #{query.modelSpecification}, '%')
                      </if>
                      <if test="query.productionAddress != null and query.productionAddress != ''">
                        AND s.production_address LIKE CONCAT('%', #{query.productionAddress}, '%')
                      </if>
                      <if test="query.entrustedEnterpriseName != null and query.entrustedEnterpriseName != ''">
                        AND EXISTS (SELECT 1 FROM dcc_registration_certificate_snapshot_entrusted se WHERE se.tenant_id = c.tenant_id AND se.snapshot_id = s.id AND se.deleted = 0 AND se.enterprise_name_snapshot LIKE CONCAT('%', #{query.entrustedEnterpriseName}, '%'))
                      </if>
                      <if test="query.projectCode != null and query.projectCode != ''">
                        AND pc.project_code LIKE CONCAT('%', #{query.projectCode}, '%')
                      </if>
                      <if test="query.missingProjectCode != null and query.missingProjectCode">
                        AND c.project_code_id IS NULL
                      </if>
                      <if test="query.missingProjectCode != null and !query.missingProjectCode">
                        AND c.project_code_id IS NOT NULL
                      </if>
                      <if test="query.missingFile != null and query.missingFile">
                        AND NOT EXISTS (
                          SELECT 1 FROM dcc_registration_certificate_file f
                           WHERE f.tenant_id = c.tenant_id
                             AND f.owner_type = 'VERSION'
                             AND f.owner_id = v.id
                             AND f.file_kind = 'REGISTRATION_CERTIFICATE'
                             AND f.status = 'BOUND'
                             AND f.deleted = 0)
                      </if>
                      <if test="query.missingFile != null and !query.missingFile">
                        AND EXISTS (
                          SELECT 1 FROM dcc_registration_certificate_file f
                           WHERE f.tenant_id = c.tenant_id
                             AND f.owner_type = 'VERSION'
                             AND f.owner_id = v.id
                             AND f.file_kind = 'REGISTRATION_CERTIFICATE'
                             AND f.status = 'BOUND'
                             AND f.deleted = 0)
                      </if>
                      <if test="query.firstObtainedStart != null">
                        AND c.first_obtained_date &gt;= #{query.firstObtainedStart}
                      </if>
                      <if test="query.firstObtainedEnd != null">
                        AND c.first_obtained_date &lt;= #{query.firstObtainedEnd}
                      </if>
                      <if test="query.approvalStart != null">
                        AND v.approval_date &gt;= #{query.approvalStart}
                      </if>
                      <if test="query.approvalEnd != null">
                        AND v.approval_date &lt;= #{query.approvalEnd}
                      </if>
                      <if test="query.effectiveStart != null">
                        AND v.effective_date &gt;= #{query.effectiveStart}
                      </if>
                      <if test="query.effectiveEnd != null">
                        AND v.effective_date &lt;= #{query.effectiveEnd}
                      </if>
                      <if test="query.expiryStart != null">
                        AND v.expiry_date &gt;= #{query.expiryStart}
                      </if>
                      <if test="query.expiryEnd != null">
                        AND v.expiry_date &lt;= #{query.expiryEnd}
                      </if>
                    </if>
                    """;
        }

        private static String oldIndexFilters() {
            return """
                    <if test="query != null">
                      <if test="query.ownerCompanyId != null">
                        AND c.owner_company_id = #{query.ownerCompanyId}
                      </if>
                      <if test="query.productMasterId != null">
                        AND c.product_master_id = #{query.productMasterId}
                      </if>
                      <if test="query.projectCodeId != null">
                        AND c.project_code_id = #{query.projectCodeId}
                      </if>
                      <if test="query.certificateNo != null and query.certificateNo != ''">
                        AND v.certificate_no LIKE CONCAT('%', #{query.certificateNo}, '%')
                      </if>
                      <if test="query.ownerCompanyName != null and query.ownerCompanyName != ''">
                        AND EXISTS (SELECT 1 FROM mdm_enterprise e WHERE e.tenant_id = c.tenant_id AND e.id = c.owner_company_id AND e.deleted = 0 AND e.type = 'OWNED_COMPANY' AND e.name LIKE CONCAT('%', #{query.ownerCompanyName}, '%'))
                      </if>
                      <if test="query.productName != null and query.productName != ''">
                        AND s.product_name LIKE CONCAT('%', #{query.productName}, '%')
                      </if>
                      <if test="query.classification != null and query.classification != ''">
                        AND v.classification LIKE CONCAT('%', #{query.classification}, '%')
                      </if>
                      <if test="query.registrantName != null and query.registrantName != ''">
                        AND s.registrant_name LIKE CONCAT('%', #{query.registrantName}, '%')
                      </if>
                      <if test="query.modelSpecification != null and query.modelSpecification != ''">
                        AND s.model_specification LIKE CONCAT('%', #{query.modelSpecification}, '%')
                      </if>
                      <if test="query.productionAddress != null and query.productionAddress != ''">
                        AND s.production_address LIKE CONCAT('%', #{query.productionAddress}, '%')
                      </if>
                      <if test="query.entrustedEnterpriseName != null and query.entrustedEnterpriseName != ''">
                        AND EXISTS (SELECT 1 FROM dcc_registration_certificate_snapshot_entrusted se WHERE se.tenant_id = c.tenant_id AND se.snapshot_id = s.id AND se.deleted = 0 AND se.enterprise_name_snapshot LIKE CONCAT('%', #{query.entrustedEnterpriseName}, '%'))
                      </if>
                      <if test="query.projectCode != null and query.projectCode != ''">
                        AND pc.project_code LIKE CONCAT('%', #{query.projectCode}, '%')
                      </if>
                      <if test="query.firstObtainedStart != null">
                        AND c.first_obtained_date &gt;= #{query.firstObtainedStart}
                      </if>
                      <if test="query.firstObtainedEnd != null">
                        AND c.first_obtained_date &lt;= #{query.firstObtainedEnd}
                      </if>
                      <if test="query.approvalStart != null">
                        AND v.approval_date &gt;= #{query.approvalStart}
                      </if>
                      <if test="query.approvalEnd != null">
                        AND v.approval_date &lt;= #{query.approvalEnd}
                      </if>
                      <if test="query.effectiveStart != null">
                        AND v.effective_date &gt;= #{query.effectiveStart}
                      </if>
                      <if test="query.effectiveEnd != null">
                        AND v.effective_date &lt;= #{query.effectiveEnd}
                      </if>
                      <if test="query.expiryStart != null">
                        AND v.expiry_date &gt;= #{query.expiryStart}
                      </if>
                      <if test="query.expiryEnd != null">
                        AND v.expiry_date &lt;= #{query.expiryEnd}
                      </if>
                    </if>
                    """;
        }
    }
}
