import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.apache.ProxyConfiguration;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.*;
import java.math.BigInteger;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;

/** Task-only reader. Real config only stdin; only S3 GetObject, no persistent body or repair. */
final class G25ReadOnlySourceBytes {
    static final int REQUIRED_FILES=39;
    static final int MAX_INPUT_BYTES=2*1024*1024;
    static final ObjectMapper JSON=new ObjectMapper(JsonFactory.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build()).enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    record Config(String configId, int storage, URI endpoint, String bucket, String region, String accessKey, String accessSecret, boolean pathStyle) {}
    record FileFact(String id, String configId, String key, String rootExpectedSha256, long rootExpectedSize) {}
    record Input(Config config, List<FileFact> files) {}
    record Opened(InputStream stream, Integer httpStatus) implements AutoCloseable {
        public void close() throws IOException { stream.close(); }
    }
    interface ObjectSource extends AutoCloseable {
        Opened get(GetObjectRequest request) throws Exception;
        default void close() throws Exception {}
    }
    interface SourceFactory { ObjectSource create(Config config) throws Exception; }
    record Result(String id, String status, String actualSha256, Long actualLength, Integer httpStatus, String errorCode) {}
    static final class InvalidInput extends IllegalArgumentException {}

    static void require(boolean condition) { if(!condition)throw new InvalidInput(); }
    static void fields(JsonNode node, String... names) {
        require(node!=null && node.isObject());Set<String> actual=new HashSet<>();node.fieldNames().forEachRemaining(actual::add);
        require(actual.equals(Set.of(names)));
    }
    static String text(JsonNode node,String name) {
        JsonNode value=node.get(name);require(value!=null && value.isTextual() && !value.textValue().isBlank());return value.textValue();
    }
    static String positiveId(JsonNode node,String name) {
        String value=text(node,name);require(value.matches("[1-9][0-9]{0,18}") && new BigInteger(value).compareTo(BigInteger.valueOf(Long.MAX_VALUE))<=0);return value;
    }
    static long size(JsonNode node) {
        String value=text(node,"rootExpectedSize");require(value.matches("0|[1-9][0-9]{0,18}"));
        BigInteger number=new BigInteger(value);require(number.compareTo(BigInteger.valueOf(Long.MAX_VALUE))<=0);return number.longValueExact();
    }
    static Input parse(InputStream stdin) throws IOException {
        byte[] bytes=stdin.readNBytes(MAX_INPUT_BYTES+1);require(bytes.length>0 && bytes.length<=MAX_INPUT_BYTES);
        JsonNode root=JSON.readTree(bytes);fields(root,"config","files");JsonNode config=root.get("config");
        fields(config,"configId","storage","endpoint","bucket","region","accessKey","accessSecret","pathStyle");
        String configId=positiveId(config,"configId");require(configId.equals("28"));require(config.get("storage").isInt() && config.get("storage").intValue()==20);
        URI endpoint;
        try {endpoint=URI.create(text(config,"endpoint"));}catch(IllegalArgumentException invalid){throw new InvalidInput();}
        require(Set.of("http","https").contains(endpoint.getScheme()) && Set.of("127.0.0.1","localhost").contains(endpoint.getHost())
                && endpoint.getRawUserInfo()==null && endpoint.getRawQuery()==null && endpoint.getRawFragment()==null
                && (endpoint.getRawPath()==null || endpoint.getRawPath().isEmpty() || endpoint.getRawPath().equals("/"))
                && endpoint.getPort()>=-1 && endpoint.getPort()!=0 && endpoint.getPort()<=65535);
        String bucket=text(config,"bucket");require(bucket.matches("[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]") && !bucket.contains(".."));
        String region=text(config,"region");require(region.matches("[a-z0-9][a-z0-9-]{0,62}"));
        require(config.get("pathStyle").isBoolean() && config.get("pathStyle").booleanValue());
        Config settings=new Config(configId,20,endpoint,bucket,region,text(config,"accessKey"),text(config,"accessSecret"),true);
        JsonNode files=root.get("files");require(files.isArray() && files.size()==REQUIRED_FILES);List<FileFact> facts=new ArrayList<>();Set<String> ids=new HashSet<>();
        for(JsonNode file:files) {
            fields(file,"id","configId","key","rootExpectedSha256","rootExpectedSize");String id=positiveId(file,"id");require(ids.add(id));
            String fileConfig=positiveId(file,"configId");require(fileConfig.equals("28"));String key=text(file,"key");
            require(key.getBytes(StandardCharsets.UTF_8).length<=1024 && key.codePoints().noneMatch(c->Character.isISOControl(c)) && !key.startsWith("/")
                    && !key.contains("\\") && !key.contains("://") && Arrays.stream(key.split("/",-1)).noneMatch(x->x.equals("..") || x.equals(".") || x.isEmpty()));
            String hash=text(file,"rootExpectedSha256");require(hash.matches("[0-9a-f]{64}"));facts.add(new FileFact(id,fileConfig,key,hash,size(file)));
        }
        return new Input(settings,List.copyOf(facts));
    }
    static String safeErrorCode(S3Exception error) {
        AwsErrorDetails details=error.awsErrorDetails();String code=details==null?null:details.errorCode();
        // Never reflect arbitrary server text; exact known machine codes only. HTTP status remains actual.
        return code!=null && Set.of("NoSuchKey","NoSuchBucket","AccessDenied","InvalidAccessKeyId","SignatureDoesNotMatch","AuthorizationHeaderMalformed","InvalidObjectState","RequestTimeout","SlowDown","InternalError","ServiceUnavailable").contains(code)?code:"S3_ERROR";
    }
    static Result verify(ObjectSource source, Config config, FileFact file) {
        long length=0;boolean opened=false;
        try {
            MessageDigest hash=MessageDigest.getInstance("SHA-256");
            GetObjectRequest request=GetObjectRequest.builder().bucket(config.bucket()).key(file.key()).build();
            try(Opened object=source.get(request)) {
                require(object!=null && object.stream()!=null);opened=true;byte[] buffer=new byte[64*1024];int count;
                while((count=object.stream().read(buffer))!=-1) {
                    if(count==0)continue;length=Math.addExact(length,count);hash.update(buffer,0,count);
                }
                String actual=HexFormat.of().formatHex(hash.digest());String status=actual.equals(file.rootExpectedSha256()) && length==file.rootExpectedSize()?"MATCH":"MISMATCH";
                return new Result(file.id(),status,actual,length,object.httpStatus(),null);
            }
        } catch(S3Exception denied) {
            return new Result(file.id(),"HTTP_ERROR",null,null,denied.statusCode(),safeErrorCode(denied));
        } catch(SdkClientException network) {
            return new Result(file.id(),"READ_ERROR",null,opened?length:null,null,"SDK_CLIENT_ERROR");
        } catch(IOException stream) {
            return new Result(file.id(),"READ_ERROR",null,opened?length:null,null,"IO_ERROR");
        } catch(Exception failure) {
            return new Result(file.id(),"READ_ERROR",null,opened?length:null,null,"READER_ERROR");
        }
    }
    static void transmissionGuard(Config config,SdkHttpRequest request) {
        int configuredPort=config.endpoint().getPort()==-1?(config.endpoint().getScheme().equals("https")?443:80):config.endpoint().getPort();
        if(request.method()!=SdkHttpMethod.GET || !request.protocol().equals(config.endpoint().getScheme())
                || !request.host().equals(config.endpoint().getHost()) || request.port()!=configuredPort)
            throw SdkClientException.create("LOCAL_GET_ONLY_GUARD");
    }
    static ObjectSource createAwsSource(Config config) {
        S3Client client=S3Client.builder().endpointOverride(config.endpoint()).region(Region.of(config.region()))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(config.accessKey(),config.accessSecret())))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .overrideConfiguration(ClientOverrideConfiguration.builder().addExecutionInterceptor(new ExecutionInterceptor(){
                    public void beforeTransmission(Context.BeforeTransmission context,ExecutionAttributes attributes){transmissionGuard(config,context.httpRequest());}
                }).retryPolicy(RetryPolicy.none()).apiCallTimeout(Duration.ofMinutes(2)).apiCallAttemptTimeout(Duration.ofMinutes(2)).build())
                .httpClientBuilder(ApacheHttpClient.builder().proxyConfiguration(ProxyConfiguration.builder().useSystemPropertyValues(false).useEnvironmentVariableValues(false).build()).connectionTimeout(Duration.ofSeconds(10)).socketTimeout(Duration.ofSeconds(30)).maxConnections(1)).build();
        return new ObjectSource() {
            public Opened get(GetObjectRequest request) {
                ResponseInputStream<GetObjectResponse> stream=client.getObject(request);
                return new Opened(stream,stream.response().sdkHttpResponse().statusCode());
            }
            public void close() {client.close();}
        };
    }
    static int run(InputStream stdin,OutputStream stdout,SourceFactory factory) {
        PrintWriter output=new PrintWriter(new OutputStreamWriter(stdout,StandardCharsets.UTF_8),true);Input input;
        try {input=parse(stdin);}catch(Exception invalid){output.println("{\"id\":null,\"status\":\"INPUT_REJECTED\",\"actualSha256\":null,\"actualLength\":null,\"httpStatus\":null,\"errorCode\":\"INVALID_INPUT\"}");return 2;}
        boolean matched=true;
        try(ObjectSource source=factory.create(input.config())) {
            for(FileFact file:input.files()) {
                Result result=verify(source,input.config(),file);matched&=result.status().equals("MATCH");output.println(JSON.writeValueAsString(result));
            }
        } catch(Exception createOrClose) {
            output.println("{\"id\":null,\"status\":\"READ_ERROR\",\"actualSha256\":null,\"actualLength\":null,\"httpStatus\":null,\"errorCode\":\"CLIENT_LIFECYCLE_ERROR\"}");return 1;
        }
        return matched?0:1;
    }
    static void muteSdkLogs() {
        if(LoggerFactory.getILoggerFactory() instanceof LoggerContext context) {context.reset();context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).setLevel(Level.OFF);}
    }
    public static void main(String[] args) {
        muteSdkLogs();
        if(args.length!=0) {System.out.println("{\"id\":null,\"status\":\"INPUT_REJECTED\",\"actualSha256\":null,\"actualLength\":null,\"httpStatus\":null,\"errorCode\":\"ARGV_NOT_ALLOWED\"}");System.exit(2);}
        System.exit(run(System.in,System.out,G25ReadOnlySourceBytes::createAwsSource));
    }
}
