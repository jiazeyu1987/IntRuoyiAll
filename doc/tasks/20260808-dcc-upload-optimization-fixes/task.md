# DCC 文件上传优化修复

## Task Goal

按照 `doc/tasks/20260808-dcc-history-version-conflict-verification/optimization-plan.md` 修复 DCC 文件上传页的历史文件升版状态冲突、编号/版本预检不一致、英文冲突错误、版本格式与生效日期前端校验，以及分类映射异常提示；保留“文控文件允许发布到未分类”的业务规则。

## Milestones

- [x] 建立实现任务门禁与 BDD/TDD 记录
- [x] 定位前端上传页、提交器、后端版本链校验和现有测试
- [x] 编写或更新失败回归测试，证明当前错误状态
- [x] 实现最小正式修复，不引入 fallback 或静默降级
- [x] 运行定向 GREEN 与回归验证
- [x] 输出验证报告和剩余风险
- [x] 完成 cleanup 与长期经验门禁
- [x] 执行本机真实页面只读 E2E 复验

## Expected Verification

- 前端静态合同覆盖历史文件升版与新建 master 互斥、编号冲突不可提交、英文错误中文化、版本格式校验、生效日期规则提示。
- 后端或前端定向测试覆盖版本格式与编号链路冲突错误映射。
- 保留“未分类允许发布”，但页面提示应表达为允许规则。
- 不提交业务数据，不操作远端服务器，不做 Git 提交。
- 本机真实 E2E 使用 Playwright 登录授权账号进入 `/dcc/controlled-file/upload`，验证现行版本/升版状态或分类路径，不点击“提交审批”，DCC 写请求数必须为 0。

## Current Status

completed

## Verification Result

DCC 任务范围 PASS：真实 E2E 已验证“按文件编号查到现行版本后自动切换升版、绑定现行版本目标并显示当前变更方式”。继续复跑时发现真实 E2E 脚本先输入再监听导致漏听 `current-version` 响应，已修正为先注册响应监听再触发输入，并用静态合同锁定。复跑 `e2e:dcc:upload-current-version:real` PASS，且 DCC 写请求数为 0。精确复验 `芋道源码/zhaohaichen` + `IDI / 技术调研报告 / 按压式球囊扩充压力泵技术调研报告.pdf V1.0` 已进入真实页面并完成只读数据预检，但本机该项目分类下历史文件选项数为 0，因此按门禁记录为 BLOCKED，未换数据冒充通过。

## Closeout Evidence

- `task-closeout-cleanup` preview/apply 已执行通过，未删除文件，无 blocked/warnings。
- 已将 DCC 上传历史文件升版状态门禁沉淀到 `docs/frontend-development.md`，并在 `docs/experience-index.md` 增加关键词路由；本轮继续复跑发现的 Playwright 监听顺序经验已合并到 `docs/e2e-rules.md`。
- 收尾继续复跑的 DCC 静态合同通过，`ts:check` 本轮 PASS。
- 真实 E2E：修正监听顺序后 `e2e:dcc:upload-current-version:real` PASS，`writeRequests=[]`；精确历史文件 E2E 因本机缺少目标历史文件选项 BLOCKED，`dccWriteRequests=[]`。
- Git 操作未执行：用户未要求提交、合并或推送；项目 Git Policy 允许不提交完成。

## Cleanup Keep

- doc/tasks/20260808-dcc-upload-optimization-fixes/frontend-feature-evidence.md
- doc/tasks/20260808-dcc-upload-optimization-fixes/bug-regression-evidence.md
- doc/tasks/20260808-dcc-upload-optimization-fixes/verification-report.md
- doc/tasks/20260808-dcc-upload-optimization-fixes/dcc-upload-history-revision-readonly.e2e.js
- doc/tasks/20260808-dcc-upload-optimization-fixes/dcc-upload-history-revision-readonly-result.json

## Applicable Gates

- DCC 上传升版不得用“新建 master”掩盖历史文件无法定位主档。
- 编号冲突、版本格式错误和生效日期规则必须在用户提交前明确暴露。
- 前端不能显示“可提交”同时让后端提交失败。
- “未分类”是允许发布目录，不作为错误阻断。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，目标是统一升版状态机、预检和错误映射。
- 是否存在临时补丁或绕过：否。
