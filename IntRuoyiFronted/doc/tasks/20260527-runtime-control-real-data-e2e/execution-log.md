# Frontend Execution Log：运行控制台真实数据 E2E

BDD: 前端必须提供每项功能真实 E2E -> Given 十项能力已有页面组件, When 执行运行控制台验收, Then AC-01 到 AC-11 都必须由 Playwright 登录真实租户并断言页面真实数据。

RED: `node tests\e2e\runtime-control-real-data-all-features.e2e.js` -> EXPECTED FAIL, expected reason: 脚本尚未创建。

RED: explicit env command -> FAIL, actual reason: `MODULE_NOT_FOUND` for `tests\e2e\runtime-control-real-data-all-features.e2e.js`.

GREEN: `node --check tests\e2e\runtime-control-real-data-all-features.e2e.js` -> PASS.

REGRESSION: `node tests\e2e\runtime-control-foolproof-static.spec.js` -> PASS.

BDD: AC-01 到 AC-11 前端真实路径 -> Given 测试租户登录运行控制台, When Playwright 逐项操作告警、责任矩阵、决策向导、候选、巡检、健康、探针、容量、备份和事故闭环, Then 每项必须等待真实后端响应并断言页面可见状态。

BDD: 芋道源码/admin 复核不得写运行控制台 -> Given 测试租户功能路径已通过, When 使用芋道源码/admin 打开运行控制台复核, Then 只能验证页面和 GET 接口可用，不得调用 `/admin-api/infra/runtime-control/*` 写请求。

RED: `node tests\e2e\runtime-control-real-data-all-features.e2e.js` -> FAIL, actual reason: AC-09 缺少 `RUNTIME_OPS_ALERT` 模板导致 `/capacity/status` 业务码 500。

RED: `node tests\e2e\runtime-control-real-data-all-features.e2e.js` -> FAIL, actual reason: AC-11 `新建事故` 文案匹配过宽；修正为按钮定位。

GREEN: `node tests\e2e\runtime-control-real-data-all-features.e2e.js` -> PASS, evidence: `AC-01 PASS alerts=20`, `AC-02 PASS ownerRows=3`, `AC-03 PASS scenarios=6`, `AC-04 PASS rollbackCandidates=1`, `AC-05 PASS restoreCandidates=1`, `AC-06 PASS checks=4`, `AC-07 PASS items=7`, `AC-08 PASS probes=9`, `AC-09 PASS status=WARN`, `AC-10 PASS backupPoints=1`, `AC-11 PASS incident=E2E事故-1779862919956`, `TEST_TENANT_PASS`, `YUDAO_ADMIN_VERIFY_PASS`。

GREEN: final `node tests\e2e\runtime-control-real-data-all-features.e2e.js` after Docker dependency recovery -> PASS, evidence: `AC-01 PASS alerts=20`, `AC-02 PASS ownerRows=3`, `AC-03 PASS scenarios=6`, `AC-04 PASS rollbackCandidates=1`, `AC-05 PASS restoreCandidates=1`, `AC-06 PASS checks=4`, `AC-07 PASS items=7`, `AC-08 PASS probes=9`, `AC-09 PASS status=WARN`, `AC-10 PASS backupPoints=1`, `AC-11 PASS incident=E2E事故-1779866553908`, `TEST_TENANT_PASS`, `YUDAO_ADMIN_VERIFY_PASS`。

GREEN: `node tests\e2e\runtime-control-foolproof-static.spec.js` -> PASS.

GREEN: `node --check tests\e2e\runtime-control-real-data-all-features.e2e.js` -> PASS.

GREEN: `node --check tests\e2e\runtime-control-rollback-app.e2e.js; node --check tests\e2e\runtime-control-restore-data.e2e.js` -> PASS.

## 2026-05-27 int_main 融合后复验

GREEN: frontend int_main `node --check tests\e2e\runtime-control-real-data-all-features.e2e.js; node tests\e2e\runtime-control-foolproof-static.spec.js; node --check tests\e2e\runtime-control-rollback-app.e2e.js; node --check tests\e2e\runtime-control-restore-data.e2e.js` -> PASS.

RED: frontend int_main `node tests\e2e\runtime-control-real-data-all-features.e2e.js` -> FAIL, actual reason: 主前端工作区合并后尚未安装新增 `playwright` 依赖，Node 抛出 `Cannot find module 'playwright'`。

GREEN: frontend int_main `pnpm install --frozen-lockfile` -> PASS，lockfile 未变更，安装 `playwright 1.60.0`。

GREEN: int_main `node tests\e2e\runtime-control-real-data-all-features.e2e.js` -> PASS, evidence: `SETUP ownerMatrix created=3`, `SETUP alert created=1`, `AC-01 PASS alerts=1`, `AC-02 PASS ownerRows=3`, `AC-03 PASS scenarios=6`, `AC-04 PASS rollbackCandidates=1`, `AC-05 PASS restoreCandidates=1`, `AC-06 PASS checks=4`, `AC-07 PASS items=7`, `AC-08 PASS probes=9`, `AC-09 PASS status=BLOCKED`, `AC-10 PASS backupPoints=1`, `AC-11 PASS incident=E2E事故-1779871222493`, `TEST_TENANT_PASS`, `YUDAO_ADMIN_VERIFY_PASS`。

## 2026-05-27 芋道源码/admin 专用只读复验

BDD: 芋道源码/admin 专用只读复验 -> Given 当前前后端均位于 `int_main` 且后端由主工作区 jar 在 48098 启动, When 使用 `芋道源码/admin` 登录 8098 运行控制台, Then 必须逐项验证 AC-01 到 AC-11 页面入口和真实 GET 响应，且复验阶段不得调用 `/admin-api/infra/runtime-control/*` 非 GET 请求。

RED: `node tests\e2e\runtime-control-yudao-admin-readonly.e2e.js` -> EXPECTED FAIL, expected reason: 专用只读复验脚本尚未创建。

GREEN: `node --check tests\e2e\runtime-control-yudao-admin-readonly.e2e.js` -> PASS。

RED: Vite dev `node tests\e2e\runtime-control-yudao-admin-readonly.e2e.js` -> FAIL, actual reason: 本地 Vite dev 依赖优化触发 `EMFILE: too many open files` / `504 Outdated Optimize Dep`，登录页只渲染标题且缺少可见登录输入，不能作为 E2E 入口。

GREEN: `pnpm build:local` -> PASS，使用当前 `int_main` 前端构建 `dist`，并用 `vite preview --host 127.0.0.1 --port 8098 --strictPort` 提供稳定前端入口。

RED: static preview `node tests\e2e\runtime-control-yudao-admin-readonly.e2e.js` -> FAIL, actual reason: `/admin-api/infra/runtime-control/overview` 在当前真实状态查询中约 41 秒完成，脚本原 30 秒等待过短。

RED: static preview `node tests\e2e\runtime-control-yudao-admin-readonly.e2e.js` -> FAIL, actual reason: AC-03 断言了占位文案 `选择场景`，而真实 UI 加载后显示已选场景；修正为断言 `.wizard-select` 与 `计算推荐` 按钮。

GREEN: static preview `node tests\e2e\runtime-control-yudao-admin-readonly.e2e.js` -> PASS, evidence: `AC-01 ADMIN_READONLY_PASS alerts=0`, `AC-02 ADMIN_READONLY_PASS ownerRows=0`, `AC-03 ADMIN_READONLY_PASS scenarios=6`, `AC-04 ADMIN_READONLY_PASS rollbackCandidates=1`, `AC-05 ADMIN_READONLY_PASS restoreCandidates=1`, `AC-06 ADMIN_READONLY_PASS inspectionEntry=visible`, `AC-07 ADMIN_READONLY_PASS items=7`, `AC-08 ADMIN_READONLY_PASS probes=1`, `AC-09 ADMIN_READONLY_PASS status=BLOCKED`, `AC-10 ADMIN_READONLY_PASS backupPoints=1`, `AC-11 ADMIN_READONLY_PASS incidents=0`, `YUDAO_ADMIN_READONLY_PASS`, `PASS: yudao/admin readonly runtime-control E2E covers AC-01 through AC-11`。

STATUS: `芋道源码/admin` 专用只读复验通过；未创建告警、责任人、巡检、探针或事故数据；脚本断言运行控制台路径没有非 GET 请求。风险记录：`/overview` 真实状态聚合当前约 41 秒才返回，功能可用但响应偏慢，后续若要求运维页秒级可用应单独优化状态查询超时与并发策略。
