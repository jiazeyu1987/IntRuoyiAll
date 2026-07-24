package cn.iocoder.yudao.module.mes.service.pro.schedule.identity;

/**
 * 产线 + 基础工序身份。
 *
 * <p>仅用于资源能力匹配和产线工序可用时间，不代表路线工序身份。</p>
 */
public record LineProcessIdentity(Long lineId, Long processId) {

    public static LineProcessIdentity of(Long lineId, Long processId) {
        return new LineProcessIdentity(lineId, processId);
    }

    public String availabilityKey() {
        return availabilityKey(lineId, processId);
    }

    public static String availabilityKey(Long lineId, Long processId) {
        return lineId + "_" + processId;
    }

}
