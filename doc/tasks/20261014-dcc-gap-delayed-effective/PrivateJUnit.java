import org.junit.platform.launcher.*;
import org.junit.platform.launcher.core.*;
import org.junit.platform.launcher.listeners.*;
import static org.junit.platform.engine.discovery.DiscoverySelectors.*;
public class PrivateJUnit {
  public static void main(String[] args) {
    var builder=LauncherDiscoveryRequestBuilder.request();
    for(String name:args)builder.selectors(selectClass(name));
    var listener=new SummaryGeneratingListener();var launcher=LauncherFactory.create();launcher.registerTestExecutionListeners(listener);
    launcher.execute(builder.build());var summary=listener.getSummary();summary.printTo(new java.io.PrintWriter(System.out));summary.printFailuresTo(new java.io.PrintWriter(System.out));
    if(summary.getTestsFailedCount()>0 || summary.getTestsSucceededCount()==0)System.exit(1);
  }
}
