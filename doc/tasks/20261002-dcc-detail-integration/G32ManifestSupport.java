package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.jackson.config.YudaoJacksonAutoConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.core.JsonParser;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** Task-only helper calls the actual compiled DCC canonical methods. No Spring app startup or mutation. */
public final class G32ManifestSupport {
    static final String UUID="92ca05d0-aec8-11f1-a944-02b4e226a5ef";
    static final ObjectMapper INPUT=new ObjectMapper().findAndRegisterModules()
        .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION).enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    static void configureCanonicalRuntime() {
        var formal=new YudaoJacksonAutoConfiguration();var builder=new Jackson2ObjectMapperBuilder();
        builder.featuresToEnable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        builder.featuresToDisable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS,com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS);
        formal.ldtEpochMillisCustomizer().customize(builder);builder.modulesToInstall(formal.timestampSupportModuleBean());formal.jsonUtils(builder.build());
        String nullProbe=JsonUtils.toJsonString(new TreeMap<String,Object>(){{put("nil",null);}});
        if(!"{\"nil\":null}".equals(nullProbe))throw new IllegalStateException("formal Boot canonical null inclusion differs from reviewed profile");
    }
    static Map<String,Object> typedRow(Map<String,Object> row) {
        var result=new LinkedHashMap<String,Object>();
        for(var entry:row.entrySet()) {
            if(entry.getValue()==null){result.put(entry.getKey(),null);continue;}
            var typed=(Map<String,Object>)entry.getValue();String type=(String)typed.get("jdbcType"),value=(String)typed.get("value");
            Object actual=switch(type) {
                case "String" -> value;
                case "Long" -> Long.valueOf(value);
                case "Integer" -> Integer.valueOf(value);
                case "Boolean" -> {if(!Set.of("true","false").contains(value))throw new IllegalArgumentException("invalid Boolean JDBC scalar");yield Boolean.valueOf(value);}
                case "BigDecimal" -> new java.math.BigDecimal(value);
                case "Timestamp" -> Timestamp.valueOf(value);
                case "Bytes" -> HexFormat.of().parseHex(value);
                default -> throw new IllegalArgumentException("unsupported actual JDBC value type");
            };result.put(entry.getKey(),actual);
        };return result;
    }
    public static void main(String[] args) {
        try {
            var factory=org.slf4j.LoggerFactory.getILoggerFactory();
            if(!(factory instanceof ch.qos.logback.classic.LoggerContext context))throw new IllegalStateException("reviewed task logger required");
            context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).setLevel(ch.qos.logback.classic.Level.OFF);
            run(args);
        } catch(Throwable failure) {
            System.out.println("{\"status\":\"G32_HELPER_FAILED\",\"errorType\":\""+failure.getClass().getSimpleName()+"\"}");
            System.exit(1);
        }
    }
    static void run(String[] args) throws Exception {
        configureCanonicalRuntime();
        if(args.length==1 && "hash-typed-rows".equals(args[0])) {
            byte[] input=System.in.readNBytes(8*1024*1024+1);if(input.length>8*1024*1024)throw new IllegalArgumentException("bounded typed rows required");
            var rows=INPUT.readValue(input,List.class);var hashes=new ArrayList<Map<String,Object>>();
            for(var raw:rows){var r=(Map<String,Object>)raw;hashes.add(Map.of("kind",r.get("kind"),"id",r.get("id"),"rowHash",DccLegacyNameVerifiedScope.rowHash(typedRow((Map<String,Object>)r.get("values")))));}
            System.out.println(INPUT.writeValueAsString(Map.of("status","JAVA_CANONICAL_ROW_HASH_OFFLINE","protocol","DccLegacyNameVerifiedScope.rowHash/BootJackson.v1","rows",hashes)));return;
        }
        if(args.length==7 && "verify-manifest".equals(args[0])) {
            DccLegacyNameVerifiedScope.fromProtectedArtifact(Path.of(args[1]),args[2],Path.of(args[3]),Path.of(args[4]),Path.of(args[5]),Path.of(args[6]));
            System.out.println("ACTUAL_STRICT_JAVA_FACTORY_PASS_OFFLINE_NO_ACTIVATION");return;
        }
        if(args.length==1 && "collect-preimages-readonly".equals(args[0])) {
            byte[] body=System.in.readNBytes(8*1024*1024+1);if(body.length>8*1024*1024)throw new IllegalArgumentException("bounded root input required");
            var input=INPUT.readValue(body,Map.class);String url=(String)input.get("jdbcUrl");
            if(!"jdbc:mysql://127.0.0.1:23306/ruoyi-vue-pro?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai".equals(url))throw new IllegalArgumentException("exact reviewed local JDBC source required");
            try(var connection=DriverManager.getConnection(url,(String)input.get("username"),(String)input.get("password"))) {
                connection.setReadOnly(true);connection.setAutoCommit(false);
                connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
                var jdbc=new JdbcTemplate(new SingleConnectionDataSource(connection,true));
                var env=jdbc.queryForMap("SELECT DATABASE() AS db,@@server_uuid AS uuid,@@version AS version");
                if(!"ruoyi-vue-pro".equals(env.get("db")) || !UUID.equals(env.get("uuid")) || !"8.0.40".equals(env.get("version")))throw new IllegalStateException("actual local source identity differs");
                var outputRows=new ArrayList<Map<String,Object>>();var locators=new ArrayList<Map<String,Object>>();
                var groups=(Map<String,Object>)input.get("exactScopeIds");
                for(String kind:List.of("claim","master","version","storage")) {
                    var ids=(List<String>)groups.get(kind);if(ids==null || ids.isEmpty() || new HashSet<>(ids).size()!=ids.size() || ids.stream().anyMatch(v->!v.matches("[1-9][0-9]*") || v.length()>19 || Long.parseLong(v)<1))throw new IllegalArgumentException("exact scoped Long string IDs required");
                    if(ids.size()!=(Set.of("claim","master").contains(kind)?25:39))throw new IllegalArgumentException("exact reviewed25/25/39/39 original scope required");
                    String table=switch(kind){case "claim"->"dcc_controlled_file_name_claim";case "master"->"dcc_controlled_file_master";case "version"->"dcc_controlled_file";default->"infra_file";};
                    String sql="SELECT * FROM "+table+" WHERE "+("storage".equals(kind)?"":"tenant_id=1 AND ")+"deleted=0 AND id IN ("+String.join(",",ids)+") ORDER BY id";
                    var rows=jdbc.queryForList(sql);if(rows.size()!=ids.size())throw new IllegalStateException("current preimage full scope missing");
                    for(var row:rows){String id=String.valueOf(row.get("id"));var item=new TreeMap<String,Object>();item.put("kind",kind);item.put("id",id);item.put("rowHash",DccLegacyNameVerifiedScope.rowHash(row));
                        var identity=new TreeMap<String,Object>();
                        List<String> fields=switch(kind){case "claim"->List.of("tenant_id","master_id","normalized_name","source_original_file_name","dcc_project_code_id","file_type_taxonomy_leaf_id","normalized_file_number");case "master"->List.of("tenant_id","dcc_project_code_id","file_type_taxonomy_leaf_id","normalized_file_number");case "version"->List.of("tenant_id","master_id","source_file_id","source_sha256","version_no","process_instance_id","source_original_file_name");default->List.of("config_id","name","path","size");};
                        for(var field:fields)identity.put(field,row.get(field)==null?null:String.valueOf(row.get(field)));item.put("identity",identity);outputRows.add(item);
                        if("storage".equals(kind)){var locator=new TreeMap<String,Object>();locator.put("id",id);locator.put("configId",String.valueOf(row.get("config_id")));locator.put("path",row.get("path"));locator.put("name",row.get("name"));locator.put("size",String.valueOf(row.get("size")));locators.add(locator);}
                    }
                }
                var config=jdbc.queryForMap("SELECT id,storage,config FROM infra_file_config WHERE id=28 AND deleted=0");var actualConfig=INPUT.readValue(String.valueOf(config.get("config")),Map.class);
                var safeConfig=new TreeMap<String,Object>();safeConfig.put("id","28");safeConfig.put("storage",config.get("storage"));for(String field:List.of("endpoint","bucket","region"))safeConfig.put(field,actualConfig.get(field));safeConfig.put("pathStyle",actualConfig.get("enablePathStyleAccess"));
                var receipt=new TreeMap<String,Object>();receipt.put("status","ACTUAL_JDBC_JAVA_ROW_HASH_RECEIPT");receipt.put("database","ruoyi-vue-pro");receipt.put("serverUuid",UUID);receipt.put("tenantId","1");receipt.put("canonicalProtocol","DccLegacyNameVerifiedScope.rowHash/BootJackson.v1");receipt.put("factsSha256",input.get("factsSha256"));receipt.put("rows",outputRows);receipt.put("storage",locators);receipt.put("config",safeConfig);receipt.put("mapperNullProbe","{\"nil\":null}");receipt.put("capturedAtUtc",java.time.Instant.now().toString());receipt.put("jdbcRuntimeVersion",env.get("version"));
                receipt.put("actualReadOnlyConnection",connection.isReadOnly());receipt.put("snapshotIsolation","REPEATABLE_READ");System.out.println(INPUT.writeValueAsString(receipt));connection.rollback();
            };return;
        }
        throw new IllegalArgumentException("expected offline hash/factory mode or Root explicit readonly preimage collect mode");
    }
}
