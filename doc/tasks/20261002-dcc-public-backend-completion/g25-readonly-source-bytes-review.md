# G25 readonly legacy source-byte reader

ready_for_closeout. Task-only Java reader compiled from the exact verifiedG20Jar AWS SDK2.44.0;50 offline cases PASS. No actualSDKclient/networkGET/database/service/Maven/Git action in this Agent batch. Actual39 object reads/config collection stay with Root. FormalJava/module/SQL/G21/G23 remain unchanged.

## Runtime invocation and secret boundary

Reader file g25-readonly-source-bytes.java declares G25ReadOnlySourceBytes. Runtime classpath is own g25-readonly-source-bytes-runtime/classes plus libs/*.jar. Root invokes Java with no application arguments and sends JSON directly on stdin from memory; never create a config file, command argument, environment credential or shell echo of the payload. Access key/secret are required explicit in-memory StaticCredentialsProvider values, not a default credential chain.

Input schema:

`{config:{configId:"28",storage:20,endpoint,bucket,region,accessKey,accessSecret,pathStyle:true},files:[{id:string,configId:"28",key,rootExpectedSha256:string,rootExpectedSize:string}]}`

files must contain exactly39 unique positive JavaLong decimal-string IDs; large9007199254740993 remains exact. Expectedsize is a nonnegative exactLong decimal string. ExpectedSHA is exactly64 lowercasehex. No unsafe numeric ID/size conversion. No unknown/duplicate JSON fields, second trailingJSON object or input larger than2MiB. Config28/storage20 required, region explicitly nonblank, pathStyle=true, actuallocalhost/127.0.0.1 http/https endpoint without auth/query/fragment/foreignpath; key is exact S3object metadata, max1024UTF8bytes, no URL/backslash/dot-path/controlchars.

AWS implementation uses only S3Client.getObject(GetObjectRequest), ResponseInputStream and64KiB buffer → SHA256/long count. No HEAD/list/download-to-file/PUT/copy/delete/bucket write, persistence of body, automatic correction or fallback. Retries are disabled. Apache system/environment proxy discovery is explicitly disabled; execution interceptor refuses any nonGET method or HTTP host/scheme/port different from validatedlocal endpoint. No implicit remote provider or callerselected write action.

Only sanitized stdout JSONL fields:

`{id,status,actualSha256,actualLength,httpStatus,errorCode}`

Normalstatus is MATCH/MISMATCH/HTTP_ERROR/READ_ERROR. actual digest/length are available only for a completed stream; a partial IO failure reports partialcount and nullhash, never MATCH. HTTP status remains the actual service status; AWS code is allowlisted machinecode(NoSuchKey/AccessDenied/etc), arbitrary remote strings become S3_ERROR. Error/credential/objectkey/URL/bucket/exceptions never appear. Reader mutes task-localLogback before parsing config/client setup; Root additionally supplies its temporaryrootOFF logback configuration. No failures are suppressed into successful output.

Exit0 requires39 MATCH and exact bothSHA+size equality. Any read/HTTP/mismatch/client lifecycle failure exits1. Invalidinput or applicationarguments exits2 with idnull/globalsanitized error. All39requested objects are evaluated individually with no retry; an error does not silently vanish or shift identity. Stream and S3 client resources close. Client creation/close failure emits a global sanitizedREAD_ERROR; Root must reject outputcounts/statuses and cannot treat partial39 rows as aggregate success.

## Evidence and actual GET provenance

The Jar was checked before extraction:507250868bytes/SHA a4cf78f59d18ac333c64029dd86cf18bf4188c6f0e75762ac3a90eb1a516ef16 from main g20-verification-receipt.json. OldG18ec540Jar receipt was rejected because the currentlypackaged file has changed; no old/newmix. Extracted41 exactofficial SDK/Jackson/HTTP/logger runtime libs into this task's temporaryruntime directory; extraction manifest records eachraw entrysha. Reader/tests/temporarycompiledclasses fingerprints are in g25-readonly-source-bytes-delivery-fingerprints.json. Do not include copiedlibs/classes infinal sourcecommit; Root can cleanup after using the compiledhelper and retained source/evidence.

BDD RED: g25-readonly-source-bytes-red.log reader classmissing before implementation. javac main+test compile PASS in g25-readonly-source-bytes-compile.log using JDK17 and existing41 libs only (deprecated RetryPolicy API advisory retained). Final g25-readonly-source-bytes-final-green.log:50offline cases PASS, actualGET0. Real official GetObjectRequest is constructed but SourceFactory is explicit fake; SDK builder/client is never created by tests. Cases cover all39exactlargeIDs, SHA/length/empty mismatch, unsafe/duplicate/foreign config/object/endpoint, region/pathStyle, missing/unknown/trailingJSON,404/403/500/unknownservercode,SDKclient/partialIO failure,50checks withresource closure and readonlytransmission guard. A one-shot partialIO testfixture initially created an infiniteInputStream; only owntest PID7160 was stopped, fixturecorrected and validGREENreexecuted. This was invalidtest behavior, not legacy/objectstorage outcome.

Root must separately seal runtimeDB/UUID/metadata collector query/facts SHA, exact39 sourcefile IDs/expectedSHA/size/name/config28/storage20 mapping and helpercompiled/source/outputSHA. The reader does not return filenames/Master/config/keys and cannot prove those relations from bodyoutput. Root's caller compares all39 uniqueIDs and twoactualmetrics and joins byexact sourceFileId to the alreadyfrozenDBmetadata; no guessedname/claim/project business rule. stdin credentials must staymemoryonly; neither fullinputnor configdump may enter any receipt/log. Optionalsafe input metadata manifest must exclude credentials/keys/endpoint. Snapshotbodyvalidation is read-onlylegacy evidence, not GUIE2E or claimbackfill authorization.

All Java compile/testprocesses ended and a focused process inventory found no G25Reader/test Java process. Current RootlocalDB authorization may coverlater operations, but this child's actualGET remains unexecuted. Root owns realreads and business interpretation.
