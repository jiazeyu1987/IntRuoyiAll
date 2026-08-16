package cn.iocoder.yudao.module.mes.service.pro.frontline;

public interface MesFrontlineSessionSnapshotService {

    MesFrontlineSessionSnapshotReference issue(MesFrontlineSessionSnapshotContent content);

    MesFrontlineSessionSnapshot require(String snapshotId, String snapshotHash, Long loginUserId);

}
