import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** Exact schema storage vectors only. Does not execute the custom-matrix application workflow. */
public class A23DraftSchema {
    interface Check{void run()throws Exception;}
    static Connection db;static int passed,failed;
    static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    static void check(String name,Check check){try{check.run();passed++;System.out.println("PASS "+name);}catch(Exception|AssertionError failure){failed++;System.out.println("FAIL "+name+": "+failure);}}
    static void metadata(String column,String type,int size)throws Exception {
        try(var rows=db.getMetaData().getColumns(null,null,"dcc_controlled_file",column)) {
            require(rows.next(),"column absent: "+column);require(type.equals(rows.getString("TYPE_NAME")),"unexpected type="+rows.getString("TYPE_NAME"));
            require(rows.getInt("COLUMN_SIZE")==size,"unexpected size="+rows.getInt("COLUMN_SIZE"));
            require(rows.getInt("NULLABLE")==DatabaseMetaData.columnNullable,"nullable required");require(rows.getString("COLUMN_DEF")==null,"no schema default");
        }
    }
    static void insert(long id,String mode,String json,String context,boolean include)throws Exception {
        String columns="id,master_id,category_id,directory_id,source_file_id,original_file_id,file_name,title,file_number,version_no,status,submitter_id,requester_id";
        String values="?,1,1,1,1,1,?,'A23 storage vector',?,'A/1','WORKING',7,7";
        if(include){columns+=",draft_approval_source_mode,draft_application_custom_matrix_json,draft_application_context_key";values+=",?,?,?";}
        try(var sql=db.prepareStatement("INSERT INTO dcc_controlled_file("+columns+") VALUES("+values+")")) {
            sql.setLong(1,id);sql.setString(2,"A23-"+id+".pdf");sql.setString(3,"A23-"+id);
            if(include){sql.setString(4,mode);sql.setString(5,json);sql.setString(6,context);}
            require(sql.executeUpdate()==1,"exact inserted row");
        }
    }
    static void exact(long id,String mode,String json,String context)throws Exception {
        try(var sql=db.prepareStatement("SELECT draft_approval_source_mode,draft_application_custom_matrix_json,draft_application_context_key FROM dcc_controlled_file WHERE id=?")) {
            sql.setLong(1,id);try(var row=sql.executeQuery()) {
                require(row.next(),"row absent");require(Objects.equals(mode,row.getString(1)),"mode differs");require(Objects.equals(json,row.getString(2)),"json differs");require(Objects.equals(context,row.getString(3)),"context differs");
            }
        }
    }
    public static void main(String[] args)throws Exception {
        try(var connection=DriverManager.getConnection("jdbc:h2:mem:a23_draft;MODE=MYSQL;DATABASE_TO_UPPER=false;NON_KEYWORDS=value;","sa","")) {
            db=connection;try(var sql=db.createStatement()){sql.execute(Files.readString(Path.of(args[0])));}
            check("mode.VARCHAR32.nullable.no-default",()->metadata("draft_approval_source_mode","CHARACTER VARYING",32));
            check("json.LONGTEXT.nullable.no-default",()->metadata("draft_application_custom_matrix_json","CHARACTER VARYING",1_000_000_000));
            check("context.VARCHAR128.nullable.no-default",()->metadata("draft_application_context_key","CHARACTER VARYING",128));
            check("legacy.omitted-fields-stay-null",()->{insert(1,null,null,null,false);exact(1,null,null,null);});
            check("custom.large-unicode-JSON-exact-roundtrip",()->{
                String vector="{\"storageVector\":\""+"审计汉字".repeat(20000)+"\"}";
                insert(2,"APPLICATION_CUSTOM",vector,"opaque-custom-draft-2",true);exact(2,"APPLICATION_CUSTOM",vector,"opaque-custom-draft-2");
            });
            check("separate-rows-contexts-and-mode-preserved",()->{
                insert(3,"PUBLIC_MATRIX",null,"opaque-public-draft-3",true);insert(4,"APPLICATION_CUSTOM","{\"storageVector\":4}","opaque-custom-draft-4",true);
                exact(3,"PUBLIC_MATRIX",null,"opaque-public-draft-3");exact(4,"APPLICATION_CUSTOM","{\"storageVector\":4}","opaque-custom-draft-4");
            });
            check("context128-preserved-and129-rejected",()->{
                insert(5,"APPLICATION_CUSTOM","{}","c".repeat(128),true);exact(5,"APPLICATION_CUSTOM","{}","c".repeat(128));
                try{insert(6,"APPLICATION_CUSTOM","{}","c".repeat(129),true);throw new AssertionError("over128 accepted/truncated");}
                catch(SQLException expected){require("22001".equals(expected.getSQLState()),"exact length rejection="+expected.getSQLState());}
            });
        }
        System.out.println("A23 H2 SUMMARY pass="+passed+" fail="+failed+"; storage only; no application/round/replay/JUnit claim");
        if(failed!=0)System.exit(1);
    }
}
