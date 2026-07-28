# Latest Version Switch Regression Evidence

## Bug

- 开启“最新版本”开关并按产品筛选时，同一产品下旧定义的旧版本表单仍会出现在列表中。
- 用户可见现象：选择最新版本后，列表仍显示 V13.0 等老版本行。

## Expected

- 开启“最新版本”开关后，列表只显示当前可见产品/批记录/表单类型分组中的最高批记录版本。
- 同一产品同时存在 V13.0 旧定义和 V14.0 新定义时，只显示 V14.0 表单。

## Reproduction

- `mvn -pl yudao-module-mes "-Dtest=cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportServiceImplDbTest#getGeneratedReportPage_latestVersionOnlyExcludesOlderDuplicateDefinitionRows" "-Dsurefire.failIfNoSpecifiedTests=false" test`

## Root Cause

- 原逻辑只按 `batchRecordDefinitionId` 计算各定义自己的最新版本。
- 当同一产品有旧定义 V13.0 和新定义 V14.0 并存时，V13.0 是旧定义内的最新版本，因此会绕过定义级过滤并继续显示。
- 服务端分页前缺少基于用户可见列表分组的二次最新版本过滤。

## Regression Test

- 新增 `MesProBatchRecordReportServiceImplDbTest#getGeneratedReportPage_latestVersionOnlyExcludesOlderDuplicateDefinitionRows`。
- 测试构造同一产品下旧定义 V13.0 `OBSOLETE` 和新定义 V14.0 `APPROVED`，断言 `latestVersionOnly=true` 时只返回 V14.0。

## RED:

- `mvn -pl yudao-module-mes "-Dtest=cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportServiceImplDbTest#getGeneratedReportPage_latestVersionOnlyExcludesOlderDuplicateDefinitionRows" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, `expected: <1> but was: <2>`。

## GREEN:

- `mvn -pl yudao-module-mes "-Dtest=cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportServiceImplDbTest#getGeneratedReportPage_latestVersionOnlyExcludesOlderDuplicateDefinitionRows" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。

## Verification

- `mvn -pl yudao-module-mes "-Dtest=cn.iocoder.yudao.module.mes.service.pro.batchrecordreport.MesProBatchRecordReportServiceImplDbTest#getGeneratedReportPage_latestVersionOnlyKeepsNewestVersionPerDefinition" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。
- `node IntRuoyiFronted\tests\e2e\batch-record-form-latest-version-switch-static.spec.js` -> PASS。

## Risk And Regression Scope

- 变更范围限定在生成表单分页接口的 `latestVersionOnly` 分支。
- 保留原定义级最新版本过滤，再在产品筛选后按可见产品/批记录/表单类型分组保留最高版本，避免前端分页后本地过滤。
- 未引入 fallback、降级、吞异常、mock 成功或默认成功值。

## Blockers

- 当前仓库存在大量非本任务脏改动且 `int_main` 领先 `origin/int_main`，最终 Git closeout/push 仍需按项目基线策略处理。
