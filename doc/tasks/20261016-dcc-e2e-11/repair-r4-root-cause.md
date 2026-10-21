# E11 R4 审批中心旧响应覆盖根因与修复

状态：修复与定向验证完成，dispatch HOLD，等待 Root review。未声明浏览器复验通过，历史 R3 结果保持原样。

## 根因 review

只读检查 `artifacts/rerun-20261020-r3-round2/business/L1-final.trace.zip` 自然请求元数据（未读取响应JSON作业务依据）：

| 请求 | UTC开始时间 | 耗时ms | 估算完成时间UTC |
| --- | --- | --- | --- |
| 无筛选 pageSize10 | 15:00:27.083 | 4043.468 | 15:00:31.126468 |
| 无筛选 pageSize20 | 15:00:27.141 | 4111.507 | 15:00:31.252507 |
| keyword=DCC241016C11R31792508286568 | 15:00:27.743 | 3378.304 | 15:00:31.121304 |

三者HTTP200；关键词请求先完成，两个旧请求随后完成。R3失败截图输入新编号但表格展示历史申请。原 `index.vue:getList` 每个请求无条件写 list/total、成功路由标记；catch无条件清列表并报错，finally无条件结束loading，符合旧请求最后覆盖的因果链。徽标还在响应返回后读取可变查询条件。

后端 `DccApprovalTaskAdapter.java` 第218、282行调用matchesKeyword，第309-320行按标题、编号、业务key过滤。结合旧行身份及请求时序，当前证据支持前端旧响应覆盖，不支持“该新申请提前生成会签任务”。此修复不改后端。

## 最小修改与边界

生产文件仅 `IntRuoyiFronted/src/views/approval-center/index.vue`：getList递增局部组件请求序号；只有最新请求的成功/catch/finally可以提交状态。成功门禁同时覆盖list、total、徽标和loaded-route；失败门禁保护数据、错误与toast；finally门禁保护loading。请求发起时固定徽标适用条件，避免读取后来编辑的关键词。最新失败原有清空和可见报错逻辑保留。

新增直接必要测试 `IntRuoyiFronted/tests/unit/approval-center-latest-list.test.cjs`。TypeScript AST提取当前SFC真实getList和依赖声明，在VM中执行，使用deferred API响应控制完成顺序；未复制算法。覆盖两个成功顺序、两个旧失败顺序、最新失败后旧成功及徽标条件快照，共6项。

既有并行状态标签 `PENDING_DELAYED_EFFECTIVE_CONFIRM` 在任务开始前即存在，保留；`repair-r4.diff` 为相对HEAD全差异，第一段状态标签不是本任务改动。没有改模板、后端、API wrapper、store或其它生产文件；没有缓存、重试或取消框架。

## BDD / RED / GREEN / REGRESSION

BDD详见task.md的R4节；记录先于生产修改。

- RED（生产修改前）：`node --test IntRuoyiFronted/tests/unit/approval-center-latest-list.test.cjs` → exit1，0/6通过，6项均为预期状态断言失败；包括旧行覆盖、提前loading结束、旧失败清空及徽标错误。
- RED归档补证：通过只读fs hook向同一测试提供HEAD的原SFC实现，exit1；见 `repair-r4-red-baseline.txt`。没有回写生产文件或切换分支。
- GREEN：相同node --test命令 → exit0，6/6通过；见 `repair-r4-green.txt`。
- `node IntRuoyiFronted/tests/e2e/approval-center-route-filter-visible-static.spec.js` → PASS（纯静态合同，不启动浏览器）。
- `node IntRuoyiFronted/tests/e2e/approval-center-tab-return-no-reload-static.spec.js` → PASS（纯静态合同）。
- `node IntRuoyiFronted/tests/e2e/approval-center-todo-standard-list-static.spec.js` → FAIL，原合同要求 `openReviewDialog(row)` 直接审核入口。使用只读fs hook加载HEAD SFC同样FAIL，见 `repair-r4-existing-baseline.txt`；本次差异没有改模板，保留为既有失败，不修改合同或扩大生产范围。
- `git diff --check -- IntRuoyiFronted/src/views/approval-center/index.vue IntRuoyiFronted/tests/unit/approval-center-latest-list.test.cjs` → exit0。

## 交接限制

未执行浏览器E2E、完整类型检查、构建打包、重启、数据库操作、Git写入或跨线程通知。单元回归证明函数乱序提交门禁，不替代真实页面复验；既有直接审核入口合同仍失败。任务经过ready_for_closeout核对，因无Git/清理授权及等待Root review标blocked/HOLD，不标completed。无新运行进程或浏览器。
