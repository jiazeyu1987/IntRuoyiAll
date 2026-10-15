import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import java.io.PrintWriter;
public final class G06TestLauncher {
    public static void main(String[] classes) throws Exception {
        if (Boolean.getBoolean("g06.verify.root.modes")) verifyRootModes();
        var request=LauncherDiscoveryRequestBuilder.request();
        for(var name:classes)request.selectors(name.startsWith("method:")
                ? DiscoverySelectors.selectMethod(name.substring("method:".length())) : DiscoverySelectors.selectClass(name));
        var listener=new SummaryGeneratingListener();var launcher=LauncherFactory.create();
        launcher.registerTestExecutionListeners(listener);launcher.execute(request.build());
        listener.getSummary().printTo(new PrintWriter(System.out,true));listener.getSummary().printFailuresTo(new PrintWriter(System.out,true));
        if(listener.getSummary().getTestsFoundCount()==0 || listener.getSummary().getTotalFailureCount()!=0)System.exit(1);
    }
    private static void verifyRootModes() throws Exception {
        String explicit=System.getProperty("dcc.repository.root"),basedir=System.getProperty("basedir");
        var expected=java.nio.file.Path.of(explicit).normalize();
        var method=Class.forName("cn.iocoder.yudao.module.dcc.service.file.DccRevisionAttachmentsDatabaseTest$Fixture")
                .getDeclaredMethod("requireRepositoryRoot");method.setAccessible(true);
        try {
            if (!expected.equals(method.invoke(null)))throw new AssertionError("Explicit standalone root mismatch");
            System.clearProperty("dcc.repository.root");System.setProperty("basedir",expected.resolve("IntRuoyiBackend/yudao-module-dcc").toString());
            if (!expected.equals(method.invoke(null)))throw new AssertionError("Maven exact-module basedir root mismatch");
            System.clearProperty("basedir");
            try {method.invoke(null);throw new AssertionError("Missing root and basedir must fail");}
            catch(java.lang.reflect.InvocationTargetException denied){if(!(denied.getCause() instanceof IllegalStateException))throw denied;}
            System.setProperty("dcc.repository.root","relative-root");
            try {method.invoke(null);throw new AssertionError("Relative explicit root must fail");}
            catch(java.lang.reflect.InvocationTargetException denied){if(!(denied.getCause() instanceof IllegalStateException))throw denied;}
            System.out.println("PASS actual fixture root resolver: explicit standalone and Maven exact-module markers; missing/relative root rejected");
        } finally {
            if(explicit==null)System.clearProperty("dcc.repository.root");else System.setProperty("dcc.repository.root",explicit);
            if(basedir==null)System.clearProperty("basedir");else System.setProperty("basedir",basedir);
        }
    }
}
