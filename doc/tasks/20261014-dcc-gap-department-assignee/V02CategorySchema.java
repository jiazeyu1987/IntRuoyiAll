import java.nio.file.*;
import java.sql.*;

/** Only checks the extracted explicit source literals against the current real test category DDL. */
public class V02CategorySchema {
    static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception {
        try(var db=DriverManager.getConnection("jdbc:h2:mem:v02_category;MODE=MYSQL;DATABASE_TO_UPPER=false;NON_KEYWORDS=value;","sa","")) {
            try(var sql=db.createStatement()){sql.execute(Files.readString(Path.of(args[0])));}
            for(int i=1;i<args.length;i++) {
                require("PUBLIC_MATRIX".equals(args[i]),"Only reviewed explicit normal source literals allowed");
                try(var insert=db.prepareStatement("INSERT INTO dcc_file_category(id,code,name,source,lifecycle_stage,approval_source_mode) VALUES(?,?,?,'LOCAL','PLAN',?)")) {
                    insert.setLong(1,i);insert.setString(2,"V02-"+i);insert.setString(3,"schema-only-row-"+i);insert.setString(4,args[i]);
                    require(insert.executeUpdate()==1,"one literal/source schema row required");
                }
            }
            try(var sql=db.createStatement();var rows=sql.executeQuery("SELECT COUNT(*) FROM dcc_file_category WHERE approval_source_mode='PUBLIC_MATRIX'")) {
                require(rows.next()&&rows.getInt(1)==args.length-1,"every explicit normal source must roundtrip");
            }
            try(var sql=db.createStatement()) {
                try { sql.executeUpdate("INSERT INTO dcc_file_category(id,code,name,source,lifecycle_stage) VALUES(999,'MISSING','missing','LOCAL','PLAN')");throw new AssertionError("missing source accepted/defaulted"); }
                catch(SQLException expected){require("23502".equals(expected.getSQLState()),"must fail NOTNULL, not another schema problem");}
            }
            System.out.println("V02 H2: "+(args.length-1)+" extracted source literals inserted/read; omitted-source actual23502; PASS");
            System.out.println("Scope: synthetic required columns only; not original fixture DO instantiation or service JUnit.");
        }
    }
}
