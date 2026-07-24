package cn.iocoder.yudao.module.showroom.content.model;

import java.util.List;

public record ShowroomHallItemOption(String itemType, Long itemId, String itemCode, String nameCn,
                                     String nameEn, Integer revisionNo, boolean incomplete,
                                     String previewImageUrl, List<Long> hallIds) {

    public ShowroomHallItemOption {
        hallIds = hallIds == null ? List.of() : List.copyOf(hallIds);
    }
}
