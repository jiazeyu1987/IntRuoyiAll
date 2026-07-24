package cn.iocoder.yudao.module.showroom.content.model;

import java.util.List;

public record ShowroomHall(Long hallId, String hallCode, String name, String nameEn, String description,
                           String descriptionEn,
                           String canvasBackgroundImageUrl,
                           List<ShowroomHallProductMapping> productMappings,
                           List<ShowroomHallItemMapping> itemMappings) {

    public ShowroomHall {
        canvasBackgroundImageUrl = canvasBackgroundImageUrl == null ? "" : canvasBackgroundImageUrl;
        productMappings = productMappings == null ? List.of() : List.copyOf(productMappings);
        itemMappings = itemMappings == null ? List.of() : List.copyOf(itemMappings);
    }

    public ShowroomHall(Long hallId, String hallCode, String name, String nameEn, String description,
                        String descriptionEn, List<ShowroomHallProductMapping> productMappings,
                        List<ShowroomHallItemMapping> itemMappings) {
        this(hallId, hallCode, name, nameEn, description, descriptionEn, "",
                productMappings, itemMappings);
    }

    public ShowroomHall(Long hallId, String hallCode, String name, String nameEn, String description,
                        String descriptionEn, List<ShowroomHallProductMapping> productMappings) {
        this(hallId, hallCode, name, nameEn, description, descriptionEn, "",
                productMappings == null ? List.of() : List.copyOf(productMappings),
                productMappings == null ? List.of() : productMappings.stream()
                        .map(mapping -> new ShowroomHallItemMapping(ShowroomHallItemMapping.TYPE_PRODUCT,
                                mapping.productId(), mapping.displayOrder(), mapping.layoutX(), mapping.layoutY(),
                                mapping.layoutWidth(), mapping.layoutHeight()))
                        .toList());
    }
}
