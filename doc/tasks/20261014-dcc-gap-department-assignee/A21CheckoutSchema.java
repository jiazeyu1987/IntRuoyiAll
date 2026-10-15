import java.nio.file.*;
import java.sql.*;

/** Checkout column storage vectors only; never a claim of server canonical hash validity or replay approval. */
public class A21CheckoutSchema {
    interface Check {void run()throws Exception;}
    static Connection db;static int passed,failed;
    static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    static void check(String name,Check check){try{check.run();passed++;System.out.println("PASS "+name);}catch(Exception|AssertionError failure){failed++;System.out.println("FAIL "+name+": "+failure);}}
    static void insert(long id,String value,boolean include)throws Exception {
        String columns="id,tenant_id,master_id,base_iteration_id,actor_id,reason,status"+(include?",checkin_payload_hash":"");
        try(var sql=db.prepareStatement("INSERT INTO dcc_controlled_file_checkout("+columns+") VALUES(?,1,?,1,7,'A21 storage vector','CHECKED_IN'"+(include?",?":"")+")")) {
            sql.setLong(1,id);sql.setLong(2,id);if(include)sql.setString(3,value);require(sql.executeUpdate()==1,"one row required");
        }
    }
    static Object read(long id)throws Exception {
        try(var sql=db.prepareStatement("SELECT checkin_payload_hash FROM dcc_controlled_file_checkout WHERE id=?")) {
            sql.setLong(1,id);try(var row=sql.executeQuery()){require(row.next(),"row absent");return row.getString(1);}
        }
    }
    public static void main(String[] args)throws Exception {
        try(var connection=DriverManager.getConnection("jdbc:h2:mem:a21_checkout;MODE=MYSQL;DATABASE_TO_UPPER=false;NON_KEYWORDS=value;","sa","")) {
            db=connection;try(var sql=db.createStatement()){sql.execute(Files.readString(Path.of(args[0])));}
            check("column.CHAR64.nullable.no-default",()->{
                try(var columns=db.getMetaData().getColumns(null,null,"dcc_controlled_file_checkout","checkin_payload_hash")) {
                    require(columns.next(),"new column absent");require("CHARACTER".equals(columns.getString("TYPE_NAME")),"must be CHAR");
                    require(columns.getInt("COLUMN_SIZE")==64,"exact size64");require(columns.getInt("NULLABLE")==DatabaseMetaData.columnNullable,"nullable");require(columns.getString("COLUMN_DEF")==null,"no default");
                }
            });
            check("legacy.omitted-hash-remains-NULL",()->{insert(1,null,false);require(read(1)==null,"no historical hash synthesized");});
            check("explicit.NULL-remains-NULL",()->{insert(2,null,true);require(read(2)==null,"no hash default");});
            check("new.explicit-64-character-roundtrip",()->{String vector="0123456789abcdef".repeat(4);insert(3,vector,true);require(vector.equals(read(3)),"exact full hash storage vector");});
            check("over64.rejected-no-row",()->{
                try{insert(4,"a".repeat(65),true);throw new AssertionError("over64 accepted/truncated");}
                catch(SQLException expected){require("22001".equals(expected.getSQLState()),"precise length rejection, actual="+expected.getSQLState());}
                try(var sql=db.createStatement();var rows=sql.executeQuery("SELECT COUNT(*) FROM dcc_controlled_file_checkout WHERE id=4")){require(rows.next()&&rows.getInt(1)==0,"failed insert zero rows");}
            });
        }
        System.out.println("A21 H2 SUMMARY pass="+passed+" fail="+failed+"; storage only, no application canonical/replay validation");
        if(failed!=0)System.exit(1);
    }
}
