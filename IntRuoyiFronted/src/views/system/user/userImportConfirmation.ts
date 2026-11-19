import { ElMessageBox } from 'element-plus'
import { h } from 'vue'
import type { UserImportPreview } from '@/api/system/user'

/** Only cancellation is handled here; unexpected dialog failures remain visible. */
export const confirmUserImport = async (
  file: Pick<File, 'name'>,
  preview: UserImportPreview
): Promise<boolean> => {
  try {
    await ElMessageBox.confirm(h('div', { style: { overflowWrap: 'anywhere' } }, [
      h('p', `文件名称：${file.name}`),
      h('p', `人员记录：${preview.rowCount} 条；涉及组织：${preview.organizations.length} 个。`),
      h('div', { style: { maxHeight: '240px', overflowY: 'auto' } }, [
        h('p', '组织范围：'),
        ...preview.organizations.map(value => h('p', value)),
        h('p', '人员范围：'),
        ...preview.personnel.map(value => h('p', value))
      ])
    ]), '确认导入文件', {
      confirmButtonText: '确认导入', cancelButtonText: '取消',
      closeOnClickModal: false
    })
    return true
  } catch (error) {
    if (error === 'cancel' || error === 'close') return false
    throw error
  }
}
