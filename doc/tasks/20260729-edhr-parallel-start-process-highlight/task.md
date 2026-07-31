# 20260729-edhr-parallel-start-process-highlight

## Task Goal

让批次执行详情页的当前可执行工序状态与工艺路线关系图一致：当“工序开始”后第一组存在多个并行直接后继工序时，这一组工序都应显示当前运行态黄色背景，而不是只显示排序第一的粗洗工序。

## Milestones

- [x] 识别当前单工序 `currentProcess*` 投影与关系图第一组并行后继口径的差异。
- [x] 用 BDD + RED 复现开始节点第一组 3 个待打开工序只标黄 1 个的问题。
- [x] 实现正式数据链路修复，不放宽填写权限、不引入 fallback。
- [x] 运行目标后端/前端合同和相关回归验证。
- [x] 完成经验沉淀、收尾清理、提交和推送。
- [x] 补充真实路线关系图多前置门禁后端回归，确保汇合工序不会被误判为当前可执行。
- [x] 启动隔离运行态并通过真实 Playwright E2E 验证芋道源码/admin 批记录管理员可见 3 个黄色当前工序。

## Expected Verification

- 后端目标 JUnit 确认多起点路线创建链路、多前置汇合门禁、旧快照但当前配置生效的真实数据形态均正确。
- `node tests/e2e/edhr-batch-admin-current-process-highlight-static.spec.js`
- `node tests/e2e/edhr-batch-process-state-background-static.spec.js`
- 必要的相邻批次执行详情静态合同与类型检查。
- 真实 Playwright E2E 登录芋道源码/admin，打开批次 `900000000903`，断言粗洗工序、清洗工序、清洁工序黄底，组装Ⅰ工序非黄底，且无 MES 写请求。

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；后端按任务来源选择正式图源，冻结快照完整覆盖任务工序时使用冻结快照，批次任务由当前路线批记录配置生成且当前配置完整覆盖任务工序时使用当前关系图，缺失正式图源仍显式阻塞。
- `是否从根因和长期维护角度解决`：是，批次详情已按后端任务门禁 `available=true` 展示所有当前可执行工序组，并且任务门禁改为读取完整直接前置集合，避免多前置汇合工序提前可执行。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- `docs/frontend-development.md#eDHR 当前工序运行态展示门禁`：当前工序黄底展示不得依赖 `OPEN_FORM`、填写人或 `activeWorkTaskId`；本任务进一步要求并行第一组按后端 `available=true` 任务门禁整体显示运行态。
- `docs/frontend-development.md#eDHR 产品信息虚拟 80 工序门禁`：产品信息虚拟工序必须排除当前正式工序匹配，避免复用来源 `routeProcessId` 误高亮。
- `docs/e2e-rules.md#静态合同与真实 E2E 同步门禁`：本任务新增聚焦静态合同覆盖窄范围黄底行为，并复跑相邻静态合同；真实浏览器 E2E 已在 slot 12 运行通过。
- `docs/backend-development.md#当前配置与发布快照边界`：任务门禁必须核对批次任务 `routeProcessId` 与冻结快照/当前 BATCH 配置的正式图源覆盖关系，并读取完整直接前置集合，不能用单值前置或排序推断多前置汇合。
- `docs/e2e-rules.md#真实 E2E 页面加载判据门禁`：真实页面验证先等待目标接口命中对象 ID，再等待目标业务控件状态；不得用不稳定的内部执行号或批号文本等待替代工序组颜色断言。

## Cleanup Keep

- `doc/tasks/20260729-edhr-parallel-start-process-highlight/real-e2e-evidence.md`
