# 任务：MES 报工待归属混合展示与修改归属前端改造

## 任务目标

- 待归属页保存成功后不再让记录消失，同一列表同时展示待归属与已归属记录。
- 行内归属结果改为橙色“选择归属”和绿色“已归属”，满足条件时提供“修改归属”入口。
- 归属弹窗支持修改模式，打开时回显旧分配，并在提交时调用首次归属或再次归属接口。

## 当前状态

已完成。

## 上一任务检查

- 前端上一相关任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260625-mes-feedback-attribution-row-fill-all\task.md`
- 当前状态：`已完成`
- 处理说明：行内 `全部` 按钮迁移已经完成，本次继续在同一归属弹窗上扩展修改模式与列表保留展示能力。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- 适用强制门禁：
  - 本轮先做前端源码、静态契约与类型层改造，不做真实登录或写入型 E2E。
  - 归属页、状态标签、行内操作与弹窗排版必须继续使用 IntPP 紧凑运营台风格，不新增顶部结果卡或营销式提示。
  - 若后续进入真实 Playwright 验证，第一条登录命令必须先执行官方 `login-preflight.mjs`，并先在 `execution-log.md` 写 `GREEN: experience-preflight -> PASS`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。不可修改记录必须明确显示阻塞原因，不隐藏状态、不假装可编辑。
- `是否从根因和长期维护角度解决`：是。通过混合列表状态与弹窗编辑模式统一前端交互，不再依赖保存后顶部结果卡和强制 PENDING 过滤。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 保存后行内仍可见 -> Given 用户在当前批次筛选下保存一条导入记录的归属 / When 页面刷新列表 / Then 该记录仍留在当前列表，归属结果列显示绿色“已归属”。`
- `BDD: 待归属与已归属混合展示 -> Given 当前批次同时存在待归属和已归属记录 / When 用户查看待归属页 / Then 列表同时显示橙色“选择归属”和绿色“已归属”状态。`
- `BDD: 可修改已归属记录出现修改归属入口 -> Given 某条已归属记录后端返回 canModifyAttribution=true / When 用户查看操作列 / Then 页面显示“修改归属”入口，并保留“查看正式报工”次级入口。`
- `BDD: 修改模式回显旧分配 -> Given 用户打开一条可修改的已归属记录 / When 归属弹窗加载候选完成 / Then 弹窗自动回显已选工序与数量，并在提交时调用再次归属接口。`
- `BDD: 移除归属即回写进度旧文案 -> Given 用户查看待归属页与弹窗说明 / When 页面渲染 / Then 旧的“归属已完成并已回写进度”文案不再出现，统一改为“提交正式报工后回写排产进度”。`

## 里程碑

1. M1：补前端任务包与 RED 静态测试。
2. M2：扩展 API 类型、列表状态展示与弹窗修改模式。
3. M3：运行 GREEN 静态验证并回写证据。

## 预期验证

- `node tests/e2e/mes-feedback-attribution-continuation-static.spec.js`
- `node tests/e2e/mes-feedback-import-current-batch-static.spec.js`
- `node tests/e2e/mes-feedback-attribution-process-picker-static.spec.js`
- `node tests/e2e/mes-feedback-tracking-static.spec.js`

## 最终验证结果

- `node tests/e2e/mes-feedback-attribution-continuation-static.spec.js` -> PASS
- `node tests/e2e/mes-feedback-import-current-batch-static.spec.js` -> PASS
- `node tests/e2e/mes-feedback-attribution-process-picker-static.spec.js` -> PASS
- `node tests/e2e/mes-feedback-tracking-static.spec.js` -> PASS
