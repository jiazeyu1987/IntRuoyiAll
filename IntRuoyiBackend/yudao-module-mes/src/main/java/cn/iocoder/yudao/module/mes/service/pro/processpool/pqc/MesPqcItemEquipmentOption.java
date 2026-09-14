package cn.iocoder.yudao.module.mes.service.pro.processpool.pqc;

public record MesPqcItemEquipmentOption(String itemCode,
                                        Long equipmentId,
                                        String equipmentCode,
                                        String equipmentName,
                                        String equipmentNumber,
                                        Boolean defaultFlag,
                                        Integer sort) {
}
