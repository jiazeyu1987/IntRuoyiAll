package cn.iocoder.yudao.module.dcc.service.productcatalog.packageio;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkService;
import cn.iocoder.yudao.module.mdm.api.companyscope.MdmCompanyScopeApi;
import cn.iocoder.yudao.module.dcc.service.productcatalog.DccProductCatalogRegistrationSyncService;
import cn.iocoder.yudao.module.system.service.controlledcontent.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.sql.Statement;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentType.DCC_REGISTRATION_CERTIFICATE;

/** Shares catalog rows globally and transfers tenant-owned data and bindings into the current tenant. */
@Service
public class DccCatalogPackageService {
    @Resource private JdbcTemplate jdbc;
    @Resource private MdmCompanyScopeApi companyScope;
    @Resource private SecurityFrameworkService security;
    @Resource private ControlledContentRegistrationProjectionService projection;
    @Resource private ObjectMapper json;
    @Resource private DccProductCatalogRegistrationSyncService catalogSync;
    private static final String FORMAT = "dcc-catalog-workbook.v4";
    record Spec(String name, String table, String fields, String identity, String filter) {
        List<String> columns() { return List.of(fields.split(",")); }
        List<String> keys() { return List.of(identity.split(",")); }
    }
    private static final List<Spec> SPECS = List.of(
        new Spec("关联公司", "mdm_enterprise", "id,enterprise_code,name,type,status,revision,deleted", "enterprise_code", "type='ENTRUSTED_PARTY'"),
        new Spec("授权公司", "mdm_enterprise", "id,enterprise_code,name,type,status,revision,deleted", "enterprise_code", "type='OWNED_COMPANY'"),
        new Spec("产品主数据", "mdm_product", "id,product_code,dcc_product_code,name_cn,name_en,model_specification,category,status,deleted", "product_code", "1=1"),
        new Spec("DCC项目代码", "dcc_project_code", "id,product_master_id,doc_control_no,project_name,project_code,category,commissioned_production,project_leader,project_leader_user_id,default_attributes_json,project_engineer,storage_location,priority,status,last_import_batch_id,batch_record_total_recognition_json", "project_name,project_code", "1=1"),
        new Spec("DCC产品目录", "dcc_product_catalog", "id,data_source,original_row_no,category_level1,category_level2,product_sequence,product,product_code,project_name,project_code,registration_certificate_name,registration_certificate_number,certificate_holder,registration_place,effective_date,expiry_date,classification,registration_info_link,product_status,remark", "data_source,original_row_no", "1=1"),
        new Spec("注册证", "dcc_registration_certificate", "id,owner_company_id,product_master_id,project_code_id,first_obtained_date,current_version_id,pending_version_id,current_snapshot_id,status,row_version,certificate_key", "owner_company_id,certificate_key", "1=1"),
        new Spec("注册证版本", "dcc_registration_certificate_version", "id,certificate_id,version_no,version_type,certificate_no,approval_date,effective_date,expiry_date,classification,category_changed,base_snapshot_id,remark,status,formalized_at,formalized_by,voided_at,voided_by,void_reason", "certificate_id,version_no", "1=1"),
        new Spec("注册证快照", "dcc_registration_certificate_snapshot", "id,version_id,revision_no,source_change_id,product_name,registrant_name,model_specification,structure_composition,intended_use,technical_requirements,residence_address,production_address,entrusted_production,self_production,entrusted_enterprises_json,effective_at", "version_id,revision_no", "1=1"),
        new Spec("受托公司绑定", "dcc_registration_certificate_snapshot_entrusted", "id,snapshot_id,enterprise_id,enterprise_name_snapshot,sort_order", "snapshot_id,enterprise_id", "1=1"),
        new Spec("目录项目注册证绑定", "dcc_data_relation", "id,product_catalog_id,project_code_id,registration_certificate_id,relation_status,relation_source,relation_remark,confirmed_by,confirmed_time", "product_catalog_id,project_code_id,registration_certificate_id", "1=1"));
    private static final Map<String, String> REFERENCES = Map.ofEntries(
        Map.entry("owner_company_id", "mdm_enterprise"), Map.entry("enterprise_id", "mdm_enterprise"),
        Map.entry("product_master_id", "mdm_product"), Map.entry("project_code_id", "dcc_project_code"),
        Map.entry("product_catalog_id", "dcc_product_catalog"),
        Map.entry("registration_certificate_id", "dcc_registration_certificate"),
        Map.entry("certificate_id", "dcc_registration_certificate"),
        Map.entry("version_id", "dcc_registration_certificate_version"),
        Map.entry("current_version_id", "dcc_registration_certificate_version"),
        Map.entry("pending_version_id", "dcc_registration_certificate_version"),
        Map.entry("current_snapshot_id", "dcc_registration_certificate_snapshot"),
        Map.entry("base_snapshot_id", "dcc_registration_certificate_snapshot"),
        Map.entry("snapshot_id", "dcc_registration_certificate_snapshot"));
    private static final Set<String> DEFERRED = Set.of("current_version_id", "pending_version_id", "current_snapshot_id", "base_snapshot_id");
    private static final Set<String> VIRTUAL_FIELDS = Set.of("certificate_key");
    private static final Set<String> LOCAL_REFERENCES = Set.of("project_leader_user_id", "confirmed_by", "formalized_by", "voided_by", "source_change_id", "last_import_batch_id");
    private static final Set<String> USER_FIELDS = Set.of("project_leader_user_id", "confirmed_by", "formalized_by", "voided_by");
    private static final Set<String> BIT_FIELDS = Set.of("deleted", "category_changed", "entrusted_production", "self_production");

    @Transactional(readOnly = true)
    public byte[] exportWorkbook() throws IOException {
        long tenant = tenant();
        var data = load(tenant, false);
        // Archived dependencies remain archived in the destination; they are part of the
        // referenced graph, not restored master records or unrelated recycle-bin contents.
        Set<String> referencedProducts = new HashSet<>();
        Set<String> referencedCompanies = new HashSet<>();
        for (var spec : SPECS) for (var row : data.get(spec.name())) {
            if (row.get("product_master_id") != null) referencedProducts.add(row.get("product_master_id"));
            for (String field : List.of("owner_company_id", "enterprise_id"))
                if (row.get(field) != null) referencedCompanies.add(row.get(field));
            if (row.get("entrusted_enterprises_json") != null)
                for (var node : json.readTree(row.get("entrusted_enterprises_json"))) referencedCompanies.add(node.path("enterpriseId").asText());
        }
        data.get("产品主数据").removeIf(row -> !referencedProducts.contains(row.get("id")));
        for (String name : List.of("关联公司", "授权公司"))
            data.get(name).removeIf(row -> "1".equals(row.get("deleted")) && !referencedCompanies.contains(row.get("id")));
        validateAccess(data);
        var sheets = schema(tenant);
        for (int i = 0; i < SPECS.size(); i++) {
            var s = SPECS.get(i);
            sheets.set(i + 1, new DccCatalogWorkbook.SheetData(s.name(), s.columns(), data.get(s.name())));
        }
        return DccCatalogWorkbook.write(sheets);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Integer> importWorkbook(byte[] bytes) throws IOException {
        long tenant = tenant();
        var sheets = DccCatalogWorkbook.read(bytes, schema(tenant));
        long sourceTenant = Long.parseLong(required(sheets.get(0).rows().get(0), "tenant", "导入说明"));
        if (sourceTenant < 0) throw invalid("工作簿来源租户无效");
        boolean crossTenant = sourceTenant != tenant;
        if (crossTenant && !isAdministrator()) throw invalid("跨租户工作簿仅允许管理员导入");
        Map<String, List<Map<String, String>>> incoming = new LinkedHashMap<>();
        for (int i = 0; i < SPECS.size(); i++) incoming.put(SPECS.get(i).name(), sheets.get(i + 1).rows());
        validateGraph(incoming);
        var existing = load(tenant, true);
        Map<String, Map<String, String>> ids = new HashMap<>();
        Map<String, Map<String, String>> beforeCertificates = new HashMap<>();
        Set<String> newCertificates = new HashSet<>();
        Set<String> newVersions = new HashSet<>();
        int created = 0, updated = 0, localReferences = 0;
        for (var spec : SPECS) {
            Map<List<String>, Map<String, String>> byKey = new HashMap<>();
            for (var row : existing.get(spec.name())) {
                if (byKey.put(key(spec, row), row) != null) throw invalid(spec.name() + "：现有业务编码重复，无法合并");
            }
            Set<List<String>> seen = new HashSet<>();
            for (var source : incoming.get(spec.name())) {
                var row = new LinkedHashMap<>(source);
                String sourceId = required(row, "id", spec.name());
                remap(row, ids, true);
                List<String> key = key(spec, row);
                if (!seen.add(key)) throw invalid(spec.name() + "：文件内业务编码重复 " + key);
                var old = byKey.get(key);
                if (crossTenant) {
                    // Account grants and source workflow histories are outside this five-tab transfer.
                    // Preserve target-local references on updates; never assign source tenant IDs.
                    for (String field : LOCAL_REFERENCES) if (row.containsKey(field)) {
                        if (row.get(field) != null) localReferences++;
                        row.put(field, old == null ? null : old.get(field));
                    }
                }
                preserveEntrustedBindings(spec, row, old);
                validateExternal(spec, row, old, tenant);
                if (spec.name().equals("注册证")) {
                    if (!isAdministrator()) companyScope.validateUserCompanyAccess(getLoginUserId(), Long.valueOf(row.get("owner_company_id")));
                    if (old != null) beforeCertificates.put(sourceId, old);
                    else newCertificates.add(sourceId);
                }
                String targetId;
                if (old == null) {
                    if (spec.name().equals("DCC产品目录")) {
                        int originalRow = Integer.parseInt(required(row, "original_row_no", spec.name()));
                        if (jdbc.update("UPDATE dcc_product_catalog_source_sequence SET last_row_no=GREATEST(last_row_no,?) WHERE data_source=?",
                            originalRow, row.get("data_source")) != 1) throw invalid("产品目录数据来源序列未初始化：" + row.get("data_source"));
                    }
                    if (spec.name().equals("注册证版本")) {
                        // Reserve a database-generated identity as a new draft while its
                        // snapshot identities are allocated in this same transaction.
                        row.put("status", "DRAFT");
                        newVersions.add(sourceId);
                    }
                    targetId = insert(spec, row, tenant);
                    created++;
                } else {
                    targetId = old.get("id");
                    if (spec.name().equals("DCC产品目录")) row.put("original_row_no", old.get("original_row_no"));
                    if (!sameFacts(spec, row, old, false)) {
                        validateFormalUpdate(spec, row, old, tenant, false);
                        update(spec, row, targetId, tenant, false);
                    }
                    updated++;
                }
                if (ids.computeIfAbsent(spec.table(), k -> new HashMap<>()).put(sourceId, targetId) != null)
                    throw invalid(spec.name() + "：文件内关联标识重复 " + sourceId);
            }
        }
        for (var spec : SPECS) {
            for (var source : incoming.get(spec.name())) {
                var row = new LinkedHashMap<>(source);
                remap(row, ids, false);
                String id = ids.get(spec.table()).get(source.get("id"));
                if (spec.name().equals("注册证版本") && newVersions.contains(source.get("id"))) {
                    if (crossTenant) for (String field : LOCAL_REFERENCES) if (row.containsKey(field)) row.put(field, null);
                    // Only replace the uncommitted draft created above, never an existing
                    // version. Insert its complete formal facts once, including base snapshot,
                    // retaining the database-allocated identity and all snapshot bindings.
                    if (jdbc.update("DELETE FROM dcc_registration_certificate_version WHERE id=? AND tenant_id=? AND status='DRAFT' AND deleted=0", id, tenant) != 1)
                        throw invalid("注册证版本：新建草稿标识不存在");
                    insertFinalVersion(spec, row, id, tenant);
                    continue;
                }
                if (spec.name().equals("注册证")) {
                    var before = beforeCertificates.get(source.get("id"));
                    if (before != null) {
                        for (String field : List.of("status", "current_version_id", "pending_version_id", "current_snapshot_id"))
                            if (!Objects.equals(before.get(field), row.get(field))) throw invalid("注册证：不能通过导入改变正式状态或当前版本，请走注册证业务流程");
                    }
                }
                var old = existing.get(spec.name()).stream().filter(item -> id.equals(item.get("id"))).findFirst().orElse(null);
                if (old == null || !sameFacts(spec, row, old, true)) {
                    if (old != null) validateFormalUpdate(spec, row, old, tenant, true);
                    update(spec, row, id, tenant, true);
                }
            }
        }
        for (var source : incoming.get("注册证")) {
            if (!newCertificates.contains(source.get("id"))) continue;
            var row = new LinkedHashMap<>(source);
            remap(row, ids, false);
            registerProjection(ids.get("dcc_registration_certificate").get(source.get("id")), row, tenant);
        }
        for (var source : incoming.get("注册证")) {
            if ("ACTIVE".equals(source.get("status"))) catalogSync.syncByRegistrationCertificateId(
                Long.valueOf(ids.get("dcc_registration_certificate").get(source.get("id"))));
        }
        return Map.of("created", created, "updated", updated, "localReferences", localReferences);
    }

    private List<DccCatalogWorkbook.SheetData> schema(long tenant) {
        List<DccCatalogWorkbook.SheetData> sheets = new ArrayList<>();
        sheets.add(new DccCatalogWorkbook.SheetData("导入说明", List.of("format", "tenant", "说明"),
            List.of(Map.of("format", FORMAT, "tenant", String.valueOf(tenant), "说明",
                "按业务标识合并，保留文件外数据。项目按名称及代码定位；产品目录全租户共享，按数据来源及原行号复用，绑定合并到当前租户。保留文本格式及关联标识。注册证正式状态不得修改；跨租户不迁移账号授权、人员及流程引用，保留目标已有引用。引用的历史主数据保留删除状态；快照与查询绑定明细按原数据分别迁移。附件及审批记录不迁移。"))));
        for (var spec : SPECS) sheets.add(new DccCatalogWorkbook.SheetData(spec.name(), spec.columns(), List.of()));
        return sheets;
    }
    private Map<String, List<Map<String, String>>> load(long tenant, boolean lock) {
        Map<String, List<Map<String, String>>> result = new LinkedHashMap<>();
        for (var spec : SPECS) {
            String fields = spec.columns().stream().filter(c -> !VIRTUAL_FIELDS.contains(c)).collect(Collectors.joining(","));
            boolean shared = spec.table().equals("dcc_product_catalog");
            List<Map<String, String>> rows = jdbc.query("SELECT " + fields + " FROM " + spec.table()
                + " WHERE " + (shared ? "" : "tenant_id=? AND ") + (spec.columns().contains("deleted") ? "" : "deleted=0 AND ") + spec.filter() + " ORDER BY id" + (lock ? " FOR UPDATE" : ""),
                (rs, index) -> {
                    Map<String, String> row = new LinkedHashMap<>();
                    for (String field : fields.split(",")) row.put(field, rs.getString(field));
                    return row;
                }, shared ? new Object[]{} : new Object[]{tenant});
            result.put(spec.name(), rows);
        }
        for (var certificate : result.get("注册证")) {
            var first = result.get("注册证版本").stream().filter(v -> Objects.equals(v.get("certificate_id"), certificate.get("id")))
                .min(Comparator.comparingInt(v -> Integer.parseInt(v.get("version_no")))).orElseThrow(() -> invalid("注册证缺少版本，无法导出"));
            certificate.put("certificate_key", required(first, "certificate_no", "注册证"));
        }
        return result;
    }
    private void validateAccess(Map<String, List<Map<String, String>>> data) {
        List<Long> companyIds = data.get("注册证").stream()
            .map(row -> Long.valueOf(row.get("owner_company_id"))).distinct().toList();
        // No certificate records means there are no company-scoped records to authorize.
        if (!companyIds.isEmpty() && !isAdministrator()) {
            companyScope.validateUserCompanyAccessBatch(getLoginUserId(), companyIds);
        }
    }
    private boolean isAdministrator() {
        return security.hasAnyRoles("super_admin", "tenant_admin");
    }
    private static long tenant() {
        if (TenantContextHolder.isIgnore()) throw invalid("导入导出必须指定业务租户");
        return TenantContextHolder.getRequiredTenantId();
    }
    private static IllegalArgumentException invalid(String message) { return new IllegalArgumentException(message); }
    private static String required(Map<String, String> row, String field, String sheet) {
        String value = row.get(field);
        if (value == null || value.isBlank()) throw invalid(sheet + "：" + field + " 不能为空");
        return value;
    }
    private static List<String> key(Spec spec, Map<String, String> row) {
        return spec.keys().stream().map(k -> required(row, k, spec.name())).toList();
    }
    private void validateGraph(Map<String, List<Map<String, String>>> data) throws IOException {
        Map<String, Set<String>> all = new HashMap<>();
        for (var spec : SPECS) for (var row : data.get(spec.name())) {
            if (!all.computeIfAbsent(spec.table(), k -> new HashSet<>()).add(required(row, "id", spec.name())))
                throw invalid(spec.name() + "：关联标识重复");
            key(spec, row);
            if (spec.name().equals("DCC产品目录") && Integer.parseInt(row.get("original_row_no")) < 2)
                throw invalid("产品目录原行号必须大于等于2");
            if (spec.name().equals("关联公司") && !"ENTRUSTED_PARTY".equals(row.get("type"))
                || spec.name().equals("授权公司") && !"OWNED_COMPANY".equals(row.get("type"))) throw invalid(spec.name() + "：公司类型不符");
        }
        for (var spec : SPECS) for (var row : data.get(spec.name())) {
            for (var ref : REFERENCES.entrySet()) if (row.get(ref.getKey()) != null
                && !all.getOrDefault(ref.getValue(), Set.of()).contains(row.get(ref.getKey())))
                throw invalid(spec.name() + "：绑定缺失 " + ref.getKey() + "=" + row.get(ref.getKey()));
        }
        for (var row : data.get("注册证快照")) {
            var nodes = json.readTree(required(row, "entrusted_enterprises_json", "注册证快照"));
            if (!nodes.isArray()) throw invalid("注册证快照：受托公司必须是数组");
            for (var node : nodes) if (!all.getOrDefault("mdm_enterprise", Set.of()).contains(node.path("enterpriseId").asText()))
                throw invalid("注册证快照：受托公司引用缺失");
        }
        // Snapshot JSON and query binding rows are exported independently and copied exactly.
        // Historical snapshots may have no query rows; import must not manufacture those rows.
        for (var relation : data.get("目录项目注册证绑定")) {
            var certificate = find(data.get("注册证"), relation.get("registration_certificate_id"));
            var project = find(data.get("DCC项目代码"), relation.get("project_code_id"));
            var catalog = find(data.get("DCC产品目录"), relation.get("product_catalog_id"));
            if (!Objects.equals(certificate.get("project_code_id"), project.get("id"))
                || catalog.get("project_code") != null && !catalog.get("project_code").isBlank()
                && !Objects.equals(catalog.get("project_code"), project.get("project_code"))) throw invalid("目录项目注册证绑定：项目代码不一致");
        }
        for (var certificate : data.get("注册证")) {
            if ("ACTIVE".equals(certificate.get("status")) && (certificate.get("current_version_id") == null || certificate.get("current_snapshot_id") == null))
                throw invalid("现行注册证必须具备当前版本和快照");
            var first = data.get("注册证版本").stream().filter(v -> Objects.equals(v.get("certificate_id"), certificate.get("id")))
                .min(Comparator.comparingInt(v -> Integer.parseInt(v.get("version_no")))).orElseThrow(() -> invalid("注册证缺少版本"));
            if (!Objects.equals(first.get("certificate_no"), certificate.get("certificate_key"))) throw invalid("注册证业务标识与首版注册证编号不符");
            for (String pointer : List.of("current_version_id", "pending_version_id")) {
                if (certificate.get(pointer) == null) continue;
                var version = find(data.get("注册证版本"), certificate.get(pointer));
                if (!Objects.equals(version.get("certificate_id"), certificate.get("id"))) throw invalid("注册证版本归属不符");
                String expected = pointer.equals("current_version_id")
                    ? ("VOIDED".equals(certificate.get("status")) ? "VOIDED" : "CURRENT") : "PENDING_EFFECTIVE";
                if (!expected.equals(version.get("status"))) throw invalid("注册证版本状态与指针不符");
            }
            if (certificate.get("current_snapshot_id") != null) {
                var snapshot = find(data.get("注册证快照"), certificate.get("current_snapshot_id"));
                String versionId = certificate.get("current_version_id") != null ? certificate.get("current_version_id") : certificate.get("pending_version_id");
                if (!Objects.equals(snapshot.get("version_id"), versionId)) throw invalid("注册证当前快照归属不符");
            }
        }
    }
    private static Map<String, String> find(List<Map<String, String>> rows, String id) {
        return rows.stream().filter(r -> Objects.equals(id, r.get("id"))).findFirst().orElseThrow(() -> invalid("绑定对象不存在：" + id));
    }
    private void remap(Map<String, String> row, Map<String, Map<String, String>> ids, boolean defer) throws IOException {
        for (var ref : REFERENCES.entrySet()) {
            String value = row.get(ref.getKey());
            if (value == null || defer && DEFERRED.contains(ref.getKey())) continue;
            String mapped = ids.getOrDefault(ref.getValue(), Map.of()).get(value);
            if (mapped == null) throw invalid("绑定对象尚未导入：" + ref.getKey() + "=" + value);
            row.put(ref.getKey(), mapped);
        }
        if (row.get("entrusted_enterprises_json") != null) {
            var nodes = json.readTree(row.get("entrusted_enterprises_json"));
            for (var node : nodes) {
                String id = ids.getOrDefault("mdm_enterprise", Map.of()).get(node.path("enterpriseId").asText());
                if (id == null) throw invalid("受托公司绑定缺失");
                ((ObjectNode) node).put("enterpriseId", Long.parseLong(id));
            }
            row.put("entrusted_enterprises_json", json.writeValueAsString(nodes));
        }
    }
    private void preserveEntrustedBindings(Spec spec, Map<String, String> row, Map<String, String> old) throws IOException {
        if (!spec.name().equals("注册证快照") || old == null) return;
        Set<String> incoming = new HashSet<>();
        for (var item : json.readTree(row.get("entrusted_enterprises_json"))) incoming.add(item.path("enterpriseId").asText());
        for (var item : json.readTree(old.get("entrusted_enterprises_json")))
            if (!incoming.contains(item.path("enterpriseId").asText())) throw invalid("注册证快照：不能删除文件外已有受托公司绑定");
    }
    private void validateExternal(Spec spec, Map<String, String> row, Map<String, String> old, long tenant) {
        if (spec.columns().contains("deleted")) {
            if (!Set.of("0", "1").contains(required(row, "deleted", spec.name()))) throw invalid(spec.name() + "：删除状态必须为0或1");
            if (old != null && !Objects.equals(old.get("deleted"), row.get("deleted")))
                throw invalid(spec.name() + "：导入不能删除或恢复目标已有主数据");
        }
        for (String field : USER_FIELDS) if (row.get(field) != null) {
            Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM system_users WHERE id=? AND tenant_id=? AND deleted=0", Integer.class, row.get(field), tenant);
            if (count == null || count != 1) throw invalid(spec.name() + "：用户引用不存在 " + field);
        }
        for (String field : List.of("source_change_id", "last_import_batch_id"))
            if (row.get(field) != null && (old == null || !Objects.equals(row.get(field), old.get(field))))
                throw invalid(spec.name() + "：不能导入外部流程引用 " + field);
        if (old != null && (spec.name().equals("注册证版本") || spec.name().equals("注册证")))
            if (!Objects.equals(row.get("status"), old.get("status"))) throw invalid(spec.name() + "：不能通过导入改变正式状态");
    }
    private List<String> writable(Spec spec, boolean deferred) {
        return spec.columns().stream().filter(c -> !c.equals("id") && !VIRTUAL_FIELDS.contains(c)
            && DEFERRED.contains(c) == deferred).toList();
    }
    private boolean sameFacts(Spec spec, Map<String, String> row, Map<String, String> old, boolean deferred) throws IOException {
        for (String field : writable(spec, deferred)) {
            String a = row.get(field), b = old.get(field);
            if (Objects.equals(a, b)) continue;
            if (field.equals("entrusted_enterprises_json") && a != null && b != null && json.readTree(a).equals(json.readTree(b))) continue;
            return false;
        }
        return true;
    }
    private void validateFormalUpdate(Spec spec, Map<String, String> row, Map<String, String> old, long tenant, boolean deferred) {
        String versionId = spec.name().equals("注册证快照") ? old.get("version_id") : null;
        if (spec.name().equals("受托公司绑定")) versionId = jdbc.queryForObject(
            "SELECT version_id FROM dcc_registration_certificate_snapshot WHERE id=? AND tenant_id=? AND deleted=0",
            String.class, old.get("snapshot_id"), tenant);
        if (versionId != null && !"DRAFT".equals(jdbc.queryForObject(
            "SELECT status FROM dcc_registration_certificate_version WHERE id=? AND tenant_id=? AND deleted=0",
            String.class, versionId, tenant))) throw invalid(spec.name() + "：正式快照及受托公司明细不可覆盖");
        List<String> immutable = spec.name().equals("注册证")
            ? List.of("owner_company_id", "product_master_id", "project_code_id", "first_obtained_date")
            : spec.name().equals("注册证版本") ? List.of("certificate_id", "version_no", "version_type", "certificate_no", "approval_date", "effective_date",
                "expiry_date", "classification", "category_changed", "base_snapshot_id", "formalized_at", "formalized_by") : List.of();
        if (!"DRAFT".equals(old.get("status"))) for (String field : immutable)
            if (DEFERRED.contains(field) == deferred && !Objects.equals(row.get(field), old.get(field))) throw invalid(spec.name() + "：正式数据不可覆盖");
    }
    private Object databaseValue(String field, String value) {
        if (value == null || !BIT_FIELDS.contains(field)) return value;
        if (!Set.of("0", "1").contains(value)) throw invalid(field + "：标志值必须为0或1");
        // MySQL BIT(1) rejects text parameters: ASCII '0' is wider than one bit.
        return Integer.valueOf(value);
    }
    private String insert(Spec spec, Map<String, String> row, long tenant) {
        var fields = writable(spec, false);
        List<Object> args = fields.stream().map(field -> databaseValue(field, row.get(field))).collect(Collectors.toCollection(ArrayList::new));
        args.add(tenant); args.add(String.valueOf(getLoginUserId())); args.add(String.valueOf(getLoginUserId()));
        String sql = "INSERT INTO " + spec.table() + " (" + String.join(",", fields) + ",tenant_id,creator,updater) VALUES ("
            + String.join(",", Collections.nCopies(args.size(), "?")) + ")";
        var holder = new GeneratedKeyHolder();
        int affected = jdbc.update(connection -> {
            var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < args.size(); i++) statement.setObject(i + 1, args.get(i));
            return statement;
        }, holder);
        if (affected != 1 || holder.getKey() == null) throw invalid(spec.name() + "：新增失败");
        return holder.getKey().toString();
    }
    private void insertFinalVersion(Spec spec, Map<String, String> row, String id, long tenant) {
        var fields = spec.columns().stream().filter(c -> !c.equals("id") && !VIRTUAL_FIELDS.contains(c)).toList();
        List<Object> args = fields.stream().map(field -> databaseValue(field, row.get(field))).collect(Collectors.toCollection(ArrayList::new));
        args.add(id); args.add(tenant); args.add(String.valueOf(getLoginUserId())); args.add(String.valueOf(getLoginUserId()));
        if (jdbc.update("INSERT INTO " + spec.table() + " (" + String.join(",", fields) + ",id,tenant_id,creator,updater) VALUES ("
            + String.join(",", Collections.nCopies(args.size(), "?")) + ")", args.toArray()) != 1) throw invalid("注册证版本：正式数据写入失败");
    }
    private void update(Spec spec, Map<String, String> row, String id, long tenant, boolean deferred) {
        var fields = writable(spec, deferred);
        if (fields.isEmpty()) return;
        List<Object> args = fields.stream().map(field -> databaseValue(field, row.get(field))).collect(Collectors.toCollection(ArrayList::new));
        boolean shared = spec.table().equals("dcc_product_catalog");
        args.add(String.valueOf(getLoginUserId())); args.add(id);
        if (!shared) args.add(tenant);
        int affected = jdbc.update("UPDATE " + spec.table() + " SET " + fields.stream().map(f -> f + "=?").collect(Collectors.joining(","))
            + ",updater=?,update_time=CURRENT_TIMESTAMP WHERE id=?" + (shared ? "" : " AND tenant_id=?")
            + (spec.columns().contains("deleted") ? "" : " AND deleted=0"), args.toArray());
        if (affected != 1) throw invalid(spec.name() + "：更新失败");
    }
    private void registerProjection(String id, Map<String, String> row, long tenant) {
        // A voided certificate retains its historical pointers, but is not an active publication.
        if ("VOIDED".equals(row.get("status"))) return;
        Long active = row.get("current_version_id") == null ? null : Long.valueOf(row.get("current_version_id"));
        Long pending = row.get("pending_version_id") == null ? null : Long.valueOf(row.get("pending_version_id"));
        var key = ControlledContentKey.of(tenant, DCC_REGISTRATION_CERTIFICATE, id);
        var empty = ControlledContentProjectionSnapshot.of(key, null, null);
        var withActive = ControlledContentProjectionSnapshot.of(key, active, null);
        if (active != null) projection.registerActive(key, empty, withActive, Long.valueOf(id), active,
            versionNo(active, tenant), "CURRENT", getLoginUserId(), "产品目录工作簿导入");
        if (pending != null) projection.registerReadyCandidate(key, withActive, ControlledContentProjectionSnapshot.of(key, active, pending),
            Long.valueOf(id), pending, versionNo(pending, tenant), "PENDING_EFFECTIVE", getLoginUserId(), "产品目录工作簿导入");
    }
    private String versionNo(Long id, long tenant) {
        return jdbc.queryForObject("SELECT version_no FROM dcc_registration_certificate_version WHERE id=? AND tenant_id=? AND deleted=0", String.class, id, tenant);
    }
}
