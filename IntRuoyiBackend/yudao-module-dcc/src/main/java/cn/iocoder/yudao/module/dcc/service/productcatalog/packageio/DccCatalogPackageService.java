package cn.iocoder.yudao.module.dcc.service.productcatalog.packageio;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.service.productcatalog.DccProductCatalogRowNumberAllocator;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;

/** Three human-readable sheets; database identities and certificate bindings never travel. */
@Service
public class DccCatalogPackageService {
    @Resource private JdbcTemplate jdbc;
    @Resource private DccProductCatalogRowNumberAllocator rowNumbers;
    @Resource private ObjectMapper json;

    record Spec(String name, String table, String fields, String headers) {
        List<String> columns() { return List.of(fields.split(",")); }
        List<String> labels() { return List.of(headers.split(",")); }
    }
    private static final Spec PROJECT = new Spec("项目代码", "dcc_project_code",
        "doc_control_no,project_name,project_code,category,commissioned_production,default_attributes_json,project_engineer,storage_location,priority,status,batch_record_total_recognition_json",
        "文控,项目名称,项目代码,类别,委托生产,默认属性配置,项目工程师,存放位置,优先级,状态,批记录总识别配置");
    private static final Spec PRODUCT = new Spec("产品目录", "dcc_product_catalog",
        "data_source,original_row_no,category_level1,category_level2,product_sequence,product,product_code,registration_certificate_name,registration_certificate_number,certificate_holder,registration_place,effective_date,expiry_date,classification,registration_info_link,product_status,remark",
        "数据来源,原行号,产品类别Ⅰ,产品类别Ⅱ,产品序号,产品名称,产品编码,注册证名称,注册证号,持证人,注册地,生效日期,有效期至,分类,注册证信息链接,产品状态,备注");
    private static final List<String> RELATION_COLUMNS = List.of("项目代码", "产品名称", "产品编码", "数据来源", "原行号");
    private record Binding(Map<String, String> project, Map<String, String> product) {}

    @Transactional(readOnly = true)
    public byte[] exportWorkbook() throws IOException {
        long tenant = tenant();
        var projects = load(PROJECT, tenant, false);
        var products = load(PRODUCT, tenant, false);
        index(projects, r -> List.of(codeKey(required(r, "project_code", "项目代码"))), "项目代码");
        var projectIds = byId(projects);
        var productIds = byId(products);
        var relations = new ArrayList<Map<String, String>>();
        Set<List<String>> seen = new HashSet<>();
        // Approved project/product links and manually confirmed links are both real links.
        // Text fields on catalog rows alone are not evidence of a confirmed relationship.
        for (var row : loadRelations(tenant, false)) {
            var project = projectIds.get(row.get("project_code_id"));
            var product = productIds.get(row.get("product_catalog_id"));
            if (project == null || product == null) throw invalid("产品与项目关系：关联对象不存在或不属于当前租户的项目目录");
            if (!seen.add(List.of(project.get("id"), product.get("id")))) continue;
            var relation = new LinkedHashMap<String, String>();
            relation.put("项目代码", project.get("project_code"));
            relation.put("产品名称", product.get("product"));
            relation.put("产品编码", product.get("product_code"));
            relation.put("数据来源", product.get("data_source"));
            relation.put("原行号", product.get("original_row_no"));
            relations.add(relation);
        }
        return DccCatalogWorkbook.write(List.of(sheet(PROJECT, projects), sheet(PRODUCT, products),
            new DccCatalogWorkbook.SheetData("产品与项目关系", RELATION_COLUMNS, relations)));
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Integer> importWorkbook(byte[] bytes) throws IOException {
        long tenant = tenant();
        var sheets = DccCatalogWorkbook.read(bytes, schema());
        var projects = decode(PROJECT, sheets.get(0));
        var products = decode(PRODUCT, sheets.get(1));
        var projectCodes = index(projects, r -> List.of(codeKey(required(r, "project_code", "项目代码"))), "项目代码");
        index(products, this::productKey, "产品目录业务标识");
        index(products, r -> List.of(required(r, "data_source", "产品目录"), required(r, "original_row_no", "产品目录")), "产品目录来源行号");
        for (var project : projects) {
            required(project, "project_name", "项目代码");
            if (!Set.of("ENABLE", "DISABLE").contains(required(project, "status", "项目代码")))
                throw invalid("项目代码：状态必须为 ENABLE 或 DISABLE");
            for (String field : List.of("default_attributes_json", "batch_record_total_recognition_json")) {
                if (project.get(field) == null) continue;
                try {
                    if (!json.readTree(project.get(field)).isObject()) throw invalid("项目代码：配置必须为 JSON 对象 " + field);
                } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
                    throw new IllegalArgumentException("项目代码：配置 JSON 格式无效 " + field, ex);
                }
            }
        }
        for (var product : products) {
            if (!Set.of("瑛泰产品", "子公司产品").contains(required(product, "data_source", "产品目录")))
                throw invalid("产品目录：数据来源必须为瑛泰产品或子公司产品");
            required(product, "product", "产品目录");
            try {
                if (Integer.parseInt(required(product, "original_row_no", "产品目录")) < 2) throw new NumberFormatException();
            } catch (NumberFormatException ex) { throw invalid("产品目录：原行号必须为大于等于2的整数"); }
        }
        List<Binding> bindings = new ArrayList<>();
        Set<List<String>> pairs = new HashSet<>();
        Map<List<String>, String> productProjects = new HashMap<>();
        int line = 1;
        for (var relation : sheets.get(2).rows()) {
            String context = "产品与项目关系第 " + (++line) + " 行";
            String code = codeKey(required(relation, "项目代码", context));
            String name = required(relation, "产品名称", context);
            var project = projectCodes.get(List.of(code));
            if (project == null) throw invalid(context + "：项目代码不存在于项目代码表：" + code);
            if (!"ENABLE".equals(project.get("status"))) throw invalid(context + "：不能绑定停用项目 " + code);
            var candidates = products.stream().filter(p -> name.equals(p.get("product")))
                .filter(p -> matches(relation, "产品编码", p, "product_code"))
                .filter(p -> matches(relation, "数据来源", p, "data_source"))
                .filter(p -> matches(relation, "原行号", p, "original_row_no")).toList();
            if (candidates.isEmpty()) throw invalid(context + "：产品不存在于产品目录表：" + name);
            if (candidates.size() != 1) throw invalid(context + "：产品名称重名，请填写产品编码、数据来源或原行号：" + name);
            var product = candidates.get(0);
            var key = productKey(product);
            var pair = new ArrayList<>(key); pair.add(code);
            if (!pairs.add(pair)) throw invalid(context + "：关系重复：" + code + " / " + name);
            if (productProjects.putIfAbsent(key, code) != null) throw invalid(context + "：同一条产品目录不能绑定不同项目");
            bindings.add(new Binding(project, product));
        }
        var oldProjects = load(PROJECT, tenant, true);
        var oldProducts = load(PRODUCT, tenant, true);
        var existingRelations = loadRelations(tenant, true);
        var existingProjects = index(oldProjects, r -> List.of(codeKey(required(r, "project_code", "现有项目代码"))), "现有项目代码");
        var existingProducts = index(oldProducts, this::productKey, "现有产品目录业务标识");
        int created = 0, updated = 0;
        for (var project : projects) {
            var old = existingProjects.get(List.of(codeKey(project.get("project_code"))));
            save(PROJECT, project, old, tenant);
            if (old == null) created++; else updated++;
        }
        for (var product : products) {
            var old = existingProducts.get(productKey(product));
            // Allocate a new global technical row number, never reuse another tenant's row.
            product.put("original_row_no", old == null ? String.valueOf(rowNumbers.allocate(product.get("data_source"))) : old.get("original_row_no"));
            save(PRODUCT, product, old, tenant);
            if (old == null) created++; else updated++;
        }
        for (var binding : bindings) {
            String projectId = binding.project().get("id"), productId = binding.product().get("id");
            boolean exists = false;
            for (var old : existingRelations) {
                if (!productId.equals(old.get("product_catalog_id"))) continue;
                if (!Set.of("ACTIVE", "CONFIRMED").contains(old.get("relation_status")))
                    throw invalid("产品与项目关系：产品已有非有效关系，请先处理：" + binding.product().get("product"));
                if (!projectId.equals(old.get("project_code_id")))
                    throw invalid("产品与项目关系：产品已有其他项目绑定：" + binding.product().get("product"));
                exists = true;
            }
            // Do not touch certificate IDs, confirmation history or approval request links.
            if (!exists) {
                if (jdbc.update("INSERT INTO dcc_data_relation (product_catalog_id,project_code_id,relation_status,relation_source,confirmed_by,confirmed_time,tenant_id,creator,updater) VALUES (?,?,'CONFIRMED','IMPORT',?,CURRENT_TIMESTAMP,?,?,?)",
                    productId, projectId, getLoginUserId(), tenant, actor(), actor()) != 1) throw invalid("产品与项目关系：新增失败");
                created++;
            } else updated++;
            if (jdbc.update("UPDATE dcc_product_catalog SET project_name=?,project_code=?,updater=?,update_time=CURRENT_TIMESTAMP WHERE id=? AND tenant_id=? AND deleted=0",
                binding.project().get("project_name"), binding.project().get("project_code"), actor(), productId, tenant) != 1)
                throw invalid("产品与项目关系：产品目录项目名称与代码同步失败");
        }
        return Map.of("created", created, "updated", updated);
    }

    private List<DccCatalogWorkbook.SheetData> schema() {
        return List.of(sheet(PROJECT, List.of()), sheet(PRODUCT, List.of()),
            new DccCatalogWorkbook.SheetData("产品与项目关系", RELATION_COLUMNS, List.of()));
    }
    private DccCatalogWorkbook.SheetData sheet(Spec spec, List<Map<String, String>> rows) {
        List<Map<String, String>> translated = new ArrayList<>();
        for (var row : rows) {
            var values = new LinkedHashMap<String, String>();
            for (int i = 0; i < spec.columns().size(); i++) values.put(spec.labels().get(i), row.get(spec.columns().get(i)));
            translated.add(values);
        }
        return new DccCatalogWorkbook.SheetData(spec.name(), spec.labels(), translated);
    }
    private List<Map<String, String>> decode(Spec spec, DccCatalogWorkbook.SheetData sheet) {
        List<Map<String, String>> result = new ArrayList<>();
        for (var row : sheet.rows()) {
            var values = new LinkedHashMap<String, String>();
            for (int i = 0; i < spec.columns().size(); i++) {
                String value = row.get(spec.labels().get(i));
                values.put(spec.columns().get(i), value == null || value.isBlank() ? null : value);
            }
            result.add(values);
        }
        return result;
    }
    private List<Map<String, String>> load(Spec spec, long tenant, boolean lock) {
        return jdbc.query("SELECT id," + spec.fields() + " FROM " + spec.table() + " WHERE tenant_id=? AND deleted=0"
            + (spec == PROJECT ? " AND library_scope='PROJECT'" : "") + " ORDER BY id" + (lock ? " FOR UPDATE" : ""),
            (rs, n) -> {
                var row = new LinkedHashMap<String, String>();
                row.put("id", rs.getString("id"));
                for (String field : spec.columns()) row.put(field, rs.getString(field));
                return row;
            }, tenant);
    }
    private List<Map<String, String>> loadRelations(long tenant, boolean lock) {
        List<Map<String, String>> result = new ArrayList<>();
        for (String table : List.of("dcc_project_product_relation", "dcc_data_relation")) {
            String status = table.equals("dcc_project_product_relation") ? "ACTIVE" : "CONFIRMED";
            result.addAll(jdbc.query("SELECT product_catalog_id,project_code_id,relation_status FROM " + table
                + " WHERE tenant_id=? AND deleted=0" + (lock ? "" : " AND relation_status=?") + " ORDER BY id" + (lock ? " FOR UPDATE" : ""),
                (rs, n) -> Map.of("product_catalog_id", rs.getString("product_catalog_id"), "project_code_id", rs.getString("project_code_id"), "relation_status", rs.getString("relation_status")),
                lock ? new Object[]{tenant} : new Object[]{tenant, status}));
        }
        return result;
    }
    private void save(Spec spec, Map<String, String> row, Map<String, String> old, long tenant) {
        List<String> fields = spec.columns();
        List<Object> args = fields.stream().map(row::get).collect(Collectors.toCollection(ArrayList::new));
        if (old == null) {
            args.add(tenant); args.add(actor()); args.add(actor());
            var holder = new GeneratedKeyHolder();
            int count = jdbc.update(connection -> {
                var statement = connection.prepareStatement("INSERT INTO " + spec.table() + " (" + spec.fields()
                    + ",tenant_id,creator,updater) VALUES (" + String.join(",", Collections.nCopies(args.size(), "?")) + ")", Statement.RETURN_GENERATED_KEYS);
                for (int i = 0; i < args.size(); i++) statement.setObject(i + 1, args.get(i));
                return statement;
            }, holder);
            if (count != 1 || holder.getKey() == null) throw invalid(spec.name() + "：新增失败");
            row.put("id", holder.getKey().toString());
        } else {
            args.add(actor()); args.add(old.get("id")); args.add(tenant);
            if (jdbc.update("UPDATE " + spec.table() + " SET " + fields.stream().map(f -> f + "=?").collect(Collectors.joining(","))
                + ",updater=?,update_time=CURRENT_TIMESTAMP WHERE id=? AND tenant_id=? AND deleted=0", args.toArray()) != 1)
                throw invalid(spec.name() + "：更新失败");
            row.put("id", old.get("id"));
        }
    }
    private List<String> productKey(Map<String, String> row) {
        String source = required(row, "data_source", "产品目录");
        String code = row.get("product_code");
        if (code != null && !code.isBlank()) return List.of(source, "编码", codeKey(code));
        return List.of(source, "名称", required(row, "product", "产品目录"),
            Objects.toString(row.get("registration_certificate_name"), ""), Objects.toString(row.get("registration_certificate_number"), ""), Objects.toString(row.get("registration_place"), ""));
    }
    private Map<List<String>, Map<String, String>> index(List<Map<String, String>> rows,
            java.util.function.Function<Map<String, String>, List<String>> key, String context) {
        Map<List<String>, Map<String, String>> result = new LinkedHashMap<>();
        for (var row : rows) {
            var identity = key.apply(row);
            if (result.putIfAbsent(identity, row) != null) throw invalid(context + "：存在重复，无法唯一匹配：" + identity);
        }
        return result;
    }
    private Map<String, Map<String, String>> byId(List<Map<String, String>> rows) {
        return rows.stream().collect(Collectors.toMap(r -> r.get("id"), r -> r));
    }
    private static boolean matches(Map<String, String> selector, String label, Map<String, String> row, String field) {
        String value = selector.get(label);
        return value == null || value.isBlank() || value.equals(row.get(field));
    }
    private static long tenant() {
        if (TenantContextHolder.isIgnore()) throw invalid("导入导出必须指定业务租户");
        return TenantContextHolder.getRequiredTenantId();
    }
    private static String required(Map<String, String> row, String field, String context) {
        String value = row.get(field);
        if (value == null || value.isBlank()) throw invalid(context + "：" + field + " 不能为空");
        if (!value.equals(value.trim())) throw invalid(context + "：" + field + " 不能包含首尾空格");
        return value;
    }
    private static String codeKey(String code) { return code.toLowerCase(Locale.ROOT); }
    private static String actor() { return Objects.toString(getLoginUserId(), ""); }
    private static IllegalArgumentException invalid(String message) { return new IllegalArgumentException(message); }
}
