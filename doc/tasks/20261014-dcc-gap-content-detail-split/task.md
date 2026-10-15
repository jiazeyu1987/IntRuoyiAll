# G10 正文与详情页面分工

## Goal
正文只展示名称、编号、版本或草稿、状态及正文；完整审批、附件、关联引用、历史等记录移至同一精确版本的只读详情。返回保留来源，不能换成当前版本。

## Milestones
1. 核对 int_qms、当前源码和相邻测试，冻结修改前指纹。
2. 先写 BDD 和可执行 Vue 渲染/导航 RED，完成授权范围内实现。
3. GREEN 与定向 REGRESSION，输出 review-ready/changed-files/integration-notes/verification-report，申请归还文件。

## Expected Verification
- Node test runner 执行真实 SFC 编译渲染和真实导航 handler；覆盖历史 Long ID、审批任务上下文、返回来源、失效上下文及读取拒绝。
- 相邻正文入口、只读权限、附件归属、详情读取及记录组件行为测试。
- Vue SFC 编译和定向 lint；git diff --check 仅核对任务范围。
- 不运行 E2E、Maven、全类型或大构建（没有 Root 唯一验证票），不宣称运行验收。

## Current Status
blocked — G10软件范围由Root A12正式接收（独立正文9+selection8=17/17 PASS）；三生产文件冻结并归还，后续G08→G07→G01。软件工作已交付，仅工程Git收尾未授权及cleanup不可用，不标completed。旧70/61/89/25均保留各自执行时点，非后续新源码总PASS。

## 设计约束检查
- 生产授权仅 detail/index.vue viewer 分支及对应导航、ControlledFileBrowseRecords.vue、shared/ControlledFileBasicInfoPanel.vue 和所属测试。
- 不改 Protected viewer、DetailRelationsPanel、公共 API、服务、路由 helper；不启动子 Agent、worktree、服务、实库写入或跨线程发消息。
- 详情继续走正式 getControlledFile 精确 ID 读取，附件继续用所选版本授权与既有服务端校验；不把 UI 隐藏当服务端授权。
- readonly 详情不得获得管理能力；保留列表/搜索/审批/历史的精确版本、申请、轮次和任务上下文，拒绝陈旧响应。
- G10正文/只读记录/BasicInfoPanel已获准接入G08唯一显示helper；generic管理分支的其余显示后续归G08。身份请求始终raw值，不从版本文案推断身份。

## BDD
- Given 从列表、搜索、审批或历史打开某精确版本正文，When 渲染正文，Then 只显示名称、编号、版本或草稿、状态与正文及详情入口，完整记录不在正文挂载。
- Given 历史 Long ID 与审批轮次/任务上下文，When 进入详情，Then 完整只读记录绑定同一 ID 和原上下文，不能读取当前版作为正文/详情。
- Given 原来源和正文路径，When 从详情返回正文再返回来源，Then 来源路由与查询保持不变。
- Given 陈旧详情、读取拒绝或上下文已变化，When 点击详情或等待响应，Then 不导航至其他记录、不挂载他版附件/记录；附件查看下载仍受正式授权校验。
- Given 两个父入口已有同版本所选申请A，When History开始切申请B、失败、重置或卸载发null，Then 关系prop同步null并且不能回填版本视图；旧上下文成功事件不能写入新上下文。
- Given 无正式本版BPM且无明确任务/流程上下文的纯版本视图，When 展示关系，Then applicationSelection为undefined；申请视图在核验完成前为null，完成后仅转交同文件正式四字段对象。

## Cleanup Keep
- doc/tasks/20261014-dcc-gap-content-detail-split/review-ready.md
- doc/tasks/20261014-dcc-gap-content-detail-split/changed-files.md
- doc/tasks/20261014-dcc-gap-content-detail-split/integration-notes.md
