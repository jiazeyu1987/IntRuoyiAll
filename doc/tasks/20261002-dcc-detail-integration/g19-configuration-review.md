# G19 项目负责人属性与审核人配置 Review

2026-10-03；生产范围分属 detail_closure（属性组件/wrapper）与并行 Owner（审核组件）。本轮不运行服务、数据库、E2E、Maven、全量 types/build 或 Git。

属性配置已实际修复并验证：`project-attribute-configuration.ts` 提供 exact decimal Long 身份，拒绝 unsafe number、0、超 Long、非字符串；`ProjectAttributeConfigurationDialog.vue` 对项目详情身份、启用账号目录、属性快照、原因、回执和异步关闭/切换/卸载做边界校验；`projectAttributes.ts` wrapper 深复制属性、裁剪并校验原因、只接受 Boolean true。旧响应不会覆盖新项目或旧项目关闭后新弹框。

审核配置生产文件在本轮修改前已被并行 Owner 更新为 `userStore.getRoles.includes('doc_control')` + 实际 permission（或全权限）+租户/操作者上下文；没有使用共享 `checkRole` 的 `super_admin` bypass。该生产文件不由本Agent覆盖，测试只读确认其行为。

## RED/GREEN 证据

- RED：旧属性组件/API真实转译行为允许 number leader ID、项目/账号响应失配、外部账号和晚响应继续 ready/PUT；旧共享权限模型允许 `super_admin` 单独通过 `checkRole(['doc_control'])`。初始新 guard 用例7 FAIL/2 PASS，确认缺口来自实际源码。
- GREEN：`node --test tests/unit/dcc-project-configuration-guards.test.cjs tests/unit/dcc-project-reviewer.test.cjs`：22/22 PASS。覆盖 exact Long 字符串、安全 numeric 转字符串、unsafe/外项目/disabled/重复/属性非法零写、关闭/换项目/卸载/同项目ABA读写、false回执；审核 super_admin-only、permission缺失、确认中角色/权限/操作者变化及晚响应零写。
- 既有回归：`node tests/e2e/dcc-project-attributes-unit.spec.js` 与 `node tests/e2e/dcc-project-attributes-race-unit.spec.js` PASS；属性 Dialog SFC script/template compile PASS；四个属性相关源码 lint 0 errors/0 warnings。

## 业务边界

后端项目属性 Controller 仍最终校验 project-code:update、项目 Editor/Owner、同租户启用负责人及审计事务；后端 reviewer config Controller/service 最终校验 project-code:update + doc_control。前端测试只证明载荷/上下文/零写，不冒称后端权限、真实租户或数据库事务通过。没有修改公共 permission/system API、后台、SQL、browser/detail 其它 Owner 文件。

当前 G19 没有新增后端支持请求；Root仍需在最终 FE type/build 和真实环境中复核并行审核组件最新指纹，再统一 closeout。
