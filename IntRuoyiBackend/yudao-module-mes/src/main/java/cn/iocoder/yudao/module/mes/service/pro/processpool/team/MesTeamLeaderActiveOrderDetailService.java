package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

public interface MesTeamLeaderActiveOrderDetailService {

    MesTeamLeaderActiveOrderDetail getDetail(Long leaderUserId, Long activeOrderId);

    MesTeamLeaderActiveOrderDetail getFormalDetail(Long activeOrderId);

    MesTeamLeaderActiveOrderDetail getArchivedFormalDetail(Long activeOrderId);

    /** Keeps formal source validation but projects only the requested signature's records. */
    MesTeamLeaderActiveOrderDetail getArchivedSignatureDetail(Long activeOrderId, Long signatureId);
}
