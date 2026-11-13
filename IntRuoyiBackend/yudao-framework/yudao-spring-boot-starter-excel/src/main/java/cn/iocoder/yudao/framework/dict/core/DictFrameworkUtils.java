package cn.iocoder.yudao.framework.dict.core;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.biz.system.dict.DictDataCommonApi;
import com.google.common.cache.CacheBuilder;
import cn.iocoder.yudao.framework.common.biz.system.dict.dto.DictDataRespDTO;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;

/**
 * 字典工具类
 *
 * @author 瑛泰源码
 */
@Slf4j
public class DictFrameworkUtils {

    private static DictDataCommonApi dictDataApi;

    private record DictKey(Long tenantId, String type) {}

    // Synchronous expiry keeps database reads in the calling tenant's context.
    private static final LoadingCache<DictKey, List<DictDataRespDTO>> GET_DICT_DATA_CACHE = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(1)).build(new CacheLoader<>() {
                @Override
                public List<DictDataRespDTO> load(DictKey key) {
                    if (!key.tenantId().equals(dictDataApi.getCurrentTenantId())) {
                        throw new IllegalStateException("DICT_TENANT_CONTEXT_MISMATCH");
                    }
                    return dictDataApi.getDictDataList(key.type());
                }
            });

    private static DictKey key(String type) {
        return new DictKey(Objects.requireNonNull(dictDataApi.getCurrentTenantId(), "DICT_TENANT_REQUIRED"), type);
    }

    public static void clearTenantCache(Long tenantId) {
        GET_DICT_DATA_CACHE.asMap().keySet().removeIf(key -> key.tenantId().equals(tenantId));
    }

    public static void init(DictDataCommonApi dictDataApi) {
        DictFrameworkUtils.dictDataApi = dictDataApi;
        log.info("[init][初始化 DictFrameworkUtils 成功]");
    }

    public static void clearCache() {
        GET_DICT_DATA_CACHE.invalidateAll();
    }

    @SneakyThrows
    public static String parseDictDataLabel(String dictType, Integer value) {
        if (value == null) {
            return null;
        }
        return parseDictDataLabel(dictType, String.valueOf(value));
    }

    @SneakyThrows
    public static String parseDictDataLabel(String dictType, String value) {
        List<DictDataRespDTO> dictDatas = GET_DICT_DATA_CACHE.get(key(dictType));
        DictDataRespDTO dictData = CollUtil.findOne(dictDatas, data -> Objects.equals(data.getValue(), value));
        return dictData != null ? dictData.getLabel(): null;
    }

    @SneakyThrows
    public static List<String> getDictDataLabelList(String dictType) {
        List<DictDataRespDTO> dictDatas = GET_DICT_DATA_CACHE.get(key(dictType));
        return convertList(dictDatas, DictDataRespDTO::getLabel);
    }

    @SneakyThrows
    public static String parseDictDataValue(String dictType, String label) {
        List<DictDataRespDTO> dictDatas = GET_DICT_DATA_CACHE.get(key(dictType));
        DictDataRespDTO dictData = CollUtil.findOne(dictDatas, data -> Objects.equals(data.getLabel(), label));
        return dictData!= null ? dictData.getValue(): null;
    }

    @SneakyThrows
    public static List<String> getDictDataValueList(String dictType) {
        List<DictDataRespDTO> dictDatas = GET_DICT_DATA_CACHE.get(key(dictType));
        return convertList(dictDatas, DictDataRespDTO::getValue);
    }
}
