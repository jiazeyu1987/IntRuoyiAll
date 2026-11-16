import { h } from 'vue'
import { ElMessageBox } from 'element-plus'
import ImportResultContent from '@/components/ImportResultContent/ImportResultContent.vue'

export interface ImportResultSection {
  title: string
  columns: string[]
  rows: string[][]
  expanded?: boolean
}

export interface ImportResultOptions {
  title: string
  summary: string
  stats: { label: string; value: number }[]
  sections?: ImportResultSection[]
  guidance?: string
  warning?: boolean
}

export const showImportResult = (result: ImportResultOptions) => {
  return ElMessageBox.alert(h(ImportResultContent, { result }), result.title, {
    confirmButtonText: '知道了',
    type: result.warning ? 'warning' : 'info',
    customClass: 'import-result-message-box'
  })
}

export const showRecordImportResult = (result: {
  entity: string
  created: string[]
  updated: string[]
  failures: Record<string, string>
  stats?: ImportResultOptions['stats']
  sections?: ImportResultSection[]
}) => {
  const failures = Object.entries(result.failures)
  const successCount = result.created.length + result.updated.length
  const summary = [
    result.created.length ? `新增 ${result.created.length} 条` : '',
    result.updated.length ? `更新 ${result.updated.length} 条` : '',
    failures.length ? `失败 ${failures.length} 条` : ''
  ].filter(Boolean)
  return showImportResult({
    title: failures.length ? (successCount ? '部分记录导入失败' : '导入失败') : '导入完成',
    summary: summary.length ? `本次${summary.join('，')}。` : '本次没有新增、更新或失败的记录。',
    warning: failures.length > 0,
    stats: [
      { label: '新增记录', value: result.created.length },
      { label: '更新记录', value: result.updated.length },
      { label: '导入失败', value: failures.length },
      ...(result.stats ?? [])
    ],
    sections: [
      { title: '失败记录', columns: [result.entity, '失败原因'], rows: failures, expanded: true },
      { title: '新增记录', columns: [result.entity], rows: result.created.map((value) => [value]) },
      { title: '更新记录', columns: [result.entity], rows: result.updated.map((value) => [value]) },
      ...(result.sections ?? [])
    ],
    guidance: failures.length
      ? '请根据失败原因修改对应记录后重新导入，避免重复提交已成功的记录。'
      : undefined
  })
}
