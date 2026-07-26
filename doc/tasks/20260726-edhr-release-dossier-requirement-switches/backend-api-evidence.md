# Backend API Evidence

## Scope

- 新增 `GET/PUT /mes/pro/edhr-release-setting/dossier-requirements`，权限沿用 `mes:pro-batch-record-execution:golden-finger`。
- 新增完整布尔配置 DTO、配置服务、配置 hash 和提交前 hash 一致性校验。
- 放行预检新增 4 个资料限制检查项：关闭为 `NOT_APPLICABLE/INFO`，开启且证据完整为 `PASS/INFO`，开启但缺任务、未完成、跳过或缺已保存 `ADD` 附件为 `BLOCKER/BLOCKER`。

## Contract

- 配置键：`mes.edhr.release.dossier.requirements`。
- PUT 必须提交完整布尔对象：`incomingInspectionReportRequired`、`sterilizationReportRequired`、`finishedProductInspectionReportRequired`、`finishedProductInspectionRecordRequired`。
- 配置缺失、非法 JSON、字段缺失、非布尔字段和预检后配置 hash 变化均 fail fast。

## BDD

- `BDD: 金手指配置可见性 -> Given 金手指用户 / When 打开个人中心配置页签 / Then 可看到 4 个资料限制开关；普通用户不可见配置页签。`
- `BDD: 默认关闭保持现状 -> Given 四个开关默认关闭 / When 特殊节点未完成且无附件 / Then 放行预检不因这些资料阻塞。`
- `BDD: 打开后阻止无资料放行 -> Given 某资料限制打开 / When 对应特殊节点未完成或无已保存附件 / Then 放行预检生成 BLOCKER 且提交放行失败。`
- `BDD: 完成并上传后允许放行 -> Given 某资料限制打开 / When 对应特殊节点已完成且存在已保存 ADD 附件 / Then 该检查项 PASS。`
- `BDD: 配置变更后必须重跑预检 -> Given 预检后开关状态发生变化 / When 提交放行 / Then 后端拒绝提交并提示重新预检。`

## RED

- `RED: mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrReleaseServiceImplTest,MesProEdhrReleasePrecheckContractTest,MesProEdhrReleaseDossierRequirementSettingServiceImplTest" test -> FAIL`，新增测试引用的配置服务、VO、控制器、检查项和 SQL seed 尚不存在。

## Verification

- `GREEN: mvn.cmd -pl yudao-module-mes -am "-DskipTests" compile -> PASS`，后端生产代码 reactor 编译通过。
- `BLOCKER: mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrReleaseServiceImplTest,MesProEdhrReleasePrecheckContractTest,MesProEdhrReleaseDossierRequirementSettingServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL`，被并行/无关 system 模块 `CodexTestRunnerServiceImpl#getRunnerStatus()` 编译缺口阻塞。
- `BLOCKER: mvn.cmd -pl yudao-module-mes "-Dtest=MesProEdhrReleaseServiceImplTest,MesProEdhrReleasePrecheckContractTest,MesProEdhrReleaseDossierRequirementSettingServiceImplTest" test -> FAIL`，辅助 MES-only 路径被并行/无关 route/BPM 编译漂移阻塞。

## Blockers

- 后端目标 JUnit 尚未取得 GREEN；需先解除并行 system、route、BPM 编译阻塞后复跑正式 `-am` 命令。
