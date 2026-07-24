from __future__ import annotations

from typing import Any


def success_result(**fields: Any) -> dict[str, Any]:
    return {"success": True, **fields}


def error_result(error_code: str, message: str, **fields: Any) -> dict[str, Any]:
    return {"success": False, "errorCode": error_code, "message": message, **fields}

