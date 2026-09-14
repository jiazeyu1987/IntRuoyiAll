# EDHR-STATIC-024 Dynamic Inspection Release Evidence

## Task Goal

修复 EDHR-STATIC-024：动态过程检验表单回填成功并具备正式签名、任务、项目和结论证据后，PQC 生产放行汇总必须接入同一正式动态检验证据并判为已就绪；缺少签名、任务身份、项目身份或结论未就绪时必须明确阻断。

## Scope

- 只处理 EDHR-STATIC-024。
- 不处理 EDHR-STATIC-025 或其它缺陷。
- 不编辑共享缺陷总表。
- 不执行 Playwright/E2E、数据库写入、服务启动/停止/重启、远程服务器操作。
- 2026-09-14 当轮用户新增授权：执行本地 Git 提交并融合进 `int_main`；未明确授权 remote push。

## Milestones

- [x] 读取项目规则、closeout 规则和 eDHR/动态检验/生产放行相关文档。
- [x] 定位动态过程检验 writer 与放行汇总读取缺口。
- [x] 先补 RED 静态/单元合同，证明动态实例回执未进入正式放行证据。
- [x] 最小修复：贯通传统 execution 与动态 FormCenter instance 的类型化正式证据。
- [x] GREEN 定向验证，并补静态逻辑检查。
- [x] 收尾记录 changed paths、验证命令、风险和 blockers。

## Expected Verification

- 静态合同或单元测试覆盖纯动态、纯传统、混合动态/传统证据。
- 静态合同或单元测试覆盖缺少动态实例证据时阻断。
- 定向 Maven 测试通过。
- 不运行 E2E，不写数据库，不启动/停止/重启服务。

## Design Constraints Check

- 动态 FormCenter instance ID 不得冒充传统 batchRecordExecutionId。
- 放行汇总必须同时保留传统执行记录和动态表单实例的证据类型。
- 正式结果证据仍需来自已确认 PQC 提交事件、签名、任务身份、项目身份和结论就绪证据。
- 不添加 fallback、默认成功、空集合成功或吞异常。
- 不扩大到 EDHR-STATIC-025 的共表单多项目写入锁定问题。

## Current Status

blocked - local implementation commit and `int_main` integration are complete; final `completed` closeout is blocked until remote push is explicitly authorized and unrelated dirty artifacts in the `int_main` worktree are resolved.
