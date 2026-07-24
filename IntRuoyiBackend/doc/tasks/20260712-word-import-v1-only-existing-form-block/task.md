# Word 导入固定 V1 首版并阻断已有表单

## Task Goal

修复 eDHR 批记录 Word 导入按钮的版本管控：所有导入只生成 V1.0；如果产品/批记录已存在主表单，不允许再次导入，必须先删除已有表单；无主表单但存在历史业务引用时不允许重置为 V1.0。

## Milestones

1. 后端导入服务固定新建 V1.0，移除升版与 pending 复用路径。
2. 后端在已有 MAIN 表单时阻断导入，在历史业务引用仍存在时阻断版本重置。
3. 前端两个导入入口固定传 `upgrade=false`，已有主表单时提示先删除。
4. 完成后端、前端静态、本地重启与测试租户真实导入验证。

## Expected Verification

- 后端定向测试覆盖首次导入 V1.0、已有主表单阻断、历史执行引用阻断、版本表 schema。
- 前端静态测试覆盖固定 `upgrade=false` 和已有表单提示。
- 本地前后端重启后，测试租户真实导入首导为 V1.0，再导入被阻断，列表产品名称完整。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，导入口径收敛为首版导入，已有表单和历史引用均由后端显式校验。
- 是否存在临时补丁或绕过：否。

## Current Status

completed

## Verification Result

- GREEN: targeted backend v1-only tests -> PASS。
- GREEN: frontend static v1-only contract -> PASS。
- GREEN: local restart and real v1-only import -> PASS。
