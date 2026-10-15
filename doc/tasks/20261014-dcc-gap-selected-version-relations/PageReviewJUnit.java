import org.junit.platform.launcher.core.*;
import org.junit.platform.launcher.listeners.*;
import static org.junit.platform.engine.discovery.DiscoverySelectors.*;

public class PageReviewJUnit {
  public static void main(String[] args) {
    String type="cn.iocoder.yudao.module.dcc.service.file.DccReferenceUsagePageDatabaseTest";
    var builder=LauncherDiscoveryRequestBuilder.request();
    if(args.length>0&&args[0].equals("regression"))builder.selectors(selectClass(type));
    else builder.selectors(selectMethod(type,"bodyPermissionIsIndependentAndNeverRequiresManagementDetail"),selectMethod(type,"wrongTenantSourceAndInvalidPageCannotEnterUsageQueries"));
    var listener=new SummaryGeneratingListener();var launcher=LauncherFactory.create();launcher.registerTestExecutionListeners(listener);launcher.execute(builder.build());
    var summary=listener.getSummary();summary.printTo(new java.io.PrintWriter(System.out,true));summary.printFailuresTo(new java.io.PrintWriter(System.out,true));
    if(summary.getTestsFoundCount()==0||summary.getTestsSkippedCount()!=0||summary.getTestsAbortedCount()!=0||summary.getTotalFailureCount()!=0)throw new AssertionError("private current PageDB verification failed");
  }
}
