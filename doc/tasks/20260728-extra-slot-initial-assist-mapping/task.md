# 损耗单与过程检验记录辅助映射初始化

## Task Goal

为“损耗单”和“过程检验记录”的最新版本初始化一个可测试的辅助模式映射。初始化只用于第一个测试版本，后续仍由用户在现有辅助映射界面手动调整。

## Milestones

- [ ] 核对目标表单槽位、最新版本、租户、用户和现有保存结构。
- [ ] 记录 BDD 和 RED 验证，确认初始化前缺少完整辅助模式映射。
- [ ] 使用现有 Jimu JSON `edhrAssistRows` 和 `mes_pro_edhr_process_form_permission_rule` scoped assignment 初始化。
- [ ] 验证每张目标表单辅助行覆盖完整、无重复、填写人数量符合签名单元格数且最少 1。

## Expected Verification

- 能定位损耗单最新版本与过程检验记录最新版本。
- 每张目标表单的辅助行覆盖全部可辅助填写单元格。
- 每张目标表单的 scoped fill assignments 与辅助行一一对应。
- 同一个原表单元格不重复分配。

## Current Status

in_progress

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；本任务使用现有辅助模式数据结构进行一次性初始化，不改变后续人工调整逻辑。
- `是否存在临时补丁或绕过`：否；写入前核对 schema、租户、版本和目标记录范围。

## Applicable Gates

- 批记录表单、损耗单、过程检验记录必须按 `form_slot_type` 区分，不得混用表单槽位和正式批记录字段。
- 写入型数据初始化必须备份目标 Jimu JSON 和权限规则。
- 不得记录明文密码、token 或数据库连接密钥。
