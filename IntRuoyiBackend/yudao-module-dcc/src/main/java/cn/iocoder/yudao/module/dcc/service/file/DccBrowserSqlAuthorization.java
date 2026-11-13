package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccProjectBrowserMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectCodeMapper;
import cn.iocoder.yudao.module.dcc.enums.DccAccessTypeEnum;
import cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum;
import cn.iocoder.yudao.module.dcc.service.directory.DccDirectoryAccessPermissionService;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import java.util.*;

/** Compiles the existing browse authorization into SQL. Only configuration and the actor's
 * supervisor tasks are resolved in Java; controlled-file candidates are never enumerated. */
@Component
public class DccBrowserSqlAuthorization {
    @Resource private DccControlledFileCategoryPermissionSupport categories;
    @Resource private DccControlledFileViewMatrixAccessService matrix;
    @Resource private DccProjectAccessService projects;
    @Resource private DccProjectCodeMapper projectMapper;
    @Resource private DccDirectoryAccessPermissionService directories;
    @Resource private DccControlledFileDetailAuthorizationGuard guard;
    @Resource private DccProjectBrowserMapper mapper;
    @Resource private DccArchiveAccessService archiveAccess;

    public static final String STAGE = "CASE f.status WHEN 'PENDING_SUPERVISOR_REVIEW' THEN 'SUPERVISOR_REVIEW' "
            + "WHEN 'PENDING_DOC_CONTROL_REVIEW' THEN 'DOC_CONTROL_REVIEW' WHEN 'PENDING_MATRIX_REVIEW' THEN 'MATRIX_REVIEW' "
            + "WHEN 'PENDING_MATRIX_APPROVAL' THEN 'MATRIX_APPROVAL' WHEN 'PENDING_DOC_CONTROL_APPROVAL' THEN 'DOC_CONTROL_APPROVAL' "
            + "WHEN 'PENDING_APPLICANT_REWORK' THEN 'APPLICANT_REWORK' END";
    private static final List<String> PENDING = List.of("PENDING_SUPERVISOR_REVIEW", "PENDING_DOC_CONTROL_REVIEW",
            "PENDING_MATRIX_REVIEW", "PENDING_MATRIX_APPROVAL", "PENDING_DOC_CONTROL_APPROVAL", "PENDING_APPLICANT_REWORK");

    /** Version options authorize each exact history independently of the row's search filters. */
    public QueryWrapper<Object> historyScope(Long actor, boolean manager, Set<Long> assigned, List<String> blacklist) {
        var scope=scope(actor,manager,null,false,false,blacklist,"historyPermission");
        scope.getParamNameValuePairs().put("browserBlacklistedExtensions",blacklist.stream().filter(Objects::nonNull).map(String::trim).filter(value->!value.isEmpty()).map(value->value.toLowerCase(Locale.ROOT)).map(value->value.startsWith("*")?value.substring(1):value).map(value->value.startsWith(".")?value:"."+value).toList());
        if (assigned!=null) {
            var readable=projects.listReadableProjectIds(actor);
            scope.and(w -> w.and(library -> library.apply("EXISTS (SELECT 1 FROM dcc_project_code library WHERE library.id=f.dcc_project_code_id AND library.tenant_id=f.tenant_id AND library.deleted=0 AND library.library_scope IN ('QMS','GENERAL'))")
                    .and(p -> in(p,"f.dcc_project_code_id",readable)))
                    .or(file -> in(file,"f.id",assigned)));
        }
        return scope;
    }

    public QueryWrapper<Object> scope(Long actor, boolean manager, Set<Long> assigned, boolean projectBrowser,
                                       boolean readableRequired, List<String> blacklist) {
        return scope(actor,manager,assigned,projectBrowser,readableRequired,blacklist,"permission");
    }

    private QueryWrapper<Object> scope(Long actor, boolean manager, Set<Long> assigned, boolean projectBrowser,
                                       boolean readableRequired, List<String> blacklist, String parameterAlias) {
        if (actor == null || actor <= 0 || TenantContextHolder.isIgnore())
            throw new IllegalArgumentException("browse requires an authenticated tenant actor");
        Long tenant = TenantContextHolder.getRequiredTenantId();
        boolean archiveAllowed=projectBrowser && archiveAccess.canQuery(actor);
        var scope = new QueryWrapper<Object>(); scope.setParamAlias(parameterAlias);
        scope.eq("f.tenant_id", tenant).eq("f.deleted", 0)
                .notIn(!archiveAllowed,"f.status", List.of("SUPERSEDED", "OBSOLETE"))
                .apply("NOT (f.status='WITHDRAWN' AND f.controlled_time IS NULL AND COALESCE(f.change_type,'')='REVISION' AND COALESCE(f.revision_change_type,'') IN ('PARTIAL','REPLACEMENT'))");
        var readable = new LinkedHashSet<>(projects.listReadableProjectIds(actor));
        if (projectBrowser || readableRequired) in(scope, "f.dcc_project_code_id", readable);
        if (assigned != null) {
            if (projectBrowser) scope.and(w -> w.apply("EXISTS (SELECT 1 FROM dcc_project_code library WHERE library.id=f.dcc_project_code_id AND library.tenant_id=f.tenant_id AND library.deleted=0 AND library.library_scope IN ('QMS','GENERAL'))")
                    .or(x -> in(x, "f.id", assigned)));
            else if (!assigned.isEmpty()) in(scope, "f.id", assigned);
        }
        for (String raw : blacklist) {
            if (raw == null || raw.trim().isEmpty()) continue;
            String extension = raw.trim().toLowerCase(Locale.ROOT);
            if (extension.startsWith("*")) extension = extension.substring(1);
            if (!extension.startsWith(".")) extension = "." + extension;
            String filename="LOWER(TRIM(CASE WHEN f.source_original_file_name IS NOT NULL THEN f.source_original_file_name WHEN NULLIF(TRIM(f.file_name),'') IS NOT NULL THEN f.file_name ELSE COALESCE(f.title,'') END))";
            scope.apply("NOT (LOCATE('.',"+filename+")>0 AND CONCAT('.',REGEXP_REPLACE("+filename+",'^.*[.]',''))={0})", extension);
        }
        if (manager && (projectBrowser || assigned == null || !assigned.isEmpty())) return scope;
        var editable = categories.editableCategoryIds(actor);
        var view = categories.exactCategoryIds(actor, DccFileCategoryPermissionActionEnum.VIEW);
        var approve = categories.exactCategoryIds(actor, DccFileCategoryPermissionActionEnum.APPROVE);
        var matrixCategories = matrix.accessibleCategoryIds(actor);
        var editors = new LinkedHashSet<Long>(); var libraries = new LinkedHashSet<Long>();
        for (var project : projectMapper.selectList()) {
            if (project.isLibrary()) libraries.add(project.getId());
            if (projects.hasProjectEditorOrOwner(actor, project.getId())) editors.add(project.getId());
        }
        var directoryIds = new LinkedHashSet<Long>();
        if (projectBrowser || assigned == null || !assigned.isEmpty()) {
            directoryIds.addAll(directories.getAuthorizedDirectoryIds(actor, DccAccessTypeEnum.QUERY));
            directoryIds.addAll(directories.getAuthorizedDirectoryIds(actor, DccAccessTypeEnum.PREVIEW));
        }
        // Supervisor approval also validates the durable workflow contract. Keep that guard;
        // the preliminary SQL is restricted to this actor's running supervisor assignments.
        var supervisors = mapper.selectSupervisorCandidates(tenant, actor).stream()
                .filter(file -> guard.hasCurrentSupervisorTask(actor, file)).map(file -> file.getId()).toList();
        scope.and(w -> {
            w.eq("f.requester_id", actor);
            if (archiveAllowed) w.or(archived -> archived.in("f.status",List.of("SUPERSEDED","OBSOLETE")));
            w.or(lifecycle -> lifecycle.isNotNull("f.category_id").and(states -> {
                states.in("f.status", List.of("TRAINING_IN_PROGRESS", "PENDING_MANUAL_DISTRIBUTION", "WORKING", "REJECTED"))
                        .and(edit -> editable(edit, editable, editors, libraries));
                states.or(ready -> ready.in("f.status", List.of("READY_TO_PUBLISH", "FINALIZING", "FINALIZATION_FAILED"))
                        .and(cat -> in(cat, "f.category_id", approve)));
            }));
            w.or(pending -> pending.in("f.status", PENDING).and(participant -> participant
                    .apply("EXISTS (SELECT 1 FROM dcc_controlled_file_route_snapshot snapshot WHERE snapshot.controlled_file_id=f.id AND snapshot.tenant_id=f.tenant_id AND snapshot.deleted=0 AND snapshot.stage_code=" + STAGE
                            + " AND REGEXP_LIKE(COALESCE(snapshot.resolved_user_ids,''),{0},'c'))", "(^|,)[\\x{0000}-\\x{0020}]*[+]?0*"+actor+"[\\x{0000}-\\x{0020}]*(,|$)")
                    .or(task -> task.ne("f.status", "PENDING_SUPERVISOR_REVIEW")
                            .apply("EXISTS (SELECT 1 FROM ACT_RU_TASK task WHERE task.PROC_INST_ID_=f.process_instance_id AND task.TENANT_ID_=CAST(f.tenant_id AS CHAR) AND task.TASK_DEF_KEY_=" + STAGE + " AND task.ASSIGNEE_={0})", String.valueOf(actor)))
                    .or(supervisor -> in(supervisor, "f.id", supervisors))));
            w.or(current -> current.in("f.status", List.of("ACTIVE", "CONTROLLED_PENDING_EFFECTIVE")).and(access -> {
                access.and(category -> category.isNotNull("f.category_id").and(categoryAccess -> {
                    categoryAccess.and(byProject -> in(byProject, "f.dcc_project_code_id", readable).and(v -> {
                        in(v, "f.dcc_project_code_id", libraries);
                        v.or(exact -> in(exact, "f.category_id", view));
                        v.or(edit -> editable(edit, editable, editors, libraries));
                    }));
                    categoryAccess.or(byMatrix -> in(byMatrix, "f.category_id", matrixCategories));
                }));
                access.or().apply("f.status='ACTIVE' AND EXISTS (SELECT 1 FROM dcc_controlled_file_distribution_recipient recipient JOIN dcc_controlled_file_distribution distribution ON distribution.id=recipient.distribution_id WHERE distribution.controlled_file_id=f.id AND recipient.tenant_id=f.tenant_id AND distribution.tenant_id=f.tenant_id AND recipient.deleted=0 AND distribution.deleted=0 AND recipient.user_id={0} AND distribution.distribution_medium='PUBLIC_FOLDER' AND distribution.status IN ('PENDING','SENT','READ','ACKNOWLEDGED'))", actor);
            }));
            w.or(directory -> in(directory, "f.directory_id", directoryIds));
        });
        return scope;
    }

    private static void editable(QueryWrapper<Object> w, Collection<Long> categories, Collection<Long> editors, Collection<Long> libraries) {
        in(w, "f.dcc_project_code_id", editors).and(category -> {
            in(category, "f.dcc_project_code_id", libraries);
            category.or(c -> in(c, "f.category_id", categories));
        });
    }
    private static QueryWrapper<Object> in(QueryWrapper<Object> wrapper, String column, Collection<Long> values) {
        return values.isEmpty() ? wrapper.apply("1=0") : wrapper.in(column, values);
    }
}
