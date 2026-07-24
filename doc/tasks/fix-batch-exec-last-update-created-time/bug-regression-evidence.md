# Bug Regression Evidence

## Bug Summary And Expected Behavior

- 批次执行列表的“最后更新时间”列读取 `updateTime`。
- 新建批次执行 row 尚未被业务更新时，`updateTime` 的初始展示值必须等于该 row 的 `createTime`。

## Reproduction

- 后端：新增 `MesProEdhrBatchExecutionServiceTest#getPage_exposesInitialUpdateTimeAsBatchRowCreateTime`，先验证分页响应必须暴露 `createTime/updateTime` 且初始相等。
- 前端：新增 `edhr-batch-execution-unified-list-template-static.spec.js` 契约断言，要求列表列名为“最后更新时间”且读取 `prop="updateTime"`。

## Root Cause

- `EdhrBatchExecutionRespVO` 未声明 `createTime/updateTime`，服务层 `toResp` 与阻塞响应也没有映射批次执行 row 的审计时间字段。
- 前端列表列已绑定 `updateTime`，但原列名仍是“最近更新时间”，与用户要求的“最后更新时间”语义不一致。

## Regression Test

- `MesProEdhrBatchExecutionServiceTest#getPage_exposesInitialUpdateTimeAsBatchRowCreateTime`
- `tests/e2e/edhr-batch-execution-unified-list-template-static.spec.js`

## RED Evidence

- `RED: mvn.cmd -pl yudao-module-mes "-Dtest=MesProEdhrBatchExecutionServiceTest#openOrCreate_generatesRouteOrderedTasksAndIsIdempotent" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, EdhrBatchExecutionRespVO 缺少 getCreateTime/getUpdateTime`
- `RED: node tests\e2e\edhr-batch-execution-unified-list-template-static.spec.js -> FAIL, 列名仍为“最近更新时间”`

## GREEN Evidence

- `GREEN: mvn.cmd -pl yudao-module-mes "-Dtest=MesProEdhrBatchExecutionServiceTest#getPage_exposesInitialUpdateTimeAsBatchRowCreateTime" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS`
- `GREEN: node tests\e2e\edhr-batch-execution-unified-list-template-static.spec.js -> PASS`
- `GREEN: git diff --check -- <本任务相关文件> -> PASS`

## Verification

- 后端目标单测、前端静态契约和 diff 空白检查均已通过。

## Risk And Regression Scope

- 影响范围：`/mes/pro/edhr-batch-execution/page` 与 `/get` 返回的批次执行响应时间字段，以及前端批次执行列表列名。
- 未引入 fallback、降级、 mock 成功或吞异常。
- 当前工作区存在其他并行未提交改动；本任务未回退这些改动，验证范围限定为本次时间字段契约。

## Blockers And Follow-Up

- 无本任务阻塞。
