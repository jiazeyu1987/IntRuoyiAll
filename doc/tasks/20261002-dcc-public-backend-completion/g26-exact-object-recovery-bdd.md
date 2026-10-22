# G26 exact missing local objects recovery preparation

## Goal
Prepare one task-only Java utility and reviewable local recovery plan for four sealed infra_file IDs pointing to three unique missing object keys. Root retains all real GET/PUT, DB, services, Maven, E2E and Git ownership. This child performs local file reads and offline fake-client tests only.

## Milestones
1. Freeze exact ID/key SHA/body SHA/size/MIME/candidate scope from Root protected evidence.
2. Record BDD and effective RED before utility implementation.
3. Implement strict input, local-byte proof, all-three missing preflight, conditional create and all-four final GET proof.
4. Offline GREEN and SDK serialization regression; provide concrete impact/rollback and pending authorization boundary.

## Expected Verification
Compile and run task-only Java tests with already extracted verified G20 AWS SDK 2.44.0 and reactive-streams 1.0.4. No Maven or real network. Verify malformed inputs never create a client, all source bytes verify before client creation, preflight all distinct keys before any PUT, only 404 NoSuchKey permits create, maximum three conditional writes, exact actual serialized If-None-Match header, no retries, truthful partial effects, sanitized output, and no retention/hold/delete/copy/multipart APIs.

## Current Status

ready_for_closeout — bounded utility/probe preparation and offline validation delivered for Root review. Real bucket probe, exact object authorization/recovery and 39-source validation remain Root-owned pending actions, not child PASS claims. Source database upgrade passed under separate Root scope; this utility neither changes DB nor treats that approval as object-write approval.

## Design Constraints / 设计约束检查
- Only g26* task assets owned by this child. Parent task status and other owner files unchanged.
- Fixed config 28/storage 20, literal loopback endpoint and path style, explicit credentials stdin only. ID, key SHA, body SHA, size, MIME and candidate path strictly frozen.
- IDs 9198354931079 and 9198354931095 share one exact key/body. Three unique writes maximum; four independent final GET results.
- All three real preflight GETs must return 404 NoSuchKey before any PUT. An existing object, even matching bytes, is a conflict and is never overwritten.
- PutObject includes If-None-Match:*; intercepted actual HTTP request must preserve it. SDK retries disabled; 409/412/unsupported/denied/uncertain results stop without fallback/retry/delete.
- Local verified bytes are loaded once into immutable task memory and transmitted from that frozen buffer. No new persisted bodies or credentials.
- Existing bucket policy remains unchanged; utility does not add/remove object lock, retention, legal hold or encryption settings, and does not claim historical object metadata or 20-year retention restored.
- Root must prove actual single-pool deployment and writer exclusion from real evidence; image tag alone is not a runtime conditional-write test.
- Technical authorization flag plus reference does not itself constitute user approval; Root obtains concrete three-object write approval before execution.

## BDD
- Given the four frozen IDs and exact local candidates, When the utility parses and hashes bytes, Then wrong IDs/config/key SHA/body SHA/length/MIME/path or malformed JSON fail before creating any client or GET.
- Given three unique keys and one proven alias pair, When preflight runs, Then all three GetObject calls must be exact 404 NoSuchKey before any PutObject; denied, absent bucket, existing object or network failure causes zero PUTs.
- Given all missing preflight results and explicit real authorization supplied by Root, When recovery runs, Then the utility creates each key once with If-None-Match:* and transmits the previously verified exact bytes, followed by four GET hash/length checks.
- Given a competing writer between GET and PUT, When the conditional PUT returns 412/409, Then recovery stops without retry or overwrite and reports any earlier created objects accurately.
- Given unsupported conditional headers, denied requests, uncertain PUT outcome or a wrong/partial final GET, When recovery runs, Then no success is fabricated, no automatic object deletion occurs and further writes stop.
- Given SDK serialization using a fake in-memory HTTP transport, When a PUT is marshalled and signed, Then its actual transmitted method/host/path/header/body/checksum are exact, no copy/multipart/retention/hold header is present, and actual network calls remain zero.
- Given exception/server strings containing keys, paths or secrets, When safe result JSONL is emitted, Then only the approved ID/status/hash/length/HTTP/error/created fields appear.

## Validation Record
- Initial RED: compile behavioral recovery test before utility existed -> exit 1, G26ExactObjectRecovery unavailable. Initial test fixture fluent-builder typing corrected before GREEN.
- GREEN: task-only javac Java 17 + real installed SDK dependency classpath -> exit 0. Recovery test -> 77 offline cases PASS, zero real GET/PUT/DB.
- Additional effective RED: server error after fake store actually wrote bytes was previously classified rejected -> assertion FAIL (exit 1). GREEN classifies 5xx/408 as PUT_OUTCOME_UNCERTAIN and preserves effects without retry/delete. Fixed wrong-bucket test also rejects before client creation.
- Bucket probe RED: behavioral test compile before probe existed -> exit 1, G26ReadOnlyBucketProbe unavailable. Real marshalling regression then caught SDK bare subresource null-valued query representation -> explicit guard failure; guard fixed to accept exactly one empty/null value for only versioning/object-lock.
- Probe GREEN: 34 offline cases PASS. Both real SDK serialization transports were fake in-memory HTTP clients; actual network zero. No ROOT database or service action, no Maven/Git.
- Final regression: recovery 77 PASS and probe 34 PASS after shared-class compilation. All source original candidate byte hashes read only and matched sealed scope; no bodies persisted/copied by tool.
- Actual GET/PUT recovery, actual bucket-policy capture, E2E, long-term retention readiness and Git integration are not claimed by these offline results.

## Root-assigned readonly bucket probe BDD
- Given actual config 28 and frozen bucket SHA, When the auxiliary bucket probe runs, Then it issues only GetBucketVersioning and GetObjectLockConfiguration; no file/body GET, PUT, DELETE or setting change is possible.
- Given malformed input or wrong config/endpoint/bucket, When parsed, Then it rejects before creating any client.
- Given actual policy XML, When read, Then the real versioning/Object Lock/default retention fields and HTTP status are recorded without guessing absent retention or reflecting unsafe error strings.
- Given denied/unavailable/unknown policy, When probed, Then failure is explicit and does not imply a policy value. An actual not-configured response is distinguished from missing bucket or denied access.
- Given a fake in-memory HTTP transport, When the real SDK marshals the two GETs, Then only the exact bucket path and one versioning/object-lock query are transmitted, and actual network calls remain zero.
