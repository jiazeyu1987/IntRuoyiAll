from __future__ import annotations

from playwright.sync_api import sync_playwright

from script.e2e.dcc_screenshot_e2e_helpers import (
    APPLICANT,
    FRONTEND_BASE_URL,
    assert_no_unexpected_browser_errors,
    assert_services_ready,
    collect_api_errors,
    collect_console_errors,
    ensure_e2e_baseline,
    login,
)


def run_dcc_navigation_smoke() -> None:
    assert_services_ready()
    ensure_e2e_baseline()

    pages = [
        ("/dcc/controlled-file/upload", ["文件上传", "文件类别", "提交审批"]),
        ("/dcc/controlled-file/browser", ["受控浏览", "文件类别"]),
        ("/dcc/controlled-file/browser", ["受控浏览"]),
        ("/dcc/controlled-file/approval-tasks", ["DCC审批任务", "任务名称"]),
        ("/dcc/controlled-file/distribution", ["文件分发规则", "文件类别"]),
        ("/dcc/controlled-file/training", ["文件培训规则", "文件类别"]),
        ("/dcc/controlled-file/signatures", ["DCC电子签名管理", "签名人"]),
    ]

    with sync_playwright() as playwright:
        browser = playwright.chromium.launch(headless=True)
        page = browser.new_page(viewport={"width": 1440, "height": 960})
        api_errors = collect_api_errors(page)
        console_errors = collect_console_errors(page)

        login(page, APPLICANT, "/dcc/controlled-file/upload")
        for path, markers in pages:
            page.goto(f"{FRONTEND_BASE_URL}{path}", wait_until="networkidle")
            page.wait_for_timeout(1000)
            body = page.locator("body").inner_text(timeout=10000)
            if "403" in body or "无权限" in body or "Access Denied" in body:
                raise AssertionError(f"DCC page denied access: {path}\n{body[:1000]}")
            for marker in markers:
                if marker not in body:
                    raise AssertionError(f"missing marker {marker!r} on {path}\n{body[:1000]}")

        assert_no_unexpected_browser_errors(api_errors, console_errors)
        browser.close()
