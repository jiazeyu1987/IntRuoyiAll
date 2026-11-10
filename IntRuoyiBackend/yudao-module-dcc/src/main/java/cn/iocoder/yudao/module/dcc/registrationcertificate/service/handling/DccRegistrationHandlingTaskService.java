package cn.iocoder.yudao.module.dcc.registrationcertificate.service.handling;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.dcc.service.projectcode.attributes.DccProjectAttributes;
import cn.iocoder.yudao.module.dcc.registrationcertificate.domain.DccRegistrationCertificateFacts;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.certificate.DccRegistrationCertificateBusinessClock;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApi;
import cn.iocoder.yudao.module.system.api.notify.dto.NotifySendSingleToUserIdempotentReqDTO;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.sql.Statement;
import java.time.*;
import java.util.*;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

/** Registration办理专属任务；提交和批准结果只接受既有审批链路的持久化事实。 */
@Service
public class DccRegistrationHandlingTaskService {
    private final JdbcTemplate jdbc;
    private final AdminUserApi users;
    private final PermissionApi permissions;
    private final NotifyMessageSendApi messages;
    private final DccRegistrationCertificateBusinessClock clock;
    public DccRegistrationHandlingTaskService(JdbcTemplate jdbc, AdminUserApi users, PermissionApi permissions,
            NotifyMessageSendApi messages, DccRegistrationCertificateBusinessClock clock) {
        this.jdbc=Objects.requireNonNull(jdbc);this.users=Objects.requireNonNull(users);this.permissions=Objects.requireNonNull(permissions);
        this.messages=Objects.requireNonNull(messages);this.clock=Objects.requireNonNull(clock);
    }
    public record Config(@JsonFormat(shape=JsonFormat.Shape.STRING) Long responsibleUserId,String responsibleName,
                         int projectUploadDays,int renewalDays) {}
    public record Task(@JsonFormat(shape=JsonFormat.Shape.STRING) Long id,String taskKind,
            @JsonFormat(shape=JsonFormat.Shape.STRING) Long projectCodeId,@JsonFormat(shape=JsonFormat.Shape.STRING) Long certificateId,
            String title,@JsonFormat(shape=JsonFormat.Shape.STRING) Long responsibleUserId,String responsibleName,
            @JsonFormat(shape=JsonFormat.Shape.STRING) Long assignedUserId,String assignedName,
            LocalDateTime dueAt,String status,@JsonFormat(shape=JsonFormat.Shape.STRING) Long lastRequestId,
            @JsonFormat(shape=JsonFormat.Shape.STRING) Long actualHandlerId,String actualHandlerName,String result,LocalDateTime completedAt) {}
    public record Candidate(@JsonFormat(shape=JsonFormat.Shape.STRING) Long id,String name) {}
    private Long tenant(){return TenantContextHolder.getRequiredTenantId();}
    private void tenant(Long id){if(!Objects.equals(tenant(),id))throw invalidParamException("注册证任务不能跨租户办理");}
    private boolean manager(Long actor){return permissions.hasAnyPermissions(actor,"dcc:registration-certificate:config:update");}
    private String account(Long id) {
        var user=id==null?null:users.getUser(id);
        if(user==null||!Objects.equals(id,user.getId())||!Objects.equals(tenant(),user.getTenantId())||!Integer.valueOf(0).equals(user.getStatus()))
            throw invalidParamException("注册证负责人或办理人必须是本租户有效账号");
        return user.getNickname();
    }
    private void handler(Long actor,String kind) {
        account(actor);
        String permission="UPLOAD".equals(kind)?"dcc:registration-certificate:upload:create":"dcc:registration-certificate:renewal:upload";
        if(!permissions.hasAnyPermissions(actor,permission))throw invalidParamException("指定办理人缺少注册证上传或延续上传权限，请先完成权限分配");
    }
    private String name(Long id){if(id==null)return null;var user=users.getUser(id);return user!=null&&Objects.equals(tenant(),user.getTenantId())?user.getNickname():"账号已停用";}
    public Config getConfig() {
        var rows=jdbc.query("SELECT responsible_user_id,project_upload_days,renewal_days FROM dcc_registration_handling_config WHERE tenant_id=?",
                (rs,n)->new Config(rs.getLong(1),name(rs.getLong(1)),rs.getInt(2),rs.getInt(3)),tenant());
        return rows.isEmpty()?null:rows.get(0);
    }
    private Config requireConfig() {
        var config=getConfig();if(config==null)throw invalidParamException("请先在注册证配置中设置注册证负责人、项目上传期限和失效换证期限");
        handler(config.responsibleUserId(),"UPLOAD");handler(config.responsibleUserId(),"RENEWAL");return config;
    }
    @Transactional(rollbackFor=Exception.class)
    public Config configure(Long actor,Long responsible,int uploadDays,int renewalDays) {
        if(!manager(actor))throw invalidParamException("无权配置注册证办理任务");account(actor);handler(responsible,"UPLOAD");handler(responsible,"RENEWAL");
        if(uploadDays<1||uploadDays>3650||renewalDays<1||renewalDays>3650)throw invalidParamException("办理期限须为1至3650天");
        jdbc.update("INSERT INTO dcc_registration_handling_config(tenant_id,responsible_user_id,project_upload_days,renewal_days,configured_by,configured_at) VALUES(?,?,?,?,?,?) ON DUPLICATE KEY UPDATE responsible_user_id=VALUES(responsible_user_id),project_upload_days=VALUES(project_upload_days),renewal_days=VALUES(renewal_days),configured_by=VALUES(configured_by),configured_at=VALUES(configured_at)",tenant(),responsible,uploadDays,renewalDays,actor,clock.now());
        return requireConfig();
    }
    /** 目标市场明确为不适用时不创建任务，返回 null；其余项目仍须配置负责人和期限。 */
    @Transactional(rollbackFor=Exception.class)
    public Long createProjectTask(Long projectId) {
        var rows=jdbc.queryForList("SELECT id,project_code,project_name,default_attributes_json FROM dcc_project_code WHERE id=? AND tenant_id=? AND deleted=0 AND status='ENABLE' FOR UPDATE",projectId,tenant());
        if(rows.size()!=1)throw invalidParamException("新建项目注册证任务的项目身份无效");
        var project=rows.get(0);
        var attributesJson=(String)project.get("default_attributes_json");
        if(attributesJson!=null) {
            var attributes=JsonUtils.parseObject(attributesJson,DccProjectAttributes.class);
            if(attributes==null)throw invalidParamException("项目默认属性无效");
            attributes.validated();
            if(attributes.libraryScope()==null&&attributes.targetMarkets().equals(List.of("NA")))return null;
        }
        var config=requireConfig();
        return create("PROJECT:"+projectId,"UPLOAD",projectId,null,null,"项目"+project.get("project_code")+" / "+project.get("project_name")+"注册证上传",config,clock.now().plusDays(config.projectUploadDays()));
    }
    @Transactional(rollbackFor=Exception.class)
    public int createExpiredTasks(Long tenantId,LocalDate businessDate) {
        tenant(tenantId);if(businessDate==null)throw invalidParamException("换证任务业务日期缺失");
        var rows=jdbc.queryForList("SELECT c.id,c.project_code_id,c.current_version_id,s.product_name,s.facts_json FROM dcc_registration_certificate c JOIN dcc_registration_certificate_version v ON v.id=c.current_version_id AND v.tenant_id=c.tenant_id AND v.certificate_id=c.id AND v.status='CURRENT' AND v.deleted=0 JOIN dcc_registration_certificate_snapshot s ON s.id=c.current_snapshot_id AND s.tenant_id=c.tenant_id AND s.version_id=v.id AND s.deleted=0 WHERE c.tenant_id=? AND c.status='ACTIVE' AND c.deleted=0 AND c.pending_version_id IS NULL ORDER BY c.id",tenant());
        int created=0;
        for(var row:rows) {
            var facts=DccRegistrationCertificateFacts.parse((String)row.get("facts_json"));
            if(facts.isFda()||"REVOKED".equals(facts.getCertificateStatus())||!facts.isExpired(businessDate))continue;
            Long certificate=((Number)row.get("id")).longValue(),version=((Number)row.get("current_version_id")).longValue();
            var current=jdbc.queryForList("SELECT current_version_id,pending_version_id,status FROM dcc_registration_certificate WHERE tenant_id=? AND id=? AND deleted=0 FOR UPDATE",tenant(),certificate);
            if(current.size()!=1||!Objects.equals(((Number)current.get(0).get("current_version_id")).longValue(),version)||current.get(0).get("pending_version_id")!=null||!"ACTIVE".equals(current.get(0).get("status")))continue;
            var source="EXPIRED:"+certificate+":"+version;
            if(!jdbc.queryForList("SELECT id FROM dcc_registration_handling_task WHERE tenant_id=? AND source_key=?",tenant(),source).isEmpty())continue;
            var config=requireConfig();Object project=row.get("project_code_id");
            create(source,"RENEWAL",project==null?null:((Number)project).longValue(),certificate,version,"失效证件换证 / "+row.get("product_name"),config,clock.now().plusDays(config.renewalDays()));created++;
        }
        return created;
    }
    private Long create(String source,String kind,Long project,Long certificate,Long version,String title,Config config,LocalDateTime due) {
        var prior=jdbc.queryForList("SELECT id FROM dcc_registration_handling_task WHERE tenant_id=? AND source_key=?",tenant(),source);
        if(!prior.isEmpty())return ((Number)prior.get(0).get("id")).longValue();
        var holder=new GeneratedKeyHolder();jdbc.update(connection->{var statement=connection.prepareStatement("INSERT INTO dcc_registration_handling_task(tenant_id,source_key,task_kind,project_code_id,certificate_id,source_version_id,title,responsible_user_id,assigned_user_id,due_at,status,created_at) VALUES(?,?,?,?,?,?,?,?,?,?,'OPEN',?)",Statement.RETURN_GENERATED_KEYS);
            Object[] values={tenant(),source,kind,project,certificate,version,title,config.responsibleUserId(),config.responsibleUserId(),due,clock.now()};for(int i=0;i<values.length;i++)statement.setObject(i+1,values[i]);return statement;},holder);
        Long id=Objects.requireNonNull(holder.getKey()).longValue();event(id,"CREATED",config.responsibleUserId(),config.responsibleUserId(),due,null,"负责人及期限来自注册证业务配置");notify(id,config.responsibleUserId(),title,due,"CREATED");return id;
    }
    private Task task(Long id,boolean lock) {
        var rows=jdbc.query("SELECT * FROM dcc_registration_handling_task WHERE tenant_id=? AND id=?"+(lock?" FOR UPDATE":""),(rs,n)->new Task(rs.getLong("id"),rs.getString("task_kind"),rs.getObject("project_code_id",Long.class),rs.getObject("certificate_id",Long.class),rs.getString("title"),rs.getLong("responsible_user_id"),name(rs.getLong("responsible_user_id")),rs.getLong("assigned_user_id"),name(rs.getLong("assigned_user_id")),rs.getObject("due_at",LocalDateTime.class),rs.getString("status"),rs.getObject("last_request_id",Long.class),rs.getObject("actual_handler_id",Long.class),name(rs.getObject("actual_handler_id",Long.class)),rs.getString("result"),rs.getObject("completed_at",LocalDateTime.class)),tenant(),id);
        if(rows.size()!=1)throw invalidParamException("注册证办理任务不存在或不属于当前租户");return rows.get(0);
    }
    private void viewer(Long actor,Task task){account(actor);if(!Objects.equals(actor,task.responsibleUserId())&&!Objects.equals(actor,task.assignedUserId())&&!manager(actor))throw invalidParamException("无权访问此注册证办理任务");}
    public List<Task> list(Long actor) {
        account(actor);var ids=jdbc.queryForList("SELECT id FROM dcc_registration_handling_task WHERE tenant_id=? AND (responsible_user_id=? OR assigned_user_id=?) ORDER BY id DESC",Long.class,tenant(),actor,actor);
        return ids.stream().map(id->task(id,false)).toList();
    }
    public Map<String,Object> detail(Long actor,Long id) {
        var task=task(id,false);viewer(actor,task);var result=new LinkedHashMap<String,Object>();result.put("task",task);
        result.put("events",jdbc.queryForList("SELECT event_type AS eventType,actor_id AS actorId,assigned_user_id AS assignedUserId,due_at AS dueAt,request_id AS requestId,reason,created_at AS createdAt FROM dcc_registration_handling_event WHERE tenant_id=? AND task_id=? ORDER BY id",tenant(),id));
        result.put("canDelegate",Objects.equals(actor,task.responsibleUserId())&&"OPEN".equals(task.status()));
        result.put("canHandle",permissions.hasAnyPermissions(actor,"UPLOAD".equals(task.taskKind())?"dcc:registration-certificate:upload:create":"dcc:registration-certificate:renewal:upload")&&Objects.equals(actor,task.assignedUserId())&&"OPEN".equals(task.status())&&(Objects.equals(actor,task.responsibleUserId())||!clock.now().isAfter(task.dueAt())));
        if("REJECTED".equals(task.result())&&Objects.equals(actor,task.actualHandlerId()))result.put("restartRequestId",task.lastRequestId().toString());
        if(task.projectCodeId()!=null)result.put("project",jdbc.queryForMap("SELECT id,project_code AS projectCode,project_name AS projectName FROM dcc_project_code WHERE tenant_id=? AND id=? AND deleted=0",tenant(),task.projectCodeId()));
        if(task.certificateId()!=null)result.put("certificate",jdbc.queryForMap("SELECT c.id AS certificateId,c.row_version AS rowVersion,c.first_obtained_date AS firstObtainedDate,c.current_version_id AS versionId,v.version_no AS versionNo,v.certificate_no AS certificateNo,v.classification,v.effective_date AS effectiveDate,v.expiry_date AS expiryDate,s.product_name AS productName FROM dcc_registration_certificate c JOIN dcc_registration_certificate_version v ON v.id=c.current_version_id AND v.tenant_id=c.tenant_id JOIN dcc_registration_certificate_snapshot s ON s.id=c.current_snapshot_id AND s.tenant_id=c.tenant_id WHERE c.tenant_id=? AND c.id=? AND c.deleted=0",tenant(),task.certificateId()));
        for(String key:List.of("project","certificate"))if(result.get(key) instanceof Map<?,?> values) stringifyIds((Map<String,Object>)values);
        for(var values:(List<Map<String,Object>>)result.get("events")){
            values.put("actorName",name(((Number)values.get("actorId")).longValue()));
            if(values.get("assignedUserId") instanceof Number assigned)values.put("assignedName",name(assigned.longValue()));
            stringifyIds(values);
        }
        return result;
    }
    private void stringifyIds(Map<String,Object> values){values.replaceAll((key,value)->value instanceof Number&&(key.equals("id")||key.endsWith("Id"))?value.toString():value);}
    public List<Candidate> candidates(Long actor,Long taskId,String keyword) {
        if(taskId==null){if(!manager(actor))throw invalidParamException("无权配置办理负责人");}
        else {var task=task(taskId,false);if(!Objects.equals(actor,task.responsibleUserId()))throw invalidParamException("仅原负责人可授权办理");}
        if(keyword==null||keyword.isBlank())return List.of();
        return users.getUserListByNickname(keyword.strip()).stream().filter(user->Objects.equals(tenant(),user.getTenantId())&&Integer.valueOf(0).equals(user.getStatus())).limit(30)
                .map(user->new Candidate(user.getId(),user.getNickname())).toList();
    }
    @Transactional(rollbackFor=Exception.class)
    public void delegate(Long actor,Long id,Long assignee,LocalDateTime due,String reason) {
        var task=task(id,true);account(actor);
        if(!Objects.equals(actor,task.responsibleUserId())||!"OPEN".equals(task.status()))throw invalidParamException("仅原负责人可对待办理任务授权或收回授权");
        handler(assignee,task.taskKind());if(due==null||!due.isAfter(clock.now())||reason==null||reason.isBlank()||reason.length()>1024)throw invalidParamException("请填写未来办理期限和授权说明");
        jdbc.update("UPDATE dcc_registration_handling_task SET assigned_user_id=?,due_at=? WHERE tenant_id=? AND id=? AND status='OPEN'",assignee,due,tenant(),id);
        Long event=event(id,"DELEGATED",actor,assignee,due,null,reason.strip());notify(id,assignee,task.title(),due,"DELEGATED:"+event);
    }
    public boolean authorizesProjectUpload(Long actor,Long projectId) {
        if(projectId==null)return false;
        var ids=jdbc.queryForList("SELECT id FROM dcc_registration_handling_task WHERE tenant_id=? AND project_code_id=? AND task_kind='UPLOAD' AND status='OPEN' AND assigned_user_id=?",Long.class,tenant(),projectId,actor);
        if(ids.size()!=1)return false;var task=task(ids.get(0),false);handler(actor,"UPLOAD");
        return Objects.equals(actor,task.responsibleUserId())||!clock.now().isAfter(task.dueAt());
    }
    /** Called only inside the actual upload/renewal submit transaction, after request persistence. */
    @Transactional(rollbackFor=Exception.class)
    public void bindRequest(Long tenantId,Long actor,Long projectId,Long certificateId,Long requestId,String kind) {
        tenant(tenantId);handler(actor,kind);
        String predicate="UPLOAD".equals(kind)?"project_code_id=?":"certificate_id=?";Object object="UPLOAD".equals(kind)?projectId:certificateId;
        if(object==null)return;
        var ids=jdbc.queryForList("SELECT id FROM dcc_registration_handling_task WHERE tenant_id=? AND task_kind=? AND "+predicate+" AND status IN ('OPEN','SUBMITTED') FOR UPDATE",Long.class,tenant(),kind,object);
        if(ids.isEmpty())return;if(ids.size()!=1)throw invalidParamException("存在多个未完成注册证办理任务，请核对原任务");var task=task(ids.get(0),true);
        var attempt=jdbc.queryForList("SELECT task_id,handler_id FROM dcc_registration_handling_attempt WHERE tenant_id=? AND request_id=?",tenant(),requestId);
        if(!attempt.isEmpty()){if(Objects.equals(((Number)attempt.get(0).get("task_id")).longValue(),task.id())&&Objects.equals(((Number)attempt.get(0).get("handler_id")).longValue(),actor))return;throw invalidParamException("注册证申请已绑定其他任务或办理人");}
        if(!Objects.equals(actor,task.assignedUserId())||!"OPEN".equals(task.status())||(!Objects.equals(actor,task.responsibleUserId())&&clock.now().isAfter(task.dueAt())))throw invalidParamException("本任务仅限当前授权办理人在授权期限内提交，请联系原负责人");
        var requests=jdbc.queryForList("SELECT requester_user_id,certificate_id,project_code_id,status,request_type FROM dcc_registration_certificate_access_request WHERE tenant_id=? AND id=? AND deleted=0",tenant(),requestId);
        if(requests.size()!=1||!Objects.equals(((Number)requests.get(0).get("requester_user_id")).longValue(),actor)||!"SUBMITTED".equals(requests.get(0).get("status"))||!"UPLOAD_CERTIFICATE".equals(requests.get(0).get("request_type")))throw invalidParamException("办理任务缺少真实的本人上传审批申请");
        var persisted=requests.get(0);if(!Objects.equals(((Number)persisted.get("certificate_id")).longValue(),certificateId)||("UPLOAD".equals(kind)&&!Objects.equals(((Number)persisted.get("project_code_id")).longValue(),projectId)))throw invalidParamException("办理任务与实际注册证或项目不一致");
        jdbc.update("INSERT INTO dcc_registration_handling_attempt(tenant_id,request_id,task_id,handler_id,submitted_at) VALUES(?,?,?,?,?)",tenant(),requestId,task.id(),actor,clock.now());
        jdbc.update("UPDATE dcc_registration_handling_task SET status='SUBMITTED',last_request_id=?,actual_handler_id=?,result=NULL,completed_at=NULL WHERE tenant_id=? AND id=?",requestId,actor,tenant(),task.id());
        event(task.id(),"SUBMITTED",actor,actor,task.dueAt(),requestId,"已提交既有注册证审批，等待审批结果");
    }
    @Transactional(rollbackFor=Exception.class)
    public void recordDecision(Long tenantId,Long approver,Long requestId,boolean approved,String reason) {
        recordTerminal(tenantId,approver,requestId,approved?"APPROVED":"REJECTED",reason);
    }
    @Transactional(rollbackFor=Exception.class)
    public void recordWithdrawal(Long tenantId,Long requester,Long requestId,String reason) {
        recordTerminal(tenantId,requester,requestId,"WITHDRAWN",reason);
    }
    private void recordTerminal(Long tenantId,Long actor,Long requestId,String outcome,String reason) {
        tenant(tenantId);
        var links=jdbc.queryForList("SELECT task_id FROM dcc_registration_handling_attempt WHERE tenant_id=? AND request_id=?",Long.class,tenant(),requestId);
        if(links.isEmpty())return;var task=task(links.get(0),true);
        var request=jdbc.queryForMap("SELECT status,requester_user_id FROM dcc_registration_certificate_access_request WHERE tenant_id=? AND id=? AND deleted=0",tenant(),requestId);
        var attempt=jdbc.queryForMap("SELECT result,handler_id FROM dcc_registration_handling_attempt WHERE tenant_id=? AND request_id=?",tenant(),requestId);
        Long actual=((Number)attempt.get("handler_id")).longValue();
        if(!outcome.equals(request.get("status"))||!Objects.equals(actual,((Number)request.get("requester_user_id")).longValue())||("WITHDRAWN".equals(outcome)&&!Objects.equals(actor,actual)))throw invalidParamException("办理结果与实际审批申请不一致");
        var prior=attempt.get("result");
        if(prior!=null){if(outcome.equals(prior))return;throw invalidParamException("注册证办理审批结果不可改写");}
        if(!Objects.equals(task.lastRequestId(),requestId)||!Objects.equals(task.actualHandlerId(),actual)||!"SUBMITTED".equals(task.status()))throw invalidParamException("办理任务不处于本次申请审批中");
        boolean approved="APPROVED".equals(outcome);
        jdbc.update("UPDATE dcc_registration_handling_attempt SET result=?,decided_at=? WHERE tenant_id=? AND request_id=?",outcome,clock.now(),tenant(),requestId);
        jdbc.update("UPDATE dcc_registration_handling_task SET status=?,result=?,completed_at=? WHERE tenant_id=? AND id=?",approved?"COMPLETED":"OPEN",outcome,approved?clock.now():null,tenant(),task.id());
        event(task.id(),outcome,actor,actual,task.dueAt(),requestId,reason==null||reason.isBlank()?(approved?"审批通过，证件已入库":"申请未通过，退回原任务继续办理"):reason);
        String resultTitle=task.title()+" / "+(approved?"审批通过，已完成":"WITHDRAWN".equals(outcome)?"申请撤回，继续办理":"审批驳回，继续办理");
        notify(task.id(),task.responsibleUserId(),resultTitle,task.dueAt(),outcome+":"+requestId+":RESPONSIBLE");
        if(!Objects.equals(actual,task.responsibleUserId()))notify(task.id(),actual,resultTitle,task.dueAt(),outcome+":"+requestId+":HANDLER");
    }
    private Long event(Long task,String type,Long actor,Long assigned,LocalDateTime due,Long request,String reason) {
        var holder=new GeneratedKeyHolder();jdbc.update(connection->{var statement=connection.prepareStatement("INSERT INTO dcc_registration_handling_event(tenant_id,task_id,event_type,actor_id,assigned_user_id,due_at,request_id,reason,created_at) VALUES(?,?,?,?,?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS);Object[] values={tenant(),task,type,actor,assigned,due,request,reason,clock.now()};for(int i=0;i<values.length;i++)statement.setObject(i+1,values[i]);return statement;},holder);return Objects.requireNonNull(holder.getKey()).longValue();
    }
    private void notify(Long task,Long actor,String title,LocalDateTime due,String event) {
        var req=new NotifySendSingleToUserIdempotentReqDTO();req.setUserId(actor);req.setTemplateCode("DCC_REGISTRATION_HANDLING_TASK");req.setBusinessKey("REGISTRATION_HANDLING:"+tenant()+":"+task+":"+event);
        req.setTemplateParams(Map.of("title",title,"dueAt",due.toString(),"handlingTaskId",task.toString()));
        Long sent=messages.sendSingleMessageIdempotentlyToAdmin(req);if(sent==null||sent<=0)throw invalidParamException("注册证办理任务站内信发送失败");
    }
}
