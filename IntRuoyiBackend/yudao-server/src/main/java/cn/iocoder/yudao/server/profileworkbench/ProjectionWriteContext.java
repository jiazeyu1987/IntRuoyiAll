package cn.iocoder.yudao.server.profileworkbench;

import java.sql.Connection;
import static cn.iocoder.yudao.module.system.api.workbench.index.ProjectionModel.*;

final class ProjectionWriteContext {
    static final ThreadLocal<ProjectionWriteContext> CURRENT=new ThreadLocal<>();
    final Scope scope; final long tenant; final Connection connection;
    boolean changed,securityChanged,failed;
    ProjectionWriteContext(Scope scope,long tenant,Connection connection){this.scope=scope;this.tenant=tenant;this.connection=connection;}
}
