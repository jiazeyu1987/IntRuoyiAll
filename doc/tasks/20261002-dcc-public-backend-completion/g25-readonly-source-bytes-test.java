import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.core.exception.SdkClientException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

final class G25ReadOnlySourceBytesTest {
    static int tests=0;
    static final byte[] BODY="offline real streaming fixture only".getBytes(StandardCharsets.UTF_8);
    static void check(boolean good,String reason) {if(!good)throw new AssertionError(reason);}
    static String hash(byte[] value)throws Exception {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));}
    static ObjectNode input()throws Exception {
        ObjectNode root=G25ReadOnlySourceBytes.JSON.createObjectNode();ObjectNode config=root.putObject("config");
        config.put("configId","28");config.put("storage",20);config.put("endpoint","http://127.0.0.1:9000");config.put("bucket","offline-bucket");config.put("region","us-east-1");config.put("accessKey","OFFLINE_FAKE_KEY");config.put("accessSecret","OFFLINE_FAKE_SECRET");config.put("pathStyle",true);
        ArrayNode files=root.putArray("files");for(int i=1;i<=39;i++){ObjectNode row=files.addObject();row.put("id",i==39?"9007199254740993":String.valueOf(i));row.put("configId","28");row.put("key","offline-private/key-"+i);row.put("rootExpectedSha256",hash(BODY));row.put("rootExpectedSize",String.valueOf(BODY.length));}return root;
    }
    record Run(int code,String output,int factories,int gets){}
    static Run run(byte[] input,G25ReadOnlySourceBytes.ObjectSource source) {
        AtomicInteger factories=new AtomicInteger(),gets=new AtomicInteger();ByteArrayOutputStream output=new ByteArrayOutputStream();
        int code=G25ReadOnlySourceBytes.run(new ByteArrayInputStream(input),output,c->{factories.incrementAndGet();return new G25ReadOnlySourceBytes.ObjectSource(){public G25ReadOnlySourceBytes.Opened get(software.amazon.awssdk.services.s3.model.GetObjectRequest req)throws Exception{gets.incrementAndGet();check(req.bucket().equals("offline-bucket") && req.key().startsWith("offline-private/key-"),"precise official GetObject input");return source.get(req);}public void close()throws Exception{source.close();}};});
        return new Run(code,output.toString(StandardCharsets.UTF_8),factories.get(),gets.get());
    }
    static Run run(ObjectNode input,G25ReadOnlySourceBytes.ObjectSource source)throws Exception {return run(G25ReadOnlySourceBytes.JSON.writeValueAsBytes(input),source);}
    static G25ReadOnlySourceBytes.ObjectSource bytes(){return req->new G25ReadOnlySourceBytes.Opened(new ByteArrayInputStream(BODY),200);}
    static void sanitized(String output)throws Exception {
        check(!output.contains("OFFLINE_FAKE") && !output.contains("offline-private") && !output.contains("offline-bucket") && !output.contains("http://") && !output.contains("SECRET_IN_EXCEPTION"),"sensitive strings must never appear");
        for(String line:output.lines().toList()) {JsonNode row=G25ReadOnlySourceBytes.JSON.readTree(line);Set<String> fields=new HashSet<>();row.fieldNames().forEachRemaining(fields::add);check(fields.equals(Set.of("id","status","actualSha256","actualLength","httpStatus","errorCode")),"only exact output fields");}
    }
    static void invalid(ObjectNode changed)throws Exception {Run run=run(changed,bytes());check(run.code==2 && run.factories==0 && run.gets==0,"invalid input must reject before any client/GET");sanitized(run.output);tests++;}
    public static void main(String[] args)throws Exception {
        G25ReadOnlySourceBytes.muteSdkLogs();
        Run good=run(input(),bytes());check(good.code==0 && good.gets==39 && good.output.lines().count()==39,"39 exact MATCH required");sanitized(good.output);check(good.output.contains("9007199254740993"),"Long string retained");tests++;
        ObjectNode mismatch=input();((ObjectNode)mismatch.get("files").get(0)).put("rootExpectedSha256","0".repeat(64));Run bad=run(mismatch,bytes());check(bad.code==1 && bad.output.contains("MISMATCH") && bad.gets==39,"SHA mismatch truthful and no retry");sanitized(bad.output);tests++;
        mismatch=input();((ObjectNode)mismatch.get("files").get(0)).put("rootExpectedSize","999");bad=run(mismatch,bytes());check(bad.code==1 && bad.output.contains("MISMATCH"),"length mismatch truthful");tests++;
        for(String endpoint:List.of("http://example.com:9000","http://127.0.0.1.example.com:9000","http://user:password@localhost:9000","http://localhost:9000/private","http://localhost:9000/?credential=x","http://localhost:9000/#key","file:///tmp","http://[::1]:9000")){ObjectNode root=input();((ObjectNode)root.get("config")).put("endpoint",endpoint);invalid(root);}
        ObjectNode root=input();((ObjectNode)root.get("config")).put("region","");invalid(root);
        root=input();((ObjectNode)root.get("config")).put("pathStyle",false);invalid(root);
        root=input();((ObjectNode)root.get("config")).put("configId","29");invalid(root);
        root=input();((ObjectNode)root.get("config")).put("storage",19);invalid(root);
        root=input();((ObjectNode)root.get("config")).put("operation","PutObject");invalid(root);
        root=input();((ArrayNode)root.get("files")).remove(0);invalid(root);
        root=input();((ObjectNode)root.get("files").get(1)).put("id","1");invalid(root);
        for(String id:List.of("9007199254740993.0","9223372036854775808","0","01","-1")){root=input();((ObjectNode)root.get("files").get(0)).put("id",id);invalid(root);}
        root=input();((ObjectNode)root.get("files").get(0)).put("id",9007199254740993L);invalid(root);
        root=input();((ObjectNode)root.get("files").get(0)).put("configId","20");invalid(root);
        for(String key:List.of("/root/private","../body","good/../body","https://host/body","good\\body","good\nsecret")){root=input();((ObjectNode)root.get("files").get(0)).put("key",key);invalid(root);}
        root=input();((ObjectNode)root.get("files").get(0)).put("rootExpectedSha256","guess");invalid(root);
        root=input();((ObjectNode)root.get("files").get(0)).put("rootExpectedSize",123);invalid(root);
        for(int http:List.of(404,403,500)){
            G25ReadOnlySourceBytes.ObjectSource denied=req->{throw S3Exception.builder().statusCode(http).message("SECRET_IN_EXCEPTION key/credential").awsErrorDetails(AwsErrorDetails.builder().errorCode(http==404?"NoSuchKey":http==403?"AccessDenied":"InternalError").build()).build();};
            Run result=run(input(),denied);check(result.code==1 && result.gets==39 && result.output.contains("HTTP_ERROR") && result.output.contains(String.valueOf(http)),"actual HTTP error is explicit/no retry");sanitized(result.output);tests++;
        }
        Run unsafeError=run(input(),req->{throw S3Exception.builder().statusCode(403).message("SECRET_IN_EXCEPTION").awsErrorDetails(AwsErrorDetails.builder().errorCode("SECRET_IN_EXCEPTION-CREDENTIAL").build()).build();});check(unsafeError.code==1 && unsafeError.output.contains("S3_ERROR"),"arbitrary errorCode sanitized");sanitized(unsafeError.output);tests++;
        Run network=run(input(),req->{throw SdkClientException.create("SECRET_IN_EXCEPTION endpoint key credential");});check(network.code==1 && network.output.contains("SDK_CLIENT_ERROR"),"SDK failure not swallowed");sanitized(network.output);tests++;
        Run partial=run(input(),req->new G25ReadOnlySourceBytes.Opened(new InputStream(){int count;public int read()throws IOException{if(count++>=4)throw new IOException("SECRET_IN_EXCEPTION");return 1;}},200));check(partial.code==1 && partial.output.contains("IO_ERROR") && !partial.output.contains("MATCH"),"partial read no success digest");sanitized(partial.output);tests++;
        Run zero=run(input(),req->new G25ReadOnlySourceBytes.Opened(new ByteArrayInputStream(new byte[0]),200));check(zero.code==1 && zero.output.contains("actualLength\":0"),"empty bytes compared rather than fabricated success");tests++;
        AtomicInteger closed=new AtomicInteger();Run close=run(input(),new G25ReadOnlySourceBytes.ObjectSource(){public G25ReadOnlySourceBytes.Opened get(software.amazon.awssdk.services.s3.model.GetObjectRequest req){return new G25ReadOnlySourceBytes.Opened(new ByteArrayInputStream(BODY){public void close(){closed.incrementAndGet();}},200);}public void close(){closed.incrementAndGet();}});check(close.code==0 && closed.get()==40,"39 streams plus client close");tests++;
        Run invalidJson=run("{\"config\":SECRET_IN_EXCEPTION}".getBytes(StandardCharsets.UTF_8),bytes());check(invalidJson.code==2 && invalidJson.factories==0,"parser error sanitized/no GET");sanitized(invalidJson.output);tests++;
        Run duplicateJson=run("{\"config\":{},\"config\":{},\"files\":[]}".getBytes(),bytes());check(duplicateJson.code==2 && duplicateJson.factories==0,"duplicate fields reject");tests++;
        Run trailing=run((G25ReadOnlySourceBytes.JSON.writeValueAsString(input())+" {} ").getBytes(StandardCharsets.UTF_8),bytes());check(trailing.code==2 && trailing.factories==0,"trailing JSON rejected");tests++;
        root=input();((ObjectNode)root.get("files").get(0)).put("key","汉".repeat(400));invalid(root);
        var config=G25ReadOnlySourceBytes.parse(new ByteArrayInputStream(G25ReadOnlySourceBytes.JSON.writeValueAsBytes(input()))).config();
        var allowed=software.amazon.awssdk.http.SdkHttpRequest.builder().protocol("http").host("127.0.0.1").port(9000).method(software.amazon.awssdk.http.SdkHttpMethod.GET).encodedPath("/offline-bucket/offline-private/key-1").build();
        G25ReadOnlySourceBytes.transmissionGuard(config,allowed);tests++;
        for(var changed:List.of(allowed.toBuilder().host("example.com").build(),allowed.toBuilder().port(80).build(),allowed.toBuilder().method(software.amazon.awssdk.http.SdkHttpMethod.PUT).build(),allowed.toBuilder().method(software.amazon.awssdk.http.SdkHttpMethod.DELETE).build())){
            boolean refused=false;try{G25ReadOnlySourceBytes.transmissionGuard(config,changed);}catch(SdkClientException expected){refused=true;}check(refused,"transmission host/port/write method must reject");tests++;
        }
        System.out.println("PASS "+tests+" offline cases; actual GETs=0; no database/network/body persistence");
    }
}
