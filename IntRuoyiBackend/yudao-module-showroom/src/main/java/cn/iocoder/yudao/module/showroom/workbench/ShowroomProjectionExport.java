package cn.iocoder.yudao.module.showroom.workbench;

import cn.iocoder.yudao.module.system.api.workbench.*;
import cn.iocoder.yudao.module.system.api.workbench.index.*;
import cn.iocoder.yudao.module.showroom.dal.mysql.workflow.ShowroomFieldAssignmentMapper;
import cn.iocoder.yudao.module.showroom.dal.dataobject.workflow.ShowroomFieldAssignmentDO;
import cn.iocoder.yudao.module.showroom.workflow.service.ShowroomAssignmentWorkbenchSource;
import cn.iocoder.yudao.module.showroom.foundation.contract.ShowroomRoleModelContract;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.util.*;
import static cn.iocoder.yudao.module.system.api.workbench.index.ProjectionModel.*;
import static cn.iocoder.yudao.module.system.api.workbench.index.ProjectionExportSupport.*;

@Component @ConditionalOnProperty(name="profile.workbench.commit-protocol",havingValue="enabled")
public class ShowroomProjectionExport implements WorkbenchProjectionExport {
    private final ShowroomFieldAssignmentMapper mapper;
    private final WorkbenchNotificationExport notifications;
    private final WorkbenchAuthorityExport authority;
    private final SourceDescriptor descriptor;
    private static final Map<String,String> COMPANY=Map.of("development_history","发展历程","park_introduction","园区介绍","incubation_platform","孵化平台","subsidiary_overview","子公司概览","stock_info","股权信息","core_manufacturing_capability","核心制造能力","honors_awards","荣誉奖项");
    private static final Map<String,String> PRODUCT=Map.ofEntries(Map.entry("owner_company_id","所属公司"),Map.entry("product_owner_type","产品归属/类型"),Map.entry("lifecycle_stage","生命周期"),Map.entry("target_market","在售国家"),Map.entry("pipeline_layout","BU"),Map.entry("indication_content","适应症"),Map.entry("core_selling_points","卖点文案"),Map.entry("model_specification","型号规格"),Map.entry("cover_image","封面"),Map.entry("registration_certificate","注册证"),Map.entry("clinical_effect","临床效果"),Map.entry("fim_status","FIM状态"),Map.entry("__PRODUCT_ALL_FIELDS__","整产品资料"));
    public ShowroomProjectionExport(ShowroomFieldAssignmentMapper mapper,WorkbenchNotificationExport notifications,WorkbenchAuthorityExport authority,ShowroomAssignmentWorkbenchSource source) {
        this.mapper=mapper;this.notifications=notifications;this.authority=authority;descriptor=source.descriptor();
    }
    @Override public String ownerModule(){return "showroom";}
    @Override public Set<String> sourceIds(){return Set.of(descriptor.sourceId());}
    @Override public Set<String> writerIds(){return Set.of("showroom.formal.task-facts","system.formal.notification-facts");}
    @Override public List<Task> exportTasks(long tenant,Watermark watermark) {
        List<Task> tasks=new ArrayList<>();
        for(var assignment:mapper.selectList(new LambdaQueryWrapper<ShowroomFieldAssignmentDO>().eq(ShowroomFieldAssignmentDO::getTenantId,tenant).eq(ShowroomFieldAssignmentDO::getStatus,"OPEN"))) {
            if(assignment.getId()==null||assignment.getTargetId()==null||assignment.getTargetType()==null||assignment.getFieldCode()==null||assignment.getNotifyMessageId()==null) throw WorkbenchFailure.SOURCE_CONTRACT_INVALID.exception();
            String content=notifications.requireContent(tenant,assignment.getNotifyMessageId());
            String type=switch(assignment.getTargetType()){case "COMPANY"->"公司";case "PRODUCT"->"产品";default->assignment.getTargetType();};
            String field=("COMPANY".equals(assignment.getTargetType())?COMPANY:PRODUCT).getOrDefault(assignment.getFieldCode(),assignment.getFieldCode());
            TaskSummary row=new TaskSummary();String id=assignment.getId().toString();row.setBusinessId(id);row.setTaskKey(descriptor.sourceId()+":"+id);row.setTaskType(descriptor.taskType());row.setSource(descriptor.label());
            row.setDetail(join(type+"#"+assignment.getTargetId(),field,content));row.setStatusLabel("待处理");row.setRowOrderKey("0".repeat(Math.max(0,20-id.length()))+id);
            tasks.add(task(tenant,watermark,descriptor,row,new ActionEnvelope(descriptor.actionKind(),1,Map.of("assignmentId",id)),AudienceKind.PERSON,candidates(assignment.getAssigneeUserId(),null)));
        }
        return List.copyOf(tasks);
    }
    private static String join(String... values) {
        return Arrays.stream(values).map(value->value.replaceAll("^[\\s\\p{Z}\\uFEFF]+|[\\s\\p{Z}\\uFEFF]+$","")).filter(value->!value.isEmpty()).collect(java.util.stream.Collectors.joining(" · "));
    }
    @Override public List<Authorization> exportAuthorizations(long tenant,Watermark watermark) {
        return authorizations(tenant,watermark,descriptor,exportTasks(tenant,watermark),authority.exportGrants(tenant,new WorkbenchAuthorityExport.Requirement(WorkbenchAuthorityExport.Mode.ANY_ROLE_OR_SUPER_ADMIN,Set.of(ShowroomRoleModelContract.editorRoleCode()))));
    }
}
