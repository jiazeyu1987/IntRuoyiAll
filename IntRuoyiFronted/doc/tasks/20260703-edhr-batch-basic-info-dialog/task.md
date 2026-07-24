# 20260703 eDHR 批次详情基础信息弹框

## 任务目标

将 eDHR 批次执行详情页截图中顶部红框内的批次基础信息收进“基础信息”弹框；在“工序复盘”区域的“刷新复盘”按钮左侧新增“基础信息”按钮，点击后查看原顶部基础信息内容。

## 里程碑

1. 建立任务文档、读取经验门禁并确认页面位置。completed
2. 补充 RED 静态回归，覆盖基础信息弹框和按钮位置。completed
3. 最小修改详情页模板与状态，不改接口契约。completed
4. 运行目标静态验证和类型检查。completed
5. 更新任务记录与收尾状态。completed

## 经验门禁

- PowerShell / Windows shell：已读取根仓 `docs/powershell-memory.md`；本轮命令显式 UTF-8，不使用 `&&`。
- 前端页面 / 表格 / 样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`；本次只调整详情页信息层级和弹框，不做无关视觉重设计。
- 前端交付：已读取 `frontend-feature-delivery` 与 `references/frontend-contract.md`；记录 BDD、RED/GREEN 和前端证据。
- 真实 E2E：本次先用静态契约验证 DOM 结构；不做登录后写入、不操作服务器、不修改租户数据。

## BDD 场景

- BDD: 基础信息收进弹框 -> Given 用户打开 eDHR 批次执行详情页 / When 页面加载完成 / Then 顶部不再直接展示整块批次基础信息和批次级信息卡片，首屏聚焦工序复盘。
- BDD: 基础信息入口在刷新复盘左侧 -> Given 用户查看工序复盘标题区 / When 查看操作按钮顺序 / Then “基础信息”按钮位于“刷新复盘”按钮左侧。
- BDD: 弹框展示原基础信息 -> Given 用户点击“基础信息” / When 弹框打开 / Then 弹框中展示批次编号、工单、批次、产品、路线、任务进度、阻塞项、关闭/拒收信息、聚合 Hash 和批次级摘要。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否；仅调整展示位置，不新增兜底数据或吞异常逻辑。
- 是否从根因和长期维护角度解决：是；将批次级信息从首屏主内容收纳到明确入口，保留工序复盘作为主体。
- 是否存在临时补丁或绕过：否。

## 预期验证

- `node tests/e2e/edhr-batch-basic-info-dialog-static.spec.js`
- `node tests/e2e/edhr-batch-detail-review-fusion-static.spec.js`
- `node tests/e2e/mes-edhr-batch-review-remove-task-index-static.spec.js`
- `node tests/e2e/edhr-process-evidence-fusion-static.spec.js`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm.cmd ts:check`

## Cleanup Keep

- `doc/tasks/20260703-edhr-batch-basic-info-dialog/frontend-feature-evidence.md`

## 当前状态

- 状态：completed
- 当前里程碑：完成
- 已完成：已将顶部红框基础信息迁入“基础信息”弹框，并把“基础信息”按钮放在“刷新复盘”左侧。
- 验证通过：新增基础信息弹框契约、详情融合契约、删除工序任务索引契约、工序证据链契约和大内存类型检查均通过。
- 阻塞：暂无。
