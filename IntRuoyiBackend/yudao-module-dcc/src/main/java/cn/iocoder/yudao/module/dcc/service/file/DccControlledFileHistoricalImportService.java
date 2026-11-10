package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import static cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants.BAD_REQUEST;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_ACCESS_DENIED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_FILE_NUMBER_CONFLICT;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileHistoricalImportReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFileSubmitReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.category.DccFileCategoryMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.*;
import cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum;
import cn.iocoder.yudao.module.dcc.service.category.DccFileTypeTaxonomyAdminService;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadSizePolicyService;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketCreateCommand;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketCreated;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketMarkBoundCommand;
import cn.iocoder.yudao.module.dcc.service.upload.DccUploadTicketService;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.system.service.dept.DeptService;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import cn.iocoder.yudao.module.system.service.controlledcontent.*;
import cn.iocoder.yudao.module.system.enums.controlledcontent.ControlledContentType;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.*;
import java.time.LocalDateTime;
import java.util.*;

/** Admission of an already controlled source; no new approval or historical signature is invented. */
@Service
public class DccControlledFileHistoricalImportService {
    public static final String SOURCE="CONTROLLED_IMPORT";
    @Resource private DccControlledFileMapper files;
    @Resource private DccControlledFileMasterMapper masters;
    @Resource private DccProjectCodeMapper projects;
    @Resource private DccProjectFolderMapper folders;
    @Resource private DccFileCategoryMapper categories;
    @Resource private DccFileTypeTaxonomyAdminService taxonomy;
    @Resource private DccProjectAccessService projectAccess;
    @Resource private DccControlledFileCategoryPermissionSupport categoryAccess;
    @Resource private DccControlledFileEditPermissionSupport editPermission;
    @Resource private PermissionService permissions;
    @Resource private DccControlledFileAttachmentMapper attachments;
    @Resource private cn.iocoder.yudao.module.system.api.user.AdminUserApi users;
    @Resource private DeptService deptService;
    @Resource private DccPublicUploadPlacementService placement;
    @Resource private DccControlledFileNameClaimService names;
    @Resource private DccControlledFileVersionPolicy versions;
    @Resource private DccWorkflowDatePolicy clock;
    @Resource private DccUploadSizePolicyService sizePolicy;
    @Resource private FileService fileService;
    @Resource private FileMapper fileMapper;
    @Resource private DccDocumentPdfConversionService converter;
    @Resource private DccControlledFileSourceOwnershipService ownership;
    @Resource private DccUploadTicketService uploadTickets;
    @Resource private PlatformTransactionManager transactionManager;
    @Resource private ControlledContentLifecycleCoreService platform;
    @Resource private cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationStore relationStore;

    public record Project(Long id,String projectCode,String projectName) {}
    public record Folder(Long id,Long parentId,String name) {}
    public record FileType(Long categoryId,Long fileTypeTaxonomyId,String label,String uploadBlocker) {}
    public record Department(Long id,String name) {}
    public record Options(List<Project> projects,List<Folder> folders,List<FileType> fileTypes,List<Department> departments) {}
    public record Result(Long id,String fileName,String versionNo,String status,LocalDateTime controlledTime,
                         LocalDateTime effectiveTime,String importSource,Long importedBy,LocalDateTime importedTime) {}

    public Options options(Long actor,Long projectId) {
        requireImporter(actor);
        var selected=projectId==null?null:projects.selectById(projectId);
        if(projectId!=null && (selected==null || !"ENABLE".equals(selected.getStatus())
                || !Objects.equals(selected.getTenantId(),TenantContextHolder.getRequiredTenantId())
                || !projectAccess.hasProjectEditorOrOwner(actor,projectId))) throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        boolean library=selected!=null && selected.isLibrary();
        var activeCategories=categories.selectList().stream()
                .filter(c->Boolean.TRUE.equals(c.getActive()) && c.getFileTypeTaxonomyId()!=null).toList();
        var projectCategories=activeCategories.stream()
                .filter(c->categoryAccess.hasCategoryPermission(c.getId(),actor,DccFileCategoryPermissionActionEnum.EDIT)).toList();
        var uploadTypes=taxonomy.getUploadTaxonomyOptions().stream().collect(java.util.stream.Collectors.toMap(
                cn.iocoder.yudao.module.dcc.controller.admin.category.vo.DccFileTypeTaxonomyRespVO::getId,java.util.function.Function.identity()));
        var types=(library?activeCategories:projectCategories).stream().map(c->{
            var option=uploadTypes.get(c.getFileTypeTaxonomyId());
            String blocker=option==null?"文件类型未启用或不存在，请核对正式类型映射":option.getUploadBlocker();
            if(blocker!=null) return new FileType(c.getId(),c.getFileTypeTaxonomyId(),
                    (option==null?"类型 "+c.getFileTypeTaxonomyId():option.getName())+"（"+blocker+"）",blocker);
            var path=taxonomy.validateCategoryMapping(c.getFileTypeTaxonomyId(),c.getId(),true);
            return new FileType(c.getId(),path.id(),java.util.stream.Stream.of(path.level1(),path.level2(),path.level3(),path.level4(),path.level5())
                    .filter(v->v!=null&&!v.isBlank()).collect(java.util.stream.Collectors.joining(" / ")),null);
        }).toList();
        var allowed=projects.selectList().stream().filter(p->!p.isLibrary() && "ENABLE".equals(p.getStatus())
                        && Objects.equals(p.getTenantId(),TenantContextHolder.getRequiredTenantId()))
                .filter(p->projectAccess.hasProjectEditorOrOwner(actor,p.getId()) && !projectCategories.isEmpty())
                .map(p->new Project(p.getId(),p.getProjectCode(),p.getProjectName())).toList();
        if(projectId==null) return new Options(allowed,List.of(),List.of(),List.of());
        if(!library && allowed.stream().noneMatch(p->p.id().equals(projectId))) throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        return new Options(allowed,folders.listByProject(projectId).stream().filter(f->Boolean.TRUE.equals(f.getActive()))
                .map(f->new Folder(f.getId(),f.getParentId(),f.getName())).toList(),types,
                deptService.getSimpleDeptList().stream().map(d->new Department(d.getId(),d.getName())).toList());
    }

    private void requireImporter(Long actor) {
        if(actor==null || actor<=0 || !permissions.hasAnyRoles(actor,"doc_control")
                || !permissions.hasAnyPermissions(actor,"dcc:controlled-file:import")
                || !permissions.hasAnyPermissions(actor,"dcc:controlled-file:edit"))
            throw exception(CONTROLLED_FILE_ACCESS_DENIED);
    }

    // Conversion callbacks read the separately committed temporary source before the formal import commits.
    @Transactional(rollbackFor=Exception.class,isolation=Isolation.READ_COMMITTED)
    public Result create(Long actor,DccControlledFileHistoricalImportReqVO req) throws java.io.IOException {
        requireImporter(actor);
        editPermission.assertEditableProjectCategory(actor,req.getDccProjectCodeId(),req.getCategoryId());
        var path=taxonomy.validateCategoryMapping(req.getFileTypeTaxonomyId(),req.getCategoryId(),true);
        // Imported/effective timestamps are DATETIME(0); persist one event instant without MySQL rounding it forward.
        var now=clock.now().withNano(0);
        var version=versions.parse(req.getVersionNo().trim());
        if(version==null || version.isWorkingIteration()) throw new ServiceException(BAD_REQUEST.getCode(),"必须填写合法正式受控版本，不能导入工作小版本");
        var materials=req.getAttachments()==null?List.<org.springframework.web.multipart.MultipartFile>of():req.getAttachments();
        for(var material:materials) {
            String materialName=material==null?null:material.getOriginalFilename();
            if(material==null || material.isEmpty() || materialName==null || materialName.isBlank() || materialName.length()>512
                    || materialName.contains("/") || materialName.contains("\\") || material.getContentType()==null || material.getContentType().isBlank())
                throw new ServiceException(BAD_REQUEST.getCode(),"附件必须具有合法文件名、内容类型且不能为空");
            sizePolicy.validateUploadSize(req.getCategoryId(),"ATTACHMENT",material.getSize(),now);
        }
        var materialHashes=new ArrayList<String>();
        for(var material:materials) materialHashes.add(DigestUtil.sha256Hex(material.getBytes()));
        var upload=req.getFile(); String name=upload.getOriginalFilename();
        if(upload.isEmpty() || name==null || name.isBlank() || name.length()>256 || name.contains("/") || name.contains("\\")
                || !DccControlledFileUploadTypePolicy.isAllowedEditableSourceName(name))
            throw new ServiceException(BAD_REQUEST.getCode(),"必须上传具有合法原始文件名的受控正文");
        sizePolicy.validateUploadSize(req.getCategoryId(),"SOURCE",upload.getSize(),now);
        byte[] content=upload.getBytes();
        if(DccControlledFileUploadTypePolicy.isPdfName(name) && !DccControlledFileUploadTypePolicy.isRealPdfFile(name,content))
            throw new ServiceException(BAD_REQUEST.getCode(),"上传文件不是有效PDF正文");
        String number=req.getFileNumber().trim(), key="IMPORT:"+req.getIdempotencyKey().trim();
        String hash=DigestUtil.sha256Hex(JsonUtils.toJsonString(Arrays.asList(req.getDccProjectCodeId(),req.getProjectFolderId(),
                req.getCategoryId(),path.id(),number,version.display(),
                name,DigestUtil.sha256Hex(content),materials.stream().map(m->Arrays.asList(m.getOriginalFilename(),m.getContentType(),m.getSize())).toList(),materialHashes)));
        var placementReq=new DccControlledFileSubmitReqVO();
        placementReq.setDccProjectCodeId(req.getDccProjectCodeId());placementReq.setProjectFolderId(req.getProjectFolderId());
        placementReq.setCategoryId(req.getCategoryId());placementReq.setFileTypeTaxonomyId(path.id());
        placementReq.setChangeType("NEW");placementReq.setProcessType("CONTROLLED_FILE");
        Long id=placement.create(actor,placementReq,storage->{
            var prior=files.selectByCreationIdempotencyForUpdate(TenantContextHolder.getRequiredTenantId(),actor,key);
            if(prior!=null) {
                if(!SOURCE.equals(prior.getImportSource()) || !hash.equals(prior.getCreationPayloadHash()))
                    throw new ServiceException(BAD_REQUEST.getCode(),"同一导入请求键对应的内容已改变");
                return prior.getId();
            }
            if(masters.selectByNewLogicalIdentity(storage.tenantId,storage.projectId,path.id(),number)!=null)
                throw exception(CONTROLLED_FILE_FILE_NUMBER_CONFLICT);
            names.requireNumberAvailableForNewFile(number);
            var master=DccControlledFileMasterDO.builder().tenantId(storage.tenantId).categoryId(storage.categoryId)
                    .directoryId(storage.storageDirectoryId).fileName(name).fileNumber(number).normalizedFileNumber(number)
                    .dccProjectCodeId(storage.projectId).fileTypeTaxonomyLeafId(path.id()).status("ACTIVE_CHAIN").build();
            masters.insert(master);
            names.claimIdentity(storage.tenantId,name,storage.projectId,path.id(),number,master.getId());
            var ticket=DccControlledFileUploadTypePolicy.isPdfName(name)?null:
                    prepareConversionSource(actor,storage.categoryId,content,name,upload.getContentType());
            Long source=ticket==null?store(content,name,upload.getContentType()):ticket.storageFileId();
            Long pdf=source;
            if(!DccControlledFileUploadTypePolicy.isPdfName(name)) {
                var converted=converter.convertToPdf(Objects.requireNonNull(fileMapper.selectById(source),"已存原正文"));
                if(converted==null || !DccControlledFileUploadTypePolicy.isRealPdfFile(converted.fileName(),converted.content()))
                    throw new IllegalStateException("正文转换未返回真实PDF");
                pdf=store(converted.content(),converted.fileName(),"application/pdf");
            }
            boolean active=true;
            var file=DccControlledFileDO.builder().tenantId(storage.tenantId).masterId(master.getId())
                    .categoryId(storage.categoryId).directoryId(storage.storageDirectoryId).dccProjectCodeId(storage.projectId)
                    .fileTypeTaxonomyId(path.id()).fileTypeLevel1(path.level1()).fileTypeLevel2(path.level2())
                    .fileTypeLevel3(path.level3()).fileTypeLevel4(path.level4()).fileTypeLevel5(path.level5())
                    .fileName(name).title(name).sourceOriginalFileName(name).fileNumber(number).versionNo(version.display())
                    .revisionCode(version.majorIdentity()).iterationNo(version.iterationNo()).revisionChangeType("INITIAL")
                    .sourceFileId(source).originalFileId(source).editableFileId(source).readOnlyFileId(pdf).publishedFileId(pdf)
                    .sourceSha256(DigestUtil.sha256Hex(content)).processType("CONTROLLED_FILE").changeType("NEW")
                    .status(active?"ACTIVE":"CONTROLLED_PENDING_EFFECTIVE").requesterId(actor).submitterId(actor)
                    .controlledTime(now).effectiveTime(now).effectiveDate(now.toLocalDate())
                    .effectiveMode(SOURCE).activatedTime(active?now:null).publishedTime(now).needTraining(false)
                    .distributionDepartmentIdsJson("[]").importSource(SOURCE).importedBy(actor).importedTime(now)
                    .creationIdempotencyKey(key).creationPayloadHash(hash).build();
            files.insert(file);
            if(!materials.isEmpty()) {
                var account=users.getUser(actor);
                if(account==null || !Objects.equals(account.getId(),actor) || !Objects.equals(account.getTenantId(),storage.tenantId)
                        || !Integer.valueOf(0).equals(account.getStatus()) || account.getUsername()==null || account.getUsername().isBlank()
                        || account.getNickname()==null || account.getNickname().isBlank())
                    throw new IllegalStateException("附件缺少本租户启用上传人账号快照");
                for(int i=0;i<materials.size();i++) {
                    var material=materials.get(i);
                    byte[] bytes;
                    try { bytes=material.getBytes(); } catch(java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
                    var row=DccControlledFileAttachmentDO.builder().controlledFileId(file.getId())
                            .storageFileId(store(bytes,material.getOriginalFilename(),material.getContentType()))
                            .originalFileName(material.getOriginalFilename()).contentType(material.getContentType()).fileSize(material.getSize())
                            .fileSha256(materialHashes.get(i)).sortNo(i).attachmentPurpose("ATTACHMENT").uploadedBy(actor)
                            .uploadedUsernameSnapshot(account.getUsername()).uploadedNicknameSnapshot(account.getNickname()).uploadedAt(now).build();
                    row.setTenantId(storage.tenantId);
                    if(attachments.insert(row)!=1 || row.getId()==null) throw new IllegalStateException("受控上传附件保存失败");
                }
            }
            ownership.claimSubmissionSource(file.getId(),new DccControlledFilePreparedSource(source,source,file.getSourceSha256(),false),actor,SOURCE);
            if(ticket!=null) uploadTickets.markBound(new DccUploadTicketMarkBoundCommand(ticket.uploadTicket(),actor,
                    storage.categoryId,ticket.sessionId(),ticket.purpose(),file.getId()));
            masters.updateById(DccControlledFileMasterDO.builder().id(master.getId()).latestControlledFileId(file.getId())
                    .currentActiveControlledFileId(active?file.getId():null).build());
            platform.projectDccControlledImport(key(file),master.getId(),file.getId(),actor,now);
            relationStore.initializeControlledImportRelations(file,actor);
            return file.getId();
        });
        return result(Objects.requireNonNull(files.selectById(id),"导入结果"));
    }

    private Long store(byte[] bytes,String name,String type) {
        Long id=fileService.createFileAndReturnId(bytes,name,"dcc/controlled-import",type);
        Runnable cleanup=ownership.rollbackCleanup(new DccControlledFilePreparedSource(id,id,DigestUtil.sha256Hex(bytes),true));
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){
            @Override public void afterCompletion(int status) {
                if(status==STATUS_ROLLED_BACK) cleanup.run();
                else if(status!=STATUS_COMMITTED) throw new IllegalStateException("导入事务结果未知，需核对正文存储");
            }
        });
        return id;
    }

    private DccUploadTicketCreated prepareConversionSource(Long actor,Long categoryId,byte[] content,String name,String type) {
        var transaction=new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        String session="CONTROLLED-IMPORT-"+UUID.randomUUID();
        var ticket=Objects.requireNonNull(transaction.execute(status->{
            Long source=store(content,name,type);
            return uploadTickets.createTicket(new DccUploadTicketCreateCommand(actor,categoryId,session,"SOURCE",
                    source,name,type,(long)content.length,content,session));
        }),"导入转换临时票据");
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){
            @Override public void afterCompletion(int status) {
                if(status==STATUS_COMMITTED) return;
                if(status!=STATUS_ROLLED_BACK) throw new IllegalStateException("导入事务结果未知，需核对转换临时正文");
                transaction.executeWithoutResult(ignored->{
                    try {
                        uploadTickets.cleanupTemporaryFileByTicket(actor,ticket.sessionId(),ticket.uploadTicket(),clock.now(),"USER_DISCARDED");
                    } catch(Exception failure) {
                        throw new IllegalStateException("导入失败后的转换临时正文清理失败",failure);
                    }
                });
            }
        });
        return ticket;
    }

    @Transactional(propagation=org.springframework.transaction.annotation.Propagation.MANDATORY,rollbackFor=Exception.class)
    public boolean activateLocked(DccControlledFileDO file,DccControlledFileMasterDO master,LocalDateTime now) {
        requireImportFacts(file);
        if(file.getEffectiveTime().isAfter(now)) return false;
        if(!"CONTROLLED_PENDING_EFFECTIVE".equals(file.getStatus()) || file.getActivatedTime()!=null
                || !Objects.equals(master.getLatestControlledFileId(),file.getId()) || master.getCurrentActiveControlledFileId()!=null)
            throw new IllegalStateException("导入版本与逻辑文件当前生效定位不一致");
        if(files.updateById(DccControlledFileDO.builder().id(file.getId()).status("ACTIVE").activatedTime(now).build())!=1
                || masters.updateById(DccControlledFileMasterDO.builder().id(master.getId()).currentActiveControlledFileId(file.getId()).build())!=1)
            throw new IllegalStateException("导入版本启用更新失败");
        platform.projectDccControlledImport(key(file),master.getId(),file.getId(),-1L,now);
        return true;
    }

    static void requireImportFacts(DccControlledFileDO file) {
        if(file==null || !SOURCE.equals(file.getImportSource()) || !SOURCE.equals(file.getEffectiveMode())
                || !Objects.equals(file.getTenantId(),TenantContextHolder.getRequiredTenantId()) || Boolean.TRUE.equals(file.getDeleted())
                || file.getImportedBy()==null || file.getImportedBy()<=0 || file.getImportedTime()==null
                || file.getControlledTime()==null || file.getEffectiveTime()==null
                || file.getReadOnlyFileId()==null || file.getPublishedFileId()==null || file.getSourceFileId()==null
                || file.getProcessInstanceId()!=null || file.getEffectiveSignatureId()!=null)
            throw new IllegalStateException("导入原件缺少真实来源、原时间或正式正文");
        if(file.getControlledTime().isAfter(file.getImportedTime()) || file.getControlledTime().isAfter(file.getEffectiveTime()))
            throw new IllegalStateException("导入原件的原受控时间与真实上传或生效时间不一致");
    }

    private ControlledContentKey key(DccControlledFileDO file) {
        return ControlledContentKey.of(file.getTenantId(),ControlledContentType.DCC_CONTROLLED_FILE,file.getMasterId().toString());
    }
    private Result result(DccControlledFileDO f) {
        return new Result(f.getId(),f.getFileName(),f.getVersionNo(),f.getStatus(),f.getControlledTime(),f.getEffectiveTime(),
                f.getImportSource(),f.getImportedBy(),f.getImportedTime());
    }
}
