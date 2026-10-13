package cn.iocoder.yudao.module.system.controller.admin.user.vo.user;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

/** 仅约束用户保存请求，不改变全局 Long 绑定规则。 */
public class UserPostIdsDeserializer extends JsonDeserializer<Set<Long>> {

    @Override
    public Set<Long> deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (!parser.isExpectedStartArrayToken()) {
            return context.reportInputMismatch(Set.class, "岗位编号必须是数组");
        }
        Set<Long> ids = new LinkedHashSet<>();
        while (parser.nextToken() != JsonToken.END_ARRAY) {
            JsonToken token = parser.currentToken();
            if (token != JsonToken.VALUE_NUMBER_INT && token != JsonToken.VALUE_STRING) {
                return context.reportInputMismatch(Set.class, "岗位编号必须是正整数或精确十进制字符串");
            }
            String identity = parser.getText();
            if (!identity.matches("[1-9][0-9]*")) {
                return context.reportInputMismatch(Set.class, "岗位编号必须是规范正整数");
            }
            try {
                ids.add(Long.parseLong(identity));
            } catch (NumberFormatException ex) {
                return context.reportInputMismatch(Set.class, "岗位编号超出 Long 范围");
            }
        }
        return ids;
    }
}
