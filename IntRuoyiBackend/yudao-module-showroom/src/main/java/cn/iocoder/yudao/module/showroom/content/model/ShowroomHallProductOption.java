package cn.iocoder.yudao.module.showroom.content.model;

import java.util.List;

public record ShowroomHallProductOption(Long productId, Long productMasterId, String productCode, String nameCn, Integer revisionNo,
                                        boolean incomplete, String previewImageUrl, List<Long> hallIds) {

    public ShowroomHallProductOption {
        previewImageUrl = previewImageUrl == null ? "" : previewImageUrl;
        hallIds = hallIds == null ? List.of() : List.copyOf(hallIds);
    }

    public ShowroomHallProductOption(Long productId, String productCode, String nameCn, Integer revisionNo,
                                     boolean incomplete, String previewImageUrl, List<Long> hallIds) {
        this(productId, null, productCode, nameCn, revisionNo, incomplete, previewImageUrl, hallIds);
    }
}
