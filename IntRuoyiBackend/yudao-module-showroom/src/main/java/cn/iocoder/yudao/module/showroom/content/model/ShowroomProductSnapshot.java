package cn.iocoder.yudao.module.showroom.content.model;

import java.util.Optional;

public record ShowroomProductSnapshot(Long productId, Long productMasterId, String productCode,
                                      String legacyProductCode, Optional<Long> currentRevisionId,
                                      boolean incomplete, boolean live) {

    public ShowroomProductSnapshot(Long productId, Long productMasterId, String productCode,
                                   Optional<Long> currentRevisionId, boolean incomplete, boolean live) {
        this(productId, productMasterId, productCode, null, currentRevisionId, incomplete, live);
    }

    public ShowroomProductSnapshot(Long productId, String productCode, Optional<Long> currentRevisionId,
                                   boolean incomplete, boolean live) {
        this(productId, null, productCode, null, currentRevisionId, incomplete, live);
    }
}
