# 任务：DCC 访问规则查看/预览说明文案

## 任务目标

在不改变 DCC 访问规则真实接口、字段绑定和保存行为的前提下，在访问规则页标题右侧补充两行简短说明文案，解释“查看”和“预览”的含义。

- 说明区放在页头红框位置，不改表格列结构和按钮逻辑。
- 每行只解释一个概念：`查看`、`预览`。
- 每行不超过 20 个字。
- 不改后端接口、请求参数和响应结构。

## 当前状态

status: completed

## 上一相关任务检查

- 已检查同页上一任务 `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260625-dcc-access-rules-permission-summary-fill-width\task.md`，状态为 `completed`，允许继续本次页面说明文案调整。
- 当前只修改访问规则页展示层、静态测试和本任务文档，不覆盖其他用户改动。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- 适用强制门禁：
  - 页头说明区域必须保持 IntPP 运维台风格，紧凑、易扫读，不做营销式说明块。
  - 说明文案只可解释现有真实权限语义，不得引入与系统行为不一致的占位描述。
  - 不得用 mock、fallback、静默吞错或假状态掩盖真实权限口径。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。通过固定说明区和静态断言约束“查看/预览”文案，减少页面语义误解。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 页头显示查看说明 -> Given 管理员打开 DCC 访问规则页 When 查看标题右侧说明区域 Then 第一行显示查看的含义，且单行不超过 20 个字。`
- `BDD: 页头显示预览说明 -> Given 管理员打开 DCC 访问规则页 When 查看标题右侧说明区域 Then 第二行显示预览的含义，且单行不超过 20 个字。`

## 里程碑

1. M1：创建任务文档并记录前置门禁。`DONE`
2. M2：补静态 RED 断言，锁定说明区文案合同。`PENDING`
3. M3：实现页头两行说明文案。`PENDING`
4. M4：执行静态 GREEN 验证并完成收尾。`PENDING`

## 预期验证

- `node tests/e2e/dcc-access-rule-permission-summary-static.spec.js`
- `node --check tests/e2e/dcc-access-rule-permission-summary-static.spec.js`


## ??????

- `node tests/e2e/dcc-access-rule-permission-summary-static.spec.js`?PASS
- `node --check tests/e2e/dcc-access-rule-permission-summary-static.spec.js`?PASS
