# 20260805 QA 规程配置 tab 扁平样式

## Task Goal

将 QA 规程配置页下方 `总览 / 检验规则 / 检验项目 / 发布检查` tab 调整为与上方 PQC 模块 tab 一致的紧凑下划线样式；继续按截图移除项目选择器左侧可见的 `DCC 项目代码` 标签，并删除项目卡片、tab 卡片和内容卡片之间的空白，仅修改前端展示层。

## Milestones

- [x] 创建任务记录并记录用户截图诉求
- [x] 定位 QA 规程配置页 tab 组件与现有静态契约
- [x] 编写或更新最小静态合同，锁定 QA tab flat underline 样式
- [x] 实现 QA tab 样式调整，保持接口与数据来源不变
- [x] 运行定向验证并记录 RED/GREEN/REGRESSION
- [x] 更新验证报告与收尾状态
- [x] 删除项目选择器可见标签与页面根布局空白
- [x] 复跑目标静态契约和相邻验证
- [ ] 将项目选择器移动到标题与状态标签之间
- [ ] 验证桌面同排与窄屏换行样式

## Expected Verification

- `workdir=IntRuoyiFronted; node tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs`
- `workdir=IntRuoyiFronted; pnpm ts:check`
- `workdir=E:\IntRuoyi; git diff --check -- IntRuoyiFronted\src\views\mes\pro\processpool\QaRegulationPage.vue IntRuoyiFronted\tests\e2e\role-matrix-qa-regulation-tab-static.spec.cjs doc\tasks\20260805-qa-tabs-flat-style`

## Current Status

blocked

- 已确认用户要求：QA 规程配置页的 QA 下方 tab 样式要与上方已调整的 tab 视觉一致。
- 本任务只允许修改 QA 页面展示样式、相关静态合同与任务文档；保护 API、请求参数、后端、权限、数据库和真实数据来源。
- 已完成 QA tab 样式调整：`总览 / 检验规则 / 检验项目 / 发布检查` 使用 flat underline 类名、深色粗体 tab、绿色 active 文案和绿色下划线，tab wrapper 顶部收紧为 12px 且底部不保留空白带。
- 已删除项目选择器左侧可见的 `DCC 项目代码` 标签，选择器通过 `aria-label` 保留可访问名称；页面根布局 `gap` 已从 `8px` 改为 `0`，项目卡片、tab 卡片和内容卡片连续衔接。
- 目标 QA 静态契约、`pnpm ts:check` 和 `git diff --check` 均通过。
- 额外复跑的 PQC 契约失败在其自身过期的默认状态类型断言，与本次 QA 文件改动无关，不属于当前 Expected Verification。
- Git 收尾仍受同文件既有并行改动影响，无法安全把本任务 hunks 与既有 QA 改动独立提交，未执行提交或推送。
- 用户追加要求：将当前标题下方的项目选择器移动到标题与 `DRAFT` 状态标签之间的黄框位置，准备执行新一轮 RED/GREEN。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否
- `是否从根因和长期维护角度解决`：是，直接统一目标页面 tab 样式，不通过隐藏内容或占位结构绕过视觉差异。
- `是否存在临时补丁或绕过`：否

## Applicable Experience Gates

- `docs/frontend-development.md#前端截图样式块静态契约门禁`：截图样式调整必须锁定目标选择器、active bar、间距和旧结构负向断言。
- `docs/frontend-development.md#前端静态契约隔离门禁`：全量类型检查先失败在非本任务文件时，保留目标最小静态契约 RED/GREEN 证据并记录阻塞。
- `docs/powershell-memory.md#同文件并行改动选择性暂存门禁`：`QaRegulationPage.vue` 与 QA 静态契约已有并行改动，本任务只追加 QA tab 样式相关 hunk。
