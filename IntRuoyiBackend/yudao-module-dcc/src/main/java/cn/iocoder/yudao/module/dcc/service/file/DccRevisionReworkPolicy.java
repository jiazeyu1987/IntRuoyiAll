package cn.iocoder.yudao.module.dcc.service.file;

import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import java.util.*;

/** Exact lineage validation, shared by correction/checkin, preparation and formal allocation. */
final class DccRevisionReworkPolicy {
    private DccRevisionReworkPolicy() {}
    static boolean sameIdentity(DccControlledFileDO a,DccControlledFileDO b) {
        return a!=null && b!=null && Objects.equals(a.getTenantId(),b.getTenantId())
                && Objects.equals(a.getMasterId(),b.getMasterId()) && Objects.equals(a.getDccProjectCodeId(),b.getDccProjectCodeId())
                && Objects.equals(a.getCategoryId(),b.getCategoryId()) && Objects.equals(a.getFileTypeTaxonomyId(),b.getFileTypeTaxonomyId())
                && Objects.equals(a.getFileNumber(),b.getFileNumber()) && Objects.equals(a.getSourceOriginalFileName(),b.getSourceOriginalFileName());
    }
    static boolean failedFormal(DccControlledFileDO file) {
        return file!=null && Set.of("REJECTED","PENDING_APPLICANT_REWORK","WITHDRAWN").contains(file.getStatus())
                && Set.of("PARTIAL","REPLACEMENT").contains(Objects.toString(file.getRevisionChangeType(),""))
                && file.getSelectedIterationControlledFileId()!=null && file.getRevisionSourceControlledFileId()!=null
                && file.getProcessInstanceId()!=null && !file.getProcessInstanceId().isBlank() && file.getControlledTime()==null;
    }
    static DccControlledFileDO predecessor(DccControlledFileDO selected,List<DccControlledFileDO> chain,DccControlledFileVersionPolicy policy) {
        if(selected==null)return null;
        var versions=new HashMap<Long,DccControlledFileDO>();for(var file:chain)if(versions.put(file.getId(),file)!=null)throw new IllegalArgumentException("duplicate lineage identity");
        var cursor=selected;var visited=new HashSet<Long>();
        while(cursor!=null){
            if(!visited.add(cursor.getId()))throw new IllegalArgumentException("cyclic revision rework lineage");
            if(!sameIdentity(selected,cursor) || !Objects.equals(selected.getRequesterId(),cursor.getRequesterId())
                    || !Objects.equals(selected.getRevisionBaseActiveControlledFileId(),cursor.getRevisionBaseActiveControlledFileId()))return null;
            if(failedFormal(cursor))return policy.requireStored(selected).formal().display().equals(cursor.getVersionNo())?cursor:null;
            if(!"WORKING".equals(cursor.getStatus()) || cursor.getRevisionChangeType()!=null || cursor.getPredecessorControlledFileId()==null)return null;
            var previous=versions.get(cursor.getPredecessorControlledFileId());
            if(previous==null || !policy.requireStored(cursor).isWorkingIteration()
                    || !policy.requireStored(cursor).formal().display().equals(policy.requireStored(previous).formal().display())
                    || policy.requireStored(cursor).compareTo(policy.requireStored(previous))<=0)return null;
            cursor=previous;
        }
        return null;
    }
    static boolean correctionMatchesBaseline(DccControlledFileDO baseline,DccControlledFileDO selected,List<DccControlledFileDO> chain,DccControlledFileVersionPolicy policy) {
        var prior=predecessor(selected,chain,policy);
        return prior!=null && sameIdentity(baseline,prior) && Objects.equals(baseline.getId(),prior.getRevisionSourceControlledFileId())
                && Objects.equals(baseline.getId(),prior.getRevisionBaseActiveControlledFileId())
                && policy.formalTarget(baseline,prior.getRevisionChangeType()).display().equals(prior.getVersionNo());
    }
    record Attempt(DccControlledFileDO predecessor,int number) {}
    static Attempt requireAttempt(DccControlledFileDO baseline,DccControlledFileDO selected,String intent,List<DccControlledFileDO> chain,DccControlledFileVersionPolicy policy) {
        String target=policy.formalTarget(baseline,intent).display();
        var allocated=chain.stream().filter(f->target.equals(f.getVersionNo())).toList();
        var prior=predecessor(selected,chain,policy);
        if(allocated.isEmpty()) {
            if(prior!=null || !policy.requireStored(selected).formal().display().equals(baseline.getVersionNo()))
                throw new IllegalArgumentException("selected correction has no legal failed formal predecessor");
            return new Attempt(null,1);
        }
        if(prior==null || !correctionMatchesBaseline(baseline,selected,chain,policy) || !intent.equals(prior.getRevisionChangeType()))
            throw new IllegalArgumentException("formal target is allocated; select its exact failed attempt correction");
        var byId=new HashMap<Long,DccControlledFileDO>();for(var row:allocated)byId.put(row.getId(),row);
        var cursor=prior;int expected=prior.getRevisionAttemptNo()==null?1:prior.getRevisionAttemptNo();int next=Math.addExact(expected,1);
        var visited=new HashSet<Long>();
        while(cursor!=null){
            if(!visited.add(cursor.getId()) || !failedFormal(cursor) || !sameIdentity(baseline,cursor)
                    || !Objects.equals(selected.getRequesterId(),cursor.getRequesterId())
                    || !Objects.equals(baseline.getId(),cursor.getRevisionSourceControlledFileId())
                    || !intent.equals(cursor.getRevisionChangeType()) || (cursor.getRevisionAttemptNo()==null?1:cursor.getRevisionAttemptNo())!=expected)
                throw new IllegalArgumentException("failed formal attempt lineage is inconsistent");
            Long predecessorId=cursor.getReworkPredecessorControlledFileId();expected--;
            if(expected==0){if(predecessorId!=null)throw new IllegalArgumentException("first attempt cannot have a predecessor");cursor=null;}
            else {cursor=predecessorId==null?null:byId.get(predecessorId);if(cursor==null)throw new IllegalArgumentException("missing failed attempt predecessor");}
        }
        if(visited.size()!=allocated.size() || expected!=0)throw new IllegalArgumentException("another formal attempt already exists for this target");
        return new Attempt(prior,next);
    }
    static boolean canAllocate(DccControlledFileDO baseline,DccControlledFileDO selected,String intent,List<DccControlledFileDO> chain,DccControlledFileVersionPolicy policy) {
        try { requireAttempt(baseline,selected,intent,chain,policy);return true; }
        catch(IllegalArgumentException failure){return false;}
    }
}
