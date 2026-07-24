# 任务：运行控制台五个运维按钮 E2E 用例

## 任务目标

- 为运行控制台五个运维按钮分别补充 Playwright E2E 用例。
- 五个按钮：`发布测试服`、`提升正式服`、`立即备份`、`回滚版本`、`恢复数据`。
- E2E 必须走真实前端登录路径，使用 `芋道源码/admin`，并验证危险动作在缺少必要输入时不会发起真实 `/infra/runtime-control/actions` 请求。
- 由五个子 agent 分别编写一个按钮的测试文件，主 agent review 结果并运行验证。

## 非目标

- 不执行真实发布、备份、回滚、恢复、提升正式服动作。
- 不修改业务代码。
- 不新增 mock 成功路径。

## BDD 场景

- BDD: 发布测试服按钮安全打开 -> Given admin 拥有运维权限, When 点击 `发布测试服`, Then 弹窗显示发布范围且默认 `只发代码`，缺少原因时不提交动作请求。
- BDD: 提升正式服按钮安全拦截 -> Given admin 拥有运维权限, When 点击 `提升正式服` 并填写原因但不输入 `PROD`, Then 不提交动作请求。
- BDD: 立即备份按钮安全拦截 -> Given admin 拥有运维权限, When 点击 `立即备份` 并填写原因但不输入 `PROD`, Then 不提交动作请求。
- BDD: 回滚版本按钮安全拦截 -> Given admin 拥有运维权限, When 点击 `回滚版本` 并填写原因和 `PROD` 但不填镜像标签, Then 不提交动作请求。
- BDD: 恢复数据按钮安全拦截 -> Given admin 拥有运维权限, When 点击 `恢复数据` 并填写原因和 `PROD` 但不填备份点, Then 不提交动作请求。

## 预期验证

- RED：新增 E2E 文件前对应测试命令不存在或未覆盖五个按钮。
- GREEN：五个 E2E 文件均可用 Playwright 跑通。
- GREEN：`node tests\e2e\runtime-control-ops-static.spec.js` 仍通过。

## 当前状态

- 状态：completed
- 已完成：
  - 已建立任务文档和 BDD 场景。
  - 已由 5 个子 agent 分别新增 5 个按钮 E2E 文件。
  - 已由主 agent review 测试安全性，确认测试只覆盖弹窗和前端拦截，不执行真实运维动作。
  - 已修正共享登录 helper，兼容测试服默认租户字段不可见的情况。
  - 已运行 5 个真实 Playwright E2E 并全部通过。
  - 已运行运行控制台静态契约测试并通过。
- 阻塞与影响：
  - 暂无。项目未把 Playwright 声明为本地依赖，本次验证使用本机已缓存的 Playwright 1.60 运行。
