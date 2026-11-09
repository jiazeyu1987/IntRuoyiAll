package cn.iocoder.yudao.module.dcc.registrationcertificate.service.file;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePreviewMetadataRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.protection.*;
import cn.iocoder.yudao.module.dcc.dal.mysql.protection.*;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFilePreviewKindEnum;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.dataobject.*;
import cn.iocoder.yudao.module.dcc.registrationcertificate.dal.mysql.*;
import cn.iocoder.yudao.module.dcc.registrationcertificate.service.approval.DccRegistrationCertificateApprovalService;
import cn.iocoder.yudao.module.dcc.service.file.*;
import cn.iocoder.yudao.module.dcc.service.preview.*;
import cn.iocoder.yudao.module.dcc.service.token.*;
import cn.iocoder.yudao.module.infra.dal.dataobject.file.FileDO;
import cn.iocoder.yudao.module.infra.dal.mysql.file.FileMapper;
import cn.iocoder.yudao.module.infra.service.file.FileService;
import cn.iocoder.yudao.module.infra.service.file.access.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.*;

/** A preview capability belongs to one frozen application, never to ordinary certificate viewing. */
@Service
public class DccRegistrationCertificateApprovalPreviewService {
    static final String PURPOSE="REGISTRATION_APPROVAL_PREVIEW";
    static final String OFFICE_AUDIENCE="REGISTRATION_APPROVAL_PREVIEW_CALLBACK";
    static final long TTL=900;
    @Resource private DccRegistrationCertificateApprovalService approvals;
    @Resource private DccRegistrationCertificateAccessRequestFileMapper requestFiles;
    @Resource private DccRegistrationCertificateFileMapper businessFiles;
    @Resource private DccRegistrationCertificateVersionMapper versions;
    @Resource private JdbcTemplate jdbcTemplate;
    @Resource private FileMapper files;
    @Resource private FileService fileService;
    @Resource private DccControlledPreviewAccessService access;
    @Resource private DccViewerTokenService tokens;
    @Resource private DccControlledPreviewWatermarkService watermarks;
    @Resource private DccControlledFileAccessEventMapper events;
    @Resource private DccControlledFileWatermarkTraceMapper traces;
    @Resource private DccOnlyOfficePreviewProperties officeProperties;
    @Resource private DccOnlyOfficePreviewTokenService officeTokens;
    private record Material(DccRegistrationCertificateAccessRequestDO request,DccRegistrationCertificateFileDO business,FileDO source,String version) {}

    private Material requireMaterial(Long tenant,Long actor,Long requestId,String processId,Long businessFileId){
        if(!Objects.equals(tenant,TenantContextHolder.getRequiredTenantId())||businessFileId==null||businessFileId<=0)throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        var request=approvals.requireApprovalDetailRequest(tenant,actor,requestId,processId);
        var links=requestFiles.selectByRequestId(tenant,requestId).stream().filter(link->Objects.equals(link.getBusinessFileId(),businessFileId)).toList();
        var business=businessFiles.selectById(businessFileId);
        if(links.size()!=1||business==null||Boolean.TRUE.equals(business.getDeleted())||!Objects.equals(business.getTenantId(),tenant)
                ||!Objects.equals(links.get(0).getTenantId(),tenant)||!Objects.equals(links.get(0).getRequestId(),requestId)
                ||Boolean.TRUE.equals(links.get(0).getDeleted())||!Objects.equals(links.get(0).getFileKind(),business.getFileKind())
                ||business.getInfraFileId()==null||business.getFileSize()==null||business.getSha256()==null||!business.getSha256().matches("[a-f0-9]{64}")
                ||business.getOriginalName()==null||business.getOriginalName().isBlank()||business.getMimeType()==null||business.getMimeType().isBlank())throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        if("VERSION".equals(business.getOwnerType())){
            var version=versions.selectById(business.getOwnerId());
            if(version==null||Boolean.TRUE.equals(version.getDeleted())||!Objects.equals(version.getTenantId(),tenant)||!Objects.equals(version.getCertificateId(),request.getCertificateId()))throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        }else if("CHANGE".equals(business.getOwnerType())){
            var count=jdbcTemplate.queryForObject("SELECT COUNT(*) FROM dcc_registration_certificate_change WHERE tenant_id=? AND id=? AND certificate_id=? AND approval_request_id=? AND deleted=0",Integer.class,tenant,business.getOwnerId(),request.getCertificateId(),requestId);
            if(!Integer.valueOf(1).equals(count))throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        }else throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        var source=files.selectById(business.getInfraFileId());
        if(source==null||Boolean.TRUE.equals(source.getDeleted()))throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        var identity=DccPreviewSourceIdentity.fromFile(source);
        if(!Objects.equals(source.getSize(),business.getFileSize()))throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        String version=DigestUtil.sha256Hex(JsonUtils.toJsonString(Arrays.asList(PURPOSE,tenant,requestId,processId,businessFileId,business.getSha256(),identity)));
        return new Material(request,business,source,version);
    }
    public DccControlledFilePreviewMetadataRespVO getPreviewMetadata(Long tenant,Long actor,Long requestId,String processId,Long businessFileId,DccRequestAuditContext audit){
        audit.requireRequestId("注册证审批材料预览");var material=requireMaterial(tenant,actor,requestId,processId,businessFileId);
        var previewKind=kind(material);
        if(previewKind==DccControlledFilePreviewKindEnum.DOWNLOAD_ONLY)throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        var issued=access.prepareSourceBoundAccess(new DccPreviewAccessRequest(tenant,actor,material.source().getId(),material.version(),material.business().getOriginalName(),"PREVIEW",PURPOSE,TTL,actor.toString(),actor.toString(),null,null,tenant.toString(),"TRACE_CODE_ONLY",audit.sourceIp(),audit.userAgent(),audit.requestId(),processId),DccPreviewSourceIdentity.fromFile(material.source()));
        var metadata=new DccControlledFilePreviewMetadataRespVO();metadata.setPreviewKind(previewKind.getCode());metadata.setFileName(material.business().getOriginalName());metadata.setContentType(material.business().getMimeType());
        metadata.setViewerToken(issued.viewerToken());metadata.setViewerTokenId(issued.viewerTokenId());metadata.setViewerTokenNonce(issued.viewerTokenNonce());metadata.setAccessEventCode(issued.accessEventCode());metadata.setWatermarkTraceCode(issued.watermarkTraceCode());
        metadata.setPreviewRegisteredAt(issued.issuedAt().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));metadata.setWatermark(watermarks.build(actor,"preview",material.business().getOriginalName()));
        if(previewKind==DccControlledFilePreviewKindEnum.OFFICE){
            if(!officeProperties.isConfigured())metadata.setPreviewUnavailableReason("审批材料Office预览配置缺失："+officeProperties.missingReason());
            else{
                var office=officeTokens.issueBusinessFile(OFFICE_AUDIENCE,BusinessFileAccessOperation.ONLYOFFICE_PREVIEW,material.source().getId(),tenant,actor,null,reference(material),officeProperties.getTokenExpireSeconds().longValue());
                metadata.setOnlyofficeBaseUrl(officeProperties.getBaseUrl().replaceAll("/+$",""));
                metadata.setOnlyofficeDocumentUrl(officeProperties.getPublicFileBaseUrl().replaceAll("/+$","")+"/admin-api/dcc/registration-certificates/approval-materials/"+requestId+"/"+processId+"/"+businessFileId+"/onlyoffice-file?infraFileId="+material.source().getId()+"&token="+office.token());
            }
        }
        return metadata;
    }
    public DccControlledFileBinary readPreview(Long tenant,Long actor,Long requestId,String processId,Long businessFileId,String token,String eventCode,String traceCode,String tokenId,String nonce,DccRequestAuditContext audit){
        audit.requireRequestId("注册证审批材料内容");var material=requireMaterial(tenant,actor,requestId,processId,businessFileId);
        if(kind(material)==DccControlledFilePreviewKindEnum.OFFICE||kind(material)==DccControlledFilePreviewKindEnum.DOWNLOAD_ONLY)throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        if(eventCode==null||eventCode.isBlank()||traceCode==null||traceCode.isBlank())throw exception(CONTROLLED_FILE_VIEWER_TOKEN_INVALID);
        var event=events.selectOne(new LambdaQueryWrapper<DccControlledFileAccessEventDO>().eq(DccControlledFileAccessEventDO::getAccessEventCode,eventCode));
        var trace=traces.selectOne(new LambdaQueryWrapper<DccControlledFileWatermarkTraceDO>().eq(DccControlledFileWatermarkTraceDO::getTraceCode,traceCode));
        if(event==null||trace==null||!Objects.equals(event.getTenantId(),tenant)||!Objects.equals(event.getUserId(),actor)||!Objects.equals(event.getControlledFileId(),material.source().getId())
                ||!Objects.equals(event.getFileVersionNo(),material.version())||!Objects.equals(event.getProcessInstanceId(),processId)||!"PREVIEW".equals(event.getAccessType())||!PURPOSE.equals(event.getPurpose())||!"SUCCESS".equals(event.getResult())
                ||!Objects.equals(trace.getAccessEventId(),event.getId())||!Objects.equals(trace.getAccessEventCode(),eventCode)||!Objects.equals(trace.getUserId(),actor)||!Objects.equals(trace.getControlledFileId(),material.source().getId())||!Objects.equals(trace.getFileVersionNo(),material.version()))throw exception(CONTROLLED_FILE_VIEWER_TOKEN_INVALID);
        tokens.verifySourceBound(token,new DccViewerTokenExpectedContext(tenant,actor,material.source().getId(),material.version(),event.getId(),PURPOSE,TTL,nonce,tokenId),DccPreviewSourceIdentity.fromFile(material.source()));
        return binary(material,actor,true);
    }
    public DccControlledFileBinary readOnlyOffice(Long requestId,String processId,Long businessFileId,Long infraFileId,String token){
        var signed=officeTokens.verifyBusinessFile(token,OFFICE_AUDIENCE,BusinessFileAccessOperation.ONLYOFFICE_PREVIEW,infraFileId);
        Long oldTenant=TenantContextHolder.getTenantId();boolean oldIgnore=TenantContextHolder.isIgnore();
        if(oldTenant!=null&&!Objects.equals(oldTenant,signed.getTenantId()))throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        try{
            TenantContextHolder.setTenantId(signed.getTenantId());TenantContextHolder.setIgnore(false);
            var material=requireMaterial(signed.getTenantId(),signed.getUserId(),requestId,processId,businessFileId);
            if(!Objects.equals(material.source().getId(),infraFileId)||!reference(material).equals(signed.toBusinessFileReference())||kind(material)!=DccControlledFilePreviewKindEnum.OFFICE)throw exception(CONTROLLED_FILE_ACCESS_DENIED);
            return binary(material,signed.getUserId(),false);
        }finally{TenantContextHolder.clear();if(oldTenant!=null)TenantContextHolder.setTenantId(oldTenant);TenantContextHolder.setIgnore(oldIgnore);}
    }
    private BusinessFileAccessReference reference(Material material){return new BusinessFileAccessReference("dcc-registration-approval","DCC_REGISTRATION_APPROVAL_FILE",material.business().getId(),material.version(),material.request().getTenantId(),material.request().getOwnerCompanyId());}
    private DccControlledFilePreviewKindEnum kind(Material material){return DccControlledFilePreviewKindEnum.resolve(material.business().getOriginalName(),material.business().getMimeType());}
    private DccControlledFileBinary binary(Material material,Long actor,boolean watermark){
        byte[] content;
        try{content=fileService.getFileContent(material.source().getConfigId(),material.source().getPath());}
        catch(Exception ex){throw new IllegalStateException("审批材料存储读取失败",ex);}
        if(content==null||content.length!=material.business().getFileSize()||!DigestUtil.sha256Hex(content).equals(material.business().getSha256()))throw exception(CONTROLLED_FILE_ACCESS_DENIED);
        return new DccControlledFileBinary(material.business().getOriginalName(),material.business().getMimeType(),content,watermark?watermarks.build(actor,"preview",material.business().getOriginalName()):null);
    }
}
