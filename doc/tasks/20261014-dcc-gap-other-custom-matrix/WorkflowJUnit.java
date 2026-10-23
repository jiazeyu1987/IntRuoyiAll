import org.junit.platform.launcher.core.*;
import org.junit.platform.launcher.listeners.*;
import static org.junit.platform.engine.discovery.DiscoverySelectors.*;
public class WorkflowJUnit {
 public static void main(String[] args)throws Exception{
  var type=Class.forName(args.length==0?"cn.iocoder.yudao.module.dcc.service.file.DccApplicationCustomMatrixWorkflowTest":args[0]);var builder=LauncherDiscoveryRequestBuilder.request();
  for(var method:type.getDeclaredMethods())if(method.isAnnotationPresent(org.junit.jupiter.api.Test.class))builder.selectors(selectMethod(type,method));
  var listener=new SummaryGeneratingListener();LauncherFactory.create().execute(builder.build(),listener);var writer=new java.io.PrintWriter(System.out,true);listener.getSummary().printTo(writer);listener.getSummary().printFailuresTo(writer);
  if(listener.getSummary().getTestsFailedCount()>0||listener.getSummary().getTestsFoundCount()==0||listener.getSummary().getTestsSkippedCount()>0)System.exit(1);
 }
}
