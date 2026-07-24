package cn.iocoder.yudao.framework.common.util.json;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.iocoder.yudao.framework.common.util.json.databind.TimestampLocalDateTimeDeserializer;
import cn.iocoder.yudao.framework.common.util.json.databind.TimestampLocalDateTimeSerializer;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JSON 宸ュ叿绫?
 *
 * @author 鐟涙嘲婧愮爜
 */
@Slf4j
public class JsonUtils {

    @Getter
    private static ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL); // 蹇界暐 null 鍊?
        // 瑙ｅ喅 LocalDateTime 鐨勫簭鍒楀寲
        SimpleModule simpleModule = new JavaTimeModule()
                .addSerializer(LocalDateTime.class, TimestampLocalDateTimeSerializer.INSTANCE)
                .addDeserializer(LocalDateTime.class, TimestampLocalDateTimeDeserializer.INSTANCE);
        objectMapper.registerModules(simpleModule);
    }

    /**
     * 鍒濆鍖?objectMapper 灞炴€?
     * <p>
     * 閫氳繃杩欐牱鐨勬柟寮忥紝浣跨敤 Spring 鍒涘缓鐨?ObjectMapper Bean
     *
     * @param objectMapper ObjectMapper 瀵硅薄
     */
    public static void init(ObjectMapper objectMapper) {
        JsonUtils.objectMapper = objectMapper;
    }

    @SneakyThrows
    public static String toJsonString(Object object) {
        return objectMapper.writeValueAsString(object);
    }

    @SneakyThrows
    public static byte[] toJsonByte(Object object) {
        return objectMapper.writeValueAsBytes(object);
    }

    @SneakyThrows
    public static String toJsonPrettyString(Object object) {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
    }

    public static <T> T parseObject(String text, Class<T> clazz) {
        if (StrUtil.isEmpty(text)) {
            return null;
        }
        try {
            return objectMapper.readValue(text, clazz);
        } catch (IOException e) {
            log.error("json parse err,json:{}", text, e);
            throw new RuntimeException(e);
        }
    }

    public static <T> T parseObject(String text, String path, Class<T> clazz) {
        if (StrUtil.isEmpty(text)) {
            return null;
        }
        try {
            JsonNode treeNode = objectMapper.readTree(text);
            JsonNode pathNode = treeNode.path(path);
            return objectMapper.readValue(pathNode.toString(), clazz);
        } catch (IOException e) {
            log.error("json parse err,json:{}", text, e);
            throw new RuntimeException(e);
        }
    }

    public static <T> T parseObject(String text, Type type) {
        if (StrUtil.isEmpty(text)) {
            return null;
        }
        try {
            return objectMapper.readValue(text, objectMapper.getTypeFactory().constructType(type));
        } catch (IOException e) {
            log.error("json parse err,json:{}", text, e);
            throw new RuntimeException(e);
        }
    }

    public static <T> T parseObject(byte[] text, Type type) {
        if (ArrayUtil.isEmpty(text)) {
            return null;
        }
        try {
            return objectMapper.readValue(text, objectMapper.getTypeFactory().constructType(type));
        } catch (IOException e) {
            log.error("json parse err,json:{}", text, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 灏嗗瓧绗︿覆瑙ｆ瀽鎴愭寚瀹氱被鍨嬬殑瀵硅薄
     * 浣跨敤 {@link #parseObject(String, Class)} 鏃讹紝鍦ˊJsonTypeInfo(use = JsonTypeInfo.Id.CLASS) 鐨勫満鏅笅锛?
     * 濡傛灉 text 娌℃湁 class 灞炴€э紝鍒欎細鎶ラ敊銆傛鏃讹紝浣跨敤杩欎釜鏂规硶锛屽彲浠ヨВ鍐炽€?
     *
     * @param text 瀛楃涓?
     * @param clazz 绫诲瀷
     * @return 瀵硅薄
     */
    public static <T> T parseObject2(String text, Class<T> clazz) {
        if (StrUtil.isEmpty(text)) {
            return null;
        }
        return JSONUtil.toBean(text, clazz);
    }

    public static <T> T parseObject(byte[] bytes, Class<T> clazz) {
        if (ArrayUtil.isEmpty(bytes)) {
            return null;
        }
        try {
            return objectMapper.readValue(bytes, clazz);
        } catch (IOException e) {
            log.error("json parse err,json:{}", bytes, e);
            throw new RuntimeException(e);
        }
    }

    public static <T> T parseObject(String text, TypeReference<T> typeReference) {
        if (StrUtil.isEmpty(text)) {
            return null;
        }
        try {
            return objectMapper.readValue(text, typeReference);
        } catch (IOException e) {
            log.error("json parse err,json:{}", text, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 瑙ｆ瀽 JSON 瀛楃涓叉垚鎸囧畾绫诲瀷鐨勫璞★紝濡傛灉瑙ｆ瀽澶辫触锛屽垯杩斿洖 null
     *
     * @param text 瀛楃涓?
     * @param typeReference 绫诲瀷寮曠敤
     * @return 鎸囧畾绫诲瀷鐨勫璞?
     */
    public static <T> T parseObjectQuietly(String text, TypeReference<T> typeReference) {
        try {
            return objectMapper.readValue(text, typeReference);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * 瑙ｆ瀽 JSON 瀛楃涓叉垚鎸囧畾绫诲瀷鐨勫璞★紝濡傛灉瑙ｆ瀽澶辫触锛屽垯杩斿洖 null
     *
     * @param text 瀛楃涓?
     * @param clazz 绫诲瀷
     * @return 鎸囧畾绫诲瀷鐨勫璞?
     */
    public static <T> T parseObjectQuietly(String text, Class<T> clazz) {
        if (StrUtil.isEmpty(text)) {
            return null;
        }
        try {
            return objectMapper.readValue(text, clazz);
        } catch (IOException e) {
            return null;
        }
    }

    public static <T> List<T> parseArray(String text, Class<T> clazz) {
        if (StrUtil.isEmpty(text)) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(text, objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (IOException e) {
            log.error("json parse err,json:{}", text, e);
            throw new RuntimeException(e);
        }
    }

    public static <T> List<T> parseArray(String text, String path, Class<T> clazz) {
        if (StrUtil.isEmpty(text)) {
            return null;
        }
        try {
            JsonNode treeNode = objectMapper.readTree(text);
            JsonNode pathNode = treeNode.path(path);
            return objectMapper.readValue(pathNode.toString(), objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (IOException e) {
            log.error("json parse err,json:{}", text, e);
            throw new RuntimeException(e);
        }
    }

    public static JsonNode parseTree(String text) {
        try {
            return objectMapper.readTree(text);
        } catch (IOException e) {
            log.error("json parse err,json:{}", text, e);
            throw new RuntimeException(e);
        }
    }

    public static JsonNode parseTree(byte[] text) {
        try {
            return objectMapper.readTree(text);
        } catch (IOException e) {
            log.error("json parse err,json:{}", text, e);
            throw new RuntimeException(e);
        }
    }

    public static boolean isJson(String text) {
        return JSONUtil.isTypeJSON(text);
    }

    /**
     * 鍒ゆ柇瀛楃涓叉槸鍚︿负 JSON 绫诲瀷鐨勫瓧绗︿覆
     * @param str 瀛楃涓?
     */
    public static boolean isJsonObject(String str) {
        return JSONUtil.isTypeJSONObject(str);
    }

    /**
     * 灏?Object 杞崲涓虹洰鏍囩被鍨?
     * <p>
     * 閬垮厤鍏堣浆 jsonString 鍐?parseObject 鐨勬€ц兘鎹熻€?
     *
     * @param obj   婧愬璞★紙鍙互鏄?Map銆丳OJO 绛夛級
     * @param clazz 鐩爣绫诲瀷
     * @return 杞崲鍚庣殑瀵硅薄
     */
    public static <T> T convertObject(Object obj, Class<T> clazz) {
        if (obj == null) {
            return null;
        }
        if (clazz.isInstance(obj)) {
            return clazz.cast(obj);
        }
        return objectMapper.convertValue(obj, clazz);
    }

    /**
     * 灏?Object 杞崲涓虹洰鏍囩被鍨嬶紙鏀寔娉涘瀷锛?
     *
     * @param obj           婧愬璞?
     * @param typeReference 鐩爣绫诲瀷寮曠敤
     * @return 杞崲鍚庣殑瀵硅薄
     */
    public static <T> T convertObject(Object obj, TypeReference<T> typeReference) {
        if (obj == null) {
            return null;
        }
        return objectMapper.convertValue(obj, typeReference);
    }

    /**
     * 灏?Object 杞崲涓?List 绫诲瀷
     * <p>
     * 閬垮厤鍏堣浆 jsonString 鍐?parseArray 鐨勬€ц兘鎹熻€?
     *
     * @param obj   婧愬璞★紙鍙互鏄?List銆佹暟缁勭瓑锛?
     * @param clazz 鐩爣鍏冪礌绫诲瀷
     * @return 杞崲鍚庣殑 List
     */
    public static <T> List<T> convertList(Object obj, Class<T> clazz) {
        if (obj == null) {
            return new ArrayList<>();
        }
        return objectMapper.convertValue(obj, objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
    }

}
