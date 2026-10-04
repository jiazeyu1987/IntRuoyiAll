package cn.iocoder.yudao.module.dcc.service.file.relations;

import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMasterMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import static cn.iocoder.yudao.module.dcc.service.file.relations.DccRelationContracts.*;

@Service
public class DccLatestControlledFileResolverImpl implements DccLatestControlledFileResolver {
    @Resource(name="dccControlledFileMapper") private DccControlledFileMapper fileMapper;
    @Resource private DccControlledFileMasterMapper masterMapper;
    @Resource private JdbcTemplate jdbc;
    public FileVersion resolveLatest(Long masterId) {
        var master=masterId==null?null:masterMapper.selectById(masterId);
        if(master==null || !Objects.equals(master.getTenantId(),TenantContextHolder.getRequiredTenantId())
                || master.getLatestControlledFileId()==null) throw new DccRelationFailure("DCC_LATEST_CONTROLLED_MISSING");
        var result=resolveSelected(master.getLatestControlledFileId());
        if(!Objects.equals(result.masterId(),masterId)) throw new DccRelationFailure("DCC_LATEST_CONTROLLED_IDENTITY_INVALID");
        return result;
    }
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public FileVersion resolveLatestForUpdate(Long masterId){
        var master=masterId==null?null:masterMapper.selectByIdForUpdate(masterId);
        if(master==null || !Objects.equals(master.getId(),masterId)
                || !Objects.equals(master.getTenantId(),TenantContextHolder.getRequiredTenantId())
                || master.getLatestControlledFileId()==null)throw new DccRelationFailure("DCC_LATEST_CONTROLLED_MISSING");
        var result=resolveSelectedForUpdate(master.getLatestControlledFileId());
        if(!Objects.equals(result.masterId(),masterId))throw new DccRelationFailure("DCC_LATEST_CONTROLLED_IDENTITY_INVALID");
        return result;
    }
    private DccControlledFileDO selected(Long fileId) {
        var file=fileId==null?null:fileMapper.selectById(fileId);
        if(file==null || !Objects.equals(file.getTenantId(),TenantContextHolder.getRequiredTenantId())
                || file.getMasterId()==null) throw new DccRelationFailure("DCC_FILE_VERSION_NOT_FOUND");
        return file;
    }
    public FileVersion resolveSelected(Long fileId) {
        return project(selected(fileId));
    }
    @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
    public FileVersion resolveSelectedForUpdate(Long fileId){
        var file=fileId==null?null:fileMapper.selectByIdAndTenantForUpdate(TenantContextHolder.getRequiredTenantId(),fileId);
        if(file==null || !Objects.equals(fileId,file.getId())
                || !Objects.equals(file.getTenantId(),TenantContextHolder.getRequiredTenantId()) || file.getMasterId()==null)
            throw new DccRelationFailure("DCC_FILE_VERSION_NOT_FOUND");
        return project(file);
    }
    private FileVersion project(DccControlledFileDO file){
        boolean controlled=file.getControlledTime()!=null &&
                ("ACTIVE".equals(file.getStatus()) || "CONTROLLED_PENDING_EFFECTIVE".equals(file.getStatus()));
        boolean pending=controlled && "CONTROLLED_PENDING_EFFECTIVE".equals(file.getStatus());
        return new FileVersion(file.getTenantId(),file.getId(),file.getMasterId(),file.getDccProjectCodeId(),
                file.getFileNumber(),file.getFileName(),file.getVersionNo(),file.getStatus(),controlled,pending,
                controlled && "ACTIVE".equals(file.getStatus()) && file.getActivatedTime()!=null);
    }
    public void assertControlledEvent(ControlledEvent event) {
        if(event==null || !Objects.equals(event.tenantId(),TenantContextHolder.getRequiredTenantId())
                || event.eventKey()==null || event.eventKey().isBlank())
            throw new DccRelationFailure("DCC_CONTROLLED_EVENT_INVALID");
        var file=selected(event.controlledFileId());
        if(!Objects.equals(file.getMasterId(),event.masterId())
                || file.getControlledTime()==null || !Objects.equals(file.getControlledTime(),event.controlledAt())
                || !Objects.equals(file.getProcessInstanceId(),event.applicationRound()))
            throw new DccRelationFailure("DCC_CONTROLLED_EVENT_IDENTITY_INVALID");
        Long facts=jdbc.queryForObject("SELECT COUNT(*) FROM dcc_workflow_lifecycle_event WHERE tenant_id=? AND event_key=? AND event_type='CONTROLLED' AND master_id=? AND controlled_file_id=? AND approval_process_instance_id=? AND occurred_at=?",
                Long.class,event.tenantId(),event.eventKey(),event.masterId(),event.controlledFileId(),event.applicationRound(),event.controlledAt());
        if(!Objects.equals(facts,1L))throw new DccRelationFailure("DCC_CONTROLLED_EVENT_NOT_REGISTERED");
    }
}
