# G10 最终软件验证归档

## Root软件接收与证据时点
- 来源：2026-10-15 Root最新授权消息及协调coordination.md“续派 A12：详情父页交还”。Root独立实跑本线程正文9+父入口selection8，17/17 PASS，审查A04身份/null隔离，G10软件范围正式接收。
- G10停止写入交还指纹以changed-files.md为准。以下70/61/89/25为各自命令执行时点的开发证据，不证明后续G08/G07/G01新源码总PASS；Root17项接收也不能扩展到新增context/确认任务或真实运行验收。
- 本轮只补文档，没有重新跑tests/回归或更改源码。fulltypescript/大构建没有唯一票，待Root整合后统一验证；不写成PASS。
- 清理现有证据：只读目录清单为六份正式Markdown，无任务临时/生成资产。本轮无删除；正式cleanup preview/apply均未执行，无工具/脚本证据仍为既有收尾限制，不能冒称正式清理PASS。Git未授权，工程blocked。

## A04 最终结果与当前门禁（2026-10-15，覆盖下方历史待接/6项harness失败状态）
- 两个父入口接入已完成，父页已停写归还Root，按G08→G07→G01排期。正文9/9和新事件8/8真实Vue/handlerPASS；不称generic所有版本文案/G07后端门禁/G01待办完成。
- A04 RED：`node --test tests/unit/dcc-content-application-selection.test.cjs`，6项4FAIL（申请未传null及缺上下文隔离），2项原版本undefined已通过；实现后6/6GREEN，扩展错BPM/属性轮次后8/8。测试初始自定义renderer清理错误、随后动态SFC缺lang=ts的夹具错误均未作为产品RED，修正后实际业务失败/成功重新记录。
- 组合12份定向回归89/89，exit0，0 skipped/cancelled：
```text
node --test --test-reporter=tap tests/unit/dcc-content-application-selection.test.cjs tests/unit/dcc-content-detail-split.test.cjs tests/unit/dcc-viewer-trace-entry.test.cjs tests/unit/dcc-readonly-metadata-gate.test.cjs tests/unit/dcc-browser-html-alignment.test.cjs tests/unit/dcc-detail-route-read.test.cjs tests/unit/dcc-shared-attachments-component.test.cjs tests/unit/dcc-attachment-download.test.cjs tests/unit/dcc-exact-working-browser-scope.test.cjs tests/unit/dcc-working-browser-navigation.test.cjs tests/unit/dcc-selected-version-relations.test.cjs tests/unit/dcc-selected-application-context.test.cjs
```
- Root后续授权的两harness真实依赖/上下文补齐后只运行新增影响范围：`node --test tests/unit/dcc-detail-application-round-context.test.cjs tests/unit/dcc-nc-e2e-findings.test.cjs tests/unit/dcc-content-application-selection.test.cjs` -> exit0，25/25；原先6项relation-contract依赖失败已解除，保持原审批/培训/日期断言，不以stub失败冒充PASS。不重复已通过的大组合。
- 改后index/BrowseRecords SFC脚本+模板再次编译PASS，ESLint error0；index仅既有第2行warning，BrowseRecords零warning；任务授权范围diff-check exit0。
- 工程Git收尾与cleanup仍未执行；ready_for_closeout仅用于记录实现可审查里程碑，最终blocked为Git收尾未授权。无E2E/服务/真实业务库写入，隔离单测不是运行态验收。

## 名称修正时点的历史复核（2026-10-15，之后被A04接入与Root接收覆盖）
- 主专属9项真实Vue/导航PASS，新增完整fileName对照title及缺名称错误的RED/GREEN，禁止用简称/默认标题冒充精确名称。
- 最新G10及不受History新import影响的9份定向文件61/61 PASS，命令为下方10份REGRESSION去除dcc-detail-application-round-context.test.cjs；不是把移出项记成PASS。
- History新增typed selection事件后，当时dcc-detail-application-round-context.test.cjs有6项失败（resolver缺 ./relation-contract，尚未进入业务断言）；后来Root明确授harness修复，25项定向PASS，见A04最终段。70/70仅是更早时点执行证据。
- BrowseRecords helper并行升级要求usage.tenantId/sourceControlledFileId，旧自有fixture导致5项失败；已只在本线程所属browser-html-alignment补真实响应身份，18/18恢复PASS，不改正式校验。
- 当时G11 A04待正式冻结，未提前接线；随后冻结并完成两父入口。G07正文入口context由Root后续排期接入。
- generic管理分支显示仍需G08后续接线。当前review-ready只涵盖G10主拆分/显示/精确版本和返回/权限开发证据，不声称全项关闭。

## 结论
正文与同版本只读详情分工、精确版本/来源导航、最小正文和完整记录保留，以及G11两父入口selection事件均已实现并由Root接收。generic管理详情显示/G07context/G01确认任务由后续owner承担；软件接收不等于整体工程completed或真实运行验收。

## RED / GREEN
1. `node --test tests/unit/dcc-content-detail-split.test.cjs`：初始 exit 1，4/5失败；真实正文分支摘要缺失、缺少只读详情分支、导航丢查询、返回丢审批来源。实现后 exit 0，5/5。
2. `node --test --test-name-pattern='submitted working iteration' tests/unit/dcc-content-detail-split.test.cjs`：exit 1，审批中小版本未显示草稿；接入 G08 helper 后 GREEN。原精确正文 ID 不变。
3. `node --test --test-name-pattern='hide.*iteration' tests/unit/dcc-browser-html-alignment.test.cjs`：恢复改前两个 display 行后真实渲染 2/2 FAIL（可见 raw A/1-2），恢复本次 helper 接线后 2/2 PASS；两次均未更改原身份和请求。
4. `node --test --test-name-pattern='requested historical round' tests/unit/dcc-content-detail-split.test.cjs`：恢复改前 History props 后真实SFC投影 FAIL（newer-round != exact-old-round），恢复绑定后 PASS。测试增强期间 __props 夹具与 Vue 内部参数冲突曾导致工具测试错误，已更名为 __fixture 后按真实业务失败重新 RED/GREEN，不把夹具错误当产品 RED。
5. `node --test --test-name-pattern='selected name|missing exact file' tests/unit/dcc-content-detail-split.test.cjs`：真实Vue RED2项FAIL（显示title简称/缺fileName仍挂载正文）；正文改正式fileName和实际viewerIdentityError后GREEN，专属9/9。

## REGRESSION
cwd：IntRuoyiFronted，执行：
```text
node --test tests/unit/dcc-content-detail-split.test.cjs tests/unit/dcc-viewer-trace-entry.test.cjs tests/unit/dcc-readonly-metadata-gate.test.cjs tests/unit/dcc-browser-html-alignment.test.cjs tests/unit/dcc-detail-route-read.test.cjs tests/unit/dcc-shared-attachments-component.test.cjs tests/unit/dcc-attachment-download.test.cjs tests/unit/dcc-detail-application-round-context.test.cjs tests/unit/dcc-exact-working-browser-scope.test.cjs tests/unit/dcc-working-browser-navigation.test.cjs
```
结果：exit 0，70/70 PASS，0 failed/skipped/cancelled。

覆盖真实 SFC 编译与 Vue 自定义 renderer 的正文和详情树、真实 handler、Long ID/所选历史版/申请轮次、原来源返回、陈旧响应、只读元数据管理拒绝、附件归属、撤销下载授权、确认取消/切版后零请求、现有检出/检入精确身份。

初次相邻回归 32 项中7项失败：3个旧页面契约/返回对象断言，4个原 BrowseRecords 测试 resolver 缺已有 effective-facts import。已更新所属旧契约及完整夹具后 32/32；未修改生产以迁就旧“正文完整记录”要求。

## 源码定向检查
- Node 使用项目 vue/compiler-sfc parse/compileScript/compileTemplate 编译三份生产 Vue：3/3 PASS（不是完整 Vite 构建/全类型）。
- Node 使用项目 require('eslint') 的 ESLint API 三份生产 Vue：errorCount=0；index.vue 一条既有 DistributionReadPanel 第2行 first-attribute-linebreak warning，其他两份无 warnings。
- `git diff --check -- <本任务生产/测试/任务目录>`：exit 0；Git 输出既有 core.autocrlf LF→CRLF 提示，无差异错误。
- 记录存在性/必需章节和 UTF-8 结构验证见 execution-log.md 收尾段。

## 权限与证据边界
- 正文/记录只在当前精确 read 成功后挂载，不让陈旧旧版附件出现在切版或读取失败页面。
- 无附件下载权限测试直接调用实际 handler 仍零请求；确认后撤销权限/切版同样零请求；API 对真实错误 JSON（HTTP 200）拒绝，未模拟成功。
- 服务端版本/账号/租户权限是正式读取接口边界，本线程没有改后端或用新 UI 布尔值替代授权。无实时服务或账户E2E，不能将上述测试称为实库/真实页面拒绝验收。
- 未运行 Maven、全类型、大构建（无 Root 唯一票），未运行 E2E/服务/真实库写入、Git提交推送/worktree/远端/子Agent。

## 当前依赖与收尾
- G11 History 正式事件尚未冻结；精确两个父入口已准备，待 Root 通知后实施并补真实 RED/GREEN。
- Git 收尾明确未授权。已复核 task-closeout-rules；task-closeout-cleanup 未在本会话可用技能/仓库脚本中发现，preview/apply 未执行，不能写 PASS。本任务没有产生运行日志/临时截图/编译输出等待删文件；任务六份 Markdown 和所属测试应保留。
