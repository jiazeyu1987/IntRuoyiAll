# G10 修改文件

只列本线程实际写入，既有并行改动不属于本线程。

## 生产
- IntRuoyiFronted/src/views/dcc/controlled-file/detail/index.vue：正文最小布局、独立只读详情分支、同精确 ID 导航/来源返回/陈旧上下文拦截，导入 G08 唯一显示 helper。
- IntRuoyiFronted/src/views/dcc/controlled-file/detail/ControlledFileBrowseRecords.vue：记录改为详情语义，锁定指定 BPM 轮次、保留历史入口来源、可见版本文案使用 G08 helper。
- IntRuoyiFronted/src/views/dcc/controlled-file/shared/ControlledFileBasicInfoPanel.vue：当前执行版本文案使用 G08 helper，原数据不变。

## 所属测试
- IntRuoyiFronted/tests/unit/dcc-content-detail-split.test.cjs（新增）：实际正文/只读详情分支编译渲染、真实导航/返回 handler、历史 Long ID、草稿显示、指定轮次及失效上下文。
- IntRuoyiFronted/tests/unit/dcc-content-application-selection.test.cjs（新增）：两个真实父入口History事件/Relations prop编译渲染；同步null、旧callback、同ID刷新、版本视图undefined、错文件/BPM/属性轮次拒绝。
- IntRuoyiFronted/tests/unit/dcc-browser-html-alignment.test.cjs：旧正文完整记录断言更新为当前需求；修复现有组件测试缺少 effective-facts 依赖解析/route.query 的夹具；新增 BrowseRecords/BasicInfoPanel 真实草稿显示测试。
- IntRuoyiFronted/tests/unit/dcc-viewer-trace-entry.test.cjs：接收保留原查询参数的 RouteLocation 对象，验证只读合同；补齐既有指派事件 handler 夹具。
- IntRuoyiFronted/tests/unit/dcc-readonly-metadata-gate.test.cjs：正文不含基本信息编辑，独立只读详情无编辑能力的实际渲染。
- IntRuoyiFronted/tests/unit/dcc-detail-application-round-context.test.cjs：Root后续授权harness加载真实relation-contract/version-display；原10项行为断言不变。
- IntRuoyiFronted/tests/unit/dcc-nc-e2e-findings.test.cjs：Root后续授权harness补两个正式非viewer宿主的viewerMode=false；原7项行为断言不变，不是E2E执行。

## 任务记录
- doc/tasks/20261014-dcc-gap-content-detail-split/task.md
- doc/tasks/20261014-dcc-gap-content-detail-split/execution-log.md
- doc/tasks/20261014-dcc-gap-content-detail-split/verification-report.md
- doc/tasks/20261014-dcc-gap-content-detail-split/review-ready.md
- doc/tasks/20261014-dcc-gap-content-detail-split/integration-notes.md
- doc/tasks/20261014-dcc-gap-content-detail-split/changed-files.md

## G10冻结交还 SHA256（原始字节，非后续owner新源码指纹）
- detail/index.vue：BD834799FD8DDB803F4168BA549E2D04E54ACD4851CDEF8CC67F9B7E546241FE
- detail/ControlledFileBrowseRecords.vue：F212E5C2154582663E420C16A937743B2FBE45B637B6245467A686D6852BED8E
- shared/ControlledFileBasicInfoPanel.vue：8ED9E429AFE55438823B7CF6D461B2CE06EC739AB25161F7E7E4EFFD47E5F8CA

原改前指纹位于 execution-log.md。未改 DetailRelationsPanel、Protected viewer、公共 API/后端/路由 helper。

## 接收来源
2026-10-15 Root最新回执及coordination.md A12：独立17/17 PASS，G10软件范围正式接收；三生产文件停止写入。index.vue已归G08→G07→G01排期，BrowseRecords/BasicInfoPanel暂Root保护。本次仅更新自有报告，不计算新生产hash或重复测试。
