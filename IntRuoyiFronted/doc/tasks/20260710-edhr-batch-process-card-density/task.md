# eDHR 批记录工序卡片信息密度优化

## 任务目标
- 调整批记录详情左侧工序列表的显示样式，降低单个工序卡片高度。
- 保留工序序号、名称、报告、状态和选中态，不改变点击、键盘操作、数据来源、权限或接口。
- 在现有 203px 左栏宽度内提高首屏可见工序数量，长名称继续单行省略。
- 将灭菌报告、成品检报告、成品检记录的展示序号依次调整为 `90`、`91`、`92`，将放行虚拟工序展示序号调整为 `99`。
- 所有左侧工序序号徽标使用黑色文字；仅修改展示序号，不改后端真实 `routeProcessSort`、节点顺序或业务契约。

## 里程碑
1. [已完成] 读取 PowerShell、项目经验、统一前端样式和前端交付门禁，定位目标组件。
2. [已完成] 记录 BDD 场景并新增失败的紧凑样式契约测试。
3. [已完成] 最小调整待处理工序和已填写工序卡片布局。
4. [已完成] 运行紧凑卡片目标测试、相关回归、ESLint 和类型检查。
5. [已完成] 新增特殊节点展示序号与黑色序号样式的失败回归测试。
6. [已完成] 实现 `90/91/92/99` 展示序号并完成真实页面视觉验证。
7. [已完成] 完成任务记录、收尾预览和独立提交。

## 预期验证
- RED：新增静态契约在现有 `72px` 待处理卡片和 `8px` 列表间距下失败。
- GREEN：待处理工序卡片最小高度收紧到 `48px`，列表间距收紧到 `6px`。
- GREEN：已填写工序卡片改为紧凑双列布局，状态标签与主要信息同卡对齐。
- GREEN：灭菌报告、成品检报告、成品检记录、放行依次显示 `90`、`91`、`92`、`99`。
- GREEN：工序序号徽标文字统一为黑色，普通工序序号显示不受影响。
- REGRESSION：工序标题单行省略、203px 左栏宽度、紧凑上下文和移动端单列布局测试继续通过。
- 真实页面：在相同视口下可见工序数量增加，卡片不重叠，长文本不撑高列表。

## BDD 场景
- BDD: 待处理工序卡片紧凑显示 -> Given 批记录存在多个待处理工序 When 用户打开批记录详情 Then 每个工序卡片保持序号和名称可读且高度不超过紧凑规格，首屏可看到更多工序。
- BDD: 已填写工序信息紧凑但完整 -> Given 工序已有报告和状态 When 用户查看左侧工序列表 Then 工序名称、报告和状态仍可见，状态标签不额外占用整行高度。
- BDD: 长工序名称不撑高卡片 -> Given 工序名称超过左栏宽度 When 用户查看列表 Then 名称保持单行省略，不引起卡片高度增长或横向溢出。
- BDD: 选中与键盘操作保持 -> Given 用户点击工序或使用 Enter/Space 选择待处理工序 When 选中状态变化 Then 蓝色边框和焦点反馈仍然清晰，业务行为不变。
- BDD: 特殊节点使用两位展示序号 -> Given 左侧列表包含灭菌报告、成品检报告和成品检记录 When 用户查看工序序号 Then 三个节点依次显示 `90`、`91`、`92`，不再显示后端的 `9000`、`9010`、`9020`。
- BDD: 放行显示末尾序号 -> Given 左侧列表末尾存在放行虚拟工序 When 用户查看放行卡片 Then 序号徽标显示 `99`，工序名称仍为“放行”。
- BDD: 序号文字统一为黑色 -> Given 左侧列表显示普通工序、特殊节点和放行工序 When 用户查看序号徽标 Then 徽标内数字均使用黑色文字。

## 经验门禁
- PowerShell/UTF-8：已读取 `docs/powershell-memory.md`，中文文件显式按 UTF-8 读取，写入使用 `apply_patch`。
- 前端样式：遵循 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md` 的紧凑、可扫描、浅灰蓝边框和 `#1677ff` 激活态。
- 前端交付：保持现有 API、路由、权限和状态归属，仅修改 `BatchExecutionDetailPage.vue` 的目标列表样式和展示序号映射。
- 真实验证：测试租户当前批次执行列表为空；最终视觉验证按登录基线使用本机 `芋道源码/admin` 只读进入截图中的 `881MO090889`，断言无 MES 写请求。
- 展示序号：特殊节点使用前端显式展示映射，禁止改写后端排序字段或节点真实顺序。
- TDD：先新增静态样式契约并取得 RED，再修改生产样式。
- 并行任务：当前排产工作台和工艺路线任务正在其他文件继续执行，与本任务目标文件不重叠，不修改其状态或代码。

## 设计约束检查
- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，统一收紧列表间距和两类工序卡片布局，不通过缩放或隐藏业务信息制造表面紧凑。
- 是否存在临时补丁或绕过：否。

## 当前状态
- COMPLETED：紧凑卡片、特殊节点展示序号和黑色序号样式均已实现并通过静态测试、类型检查和本机真实页面只读验证。

## Current Status
completed

## 最终验证
- `node tests/e2e/edhr-batch-process-card-density-static.spec.js` -> PASS。
- `node tests/e2e/edhr-batch-process-display-sort-static.spec.js` -> PASS。
- `NODE_OPTIONS=--max-old-space-size=8192 pnpm ts:check` -> PASS。
- 官方登录预检进入本机 `芋道源码/admin` eDHR 批次执行页 -> PASS。
- 真实页面工序卡片高度：待处理卡片均为 `48px`，已填写卡片均为 `54px`，且无业务写请求 -> PASS。

## Cleanup Candidates
- `doc/tasks/20260710-edhr-batch-process-card-density/verify-visual.cjs`
- `doc/tasks/20260710-edhr-batch-process-card-density/process-card-density.png`
- `doc/tasks/20260710-edhr-batch-process-card-density/process-card-density-full.png`
- `doc/tasks/20260710-edhr-batch-process-card-density/process-card-density-result.json`
