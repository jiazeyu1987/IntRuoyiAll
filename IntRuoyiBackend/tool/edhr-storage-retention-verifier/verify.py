from __future__ import annotations

import json
import os
import sys
import uuid
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
from typing import Any


REQUIRED_ENV = [
    "EDHR_S3_ENDPOINT",
    "EDHR_S3_BUCKET",
    "EDHR_S3_REGION",
    "EDHR_S3_ACCESS_KEY",
    "EDHR_S3_SECRET_KEY",
    "EDHR_S3_RETENTION_MODE",
    "EDHR_S3_RETAIN_UNTIL_DAYS",
    "EDHR_S3_REQUIRE_LEGAL_HOLD",
]

VALID_RETENTION_MODES = {"GOVERNANCE", "COMPLIANCE"}
LEGAL_HOLD_TRUE_VALUES = {"1", "true", "yes", "y", "on"}
LEGAL_HOLD_FALSE_VALUES = {"0", "false", "no", "n", "off"}

EXIT_PASS = 0
EXIT_FAIL = 1
EXIT_BLOCKED = 2


@dataclass(frozen=True)
class VerifierConfig:
    endpoint: str
    bucket: str
    region: str
    access_key: str
    secret_key: str
    retention_mode: str
    retain_until_days: int
    require_legal_hold: bool


def main() -> int:
    env = _read_required_env()
    result = _base_result(env)

    try:
        return _run(env, result)
    except Exception as exc:  # noqa: BLE001 - JSON output must remain stable for verifier callers.
        _add_check(
            result,
            "unexpectedException",
            "FAIL",
            _sanitize(f"{type(exc).__name__}: {exc}", env),
        )
        result["status"] = "FAIL"
        _emit(result)
        return EXIT_FAIL


def _run(env: dict[str, str], result: dict[str, Any]) -> int:
    missing_env = [name for name in REQUIRED_ENV if not env[name]]
    if missing_env:
        return _blocked(
            result,
            missing_env,
            "requiredEnvironment",
            "Required environment variables are missing or empty.",
        )
    _add_check(result, "requiredEnvironment", "PASS", "All required environment variables are present.")

    config, config_errors = _parse_config(env)
    if config_errors:
        return _blocked(result, config_errors, "environmentValues", "Required environment values are invalid.")
    result["bucket"] = config.bucket
    result["retentionMode"] = config.retention_mode

    dependencies = _load_dependencies()
    if dependencies["missing"]:
        return _blocked(
            result,
            dependencies["missing"],
            "pythonDependencies",
            "Required Python dependencies are not installed.",
        )
    _add_check(result, "pythonDependencies", "PASS", "boto3 and botocore are importable.")

    boto3 = dependencies["boto3"]
    Config = dependencies["Config"]
    ClientError = dependencies["ClientError"]
    BotoCoreError = dependencies["BotoCoreError"]

    try:
        client = boto3.client(
            "s3",
            endpoint_url=config.endpoint,
            region_name=config.region,
            aws_access_key_id=config.access_key,
            aws_secret_access_key=config.secret_key,
            config=Config(signature_version="s3v4"),
        )
    except Exception as exc:  # noqa: BLE001 - boto client construction errors are prerequisites.
        return _blocked(
            result,
            ["boto3 S3 client construction"],
            "s3Client",
            _sanitize(f"Could not construct S3 client: {type(exc).__name__}: {exc}", env),
        )

    versioning_exit = _require_bucket_versioning(client, config, result, ClientError, BotoCoreError, env)
    if versioning_exit is not None:
        return versioning_exit

    object_lock_exit = _require_object_lock(client, config, result, ClientError, BotoCoreError, env)
    if object_lock_exit is not None:
        return object_lock_exit

    retain_until = datetime.now(timezone.utc).replace(microsecond=0) + timedelta(
        days=config.retain_until_days
    )
    result["retainUntil"] = _format_datetime(retain_until)

    timestamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%SZ")
    key = f"edhr-retention-verifier/{timestamp}-{uuid.uuid4().hex}.txt"
    result["key"] = key
    body = (
        "edhr storage retention verifier\n"
        f"createdAt={_format_datetime(datetime.now(timezone.utc))}\n"
        f"retentionMode={config.retention_mode}\n"
    ).encode("utf-8")

    upload_exit = _upload_protected_object(
        client,
        config,
        result,
        key,
        body,
        retain_until,
        ClientError,
        BotoCoreError,
        env,
    )
    if upload_exit is not None:
        return upload_exit

    version_id = result["versionId"]

    retention_exit = _verify_retention(
        client,
        config,
        result,
        key,
        version_id,
        retain_until,
        ClientError,
        BotoCoreError,
        env,
    )
    if retention_exit is not None:
        return retention_exit

    legal_hold_exit = _verify_legal_hold(
        client,
        config,
        result,
        key,
        version_id,
        ClientError,
        BotoCoreError,
        env,
    )
    if legal_hold_exit is not None:
        return legal_hold_exit

    delete_exit = _verify_delete_is_denied(
        client,
        config,
        result,
        key,
        version_id,
        ClientError,
        BotoCoreError,
        env,
    )
    if delete_exit is not None:
        return delete_exit

    readable_exit = _verify_protected_version_readable(
        client,
        config,
        result,
        key,
        version_id,
        body,
        ClientError,
        BotoCoreError,
        env,
    )
    if readable_exit is not None:
        return readable_exit

    result["status"] = "PASS"
    _emit(result)
    return EXIT_PASS


def _read_required_env() -> dict[str, str]:
    return {name: (os.environ.get(name) or "").strip() for name in REQUIRED_ENV}


def _parse_config(env: dict[str, str]) -> tuple[VerifierConfig | None, list[str]]:
    errors: list[str] = []

    retention_mode = env["EDHR_S3_RETENTION_MODE"].upper()
    if retention_mode not in VALID_RETENTION_MODES:
        errors.append("EDHR_S3_RETENTION_MODE must be GOVERNANCE or COMPLIANCE")

    retain_until_days: int | None = None
    try:
        retain_until_days = int(env["EDHR_S3_RETAIN_UNTIL_DAYS"])
        if retain_until_days <= 0:
            raise ValueError("must be positive")
    except ValueError:
        errors.append("EDHR_S3_RETAIN_UNTIL_DAYS must be a positive integer")

    legal_hold_value = env["EDHR_S3_REQUIRE_LEGAL_HOLD"].lower()
    require_legal_hold: bool | None = None
    if legal_hold_value in LEGAL_HOLD_TRUE_VALUES:
        require_legal_hold = True
    elif legal_hold_value in LEGAL_HOLD_FALSE_VALUES:
        require_legal_hold = False
    else:
        errors.append("EDHR_S3_REQUIRE_LEGAL_HOLD must be true or false")

    if errors:
        return None, errors

    return (
        VerifierConfig(
            endpoint=env["EDHR_S3_ENDPOINT"],
            bucket=env["EDHR_S3_BUCKET"],
            region=env["EDHR_S3_REGION"],
            access_key=env["EDHR_S3_ACCESS_KEY"],
            secret_key=env["EDHR_S3_SECRET_KEY"],
            retention_mode=retention_mode,
            retain_until_days=retain_until_days if retain_until_days is not None else 0,
            require_legal_hold=require_legal_hold if require_legal_hold is not None else False,
        ),
        [],
    )


def _load_dependencies() -> dict[str, Any]:
    dependencies: dict[str, Any] = {
        "boto3": None,
        "Config": None,
        "ClientError": None,
        "BotoCoreError": None,
        "missing": [],
    }
    try:
        import boto3  # type: ignore[import-not-found]

        dependencies["boto3"] = boto3
    except ImportError:
        dependencies["missing"].append("python package boto3")

    try:
        from botocore.config import Config  # type: ignore[import-not-found]
        from botocore.exceptions import BotoCoreError, ClientError  # type: ignore[import-not-found]

        dependencies["Config"] = Config
        dependencies["ClientError"] = ClientError
        dependencies["BotoCoreError"] = BotoCoreError
    except ImportError:
        dependencies["missing"].append("python package botocore")

    return dependencies


def _require_bucket_versioning(
    client: Any,
    config: VerifierConfig,
    result: dict[str, Any],
    ClientError: type[Exception],
    BotoCoreError: type[Exception],
    env: dict[str, str],
) -> int | None:
    try:
        response = client.get_bucket_versioning(Bucket=config.bucket)
    except ClientError as exc:
        return _blocked(
            result,
            ["EDHR_S3_BUCKET versioning read permission or existing bucket"],
            "bucketVersioning",
            f"Could not read bucket versioning: {_client_error_detail(exc, env)}",
        )
    except BotoCoreError as exc:
        return _blocked(
            result,
            ["reachable S3 endpoint for bucket versioning"],
            "bucketVersioning",
            _sanitize(f"Could not read bucket versioning: {type(exc).__name__}: {exc}", env),
        )

    status = response.get("Status")
    if status != "Enabled":
        return _blocked(
            result,
            ["EDHR_S3_BUCKET versioning must be Enabled"],
            "bucketVersioning",
            f"Bucket versioning status is {status or 'unset'}, expected Enabled.",
        )

    _add_check(result, "bucketVersioning", "PASS", "Bucket versioning is Enabled.")
    return None


def _require_object_lock(
    client: Any,
    config: VerifierConfig,
    result: dict[str, Any],
    ClientError: type[Exception],
    BotoCoreError: type[Exception],
    env: dict[str, str],
) -> int | None:
    try:
        response = client.get_object_lock_configuration(Bucket=config.bucket)
    except ClientError as exc:
        return _blocked(
            result,
            ["EDHR_S3_BUCKET Object Lock configuration must be readable and Enabled"],
            "objectLockConfiguration",
            f"Could not read Object Lock configuration: {_client_error_detail(exc, env)}",
        )
    except BotoCoreError as exc:
        return _blocked(
            result,
            ["reachable S3 endpoint for Object Lock configuration"],
            "objectLockConfiguration",
            _sanitize(f"Could not read Object Lock configuration: {type(exc).__name__}: {exc}", env),
        )

    configuration = response.get("ObjectLockConfiguration") or {}
    enabled = configuration.get("ObjectLockEnabled")
    if enabled != "Enabled":
        return _blocked(
            result,
            ["EDHR_S3_BUCKET ObjectLockEnabled must be Enabled"],
            "objectLockConfiguration",
            f"Bucket ObjectLockEnabled is {enabled or 'unset'}, expected Enabled.",
        )

    _add_check(result, "objectLockConfiguration", "PASS", "Bucket ObjectLockEnabled is Enabled.")
    return None


def _upload_protected_object(
    client: Any,
    config: VerifierConfig,
    result: dict[str, Any],
    key: str,
    body: bytes,
    retain_until: datetime,
    ClientError: type[Exception],
    BotoCoreError: type[Exception],
    env: dict[str, str],
) -> int | None:
    request: dict[str, Any] = {
        "Bucket": config.bucket,
        "Key": key,
        "Body": body,
        "ContentType": "text/plain; charset=utf-8",
        "ObjectLockMode": config.retention_mode,
        "ObjectLockRetainUntilDate": retain_until,
    }
    if config.require_legal_hold:
        request["ObjectLockLegalHoldStatus"] = "ON"

    try:
        response = client.put_object(**request)
    except ClientError as exc:
        return _blocked(
            result,
            ["S3 PutObject permission with Object Lock headers"],
            "putProtectedObject",
            f"Could not upload protected object: {_client_error_detail(exc, env)}",
        )
    except BotoCoreError as exc:
        return _blocked(
            result,
            ["reachable S3 endpoint for PutObject"],
            "putProtectedObject",
            _sanitize(f"Could not upload protected object: {type(exc).__name__}: {exc}", env),
        )

    version_id = response.get("VersionId")
    if not version_id:
        return _fail(
            result,
            "putProtectedObject",
            "PutObject succeeded but response did not include VersionId.",
        )

    result["versionId"] = version_id
    _add_check(
        result,
        "putProtectedObject",
        "PASS",
        "Protected object uploaded with retention headers and returned a VersionId.",
        {"versionId": version_id},
    )
    return None


def _verify_retention(
    client: Any,
    config: VerifierConfig,
    result: dict[str, Any],
    key: str,
    version_id: str,
    expected_retain_until: datetime,
    ClientError: type[Exception],
    BotoCoreError: type[Exception],
    env: dict[str, str],
) -> int | None:
    try:
        response = client.get_object_retention(Bucket=config.bucket, Key=key, VersionId=version_id)
    except ClientError as exc:
        return _blocked(
            result,
            ["S3 GetObjectRetention permission for protected version"],
            "getObjectRetention",
            f"Could not read object retention: {_client_error_detail(exc, env)}",
        )
    except BotoCoreError as exc:
        return _blocked(
            result,
            ["reachable S3 endpoint for GetObjectRetention"],
            "getObjectRetention",
            _sanitize(f"Could not read object retention: {type(exc).__name__}: {exc}", env),
        )

    retention = response.get("Retention") or {}
    actual_mode = retention.get("Mode")
    actual_retain_until = _as_utc_datetime(retention.get("RetainUntilDate"))
    result["retentionMode"] = actual_mode
    result["retainUntil"] = _format_datetime(actual_retain_until) if actual_retain_until else None

    if actual_mode != config.retention_mode:
        return _fail(
            result,
            "getObjectRetention",
            f"Retention mode is {actual_mode or 'unset'}, expected {config.retention_mode}.",
        )
    if actual_retain_until is None:
        return _fail(result, "getObjectRetention", "RetainUntilDate is missing from retention evidence.")
    if actual_retain_until < expected_retain_until:
        return _fail(
            result,
            "getObjectRetention",
            (
                "RetainUntilDate is earlier than requested: "
                f"actual={_format_datetime(actual_retain_until)}, "
                f"expectedAtLeast={_format_datetime(expected_retain_until)}."
            ),
        )

    _add_check(
        result,
        "getObjectRetention",
        "PASS",
        "Object retention evidence matches the requested mode and retain-until policy.",
        {
            "mode": actual_mode,
            "retainUntil": _format_datetime(actual_retain_until),
        },
    )
    return None


def _verify_legal_hold(
    client: Any,
    config: VerifierConfig,
    result: dict[str, Any],
    key: str,
    version_id: str,
    ClientError: type[Exception],
    BotoCoreError: type[Exception],
    env: dict[str, str],
) -> int | None:
    try:
        response = client.get_object_legal_hold(Bucket=config.bucket, Key=key, VersionId=version_id)
    except ClientError as exc:
        return _blocked(
            result,
            ["S3 GetObjectLegalHold permission for protected version"],
            "getObjectLegalHold",
            f"Could not read object legal hold: {_client_error_detail(exc, env)}",
        )
    except BotoCoreError as exc:
        return _blocked(
            result,
            ["reachable S3 endpoint for GetObjectLegalHold"],
            "getObjectLegalHold",
            _sanitize(f"Could not read object legal hold: {type(exc).__name__}: {exc}", env),
        )

    legal_hold = response.get("LegalHold") or {}
    actual_status = legal_hold.get("Status")
    result["legalHoldStatus"] = actual_status

    if actual_status not in {"ON", "OFF"}:
        return _fail(
            result,
            "getObjectLegalHold",
            f"LegalHold.Status is {actual_status or 'unset'}, expected ON or OFF evidence.",
        )
    if config.require_legal_hold and actual_status != "ON":
        return _fail(
            result,
            "getObjectLegalHold",
            f"LegalHold.Status is {actual_status}, expected ON.",
        )

    expected = "ON" if config.require_legal_hold else "readable ON/OFF evidence"
    _add_check(
        result,
        "getObjectLegalHold",
        "PASS",
        f"Object legal hold evidence is {actual_status}; expected {expected}.",
        {"legalHoldStatus": actual_status},
    )
    return None


def _verify_delete_is_denied(
    client: Any,
    config: VerifierConfig,
    result: dict[str, Any],
    key: str,
    version_id: str,
    ClientError: type[Exception],
    BotoCoreError: type[Exception],
    env: dict[str, str],
) -> int | None:
    try:
        client.delete_object(Bucket=config.bucket, Key=key, VersionId=version_id)
    except ClientError as exc:
        detail = _client_error_summary(exc, env)
        if _is_delete_denied(detail):
            _add_check(
                result,
                "deleteProtectedVersion",
                "PASS",
                "DeleteObject for the protected version was rejected.",
                detail,
            )
            return None
        if detail.get("code") in {"NoSuchKey", "NoSuchVersion"}:
            return _fail(
                result,
                "deleteProtectedVersion",
                f"Protected version disappeared before delete verification: {_format_error_detail(detail)}",
            )
        return _blocked(
            result,
            ["deterministic DeleteObject response for protected version"],
            "deleteProtectedVersion",
            f"DeleteObject could not complete deterministically: {_format_error_detail(detail)}",
        )
    except BotoCoreError as exc:
        return _blocked(
            result,
            ["reachable S3 endpoint for DeleteObject"],
            "deleteProtectedVersion",
            _sanitize(f"DeleteObject could not complete: {type(exc).__name__}: {exc}", env),
        )

    return _fail(
        result,
        "deleteProtectedVersion",
        "DeleteObject unexpectedly succeeded for the protected object version.",
    )


def _verify_protected_version_readable(
    client: Any,
    config: VerifierConfig,
    result: dict[str, Any],
    key: str,
    version_id: str,
    expected_body: bytes,
    ClientError: type[Exception],
    BotoCoreError: type[Exception],
    env: dict[str, str],
) -> int | None:
    try:
        response = client.get_object(Bucket=config.bucket, Key=key, VersionId=version_id)
        stream = response["Body"]
        try:
            body = stream.read()
        finally:
            close = getattr(stream, "close", None)
            if callable(close):
                close()
    except ClientError as exc:
        detail = _client_error_summary(exc, env)
        if detail.get("code") in {"NoSuchKey", "NoSuchVersion"}:
            return _fail(
                result,
                "protectedVersionReadable",
                f"Protected version cannot be read after delete rejection: {_format_error_detail(detail)}",
            )
        return _blocked(
            result,
            ["S3 GetObject permission for protected version"],
            "protectedVersionReadable",
            f"Could not read protected version: {_format_error_detail(detail)}",
        )
    except BotoCoreError as exc:
        return _blocked(
            result,
            ["reachable S3 endpoint for GetObject"],
            "protectedVersionReadable",
            _sanitize(f"Could not read protected version: {type(exc).__name__}: {exc}", env),
        )

    returned_version_id = response.get("VersionId")
    if returned_version_id and returned_version_id != version_id:
        return _fail(
            result,
            "protectedVersionReadable",
            f"GetObject returned VersionId {returned_version_id}, expected {version_id}.",
        )
    if body != expected_body:
        return _fail(result, "protectedVersionReadable", "Protected version body changed unexpectedly.")

    _add_check(
        result,
        "protectedVersionReadable",
        "PASS",
        "Protected object version remains readable after rejected delete attempt.",
        {"versionId": version_id},
    )
    return None


def _base_result(env: dict[str, str]) -> dict[str, Any]:
    return {
        "status": "BLOCKED",
        "missingPrerequisites": [],
        "bucket": env.get("EDHR_S3_BUCKET") or None,
        "key": None,
        "versionId": None,
        "retentionMode": None,
        "retainUntil": None,
        "legalHoldStatus": None,
        "checks": [],
    }


def _blocked(
    result: dict[str, Any],
    missing_prerequisites: list[str],
    check_name: str,
    detail: str,
) -> int:
    result["status"] = "BLOCKED"
    result["missingPrerequisites"] = missing_prerequisites
    _add_check(result, check_name, "BLOCKED", detail, {"missingPrerequisites": missing_prerequisites})
    _emit(result)
    return EXIT_BLOCKED


def _fail(result: dict[str, Any], check_name: str, detail: str) -> int:
    result["status"] = "FAIL"
    _add_check(result, check_name, "FAIL", detail)
    _emit(result)
    return EXIT_FAIL


def _add_check(
    result: dict[str, Any],
    name: str,
    status: str,
    detail: str,
    evidence: dict[str, Any] | None = None,
) -> None:
    check: dict[str, Any] = {
        "name": name,
        "status": status,
        "detail": detail,
    }
    if evidence:
        check["evidence"] = evidence
    result["checks"].append(check)


def _emit(result: dict[str, Any]) -> None:
    print(json.dumps(result, ensure_ascii=False, indent=2))


def _as_utc_datetime(value: Any) -> datetime | None:
    if not isinstance(value, datetime):
        return None
    if value.tzinfo is None:
        return value.replace(tzinfo=timezone.utc)
    return value.astimezone(timezone.utc)


def _format_datetime(value: datetime) -> str:
    return value.astimezone(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")


def _client_error_summary(exc: Exception, env: dict[str, str]) -> dict[str, Any]:
    response = getattr(exc, "response", {}) or {}
    error = response.get("Error", {}) or {}
    metadata = response.get("ResponseMetadata", {}) or {}
    message = _sanitize(str(error.get("Message") or ""), env)
    return {
        "code": error.get("Code"),
        "message": message,
        "httpStatus": metadata.get("HTTPStatusCode"),
    }


def _client_error_detail(exc: Exception, env: dict[str, str]) -> str:
    return _format_error_detail(_client_error_summary(exc, env))


def _format_error_detail(detail: dict[str, Any]) -> str:
    code = detail.get("code") or "Unknown"
    http_status = detail.get("httpStatus") or "unknown"
    message = detail.get("message") or "no message"
    return f"{code} (HTTP {http_status}): {message}"


def _is_delete_denied(detail: dict[str, Any]) -> bool:
    return detail.get("code") in {"AccessDenied", "InvalidRequest", "MethodNotAllowed"} or detail.get(
        "httpStatus"
    ) in {403, 405}


def _sanitize(message: str, env: dict[str, str]) -> str:
    sanitized = message
    for name in ("EDHR_S3_ACCESS_KEY", "EDHR_S3_SECRET_KEY"):
        value = env.get(name)
        if value:
            sanitized = sanitized.replace(value, "[REDACTED]")
    return sanitized


if __name__ == "__main__":
    sys.exit(main())
