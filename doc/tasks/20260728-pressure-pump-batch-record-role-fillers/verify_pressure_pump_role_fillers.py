import argparse
import json
import pathlib
import subprocess
import sys
import urllib.parse
import urllib.request
from datetime import datetime


ROOT = pathlib.Path(__file__).resolve().parents[3]
TASK_DIR = pathlib.Path(__file__).resolve().parent
ARTIFACT_PATH = TASK_DIR / "pressure-pump-role-filler-verification.json"

BACKEND_URL = "http://127.0.0.1:48081"
TENANT_ID = 1
TENANT_NAME = "\u828b\u9053\u6e90\u7801"
LOGIN_ENV = ROOT / "IntRuoyiFronted" / ".env"
BATCH_RECORD_VERSION_ID = 130
BATCH_RECORD_NAME_HEX = "E79083E59B8AE689A9E5BCA0E58E8BE58A9BE6B3B5"
ROLE_NAME_SUFFIX = "\u586b\u5199\u8005\u89d2\u8272"
ROLE_CATEGORY_ID = 5
EXPECTED_REPORT_COUNT = 15
EXPECTED_USERS_PER_ROLE = 3


def mysql_query(sql):
    command = [
        "docker",
        "exec",
        "-i",
        "int-ruoyi-mysql",
        "sh",
        "-lc",
        'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -D ruoyi-vue-pro '
        "--default-character-set=utf8mb4 -N -B -r",
    ]
    completed = subprocess.run(
        command,
        input=sql,
        text=True,
        capture_output=True,
        encoding="utf-8",
    )
    if completed.returncode != 0:
        raise RuntimeError(completed.stderr.strip() or "mysql query failed")
    return completed.stdout


def read_env(path):
    values = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith("#") or "=" not in stripped:
            continue
        key, value = stripped.split("=", 1)
        values[key.strip()] = value.strip().strip("'").strip('"')
    return values


def request_json(method, path, headers=None, body=None):
    data = None
    request_headers = dict(headers or {})
    if body is not None:
        data = json.dumps(body, ensure_ascii=False).encode("utf-8")
        request_headers["Content-Type"] = "application/json"
    request = urllib.request.Request(
        f"{BACKEND_URL}/admin-api{path}",
        data=data,
        headers=request_headers,
        method=method,
    )
    with urllib.request.urlopen(request, timeout=60) as response:
        payload = response.read().decode("utf-8")
    result = json.loads(payload)
    code = int(result.get("code", -1))
    if code not in (0, 200):
        raise RuntimeError(f"API {method} {path} failed with code={result.get('code')} msg={result.get('msg')}")
    return result.get("data")


def login():
    env = read_env(LOGIN_ENV)
    tenant = env.get("VITE_APP_DEFAULT_LOGIN_TENANT")
    username = env.get("VITE_APP_DEFAULT_LOGIN_USERNAME")
    password = env.get("VITE_APP_DEFAULT_LOGIN_PASSWORD")
    if tenant != TENANT_NAME or username != "admin" or not password:
        raise RuntimeError("default local login source is missing or not the authorized tenant/admin account")
    tenant_data = request_json(
        "GET",
        "/system/tenant/get-id-by-name?name=" + urllib.parse.quote(tenant),
    )
    if int(tenant_data) != TENANT_ID:
        raise RuntimeError(f"tenant id mismatch: expected {TENANT_ID}, got {tenant_data}")
    token = request_json(
        "POST",
        "/system/auth/login",
        headers={"tenant-id": str(TENANT_ID), "isEncrypt": "false"},
        body={"username": username, "password": password},
    )
    access_token = token.get("accessToken") if isinstance(token, dict) else None
    if not access_token:
        raise RuntimeError("login succeeded without access token")
    return {
        "tenant": tenant,
        "username": username,
        "headers": {
            "Authorization": f"Bearer {access_token}",
            "tenant-id": str(TENANT_ID),
        },
    }


def role_name_for_title(title):
    base = title[:-4] if title.endswith("\u751f\u4ea7\u8bb0\u5f55") else title
    return f"{base}{ROLE_NAME_SUFFIX}"


def load_reports_and_roles():
    sql = f"""
SELECT
  r.source_table_index,
  r.report_id,
  r.report_code,
  r.table_title,
  COALESCE(p.candidate_source_type, ''),
  COALESCE(p.candidate_source_ids, ''),
  COALESCE(sr.id, 0),
  COALESCE(sr.name, ''),
  COALESCE(sr.code, ''),
  COALESCE(sr.category_id, 0),
  COALESCE(sr.status, -1),
  COUNT(DISTINCT ur.user_id) AS assigned_user_count
FROM mes_pro_batch_record_report r
LEFT JOIN mes_pro_edhr_process_form_permission_rule p
  ON p.tenant_id = r.tenant_id
 AND p.deleted = 0
 AND p.batch_record_report_id = r.report_id
 AND p.batch_record_version_id = r.batch_record_version_id
 AND p.rule_type = 'FILL'
 AND p.scope_key = 'ALL'
LEFT JOIN system_role sr
  ON sr.tenant_id = r.tenant_id
 AND sr.deleted = 0
 AND sr.id = CAST(p.candidate_source_ids AS UNSIGNED)
LEFT JOIN system_user_role ur
  ON ur.tenant_id = r.tenant_id
 AND ur.deleted = 0
 AND ur.role_id = sr.id
WHERE r.tenant_id = {TENANT_ID}
  AND r.deleted = 0
  AND r.batch_record_version_id = {BATCH_RECORD_VERSION_ID}
  AND r.form_slot_type = 'MAIN'
  AND r.batch_record_name = (CONVERT(UNHEX('{BATCH_RECORD_NAME_HEX}') USING utf8mb4) COLLATE utf8mb4_unicode_ci)
GROUP BY
  r.source_table_index,
  r.report_id,
  r.report_code,
  r.table_title,
  p.candidate_source_type,
  p.candidate_source_ids,
  sr.id,
  sr.name,
  sr.code,
  sr.category_id,
  sr.status
ORDER BY r.source_table_index;
"""
    reports = []
    for line in mysql_query(sql).splitlines():
        parts = line.split("\t")
        if len(parts) != 12:
            raise RuntimeError(f"unexpected report row shape: {line[:200]}")
        reports.append(
            {
                "index": int(parts[0]),
                "reportId": parts[1],
                "reportCode": parts[2],
                "title": parts[3],
                "candidateSourceType": parts[4],
                "candidateSourceIds": parts[5],
                "roleId": int(parts[6]),
                "roleName": parts[7],
                "roleCode": parts[8],
                "roleCategoryId": int(parts[9]),
                "roleStatus": int(parts[10]),
                "assignedUserCount": int(parts[11]),
            }
        )
    return reports


def load_role_users():
    sql = """
SELECT
  ur.role_id,
  u.id,
  u.username,
  u.nickname,
  u.status,
  BIN(u.deleted),
  CASE WHEN u.username = 'admin' THEN 1 ELSE 0 END
FROM system_user_role ur
JOIN system_users u
  ON u.tenant_id = ur.tenant_id
 AND u.id = ur.user_id
WHERE ur.tenant_id = 1
  AND ur.deleted = 0
  AND ur.role_id BETWEEN 910415 AND 910429
ORDER BY ur.role_id, u.id;
"""
    result = {}
    for line in mysql_query(sql).splitlines():
        parts = line.split("\t")
        if len(parts) != 7:
            raise RuntimeError(f"unexpected role user row shape: {line[:200]}")
        role_id = int(parts[0])
        result.setdefault(role_id, []).append(
            {
                "userId": int(parts[1]),
                "username": parts[2],
                "nickname": parts[3],
                "status": int(parts[4]),
                "deleted": int(parts[5]),
                "isAdmin": parts[6] == "1",
            }
        )
    return result


def verify_database(reports, role_users):
    failures = []
    if len(reports) != EXPECTED_REPORT_COUNT:
        failures.append(f"expected {EXPECTED_REPORT_COUNT} MAIN reports, got {len(reports)}")
    seen_role_ids = set()
    for report in reports:
        expected_role_name = role_name_for_title(report["title"])
        expected_role_code = f"pressure_pump_filler_{report['index']:02d}"
        if report["candidateSourceType"] != "ROLE":
            failures.append(f"{report['title']}: candidateSourceType={report['candidateSourceType']} expected ROLE")
        if str(report["roleId"]) != str(report["candidateSourceIds"]):
            failures.append(f"{report['title']}: candidateSourceIds does not match roleId")
        if report["roleName"] != expected_role_name:
            failures.append(f"{report['title']}: roleName={report['roleName']} expected {expected_role_name}")
        if report["roleCode"] != expected_role_code:
            failures.append(f"{report['title']}: roleCode={report['roleCode']} expected {expected_role_code}")
        if report["roleCategoryId"] != ROLE_CATEGORY_ID:
            failures.append(f"{report['title']}: roleCategoryId={report['roleCategoryId']} expected {ROLE_CATEGORY_ID}")
        if report["roleStatus"] != 0:
            failures.append(f"{report['title']}: role status is not enabled")
        users = role_users.get(report["roleId"], [])
        if len(users) != EXPECTED_USERS_PER_ROLE:
            failures.append(f"{report['title']}: role user count={len(users)} expected {EXPECTED_USERS_PER_ROLE}")
        for user in users:
            if user["status"] != 0 or user["deleted"] != 0 or user["isAdmin"]:
                failures.append(f"{report['title']}: invalid assigned user {user['username']}")
        if report["roleId"] in seen_role_ids:
            failures.append(f"{report['title']}: roleId reused across reports")
        seen_role_ids.add(report["roleId"])
    return failures


def verify_api(auth, reports, role_users):
    failures = []
    api_rows = []
    for report in reports:
        data = request_json(
            "GET",
            "/mes/pro/edhr-process-form-permission-rule/get-by-report?batchRecordReportId="
            + urllib.parse.quote(report["reportId"]),
            headers=auth["headers"],
        )
        fill_rule = data.get("fillRule") or {}
        candidate_source_names = fill_rule.get("candidateSourceNames") or []
        candidate_users = fill_rule.get("candidateUsers") or []
        expected_user_ids = sorted(user["userId"] for user in role_users.get(report["roleId"], []))
        actual_user_ids = sorted(int(user.get("userId")) for user in candidate_users if user.get("userId") is not None)
        if data.get("fillRuleStatus") != "CONFIGURED":
            failures.append(f"{report['title']}: API fillRuleStatus={data.get('fillRuleStatus')}")
        if fill_rule.get("candidateSourceType") != "ROLE":
            failures.append(f"{report['title']}: API candidateSourceType={fill_rule.get('candidateSourceType')}")
        if [int(item) for item in fill_rule.get("candidateSourceIds") or []] != [report["roleId"]]:
            failures.append(f"{report['title']}: API candidateSourceIds mismatch")
        if report["roleName"] not in candidate_source_names:
            failures.append(f"{report['title']}: API candidateSourceNames missing role name")
        if actual_user_ids != expected_user_ids:
            failures.append(f"{report['title']}: API candidateUsers {actual_user_ids} expected {expected_user_ids}")
        api_rows.append(
            {
                "index": report["index"],
                "title": report["title"],
                "reportId": report["reportId"],
                "reportCode": report["reportCode"],
                "roleId": report["roleId"],
                "roleName": report["roleName"],
                "candidateSourceNames": candidate_source_names,
                "candidateUserCount": len(candidate_users),
                "candidateUserIds": actual_user_ids,
            }
        )
    return failures, api_rows


def verify():
    reports = load_reports_and_roles()
    role_users = load_role_users()
    db_failures = verify_database(reports, role_users)
    auth = login()
    api_failures, api_rows = verify_api(auth, reports, role_users)
    failures = db_failures + api_failures
    artifact = {
        "status": "PASS" if not failures else "FAIL",
        "generatedAt": datetime.now().isoformat(timespec="seconds"),
        "backendUrl": BACKEND_URL,
        "tenant": auth["tenant"],
        "tenantId": TENANT_ID,
        "username": auth["username"],
        "batchRecordName": "\u7403\u56ca\u6269\u5f20\u538b\u529b\u6cf5",
        "batchRecordVersionId": BATCH_RECORD_VERSION_ID,
        "expectedReportCount": EXPECTED_REPORT_COUNT,
        "reportCount": len(reports),
        "expectedUsersPerRole": EXPECTED_USERS_PER_ROLE,
        "roleCount": len({report["roleId"] for report in reports if report["roleId"]}),
        "apiVerifiedCount": len(api_rows),
        "reports": [
            {
                **row,
                "assignedUsers": [
                    {
                        "userId": user["userId"],
                        "username": user["username"],
                        "nickname": user["nickname"],
                    }
                    for user in role_users.get(row["roleId"], [])
                ],
            }
            for row in api_rows
        ],
        "failures": failures,
    }
    ARTIFACT_PATH.write_text(json.dumps(artifact, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    if failures:
        print("VERIFY FAIL")
        for failure in failures:
            print(f"- {failure}")
        return 1
    print(
        "VERIFY PASS: "
        f"reports={artifact['reportCount']} roles={artifact['roleCount']} "
        f"usersPerRole={EXPECTED_USERS_PER_ROLE} apiVerified={artifact['apiVerifiedCount']} "
        f"artifact={ARTIFACT_PATH}"
    )
    return 0


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--verify", action="store_true")
    args = parser.parse_args()
    if not args.verify:
        raise SystemExit("choose --verify")
    raise SystemExit(verify())


if __name__ == "__main__":
    main()
