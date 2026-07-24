# DCC 浏览/查阅权限按审阅矩阵限制后端执行日志

- 用户需求：`DCC 的“浏览/查阅”权限需要按 DCC审阅矩阵 列表直接限制`
- BDD: 旧 VIEW/QUERY 不再放行浏览 -> Given 用户仍命中旧 VIEW/目录 QUERY 权限但不在当前类别审阅矩阵解析参与人内 When 查询 DCC 浏览列表或打开文件详情 Then 该文件不可见且不可查阅。
- BDD: 审阅矩阵参与人可直接查阅 -> Given 用户命中当前类别审阅矩阵解析参与人 When 打开浏览列表、详情和已发布文件预览 Then 即使旧 VIEW/目录 QUERY/PREVIEW 权限未命中也允许查阅。
- BDD: 老流程待审预览不回溯 -> Given 文件已在旧 route snapshot 中流转且当前用户命中旧 snapshot 参与人但不在当前矩阵参与人内 When 预览该待审文件原件 Then 仍按 snapshot 放行。
- GREEN: experience-preflight -> PASS，本轮仅执行本机 DCC 定向单测与后端证据校验，不登录真实前端、不写测试服或正式服。
- GREEN: 读取 `backend-api-delivery` skill 与 contract -> PASS
- GREEN: 读取 `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260622-dcc-review-matrix-tab\task.md` -> PASS，确认上一后端任务已完成
- RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc -Dtest=DccControlledFileQueryServiceTest,DccControlledFileReviewMatrixAccessServiceTest,DccControlledFilePreviewProtectionTest,DccOnlyOfficeControlledPreviewTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL，首轮暴露旧测试仍依赖目录 `QUERY/PREVIEW` 与旧 viewer token 证据桩，证明浏览/预览真源切换后旧断言需要同步更新
- GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc -Dtest=DccControlledFileQueryServiceTest,DccControlledFileReviewMatrixAccessServiceTest,DccControlledFilePreviewProtectionTest,DccOnlyOfficeControlledPreviewTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS，`65` tests passed，已覆盖浏览列表、详情、已发布预览按审阅矩阵放行，以及待审原件继续按 snapshot 放行
- GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc -Dtest=DccControlledFileQueryServiceTest,DccControlledFileReviewMatrixAccessServiceTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS，`51` tests passed，确认查询权限切换与矩阵参与人解析在最小回归集上稳定通过
- GREEN: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260622-dcc-review-matrix-browse-permission-change\backend-api-evidence.md` -> PASS，后端证据合同完整。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260622-dcc-review-matrix-browse-permission-change --mode preview` -> PASS，`backend-api-evidence.md` 被识别为可清理附属产物，其余任务核心记录保留。
- CHANGE: 新增 `DccControlledFileReviewMatrixAccessService`，按当前类别生效审阅矩阵解析岗位、上传者派生岗位、直接用户与系统岗位用户，生成浏览/查阅参与人集合。
- CHANGE: `DccControlledFileQueryServiceImpl` 改为先拉取候选文件，再按“申请人 / 目录管理 / 当前矩阵参与人 / 旧 snapshot 参与人”过滤浏览和预览权限；已发布文件二进制读取不再依赖旧目录 `PREVIEW` 放行。
- CHANGE: `DccControlledFileMapper` 新增 `selectBrowserSummaryList` 供浏览列表先取候选后再按新真源过滤，避免旧分页 SQL 把目录 `QUERY` 规则提前固化。
- CHANGE: `DccControlledFilePreviewProtectionTest`、`DccOnlyOfficeControlledPreviewTest`、`DccControlledFileQueryServiceTest` 已同步改为基于审阅矩阵真源断言浏览/预览行为。
