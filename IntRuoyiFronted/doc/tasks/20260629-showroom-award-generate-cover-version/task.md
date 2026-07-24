# 任务：奖项行内生图并自动发布新版本

- Task ID: `20260629-showroom-award-generate-cover-version`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Created: `2026-06-29`
- Current Status: `blocked`

## Task Goal

在 `展厅管理 -> 产品管理 -> 奖项` 列表行操作区新增 `生图` 按钮；点击后基于当前奖项封面图进行美化、尺寸统一、主体居中摆正，并自动替换封面、升版本、发布为当前版本。

## Previous Task Check

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260629-showroom-award-tab-empty\task.md`
- 状态：`blocked`
- 处理说明：上一任务已在本机真实账号下无法复现“奖项页签无数据”，已明确记录阻塞；本次为独立新需求，不与上一任务混改。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`：中文任务文档、日志与命令输出统一按 UTF-8 处理。
- `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`：奖项列表继续沿用紧凑操作台与行内文字按钮风格，不改页面结构。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。以后端正式奖项生图接口承接按钮能力，不走前端本地技能直连。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 奖项列表行展示生图入口 -> Given 企宣用户进入奖项列表 / When 行操作区渲染 / Then 当前奖项行显示“生图”按钮并可独立进入加载态。`
- `BDD: 奖项生图成功后自动发布新版本 -> Given 奖项当前封面可读且当前版本中英文语音完整 / When 用户点击“生图” / Then 系统生成新封面、创建并发布新的奖项修订版，列表刷新显示新的 revisionNo。`
- `BDD: 奖项缺少封面或语音时生图失败快报错 -> Given 奖项当前封面缺失或当前版本语音不完整 / When 用户点击“生图” / Then 系统显式失败，不替换封面、不发布半成品版本。`

## Milestones

1. M1：建立任务文档、阻断旧任务并补 RED。`completed`
2. M2：实现后端奖项生图并自动发布链路。`completed`
3. M3：实现前端奖项行内按钮与调用。`completed`
4. M4：完成验证、证据与收尾。`blocked`

## Current Blockers

- 2026-06-29 用户切换当前优先级到 `SRM NAS定位` 搜索回车行为修复；本任务真实页面验收与收尾暂缓。
- 影响：奖项行内生图前端入口与最终闭环结论暂未完成，不应与本次 SRM 页面缺陷修复混合提交或混合验收。

## Expected Verification

- `node scripts/showroom-admin-award-generate-cover.test.mjs`
- `node scripts/showroom-admin-award-list.test.mjs`

## Current Blockers

- 正在等待本机真实页面对 `AWARD-003` 执行一次成功生图验收；前后端代码与定向测试已通过。

## Final Verification

- `node scripts/showroom-admin-award-generate-cover.test.mjs`
- `node scripts/showroom-admin-award-list.test.mjs`
