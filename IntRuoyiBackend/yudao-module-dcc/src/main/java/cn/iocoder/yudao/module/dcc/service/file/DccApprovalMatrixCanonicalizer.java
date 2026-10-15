package cn.iocoder.yudao.module.dcc.service.file;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import java.util.*;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
public final class DccApprovalMatrixCanonicalizer {
    public static final String FORMAT="DCC_APPROVAL_MATRIX_BASELINE_V2";
    private static final Set<String> ROOT=Set.of("formatVersion","tenantId","categoryId","fileTypeTaxonomyId","routeId","routeVersionNo","actionType","effectiveTime","routeRemark","nodes");
    private static final Set<String> NODE=Set.of("id","stageNo","stageCode","stageName","stageOrder","sort","candidateSourceType","candidateSourceId","candidateSourceIds","approveMethod","approveRatio","requireAllApprovals","required","stageType","subjectLabel","marker","subjectType","subjectId","subjectName","subjectDepartmentPath","ruleRemark");
    private DccApprovalMatrixCanonicalizer(){}
    /** The configured NON_NULL serializer omits original nullable fields; reconstruct the declared
     * immutable VO shape before applying the unchanged strict raw whitelist. No source values are inferred. */
    public static String canonicalizeBaseline(cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccApprovalMatrixBaselineVO baseline){
        if(baseline==null)throw invalid("baseline");
        Map<String,Object> raw=JsonUtils.parseObject(JsonUtils.toJsonString(baseline),Map.class);raw.remove("matrixDigest");
        ROOT.forEach(key->raw.putIfAbsent(key,null));
        if(!(raw.get("nodes") instanceof List<?> rows))throw invalid("nodes");
        for(Object row:rows){if(!(row instanceof Map<?,?>))throw invalid("node");var node=(Map<String,Object>)row;NODE.forEach(key->node.putIfAbsent(key,null));}
        return canonicalize(raw);
    }
    public static String canonicalize(Map<String,Object> raw){
        exact(raw,ROOT);if(!FORMAT.equals(raw.get("formatVersion")))throw invalid("formatVersion");
        var result=new TreeMap<String,Object>();result.put("formatVersion",FORMAT);
        for(String key:List.of("tenantId","categoryId","fileTypeTaxonomyId","routeId"))result.put(key,identity(raw.get(key)));
        int version=integer(raw.get("routeVersionNo"));if(version<=0)throw invalid("routeVersionNo");result.put("routeVersionNo",version);
        result.put("actionType",text(raw.get("actionType"),true));result.put("routeRemark",text(raw.get("routeRemark"),true));
        Object time=raw.get("effectiveTime");result.put("effectiveTime",time==null?null:time(text(time,false)));
        if(!(raw.get("nodes") instanceof List<?> rows) || rows.isEmpty())throw invalid("nodes");
        var nodes=new ArrayList<Map<String,Object>>();var ids=new HashSet<String>();
        for(Object row:rows){if(!(row instanceof Map<?,?>))throw invalid("node");var node=(Map<String,Object>)row;exact(node,NODE);
            var copy=new TreeMap<String,Object>();String id=identity(node.get("id"));if(!ids.add(id))throw invalid("duplicate node");copy.put("id",id);
            for(String key:List.of("stageNo","sort"))copy.put(key,integer(node.get(key)));
            for(String key:List.of("stageOrder","approveRatio"))copy.put(key,node.get(key)==null?null:integer(node.get(key)));
            for(String key:List.of("candidateSourceId","subjectId"))copy.put(key,node.get(key)==null?null:identity(node.get(key)));
            for(String key:List.of("stageName","candidateSourceType","approveMethod"))copy.put(key,text(node.get(key),false));
            for(String key:List.of("stageCode","candidateSourceIds","stageType","subjectLabel","marker","subjectType","subjectName","subjectDepartmentPath","ruleRemark"))copy.put(key,text(node.get(key),true));
            for(String key:List.of("requireAllApprovals","required")){if(!(node.get(key) instanceof Boolean))throw invalid(key);copy.put(key,node.get(key));}
            nodes.add(copy);
        }
        nodes.sort((a,b)->{for(String key:List.of("stageOrder","sort","stageNo")){Object x=a.get(key),y=b.get(key);if(Objects.equals(x,y))continue;if(x==null)return 1;if(y==null)return -1;return Integer.compare((Integer)x,(Integer)y);}return new BigInteger((String)a.get("id")).compareTo(new BigInteger((String)b.get("id")));});
        result.put("nodes",nodes);return json(result);
    }
    private static void exact(Map<String,Object> raw,Set<String> fields){if(raw==null || !raw.keySet().equals(fields))throw invalid("raw whitelist");}
    private static String identity(Object value){String text;if(value instanceof String s)text=s;else if(value instanceof Long || value instanceof Integer)text=value.toString();else throw invalid("Long identity");if(!text.matches("[1-9][0-9]*") || new BigInteger(text).compareTo(BigInteger.valueOf(Long.MAX_VALUE))>0)throw invalid("Long identity");return text;}
    private static int integer(Object value){if(value instanceof Integer i)return i;if(value instanceof Long l && l>=Integer.MIN_VALUE && l<=Integer.MAX_VALUE)return l.intValue();throw invalid("Integer");}
    private static String text(Object value,boolean nullable){if(value==null && nullable)return null;if(!(value instanceof String s))throw invalid("text");for(int i=0;i<s.length();i++){char c=s.charAt(i);if(Character.isHighSurrogate(c)){if(++i>=s.length() || !Character.isLowSurrogate(s.charAt(i)))throw invalid("Unicode");}else if(Character.isLowSurrogate(c))throw invalid("Unicode");}return s;}
    private static String time(String value){if(!value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}[T ][0-9]{2}:[0-9]{2}:[0-9]{2}(\\.[0-9]{1,9})?"))throw invalid("LocalDateTime");try{var time=LocalDateTime.parse(value.replace(' ','T'));if(time.getYear()<1000)throw invalid("year");return time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))+"."+String.format(java.util.Locale.ROOT,"%09d",time.getNano());}catch(java.time.DateTimeException error){throw invalid("calendar");}}
    private static String json(Object value){if(value==null)return "null";if(value instanceof String text)return quote(text);if(value instanceof Integer || value instanceof Boolean)return value.toString();if(value instanceof List<?> list)return list.stream().map(DccApprovalMatrixCanonicalizer::json).collect(java.util.stream.Collectors.joining(",","[","]"));if(value instanceof Map<?,?> map)return map.entrySet().stream().map(e->quote((String)e.getKey())+":"+json(e.getValue())).collect(java.util.stream.Collectors.joining(",","{","}"));throw invalid("canonical value");}
    private static String quote(String text){var out=new StringBuilder("\"");for(int i=0;i<text.length();i++){char c=text.charAt(i);switch(c){case '"'->out.append("\\\"");case '\\'->out.append("\\\\");case '\b'->out.append("\\b");case '\f'->out.append("\\f");case '\n'->out.append("\\n");case '\r'->out.append("\\r");case '\t'->out.append("\\t");default->{if(c<0x20)out.append(String.format(java.util.Locale.ROOT,"\\u%04x",(int)c));else out.append(c);}}}return out.append('"').toString();}
    private static IllegalArgumentException invalid(String field){return new IllegalArgumentException("公共矩阵原配置合同无效："+field);}
    public static String digest(Map<String,Object> raw){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonicalize(raw).getBytes(StandardCharsets.UTF_8)));}catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}}
}
