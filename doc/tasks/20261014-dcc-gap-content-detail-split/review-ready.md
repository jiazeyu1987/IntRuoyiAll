# G10 最终软件交付回执

## Root接收来源与冻结范围
- 2026-10-15 Root最新分派明确已独立实跑正文/导航9项与两个父入口selection8项，17/17 PASS，并review A04 identity/null隔离后接收G10软件范围。
- 同一结论已登记于doc/tasks/20261014-dcc-gap-fix-coordination/coordination.md的“续派 A12：详情父页交还”（行216起）。本报告引用Root实际审查回执，不把本线程旧70/61/89测试计数当Root复跑或后续新源码的总PASS。
- G10生产三文件冻结：detail/index.vue按A12授G08 generic显示，随后G07读context、G01确认task；BrowseRecords与BasicInfoPanel由Root保护。本线程只归档，不再写源码或tests，也不重复回归。
- 精确交还原始字节SHA256：index.vue BD834799FD8DDB803F4168BA549E2D04E54ACD4851CDEF8CC67F9B7E546241FE；BrowseRecords F212E5C2154582663E420C16A937743B2FBE45B637B6245467A686D6852BED8E；BasicInfoPanel 8ED9E429AFE55438823B7CF6D461B2CE06EC739AB25161F7E7E4EFFD47E5F8CA。这些是G10停止写入时指纹，不能覆盖后续owner修改后的文件。
- 没有full TypeScript/大构建唯一验证票，Root整合后统一验证；软件接收不扩展为运行/E2E/工程completed。

## 已有清理证据
- 当前任务目录只含task/execution-log/verification-report/review-ready/changed-files/integration-notes六份正式Markdown，没有临时日志、截图、生成脚本、编译产物；本轮只读文件清单核对已完成，无删除。
- task-closeout-cleanup preview/apply此前未执行：会话无该能力、仓库无对应可用脚本；本次不把目录核对冒称正式cleanup PASS。Git收尾未授权，按规则保留blocked状态。

## A04 最终接入与生产文件归还（2026-10-15，覆盖下方历史等待状态）
- Root正式冻结A04后已完成index.vue与BrowseRecords两个父入口的selection-change/applicationSelection接线。申请视图初始/切换/失败为null，核验后转交四字段原身份；上下文或file对象/读取序列变化同步清空并重建History，旧callback不可写新上下文。
- 两父入口独立复核同文件与申请身份；锁定流程上下文拒绝其他BPM；错误显示正式提示且保持null。明确无本版BPM和任务/流程上下文的纯版本视图传undefined，不将申请失败改读版本快照。
- 新实际Vue RED：6项中4FAIL（申请prop为undefined、缺上下文隔离）；GREEN 6/6，增加锁定BPM/非法属性轮次后8/8。交还前该源码时点正文9项PASS，组合12份回归89/89；后续改版须重新验证。
- Root随后明确授权修dcc-detail-application-round-context和dcc-nc-e2e-findings两个harness；只加载真实relation-contract/version-display模块、补viewerMode=false的管理/培训路由宿主，原行为断言不变。两harness+8项接线定向25/25，此前6项依赖失败已解除。
- 本线程生产文件已停写并归还Root排期：detail/index.vue、ControlledFileBrowseRecords.vue、BasicInfoPanel.vue。后续G08 generic显示→G07读context→G01确认待办，由Root登记唯一owner；G10不继续占有父页等待其它需求。
- G10交还时生产SHA256及改动路径见changed-files.md；当时三Vue编译PASS，定向lint无error，仅index.vue既有第2行格式warning，diff-check PASS。
- Git/E2E/服务/数据库写入仍未授权，不称工程completed或运行验收。

## 已实现
- 正文分支只有精确名称/编号/版本或草稿/状态/Protected 正文和详情入口，不挂载基本信息、审批、附件、关系或历史。
- 独立只读详情分支挂载完整 BasicInfoPanel 和 BrowseRecords；BrowseRecords 保留附件、审批历史、关系引用、版本历史、签名、培训及下发记录。
- 同 ID 导航保留原 from、processInstanceId、taskId、applicationType 等查询，清除 viewer/handling/controlledPrint 并设置 traceability；返回指向原正文，正文再返回原来源。站内已注册审批来源可返回，外部/未注册/畸形来源明确报错，不擅自改成浏览列表。
- 所选版本未成功读取、正在切换、陈旧或身份不匹配时不挂载正文/记录且不可进入详情；附件沿用正式版本归属/预览/下载授权。
- 收到 Root 转交 G08 显示合同后接入唯一 getDccVersionDisplayText：正文审批中小版本和只读详情的版本/来源/选用草稿/历史，以及 BasicInfoPanel 均隐藏小版本文案，raw 身份不变。
- 所选审批 processInstanceId 传入 History 并锁定该轮次，不使用 file 最新轮次覆盖。
- Root review 名称核对：完整名称为正式 fileName，title 是独立简称。正文和详情标题使用 fileName，缺失时显式报错并阻止正文/详情入口；没有使用 title 或默认标题代替精确名称。新 Vue RED 两项 → GREEN，主专属测试现9项。

## 历史开发证据（按执行时点保留）
- Node Vue SFC 编译渲染及真实 handler：主拆分 RED 4/5 FAIL → GREEN 5/5。
- 草稿正文：已提交 PENDING_SUPERVISOR_REVIEW 小版本 RED → GREEN；BasicInfoPanel/BrowseRecords 两项真实渲染 RED → GREEN。
- History primary-bpm-round 真实 SFC prop 投影：旧 file 最新轮次 RED（newer-round != exact-old-round）→ GREEN。
- 已跑相邻路由、只读编辑门禁、记录组件及附件权限回归；完整最终命令/计数以 verification-report.md 为准。
- 定向 ESLint 生产三文件 errorCount=0；index.vue 1条既有第2行格式 warning，未改无关分发分支。
- 名称修正时点主9项PASS、9份定向61/61；当时共享History新import使相邻application-round-context 6项harness报 ./relation-contract，随后Root明确授权补齐并定向PASS，见A04最终接入段。

## 归还后的依赖范围
- G11 History selection-change两父入口已完成，Root A12已接收；不再等待旧文档更新或继续持有父页。
- G07正文入口context待Root公共API/Protected viewer阶段接入；本线程保持既有接口，不以本轮UI隔离冒充日常草稿后端门禁完成。
- 本轮 G10 viewer 与只读详情显示已接线；generic 管理/签核详情的其它可见版本字段由 G08 按 Root 阶段转交继续统一，不修改后台请求字段。
- Root已审查接收，后续唯一owner按A12执行；G10不再请求重复归还审批。
- 工程 Git 收尾未授权；不自动标 completed，无 E2E、Maven、全类型/大构建、服务重启、数据库写入或子 Agent。
