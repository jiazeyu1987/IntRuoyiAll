# 测试服 wangsiyu 受控浏览空列表验证报告

## 结论

测试服 `wangsiyu` 无法查看 `作废保留` 目录中的文件，根因不是目录没有文件，也不是前端没有文控页签，而是文件所属类别 `其他`（`category_id=906104`）没有任何启用的 DCC 查看矩阵规则。

当前账号只有页面入口权限。受控浏览对普通 ACTIVE 文件还会执行文件级可见性判断：申请人本人、目录管理员或当前类别查看矩阵命中三者之一。该文件申请人为 `1`，`wangsiyu` 为 `910250`；账号没有目录管理权限；类别查看矩阵启用规则数为 `0`，因此文件被后端过滤。

## 证据范围

- 环境：测试服务器 `172.30.30.58`
- 租户：`tenant_id=1`
- 账号标签：`wangsiyu`，用户 ID `910250`
- 目录：`质量管理/1. QMS documents/3 RE 扫描件/作废保留`，目录 ID `909083`
- 操作范围：只读 schema/数据核查
- 未执行：MySQL 写入、Redis 清理、代码修改、服务重启、发布

## 数据证据

| 检查项 | 结果 |
| --- | --- |
| 有效目录 | `909083`，active 且未删除 |
| 目录及子目录文件数 | `1` |
| 当前文件 | `2054545668044051396` |
| 文件状态 | `ACTIVE` |
| 发布文件 | `published_file_id=9198354889671` |
| 当前版本指针 | `dcc_controlled_file_master.current_active_controlled_file_id=2054545668044051396` |
| 文件类别 | `其他`，`category_id=906104` |
| 文件申请人 | `requester_id=1` |
| wangsiyu 当前有效角色 | `approval_center_entry(910295)`、`wenkong_no_download(910417)` |
| 目录管理/下载危险权限 | `0` |
| 类别启用查看矩阵规则 | `0` |

## 代码依据

- [DccControlledFileMapper.java](/E:/IntRuoyi/IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/mysql/file/DccControlledFileMapper.java:370)：
  `latestVersionOnly=true` 且未指定状态时，浏览候选只取 `ACTIVE`。
- [DccControlledFileQueryServiceImpl.java](/E:/IntRuoyi/IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java:344)：
  候选文件还要经过 `canAccessQuery`，随后聚合当前版本。
- [DccControlledFileQueryServiceImpl.java](/E:/IntRuoyi/IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java:989)：
  普通浏览范围要求申请人本人、当前阶段参与人或当前查看矩阵命中。
- [DccControlledFileViewMatrixAccessService.java](/E:/IntRuoyi/IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileViewMatrixAccessService.java:97)：
  类别没有启用查看矩阵规则时返回 `VIEW_MATRIX_NOT_CONFIGURED`，无法解析出可查看用户。
- [index.vue](/E:/IntRuoyi/IntRuoyiFronted/src/views/dcc/controlled-file/browser/index.vue:2330)：
  前端受控浏览固定请求 `latestVersionOnly=true`；选择目录时 `includeDescendantDirectories=false`，本次目录下没有子目录，因此不是空列表原因。

## 最小修复建议

应在 DCC 类别管理中为 `其他(906104)` 配置并启用正式查看矩阵规则，按业务要求绑定用户、部门、岗位或角色；如果只允许 `wangsiyu` 查看，应绑定其用户/部门/角色到该类别的查看矩阵，而不是扩大菜单角色或恢复下载角色。

本次没有执行上述修复，避免在未确认业务可见范围前扩大文件查看范围。

## 验证状态

- 只读 schema 核对：PASS
- 目录/ACTIVE/发布文件/master 当前版本核对：PASS
- 用户有效角色与危险权限核对：PASS
- 类别查看矩阵命中核对：PASS，规则数为 `0`，根因成立
- 真实页面复验：本次未执行；本报告基于已授权测试库只读证据和源码链路，不能替代 Playwright 页面验收
- 任务清理预览与应用：PASS，无删除项、阻塞项或警告
