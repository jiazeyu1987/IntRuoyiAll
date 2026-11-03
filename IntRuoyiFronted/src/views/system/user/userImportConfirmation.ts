import { ElMessageBox } from 'element-plus'
import { h } from 'vue'
import type { UserImportPreview } from '@/api/system/user'
import { useUserStore } from '@/store/modules/user'

/** Only cancellation is handled here; unexpected dialog failures remain visible. */
export const confirmUserImport = async (
  filename: string,
  mode: 'create' | 'update' | 'dingtalk',
  tenant: number | string,
  preview: UserImportPreview
): Promise<boolean> => {
  const user = useUserStore().getUser
  const impact = mode === 'dingtalk'
    ? '将按文件创建用户、新建或启用组织，并根据部门主管信息回填部门负责人；具体失败及未回填原因在导入结果中展示。'
    : mode === 'update'
      ? '已开启覆盖更新：将创建新用户，并更新文件中已存在的用户资料及组织关联。'
      : '未开启覆盖更新：将创建新用户，已存在的用户不执行覆盖更新。'
  try {
    await ElMessageBox.confirm(h('div', { style: { overflowWrap: 'anywhere' } }, [
      h('p', `文件：${filename}`),
      h('p', `当前操作租户 ID：${tenant}；操作用户：${user.username}；所属部门 ID：${user.deptId}。`),
      h('p', '导入作用于当前租户内的用户及文件指定组织，不受用户列表的部门筛选限制。实际可操作范围由服务端权限校验。'),
      h('p', impact),
      h('p', `已解析文件 ${preview.rowCount} 条记录。以下为文件实际人员及组织范围，最终写入仍需服务端重新校验，不承诺全部成功。`),
      h('details', { open: true }, [h('summary', '人员标识'),
        h('div', { style: { maxHeight: '180px', overflowY: 'auto', whiteSpace: 'pre-wrap' } }, preview.personnel.join('\n'))]),
      h('details', { open: true }, [h('summary', '组织路径范围'),
        h('div', { style: { maxHeight: '180px', overflowY: 'auto', whiteSpace: 'pre-wrap' } }, preview.organizations.join('\n'))]),
      h('p', '确认后将写入数据，取消不会提交导入。')
    ]), '确认导入及变更影响', {
      type: 'warning', confirmButtonText: '确认导入', cancelButtonText: '取消',
      closeOnClickModal: false
    })
    return true
  } catch (error) {
    if (error === 'cancel' || error === 'close') return false
    throw error
  }
}
