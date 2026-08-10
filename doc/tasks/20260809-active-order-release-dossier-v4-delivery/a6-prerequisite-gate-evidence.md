# A6 Fixture And Real E2E Prerequisite Gate Evidence

## Status

`STRUCTURED_BLOCKED / PRECONDITION`

A6 未执行写入型真实 Playwright E2E，未生成正式 fixture manifest。已新增并实际执行可执行 preflight：它在任何运行态请求、数据库访问、浏览器导航或业务写入前因 27 个显式授权/fixture/五角色凭据环境变量缺失而以 Node exit `2` 输出无秘密 `BLOCKED` JSON。M0 5.2 要求的逐工序三类传统 `batchRecordReportId` 前置在当前本机数据库中也已由先前只读证据证明不存在；按 fail-fast 合同停止，不以动态表单、静态测试、API-only、SQL 造数或空 manifest 降级。

## Scope

- 任务：V4 M0 A6 正式 fixture manifest 与真实 Playwright E2E。
- 计划前缀：`AORD-V4-M0-A6-20260809-`。
- 工作区：`E:\IntRuoyi`，分支 `int_main`。
- 只读门禁：运行态、工具链、测试账号/签名行、路线/版本/工序、传统报表绑定、QA 规程和现有放行申请。
- 未授权动作：直接 SQL 写入/清理、mock、默认值、`formBindings` 替代、停止或替换共享进程、修改 A1-A5 产品代码。

## Requirement Matrix

| Requirement | Evidence | Result |
| --- | --- | --- |
| int_main 前后端基准可达 | 8081 Vite 进程路径属于 `E:\IntRuoyi\IntRuoyiFronted`；首页 HTTP 200；48081 Java 进程参数属于 `E:\IntRuoyi`；`/actuator/health` 为 `UP` | PASS |
| Playwright 工具链 | Node `v24.12.0`、npx `11.6.2`、仓库 Playwright `1.60.0`；Chrome、Edge、`playwright.config.ts` 和 `test:e2e` 均存在 | PASS |
| 五类业务账号可真实登录并可签名 | tenant `1` 六个历史 M0 账号及授权/活动签名图片行存在；当前 shell 没有 A6/FRONTLINE/PQC/RELEASE 专用凭据，历史文档明确不保存共享密码和签名口令 | BLOCKED: credentials not proven |
| 任务自有产品/工单/活跃订单 | 当前未创建；必须在正式 UI 链路中创建并用 A6 前缀追踪 | NOT RUN after earlier blocker |
| 发布路线/版本/工序 | tenant `1` 路线 `922119` 当前发布版本 `627/V27` 存在，14 个当前工序存在 | READ-ONLY PASS, not task-owned |
| 三类传统报表绑定 | 全库 `MAIN/BATCH_RECORD` 非空 report ID；`PROCESS_INSPECTION` 与 `LOSS_REPORT` 的 `batch_record_report_id` 均为空，无任一工序同时具有三类非空传统 report ID | BLOCKED |
| 发布 QA 版本/items/equipment | 历史 V21 有 14 个 PUBLISHED 版本；当前 V27 只读结果不足以证明全部 A6 目标工序的 published items/equipment | NOT RUN after earlier blocker |
| PQC/PRODUCTION_LOSS 映射 | 未在任务自有发布路线与三类传统报表上证明 | NOT RUN after earlier blocker |
| RELEASE_APPROVE 候选 | 未在任务自有发布路线版本上证明 | NOT RUN after earlier blocker |
| 可清理任务 fixture | 未产生 A6 写入，因此没有可清理 A6 ID | PASS: none created |

## Executable Preflight Contract

- Gate: `E:\IntRuoyi\IntRuoyiFronted\tests\e2e\active-order-release-dossier-v4-preflight.cjs`。
- Static contract: `E:\IntRuoyi\IntRuoyiFronted\tests\e2e\active-order-release-dossier-v4-preflight-static.spec.cjs`。
- Actual blocker result: `E:\IntRuoyi\doc\tasks\20260809-active-order-release-dossier-v4-delivery\a6-preflight-blocked.json`。
- 环境门禁要求 27 个显式变量：测试租户授权标记、tenant、frontend `8081`/backend `48081`、browser/database、产品/路线/发布版本/工序，以及五角色各自用户名、登录密码、签名口令。门禁不读取 `.env`，不含默认账号/密码/历史授权标记；两个 URL 通过只读 HTTP GET 核验 int_main 首页和 backend `UP` 后才访问数据库。
- 五角色登录密码和签名口令只要求显式非空，不自设 M0 未冻结的长度限制；登录有效性由后续五角色真实 UI 登录证明，签名有效性仍须由真实业务 E2E 签名路径证明。
- 电子签名 `BIT(1)` 在 SQL 中先 `CAST(COALESCE(..., 0) AS UNSIGNED)`，Node 再按数值 `1` 核验，避免 MySQL CLI 二进制表现差异。
- 环境完整后，门禁仅用 `SHOW`/`SELECT` 依次验证正式路线身份、五角色账号及有效签名、每个工序三类传统 report/definition/APPROVED version、PUBLISHED QA/items/equipment、三类 source mapping 和唯一 `RELEASE_APPROVE` 候选；源码和通过条件均排除 `formBindings` 与 `form_template_id`。
- 上述数据库门禁全部通过后才加载 Playwright。五角色监听页面 console/pageerror/requestfailed/目标错误响应，在首次导航前安装；浏览器网络 guard 仅允许认证登录 POST 和 GET/HEAD/OPTIONS，任何业务写请求会被阻止并使 preflight 阻塞。
- `BLOCKED`/`ERROR` 结果在写入 JSON 前对 token、登录密码、签名口令做值级脱敏并执行无泄漏断言；结果只含变量名和定位信息，不含秘密值。

## Reproducible Read-Only Evidence

数据库命令使用容器内已有凭据变量且不输出凭据；所有 SQL 均为 `SHOW`/`SELECT`。

```text
docker ps --format "{{.Names}}|{{.Image}}|{{.Status}}"
-> int-ruoyi-mysql mysql:8.0.39 Up

SELECT tenant_id, form_slot_type, record_category,
       COUNT(*),
       SUM(batch_record_report_id IS NOT NULL AND batch_record_report_id <> '')
FROM mes_pro_route_flow_process_batch_record
WHERE deleted = 0
GROUP BY tenant_id, form_slot_type, record_category;

-> tenant 1: MAIN/BATCH_RECORD 1224/1224 non-empty
-> tenant 1: PROCESS_INSPECTION/INTERNAL_RECORD 176/0 non-empty
-> tenant 1: LOSS_REPORT/INTERNAL_RECORD 352/0 non-empty
-> tenant 122: MAIN/BATCH_RECORD 15/15 non-empty
-> tenant 122: PROCESS_INSPECTION/INTERNAL_RECORD 2/0 non-empty
-> tenant 122: LOSS_REPORT/INTERNAL_RECORD 4/0 non-empty

SELECT tenant_id, route_id, route_process_id,
       COUNT(DISTINCT CASE
         WHEN form_slot_type IN ('MAIN','PROCESS_INSPECTION','LOSS_REPORT')
          AND batch_record_report_id IS NOT NULL
          AND batch_record_report_id <> ''
         THEN form_slot_type END) AS types
FROM mes_pro_route_flow_process_batch_record
WHERE deleted = 0
GROUP BY tenant_id, route_id, route_process_id
HAVING types = 3;

-> 0 rows
```

路线 `922119` 的现有 `PROCESS_INSPECTION`/`LOSS_REPORT` 行仅有 `form_slot_type` 和 `form_template_id`，`batch_record_report_id` 为空。根据项目术语合同，这属于动态表单槽位数据，不能替代逐工序传统批记录报表来源。

## Test Execution

| Test type | Tests | Passed | Failed | Blocked | Notes |
| --- | ---: | ---: | ---: | ---: | --- |
| Runtime/tool prerequisite | 2 | 2 | 0 | 0 | 前后端与 Playwright 工具链 |
| Account/signature prerequisite | 1 | 0 | 0 | 1 | 数据行存在，但业务登录凭据/签名口令未证明 |
| Formal source prerequisite | 1 | 0 | 0 | 1 | 全库缺三类传统 report ID 同工序绑定 |
| Executable preflight static contract | 1 | 1 | 0 | 0 | 27 项显式 env、前后端 base URL、正式来源、只读顺序、无秘密 BLOCKED 合同 |
| Actual executable preflight | 1 | 0 | 0 | 1 | exit 2；27 个显式 env 缺失；运行态请求/数据库/浏览器/业务写均未开始 |
| Real Playwright business E2E | 0 | 0 | 0 | 0 | 按 fail-fast 未启动 |
| Final API/DB transaction verification | 0 | 0 | 0 | 0 | 没有业务写入可核验 |

这不是业务 RED 或产品测试失败；属于测试数据/凭据前置阻塞。未用静态合同冒充真实 E2E GREEN。

## Artifacts And Side Effects

- Formal fixture manifest: 未创建。
- Playwright business-flow spec: 未创建；在正向前置不满足时不提交不可执行或占位 business E2E spec。已创建的 preflight 仅做真实 E2E 前置验证，不能冒充业务链路通过。
- Executable preflight result: `a6-preflight-blocked.json`，schema `AORD_V4_M0_A6_PREFLIGHT_V1`，`status=BLOCKED`，`canRunRealE2E=false`，27 个缺失环境变量名按 Node 字典序稳定输出。
- Screenshots/traces/videos: 0。
- Production submit/review/PQC/release IDs: 0。
- Console errors: 未采集，真实页面业务路径未启动。
- Page errors: 未采集，真实页面业务路径未启动。
- Target business network errors: 未采集，真实页面业务路径未启动。
- Backend: health `UP`；未调用业务写接口，未观察业务异常。
- Cleanup: 无 A6 业务写入，无 A6 任务数据残留；未执行 UI/SQL 清理。

## Verification

```text
RED: node tests\e2e\active-order-release-dossier-v4-preflight-static.spec.cjs
-> FAIL exit 1: missing executable A6 V4 preflight gate

GREEN: node --check tests\e2e\active-order-release-dossier-v4-preflight.cjs
-> PASS

GREEN: node --check tests\e2e\active-order-release-dossier-v4-preflight-static.spec.cjs
-> PASS

GREEN: node tests\e2e\active-order-release-dossier-v4-preflight-static.spec.cjs
-> PASS: active-order release dossier V4 executable preflight static contract

PREFLIGHT: node tests\e2e\active-order-release-dossier-v4-preflight.cjs --result-path ..\doc\tasks\20260809-active-order-release-dossier-v4-delivery\a6-preflight-blocked.json
-> BLOCKED, Node exit 2, MISSING_EXPLICIT_ENV, missingEnvKeys=27
-> browserBusinessWrites=0, businessApiWrites=0, sqlWrites=0, manifestCreated=false
```

主审返修 RED/GREEN：

```text
Review RED: node tests\e2e\active-order-release-dossier-v4-preflight-static.spec.cjs
-> FAIL exit 1: preflight must require AORD_V4_M0_BACKEND_URL

Review GREEN: node --check tests\e2e\active-order-release-dossier-v4-preflight.cjs
-> PASS

Review GREEN: node --check tests\e2e\active-order-release-dossier-v4-preflight-static.spec.cjs
-> PASS

Review GREEN: node tests\e2e\active-order-release-dossier-v4-preflight-static.spec.cjs
-> PASS: backend URL/health order, BIT unsigned cast, non-empty-only secrets and hyphenated report ID contract
```

## Blockers

在正式 UI 中为任务自有发布路线的目标工序保存非空且准确的三类传统 `batchRecordReportId`，并同时证明 report/definition/version、PQC/`PRODUCTION_LOSS` 映射、发布 QA items/equipment、非空 `RELEASE_APPROVE` 候选。另需向 A6 运行环境安全注入已确认测试租户五类账号的登录凭据和签名口令，不得写入仓库或证据文件。

上述前置齐全后，A6 才能从真实页面执行生产提交/确认、PQC 提交/复核、自然双 100、申请放行、三资料审计查看、负责人终态、同快照幂等、缺来源负向路径与 UI 清理，并生成 M0 5.1 manifest。

## Authorized Rerun Result

用户随后授权本机 int_main 的 `芋道源码` tenant 和精确名称“球囊扩张导管”路线。只读重跑确认 `tenantId=1`、`routeId=900025`、ACTIVE `routeVersionId=271/V9`、23 个 `routeProcessIds=926785..926807`。最高 ID `272/V10` 是 DRAFT，未误作发布版。

首次业务写入前仍存在不可绕过 blocker：路线有四个启用产品 `[902231,902252,902262,907242]` 而非唯一 product；三类传统绑定总行数为 0；唯一 LOSS_REPORT 缺 definition/version；ACTIVE V9 快照缺 `batchRecordAttachmentOwners` 数组，因此不满足真实页面复制任务路线的来源门禁；目标路线 QA 总数为 0；三类必需 mapping 总数为 0；路线级 `RELEASE_APPROVE` 总数为 0。正式生产组长与 PQC 组长角色用户还缺有效签名，signed super_admin 不能自动替代业务角色。

本轮未进入真实 UI 配置或业务写入，业务写请求、SQL 写入、manifest 和任务残留均为 0。账号真实 UI 登录也未完成：executor 环境没有秘密环境变量，且上游数据门禁已先阻塞；不能把数据库账号/签名行当作凭据可用证据。
