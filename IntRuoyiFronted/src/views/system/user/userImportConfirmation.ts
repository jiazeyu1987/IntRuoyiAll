import { ElMessageBox } from 'element-plus'
import { h } from 'vue'

/** Only cancellation is handled here; unexpected dialog failures remain visible. */
export const confirmUserImport = async (
  file: Pick<File, 'name'>
): Promise<boolean> => {
  try {
    await ElMessageBox.confirm(h('div', { style: { overflowWrap: 'anywhere' } }, [
      h('p', `文件名称：${file.name}`)
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
