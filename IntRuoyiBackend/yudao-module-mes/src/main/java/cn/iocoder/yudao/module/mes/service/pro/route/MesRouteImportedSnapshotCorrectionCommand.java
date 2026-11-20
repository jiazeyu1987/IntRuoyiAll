package cn.iocoder.yudao.module.mes.service.pro.route;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.sql.*;
import java.util.*;

/** Offline, operator-authorized exception. Original bytes must already be sealed outside the database. */
public final class MesRouteImportedSnapshotCorrectionCommand {
    private static final ObjectMapper JSON=new ObjectMapper();
    private static final String ACTION="CORRECT_IMPORTED_SNAPSHOT_INDEX";
    private static void require(boolean condition,String reason) {if(!condition) throw new IllegalArgumentException(reason);}
    private static String identifier(String value) {
        require(value.matches("[a-z_0-9]+"),"unsafe database identifier"); return "`"+value+"`";
    }
    static String eventKey(JsonNode row) {return "route-import-index-correction:v1:"+row.path("id").asText()+":"+row.path("originalSha256").asText();}
    static String auditReason(String planSha,String artifact) {
        require(planSha.matches("[a-f0-9]{64}") && artifact.matches("/[A-Za-z0-9_./-]+") && !artifact.contains(".."),"sealed plan identity required");
        String result="User-authorized correction 20261119-update-and-release-prod; original JSON and frozen identity retained; planSha256="+planSha+"; artifact="+artifact;
        require(result.length()<=1024,"audit reason exceeds schema");return result;
    }
    private static List<Map<String,String>> query(Connection connection,String sql,Object... args) throws SQLException {
        try(PreparedStatement statement=connection.prepareStatement(sql)) {
            for(int i=0;i<args.length;i++) statement.setObject(i+1,args[i]);
            try(ResultSet result=statement.executeQuery()) {
                var rows=new ArrayList<Map<String,String>>(); var metadata=result.getMetaData();
                while(result.next()) {Map<String,String> row=new LinkedHashMap<>();for(int i=1;i<=metadata.getColumnCount();i++) row.put(metadata.getColumnLabel(i),result.getString(i));rows.add(row);}
                return rows;
            }
        }
    }
    private static long count(Connection c,String sql,Object... args) throws SQLException {
        var rows=query(c,sql,args);require(rows.size()==1 && rows.get(0).size()==1,"scalar database result required");
        return Long.parseLong(rows.get(0).values().iterator().next());
    }
    private static void noMatches(Connection c,String sql,Object... args) throws SQLException {require(count(c,sql,args)==0,"signature or downstream reference blocks correction");}

    private static void references(Connection c,JsonNode plan) throws SQLException {
        String ids=String.join(",", iterable(plan.path("corrections")).stream().map(row->row.path("id").asText()).toList());
        Set<String> projections=Set.of("mes_pro_route_schedule_config","mes_pro_route_process_loss_reason","mes_pro_route_device_identity_snapshot");
        var fields=query(c,"SELECT table_name,column_name FROM information_schema.columns WHERE table_schema=DATABASE() AND (column_name LIKE '%route_version_id' OR column_name IN ('content_hash','before_content_hash','after_content_hash','route_binding_snapshot_hash','route_snapshot_json')) ORDER BY table_name,ordinal_position");
        Set<String> engines=new HashSet<>(Set.of("mes_pro_route_version","controlled_content_version_ref","controlled_content_transition_audit","system_electronic_signature"));
        for(var field:fields) {
            String table=field.get("TABLE_NAME"),column=field.get("COLUMN_NAME");
            // Drivers may preserve lowercase information_schema column labels.
            if(table==null) {table=field.get("table_name");column=field.get("column_name");}
            engines.add(table);
            if(column.endsWith("route_version_id")) {
                if(!projections.contains(table)) noMatches(c,"SELECT COUNT(*) FROM "+identifier(table)+" WHERE "+identifier(column)+" IN ("+ids+")");
            } else if(column.equals("route_snapshot_json")) {
                if(!table.equals("mes_pro_route_version")) for(JsonNode row:plan.path("corrections"))
                    noMatches(c,"SELECT COUNT(*) FROM "+identifier(table)+" WHERE LOCATE(?,CAST("+identifier(column)+" AS CHAR))>0",row.path("id").asText());
            } else for(JsonNode row:plan.path("corrections"))
                noMatches(c,"SELECT COUNT(*) FROM "+identifier(table)+" WHERE "+identifier(column)+"=?",row.path("originalSha256").asText());
        }
        for(String table:engines) {
            require("InnoDB".equalsIgnoreCase(query(c,"SELECT engine FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name=?",table).get(0).values().iterator().next()),"transactional reference tables required");
        }
        // Signatures can bind a subject ID or embed it in canonical before/after content.
        for(var field:query(c,"DESCRIBE system_electronic_signature")) {
            String column=field.get("Field"),type=field.get("Type");
            if(type.contains("char") || type.contains("text") || type.equals("json") || type.startsWith("bigint"))
                for(JsonNode row:plan.path("corrections")) for(String term:List.of(row.path("id").asText(),row.path("originalSha256").asText()))
                    noMatches(c,"SELECT COUNT(*) FROM system_electronic_signature WHERE LOCATE(?,CAST("+identifier(column)+" AS CHAR))>0",term);
        }
        noMatches(c,"SELECT COUNT(*) FROM controlled_content_version_ref WHERE source_native_version_id IN ("+ids+") OR successor_native_version_id IN ("+ids+")");
        noMatches(c,"SELECT COUNT(*) FROM controlled_content_version_ref WHERE source_version_ref_id IN (SELECT id FROM (SELECT id FROM controlled_content_version_ref WHERE content_type='MES_ROUTE' AND native_version_id IN ("+ids+")) r) OR successor_version_ref_id IN (SELECT id FROM (SELECT id FROM controlled_content_version_ref WHERE content_type='MES_ROUTE' AND native_version_id IN ("+ids+")) r)");
    }
    private static List<JsonNode> iterable(JsonNode rows) {List<JsonNode> result=new ArrayList<>();rows.forEach(result::add);return result;}

    public static Map<String,Object> execute(Connection connection,JsonNode suppliedPlan,String planSha,String artifact,boolean apply) throws Exception {
        require(connection.getAutoCommit() && MesRouteImportedSnapshotCorrection.CONTRACT.equals(suppliedPlan.path("contract").asText()),"fresh connection and explicit correction contract required");
        String reason=auditReason(planSha,artifact);
        connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
        connection.setReadOnly(!apply);connection.setAutoCommit(false);
        try {
            ArrayNode originalRows=JSON.createArrayNode();Map<Long,List<Map<String,String>>> originals=new LinkedHashMap<>();
            Map<Long,List<Map<String,String>>> registered=new LinkedHashMap<>();
            for(JsonNode row:suppliedPlan.path("corrections")) {
                long id=row.path("id").asLong();
                var selected=query(connection,"SELECT * FROM mes_pro_route_version WHERE id=?"+(apply?" FOR UPDATE":""),id);
                require(selected.size()==1,"original version missing");originals.put(id,selected);
                ObjectNode original=JSON.createObjectNode();selected.get(0).forEach((key,value)->{if(value==null)original.putNull(key);else if(Set.of("id","route_id","tenant_id","active","deleted").contains(key))original.put(key,Long.parseLong(value));else original.put(key,value);});
                originalRows.add(original);
                var refs=query(connection,"SELECT * FROM controlled_content_version_ref WHERE content_type='MES_ROUTE' AND native_version_id=?"+(apply?" FOR UPDATE":""),id);
                require(refs.size()==1,"unique controlled registration required");var ref=refs.get(0);
                require("163".equals(ref.get("tenant_id")) && "ACTIVE".equals(ref.get("canonical_status")) && "ACTIVE".equals(ref.get("domain_status"))
                    && ref.get("approval_process_instance_id")==null && ref.get("source_native_version_id")==null && ref.get("successor_native_version_id")==null
                    && ref.get("source_version_ref_id")==null && ref.get("successor_version_ref_id")==null && "0".equals(ref.get("deleted")),"controlled lifecycle changed");
                registered.put(id,refs);
                noMatches(connection,"SELECT COUNT(*) FROM controlled_content_transition_audit WHERE event_key=?",eventKey(row));
                require(count(connection,"SELECT COUNT(*) FROM controlled_content_transition_audit WHERE version_ref_id=? AND tenant_id=163 AND action='REGISTER_ACTIVE' AND reason='跨租户流程关联资源导入'",ref.get("id"))==1,"original import provenance required");
            }
            JsonNode fresh=JSON.valueToTree(MesRouteImportedSnapshotCorrection.plan(originalRows));
            require(fresh.equals(suppliedPlan),"sealed plan differs from fresh authoritative calculation");
            references(connection,fresh);
            int updated=0;
            if(apply) for(JsonNode row:fresh.path("corrections")) {
                long id=row.path("id").asLong();var ref=registered.get(id).get(0);
                try(PreparedStatement audit=connection.prepareStatement("INSERT INTO controlled_content_transition_audit (tenant_id,version_ref_id,content_type,content_key,from_status,to_status,domain_from_status,domain_to_status,action,event_key,actor_id,reason) VALUES (163,?,'MES_ROUTE',?,'ACTIVE','ACTIVE','ACTIVE','ACTIVE',?,?,NULL,?)")) {
                    audit.setString(1,ref.get("id"));audit.setString(2,ref.get("content_key"));audit.setString(3,ACTION);audit.setString(4,eventKey(row));audit.setString(5,reason);
                    require(audit.executeUpdate()==1,"correction audit insert failed");
                }
                try(PreparedStatement update=connection.prepareStatement("UPDATE mes_pro_route_version SET route_snapshot_json=?,route_snapshot_sha256=?,update_time=update_time WHERE id=? AND tenant_id=163 AND route_snapshot_sha256=? AND BINARY route_snapshot_json=BINARY ?")) {
                    update.setString(1,row.path("correctedJson").asText());update.setString(2,row.path("correctedSha256").asText());update.setLong(3,id);update.setString(4,row.path("originalSha256").asText());update.setString(5,row.path("originalJson").asText());
                    require(update.executeUpdate()==1,"frozen original changed concurrently");updated++;
                }
                var after=query(connection,"SELECT * FROM mes_pro_route_version WHERE id=?",id).get(0);
                var expected=new LinkedHashMap<>(originals.get(id).get(0));expected.put("route_snapshot_json",row.path("correctedJson").asText());expected.put("route_snapshot_sha256",row.path("correctedSha256").asText());
                require(expected.equals(after),"unapproved version metadata changed");
                require(registered.get(id).equals(query(connection,"SELECT * FROM controlled_content_version_ref WHERE content_type='MES_ROUTE' AND native_version_id=?",id)),"controlled registration changed");
            }
            if(apply) connection.commit();else connection.rollback();
            return Map.of("contract",MesRouteImportedSnapshotCorrection.CONTRACT,"status",apply?"CORRECTED":"READY","rowCount",5,"indexCount",74,"updatedCount",updated,"planSha256",planSha);
        } catch(Exception failure) {connection.rollback();throw failure;}
    }
    public static void main(String[] args) throws Exception {
        require(args.length==3 && args[0].startsWith("--plan=") && args[1].startsWith("--report=") && Set.of("--dry-run","--apply").contains(args[2]),"explicit plan/report/mode required");
        Path path=Path.of(args[0].substring(7)),report=Path.of(args[1].substring(9));
        require(path.isAbsolute() && report.isAbsolute() && !Files.exists(report),"fresh private artifact paths required");
        byte[] bytes=Files.readAllBytes(path);String sha=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        try(Connection connection=DriverManager.getConnection(System.getenv("MES_SNAPSHOT_JDBC_URL"),System.getenv("MES_SNAPSHOT_JDBC_USER"),System.getenv("MES_SNAPSHOT_JDBC_PASSWORD"))) {
            String artifact = System.getenv("MES_SNAPSHOT_ARTIFACT_PATH");
            require(artifact != null, "explicit sealed original artifact path required");
            Files.writeString(report,JSON.writeValueAsString(execute(connection,JSON.readTree(bytes),sha,artifact,args[2].equals("--apply"))),StandardOpenOption.CREATE_NEW);
        } catch(Exception failure) {
            Files.writeString(report,JSON.writeValueAsString(Map.of("status","BLOCKED","errorCode",failure instanceof SQLException?"DATABASE_COMMAND_FAILED":"CORRECTION_CONTRACT_FAILED")),StandardOpenOption.CREATE_NEW);
            System.err.println("CORRECTION_BLOCKED: inspect bound private report");System.exit(2);
        }
    }
}
