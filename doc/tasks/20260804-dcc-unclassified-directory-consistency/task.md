# 20260804 DCC 未分类目录自动落位一致性

## Task Goal

把 DCC 中仍要求“文件类别必须先绑定目录”的同类入口统一为正式 `UNCLASSIFIED / 未分类` 目录自动落位，避免用户在 NAS 导入、本地文件夹导入或元数据维护中被要求手工到 DCC 文件类别维护目录绑定。

## Milestones

- [x] 梳理 NAS 导入、本地文件夹导入、元数据编辑的目录绑定校验入口。
- [x] 用回归测试复现未绑定类别仍被阻塞的问题。
- [x] 后端改为复用正式未分类目录解析，不引入静默降级或默认成功。
- [x] 前端移除用户侧“必须绑定目录”的阻塞提示，显示自动落位未分类。
- [x] 运行后端与前端聚焦回归验证。
- [x] 扫描系统内同类旧阻塞文案和目录绑定校验残留。
- [x] 执行可安全运行的真实页面 E2E，并记录元数据编辑真实 E2E 权限阻塞。

## Expected Verification

- 后端 JUnit：NAS 转移/本地文件夹导入/元数据更新在类别未绑定目录时解析到唯一启用 `UNCLASSIFIED` 目录。
- 前端静态合同：NAS 管理页不再阻塞未绑定模板类别；元数据弹窗不再提示当前类别未绑定受控目录。
- 回归扫描：上传页的“文件分类叶子节点自动绑定类别、未绑定提交目录自动未分类、OnlyOffice 文档地址”契约保持通过。
- 真实页面 E2E：上传页未绑定提交目录只读链路 PASS；NAS 转移弹窗未绑定模板类别只读链路 PASS；元数据弹窗真实路径若当前账号缺 `doc_control` 入口，记录为前置阻塞，不以 API-only 替代。

## Current Status

ready_for_closeout

实现和聚焦验证已完成；任务收尾仍受全局 Git/推送规则和现有工作区大量无关脏改动约束，未标记 completed。

## Cleanup Keep

- doc/tasks/20260804-dcc-unclassified-directory-consistency/nas-unclassified-dialog-readonly.e2e.cjs
- doc/tasks/20260804-dcc-unclassified-directory-consistency/metadata-unclassified-dialog-readonly.e2e.cjs

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。未绑定类别时只解析正式唯一启用 `UNCLASSIFIED / 未分类` 目录；目录缺失或不唯一继续 fail fast。
- `是否从根因和长期维护角度解决`：是。复用已有 `DccUploadDirectoryResolver`，避免各入口各自实现目录绑定规则。
- `是否存在临时补丁或绕过`：否。
