#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import os
import sys
import urllib.parse
import urllib.request
from dataclasses import dataclass
from typing import Any


REQUIRED_ENV = (
    "PRODUCTION_PLAN_ERP_K3CLOUD_BASE_URL",
    "PRODUCTION_PLAN_ERP_K3CLOUD_ACCT_ID",
    "PRODUCTION_PLAN_ERP_K3CLOUD_PASSWORD",
)


@dataclass(frozen=True)
class ProbeObject:
    form_id: str
    field_keys: tuple[str, ...]
    source_key_fields: tuple[str, ...]
    status_fields: tuple[str, ...]
    sample_filter: str
    order_string: str


PROBE_OBJECTS: tuple[ProbeObject, ...] = (
    ProbeObject(
        form_id="BD_MATERIAL",
        field_keys=("FID", "FNumber", "FModifyDate", "FForbidStatus", "FDocumentStatus"),
        source_key_fields=("FID", "FNumber"),
        status_fields=("FForbidStatus", "FDocumentStatus"),
        sample_filter="FNumber <> ''",
        order_string="FModifyDate DESC,FID DESC",
    ),
    ProbeObject(
        form_id="STK_Inventory",
        field_keys=(
            "FID",
            "FMATERIALID.FNumber",
            "FStockOrgId.FNumber",
            "FStockId.FNumber",
            "FLOT.FNumber",
            "FBaseQty",
            "FModifyDate",
        ),
        source_key_fields=("FStockOrgId.FNumber", "FStockId.FNumber", "FMATERIALID.FNumber", "FLOT.FNumber"),
        status_fields=("FBaseQty",),
        sample_filter="FMATERIALID.FNumber <> ''",
        order_string="FModifyDate DESC,FID DESC",
    ),
    ProbeObject(
        form_id="PUR_PurchaseOrder",
        field_keys=("FID", "FBillNo", "FModifyDate", "FDocumentStatus", "FCloseStatus", "FCancelStatus"),
        source_key_fields=("FID", "FBillNo"),
        status_fields=("FDocumentStatus", "FCloseStatus", "FCancelStatus"),
        sample_filter="FBillNo <> ''",
        order_string="FModifyDate DESC,FID DESC",
    ),
    ProbeObject(
        form_id="SAL_SaleOrder",
        field_keys=("FID", "FBillNo", "FModifyDate", "FDocumentStatus", "FCloseStatus", "FCancelStatus"),
        source_key_fields=("FID", "FBillNo"),
        status_fields=("FDocumentStatus", "FCloseStatus", "FCancelStatus"),
        sample_filter="FBillNo <> ''",
        order_string="FModifyDate DESC,FID DESC",
    ),
    ProbeObject(
        form_id="PRD_MO",
        field_keys=("FID", "FBillNo", "FModifyDate", "FDocumentStatus", "FStatus"),
        source_key_fields=("FID", "FBillNo"),
        status_fields=("FDocumentStatus", "FStatus"),
        sample_filter="FBillNo <> ''",
        order_string="FModifyDate DESC,FID DESC",
    ),
    ProbeObject(
        form_id="ENG_BOM",
        field_keys=("FID", "FNumber", "FModifyDate", "FDocumentStatus", "FMATERIALID.FNumber"),
        source_key_fields=("FID", "FNumber", "FMATERIALID.FNumber"),
        status_fields=("FDocumentStatus",),
        sample_filter="FMATERIALID.FNumber <> ''",
        order_string="FModifyDate DESC,FID DESC",
    ),
)


def build_plan() -> dict[str, Any]:
    return {
        "purpose": "Kingdee incremental sync read-only field probe",
        "required_env": list(REQUIRED_ENV),
        "objects": [
            {
                "form_id": item.form_id,
                "field_keys": list(item.field_keys),
                "source_key_fields": list(item.source_key_fields),
                "status_fields": list(item.status_fields),
                "sample_filter": item.sample_filter,
                "order_string": item.order_string,
            }
            for item in PROBE_OBJECTS
        ],
    }


def require_env() -> dict[str, str]:
    values = {name: os.environ.get(name, "").strip() for name in REQUIRED_ENV}
    missing = [name for name, value in values.items() if not value]
    if missing:
        print(
            "missing required environment variables: " + ", ".join(missing),
            file=sys.stderr,
        )
        raise SystemExit(2)
    values["PRODUCTION_PLAN_ERP_K3CLOUD_USERNAME"] = os.environ.get(
        "PRODUCTION_PLAN_ERP_K3CLOUD_USERNAME",
        "贾泽宇",
    )
    values["PRODUCTION_PLAN_ERP_K3CLOUD_LCID"] = os.environ.get(
        "PRODUCTION_PLAN_ERP_K3CLOUD_LCID",
        "2052",
    )
    return values


def service_url(base_url: str, service_name: str) -> str:
    normalized = base_url.rstrip("/")
    if not normalized.lower().endswith("/k3cloud"):
        normalized += "/K3Cloud"
    return normalized + "/" + service_name


def post_form(url: str, data: dict[str, str], cookie: str | None = None, timeout: int = 30) -> tuple[str, dict[str, str]]:
    encoded = urllib.parse.urlencode(data).encode("utf-8")
    request = urllib.request.Request(url, data=encoded, method="POST")
    request.add_header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
    if cookie:
        request.add_header("Cookie", cookie)
    with urllib.request.urlopen(request, timeout=timeout) as response:
        body = response.read().decode("utf-8")
        headers = {key.lower(): value for key, value in response.headers.items()}
    return body, headers


def login(config: dict[str, str]) -> str:
    body, headers = post_form(
        service_url(
            config["PRODUCTION_PLAN_ERP_K3CLOUD_BASE_URL"],
            "Kingdee.BOS.WebApi.ServicesStub.AuthService.ValidateUser.common.kdsvc",
        ),
        {
            "acctID": config["PRODUCTION_PLAN_ERP_K3CLOUD_ACCT_ID"],
            "username": config["PRODUCTION_PLAN_ERP_K3CLOUD_USERNAME"],
            "password": config["PRODUCTION_PLAN_ERP_K3CLOUD_PASSWORD"],
            "lcid": config["PRODUCTION_PLAN_ERP_K3CLOUD_LCID"],
        },
    )
    payload = json.loads(body)
    if not (payload.get("LoginResultType") == 1 or payload.get("IsSuccessByAPI") is True):
        raise RuntimeError("Kingdee login failed; see account configuration and permissions.")
    raw_cookie = headers.get("set-cookie", "")
    cookies = []
    for part in raw_cookie.split(","):
        cookie = part.split(";", 1)[0].strip()
        if "=" in cookie:
            cookies.append(cookie)
    if not cookies:
        raise RuntimeError("Kingdee login response missing Set-Cookie.")
    return "; ".join(cookies)


def probe_object(config: dict[str, str], cookie: str, item: ProbeObject) -> dict[str, Any]:
    query = {
        "FormId": item.form_id,
        "FieldKeys": ",".join(item.field_keys),
        "FilterString": item.sample_filter,
        "OrderString": item.order_string,
        "StartRow": 0,
        "Limit": 1,
    }
    body, _ = post_form(
        service_url(
            config["PRODUCTION_PLAN_ERP_K3CLOUD_BASE_URL"],
            "Kingdee.BOS.WebApi.ServicesStub.DynamicFormService.ExecuteBillQuery.common.kdsvc",
        ),
        {"data": json.dumps(query, ensure_ascii=False)},
        cookie,
    )
    payload = json.loads(body)
    if isinstance(payload, dict):
        return {
            "form_id": item.form_id,
            "ok": False,
            "error": payload,
            "field_keys": list(item.field_keys),
        }
    if not isinstance(payload, list):
        return {
            "form_id": item.form_id,
            "ok": False,
            "error": "unexpected response type",
            "field_keys": list(item.field_keys),
        }
    return {
        "form_id": item.form_id,
        "ok": True,
        "sample_count": len(payload),
        "field_keys": list(item.field_keys),
        "source_key_fields": list(item.source_key_fields),
        "status_fields": list(item.status_fields),
    }


def run_probe() -> int:
    config = require_env()
    cookie = login(config)
    results = [probe_object(config, cookie, item) for item in PROBE_OBJECTS]
    ok = all(item["ok"] for item in results)
    print(json.dumps({"ok": ok, "results": results}, ensure_ascii=False, indent=2))
    return 0 if ok else 1


def main() -> int:
    parser = argparse.ArgumentParser(description="Probe Kingdee incremental sync fields without mutating ERP data.")
    parser.add_argument("--print-plan", action="store_true", help="Print planned FormId/field checks without network access.")
    args = parser.parse_args()
    if args.print_plan:
        print(json.dumps(build_plan(), ensure_ascii=False, indent=2))
        return 0
    try:
        return run_probe()
    except SystemExit:
        raise
    except Exception as exc:
        print(f"kingdee incremental field probe failed: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
