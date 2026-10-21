# E2E-13 r5 原 O / 4222 新包门禁与定向复验

当前状态：APPROVED_TWO_SITE_FIX_GREEN_14_MAVEN_RELEASED；Root已批准并完成两处Query窄修复，新增同14例GREEN/exit0。原新包页面复验的申请信息已通过，关联/正文当时失败；本次修复后的真实页面尚待Root出包和明确放行，不能写为页面PASS。

## 必须先由 Root 提供的放行证明

只有 Root 在本轮明确通知 E2E-13 可执行，并给出新 int_qms 后端 PID、Jar 路径及 SHA256、health UP、嵌套 DCC jar 修复核验，以及当前前端 PID/构建身份后，才开始 Playwright。证明应确认后端包含 `DccControlledFileQueryServiceImpl.assertRelationNameVisible` 的培训职责名称元数据窄准入和 `DccSelectedDetailReadService.readApplication` 当前等待职责元数据分支；前端实际加载 `detail/index.vue.resolveDetailBodyReadContext` 的精确 TRAINING_APPLICATION 正文上下文。Root 同时确认本例当前运行租户、原文件和流程实例未被重置。后端遗漏任一修复或前端仍旧时，拒绝执行并把差异记录为运行态 BLOCKED。

## 放行前的历史证据（已被23:13新包放行取代）

协调目录 `dispatch.json` 最后修改于 2026-10-19 16:30，E2E-13 gate=execute 只是旧授权，不是本轮新包放行。它记录旧 `workflow-v6`，后端 PID 25124，SHA256 `ba0a8ead6e8e85cf29a8e339bbbc61a93ec30b3bb2adaf876d2736ff15f006b3`。关联 `reboot1019-runtime-proof.json` 同为 16:30，状态 `READY_HEALTH_AND_REAL_UI_VERIFIED`，对应同一旧 SHA。没有本轮新包、PID/SHA/嵌套模块核验，也没有 E2E-13 release proof。因此禁止将该旧包状态用于验收。

配置要求：profile `int_qms`、前端8061、后端48061。E2E-13无新增 env/config/SQL迁移/账号/角色要求。Root统一构建、部署及管理共享服务。

## 已执行的新包定向页面步骤及保留验收要求

沿用 `r4-runtime-acceptance.md` 的详细断言，仅增加新包强制门禁：

1. 用真实 Playwright 登录芋道源码租户的原 `DCC241016C13R18O`（用户910463），仅使用既有保护凭据；不得打印、重置、猜测或替换账号。
2. 从真实工作台 `dcc-workbench-offline-training-records` 唯一行打开 `DCC241016C13R18ADJUST`，确认文件ID `2054545668044084222`、版本A/1、BPM `fb0c1653-ca6f-11f1-9342-b082e25ec548`。不手输详情 URL。
3. 核验页面可见负责人 O/#910463 与负责人签名 #860/原 BPM；辅助申请证据需实际渲染当前轮次属性、签名及其真实节点行（若有）；关联分区应结束于该申请的正式内容或真实空态，不得留在“先选择并核验正式申请轮次”。核验 O 没有培训上传按钮。
4. 点击页面真实可见的“预览受控文件”，检查自然打开的 viewer 文件身份、A/1、TRAINING_APPLICATION 上下文和正文页实际渲染。弹窗打开、iframe存在或HTTP状态成功均不够；应见原文件正文的页图/文本内容。正文失败须如实记录，不能被辅助信息成功掩盖。
5. 关闭viewer并确认原详情身份/负责人/签名未变；保存 DOM 可见值、截图、trace、自然网络请求method/path/status、页面错误及非GET写请求数量。只通过页面断言，不调用 fetch/APIRequest，不解析响应JSON，不SQL。避免点击批准、签名、培训上传、上传、撤销/修改等写操作。

原批准两分支不重跑。保留 4218/A1/signature853 与4222/A1/signature860的既有批准证据。
