package cn.iocoder.yudao.module.dcc.service.file.relations;

import cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationContracts.ReferenceView;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Objects;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationContracts.*;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DccProjectReferenceService {
    private final DccRelationStore store;
    private final DccProjectReferenceAuthority authority;
    private final DccRelationAccessPolicy access;
    private final DccLatestControlledFileResolver files;
    @jakarta.annotation.Resource private cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService projectAccessService;
    @org.springframework.context.annotation.Lazy @jakarta.annotation.Resource private cn.iocoder.yudao.module.dcc.service.file.DccControlledFileQueryService queryService;
    public DccProjectReferenceService(DccRelationStore store, DccProjectReferenceAuthority authority,
                                      DccRelationAccessPolicy access, DccLatestControlledFileResolver files) {
        this.store = store; this.authority = authority; this.access = access; this.files = files;
    }
    @Transactional(rollbackFor=Exception.class)
    public List<ReferenceView> createBatch(Long actorId,Long projectId,Long folderId,List<Long> selectedFileIds,String reason){
        require(actorId,projectId,folderId);requireReason(reason);
        if(selectedFileIds==null || selectedFileIds.isEmpty()) throw new DccRelationInputFailure("DCC_REFERENCE_SELECTION_REQUIRED");
        store.lockReferenceContext(TenantContextHolder.getRequiredTenantId(),projectId,folderId);
        authority.assertProjectLeader(actorId,projectId);authority.assertFolderBelongsToProject(projectId,folderId);
        Long tenant=TenantContextHolder.getRequiredTenantId();
        var selected=new java.util.ArrayList<FileVersion>();var unique=new java.util.LinkedHashSet<Long>();
        for(Long id:selectedFileIds){
            require(id);var file=selected(tenant,id);
            if(!unique.add(file.masterId())) throw new DccRelationInputFailure("DCC_REFERENCE_DUPLICATE_SELECTION");
            selected.add(file);
        }
        // All files in one confirmation use a uniform master lock order.
        selected.stream().map(FileVersion::masterId).sorted().forEach(master->store.lockMaster(tenant,master));
        var result=new java.util.ArrayList<ReferenceView>();
        for(var file:selected) result.add(create(actorId,projectId,folderId,file.controlledFileId(),reason));
        return List.copyOf(result);
    }
    @Transactional(rollbackFor = Exception.class)
    public ReferenceView create(Long actorId, Long projectId, Long folderId, Long selectedFileId, String reason) {
        require(actorId, projectId, folderId, selectedFileId);
        requireReason(reason);
        store.lockReferenceContext(TenantContextHolder.getRequiredTenantId(),projectId,folderId);
        authority.assertProjectLeader(actorId, projectId);
        authority.assertFolderBelongsToProject(projectId, folderId);
        Long tenant = TenantContextHolder.getRequiredTenantId();
        FileVersion selected = selected(tenant, selectedFileId);
        store.lockMaster(tenant, selected.masterId());
        var locked=files.resolveSelectedForUpdate(selectedFileId);
        if(locked==null || !Objects.equals(tenant,locked.tenantId()) || !Objects.equals(selectedFileId,locked.controlledFileId())
                || !Objects.equals(selected.masterId(),locked.masterId()))
            throw new DccRelationFailure("DCC_REFERENCE_SOURCE_IDENTITY_INVALID");
        selected=locked;
        access.assertNameVisible(actorId, selectedFileId);
        var existing = store.reference(tenant, projectId, folderId, selected.masterId());
        if (existing != null) {
            if (!Objects.equals(existing.selectedControlledFileId(), selectedFileId))
                throw new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationFailure("DCC_REFERENCE_VERSION_CONFLICT");
            return new ReferenceView(existing,selected,store.referenceProjectCountForUpdate(tenant,selected.masterId()));
        }
        // The selected row was read with FOR UPDATE after its master lock, including exact replay.
        if (!selected.controlled()) throw new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationFailure("DCC_REFERENCE_SOURCE_NOT_CONTROLLED");
        if (store.jdbc().update("INSERT INTO dcc_project_file_reference "
                + "(tenant_id,project_id,folder_id,master_id,selected_controlled_file_id,created_by) VALUES (?,?,?,?,?,?)",
                tenant, projectId, folderId, selected.masterId(), selectedFileId, actorId) != 1)
            throw new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationFailure("DCC_REFERENCE_WRITE_FAILED");
        var created = Objects.requireNonNull(store.reference(tenant,projectId,folderId,selected.masterId()));
        store.audit("dcc.project-reference.create", "REFERENCE:"+created.id(), reason, null, created);
        return new ReferenceView(created,selected,store.referenceProjectCountForUpdate(tenant,selected.masterId()));
    }
    @Transactional(rollbackFor = Exception.class)
    public long cancel(Long actorId, Long projectId, Long folderId, Long masterId, Long expectedReferenceId, boolean confirmed, String reason) {
        require(actorId, projectId, folderId, masterId, expectedReferenceId); requireReason(reason);
        store.lockReferenceContext(TenantContextHolder.getRequiredTenantId(),projectId,folderId);
        authority.assertProjectLeader(actorId, projectId);
        authority.assertFolderBelongsToProject(projectId, folderId);
        if (!confirmed) throw new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationInputFailure("DCC_REFERENCE_CONFIRMATION_REQUIRED");
        Long tenant = TenantContextHolder.getRequiredTenantId();
        store.lockMaster(tenant, masterId);
        var existing = store.reference(tenant, projectId, folderId, masterId);
        if (existing == null) throw new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationFailure("DCC_REFERENCE_NOT_FOUND");
        if(!Objects.equals(existing.id(),expectedReferenceId))
            throw new DccRelationFailure("DCC_REFERENCE_CONCURRENT_CHANGE");
        if (store.jdbc().update("DELETE FROM dcc_project_file_reference WHERE tenant_id=? AND id=?",tenant,existing.id())!=1)
            throw new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationFailure("DCC_REFERENCE_CANCEL_FAILED");
        store.audit("dcc.project-reference.cancel", "REFERENCE:"+existing.id(), reason, existing, null);
        return store.referenceProjectCountForUpdate(tenant,masterId);
    }
    public List<ReferenceView> list(Long actorId, Long projectId, Long folderId) {
        require(actorId, projectId, folderId); authority.assertFolderBelongsToProject(projectId,folderId);
        Long tenant = TenantContextHolder.getRequiredTenantId();
        return store.references(tenant,projectId,folderId).stream().map(ref -> {
            FileVersion file=selected(tenant,ref.selectedControlledFileId());
            access.assertNameVisible(actorId,file.controlledFileId());
            if(!Objects.equals(ref.masterId(),file.masterId())) throw new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationFailure("DCC_REFERENCE_IDENTITY_MISMATCH");
            return view(tenant,ref,file);
        }).toList();
    }
    public ReferenceUsage getUsage(Long actorId,Long selectedFileId){
        require(actorId,selectedFileId);Long tenant=TenantContextHolder.getRequiredTenantId();
        var selected=selected(tenant,selectedFileId);access.assertNameVisible(actorId,selectedFileId);
        long count=store.referenceProjectCount(tenant,selected.masterId());
        return new ReferenceUsage(selected.masterId(),count,count>0);
    }
    @Transactional(readOnly=true,isolation=org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public ReferenceUsagePage getUsagePage(Long actorId,Long selectedFileId,Integer pageNo,Integer pageSize) {
        require(actorId,selectedFileId);
        if(pageNo==null || pageSize==null || pageNo<1 || pageSize<1 || pageSize>200)throw new DccRelationInputFailure("DCC_REFERENCE_USAGE_QUERY_INVALID");
        Long tenant=TenantContextHolder.getRequiredTenantId();var source=selected(tenant,selectedFileId);access.assertNameVisible(actorId,selectedFileId);
        store.assertUsageIdentity(tenant,source);
        var readable=Objects.requireNonNull(projectAccessService.listReadableProjectIds(actorId),"formal readable destination projects");
        if(readable.stream().anyMatch(id->id==null || id<=0))throw new IllegalStateException("formal destination project scope contains invalid identity");
        var scope=readable.stream().distinct().sorted().toList();
        long global=store.referenceProjectCount(tenant,source.masterId());var visible=Objects.requireNonNull(store.visibleUsageCounts(tenant,source.masterId(),scope),"authorized usage counts");
        var rows=store.usagePage(tenant,source.masterId(),scope,pageSize,Math.multiplyExact((long)pageNo-1,pageSize)).stream().map(row->{
            if(!scope.contains(row.projectId()) || !Objects.equals(source.masterId(),row.masterId()))throw new DccRelationFailure("DCC_REFERENCE_USAGE_IDENTITY_INVALID");
            var permission=Objects.requireNonNull(queryService.getRelationPermissions(actorId,row.selectedControlledFileId()),"exact fixed version relation permissions");
            if(!Objects.equals(permission.controlledFileId(),row.selectedControlledFileId()) || !Objects.equals(permission.tenantId(),tenant)
                    || !Objects.equals(permission.masterId(),source.masterId()) || !Objects.equals(permission.versionNo(),row.versionNo()))
                throw new DccRelationFailure("DCC_REFERENCE_USAGE_IDENTITY_INVALID");
            return new ReferenceUsageRow(row.referenceId(),row.projectId(),row.projectName(),row.folderId(),row.folderName(),row.masterId(),row.selectedControlledFileId(),
                    row.fileNumber(),row.fileName(),row.versionNo(),row.status(),row.controlled(),row.pendingEffect(),row.executable(),permission.canPreview());
        }).toList();
        if(visible.projects()>global)throw new IllegalStateException("authorized reference count exceeds global count");
        return new ReferenceUsagePage(tenant,selectedFileId,source.masterId(),global,visible.projects(),visible.rows(),rows,visible.projects()<global);
    }
    private ReferenceView view(Long tenant, Reference ref, FileVersion selected) {
        return new ReferenceView(ref,selected,store.referenceProjectCount(tenant,ref.masterId()));
    }
    private FileVersion selected(Long tenant,Long fileId) {
        FileVersion file=files.resolveSelected(fileId);
        if(file==null || !Objects.equals(tenant,file.tenantId()) || !Objects.equals(fileId,file.controlledFileId()) || file.masterId()==null)
            throw new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationFailure("DCC_REFERENCE_SOURCE_IDENTITY_INVALID");
        return file;
    }
    private void require(Long... values) {
        for(Long value:values) if(value==null || value<=0) throw new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationInputFailure("DCC_REFERENCE_ID_REQUIRED");
    }
    private void requireReason(String reason) {
        if(reason==null || reason.isBlank()) throw new cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationInputFailure("DCC_RELATION_REASON_REQUIRED");
    }
}
