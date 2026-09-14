# MES 工序菜单 E2E 独立复验报告

## Result

PASS

## Verified Behavior

- `芋道源码/admin` 通过官方登录前置。
- `MES 系统 > 生产管理` 下可见入口名称为 `MES工序`。
- 页面与可见菜单中不再显示 `标准模板列表`。
- 顶部搜索输入 `mes工序`，结果为 `MES工序/mes/pro/mes-process`。
- 点击搜索结果后进入 `/mes/pro/mes-process`。
- 资源接口返回 HTTP `200`、业务码 `0`、总数 `580`。
- 页面表格、分页和目标列正常渲染。
- 页面无 `系统异常`，MES 写请求 `0`，MES HTTP 失败 `0`，浏览器 page error `0`。

## Environment

- Frontend: `http://127.0.0.1:8081`, HTTP `200`
- Backend: `http://127.0.0.1:48081`, health `UP`
- Frontend runtime: PID `57460`, `E:\IntRuoyi\IntRuoyiFronted`
- Backend runtime: PID `37596`, `E:\IntRuoyi\output\runtime\int_main`
- Identity: `芋道源码/admin`
- Browser: local Google Chrome

## Commands

- 官方登录前置 `scripts/preflight/login-preflight.mjs` -> PASS
- `node --check output\playwright\20260730-mes-process-menu-e2e-reverify\mes-process-menu-real.e2e.mjs` -> PASS
- `node output\playwright\20260730-mes-process-menu-e2e-reverify\mes-process-menu-real.e2e.mjs` -> PASS

## Runtime Evidence

- Final URL: `http://127.0.0.1:8081/mes/pro/mes-process`
- Search query: `mes工序`
- Search result: `MES工序/mes/pro/mes-process`
- Resource API: `/admin-api/mes/pro/route-resource/page?pageNo=1&pageSize=20`
- Resource result: HTTP `200`, code `0`, total `580`
- MES write request count: `0`
- MES failed response count: `0`
- Page error count: `0`

## Visual Evidence

- 临时截图已人工检查：页面页签和面包屑显示 `MES工序`，资源列表与分页正常渲染。
- 截图和一次性 Playwright 脚本属于本任务临时产物，将在 closeout cleanup 中删除；关键结论已归档在本报告和 `execution-log.md`。

## Remaining Risk

- 本轮是当前 `int_main` 本机运行态的只读复验，没有执行写入型业务流程。

## Cleanup

- `task-closeout-cleanup` preview -> PASS
- `task-closeout-cleanup` apply -> PASS
- Keep: `task.md`, `execution-log.md`, `verification-report.md`
- Delete: `output/playwright/20260730-mes-process-menu-e2e-reverify/`
- Blocked: `0`
- Warnings: `0`
- Final task status: `completed`
