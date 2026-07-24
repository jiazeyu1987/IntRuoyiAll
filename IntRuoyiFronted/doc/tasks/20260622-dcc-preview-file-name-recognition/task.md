# 任务：DCC 受控预览基础信息识别入口补齐

## 任务目标

让 DCC 受控预览右侧基础信息面板与正常详情页共享同一套“识别基础信息”入口，使文控在预览态也能直接触发后端识别，并在刷新后看到回写后的文件名称。

## 上一任务检查

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260617-homepage-default-visible\task.md`
- 状态：`COMPLETED`
- 处理：上一任务已完整收尾，不阻塞本次 DCC 预览识别入口补齐。

## 用户要求与执行边界

- 用户要求：
  - 在 DCC 受控预览里可以识别文件名。
  - 文件名识别结果需要落成 DCC 基础数据里的项目名称。
  - 可结合测试服务器真实文件样本选择最适合方式。
- 本任务边界：
  - 只补齐预览态基础信息识别入口与对应静态回归测试。
  - 不在前端本地猜测项目名称；识别结果必须以服务端回写后的详情刷新为准。
  - 不新增视觉重构，保持现有 Int 运营台风格。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中文档：
    - `D:\ProjectPackage\Int\IntRuoyi\docs\server-access.md`
    - `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
    - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- 本任务强制门禁摘录：
  - 预览态与详情态应复用同一基础信息面板，不新增本地兜底识别或假回写。
  - 涉及测试服真实登录或 E2E 前，必须先在执行日志记录 `experience-preflight`，缺前置时直接阻塞。
  - 前端改动保持现有蓝/中性运营台风格，不做无关视觉改版。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。通过共享基础信息面板把预览态识别入口接到现有后端正式链路，而不是在预览页单独复制逻辑或本地推断。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 受控预览可直接触发基础信息识别 -> Given 文控在 DCC 受控预览右侧基础信息面板查看文件 / When 点击识别基础信息 / Then 前端必须调用与详情页相同的后端识别动作并在成功后刷新详情。`
- `BDD: 预览态仍复用共享基础信息面板 -> Given 详情页与受控预览都展示基础信息 / When 代码演进 / Then 识别入口、识别 loading 和基础条目跳转能力必须继续集中在共享面板上而不是分叉复制。`

## 里程碑

1. 建立任务文档并记录经验门禁。`DONE`
2. 补 RED 静态回归测试，要求预览态暴露识别入口与事件。`TODO`
3. 最小修改预览页共享面板参数，通过 GREEN。`TODO`
4. 运行前端目标静态验证并回填后端测试服阻塞说明。`TODO`

## 预期验证

- `node scripts/dcc-controlled-preview-project-code-recognition.test.mjs`
- `node tests/e2e/dcc-project-code-recognition-static.spec.js`

## 当前状态

COMPLETED：已将预览态接入共享 `ControlledFileBasicInfoPanel` 的识别入口，并通过静态合同测试确认预览态会传入 `show-product-recognition`、`project-code-recognition-loading`、`@recognize-project-code` 与 `@open-dcc-project-code`。当前前端侧没有阻塞。
