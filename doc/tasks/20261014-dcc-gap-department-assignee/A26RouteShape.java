import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** Storage contract only. PUBLIC legality is checked separately by actual production source code. */
public class A26RouteShape {
    interface Check{void run()throws Exception;}
    static Connection db;static int passed,failed;
    static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    static void check(String name,Check check){try{check.run();passed++;System.out.println("PASS "+name);}catch(Exception|AssertionError failure){failed++;System.out.println("FAIL "+name+": "+failure);}}
    static void insert(long id,Integer version,boolean include)throws Exception {
        try(var sql=db.prepareStatement("INSERT INTO dcc_controlled_file_route_snapshot(id,controlled_file_id,stage_no,candidate_source_type,approve_method"+(include?",route_version_no":"")+") VALUES(?,?,2,'DEPT','ALL'"+(include?",?":"")+")")) {
            sql.setLong(1,id);sql.setLong(2,id);if(include)sql.setObject(3,version);require(sql.executeUpdate()==1,"one exact row");
        }
    }
    static String originalPublicRow()throws Exception {
        try(var sql=db.createStatement();var rows=sql.executeQuery("SELECT id,controlled_file_id,route_version_no,stage_no,candidate_source_type,approve_method FROM dcc_controlled_file_route_snapshot WHERE id=1")) {
            require(rows.next(),"public row absent");return rows.getLong(1)+"|"+rows.getLong(2)+"|"+rows.getInt(3)+"|"+rows.getInt(4)+"|"+rows.getString(5)+"|"+rows.getString(6);
        }
    }
    public static void main(String[] args)throws Exception {
        try(var connection=DriverManager.getConnection("jdbc:h2:mem:a26_route;MODE=MYSQL;DATABASE_TO_UPPER=false;NON_KEYWORDS=value;","sa","")) {
            db=connection;try(var sql=db.createStatement()){sql.execute(Files.readString(Path.of(args[0])));}
            insert(1,7,true);String before=originalPublicRow();
            check("route-version.INTEGER.NULL.no-default",()->{try(var column=db.getMetaData().getColumns(null,null,"dcc_controlled_file_route_snapshot","route_version_no")){
                require(column.next(),"route version absent");require("INTEGER".equals(column.getString("TYPE_NAME")),"INT type unchanged");require(column.getInt("NULLABLE")==DatabaseMetaData.columnNullable,"custom needs NULL");require(column.getString("COLUMN_DEF")==null,"no fake default1");
            }});
            check("custom.explicit-NULL-stays-NULL",()->{insert(2,null,true);try(var sql=db.createStatement();var row=sql.executeQuery("SELECT route_version_no FROM dcc_controlled_file_route_snapshot WHERE id=2")){require(row.next()&&row.getObject(1)==null,"custom public version must remain null");}});
            check("omitted-version.no-fake-default",()->{insert(3,null,false);try(var sql=db.createStatement();var row=sql.executeQuery("SELECT route_version_no FROM dcc_controlled_file_route_snapshot WHERE id=3")){require(row.next()&&row.getObject(1)==null,"no version1 invention");}});
            check("existing-public-real7-row-exactly-unchanged",()->require(before.equals(originalPublicRow()),"old public row changed"));
        }
        System.out.println("A26 H2 SUMMARY pass="+passed+" fail="+failed+"; NULL schema ability is not PUBLIC business authorization");
        if(failed!=0)System.exit(1);
    }
}
