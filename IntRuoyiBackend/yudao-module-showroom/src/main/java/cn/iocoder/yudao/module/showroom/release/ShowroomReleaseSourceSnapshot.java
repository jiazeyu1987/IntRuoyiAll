package cn.iocoder.yudao.module.showroom.release;

import cn.iocoder.yudao.module.showroom.content.model.ShowroomCompanyRevision;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomCompanySnapshot;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomAwardRevision;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomAwardSnapshot;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomHall;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductAttachment;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductRevision;
import cn.iocoder.yudao.module.showroom.content.model.ShowroomProductSnapshot;
import cn.iocoder.yudao.module.showroom.narration.ShowroomNarrationVersion;

import java.time.Instant;
import java.util.List;
import java.util.Map;

record ShowroomReleaseSourceSnapshot(
        Long companyRevisionId,
        String hallSnapshotHash,
        String hallProductMappingHash,
        Instant resolvedAt,
        ShowroomCompanySnapshot companySnapshot,
        ShowroomCompanyRevision companyRevision,
        ResolvedBinarySource companyHomeImage,
        ResolvedNarrationPair companyNarrations,
        List<ResolvedHall> halls,
        Map<Long, ResolvedProduct> productsById,
        Map<Long, ResolvedAward> awardsById,
        List<Long> productRevisionIds,
        List<Long> awardRevisionIds,
        List<Long> previewAssetVersionIds,
        List<Long> narrationVersionIds) {

    record ResolvedHall(ShowroomHall hall, ResolvedBinarySource previewImage, ResolvedNarrationPair narrations) {
    }

    record ResolvedProduct(ShowroomProductSnapshot snapshot, ShowroomProductRevision revision,
                           ResolvedBinarySource previewImage, ResolvedNarrationPair narrations,
                           List<ResolvedProductAttachment> attachments) {
    }

    record ResolvedAward(ShowroomAwardSnapshot snapshot, ShowroomAwardRevision revision,
                         ResolvedBinarySource previewImage, ResolvedNarrationPair narrations) {
    }

    record ResolvedProductAttachment(ShowroomProductAttachment attachment, String url) {
    }

    record ResolvedNarrationPair(ShowroomNarrationVersion zh, ShowroomNarrationVersion en,
                                 ResolvedBinarySource zhAudio, ResolvedBinarySource enAudio) {
    }
}
