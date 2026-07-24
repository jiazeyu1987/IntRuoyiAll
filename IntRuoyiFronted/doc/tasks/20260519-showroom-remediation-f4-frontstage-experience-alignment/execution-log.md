BDD: 设备模式入口语义对齐 -> Given 前台首页需要明确区分 screen、pad、mobile 三种入口 / When 用户查看前台主页面 / Then 页面必须清楚展示三种设备模式入口的语义和路径，而不是只剩模糊的主页页签。
BDD: 公司返回路径显式可见 -> Given 设计文档要求展厅页和产品页始终保留回公司页的一键路径 / When 用户在前台主页面查看体验说明 / Then 页面必须显式展示“返回公司页”的动作与语义，而不是隐含在 tabs 或 fallback 导航里。
BDD: 设置页只影响讲解 -> Given v1 只有 PUBLIC 观众类型且语言切换只影响讲解文字和讲解音频 / When 用户查看设置行为 / Then 页面必须明确显示 PUBLIC 与讲解语言语义，不得暗示会切换整个界面语言。
BDD: 预览图与讲解缺失态清晰暴露 -> Given live 数据可能仍缺少 previewImageUrl、讲解文字或讲解音频 / When 页面渲染预览图和讲解区 / Then 必须直接显示缺失状态，不得伪造预览图、默认讲解文案或默认音频。

- M1: In progress. 已完成 AGENTS、F4 task、PRD、前端设计、用户流、B2/B5 契约记录与当前前台实现核对。
- M1 note: `index.vue` 当前仍是简化版 tabs + 表格；真实路由入口已切到 screen/pad/mobile 设备壳，因此本任务将通过 `index/shared/core` 交付体验模型与语义结构，为后续 F5 集成保留清晰边界。
- M1: Completed. 现状与依赖核对完成，确认本任务不进入 `wave-b` 目录，并以显式缺失态处理 preview/narration 资源。
- RED: `node --test scripts/showroom-frontstage-experience-alignment*.mjs` -> FAIL, 缺少 `src/views/showroom-frontstage/core/experience.ts`、缺少共享体验类型/状态 helper、`index.vue` 仍是旧的 `el-tabs` + 简化表格结构。
- GREEN: `node --test scripts/showroom-frontstage-experience-alignment*.mjs` -> PASS
- GREEN: `pnpm exec eslint src/views/showroom-frontstage/index.vue src/views/showroom-frontstage/shared src/views/showroom-frontstage/core` -> PASS
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260519-showroom-remediation-f4-frontstage-experience-alignment --mode preview` -> PASS, keep/delete 结果与本任务预期一致且无删除项
- Note: `node --test scripts/showroom-frontstage.test.mjs scripts/showroom-frontstage-shared.test.mjs` still reports pre-existing router-module expectation drift outside this task scope.
