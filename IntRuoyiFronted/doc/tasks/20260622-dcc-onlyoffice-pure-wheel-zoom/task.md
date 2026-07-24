# 任务：DCC OnlyOffice 纯滚轮缩放可行性与实现

## 任务目标

- 评估并尽可能实现 DCC OnlyOffice 受控预览中的“无 Ctrl 纯滚轮缩放”。
- 若浏览器和 iframe 边界导致无法在不破坏阅读交互的前提下稳定实现，必须明确记录阻塞原因与影响。

## 上一任务检查

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260622-dcc-onlyoffice-zoom-controls\task.md`
- 状态：`COMPLETED`
- 处理：上一任务已完成按钮缩放与 `Ctrl + 滚轮` 缩放，本轮在该结果上继续评估“纯滚轮”。

## 用户要求与执行边界

- 用户要求：
  - `无 Ctrl 的纯滚轮也缩放`
- 本任务边界：
  - 优先验证真实浏览器事件边界，再决定是否改前端组件。
  - 不修改后端接口和受控权限。
  - 若方案会明显破坏 OnlyOffice 内部正常滚动阅读，必须先记录该风险。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中文档：
    - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- 本任务强制门禁摘录：
  - 先补 RED 或前置可行性证据，再改实现。
  - 不得通过放开受控权限、替换后端预览方式或伪造浏览器事件规避真实限制。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。先验证 iframe 与浏览器事件边界，再决定是否交付。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 纯滚轮可直接缩放 OnlyOffice 预览 -> Given 用户在 DCC OnlyOffice 受控预览中阅读文档 When 在预览区域直接滚动滚轮 Then 预览应执行缩放而不是依赖 Ctrl 修饰键。`
- `BDD: 纯滚轮缩放不破坏受控阅读边界 -> Given 预览仍是受控只读文档 When 用户使用纯滚轮缩放 Then 编辑、下载、打印、复制限制必须保持。`

## 里程碑

1. 建立任务文档并记录当前已知边界。`DONE`
2. 用真实浏览器小实验验证 iframe 对纯滚轮事件的拦截边界。`DONE`
3. 若可行，补 RED 测试并最小实现；若不可行，明确阻塞。`DONE`

## 预期验证

- 浏览器实验脚本
- 若实现成功，再运行：
  - `node scripts/dcc-onlyoffice-zoom-controls.test.mjs`
  - `node scripts/dcc-onlyoffice-readonly-config.test.mjs`

## 当前状态

COMPLETED

## 当前结论

- 用户最终确认采用“`Ctrl + 滚轮` 控制放大缩小，防止牺牲上下滚动，同时保留放大缩小按钮”的方案。
- 当前 `OnlyOfficeReadOnlyViewer.vue` 已满足该方案，无需继续推进“纯滚轮直接缩放”实现。
- 本任务以可行性验证和方案收口完成，不新增代码改动。
