package cn.iocoder.yudao.server.profileworkbench;

/** A deployment selects exactly one query engine. Failures never switch engines. */
public interface WorkbenchQueryApplication {
    WorkbenchSourceRegistry.Directory directory();
    WorkbenchContextResponse context(WorkbenchContextRequest request);
    ProfileWorkbenchTodoPageRespVO page(String token,String epoch,int page,int size);
    ProfileWorkbenchTodoCountRespVO count(String token,String epoch);
    VisibilityResponse visibility(WorkbenchVisibilityRequest request);
    record VisibilityResponse(boolean changed,String visibilityVersion,String contextEpoch) { }
}
