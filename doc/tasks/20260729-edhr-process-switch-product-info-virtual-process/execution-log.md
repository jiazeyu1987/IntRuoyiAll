# Execution Log

## User Intent

- 用户反馈“切换工序”缺少独立产品信息工序，产品信息填写人出现在粗洗工序中，并要求修复。

## BDD

- BDD: 切换工序独立展示产品信息 -> Given 当前批次任务中的产品信息保留粗洗工序来源 `routeProcessId` 且 `batchRecordSort=80`, When 用户打开填写页“切换工序”, Then 弹窗必须显示独立“产品信息”卡片，粗洗卡片不得包含产品信息任务。
- BDD: 产品信息填写人按虚拟工序隔离 -> Given 产品信息任务和粗洗任务复用来源 `routeProcessId`, When 用户分别在产品信息或粗洗填写页打开“切换填写人”, Then 候选范围必须只来自当前页面所属虚拟工序任务，不得跨组混入。

## Investigation

- 根因已定位：`ExecutionPage.vue` 的工序分组键只使用 `routeProcessId || routeProcessSort`；产品信息任务保留粗洗来源 `routeProcessId`，因此被合并。
- 同一页面的填写人范围也只按 `routeProcessId` 过滤，导致产品信息填写人归入粗洗工序。
- `BatchExecutionDetailPage.vue` 已存在产品信息虚拟 `80` 工序识别规则，本任务需要让填写页遵循同一业务口径。
- 已读取 `docs/experience-index.md`，命中产品信息虚拟 80 工序、辅助模式工序切换、Route Query ID 比较、切换填写人 FormCenter 槽位和 Windows 静态合同门禁，并同步到 `task.md`。

## Git Baseline

- 当前仓库：`E:\IntRuoyi`，分支 `int_main`，存在可用 `origin`。
- 任务开始时工作区无未提交文件；本地分支领先 `origin/int_main` 12 个提交，不需要创建脏工作区基线提交。
- 并发任务在本任务实现尚未单独提交时创建基线提交 `443621b4 chore: baseline dirty worktree before card text sizing`，该提交已完整包含本任务的 `ExecutionPage.vue`、三个静态合同和初始任务文档；未重写或拆分并发提交。
- 后续并发任务已将 `int_main` 推送至 `origin/int_main`；2026-07-29 收尾前复查为本地分支与远端无 ahead/behind。

## Verification Evidence

- RED: `node tests/e2e/edhr-assist-product-info-virtual-process-static.spec.js` -> FAIL，首个预期失败为填写页尚未定义产品信息虚拟 `80` 工序展示口径。
- GREEN: `node tests/e2e/edhr-assist-product-info-virtual-process-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-switch-filler-selectability-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-assist-fill-mode-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-switch-filler-formcenter-slot-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-assist-process-switch-all-statuses-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-batch-product-info-virtual-process-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS，耗时约 159 秒。
- GREEN: 官方登录前置 `node scripts/preflight/login-preflight.mjs` 使用本机 Chrome、`http://127.0.0.1:8081` 和本机默认身份 -> PASS。
- GREEN: 真实页面只读 E2E -> PASS；目标批次执行 `900000000910`、工单 `881MO090935`、粗洗任务 `7231`。切换工序共显示 15 张卡片，其中“粗洗工序 草稿”和“产品信息 待打开”各 1 张且互相独立；粗洗“切换填写人”只包含任务 `7231/7233/7234`，不包含产品信息任务 `7232`；MES 写请求数 `0`，console error 数 `0`。
- GREEN: experience-preflight -> PASS；已将填写页 `ExecutionPage.vue` 的切换工序/切换填写人分组门禁合并到 `docs/frontend-development.md#eDHR 产品信息虚拟 80 工序门禁`，并更新 `docs/experience-index.md` 路由关键词。

## Completed Work

- `ExecutionPage.vue` 新增与批次详情一致的产品信息虚拟工序识别：正式 `MAIN + BATCH_RECORD` 产品信息任务使用独立 `product-info:<reportId/taskId>` 分组键。
- 切换工序卡片对产品信息使用显示名称“产品信息”和显示排序 `80`，后端来源 `routeProcessId` 保持不变。
- 切换填写人由“按来源 routeProcessId 过滤”调整为“按当前任务显示工序分组键过滤”，隔离产品信息与粗洗任务，同时保留普通工序内全部传统批记录和 FormCenter 槽位任务。
- 已使用真实前端填写页的 `batchTaskPreview=1 + task/preview` 只读路径复验用户截图对应数据，确认运行态已加载修复后的分组逻辑。

## Blockers

- 无。

## Closeout

- `task-closeout-cleanup preview` -> PASS；keep 为 `task.md`、`execution-log.md`、`verification-report.md`，delete 仅为本任务 `bug-regression-evidence.md` 和 `e2e-artifacts/`。
- `task-closeout-cleanup apply` -> PASS；已删除临时 E2E JSON、截图和中间回归证据，未触及生产代码、正式测试或并发任务文件。
- 清理完成后保持 `ready_for_closeout`，等待收尾提交与远端推送完成后再标记 `completed`。
