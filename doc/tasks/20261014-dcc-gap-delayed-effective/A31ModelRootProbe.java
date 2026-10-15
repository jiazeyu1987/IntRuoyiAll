import java.lang.reflect.*;
import java.nio.file.*;

/** Targeted path contract only. The original eight engine branches remain with Root's M08. */
public class A31ModelRootProbe {
    private static Method rootMethod;
    private static Path read() throws Exception { return (Path)rootMethod.invoke(null); }
    private static void reject() throws Exception {
        try { read(); throw new AssertionError("Invalid root was accepted"); }
        catch(InvocationTargetException error) {
            if(!(error.getCause() instanceof IllegalStateException))throw error;
        }
    }
    public static void main(String[] args) throws Exception {
        Path expected=Path.of(args[0]).toAbsolutePath().normalize();
        rootMethod=Class.forName("cn.iocoder.yudao.module.dcc.DccWorkflowV6BpmnTest").getDeclaredMethod("requireRepositoryRoot");rootMethod.setAccessible(true);
        System.clearProperty("dcc.repository.root");
        System.setProperty("basedir",expected.resolve("IntRuoyiBackend/yudao-module-dcc").toString());
        System.setProperty("user.dir",expected.resolve("IntRuoyiBackend/yudao-module-dcc").toString());
        if(!expected.equals(read()))throw new AssertionError("Surefire module basedir resolved wrong root");
        System.setProperty("user.dir",expected.resolve("IntRuoyiBackend").toString());
        if(!expected.equals(read()))throw new AssertionError("Maven backend cwd changed the explicit basedir contract");
        System.setProperty("dcc.repository.root",expected.toString());System.clearProperty("basedir");
        if(!expected.equals(read()))throw new AssertionError("Standalone explicit root resolved incorrectly");
        System.setProperty("dcc.repository.root",expected.resolve("missing-a31-repository").toString());reject();
        System.clearProperty("dcc.repository.root");reject();
        System.setProperty("basedir",expected.resolve("IntRuoyiBackend").toString());reject();
        System.setProperty("basedir",expected.resolve("IntRuoyiBackend/yudao-module-dcc").toString());
        System.setProperty("dcc.repository.root","");reject();
        System.out.println("A31_PATH_CONTRACT_PASS module/backend cwd + explicit standalone + missing/wrong/empty reject");
    }
}
