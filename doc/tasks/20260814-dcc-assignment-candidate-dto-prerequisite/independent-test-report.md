# Independent Test Report

## Verdict

**PASS**

本结论仅覆盖“补齐两个 DCC assignment candidate DTO”这一前置切片。独立验证未发现合同错配、越界代码、fallback、兼容分支、默认成功、旧冲突改动或验证缺口。

## Objective And Scope

- 请求 DTO 必须继承 `PageParam`，仅新增 `keyword`。
- 响应 DTO 必须精确提供 `id/masterId/fileName/fileNumber/versionNo/status/currentProjectCodeId/currentProjectName/currentProjectCode/selectable/disabledReason`。
- 既有 controller、service、mapper、测试和前端消费者合同不得修改或绕过。
- 变更范围只允许两个 DTO 与本任务证据文档；本报告是唯一独立验证新增文件。

## Contract Verification

- 请求 DTO：`@Data`、`@EqualsAndHashCode(callSuper = true)`、继承 `PageParam`，声明字段仅为 `String keyword`。
- 响应 DTO：`@Data`，声明字段精确为冻结的 11 个字段；`javap -private` 与源文件一致。
- Controller：候选分页接口继续接收请求 DTO，并返回 `PageResult<DccProjectCodeAssignmentCandidateRespVO>`。
- Service：逐项写入响应 DTO 的 11 个字段；候选可选状态与审批中禁用原因保持原行为。
- Mapper：只读取请求 DTO 的 `keyword` 与继承分页参数，不需要额外请求字段。
- Test：`DccProjectCodeAssignmentServiceImplTest` 覆盖跨项目候选、可选状态、当前项目身份及审批中禁用原因。
- Frontend：现有 TypeScript 请求和响应类型与两个 Java DTO 字段一致。

## Independent Commands

- `mvn.cmd -pl yudao-module-dcc -am "-DskipITs" "-Dtest=DccProjectCodeAssignmentServiceImplTest,DccControlledFileMapperTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS；实际运行 23 项，Failures 0、Errors 0、Skipped 0，`BUILD SUCCESS`。
- Backend API validator self-test -> PASS；`backend-api-evidence.md` validator -> PASS。
- Bug regression validator self-test -> PASS；`bug-regression-evidence.md` validator -> PASS。
- `pwsh -NoProfile -File scripts\preflight\branch-runtime-port-guard.ps1` -> PASS；`int_main slot 14`，前端 `8095`，后端 `48095`。
- `git diff --check` -> PASS。

## Integrity And Scope Gates

- 分支 HEAD 等于任务声明基线 `bba5ba689a75008a0fb8d1ce3eb9f38ee68e47a4`；HEAD 相对该基线无额外提交差异。
- 新增本报告前，工作树精确包含两个 DTO 和五个任务证据文件，0 个越界路径；既有 controller、service、mapper、测试、前端、schema、权限及运行配置均未改变。
- 对全部未跟踪任务文件逐文件运行 `git -c core.autocrlf=false diff --no-index --check -- NUL <path>`，全部通过。执行证据中记录的 `task.md` 旧空白行 blocker 在本次独立实测时已不存在。
- 全部任务变更文件严格 UTF-8 解码通过，冲突标记文件数为 0。
- 两个 DTO 的禁止项扫描未发现 fallback、compatibility、graceful degradation、default success、mock success、`try/catch`。
- 两个 DTO 与主工作区保留副本 SHA-256 逐文件一致；未发现其它旧冲突改动被带入隔离 worktree。

## Findings

无放行阻塞项。

## Final Conclusion

该前置切片独立验证 **PASS**，可交由主管继续处理任务状态、提交、快进合并和 closeout。本次独立验证未修改生产代码、测试、其它证据、`task.md` 或主管状态，也未执行 commit、merge、clean。
