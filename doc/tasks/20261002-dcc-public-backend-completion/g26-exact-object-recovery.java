import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.*;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.interceptor.*;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.http.*;
import software.amazon.awssdk.http.apache.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.*;
import software.amazon.awssdk.services.s3.model.*;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;

/** Task-only recovery; exact three missing keys, no overwrite/delete/copy/metadata repair. */
final class G26ExactObjectRecovery {
    static final ObjectMapper JSON=new ObjectMapper(JsonFactory.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build()).enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    static final String PDF_SHA="2ad539f9095e70d70e94571207a51ca6ca1e3f7a6f7e04947c59db78100212c0",DOCX_SHA="cb3f40ac2ca8cebd85c5a895a64588e5b6298517a2061c8227c62e715a73a71f";
    static final String PDF_MIME="application/pdf",DOCX_MIME="application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    static final String PRODUCTION_BUCKET_SHA="eef43e6566706fff3d910f5ea7220e06c51ad4bbcd34aa71e8c863ca7cece381";
    record Scope(String keySha256,String bodySha256,long size,String mime,Set<Path> candidates){}
    static final Set<Path> PDF_CANDIDATES=Set.of(Path.of("C:/IntRuoyiAll-int_main/doc/tasks/20260918-dcc-void-e2e/obsolete-e2e-source.pdf").toAbsolutePath().normalize(),Path.of("C:/IntRuoyiAll-int_main/doc/tasks/20260918-dcc-upload-full-e2e/upload-source.pdf").toAbsolutePath().normalize());
    static final Set<Path> DOCX_CANDIDATES=Set.of(Path.of("C:/IntRuoyi/20261001-dcc-integration/doc/tasks/20260729-test-server-wangsiyu-file-upload-simulation/input/codex-upload-simulation-20260729.docx").toAbsolutePath().normalize(),Path.of("C:/IntRuoyiAll-int_main/doc/tasks/20260729-test-server-wangsiyu-file-upload-simulation/input/codex-upload-simulation-20260729.docx").toAbsolutePath().normalize());
    static final Map<String,Scope> PRODUCTION_SCOPE=Map.of(
        "9198354931064",new Scope("8e0457c92be8e53d420496a5202065e667392fc6e2f06f1fb919d66ae161097e",PDF_SHA,37120,PDF_MIME,PDF_CANDIDATES),
        "9198354931068",new Scope("1484202bd71e6b66bf6d21d0900ddcd997ce4d5cdd01b51dac6debd98d58803a",PDF_SHA,37120,PDF_MIME,PDF_CANDIDATES),
        "9198354931079",new Scope("94de5840221043406c340aff0a96e0bc7244040fd1cea1ad195711a791b21779",DOCX_SHA,36872,DOCX_MIME,DOCX_CANDIDATES),
        "9198354931095",new Scope("94de5840221043406c340aff0a96e0bc7244040fd1cea1ad195711a791b21779",DOCX_SHA,36872,DOCX_MIME,DOCX_CANDIDATES));
    record Config(String configId,int storage,URI endpoint,String bucket,String region,String accessKey,String accessSecret){}
    record FileFact(String id,String key,String hash,long size,String mime,Path candidate){}
    record Input(Config config,List<FileFact> files,String authorizationReference){}
    record Opened(InputStream stream,int httpStatus)implements AutoCloseable{public void close()throws IOException{stream.close();}}
    interface Store extends AutoCloseable{Opened get(GetObjectRequest request)throws Exception;int put(PutObjectRequest request,byte[] verifiedBytes)throws Exception;default void close()throws Exception{}}
    interface Factory{Store create(Config c,Set<String> exactKeys)throws Exception;}
    record Result(String id,String status,String phase,String actualSha256,Long actualLength,Integer httpStatus,String errorCode,boolean putAttempted,boolean putAccepted){}
    static final class InvalidInput extends IllegalArgumentException{}
    static final class LocalProofFailure extends IOException{final String id,code;LocalProofFailure(String id,String code){this.id=id;this.code=code;}}
    static void require(boolean ok){if(!ok)throw new InvalidInput();}
    static void fields(JsonNode n,String...keys){require(n!=null&&n.isObject());Set<String> actual=new HashSet<>();n.fieldNames().forEachRemaining(actual::add);require(actual.equals(Set.of(keys)));}
    static String text(JsonNode n,String key){JsonNode value=n.get(key);require(value!=null&&value.isTextual()&&!value.textValue().isBlank());return value.textValue();}
    static String hash(byte[] bytes)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
    static Input parse(InputStream stdin,Map<String,Scope> scope,String expectedBucketSha)throws Exception{
        byte[] raw=stdin.readNBytes(262145);require(raw.length>0&&raw.length<=262144);JsonNode root=JSON.readTree(raw);fields(root,"authorizationReference","config","files");
        String authorization=text(root,"authorizationReference");require(authorization.length()<=512&&authorization.codePoints().noneMatch(Character::isISOControl));JsonNode c=root.get("config");fields(c,"configId","storage","endpoint","bucket","region","accessKey","accessSecret","pathStyle");
        require(text(c,"configId").equals("28")&&c.get("storage").isInt()&&c.get("storage").intValue()==20);URI endpoint=URI.create(text(c,"endpoint"));
        require(endpoint.getScheme().equals("http")&&Set.of("127.0.0.1","localhost").contains(endpoint.getHost())&&endpoint.getPort()==9000&&endpoint.getRawUserInfo()==null&&endpoint.getRawQuery()==null&&endpoint.getRawFragment()==null&&(endpoint.getRawPath()==null||endpoint.getRawPath().isEmpty()||endpoint.getRawPath().equals("/")));
        String bucket=text(c,"bucket");require(bucket.matches("[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]")&&!bucket.contains("..")&&hash(bucket.getBytes(StandardCharsets.UTF_8)).equals(expectedBucketSha));String region=text(c,"region");require(region.equals("us-east-1"));require(c.get("pathStyle").isBoolean()&&c.get("pathStyle").booleanValue());
        Config config=new Config("28",20,endpoint,bucket,region,text(c,"accessKey"),text(c,"accessSecret"));JsonNode rows=root.get("files");require(rows.isArray()&&rows.size()==4&&scope.size()==4);
        List<FileFact> files=new ArrayList<>();Set<String> ids=new HashSet<>();Map<String,FileFact> unique=new LinkedHashMap<>();
        for(JsonNode row:rows){fields(row,"id","configId","key","rootExpectedSha256","rootExpectedSize","mime","candidatePath");String id=text(row,"id");Scope expected=scope.get(id);require(expected!=null&&ids.add(id)&&text(row,"configId").equals("28"));String key=text(row,"key");require(key.getBytes(StandardCharsets.UTF_8).length<=1024&&!key.startsWith("/")&&!key.contains("\\")&&!key.contains("://")&&key.codePoints().noneMatch(Character::isISOControl)&&Arrays.stream(key.split("/",-1)).noneMatch(x->x.isEmpty()||x.equals(".")||x.equals(".."))&&hash(key.getBytes(StandardCharsets.UTF_8)).equals(expected.keySha256()));
            String hash=text(row,"rootExpectedSha256"),size=text(row,"rootExpectedSize"),mime=text(row,"mime");require(hash.equals(expected.bodySha256())&&size.equals(String.valueOf(expected.size()))&&mime.equals(expected.mime()));Path candidate=Path.of(text(row,"candidatePath")).toAbsolutePath().normalize();require(expected.candidates().contains(candidate));
            FileFact file=new FileFact(id,key,hash,expected.size(),mime,candidate);FileFact previous=unique.putIfAbsent(key,file);require(previous==null||(previous.hash().equals(hash)&&previous.size()==file.size()&&previous.mime().equals(mime)));files.add(file);
        }
        require(ids.equals(scope.keySet())&&unique.size()==3);return new Input(config,List.copyOf(files),authorization);
    }
    static Map<String,byte[]> loadAll(Input input)throws Exception{
        Map<String,byte[]> bodies=new LinkedHashMap<>();
        for(FileFact file:input.files()){
            byte[] bytes;
            try{Path path=file.candidate();require(Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS)&&!Files.isSymbolicLink(path));Path real=path.toRealPath();require(real.equals(path));try(InputStream in=Files.newInputStream(path,LinkOption.NOFOLLOW_LINKS)){bytes=in.readNBytes(Math.toIntExact(file.size()+1));}}
            catch(Exception unreadable){throw new LocalProofFailure(file.id(),"LOCAL_SOURCE_UNAVAILABLE");}
            if(bytes.length!=file.size()||!hash(bytes).equals(file.hash()))throw new LocalProofFailure(file.id(),"LOCAL_BODY_MISMATCH");
            byte[] old=bodies.putIfAbsent(file.key(),bytes);if(old!=null&&!Arrays.equals(old,bytes))throw new LocalProofFailure(file.id(),"ALIAS_BODY_CONFLICT");
        }
        return Collections.unmodifiableMap(bodies);
    }
    static String errorCode(S3Exception failure){String code=failure.awsErrorDetails()==null?null:failure.awsErrorDetails().errorCode();return code!=null&&Set.of("NoSuchKey","NoSuchBucket","AccessDenied","InvalidAccessKeyId","SignatureDoesNotMatch","InvalidObjectState","PreconditionFailed","ConditionalRequestConflict","NotImplemented","InvalidRequest","InvalidArgument","RequestTimeout","SlowDown","InternalError","ServiceUnavailable","BadDigest").contains(code)?code:"S3_ERROR";}
    static GetObjectRequest getRequest(Config c,FileFact f){return GetObjectRequest.builder().bucket(c.bucket()).key(f.key()).build();}
    static PutObjectRequest putRequest(Config c,FileFact f,byte[] bytes)throws Exception{return PutObjectRequest.builder().bucket(c.bucket()).key(f.key()).contentLength((long)bytes.length).contentType(f.mime()).checksumSHA256(Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(bytes))).ifNoneMatch("*").build();}
    static Result result(FileFact f,String status,String phase,String sha,Long size,Integer http,String error,boolean attempted,boolean accepted){return new Result(f.id(),status,phase,sha,size,http,error,attempted,accepted);}
    static void replaceKey(List<Result> results,Input input,String key,String status,String phase,Integer http,String error,boolean attempted,boolean accepted){for(int i=0;i<input.files().size();i++){FileFact f=input.files().get(i);if(f.key().equals(key))results.set(i,result(f,status,phase,null,null,http,error,attempted,accepted));}}
    static boolean recover(Store store,Input input,Map<String,byte[]> bodies,List<Result> results)throws Exception{
        Map<String,FileFact> unique=new LinkedHashMap<>();for(FileFact f:input.files())unique.putIfAbsent(f.key(),f);
        for(FileFact f:unique.values()){
            try(Opened existing=store.get(getRequest(input.config(),f))){replaceKey(results,input,f.key(),"PREFLIGHT_BLOCKED","PREFLIGHT",existing.httpStatus(),"OBJECT_ALREADY_EXISTS",false,false);return false;}
            catch(S3Exception failure){boolean deleteMarker=failure.awsErrorDetails()!=null&&failure.awsErrorDetails().sdkHttpResponse()!=null&&failure.awsErrorDetails().sdkHttpResponse().firstMatchingHeader("x-amz-delete-marker").orElse("false").equalsIgnoreCase("true");if(failure.statusCode()==404&&errorCode(failure).equals("NoSuchKey")&&!deleteMarker)continue;replaceKey(results,input,f.key(),"PREFLIGHT_BLOCKED","PREFLIGHT",failure.statusCode(),deleteMarker?"EXISTING_DELETE_MARKER":errorCode(failure),false,false);return false;}
            catch(Exception failed){replaceKey(results,input,f.key(),"PREFLIGHT_BLOCKED","PREFLIGHT",null,"PREFLIGHT_READ_ERROR",false,false);return false;}
        }
        for(FileFact f:unique.values()){
            replaceKey(results,input,f.key(),"PUT_OUTCOME_UNCERTAIN","PUT",null,"PUT_OUTCOME_UNCERTAIN",true,false);
            try{int http=store.put(putRequest(input.config(),f,bodies.get(f.key())),bodies.get(f.key()));if(http<200||http>=300){replaceKey(results,input,f.key(),"PUT_REJECTED","PUT",http,"PUT_NON_SUCCESS",true,false);return false;}replaceKey(results,input,f.key(),"CREATED_NOT_VERIFIED","PUT",http,null,true,true);}
            catch(S3Exception failure){boolean definite=failure.statusCode()>=400&&failure.statusCode()<500&&failure.statusCode()!=408||failure.statusCode()==501&&errorCode(failure).equals("NotImplemented");replaceKey(results,input,f.key(),definite?"PUT_REJECTED":"PUT_OUTCOME_UNCERTAIN","PUT",failure.statusCode(),errorCode(failure),true,false);return false;}
            catch(Exception uncertain){return false;}
        }
        boolean good=true;
        for(int i=0;i<input.files().size();i++){FileFact f=input.files().get(i);long length=0;
            try(Opened actual=store.get(getRequest(input.config(),f))){MessageDigest digest=MessageDigest.getInstance("SHA-256");byte[] buf=new byte[64*1024];int n;while((n=actual.stream().read(buf))!=-1){if(n==0)continue;length=Math.addExact(length,n);digest.update(buf,0,n);if(length>f.size())break;}String sha=HexFormat.of().formatHex(digest.digest());boolean matched=actual.httpStatus()==200&&length==f.size()&&sha.equals(f.hash());good&=matched;results.set(i,result(f,matched?"RESTORED_VERIFIED":"FINAL_BODY_MISMATCH","FINAL_GET",sha,length,actual.httpStatus(),matched?null:"FINAL_BODY_MISMATCH",true,true));}
            catch(S3Exception failure){good=false;results.set(i,result(f,"FINAL_READ_ERROR","FINAL_GET",null,null,failure.statusCode(),errorCode(failure),true,true));}
            catch(Exception failure){good=false;results.set(i,result(f,"FINAL_READ_ERROR","FINAL_GET",null,length,null,"FINAL_READ_ERROR",true,true));}
        }
        return good;
    }
    static String encodedPath(Config c,String key){StringBuilder out=new StringBuilder("/");String full=c.bucket()+"/"+key;for(byte value:full.getBytes(StandardCharsets.UTF_8)){int b=value&255;if(b>='a'&&b<='z'||b>='A'&&b<='Z'||b>='0'&&b<='9'||b=='-'||b=='_'||b=='.'||b=='~'||b=='/')out.append((char)b);else out.append('%').append("0123456789ABCDEF".charAt(b>>>4)).append("0123456789ABCDEF".charAt(b&15));}return out.toString();}
    static void transmissionGuard(Config c,Set<String> keys,SdkHttpRequest request){
        boolean method=request.method()==SdkHttpMethod.GET||request.method()==SdkHttpMethod.PUT;boolean exact=keys.stream().anyMatch(key->encodedPath(c,key).equals(request.encodedPath()));boolean conditional=request.method()!=SdkHttpMethod.PUT||request.matchingHeaders("If-None-Match").equals(List.of("*"));
        boolean forbidden=request.headers().keySet().stream().map(x->x.toLowerCase(Locale.ROOT)).anyMatch(x->x.startsWith("x-amz-object-lock-")||x.startsWith("x-amz-copy-source")||x.startsWith("x-amz-server-side-encryption"));
        if(!method||!exact||!conditional||forbidden||!request.rawQueryParameters().isEmpty()||!request.protocol().equals(c.endpoint().getScheme())||!request.host().equals(c.endpoint().getHost())||request.port()!=9000)throw SdkClientException.create("G26_EXACT_LOCAL_CONDITIONAL_CREATE_GUARD");
    }
    static Store createAwsStore(Config c,Set<String> keys,SdkHttpClient offlineTransport){
        var builder=S3Client.builder().endpointOverride(c.endpoint()).region(Region.of(c.region())).credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(c.accessKey(),c.accessSecret()))).serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).chunkedEncodingEnabled(false).build()).overrideConfiguration(ClientOverrideConfiguration.builder().addExecutionInterceptor(new ExecutionInterceptor(){public void beforeTransmission(Context.BeforeTransmission context,ExecutionAttributes attributes){transmissionGuard(c,keys,context.httpRequest());}}).retryPolicy(RetryPolicy.none()).apiCallTimeout(Duration.ofMinutes(2)).apiCallAttemptTimeout(Duration.ofMinutes(2)).build());
        if(offlineTransport!=null)builder.httpClient(offlineTransport);else builder.httpClientBuilder(ApacheHttpClient.builder().proxyConfiguration(ProxyConfiguration.builder().useSystemPropertyValues(false).useEnvironmentVariableValues(false).build()).connectionTimeout(Duration.ofSeconds(10)).socketTimeout(Duration.ofSeconds(30)).maxConnections(1));
        S3Client client=builder.build();return new Store(){public Opened get(GetObjectRequest request){ResponseInputStream<GetObjectResponse> stream=client.getObject(request);return new Opened(stream,stream.response().sdkHttpResponse().statusCode());}public int put(PutObjectRequest request,byte[] bytes){return client.putObject(request,RequestBody.fromBytes(bytes)).sdkHttpResponse().statusCode();}public void close(){client.close();}};
    }
    static void emit(PrintWriter out,Result r)throws IOException{out.println(JSON.writeValueAsString(r));}
    static int run(InputStream stdin,OutputStream stdout,Factory factory,Map<String,Scope> scope,String expectedBucketSha,boolean authorized){PrintWriter out=new PrintWriter(new OutputStreamWriter(stdout,StandardCharsets.UTF_8),true);Input input;
        try{require(authorized);input=parse(stdin,scope,expectedBucketSha);}catch(Exception invalid){try{emit(out,new Result(null,"INPUT_REJECTED","INPUT",null,null,null,"INVALID_INPUT_OR_AUTHORIZATION",false,false));}catch(IOException impossible){return 2;}return 2;}
        Map<String,byte[]> bodies;
        try{bodies=loadAll(input);}catch(LocalProofFailure invalid){try{emit(out,new Result(invalid.id,"LOCAL_PROOF_FAILED","LOCAL_SOURCE",null,null,null,invalid.code,false,false));}catch(IOException impossible){return 1;}return 1;}catch(Exception invalid){try{emit(out,new Result(null,"LOCAL_PROOF_FAILED","LOCAL_SOURCE",null,null,null,"LOCAL_SOURCE_PROOF_ERROR",false,false));}catch(IOException impossible){return 1;}return 1;}
        List<Result> results=new ArrayList<>();for(FileFact f:input.files())results.add(result(f,"NOT_ATTEMPTED","NONE",null,null,null,null,false,false));boolean good=false,lifecycleFailure=false;
        Set<String> exactKeys=new HashSet<>();for(FileFact f:input.files())exactKeys.add(f.key());
        try(Store store=factory.create(input.config(),Set.copyOf(exactKeys))){good=recover(store,input,bodies,results);}catch(Exception lifecycle){lifecycleFailure=true;good=false;}
        try{for(Result r:results)emit(out,r);if(lifecycleFailure)emit(out,new Result(null,"CLIENT_ERROR","LIFECYCLE",null,null,null,"CLIENT_LIFECYCLE_ERROR",false,false));}catch(IOException output){return 1;}finally{for(byte[] bytes:bodies.values())Arrays.fill(bytes,(byte)0);}
        return good&&!out.checkError()?0:1;
    }
    static void muteSdkLogs(){if(LoggerFactory.getILoggerFactory() instanceof LoggerContext context){context.reset();context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).setLevel(Level.OFF);}}
    public static void main(String[] args){muteSdkLogs();boolean authorized=args.length==1&&args[0].equals("--authorize-exact-local-object-recovery");System.exit(run(System.in,System.out,(c,keys)->createAwsStore(c,keys,null),PRODUCTION_SCOPE,PRODUCTION_BUCKET_SHA,authorized));}
}
