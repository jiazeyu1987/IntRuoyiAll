# B4 执行记录

## BDD

- BDD: assignment create/get/page/complete-and-submit -> Given 一个可编辑的产品或公司字段和真实可追踪的站内信记录，When 创建指派、查询详情、分页列表并由被指派人完成提交，Then assignment、notify_message、change_request 必须形成可回查链路。
- BDD: product comment create/page/reply/resolve -> Given 一个产品文档讨论线程，When 创建顶层评论、分页查询、回复和解决，Then 回复必须继承同一产品与锚点身份，线程状态可见且持久化。
- BDD: notify linkage traceability -> Given 一条补充指派，When 系统发送补充提醒，Then `showroom_field_assignment.notify_message_id` 必须指向真实持久化的 `system_notify_message.id`。

## RED

- RED: `mvn -pl yudao-module-showroom '-Dtest=ShowroomAssignmentWorkflowTest,ShowroomDiscussionContentTest,ShowroomHttpApiIntegrationTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> FAIL, `ShowroomApiRuntime` 与已提交的 controller 契约不一致，缺少 `versionHistory/getProductDetail` 等方法且 `CompanyCurrentRespVO` 构造参数不匹配。
- RED: `mvn -pl yudao-module-showroom '-Dtest=ShowroomAssignmentWorkflowTest,ShowroomDiscussionContentTest,ShowroomHttpApiIntegrationTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> FAIL, 真实 notify 发送链路已接通，但测试模板缺少 `params`，触发 `SHOWROOM_NOTIFY_SEND_FAILED`。

## GREEN

- GREEN: `mvn -pl yudao-module-showroom '-Dtest=ShowroomAssignmentWorkflowTest,ShowroomDiscussionContentTest,ShowroomHttpApiIntegrationTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> PASS

## 完成项

- assignment 已切换为持久化 service，真实校验 assignee `EDITOR` 角色，真实调用站内信发送并保存 `notify_message_id`。
- assignment `complete-and-submit` 已从真实用户部门解析 `submitter_dept_id` 与 `leader_user_id`，保存单字段草稿 revision 并创建 change request。
- product comment 已切换为持久化 service，支持 `create/page/reply/resolve`，并校验 change request 锚点属于同一 product。
- controller 已补齐 assignment/comment 路由；测试 schema 已补齐所需的 system notify/user-role 基础表。

## Reopen RED

- RED: `mvn -pl yudao-module-showroom '-Dtest=ShowroomAssignmentWorkflowTest,ShowroomDiscussionContentTest,ShowroomHttpApiIntegrationTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> FAIL, 当前主工作区先在编译阶段被 narration/TTS 改动阻断：`ShowroomApiRuntime.java` 与 `ShowroomAliyunNlsAudioGenerationAdapter.java` 引用了不存在的 `cn.iocoder.yudao.module.ai.service.tts.*`。

## Final GREEN

- GREEN: 先安装当前工作区的 `yudao-module-ai` / `yudao-module-infra` 到本地 Maven 仓库，再重新运行 `mvn -pl yudao-module-showroom '-Dtest=ShowroomAssignmentWorkflowTest,ShowroomDiscussionContentTest,ShowroomHttpApiIntegrationTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> PASS

## 结果

- `assignment` 的真实 notify linkage 通过 `system_notify_message` 落表并在回查接口中可见。
- `product comment` 的 create/page/reply/resolve 回归通过，且 change-request 锚点参与者限制已生效。
- 当前 B4 指定命令最终通过，未保留阻塞项。
