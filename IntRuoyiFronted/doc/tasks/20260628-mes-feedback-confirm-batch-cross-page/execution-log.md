# 执行日志：报工整批确认跨分页漏填修复

BDD: 锁定批次跨分页时整批确认仍覆盖全量记录 -> Given 当前导入批次包含超过一页的已归属真实工序草稿 / When 班组长在任意分页点击确认报工 / Then 前端必须按当前锁定批次拉取全量待归属记录并构建确认 payload，而不是只提交当前页。

BDD: 补齐最后一条后不再反向提示其他页漏填 -> Given 第一页已有 10 条真实工序草稿且第二页剩余 1 条缺字段 / When 用户补齐第二页最后 1 条后确认报工 / Then 系统不应再把第一页 10 条误判为漏填，而应提交当前锁定批次的全部真实工序草稿。

BDD: 其他订单行继续只跳过不阻断 -> Given 当前锁定批次同时存在真实工序草稿和其他订单行 / When 前端构建整批确认 payload / Then 仍只包含真实工序草稿，其他订单行继续被排除。

RED: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-feedback-import-confirm-batch-cross-page-static.spec.js` -> FAIL，确认报工逻辑只基于当前页 `importRecordList` 构建 payload，缺少跨分页草稿缓存和全量批次拉取能力。

CHANGE: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\mes\pro\feedback\index.vue` 新增按 `importRecordId` 维度的本地草稿缓存，在分页刷新和确认前先持久化当前页补填值，再按当前锁定批次 `importRecordIds` 以 `pageSize=-1` 拉取全量待归属记录并合并草稿后统一校验/提交；`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\api\mes\pro\feedback\index.ts` 补充待归属分页请求类型；新增静态契约 `tests/e2e/mes-feedback-import-confirm-batch-cross-page-static.spec.js` 防止回归。

GREEN: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-feedback-import-confirm-batch-cross-page-static.spec.js` -> PASS

GREEN: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-feedback-import-current-batch-static.spec.js` -> PASS

REGRESSION: `@'...vue/compiler-sfc parse...'@ | node -` -> PASS，`src/views/mes/pro/feedback/index.vue` 可被 `vue/compiler-sfc` 正常解析，修复了 `buildConfirmBatchPayload` 返回对象漏写 `rows:` 导致的 `Unexpected token, expected "," (415:4)` 编译错误。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260628-mes-feedback-confirm-batch-cross-page\frontend-feature-evidence.md` -> PASS

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260628-mes-feedback-confirm-batch-cross-page --mode preview` -> PASS

FOLLOW-UP: 用户在浏览器控制台反馈 `vue-router.js?v=611af47a:2594 TypeError: Failed to fetch dynamically imported module: http://localhost:8081/src/views/mes/pro/feedback/index.vue?t=1782637102780`。

FOLLOW-UP-CHECK: `Invoke-WebRequest http://localhost:8081/src/views/mes/pro/feedback/index.vue?t=1782637102780` -> PASS，当前 Vite 已返回 200，且内容为正常转换后的 JS 模块。

FOLLOW-UP-CHECK: `Invoke-WebRequest http://localhost:8081/src/api/mes/pro/feedback/index.ts?t=1782639752925`、`Invoke-WebRequest http://localhost:8081/src/views/system/user/components/UserSelectV2.vue?t=1782639752925` -> PASS，相关直接依赖均返回 200。

FOLLOW-UP-CONCLUSION: 当前本地 dev server 已能正常提供 `src/views/mes/pro/feedback/index.vue` 及其已检查依赖，本次用户截图更像是旧失败模块状态或先前版本的动态导入报错残留，而非当前文件仍存在语法错误。

FOLLOW-UP: 用户在确认报工时追加反馈后端报错 `Validation failed ... pageSize: rejected value [-1]`，说明整批拉取虽然覆盖了跨分页记录，但请求参数违反了后端 `PageParam` 最小值校验。

BDD: 整批拉取请求必须满足后端分页契约 -> Given 当前锁定批次已锁定 N 条导入记录 / When 前端为整批确认拉取全量待归属记录 / Then 请求必须使用大于等于 1 的 pageSize，并且 pageSize 至少覆盖当前锁定批次 importRecordIds 数量，不能再传 -1 触发后端校验失败。

RED: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-feedback-import-confirm-batch-cross-page-static.spec.js` -> FAIL，当前实现仍写死 `pageSize: -1`，新的静态契约已阻止该非法分页参数继续进入整批确认链路。

CHANGE: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\mes\pro\feedback\index.vue` 将整批拉取 `pageSize` 改为 `Math.max(currentImportRecordIds.value.length, 1)`，继续按当前锁定批次 `importRecordIds` 拉取全量记录，但不再违反后端分页参数最小值约束；`tests/e2e/mes-feedback-import-confirm-batch-cross-page-static.spec.js` 同步升级为“必须使用正数 pageSize、且禁止 `pageSize=-1`”的回归门禁。

GREEN: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-feedback-import-confirm-batch-cross-page-static.spec.js` -> PASS

GREEN: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-feedback-import-current-batch-static.spec.js` -> PASS
