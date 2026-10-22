# G26 Root execution contract

## Entry points and dependencies
Integration child directory: `C:/IntRuoyi/20261001-dcc-integration/doc/tasks/20261002-dcc-public-backend-completion`.

Compile all four package-less source files into `g26-exact-object-recovery-runtime/classes` using Java 17 and UTF-8. Runtime classpath is:

1. This child `g26-exact-object-recovery-runtime/classes`.
2. This child `g25-readonly-source-bytes-runtime/libs/*` (41 previously extracted verified G20 libraries including AWS SDK 2.44.0).
3. Root master task `g25-source-bytes-extra-runtime/*` (reactive-streams 1.0.4, original Jar entry verified by Root).

No Maven, install, shared dependency edit or new HTTP client library is required. The deprecated `RetryPolicy.none()` API is deliberately selected from the installed SDK to disable retry; compiler note is recorded rather than suppressed.

Root verifies the delivered source/class/dependency hashes before launching. Credentials are supplied to a Java subprocess in stdin memory only. No input JSON temporary file or shell interpolation, no credentials in argv or log, and no raw exception output. Capture only sanitized stdout JSON and actual process exit code. Logging root OFF prevents SDK credentials/keys from leaking; each failure is still surfaced in structured results.

## Independent readonly bucket probe
Class: `G26ReadOnlyBucketProbe`, **no argv**. Stdin exactly:

```json
{
  "config": {
    "configId": "28",
    "storage": 20,
    "endpoint": "http://127.0.0.1:9000",
    "bucket": "ACTUAL_VALUE_FROM_ROOT_MEMORY",
    "region": "us-east-1",
    "accessKey": "ACTUAL_VALUE_FROM_ROOT_MEMORY",
    "accessSecret": "ACTUAL_VALUE_FROM_ROOT_MEMORY",
    "pathStyle": true
  }
}
```

This is a placeholder shape, not executable production credentials. Duplicate/extra/trailing fields reject. Bucket UTF-8 SHA must be `eef43e6566706fff3d910f5ea7220e06c51ad4bbcd34aa71e8c863ca7cece381`; actual key/name is never printed. Literal `localhost:9000` is also accepted for the same loopback service; other endpoint/port/scheme/path/config/region reject.

Only two actual SDK operations exist: `GetBucketVersioning`, `GetObjectLockConfiguration`. Actual HTTP guard permits GET on exact bucket path with exactly one empty `versioning` or `object-lock` subresource. The SDK represents the bare subresource query as one null value; offline marshalling regression covers it. No object read, HEAD, PUT, DELETE, copy, multipart or bucket setting mutation is available.

Exit 0 means both real responses were captured consistently (`READONLY_POLICY_CAPTURED`); versioning `UNSET` and actual 404 `ObjectLockConfigurationNotFoundError` / `NoSuchObjectLockConfiguration` are explicit facts, not assumed policy. Exit 1 reports denied/unavailable/bad response/lifecycle. Exit 2 rejects input/argv. Success does not mean long-term retention ready. Object legal hold is not a bucket default and the probe does not claim per-object protection facts.

Single sanitized JSON object fields: `bucketSha256`, `status`, `versioningStatus`, `mfaDeleteStatus`, `versioningHttpStatus`, `versioningErrorCode`, `objectLockStatus`, `retentionMode`, `retentionDays`, `retentionYears`, `objectLockHttpStatus`, `objectLockErrorCode`, `lifecycleErrorCode`. All absent facts remain null/UNSET as specified. Unknown server errors become `S3_ERROR`; no reflected exception/message/resource.

Root actual probe has now passed (exit 0): versioning/MFA UNSET with HTTP 200; Object Lock NOT_CONFIGURED with actual HTTP 404 ObjectLockConfigurationNotFoundError; default retention absent, no lifecycle error. Protected actual output/receipt are under `C:/IntRuoyiBackups/20261003-dcc-integration/g26-bucket-policy-probe/`. This is actual bucket-policy read evidence; child offline tests remain separate. Restoration preserves this actual policy and sends no retention/hold fields. Application config 7-day COMPLIANCE/hold values are not actual bucket protection. DCC 20-year business archiving does not imply this bucket has 20-year WORM protection, and this recovery makes no such claim.

## Exact local object recovery
Class: `G26ExactObjectRecovery`, exactly one non-secret argv: `--authorize-exact-local-object-recovery`.

**Actual user approval plus Root review is required before launching.** This flag merely guards accidental launch; the utility cannot authenticate user messages. Root must independently record real approved action identity and perform the checks listed in the reviewed plan. Readonly probe needs no object-write approval.

Stdin config is the same strict config above. Root adds a real `authorizationReference` string and `files` array containing **exactly the four scope IDs**. Each file object contains exactly:

```json
{
  "id": "9198354931064",
  "configId": "28",
  "key": "ACTUAL_INFRA_FILE_PATH_FROM_ROOT_MEMORY",
  "rootExpectedSha256": "2ad539f9095e70d70e94571207a51ca6ca1e3f7a6f7e04947c59db78100212c0",
  "rootExpectedSize": "37120",
  "mime": "application/pdf",
  "candidatePath": "C:/IntRuoyiAll-int_main/doc/tasks/20260918-dcc-upload-full-e2e/upload-source.pdf"
}
```

Input top-level fields are exactly `authorizationReference`, `config`, `files`. Scope is fixed inside production class: all four IDs/config/key SHA/source SHA/size/MIME and the reviewed candidate paths. Positive Long IDs and sizes remain exact strings; MIME and source hashes are actual sealed facts. Root canonical order should be `1064,1068,1079,1095`; actual object order is first appearance of the three distinct keys. Shared 1079/1095 aliases require byte/SHA/length/MIME agreement; any different alias rejects before client creation. Changing to another bucket, key, body or config rejects.

No actual body is written to a local staging file. The selected approved original is opened with NOFOLLOW_LINKS, resolved to its exact original path, read once with bounded size, hash-verified, held in memory and sent using `RequestBody.fromBytes`. Original candidate files remain untouched. No retry/fallback to another source path.

## Result interpretation and partial effects
Result JSONL uses exactly nine fields: `id`, `status`, `phase`, `actualSha256`, `actualLength`, `httpStatus`, `errorCode`, `putAttempted`, `putAccepted`.

- Exit 0 requires all three actual conditionally created objects accepted and all four final GET hash/length proofs successful, no client/output lifecycle failure.
- Exit 1 means local/source, real GET/PUT/final verification/lifecycle failure. Effects are accurately retained in rows. No automatic restore/retry/delete.
- Exit 2 means invalid input or missing technical flag, zero client creation/network.
- `NOT_ATTEMPTED` means no PUT for that row's object. `PREFLIGHT_BLOCKED` means all writes stopped before any PUT. `PUT_REJECTED` means actual definite conditional/permission/unsupported rejection. `PUT_OUTCOME_UNCERTAIN` means network failure, HTTP 408 or other server uncertain response; it may already exist, and Root must read without retry. `CREATED_NOT_VERIFIED` means earlier PUT was accepted but no final proof (later PUT failed). `RESTORED_VERIFIED` requires actual final GET matching exact bytes. `FINAL_READ_ERROR`/`FINAL_BODY_MISMATCH` retain `putAccepted=true` and do not mean changes were undone.
- Existing object at any preflight position is conflict even if bytes happen to match; a delete-marker response is also conflict instead of silently resurrecting it.
- Four result rows may include two `putAccepted=true` values for 1079 and 1095, reflecting the same one physical PUT. **Never count accepted row booleans as unique object writes.** Derive unique operations from the fixed key-SHA mapping: maximum three PUT calls; alias pair maps to one.
- HTTP 5xx may follow actual storage; reported as uncertain. 501 `NotImplemented` is definite unsupported. 409/412/403/other non-timeout 4xx are definite rejected. No conditional response triggers an unconditional retry.
- A client close failure emits an additional null-ID `CLIENT_ERROR` row and exit 1 after retaining all per-ID facts; it does not undo PUTs. Invalid/local early errors may have a single null/failing-ID row and no client creation; Root must not require four rows in those early failure cases or call them successful.
- Final successful hashes are actual object GET digests. Non-success actual digest/partial length is evidence only. No metadata-only shortcut or ETag-as-SHA substitution.

Root must record physical object operation counts separately from per-ID proof counts, followed by the original independent 39-entry source-byte reader. Four restored references alone do not prove the other 35 current objects unchanged. Neither this recovery nor the readonly probe is frontend business E2E.

## Boundaries still requiring Root verification
User actual object-write authorization; current metadata matches sealed scope; writer exclusion; exact binary/one-local-pool topology; actual bucket policy; final output directory ownership; no service/credential/log leak. Code intentionally has no per-object legal hold/retention setter and cannot create a business retention approval on its own. Any additional metadata policy work requires its own concrete scope.
