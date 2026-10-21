package cn.iocoder.yudao.server.profileworkbench;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import java.util.*;
import java.util.regex.Pattern;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import static cn.iocoder.yudao.module.system.api.workbench.index.ProjectionModel.*;

/** Reject a relevant Mapper write lacking its root's pre-write transaction fence. */
@Component @ConditionalOnProperty(name="profile.workbench.commit-protocol",havingValue="enabled")
@Intercepts(@Signature(type=Executor.class,method="update",args={MappedStatement.class,Object.class}))
public class ProjectionWriterGuard implements Interceptor {
    private static final Set<String> TASK_MAPPERS=Set.of("DccControlledFileMapper","DccControlledFileDistributionMapper","DccControlledFileDistributionRecipientMapper","DccControlledFileTrainingProgressMapper","MesProEdhrWorkTaskMapper","MesProEdhrBatchExecutionMapper","MesProWorkOrderMapper","MesMdItemMapper","ShowroomFieldAssignmentMapper","NotifyMessageMapper");
    private static final Set<String> SECURITY_MAPPERS=Set.of("MenuMapper","RoleMapper","RoleMenuMapper","UserRoleMapper","AdminUserMapper","TemporaryRoleGrantMapper","SystemEntitlementPolicyMapper","SystemEntitlementGrantMapper","SystemEntitlementClaimMapper","TenantMapper","TenantPackageMapper","DeptMapper","PostMapper");
    private static final Set<String> GLOBAL_MAPPERS=Set.of("MenuMapper","SystemEntitlementPolicyMapper","TenantPackageMapper","TenantMapper");
    private static final Pattern TASK_TABLES=Pattern.compile("(?i)\\b(?:dcc_controlled_file|dcc_controlled_file_distribution|dcc_controlled_file_distribution_recipient|dcc_controlled_file_training_progress|mes_pro_edhr_work_task|mes_pro_edhr_batch_execution|mes_pro_work_order|mes_md_item|showroom_field_assignment|system_notify_message)\\b");
    private static final Pattern GLOBAL_TABLES=Pattern.compile("(?i)\\b(?:system_menu|system_entitlement_policy|system_tenant_package|system_tenant)\\b");
    private static final Pattern SECURITY_TABLES=Pattern.compile("(?i)\\b(?:system_menu|system_role|system_role_menu|system_user_role|system_users|system_temporary_role_grant|system_entitlement_policy|system_entitlement_grant|system_entitlement_claim|system_tenant|system_tenant_package|system_dept|system_post|system_user_post|system_role_category)\\b");
    @Override public Object intercept(Invocation invocation) throws Throwable {
        MappedStatement statement=(MappedStatement)invocation.getArgs()[0];String id=statement.getId();String namespace=id.substring(0,id.lastIndexOf('.'));String mapper=namespace.substring(namespace.lastIndexOf('.')+1);
        String sql=statement.getBoundSql(invocation.getArgs()[1]).getSql();
        // Plain append-only notice insertion cannot change an existing assignment's indexed message.
        // Assignment creation/binding has its own fenced root and exports that message in the same batch.
        if(mapper.equals("NotifyMessageMapper")&&statement.getSqlCommandType()==SqlCommandType.INSERT
            &&!Pattern.compile("(?i)\\b(?:ON\\s+DUPLICATE|REPLACE)\\b").matcher(sql).find()) return invocation.proceed();
        boolean security=SECURITY_MAPPERS.contains(mapper)||SECURITY_TABLES.matcher(sql).find();
        if(!TASK_MAPPERS.contains(mapper)&&!security&&!TASK_TABLES.matcher(sql).find()) return invocation.proceed();
        ProjectionWriteContext context=ProjectionWriteContext.CURRENT.get();
        if(context==null) throw new IllegalStateException("Relevant Mapper write has no workbench root fence: "+id);
        boolean global;
        try {global=GLOBAL_MAPPERS.contains(mapper)||(GLOBAL_TABLES.matcher(sql).find()&&mutatesGlobalTable(sql));}
        catch(RuntimeException failure){context.failed=true;throw failure;}
        if(global&&context.scope!=Scope.GLOBAL) throw reject(context,"Global security write under tenant fence: "+id);
        Long tenant=TenantContextHolder.getTenantId();
        if(context.scope==Scope.TENANT&&(tenant==null||tenant!=context.tenant)) throw reject(context,"Tenant changed inside workbench commit fence");
        // REQUIRES_NEW cannot silently detach an outbox from the formal write.
        if(((Executor)invocation.getTarget()).getTransaction().getConnection()!=context.connection) throw reject(context,"Formal write detached from workbench commit transaction");
        Object result;
        try {result=invocation.proceed();} catch(Throwable failure){context.failed=true;throw failure;}
        if(!(result instanceof Number count)) throw reject(context,"Mapper update row count missing");
        if(count.longValue()!=0) {context.changed=true;context.securityChanged|=security;}
        return result;
    }
    private static IllegalStateException reject(ProjectionWriteContext context,String message){context.failed=true;return new IllegalStateException(message);}
    /** Read references in EXISTS/CTE/INSERT SELECT are not mutation targets. */
    private static boolean mutatesGlobalTable(String sql) {
        try {
            var statement=CCJSqlParserUtil.parse(sql);
            if(statement instanceof Insert insert) return globalTable(insert.getTable());
            if(statement instanceof Update update) {
                if(globalTable(update.getTable())) return true;
                Map<String,Table> targets=new HashMap<>();addTarget(targets,update.getTable());
                if(update.getStartJoins()!=null) update.getStartJoins().forEach(j->{if(j.getRightItem() instanceof Table t) addTarget(targets,t);});
                if(update.getJoins()!=null) update.getJoins().forEach(j->{if(j.getRightItem() instanceof Table t) addTarget(targets,t);});
                for(var set:update.getUpdateSets()) for(var column:set.getColumns()) {
                    // In a joined UPDATE, an unqualified SET column may belong to a global joined target.
                    // Without schema-bound resolution it must take the global fence.
                    if((column.getTable()==null||identifier(column.getTable().getName()).isEmpty())&&targets.values().stream().anyMatch(ProjectionWriterGuard::globalTable)) return true;
                    if(column.getTable()!=null&&globalTable(targets.get(identifier(column.getTable().getName())))) return true;
                }
                return false;
            }
            if(statement instanceof Delete delete) {
                Map<String,Table> targets=new HashMap<>();addTarget(targets,delete.getTable());
                if(delete.getJoins()!=null) delete.getJoins().forEach(j->{if(j.getRightItem() instanceof Table t) addTarget(targets,t);});
                if(delete.getTables()!=null&&!delete.getTables().isEmpty())
                    return delete.getTables().stream().anyMatch(t->globalTable(targets.getOrDefault(identifier(t.getName()),t)));
                return globalTable(delete.getTable());
            }
            throw new IllegalStateException("Unsupported formal global mutation statement");
        } catch(net.sf.jsqlparser.JSQLParserException failure) {
            throw new IllegalStateException("Cannot determine formal global mutation target",failure);
        }
    }
    private static void addTarget(Map<String,Table> targets,Table table) {
        if(table==null) return;targets.put(identifier(table.getName()),table);
        if(table.getAlias()!=null) targets.put(identifier(table.getAlias().getName()),table);
    }
    private static String identifier(String name){return name==null?"":name.replace("`","").replace("\"","").toLowerCase(Locale.ROOT);}
    private static boolean globalTable(Table table){return table!=null&&GLOBAL_TABLES.matcher(identifier(table.getName())).matches();}
}
