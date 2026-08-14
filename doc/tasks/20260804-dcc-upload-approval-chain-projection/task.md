# DCC 上传审批链路投影修复

## Task Goal

修复 DCC 受控文件上传页选择“技术调研报告”等已配置审批矩阵的文件类别时，预检仍提示“审批链路不完整”的问题。根因聚焦在文件类别列表接口未向前端投影当前有效审批矩阵的会签/批准岗位 ID。

## Milestones

- [x] 记录问题现象、适用门禁和预期行为
- [x] 增加控制器级 RED 回归测试，证明 `/dcc/file-categories` 需返回审批矩阵岗位投影
- [x] 实现最小后端修复，复用当前有效审批矩阵路线节点生成 `signoffPositionIds` / `approvalPositionIds`
- [x] 运行定向 Maven 验证并记录 GREEN 证据
- [x] 运行真实前端路径 E2E，验证“技术调研报告”上传预检不再误报审批链路不完整

## Expected Verification

- `mvn -pl yudao-module-dcc -am "-Dtest=DccFileCategoryControllerConfigPackageContractTest#getCategoryList_projectsActiveApprovalMatrixPositionIds,DccCategoryApprovalMatrixAdminServiceImplTest#getActiveMatrixPositionIdsByCategoryIds_readsLatestActiveRoutePositionNodes" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `mvn -pl yudao-module-dcc -am "-Dtest=DccFileCategoryControllerConfigPackageContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `node tests\e2e\dcc-upload-approval-chain-projection-real.e2e.js`
- 必要时复跑同测试类相邻用例，确认现有 `canUpload` / 目录绑定投影不回归。

## Applicable Gates

- DCC 上传类别权限投影门禁：类别列表接口必须返回当前用户类别级投影；上传页不得靠前端兜底展示无权限或未配置类别。
- DCC 文控审批处理入口门禁：DCC 上传审批链路必须来自真实 DCC 配置，不得用 BPM 原生审批、API-only 或默认审批人冒充链路完整。
- Maven Reactor 兄弟模块验证门禁：DCC 模块定向测试使用 `-pl yudao-module-dcc -am` 构建依赖模块。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；未配置当前有效审批矩阵时仍保持空链路，由前端继续提示不完整。
- `是否从根因和长期维护角度解决`：是；在后端文件类别列表正式投影当前有效审批矩阵节点，而不是改前端文案或硬编码类别。
- `是否存在临时补丁或绕过`：否。

## Current Status

ready_for_closeout

## Closeout Notes

- 实现、定向 Maven 验证和真实前端路径 E2E 均已完成。
- E2E 前发现 48081 仍运行旧 Jar，已按本地运行态门禁复制旧运行 Jar 并仅替换本任务 DCC class，重启到新 PID 72116 后完成复验。
- 未执行提交/推送：当前工作区在本任务开始前已有大量无关脏改动且分支 `int_main` ahead 8，本任务未混入或回滚无关改动。
