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
            return pageScript("SELECT COUNT(*) " + from(), currentWhere() + filters(), "");
        }

        public static String selectPage() {
            return pageScript(select(), currentWhere() + filters(),
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
            return pageScript("SELECT COUNT(*) " + from(),
                    commonWhere() + " AND v.status = 'OLD' AND s.id = " + latestSnapshotId() + oldIndexFilters(), "");
        }

        public static String selectOldIndexPage() {
            return pageScript(select(), commonWhere() + " AND v.status = 'OLD' AND s.id = " + latestSnapshotId() + oldIndexFilters(),
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
                "CASE WHEN (" + "CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.ownProduct')) = 'false' THEN NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.projectCode')), 'null') ELSE pc.project_code END" + ") IS NULL THEN 0 ELSE 1 END";
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
            String stableOrder = ", c.owner_company_id ASC, CAST(CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.expiryDate')) = 'null' THEN NULL ELSE JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.expiryDate')) END AS DATE) ASC, c.id ASC, v.version_no ASC";
            return "<choose>" + factsSorts(stableOrder)
                    + sortWhen(SORT_FIELD_CERTIFICATE_NO, SORT_ORDER_ASC, "NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.certificateNo')), 'null')",
                    textBlankLast("NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.certificateNo')), 'null')"), stableOrder)
                    + sortWhen(SORT_FIELD_CERTIFICATE_NO, SORT_ORDER_DESC, "NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.certificateNo')), 'null')",
                    textBlankLast("NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.certificateNo')), 'null')"), stableOrder)
                    + sortWhen(SORT_FIELD_OWNER_COMPANY_NAME, SORT_ORDER_ASC, OWNER_COMPANY_NAME_SORT_EXPRESSION,
                    textBlankLast(OWNER_COMPANY_NAME_SORT_EXPRESSION), stableOrder)
                    + sortWhen(SORT_FIELD_OWNER_COMPANY_NAME, SORT_ORDER_DESC, OWNER_COMPANY_NAME_SORT_EXPRESSION,
                    textBlankLast(OWNER_COMPANY_NAME_SORT_EXPRESSION), stableOrder)
                    + sortWhen(SORT_FIELD_PRODUCT_NAME, SORT_ORDER_ASC, "s.product_name",
                    textBlankLast("s.product_name"), stableOrder)
                    + sortWhen(SORT_FIELD_PRODUCT_NAME, SORT_ORDER_DESC, "s.product_name",
                    textBlankLast("s.product_name"), stableOrder)
                    + sortWhen(SORT_FIELD_CLASSIFICATION, SORT_ORDER_ASC, "NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.classification')), 'null')",
                    textBlankLast("NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.classification')), 'null')"), stableOrder)
                    + sortWhen(SORT_FIELD_CLASSIFICATION, SORT_ORDER_DESC, "NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.classification')), 'null')",
                    textBlankLast("NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.classification')), 'null')"), stableOrder)
                    + sortWhen(SORT_FIELD_PROJECT_CODE, SORT_ORDER_ASC, "CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.ownProduct')) = 'false' THEN NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.projectCode')), 'null') ELSE pc.project_code END",
                    textBlankLast("CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.ownProduct')) = 'false' THEN NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.projectCode')), 'null') ELSE pc.project_code END"), stableOrder)
                    + sortWhen(SORT_FIELD_PROJECT_CODE, SORT_ORDER_DESC, "CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.ownProduct')) = 'false' THEN NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.projectCode')), 'null') ELSE pc.project_code END",
                    textBlankLast("CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.ownProduct')) = 'false' THEN NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.projectCode')), 'null') ELSE pc.project_code END"), stableOrder)
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
                    + sortWhen(SORT_FIELD_APPROVAL_DATE, SORT_ORDER_ASC, "CAST(CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.approvalDate')) = 'null' THEN NULL ELSE JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.approvalDate')) END AS DATE)",
                    nullLast("CAST(CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.approvalDate')) = 'null' THEN NULL ELSE JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.approvalDate')) END AS DATE)"), stableOrder)
                    + sortWhen(SORT_FIELD_APPROVAL_DATE, SORT_ORDER_DESC, "CAST(CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.approvalDate')) = 'null' THEN NULL ELSE JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.approvalDate')) END AS DATE)",
                    nullLast("CAST(CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.approvalDate')) = 'null' THEN NULL ELSE JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.approvalDate')) END AS DATE)"), stableOrder)
                    + sortWhen(SORT_FIELD_EFFECTIVE_DATE, SORT_ORDER_ASC, "CAST(CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.effectiveDate')) = 'null' THEN NULL ELSE JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.effectiveDate')) END AS DATE)",
                    nullLast("CAST(CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.effectiveDate')) = 'null' THEN NULL ELSE JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.effectiveDate')) END AS DATE)"), stableOrder)
                    + sortWhen(SORT_FIELD_EFFECTIVE_DATE, SORT_ORDER_DESC, "CAST(CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.effectiveDate')) = 'null' THEN NULL ELSE JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.effectiveDate')) END AS DATE)",
                    nullLast("CAST(CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.effectiveDate')) = 'null' THEN NULL ELSE JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.effectiveDate')) END AS DATE)"), stableOrder)
                    + sortWhen(SORT_FIELD_EXPIRY_DATE, SORT_ORDER_ASC, "CAST(CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.expiryDate')) = 'null' THEN NULL ELSE JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.expiryDate')) END AS DATE)",
                    nullLast("CAST(CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.expiryDate')) = 'null' THEN NULL ELSE JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.expiryDate')) END AS DATE)"), stableOrder)
                    + sortWhen(SORT_FIELD_EXPIRY_DATE, SORT_ORDER_DESC, "CAST(CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.expiryDate')) = 'null' THEN NULL ELSE JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.expiryDate')) END AS DATE)",
                    nullLast("CAST(CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.expiryDate')) = 'null' THEN NULL ELSE JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.expiryDate')) END AS DATE)"), stableOrder)
                    + sortWhen(SORT_FIELD_REMINDER, SORT_ORDER_ASC, REMINDER_PRIORITY_SORT_EXPRESSION,
                    NO_EMPTY_RANK, stableOrder)
                    + sortWhen(SORT_FIELD_REMINDER, SORT_ORDER_DESC, REMINDER_PRIORITY_SORT_EXPRESSION,
                    NO_EMPTY_RANK, stableOrder)
                    + sortWhen(SORT_FIELD_REMARK, SORT_ORDER_ASC, "NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.remark')), 'null')",
                    textBlankLast("NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.remark')), 'null')"), stableOrder)
                    + sortWhen(SORT_FIELD_REMARK, SORT_ORDER_DESC, "NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.remark')), 'null')",
                    textBlankLast("NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.remark')), 'null')"), stableOrder)
                    + "<otherwise> ORDER BY c.owner_company_id ASC, CAST(CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.expiryDate')) = 'null' THEN NULL ELSE JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.expiryDate')) END AS DATE) ASC, c.id ASC, v.version_no ASC</otherwise>"
                    + "</choose>";
        }

        private static String oldIndexOrderBy() {
            String stableOrder = ", CAST(CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.expiryDate')) = 'null' THEN NULL ELSE JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.expiryDate')) END AS DATE) DESC, c.id ASC, v.version_no DESC";
            return "<choose>" + factsSorts(stableOrder)
                    + sortWhen(SORT_FIELD_CERTIFICATE_NO, SORT_ORDER_ASC, "NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.certificateNo')), 'null')",
                    textBlankLast("NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.certificateNo')), 'null')"), stableOrder)
                    + sortWhen(SORT_FIELD_CERTIFICATE_NO, SORT_ORDER_DESC, "NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.certificateNo')), 'null')",
                    textBlankLast("NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.certificateNo')), 'null')"), stableOrder)
                    + sortWhen(SORT_FIELD_OWNER_COMPANY_NAME, SORT_ORDER_ASC, OWNER_COMPANY_NAME_SORT_EXPRESSION,
                    textBlankLast(OWNER_COMPANY_NAME_SORT_EXPRESSION), stableOrder)
                    + sortWhen(SORT_FIELD_OWNER_COMPANY_NAME, SORT_ORDER_DESC, OWNER_COMPANY_NAME_SORT_EXPRESSION,
                    textBlankLast(OWNER_COMPANY_NAME_SORT_EXPRESSION), stableOrder)
                    + sortWhen(SORT_FIELD_PRODUCT_NAME, SORT_ORDER_ASC, "s.product_name",
                    textBlankLast("s.product_name"), stableOrder)
                    + sortWhen(SORT_FIELD_PRODUCT_NAME, SORT_ORDER_DESC, "s.product_name",
                    textBlankLast("s.product_name"), stableOrder)
                    + sortWhen(SORT_FIELD_CLASSIFICATION, SORT_ORDER_ASC, "NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.classification')), 'null')",
                    textBlankLast("NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.classification')), 'null')"), stableOrder)
                    + sortWhen(SORT_FIELD_CLASSIFICATION, SORT_ORDER_DESC, "NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.classification')), 'null')",
                    textBlankLast("NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.classification')), 'null')"), stableOrder)
                    + sortWhen(SORT_FIELD_VERSION_NO, SORT_ORDER_ASC, "v.version_no",
                    nullLast("v.version_no"), stableOrder)
                    + sortWhen(SORT_FIELD_VERSION_NO, SORT_ORDER_DESC, "v.version_no",
                    nullLast("v.version_no"), stableOrder)
                    + sortWhen(SORT_FIELD_STATUS, SORT_ORDER_ASC, "v.status",
                    textBlankLast("v.status"), stableOrder)
                    + sortWhen(SORT_FIELD_STATUS, SORT_ORDER_DESC, "v.status",
                    textBlankLast("v.status"), stableOrder)
                    + sortWhen(SORT_FIELD_EXPIRY_DATE, SORT_ORDER_ASC, "CAST(CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.expiryDate')) = 'null' THEN NULL ELSE JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.expiryDate')) END AS DATE)",
                    nullLast("CAST(CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.expiryDate')) = 'null' THEN NULL ELSE JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.expiryDate')) END AS DATE)"), stableOrder)
                    + sortWhen(SORT_FIELD_EXPIRY_DATE, SORT_ORDER_DESC, "CAST(CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.expiryDate')) = 'null' THEN NULL ELSE JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.expiryDate')) END AS DATE)",
                    nullLast("CAST(CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.expiryDate')) = 'null' THEN NULL ELSE JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.expiryDate')) END AS DATE)"), stableOrder)
                    + "<otherwise> ORDER BY CAST(CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.expiryDate')) = 'null' THEN NULL ELSE JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.expiryDate')) END AS DATE) DESC, c.id ASC, v.version_no DESC</otherwise>"
                    + "</choose>";
        }

        private static String factsSorts(String stableOrder) {
            String result = "";
            for (String field : List.of("certificateType", "certificateStatus", "productNameEn", "productCode", "kpNumber", "submissionType", "approvalLetterStatus", "factoryRegistrationStatus", "listingDate", "certificateHolder", "registrationLink", "tradeName", "notifiedBodyCode", "regulationCode", "listingNumber", "customerHeld", "ownProduct", "changeDescription", "firstObtainedDate", "approvalDate", "effectiveDate", "expiryDate", "remark")) {
                String expression = "NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$." + field + "')), 'null')";
                result += sortWhen(field, SORT_ORDER_ASC, expression, textBlankLast(expression), stableOrder);
                result += sortWhen(field, SORT_ORDER_DESC, expression, textBlankLast(expression), stableOrder);
            }
            result += sortWhen("registrantName",SORT_ORDER_ASC,"s.registrant_name",textBlankLast("s.registrant_name"),stableOrder);
            result += sortWhen("registrantName",SORT_ORDER_DESC,"s.registrant_name",textBlankLast("s.registrant_name"),stableOrder);
            result += sortWhen("modelSpecification",SORT_ORDER_ASC,"s.model_specification",textBlankLast("s.model_specification"),stableOrder);
            result += sortWhen("modelSpecification",SORT_ORDER_DESC,"s.model_specification",textBlankLast("s.model_specification"),stableOrder);
            result += sortWhen("structureComposition",SORT_ORDER_ASC,"s.structure_composition",textBlankLast("s.structure_composition"),stableOrder);
            result += sortWhen("structureComposition",SORT_ORDER_DESC,"s.structure_composition",textBlankLast("s.structure_composition"),stableOrder);
            result += sortWhen("intendedUse",SORT_ORDER_ASC,"s.intended_use",textBlankLast("s.intended_use"),stableOrder);
            result += sortWhen("intendedUse",SORT_ORDER_DESC,"s.intended_use",textBlankLast("s.intended_use"),stableOrder);
            result += sortWhen("technicalRequirements",SORT_ORDER_ASC,"s.technical_requirements",textBlankLast("s.technical_requirements"),stableOrder);
            result += sortWhen("technicalRequirements",SORT_ORDER_DESC,"s.technical_requirements",textBlankLast("s.technical_requirements"),stableOrder);
            result += sortWhen("residenceAddress",SORT_ORDER_ASC,"s.residence_address",textBlankLast("s.residence_address"),stableOrder);
            result += sortWhen("residenceAddress",SORT_ORDER_DESC,"s.residence_address",textBlankLast("s.residence_address"),stableOrder);
            result += sortWhen("productionAddress",SORT_ORDER_ASC,"s.production_address",textBlankLast("s.production_address"),stableOrder);
            result += sortWhen("productionAddress",SORT_ORDER_DESC,"s.production_address",textBlankLast("s.production_address"),stableOrder);
            result += sortWhen("entrustedProduction",SORT_ORDER_ASC,"s.entrusted_production",textBlankLast("s.entrusted_production"),stableOrder);
            result += sortWhen("entrustedProduction",SORT_ORDER_DESC,"s.entrusted_production",textBlankLast("s.entrusted_production"),stableOrder);
            result += sortWhen("selfProduction",SORT_ORDER_ASC,"s.self_production",textBlankLast("s.self_production"),stableOrder);
            result += sortWhen("selfProduction",SORT_ORDER_DESC,"s.self_production",textBlankLast("s.self_production"),stableOrder);
            result += sortWhen("entrustedEnterpriseName",SORT_ORDER_ASC,"CAST(s.entrusted_enterprises_json AS CHAR)",textBlankLast("CAST(s.entrusted_enterprises_json AS CHAR)"),stableOrder);
            result += sortWhen("entrustedEnterpriseName",SORT_ORDER_DESC,"CAST(s.entrusted_enterprises_json AS CHAR)",textBlankLast("CAST(s.entrusted_enterprises_json AS CHAR)"),stableOrder);
            result += sortWhen("ceRevision",SORT_ORDER_ASC,"NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.ceCertificates[0].revision')), 'null')",textBlankLast("NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.ceCertificates[0].revision')), 'null')"),stableOrder);
            result += sortWhen("ceRevision",SORT_ORDER_DESC,"NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.ceCertificates[0].revision')), 'null')",textBlankLast("NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.ceCertificates[0].revision')), 'null')"),stableOrder);
            result += sortWhen("ceCertificateNo",SORT_ORDER_ASC,"NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.ceCertificates[0].certificateNo')), 'null')",textBlankLast("NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.ceCertificates[0].certificateNo')), 'null')"),stableOrder);
            result += sortWhen("ceCertificateNo",SORT_ORDER_DESC,"NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.ceCertificates[0].certificateNo')), 'null')",textBlankLast("NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.ceCertificates[0].certificateNo')), 'null')"),stableOrder);
            result += sortWhen("ceIssueDate",SORT_ORDER_ASC,"NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.ceCertificates[0].issueDate')), 'null')",textBlankLast("NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.ceCertificates[0].issueDate')), 'null')"),stableOrder);
            result += sortWhen("ceIssueDate",SORT_ORDER_DESC,"NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.ceCertificates[0].issueDate')), 'null')",textBlankLast("NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.ceCertificates[0].issueDate')), 'null')"),stableOrder);
            result += sortWhen("projectCode",SORT_ORDER_ASC,"CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.ownProduct')) = 'false' THEN NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.projectCode')), 'null') ELSE pc.project_code END",textBlankLast("CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.ownProduct')) = 'false' THEN NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.projectCode')), 'null') ELSE pc.project_code END"),stableOrder);
            result += sortWhen("projectCode",SORT_ORDER_DESC,"CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.ownProduct')) = 'false' THEN NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.projectCode')), 'null') ELSE pc.project_code END",textBlankLast("CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.ownProduct')) = 'false' THEN NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.projectCode')), 'null') ELSE pc.project_code END"),stableOrder);
            return result;
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
            return "#{query.businessDate} >= TIMESTAMPADD(MONTH, -" + months + ", CAST(CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.expiryDate')) = 'null' THEN NULL ELSE JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.expiryDate')) END AS DATE))";
        }

        private static String script(String select, String where, String suffix) {
            return "<script>" + select + where + suffix + "</script>";
        }

        private static String pageScript(String select, String where, String suffix) {
            // Only CE array searches need more than MySQL's default 1000 recursive rows.
            String ceHint = "<if test=\"query != null and ((query.ceCertificateNo != null and query.ceCertificateNo != '') or (query.ceRevision != null and query.ceRevision != '') or query.ceIssueStart != null or query.ceIssueEnd != null)\">"
                    + " /*+ SET_VAR(cte_max_recursion_depth=4294967295) */ </if>";
            return script(select.replaceFirst("SELECT", "SELECT" + ceHint), where, suffix);
        }

        private static String select() {
            // MySQL 8 can turn valid JSON ISO dates into NULL when CAST receives NULLIF; CASE preserves them.
            return """
                    SELECT c.id AS certificate_id,
                           c.row_version,
                           v.id AS version_id,
                           s.id AS snapshot_id,
                           s.revision_no AS snapshot_revision,
                           c.owner_company_id,
                           c.product_master_id,
                           c.project_code_id,
                           CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.ownProduct')) = 'false' THEN NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.projectCode')), 'null') ELSE pc.project_code END AS project_code,
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
                           CAST(CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.firstObtainedDate')) = 'null' THEN NULL ELSE JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.firstObtainedDate')) END AS DATE) AS first_obtained_date,
                           s.facts_json,
                           s.product_name,
                           NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.certificateNo')), 'null') AS certificate_no,
                           v.version_no,
                           v.status,
                           CAST(CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.approvalDate')) = 'null' THEN NULL ELSE JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.approvalDate')) END AS DATE) AS approval_date,
                           CAST(CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.effectiveDate')) = 'null' THEN NULL ELSE JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.effectiveDate')) END AS DATE) AS effective_date,
                           CAST(CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.expiryDate')) = 'null' THEN NULL ELSE JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.expiryDate')) END AS DATE) AS expiry_date,
                           NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.classification')), 'null') AS classification,
                           NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.remark')), 'null') AS remark,
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

        private static String factsFilters() {
            return """
                    <if test="query != null">
                      <if test="query.certificateType != null and query.certificateType != ''">
                        AND JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.certificateType')) = #{query.certificateType}
                      </if>
                      <if test="query.fieldFilters != null">
                        <foreach collection="query.fieldFilters" index="field" item="value">
                          <choose>
                            <when test="field == 'structureComposition'">AND s.structure_composition LIKE CONCAT('%', #{value}, '%')</when>
                            <when test="field == 'intendedUse'">AND s.intended_use LIKE CONCAT('%', #{value}, '%')</when>
                            <when test="field == 'technicalRequirements'">AND s.technical_requirements LIKE CONCAT('%', #{value}, '%')</when>
                            <when test="field == 'residenceAddress'">AND s.residence_address LIKE CONCAT('%', #{value}, '%')</when>
                            <when test="field == 'entrustedProduction'">AND CASE WHEN s.entrusted_production THEN 'true' ELSE 'false' END = #{value}</when>
                            <when test="field == 'selfProduction'">AND CASE WHEN s.self_production THEN 'true' ELSE 'false' END = #{value}</when>
                            <when test="field == 'projectCode'">AND (CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.ownProduct')) = 'false' THEN NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.projectCode')), 'null') ELSE pc.project_code END) LIKE CONCAT('%',#{value},'%')</when>
                            <when test="field == 'entrustedEnterpriseName'">AND EXISTS(SELECT 1 FROM dcc_registration_certificate_snapshot_entrusted se WHERE se.tenant_id=s.tenant_id AND se.snapshot_id=s.id AND se.enterprise_name_snapshot LIKE CONCAT('%',#{value},'%') AND se.deleted=0)</when>
                            <otherwise>AND JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, CONCAT('$.', #{field}))) LIKE CONCAT('%', #{value}, '%')</otherwise>
                          </choose>
                        </foreach>
                      </if>
                      <if test="(query.ceCertificateNo != null and query.ceCertificateNo != '') or (query.ceRevision != null and query.ceRevision != '') or query.ceIssueStart != null or query.ceIssueEnd != null">
                        AND EXISTS (
                          WITH RECURSIVE ce (idx) AS (
                            SELECT 0 WHERE JSON_LENGTH(JSON_EXTRACT(s.facts_json, '$.ceCertificates')) &gt; 0
                            UNION ALL
                            SELECT idx + 1 FROM ce WHERE idx + 1 &lt; JSON_LENGTH(JSON_EXTRACT(s.facts_json, '$.ceCertificates'))
                          )
                          SELECT 1 FROM ce WHERE 1=1
                          <if test="query.ceRevision != null and query.ceRevision != ''">AND CASE WHEN JSON_TYPE(JSON_EXTRACT(s.facts_json, CONCAT('$.ceCertificates[', ce.idx, '].revision'))) = 'NULL' THEN NULL ELSE JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, CONCAT('$.ceCertificates[', ce.idx, '].revision'))) END LIKE CONCAT('%', #{query.ceRevision}, '%')</if>
                          <if test="query.ceCertificateNo != null and query.ceCertificateNo != ''">AND CASE WHEN JSON_TYPE(JSON_EXTRACT(s.facts_json, CONCAT('$.ceCertificates[', ce.idx, '].certificateNo'))) = 'NULL' THEN NULL ELSE JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, CONCAT('$.ceCertificates[', ce.idx, '].certificateNo'))) END LIKE CONCAT('%', #{query.ceCertificateNo}, '%')</if>
                          <if test="query.ceIssueStart != null">AND CAST(CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, CONCAT('$.ceCertificates[', ce.idx, '].issueDate'))) = 'null' THEN NULL ELSE JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, CONCAT('$.ceCertificates[', ce.idx, '].issueDate'))) END AS DATE) &gt;= #{query.ceIssueStart}</if>
                          <if test="query.ceIssueEnd != null">AND CAST(CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, CONCAT('$.ceCertificates[', ce.idx, '].issueDate'))) = 'null' THEN NULL ELSE JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, CONCAT('$.ceCertificates[', ce.idx, '].issueDate'))) END AS DATE) &lt;= #{query.ceIssueEnd}</if>)
                      </if>
                      <if test="query.listingStart != null">AND CAST(CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(s.facts_json,'$.listingDate')) = 'null' THEN NULL ELSE JSON_UNQUOTE(JSON_EXTRACT(s.facts_json,'$.listingDate')) END AS DATE) &gt;= #{query.listingStart}</if>
                      <if test="query.listingEnd != null">AND CAST(CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(s.facts_json,'$.listingDate')) = 'null' THEN NULL ELSE JSON_UNQUOTE(JSON_EXTRACT(s.facts_json,'$.listingDate')) END AS DATE) &lt;= #{query.listingEnd}</if>
                    </if>
                    """;
        }

        private static String filters() {
            return factsFilters() + """
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
                        AND NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.certificateNo')), 'null') LIKE CONCAT('%', #{query.certificateNo}, '%')
                      </if>
                      <if test="query.ownerCompanyName != null and query.ownerCompanyName != ''">
                        AND EXISTS (SELECT 1 FROM mdm_enterprise e WHERE e.tenant_id = c.tenant_id AND e.id = c.owner_company_id AND e.deleted = 0 AND e.type = 'OWNED_COMPANY' AND e.name LIKE CONCAT('%', #{query.ownerCompanyName}, '%'))
                      </if>
                      <if test="query.productName != null and query.productName != ''">
                        AND s.product_name LIKE CONCAT('%', #{query.productName}, '%')
                      </if>
                      <if test="query.classification != null and query.classification != ''">
                        AND NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.classification')), 'null') LIKE CONCAT('%', #{query.classification}, '%')
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
                        AND (CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.ownProduct')) = 'false' THEN NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.projectCode')), 'null') ELSE pc.project_code END) LIKE CONCAT('%', #{query.projectCode}, '%')
                      </if>
                      <if test="query.missingProjectCode != null and query.missingProjectCode">
                        AND (CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.ownProduct')) = 'false' THEN NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.projectCode')), 'null') ELSE pc.project_code END) IS NULL
                      </if>
                      <if test="query.missingProjectCode != null and !query.missingProjectCode">
                        AND (CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.ownProduct')) = 'false' THEN NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.projectCode')), 'null') ELSE pc.project_code END) IS NOT NULL
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
                        AND CAST(CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.firstObtainedDate')) = 'null' THEN NULL ELSE JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.firstObtainedDate')) END AS DATE) &gt;= #{query.firstObtainedStart}
                      </if>
                      <if test="query.firstObtainedEnd != null">
                        AND CAST(CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.firstObtainedDate')) = 'null' THEN NULL ELSE JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.firstObtainedDate')) END AS DATE) &lt;= #{query.firstObtainedEnd}
                      </if>
                      <if test="query.approvalStart != null">
                        AND CAST(CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.approvalDate')) = 'null' THEN NULL ELSE JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.approvalDate')) END AS DATE) &gt;= #{query.approvalStart}
                      </if>
                      <if test="query.approvalEnd != null">
                        AND CAST(CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.approvalDate')) = 'null' THEN NULL ELSE JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.approvalDate')) END AS DATE) &lt;= #{query.approvalEnd}
                      </if>
                      <if test="query.effectiveStart != null">
                        AND CAST(CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.effectiveDate')) = 'null' THEN NULL ELSE JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.effectiveDate')) END AS DATE) &gt;= #{query.effectiveStart}
                      </if>
                      <if test="query.effectiveEnd != null">
                        AND CAST(CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.effectiveDate')) = 'null' THEN NULL ELSE JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.effectiveDate')) END AS DATE) &lt;= #{query.effectiveEnd}
                      </if>
                      <if test="query.expiryStart != null">
                        AND CAST(CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.expiryDate')) = 'null' THEN NULL ELSE JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.expiryDate')) END AS DATE) &gt;= #{query.expiryStart}
                      </if>
                      <if test="query.expiryEnd != null">
                        AND CAST(CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.expiryDate')) = 'null' THEN NULL ELSE JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.expiryDate')) END AS DATE) &lt;= #{query.expiryEnd}
                      </if>
                    </if>
                    """;
        }

        private static String oldIndexFilters() {
            return factsFilters() + """
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
                        AND NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.certificateNo')), 'null') LIKE CONCAT('%', #{query.certificateNo}, '%')
                      </if>
                      <if test="query.ownerCompanyName != null and query.ownerCompanyName != ''">
                        AND EXISTS (SELECT 1 FROM mdm_enterprise e WHERE e.tenant_id = c.tenant_id AND e.id = c.owner_company_id AND e.deleted = 0 AND e.type = 'OWNED_COMPANY' AND e.name LIKE CONCAT('%', #{query.ownerCompanyName}, '%'))
                      </if>
                      <if test="query.productName != null and query.productName != ''">
                        AND s.product_name LIKE CONCAT('%', #{query.productName}, '%')
                      </if>
                      <if test="query.classification != null and query.classification != ''">
                        AND NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.classification')), 'null') LIKE CONCAT('%', #{query.classification}, '%')
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
                        AND (CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.ownProduct')) = 'false' THEN NULLIF(JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.projectCode')), 'null') ELSE pc.project_code END) LIKE CONCAT('%', #{query.projectCode}, '%')
                      </if>
                      <if test="query.firstObtainedStart != null">
                        AND CAST(CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.firstObtainedDate')) = 'null' THEN NULL ELSE JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.firstObtainedDate')) END AS DATE) &gt;= #{query.firstObtainedStart}
                      </if>
                      <if test="query.firstObtainedEnd != null">
                        AND CAST(CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.firstObtainedDate')) = 'null' THEN NULL ELSE JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.firstObtainedDate')) END AS DATE) &lt;= #{query.firstObtainedEnd}
                      </if>
                      <if test="query.approvalStart != null">
                        AND CAST(CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.approvalDate')) = 'null' THEN NULL ELSE JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.approvalDate')) END AS DATE) &gt;= #{query.approvalStart}
                      </if>
                      <if test="query.approvalEnd != null">
                        AND CAST(CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.approvalDate')) = 'null' THEN NULL ELSE JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.approvalDate')) END AS DATE) &lt;= #{query.approvalEnd}
                      </if>
                      <if test="query.effectiveStart != null">
                        AND CAST(CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.effectiveDate')) = 'null' THEN NULL ELSE JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.effectiveDate')) END AS DATE) &gt;= #{query.effectiveStart}
                      </if>
                      <if test="query.effectiveEnd != null">
                        AND CAST(CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.effectiveDate')) = 'null' THEN NULL ELSE JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.effectiveDate')) END AS DATE) &lt;= #{query.effectiveEnd}
                      </if>
                      <if test="query.expiryStart != null">
                        AND CAST(CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.expiryDate')) = 'null' THEN NULL ELSE JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.expiryDate')) END AS DATE) &gt;= #{query.expiryStart}
                      </if>
                      <if test="query.expiryEnd != null">
                        AND CAST(CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.expiryDate')) = 'null' THEN NULL ELSE JSON_UNQUOTE(JSON_EXTRACT(s.facts_json, '$.expiryDate')) END AS DATE) &lt;= #{query.expiryEnd}
                      </if>
                    </if>
                    """;
        }
    }
}
