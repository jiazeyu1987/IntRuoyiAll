# Verification Report

## Summary

- PASS: 按用户新口径“权限角色小于 2 个”，已从 `芋道源码` 租户随机选择 30 个用户。
- PASS: 已创建业务角色 `PQC权限角色`，角色 ID `910438`，角色编码 `pqc_permission`。
- PASS: 已将 PQC 菜单链路 `5100,900220,900435,900438` 绑定到该角色。
- PASS: 已给 30 个目标用户绑定该 PQC 角色，复核无重复绑定、无无效用户。

## Role

- Tenant: `芋道源码`, ID `1`.
- Role: `910438 / PQC权限角色 / pqc_permission`.
- Role menu IDs: `5100,900220,900435,900438`.
- Role menu count: `4`.

## Assigned Users

- `178 hongqiuyu 洪秋雨`
- `283 huangannan 黄安南`
- `305 wangyicheng 汪意诚`
- `330 qinhan 秦晗`
- `382 chenhong 陈红`
- `408 xuchongling 许崇玲`
- `448 longqiaohong 龙巧红`
- `567 wanwen 万文`
- `593 fudengjuan 付登娟`
- `597 shiqianglin 石强琳`
- `690 songqingquan 宋青泉`
- `833 daiyanlin 戴言林`
- `919 wangjun2 王俊`
- `936 qiuyanxian 邱艳仙`
- `1019 zhangqin 张琴`
- `1076 wangyaling 王亚玲`
- `1192 caoxisong 曹晰淞`
- `1251 yangsheng 杨晟`
- `1440 renzongfang 任宗芳`
- `1500 chenwei 陈伟`
- `1538 wanghu 王虎`
- `1606 xiefengxia 谢凤霞`
- `1611 songlongmin 宋龙民`
- `1716 wuweiwen 吴伟文`
- `1820 yangfeng3 杨峰`
- `1905 liuhongtao 刘洪涛`
- `2037 wangfengling 王凤灵`
- `2076 gaoqingjun 高庆军`
- `2110 huxiaomin 胡晓敏`
- `2160 yangxiang 杨祥`

## Verification

- RED: original no-permission candidate check -> FAIL, no eligible users under the original criterion.
- RED: first mutation transaction -> FAIL before data mutation due collation mismatch; follow-up check showed role count 0 and inserted user-role count 0.
- GREEN: retry transaction with explicit `utf8mb4_unicode_ci` -> PASS; selected user count 30 and inserted user-role count 30.
- REGRESSION: role menu count `4`, assigned count `30`, invalid selected users `0`, duplicate bindings `0`.
- REGRESSION: all 30 selected users have PQC role and effective role count after binding equals `2`.

## Rollback

- Delete 30 `system_user_role` rows where `role_id = 910438`, `tenant_id = 1`, and `creator = "codex-20260806-pqc-role"`.
- Delete `system_role_menu` rows where `role_id = 910438`, `tenant_id = 1`, and `creator = "codex-20260806-pqc-role"`.
- Delete `system_role` row `id = 910438` only if rollback is requested and no non-task-owned bindings were added later.
