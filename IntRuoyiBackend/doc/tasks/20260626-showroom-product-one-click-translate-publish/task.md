# 任务：展厅产品一键翻译并发布

## 任务目标

- 后端新增展厅产品一键翻译并发布任务接口。
- 新增任务持久化表与 item 明细表。
- 复用现有产品字段 AI 翻译与关键词中英对照表，逐个产品保存并发布新版本。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。
- `是否存在临时补丁或绕过`：否。

## 当前状态

`COMPLETED`

## 验证

- `mvn -pl yudao-module-showroom "-Dtest=ShowroomProductTranslatePublishBatchIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：PASS。
- `python -X utf8 -m pytest script/tests/test_showroom_translate_publish_sql.py -q`：PASS。
- 真实 E2E 后 SQL 回读任务、item、产品 current revision：PASS。
