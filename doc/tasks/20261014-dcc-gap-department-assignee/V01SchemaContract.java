import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;

/** Runs only task-owned in-memory H2 DDL/data. No application service or business DB connection. */
public class V01SchemaContract {
    interface Check { void run() throws Exception; }
    static int passed, failed;
    static Connection db;
    static void check(String name, Check check) {
        try { check.run(); passed++; System.out.println("CASE\t"+name+"\tPASS"); }
        catch(Exception | AssertionError problem) { failed++; System.out.println("CASE\t"+name+"\tFAIL\t"+problem); }
    }
    static void require(boolean condition, String message) { if(!condition) throw new AssertionError(message); }
    static void column(String table, String name, String type, int size, boolean nullable) throws Exception {
        try(var columns=db.getMetaData().getColumns(null,null,table,name)) {
            require(columns.next(), "missing "+table+"."+name);
            require(type.equals(columns.getString("TYPE_NAME")),name+" type="+columns.getString("TYPE_NAME"));
            if(size>0) require(columns.getInt(type.equals("TIMESTAMP")?"DECIMAL_DIGITS":"COLUMN_SIZE")==size,name+" size/precision");
            require(columns.getInt("NULLABLE")== (nullable?DatabaseMetaData.columnNullable:DatabaseMetaData.columnNoNulls),name+" nullable");
            require(columns.getString("COLUMN_DEF")==null,name+" must not have a default");
        }
    }
    static void insert(String table, Map<String,Object> values) throws Exception {
        String sql="INSERT INTO "+table+" ("+String.join(",",values.keySet())+") VALUES ("+String.join(",",Collections.nCopies(values.size(),"?"))+")";
        try(var command=db.prepareStatement(sql)) {
            int position=1;for(var value:values.values())command.setObject(position++,value);
            require(command.executeUpdate()==1,"one inserted row required");
        }
    }
    static void rejectsMissing(String mode) throws Exception {
        var data=new LinkedHashMap<String,Object>();data.put("id",300L);data.put("code","MISSING");data.put("name","缺失来源");data.put("source","LOCAL");data.put("lifecycle_stage","PLAN");
        if(mode.equals("NULL"))data.put("approval_source_mode",null);
        try { insert("dcc_file_category",data); throw new AssertionError("missing source was accepted/defaulted"); }
        catch(SQLException rejected) { require("23502".equals(rejected.getSQLState()),"expected NOT NULL rejection, got "+rejected.getSQLState()); }
    }
    static Map<String,Object> file(long id) {
        var values=new LinkedHashMap<String,Object>();values.put("id",id);values.put("master_id",1L);values.put("category_id",1L);
        values.put("directory_id",1L);values.put("source_file_id",10L);values.put("original_file_id",10L);values.put("file_name","V01-"+id+".pdf");
        values.put("title","V01");values.put("file_number","V01-"+id);values.put("version_no","A/1");values.put("status","PENDING_MATRIX_REVIEW");values.put("submitter_id",7L);values.put("requester_id",7L);
        return values;
    }
    static Map<String,Object> projection(long id,LocalDateTime time) {
        var row=new LinkedHashMap<String,Object>();row.put("id",id);row.put("controlled_file_id",id);row.put("task_id","V01-"+id);row.put("actor_id",7L);
        row.put("action_type","APPROVE");row.put("signature_mode","PASSWORD");row.put("signed_at",time);return row;
    }
    static Map<String,Object> unified(long id,LocalDateTime time) {
        var row=new LinkedHashMap<String,Object>();row.put("id",id);row.put("tenant_id",1L);row.put("actor_id",7L);
        for(var field:List.of("module_code","action_code","subject_type","subject_id","subject_version","meaning_code","meaning_label","reason","time_evidence_id","authentication_method","canonical_content_json","algorithm","key_version","policy_version","verification_status","idempotency_key"))row.put(field,"V01-"+id);
        for(var field:List.of("content_hash","evidence_hash","command_hash"))row.put(field,"a".repeat(64));
        row.put("signed_at",time);return row;
    }
    static void exact(String table,long id,String column,Object expected) throws Exception {
        try(var statement=db.prepareStatement("SELECT "+column+" FROM "+table+" WHERE id=?")) {
            statement.setLong(1,id);try(var result=statement.executeQuery()) {
                require(result.next(),"row absent");Object value=expected instanceof LocalDateTime ? result.getObject(1,LocalDateTime.class) : result.getObject(1);
                require(Objects.equals(expected,value),table+"."+column+" expected="+expected+" actual="+value);
            }
        }
    }
    public static void main(String[] args) throws Exception {
        // The mode/casing/non-keyword options equal the existing unit-test profiles; global files are untouched.
        try(var connection=DriverManager.getConnection("jdbc:h2:mem:v01_"+UUID.randomUUID()+";MODE=MYSQL;DATABASE_TO_UPPER=false;NON_KEYWORDS=value;","sa","")) {
            db=connection;
            for(String ddl:args)try(var command=db.createStatement()){command.execute(Files.readString(Path.of(ddl)));}
            check("file.delayed_effective_time",()->column("dcc_controlled_file","delayed_effective_time","TIMESTAMP",6,true));
            check("file.delayed_effective_confirmed_at",()->column("dcc_controlled_file","delayed_effective_confirmed_at","TIMESTAMP",6,true));
            check("file.delayed_effective_signature_id",()->column("dcc_controlled_file","delayed_effective_signature_id","BIGINT",0,true));
            check("file.delayed_effective_process_instance_id",()->column("dcc_controlled_file","delayed_effective_process_instance_id","CHARACTER VARYING",128,true));
            check("projection.selected_effective_time",()->column("dcc_controlled_file_signature","selected_effective_time","TIMESTAMP",6,true));
            check("projection.attribute_round",()->column("dcc_controlled_file_signature","attribute_round","INTEGER",0,true));
            check("unified.signed_at.precision",()->column("system_electronic_signature","signed_at","TIMESTAMP",6,false));
            check("projection.signed_at.precision",()->column("dcc_controlled_file_signature","signed_at","TIMESTAMP",6,true));
            check("file.controlled_time.precision",()->column("dcc_controlled_file","controlled_time","TIMESTAMP",6,true));
            check("category.approval_source_mode.required.no-default",()->column("dcc_file_category","approval_source_mode","CHARACTER VARYING",32,false));
            check("category.missing-source-rejected",()->rejectsMissing("OMIT"));
            check("category.null-source-rejected",()->rejectsMissing("NULL"));
            check("category.explicit-two-sources-roundtrip",()->{
                long id=1;for(String mode:List.of("PUBLIC_MATRIX","APPLICATION_CUSTOM")) {
                    insert("dcc_file_category",Map.of("id",id,"code","V01-"+id,"name",mode,"source","LOCAL","lifecycle_stage","PLAN","approval_source_mode",mode));
                    exact("dcc_file_category",id++,"approval_source_mode",mode);
                }
            });
            check("new-confirmation.null-history-and-identity-roundtrip",()->{
                insert("dcc_controlled_file",file(101));
                for(String name:List.of("delayed_effective_time","delayed_effective_confirmed_at","delayed_effective_signature_id","delayed_effective_process_instance_id"))exact("dcc_controlled_file",101,name,null);
                var time=LocalDateTime.of(2026,10,15,10,20,30,123456000);var selected=time.plusDays(2).withNano(654321000);
                var row=file(102);row.put("delayed_effective_time",selected);row.put("delayed_effective_confirmed_at",time);row.put("delayed_effective_signature_id",9007199254740993L);row.put("delayed_effective_process_instance_id","exact-confirmation-round");row.put("controlled_time",time);
                insert("dcc_controlled_file",row);exact("dcc_controlled_file",102,"delayed_effective_signature_id",9007199254740993L);exact("dcc_controlled_file",102,"delayed_effective_process_instance_id","exact-confirmation-round");
                exact("dcc_controlled_file",102,"delayed_effective_time",selected);exact("dcc_controlled_file",102,"delayed_effective_confirmed_at",time);exact("dcc_controlled_file",102,"controlled_time",time);
                var signature=projection(102,time);signature.put("selected_effective_time",selected);signature.put("attribute_round",3);
                insert("dcc_controlled_file_signature",signature);exact("dcc_controlled_file_signature",102,"selected_effective_time",selected);exact("dcc_controlled_file_signature",102,"attribute_round",3);
            });
            check("C05.microsecond-S-preserved-in-four-places",()->{
                var signed=LocalDateTime.of(2026,10,15,11,12,13,987654000);var row=file(103);row.put("controlled_time",signed);row.put("delayed_effective_confirmed_at",signed);
                insert("dcc_controlled_file",row);insert("dcc_controlled_file_signature",projection(103,signed));insert("system_electronic_signature",unified(103,signed));
                exact("dcc_controlled_file",103,"controlled_time",signed);exact("dcc_controlled_file",103,"delayed_effective_confirmed_at",signed);exact("dcc_controlled_file_signature",103,"signed_at",signed);exact("system_electronic_signature",103,"signed_at",signed);
            });
            check("C05.legacy-second-values-stay-identical",()->{
                var signed=LocalDateTime.of(2026,9,1,12,0,0);var row=file(104);row.put("controlled_time",signed);
                insert("dcc_controlled_file",row);insert("dcc_controlled_file_signature",projection(104,signed));insert("system_electronic_signature",unified(104,signed));
                exact("dcc_controlled_file",104,"controlled_time",signed);exact("dcc_controlled_file_signature",104,"signed_at",signed);exact("system_electronic_signature",104,"signed_at",signed);
            });
        }
        System.out.println("H2 SUMMARY pass="+passed+" fail="+failed);
        if(failed!=0)System.exit(1);
    }
}
