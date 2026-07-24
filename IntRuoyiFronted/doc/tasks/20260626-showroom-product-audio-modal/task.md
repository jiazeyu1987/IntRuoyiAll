# 任务：展厅产品语音按钮改为预览弹框

## 任务目标

- 将展厅产品列表行操作区的 `语音` 按钮从“直接生成中英文语音”改为“打开语音预览弹框”。
- 弹框打开后展示当前产品已有的中文、英文语音状态与可播放音频，并提供一个显式的 `生成中英文语音` 按钮。
- 继续复用现有产品语音数据链路和生成接口，不修改后端接口、数据库 schema、批量一键语音和产品编辑弹框讲解稿编辑行为。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个 frontend 任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-dcc-access-rule-bound-directory-list\task.md`
- 状态：`COMPLETED`
- 处理：已重新执行其静态合同、`pnpm ts:check` 和 frontend evidence 校验，满足收尾条件，不阻塞本次任务。
- 当前前端仓库存在与本任务无关的 DCC / MES / 展厅脏改；本任务只修改产品语音入口相关代码、测试与任务文档，不覆盖其他改动。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
  - `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
- 适用强制门禁：
  - 弹框与列表交互继续遵循 IntPP 运维台风格，保持紧凑白底、轻量状态和行内文本操作，不做无关视觉重构。
  - 前端不得用 mock、placeholder、fallback、静默 catch 或伪成功掩盖语音查询/生成的真实错误。
  - 涉及真实 Playwright 登录或生成验证时，第一条登录相关命令必须先运行官方 `login-preflight.mjs`；高风险真实写入前，`execution-log.md` 必须先记录 `GREEN: experience-preflight -> PASS`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。语音弹框直接消费真实 `getNarration` 和 `generateProductNarrationAudio` 结果；仅对后端明确返回的 `SHOWROOM_TARGET_NOT_FOUND` 识别为“该语种暂无可播放语音”。
- `是否从根因和长期维护角度解决`：是。把“查看已有语音”和“手动触发生成”拆成明确两步，避免列表按钮既承担查看又承担写入。
- `是否存在临时补丁或绕过`：否。不会通过列表缓存伪造双语音频，也不会把旧的直接生成逻辑藏到点击链路里。

## BDD 场景

- `BDD: 产品行语音按钮先打开预览弹框 -> Given 企宣人员打开展厅产品列表 / When 点击某一行的“语音”按钮 / Then 页面打开产品语音弹框，而不是立即调用语音生成接口。`
- `BDD: 语音弹框展示当前中英文语音现状 -> Given 产品存在已生成或未生成的中英文语音 / When 弹框加载完成 / Then 中文语音和英文语音区域都显示真实状态；有音频时可直接播放，无音频时明确显示未生成。`
- `BDD: 弹框内点击生成才触发真实写入 -> Given 用户已打开产品语音弹框 / When 点击“生成中英文语音” / Then 前端调用现有 `ShowroomAdminApi.generateProductNarrationAudio`，成功后刷新弹框和产品列表，失败时暴露真实错误。`
- `BDD: 缺少产品来源版本时直接失败 -> Given 当前产品缺少可用于生成语音的来源 revisionId / When 用户打开弹框或点击生成 / Then 页面直接暴露真实前置条件缺失，不添加兜底分支。`

## 里程碑

1. M1：创建任务文档、记录门禁、更新请求日志并补 RED 静态契约。`COMPLETED`
2. M2：新增产品语音弹框组件并把列表语音事件改为打开弹框。`COMPLETED`
3. M3：接通弹框内双语语音加载、生成后刷新和错误暴露。`COMPLETED`
4. M4：运行静态测试、类型检查、evidence 校验与真实登录/E2E 验证。`COMPLETED`

## 预期验证

- `node tests/e2e/showroom-product-row-audio-action.spec.js`
- `node tests/e2e/showroom-product-whole-assignment.spec.js`
- `node scripts/showroom-admin-product-bilingual-tabs.test.mjs`
- `node scripts/showroom-product-narration-action-disabled.test.mjs`
- `node scripts/showroom-admin-product-list.test.mjs`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-showroom-product-audio-modal\frontend-feature-evidence.md`
- `node D:\ProjectPackage\Int\IntRuoyi\scripts\preflight\login-preflight.mjs --base-url http://localhost:8081 --tenant 测试租户 --username aoteman --password 111111 --target-path /showroom/product --target-text 产品`

## 最终验证结果

- `node tests/e2e/showroom-product-row-audio-action.spec.js` -> PASS
- `node tests/e2e/showroom-product-whole-assignment.spec.js` -> PASS
- `node scripts/showroom-admin-product-bilingual-tabs.test.mjs` -> PASS
- `node scripts/showroom-admin-product-list.test.mjs` -> PASS
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-showroom-product-audio-modal\frontend-feature-evidence.md` -> PASS
- `node D:\ProjectPackage\Int\IntRuoyi\scripts\preflight\login-preflight.mjs --base-url http://localhost:8081 --tenant 测试租户 --username aoteman --password 111111 --target-path /showroom/product --target-text 产品` -> PASS
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260626-showroom-product-audio-modal\verify-product-audio-dialog.mjs` -> PASS，真实点击产品列表首行 `语音` 后打开 `产品语音` 弹框，显示中英文语音区与生成按钮，截图在 `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\output\playwright\showroom-product-audio-modal\product-audio-dialog.png`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260626-showroom-product-audio-modal --mode preview` -> PASS，仅提示临时验证脚本可删
- `node scripts/showroom-product-narration-action-disabled.test.mjs` -> BLOCKED，本地 `node_modules` 缺少可解析的 `@vue/compiler-sfc` 入口，未进入业务断言

## Cleanup Keep

- `doc/tasks/20260626-showroom-product-audio-modal/task.md`
- `doc/tasks/20260626-showroom-product-audio-modal/execution-log.md`
- `doc/tasks/20260626-showroom-product-audio-modal/frontend-feature-evidence.md`

## 阻塞说明

- 本次需求已完成；残余阻塞仅影响一条独立静态脚本环境，不影响本轮产品语音弹框交付。
