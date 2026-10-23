package cn.iocoder.yudao.module.dcc.service.file.relations;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;

/** Explicit D arrangement deadline; no date correction, omitted seconds, or timestamp conversion. */
public class DccRelationDeadlineDeserializer extends JsonDeserializer<LocalDateTime> {
    private static final DateTimeFormatter FORMAT=DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss",Locale.ROOT)
            .withResolverStyle(ResolverStyle.STRICT);
    @Override
    public LocalDateTime deserialize(JsonParser parser,DeserializationContext context) throws IOException {
        if(parser.currentToken()!=JsonToken.VALUE_STRING)throw invalid(parser);
        String text=parser.getText();
        if(!text.matches("[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}"))throw invalid(parser);
        try{
            var deadline=LocalDateTime.parse(text,FORMAT);
            if(deadline.getYear()<1)throw invalid(parser);
            return deadline;
        }catch(DateTimeParseException error){throw invalid(parser);}
    }
    private JsonMappingException invalid(JsonParser parser){
        return JsonMappingException.from(parser,"整改期限必须为有效日期时间，格式 yyyy-MM-dd HH:mm:ss，精确到秒");
    }
}
