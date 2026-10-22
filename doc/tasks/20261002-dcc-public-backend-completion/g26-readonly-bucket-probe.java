import com.fasterxml.jackson.databind.JsonNode;
import software.amazon.awssdk.auth.credentials.*;
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
import java.time.Duration;
import java.util.*;

/** Root-only readonly local bucket probe; no object/body API or mutation. */
final class G26ReadOnlyBucketProbe {
    interface Probe extends AutoCloseable{GetBucketVersioningResponse versioning(GetBucketVersioningRequest r)throws Exception;GetObjectLockConfigurationResponse lock(GetObjectLockConfigurationRequest r)throws Exception;default void close()throws Exception{}}
    interface Factory{Probe create(G26ExactObjectRecovery.Config c)throws Exception;}
    static final class Report {
        public String bucketSha256,status,versioningStatus,mfaDeleteStatus,versioningErrorCode,objectLockStatus,retentionMode,objectLockErrorCode,lifecycleErrorCode;
        public Integer versioningHttpStatus,retentionDays,retentionYears,objectLockHttpStatus;
    }
    static G26ExactObjectRecovery.Config parse(InputStream stdin,String expectedSha)throws Exception{
        byte[] raw=stdin.readNBytes(16385);G26ExactObjectRecovery.require(raw.length>0&&raw.length<=16384);JsonNode root=G26ExactObjectRecovery.JSON.readTree(raw);G26ExactObjectRecovery.fields(root,"config");JsonNode c=root.get("config");G26ExactObjectRecovery.fields(c,"configId","storage","endpoint","bucket","region","accessKey","accessSecret","pathStyle");
        G26ExactObjectRecovery.require(G26ExactObjectRecovery.text(c,"configId").equals("28")&&c.get("storage").isInt()&&c.get("storage").intValue()==20&&c.get("pathStyle").isBoolean()&&c.get("pathStyle").booleanValue());
        URI endpoint=URI.create(G26ExactObjectRecovery.text(c,"endpoint"));G26ExactObjectRecovery.require(endpoint.getScheme().equals("http")&&Set.of("127.0.0.1","localhost").contains(endpoint.getHost())&&endpoint.getPort()==9000&&endpoint.getRawUserInfo()==null&&endpoint.getRawQuery()==null&&endpoint.getRawFragment()==null&&(endpoint.getRawPath()==null||endpoint.getRawPath().isEmpty()||endpoint.getRawPath().equals("/")));
        String bucket=G26ExactObjectRecovery.text(c,"bucket"),region=G26ExactObjectRecovery.text(c,"region");G26ExactObjectRecovery.require(bucket.matches("[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]")&&!bucket.contains("..")&&G26ExactObjectRecovery.hash(bucket.getBytes(StandardCharsets.UTF_8)).equals(expectedSha)&&region.equals("us-east-1"));
        return new G26ExactObjectRecovery.Config("28",20,endpoint,bucket,region,G26ExactObjectRecovery.text(c,"accessKey"),G26ExactObjectRecovery.text(c,"accessSecret"));
    }
    static void guard(G26ExactObjectRecovery.Config c,SdkHttpRequest r){
        boolean query=r.rawQueryParameters().size()==1&&r.rawQueryParameters().entrySet().stream().allMatch(e->Set.of("versioning","object-lock").contains(e.getKey())&&e.getValue().size()<=1&&e.getValue().stream().allMatch(value->value==null||value.isEmpty()));
        if(r.method()!=SdkHttpMethod.GET)throw SdkClientException.create("G26_BUCKET_GUARD_METHOD");
        if(!r.protocol().equals(c.endpoint().getScheme())||!r.host().equals(c.endpoint().getHost())||r.port()!=9000)throw SdkClientException.create("G26_BUCKET_GUARD_ENDPOINT");
        if(!r.encodedPath().equals("/"+c.bucket()))throw SdkClientException.create("G26_BUCKET_GUARD_PATH");
        if(!query)throw SdkClientException.create("G26_BUCKET_GUARD_QUERY");
    }
    static Probe createAws(G26ExactObjectRecovery.Config c,SdkHttpClient offline){
        var b=S3Client.builder().endpointOverride(c.endpoint()).region(Region.of(c.region())).credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(c.accessKey(),c.accessSecret()))).serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build()).overrideConfiguration(ClientOverrideConfiguration.builder().addExecutionInterceptor(new ExecutionInterceptor(){public void beforeTransmission(Context.BeforeTransmission x,ExecutionAttributes a){guard(c,x.httpRequest());}}).retryPolicy(RetryPolicy.none()).apiCallTimeout(Duration.ofSeconds(60)).apiCallAttemptTimeout(Duration.ofSeconds(60)).build());
        if(offline!=null)b.httpClient(offline);else b.httpClientBuilder(ApacheHttpClient.builder().proxyConfiguration(ProxyConfiguration.builder().useSystemPropertyValues(false).useEnvironmentVariableValues(false).build()).connectionTimeout(Duration.ofSeconds(10)).socketTimeout(Duration.ofSeconds(30)).maxConnections(1));
        S3Client client=b.build();return new Probe(){public GetBucketVersioningResponse versioning(GetBucketVersioningRequest r){return client.getBucketVersioning(r);}public GetObjectLockConfigurationResponse lock(GetObjectLockConfigurationRequest r){return client.getObjectLockConfiguration(r);}public void close(){client.close();}};
    }
    static String safeCode(S3Exception e){String code=e.awsErrorDetails()==null?null:e.awsErrorDetails().errorCode();return code!=null&&Set.of("ObjectLockConfigurationNotFoundError","NoSuchObjectLockConfiguration","NoSuchBucket","AccessDenied","InvalidAccessKeyId","SignatureDoesNotMatch","InvalidRequest","NotImplemented","InternalError","ServiceUnavailable","RequestTimeout").contains(code)?code:"S3_ERROR";}
    static boolean versioning(Probe probe,G26ExactObjectRecovery.Config c,Report r){
        try{var value=probe.versioning(GetBucketVersioningRequest.builder().bucket(c.bucket()).build());r.versioningHttpStatus=value.sdkHttpResponse().statusCode();G26ExactObjectRecovery.require(r.versioningHttpStatus==200);String status=value.statusAsString(),mfa=value.mfaDeleteAsString();G26ExactObjectRecovery.require(status==null||Set.of("Enabled","Suspended").contains(status));G26ExactObjectRecovery.require(mfa==null||Set.of("Enabled","Disabled").contains(mfa));r.versioningStatus=status==null?"UNSET":status;r.mfaDeleteStatus=mfa==null?"UNSET":mfa;return true;}
        catch(S3Exception e){r.versioningHttpStatus=e.statusCode();r.versioningErrorCode=safeCode(e);return false;}catch(Exception e){r.versioningErrorCode="VERSIONING_READ_OR_RESPONSE_ERROR";return false;}
    }
    static boolean lock(Probe probe,G26ExactObjectRecovery.Config c,Report r){
        try{var value=probe.lock(GetObjectLockConfigurationRequest.builder().bucket(c.bucket()).build());r.objectLockHttpStatus=value.sdkHttpResponse().statusCode();G26ExactObjectRecovery.require(r.objectLockHttpStatus==200);var config=value.objectLockConfiguration();G26ExactObjectRecovery.require(config!=null&&"Enabled".equals(config.objectLockEnabledAsString()));r.objectLockStatus="Enabled";var rule=config.rule();if(rule!=null&&rule.defaultRetention()!=null){var d=rule.defaultRetention();G26ExactObjectRecovery.require(Set.of("COMPLIANCE","GOVERNANCE").contains(d.modeAsString())&&(d.days()!=null) != (d.years()!=null));G26ExactObjectRecovery.require(d.days()==null||d.days()>0);G26ExactObjectRecovery.require(d.years()==null||d.years()>0);r.retentionMode=d.modeAsString();r.retentionDays=d.days();r.retentionYears=d.years();}return true;}
        catch(S3Exception e){r.objectLockHttpStatus=e.statusCode();r.objectLockErrorCode=safeCode(e);if(e.statusCode()==404&&Set.of("ObjectLockConfigurationNotFoundError","NoSuchObjectLockConfiguration").contains(r.objectLockErrorCode)){r.objectLockStatus="NOT_CONFIGURED";return true;}return false;}catch(Exception e){r.objectLockErrorCode="OBJECT_LOCK_READ_OR_RESPONSE_ERROR";return false;}
    }
    static int run(InputStream stdin,OutputStream stdout,Factory factory,String expectedSha){var out=new PrintWriter(new OutputStreamWriter(stdout,StandardCharsets.UTF_8),true);Report report=new Report();G26ExactObjectRecovery.Config c;
        try{c=parse(stdin,expectedSha);report.bucketSha256=expectedSha;}catch(Exception e){report.status="INPUT_REJECTED";report.lifecycleErrorCode="INVALID_INPUT";try{out.println(G26ExactObjectRecovery.JSON.writeValueAsString(report));}catch(Exception impossible){return 2;}return 2;}
        boolean good=false;try(Probe probe=factory.create(c)){boolean version=versioning(probe,c,report),lock=lock(probe,c,report);good=version&&lock;}catch(Exception e){report.lifecycleErrorCode="CLIENT_LIFECYCLE_ERROR";good=false;}report.status=good?"READONLY_POLICY_CAPTURED":"PROBE_FAILED";try{out.println(G26ExactObjectRecovery.JSON.writeValueAsString(report));}catch(Exception e){return 1;}return good&&!out.checkError()?0:1;
    }
    public static void main(String[] args){G26ExactObjectRecovery.muteSdkLogs();if(args.length!=0){System.out.println("{\"status\":\"INPUT_REJECTED\",\"lifecycleErrorCode\":\"ARGV_NOT_ALLOWED\"}");System.exit(2);}System.exit(run(System.in,System.out,c->createAws(c,null),G26ExactObjectRecovery.PRODUCTION_BUCKET_SHA));}
}
