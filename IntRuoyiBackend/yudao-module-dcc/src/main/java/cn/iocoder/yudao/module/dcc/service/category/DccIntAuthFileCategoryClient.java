package cn.iocoder.yudao.module.dcc.service.category;

import java.util.List;

public interface DccIntAuthFileCategoryClient {

    List<IntAuthFileCategory> listFileCategories();

    record IntAuthFileCategory(Long id, String name, boolean seededFromJson, boolean active) {
    }

}
