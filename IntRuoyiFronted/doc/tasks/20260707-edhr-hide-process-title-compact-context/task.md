# eDHR 工序栏隐藏标题并压小上下文

## 任务目标

按截图要求，工序栏顶部左侧不再显示 `工序` 标题；右侧生产工单号和批记录号继续直接显示值，并将字号调小，减少顶部占用空间。

## 经验门禁

- PowerShell / Windows shell：已读取 `docs/powershell-memory.md`，命令输出显式 UTF-8，不使用 `&&`。
- 前端页面 / 样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`，保持紧凑运维台视觉。
- 前端复刻：已读取 `replicate-frontend-ui`，只改当前前端模板与样式，不改接口、DTO、后端、mock 或数据源。
- 前端特性：已读取 `frontend-feature-delivery`，保留现有 API、权限、路由和状态边界。
- BDD/TDD：先记录 Given/When/Then 和 RED/GREEN 证据；静态测试锁定无标题与更小上下文字号。
- 禁止 fallback：不新增降级、兜底、mock 或静默吞错。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，直接移除冗余标题节点并调整上下文样式。
- 是否存在临时补丁或绕过：否。

## BDD 场景

BDD: 工序栏标题隐藏 -> Given 用户打开批次详情页 / When 查看工序栏顶部 / Then 左侧不显示 `工序` 标题，顶部仅保留当前批记录上下文。

BDD: 上下文字号更小 -> Given 当前生产工单号较长 / When 页面渲染工序栏顶部上下文 / Then 工单号和批记录号使用更小字号完整展示。

## 里程碑

- [x] M1：创建任务文档并记录 BDD、门禁和设计约束。
- [x] M2：新增 RED 静态测试，证明当前仍显示标题且字号为旧尺寸。
- [x] M3：隐藏左侧标题并压小右侧上下文字号。
- [x] M4：运行静态测试、回归测试和 lint，记录 GREEN 证据。
- [x] M5：收尾清理预览并按范围提交。

## 预期验证

- `node tests/e2e/edhr-process-header-compact-context-static.spec.js` 先 RED 后 GREEN。
- `node tests/e2e/edhr-process-header-context-static.spec.js` 保持通过。
- `node tests/e2e/edhr-process-form-action-columns-static.spec.js` 保持通过。
- `node --check tests/e2e/edhr-process-header-compact-context-static.spec.js` 通过。

## 完成记录

- 实现：移除工序栏顶部左侧 `工序` 标题节点，红框位置不再显示文本。
- 样式：绿色区域上下文占满顶部宽度，字号从 12px 调整为 11px。
- 回归：同步更新工序栏上下文、三栏布局和紧凑上下文静态契约。

## 当前状态

completed
