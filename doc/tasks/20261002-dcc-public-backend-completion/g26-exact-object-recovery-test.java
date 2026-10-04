import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.*;
import software.amazon.awssdk.services.s3.model.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

final class G26ExactObjectRecoveryTest {
    static int tests;
    static final List<String> IDS=List.of("9198354931064","9198354931068","9198354931079","9198354931095");
    static final byte[] PDF="only offline PDF fixture".getBytes(StandardCharsets.UTF_8),DOCX="only offline DOCX fixture".getBytes(StandardCharsets.UTF_8);
    static Path pdf,docx;
    static Map<String,G26ExactObjectRecovery.Scope> scopes;
    static void check(boolean ok,String reason){if(!ok)throw new AssertionError(reason);}
    static String hash(byte[] bytes)throws Exception{return G26ExactObjectRecovery.hash(bytes);}
    static String key(int i){return "offline-private/"+(i==3?2:i)+".bin";}
    static ObjectNode input()throws Exception {
        ObjectNode root=G26ExactObjectRecovery.JSON.createObjectNode();
        root.put("authorizationReference","ROOT-REAL-USER-APPROVAL-REFERENCE");
        ObjectNode c=root.putObject("config");c.put("configId","28");c.put("storage",20);c.put("endpoint","http://127.0.0.1:9000");c.put("bucket","offline-bucket");c.put("region","us-east-1");c.put("accessKey","OFFLINE_FAKE_KEY");c.put("accessSecret","OFFLINE_FAKE_SECRET");c.put("pathStyle",true);
        ArrayNode rows=root.putArray("files");for(int i=0;i<4;i++){byte[] body=i<2?PDF:DOCX;ObjectNode r=rows.addObject();r.put("id",IDS.get(i));r.put("configId","28");r.put("key",key(i));r.put("rootExpectedSha256",hash(body));r.put("rootExpectedSize",String.valueOf(body.length));r.put("mime",i<2?"application/pdf":"application/vnd.openxmlformats-officedocument.wordprocessingml.document");r.put("candidatePath",(i<2?pdf:docx).toString());}return root;
    }
    static S3Exception error(int status,String code){return (S3Exception)S3Exception.builder().statusCode(status).message("SECRET_IN_EXCEPTION private/key password").awsErrorDetails(AwsErrorDetails.builder().errorCode(code).build()).build();}
    static class Store implements G26ExactObjectRecovery.Store {
        final Map<String,byte[]> objects=new HashMap<>();final List<String> operations=new ArrayList<>();
        int gets,puts,failGetAt=-1,failPutAt=-1;S3Exception getError,putError;boolean corruptFinal,partialFinal;int closed;
        public G26ExactObjectRecovery.Opened get(GetObjectRequest req)throws Exception{
            gets++;operations.add("GET:"+req.key());if(gets==failGetAt)throw getError;
            byte[] bytes=objects.get(req.key());if(bytes==null)throw error(404,"NoSuchKey");
            if(corruptFinal)bytes=PDF;
            InputStream stream=partialFinal?new InputStream(){int n;public int read()throws IOException{if(n++>2)throw new IOException("SECRET_IN_EXCEPTION");return 1;}}:new ByteArrayInputStream(bytes);
            return new G26ExactObjectRecovery.Opened(stream,200);
        }
        public int put(PutObjectRequest req,byte[] bytes)throws Exception {
            puts++;operations.add("PUT:"+req.key());check("*".equals(req.ifNoneMatch()),"conditional create request");check(req.contentLength()==bytes.length,"body length exact");check(req.checksumSHA256().equals(Base64.getEncoder().encodeToString(java.security.MessageDigest.getInstance("SHA-256").digest(bytes))),"explicit SHA checksum exact");check(req.objectLockMode()==null && req.objectLockRetainUntilDate()==null && req.objectLockLegalHoldStatus()==null,"no guessed object retention/hold");
            if(puts==failPutAt)throw putError;if(objects.containsKey(req.key()))throw error(412,"PreconditionFailed");objects.put(req.key(),bytes.clone());return 200;
        }
        public void close(){closed++;}
    }
    record Run(int code,String output,int clients){}
    static Run run(byte[] raw,Store store,boolean authorized)throws Exception {
        AtomicInteger clients=new AtomicInteger();ByteArrayOutputStream out=new ByteArrayOutputStream();int code=G26ExactObjectRecovery.run(new ByteArrayInputStream(raw),out,(c,keys)->{clients.incrementAndGet();check(keys.size()==3,"factory exact three keys");return store;},scopes,hash("offline-bucket".getBytes(StandardCharsets.UTF_8)),authorized);String text=out.toString(StandardCharsets.UTF_8);safe(text);return new Run(code,text,clients.get());
    }
    static Run run(ObjectNode root,Store store)throws Exception{return run(G26ExactObjectRecovery.JSON.writeValueAsBytes(root),store,true);}
    static void safe(String text)throws Exception {
        for(String banned:List.of("OFFLINE_FAKE","SECRET_IN_EXCEPTION","offline-private","offline-bucket","http://",pdf.toString(),docx.toString()))check(!text.contains(banned),"safe output only");
        for(String line:text.lines().toList()){JsonNode row=G26ExactObjectRecovery.JSON.readTree(line);Set<String> keys=new HashSet<>();row.fieldNames().forEachRemaining(keys::add);check(keys.equals(Set.of("id","status","phase","actualSha256","actualLength","httpStatus","errorCode","putAttempted","putAccepted")),"exact output whitelist");}
    }
    static ObjectNode row(ObjectNode root,int i){return(ObjectNode)root.get("files").get(i);}
    static void invalid(ObjectNode root)throws Exception{Store s=new Store();Run r=run(root,s);check(r.code==2 && r.clients==0 && s.gets==0 && s.puts==0,"invalid input before any client");tests++;}
    static void scenario(boolean ok,String reason){check(ok,reason);tests++;}
    public static void main(String[] args)throws Exception{
        G26ExactObjectRecovery.muteSdkLogs();Path dir=Files.createTempDirectory("g26-offline-owned-");pdf=dir.resolve("pdf.bin");docx=dir.resolve("docx.bin");Files.write(pdf,PDF);Files.write(docx,DOCX);
        try{
            Map<String,G26ExactObjectRecovery.Scope> map=new LinkedHashMap<>();for(int i=0;i<4;i++){byte[] body=i<2?PDF:DOCX;map.put(IDS.get(i),new G26ExactObjectRecovery.Scope(hash(key(i).getBytes(StandardCharsets.UTF_8)),hash(body),body.length,i<2?"application/pdf":"application/vnd.openxmlformats-officedocument.wordprocessingml.document",Set.of((i<2?pdf:docx).toAbsolutePath().normalize())));}scopes=Map.copyOf(map);
            Store s=new Store();Run r=run(input(),s);scenario(r.code==0 && s.puts==3 && s.gets==7 && s.objects.size()==3 && r.output.lines().count()==4 && s.closed==1,"three writes / four final actual reads / one alias");scenario(s.operations.subList(0,3).stream().allMatch(x->x.startsWith("GET:")),"all unique missing preflight before any PUT");scenario(r.output.lines().allMatch(x->x.contains("RESTORED_VERIFIED")),"all four verified");
            s=new Store();r=run(G26ExactObjectRecovery.JSON.writeValueAsBytes(input()),s,false);scenario(r.code==2 && r.clients==0 && s.puts==0,"technical authorization required");
            ObjectNode n=input();((ArrayNode)n.get("files")).remove(0);invalid(n);n=input();((ArrayNode)n.get("files")).add(row(n,0).deepCopy());invalid(n);n=input();row(n,0).put("id","9198354931000");invalid(n);n=input();row(n,1).put("id",IDS.get(0));invalid(n);
            n=input();row(n,0).put("id",9198354931064L);invalid(n);n=input();row(n,0).put("configId","29");invalid(n);n=input();row(n,0).put("key","another/exact/target");invalid(n);n=input();row(n,0).put("rootExpectedSha256","0".repeat(64));invalid(n);n=input();row(n,0).put("rootExpectedSize","9999");invalid(n);n=input();row(n,0).put("rootExpectedSize",PDF.length);invalid(n);n=input();row(n,0).put("mime","application/octet-stream");invalid(n);n=input();row(n,0).put("candidatePath",docx.toString());invalid(n);n=input();row(n,0).put("extra","secret");invalid(n);
            for(String endpoint:List.of("http://example.com:9000","http://127.0.0.1.example.com:9000","http://u:p@localhost:9000","http://localhost:9000/private","http://localhost:9000/?x=1","http://localhost:9000/#x","http://[::1]:9000","http://127.0.0.1:9001","https://127.0.0.1:9000")){n=input();((ObjectNode)n.get("config")).put("endpoint",endpoint);invalid(n);}
            n=input();((ObjectNode)n.get("config")).put("storage",19);invalid(n);n=input();((ObjectNode)n.get("config")).put("pathStyle",false);invalid(n);n=input();((ObjectNode)n.get("config")).put("region","");invalid(n);n=input();n.put("authorizationReference","");invalid(n);
            for(String raw:List.of("{\"config\":SECRET_IN_EXCEPTION}","{\"config\":{},\"config\":{},\"files\":[]}",G26ExactObjectRecovery.JSON.writeValueAsString(input())+" {}")){s=new Store();r=run(raw.getBytes(StandardCharsets.UTF_8),s,true);scenario(r.code==2 && r.clients==0,"strict malformed duplicate trailing input");}
            Files.write(pdf,"corrupted exact local bytes".getBytes());s=new Store();r=run(input(),s);scenario(r.code==1 && r.clients==0 && r.output.contains("LOCAL_BODY_MISMATCH"),"all local proof before client / no alternate fallback");Files.write(pdf,PDF);
            Files.delete(docx);s=new Store();r=run(input(),s);scenario(r.code==1 && r.clients==0 && r.output.contains("LOCAL_SOURCE_UNAVAILABLE"),"missing source no client");Files.write(docx,DOCX);
            for(int pos=1;pos<=3;pos++){s=new Store();s.objects.put(key(pos-1),pos<3?PDF:DOCX);r=run(input(),s);scenario(r.code==1 && s.puts==0 && r.output.contains("OBJECT_ALREADY_EXISTS"),"existing object at every preflight position is all-zero-write conflict");}
            for(S3Exception e:List.of(error(403,"AccessDenied"),error(404,"NoSuchBucket"),error(404,"S3_ERROR"),error(500,"InternalError"))){s=new Store();s.failGetAt=2;s.getError=e;r=run(input(),s);scenario(r.code==1 && s.puts==0,"only exact 404 NoSuchKey permits creation");}
            s=new Store();s.failGetAt=2;s.getError=(S3Exception)S3Exception.builder().statusCode(404).awsErrorDetails(AwsErrorDetails.builder().errorCode("NoSuchKey").sdkHttpResponse(SdkHttpResponse.builder().statusCode(404).putHeader("x-amz-delete-marker","true").build()).build()).build();r=run(input(),s);scenario(r.code==1 && s.puts==0 && r.output.contains("EXISTING_DELETE_MARKER"),"historical delete marker is not silently resurrected");
            for(S3Exception e:List.of(error(412,"PreconditionFailed"),error(409,"ConditionalRequestConflict"),error(501,"NotImplemented"),error(403,"AccessDenied"),error(400,"UNSAFE_SECRET_CODE"))){s=new Store();s.failPutAt=2;s.putError=e;r=run(input(),s);scenario(r.code==1 && s.puts==2 && s.objects.size()==1 && r.output.contains("PUT_REJECTED") && r.output.contains("NOT_ATTEMPTED"),"PUT failure stops no retry / accurate earlier effect");}
            s=new Store(){public int put(PutObjectRequest q,byte[] b)throws Exception{super.put(q,b);throw error(500,"InternalError");}};r=run(input(),s);scenario(r.code==1 && s.puts==1 && s.objects.size()==1 && r.output.contains("PUT_OUTCOME_UNCERTAIN") && r.output.contains("500"),"actual server 5xx may follow a write / uncertainty must remain explicit");
            n=input();((ObjectNode)n.get("config")).put("bucket","another-bucket");invalid(n);
            s=new Store(){public int put(PutObjectRequest q,byte[] b)throws Exception{super.put(q,b);throw SdkClientException.create("SECRET_IN_EXCEPTION");}};r=run(input(),s);scenario(r.code==1 && s.puts==1 && s.objects.size()==1 && r.output.contains("PUT_OUTCOME_UNCERTAIN"),"uncertain write stops without retry/delete/fake success");
            s=new Store(){public int put(PutObjectRequest q,byte[] b)throws Exception{puts++;operations.add("PUT:"+q.key());return 204;}};r=run(input(),s);scenario(r.code==1 && s.puts==3 && s.objects.isEmpty() && r.output.contains("FINAL_READ_ERROR"),"2xx write acceptance without stored bytes fails independent final GET");
            s=new Store(){public int put(PutObjectRequest q,byte[] b){puts++;return 501;}};r=run(input(),s);scenario(r.code==1 && s.puts==1 && r.output.contains("PUT_NON_SUCCESS"),"unsupported conditional create stops without unconditional retry");
            s=new Store(){public G26ExactObjectRecovery.Opened get(GetObjectRequest q){gets++;throw SdkClientException.create("SECRET_IN_EXCEPTION");}};r=run(input(),s);scenario(r.code==1 && s.puts==0 && r.output.contains("PREFLIGHT_READ_ERROR"),"network preflight failure zero PUT");
            s=new Store(){public void close(){throw SdkClientException.create("SECRET_IN_EXCEPTION");}};r=run(input(),s);scenario(r.code==1 && s.puts==3 && r.output.contains("CLIENT_LIFECYCLE_ERROR"),"client close failure preserves verified rows but no overall success");
            s=new Store();s.corruptFinal=true;r=run(input(),s);scenario(r.code==1 && s.puts==3 && r.output.contains("FINAL_BODY_MISMATCH"),"bad final bytes fail after truthful three created effects");s=new Store();s.partialFinal=true;r=run(input(),s);scenario(r.code==1 && s.puts==3 && r.output.contains("FINAL_READ_ERROR"),"final partial stream fail");
            var parsed=G26ExactObjectRecovery.parse(new ByteArrayInputStream(G26ExactObjectRecovery.JSON.writeValueAsBytes(input())),scopes,hash("offline-bucket".getBytes(StandardCharsets.UTF_8)));var c=parsed.config();Set<String> allowed=new HashSet<>();for(var f:parsed.files())allowed.add(f.key());
            var http=SdkHttpRequest.builder().protocol("http").host("127.0.0.1").port(9000).method(SdkHttpMethod.PUT).encodedPath("/offline-bucket/"+key(0)).putHeader("If-None-Match","*").build();G26ExactObjectRecovery.transmissionGuard(c,allowed,http);tests++;
            for(var bad:List.of(http.toBuilder().host("example.com").build(),http.toBuilder().port(80).build(),http.toBuilder().method(SdkHttpMethod.DELETE).build(),http.toBuilder().encodedPath("/offline-bucket/another/key").build(),http.toBuilder().removeHeader("If-None-Match").build(),http.toBuilder().putHeader("If-None-Match","wrong").build(),http.toBuilder().putRawQueryParameter("uploads","x").build(),http.toBuilder().putHeader("x-amz-object-lock-mode","COMPLIANCE").build())){boolean rejected=false;try{G26ExactObjectRecovery.transmissionGuard(c,allowed,bad);}catch(SdkClientException expected){rejected=true;}scenario(rejected,"actual transmission exact target/conditional/method/no policy headers");}
            AtomicInteger transports=new AtomicInteger();var transport=new SdkHttpClient(){public ExecutableHttpRequest prepareRequest(HttpExecuteRequest request){transports.incrementAndGet();var q=request.httpRequest();check(q.method()==SdkHttpMethod.PUT && q.firstMatchingHeader("If-None-Match").orElse("").equals("*"),"actual SDK serializes conditional header");check(q.encodedPath().equals("/offline-bucket/"+key(0)) && q.host().equals("127.0.0.1") && q.port()==9000,"actual SDK exact endpoint/key");check(q.firstMatchingHeader("x-amz-checksum-sha256").isPresent(),"actual SDK checksum transmitted");for(String h:q.headers().keySet())check(!h.toLowerCase(Locale.ROOT).contains("object-lock") && !h.toLowerCase(Locale.ROOT).contains("copy-source"),"no lock/copy headers");return new ExecutableHttpRequest(){public HttpExecuteResponse call()throws IOException{byte[] actual=request.contentStreamProvider().orElseThrow().newStream().readAllBytes();check(Arrays.equals(actual,PDF),"actual SDK transmitted verified exact bytes");return HttpExecuteResponse.builder().response(SdkHttpResponse.builder().statusCode(200).putHeader("ETag","\"offline-etag\"").build()).responseBody(AbortableInputStream.create(new ByteArrayInputStream(new byte[0]))).build();}public void abort(){}};}public void close(){}};
            try(var aws=G26ExactObjectRecovery.createAwsStore(c,allowed,transport)){int status=aws.put(G26ExactObjectRecovery.putRequest(c,parsed.files().get(0),PDF),PDF);scenario(status==200 && transports.get()==1,"real SDK marshalling with offline HTTP transport only");}
            String special="offline-private/含 空格+%#.pdf";var specialFact=new G26ExactObjectRecovery.FileFact(IDS.get(0),special,hash(PDF),PDF.length,"application/pdf",pdf);var specialTransport=new SdkHttpClient(){public ExecutableHttpRequest prepareRequest(HttpExecuteRequest req){check(req.httpRequest().encodedPath().equals(G26ExactObjectRecovery.encodedPath(c,special)),"actual SDK exact UTF-8 and escaped special key");return new ExecutableHttpRequest(){public HttpExecuteResponse call(){return HttpExecuteResponse.builder().response(SdkHttpResponse.builder().statusCode(200).build()).responseBody(AbortableInputStream.create(new ByteArrayInputStream(new byte[0]))).build();}public void abort(){}};}public void close(){}};
            try(var aws=G26ExactObjectRecovery.createAwsStore(c,Set.of(special),specialTransport)){scenario(aws.put(G26ExactObjectRecovery.putRequest(c,specialFact,PDF),PDF)==200,"real SDK special object key keeps exact requested bytes");}
            scenario(G26ExactObjectRecovery.PRODUCTION_SCOPE.keySet().equals(Set.copyOf(IDS)) && G26ExactObjectRecovery.PRODUCTION_SCOPE.get(IDS.get(2)).keySha256().equals(G26ExactObjectRecovery.PRODUCTION_SCOPE.get(IDS.get(3)).keySha256()),"fixed exact four IDs / shared object identity");
            for(var entry:G26ExactObjectRecovery.PRODUCTION_SCOPE.entrySet()){
                var scope=entry.getValue();for(Path candidate:scope.candidates()){byte[] original=Files.readAllBytes(candidate);scenario(original.length==scope.size() && hash(original).equals(scope.bodySha256()),"readonly exact historical candidate SHA/size matches immutable scope");}
            }
            System.out.println("PASS "+tests+" offline cases; real GET=0 PUT=0 DB=0; SDK transport entirely in memory");
        }finally{Files.deleteIfExists(pdf);Files.deleteIfExists(docx);Files.deleteIfExists(dir);}
    }
}
