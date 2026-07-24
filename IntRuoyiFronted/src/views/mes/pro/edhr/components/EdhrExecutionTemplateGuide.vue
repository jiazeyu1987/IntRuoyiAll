<template>
  <div class="edhr-template-guide">
    <el-alert
      v-if="parseError"
      :title="parseError"
      type="error"
      :closable="false"
      show-icon
      class="edhr-template-guide__alert"
    />

    <template v-else>
      <section class="edhr-template-guide__summary">
        <el-tag type="primary">填写单元格 {{ summary.fillableCount }}</el-tag>
        <el-tag type="success">签名位 {{ summary.signatureCount }}</el-tag>
        <el-tag type="warning">必填 {{ summary.requiredCount }}</el-tag>
        <el-tag type="info">附件规则 {{ summary.attachmentRuleCount }}</el-tag>
      </section>

      <div class="edhr-template-guide__sheet-wrap">
        <table class="edhr-template-guide__sheet">
          <tbody>
            <tr v-for="row in renderedRows" :key="row.rowIndex">
              <td
                v-for="cell in row.cells"
                :key="cell.identity"
                :rowspan="cell.rowSpan"
                :colspan="cell.colSpan"
                :class="cell.classNames"
              >
                <span class="edhr-template-guide__text">{{ cell.primaryText }}</span>
                <span v-if="cell.ruleHint" class="edhr-template-guide__hint">{{ cell.ruleHint }}</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import type {
  BatchRecordReportCellRuleVO,
  BatchRecordReportSignatureCellMarkerVO
} from '@/api/mes/pro/batchrecordreport'
import {
  formatTemplateAttachmentRule,
  cleanedAttachmentRule,
  normalizeCellRule,
  normalizeTemplateCellMerge,
  resolveTemplateSignatureActionLabel,
  stringifyTemplateCell,
  templateGuideValueTypeLabels,
  type TemplateRawCell,
  type TemplateRawLayout
} from '@/views/mes/pro/batchrecord-shared/batchRecordTemplateRules'

defineOptions({ name: 'EdhrExecutionTemplateGuide' })

// 默认签名提示保留：复核签名 / 提交签名 / 审批签名。

type RenderedCell = {
  identity: string
  rowSpan: number
  colSpan: number
  primaryText: string
  ruleHint: string
  classNames: Record<string, boolean>
}

type RenderedRow = {
  rowIndex: number
  cells: RenderedCell[]
}

const props = defineProps<{
  sheetLayoutJson?: string
  cellRules?: BatchRecordReportCellRuleVO[]
  signatureMarkers?: BatchRecordReportSignatureCellMarkerVO[]
}>()

const parseError = ref('')

const parseJson = <T,>(raw: string | undefined, label: string): T | undefined => {
  if (!raw?.trim()) return undefined
  try {
    return JSON.parse(raw) as T
  } catch (error) {
    const message = error instanceof Error ? error.message : '未知错误'
    throw new Error(`${label} 解析失败：${message}`)
  }
}

const layout = computed(() => {
  parseError.value = ''
  try {
    const parsed = parseJson<TemplateRawLayout>(props.sheetLayoutJson, '模板布局')
    if (!parsed?.rows || !Object.keys(parsed.rows).length) {
      parseError.value = '缺少电子批记录模板布局，无法显示模板说明。'
      return undefined
    }
    return parsed
  } catch (error) {
    parseError.value = error instanceof Error ? error.message : '模板布局解析失败。'
    return undefined
  }
})

const normalizedRules = computed(() => {
  const rules = (props.cellRules || []).map(normalizeCellRule)
  if (!rules.length && !parseError.value) {
    parseError.value = '模板缺少单元格规则，无法显示模板说明。'
  }
  return rules
})

const ruleMap = computed(() => {
  const map = new Map<string, BatchRecordReportCellRuleVO>()
  normalizedRules.value.forEach((rule) => {
    map.set(`${rule.rowIndex}:${rule.columnIndex}`, rule)
  })
  return map
})

const markerMap = computed(() => {
  const map = new Map<string, BatchRecordReportSignatureCellMarkerVO>()
  ;(props.signatureMarkers || []).forEach((marker) => {
    if (!marker.enabled) return
    map.set(`${marker.rowIndex}:${marker.columnIndex}`, marker)
  })
  return map
})

const rowIndexes = computed(() => {
  const rows = layout.value?.rows || {}
  return Object.keys(rows)
    .map((key) => Number(key))
    .filter((key) => Number.isInteger(key))
    .sort((a, b) => a - b)
})

const columnIndexes = computed(() => {
  const set = new Set<number>()
  Object.values(layout.value?.rows || {}).forEach((row) => {
    Object.keys(row.cells || {}).forEach((columnKey) => {
      const columnIndex = Number(columnKey)
      if (Number.isInteger(columnIndex)) set.add(columnIndex)
    })
  })
  normalizedRules.value.forEach((rule) => set.add(rule.columnIndex))
  ;(props.signatureMarkers || []).forEach((marker) => set.add(marker.columnIndex))
  return Array.from(set).sort((a, b) => a - b)
})

const coveredSet = computed(() => {
  const covered = new Set<string>()
  Object.entries(layout.value?.rows || {}).forEach(([rowKey, row]) => {
    const rowIndex = Number(rowKey)
    if (!Number.isInteger(rowIndex)) return
    Object.entries(row.cells || {}).forEach(([columnKey, cell]) => {
      const columnIndex = Number(columnKey)
      if (!Number.isInteger(columnIndex)) return
      const merge = normalizeTemplateCellMerge(cell)
      for (let rowOffset = 0; rowOffset < merge.rowSpan; rowOffset += 1) {
        for (let columnOffset = 0; columnOffset < merge.colSpan; columnOffset += 1) {
          if (rowOffset === 0 && columnOffset === 0) continue
          covered.add(`${rowIndex + rowOffset}:${columnIndex + columnOffset}`)
        }
      }
    })
  })
  return covered
})

const resolveRuleHint = (
  rule: BatchRecordReportCellRuleVO | undefined,
  marker: BatchRecordReportSignatureCellMarkerVO | undefined
) => {
  const parts: string[] = []
  if (marker) {
    parts.push(resolveTemplateSignatureActionLabel(marker))
  }
  const attachmentRule = cleanedAttachmentRule(rule?.attachmentRule)
  if (attachmentRule) {
    parts.push('附件')
    if (attachmentRule.required) parts.push('必填')
    if (attachmentRule.minCount && attachmentRule.minCount > 0) parts.push(`至少 ${attachmentRule.minCount} 个`)
    if (attachmentRule.maxCount && attachmentRule.maxCount > 0) parts.push(`最多 ${attachmentRule.maxCount} 个`)
    const attachmentText = formatTemplateAttachmentRule(attachmentRule)
    if (attachmentText) {
      const compactAttachmentText = attachmentText
        .replace('必需附件 / ', '')
        .replace('可选附件 / ', '')
        .replace('必需附件', '')
        .replace('可选附件', '')
        .trim()
      if (compactAttachmentText) {
        compactAttachmentText.split(' / ').forEach((part) => parts.push(part))
      }
    }
  }
  if (!rule) {
    return parts.join(' / ')
  }
  if (!marker) {
    parts.unshift(templateGuideValueTypeLabels[rule.valueType] || '文字')
  }
  if (rule.required) parts.push('必填')
  if (rule.unit) parts.push(`单位 ${rule.unit}`)
  if (rule.valueType === 'DATE' || rule.valueType === 'DATETIME') {
    const format = typeof rule.constraints?.format === 'string' ? rule.constraints.format.trim() : ''
    if (format) parts.push(`格式 ${format}`)
  }
  return parts.join(' / ')
}

const resolvePrimaryText = (
  rawCell: TemplateRawCell | undefined,
  rule: BatchRecordReportCellRuleVO | undefined,
  marker: BatchRecordReportSignatureCellMarkerVO | undefined
) => {
  if (marker) return resolveTemplateSignatureActionLabel(marker)
  if (rule) return templateGuideValueTypeLabels[rule.valueType] || '文字'
  return stringifyTemplateCell(rawCell?.value ?? rawCell?.text)
}

const renderedRows = computed<RenderedRow[]>(() => {
  const rows = layout.value?.rows || {}
  return rowIndexes.value.map((rowIndex) => {
    const rawRow = rows[String(rowIndex)] || {}
    const cells: RenderedCell[] = []
    columnIndexes.value.forEach((columnIndex) => {
      if (coveredSet.value.has(`${rowIndex}:${columnIndex}`)) return
      const rawCell = rawRow.cells?.[String(columnIndex)]
      const merge = normalizeTemplateCellMerge(rawCell)
      const rule = ruleMap.value.get(`${rowIndex}:${columnIndex}`)
      const marker = markerMap.value.get(`${rowIndex}:${columnIndex}`)
      const primaryText = resolvePrimaryText(rawCell, rule, marker)
      const ruleHint = resolveRuleHint(rule, marker)
      cells.push({
        identity: `${rowIndex}:${columnIndex}`,
        rowSpan: merge.rowSpan,
        colSpan: merge.colSpan,
        primaryText,
        ruleHint,
        classNames: {
          'edhr-template-guide__cell': true,
          'is-empty': !primaryText,
          'is-static': !rule && !marker,
          'is-fillable': Boolean(rule),
          'is-signature': Boolean(marker),
          'has-attachment': Boolean(cleanedAttachmentRule(rule?.attachmentRule))
        }
      })
    })
    return { rowIndex, cells }
  })
})

const summary = computed(() => {
  const fillableCount = normalizedRules.value.length
  const signatureCount = (props.signatureMarkers || []).filter((marker) => marker.enabled).length
  const requiredCount = normalizedRules.value.filter((rule) => rule.required).length
  const attachmentRuleCount = normalizedRules.value.filter((rule) => cleanedAttachmentRule(rule.attachmentRule)).length
  return { fillableCount, signatureCount, requiredCount, attachmentRuleCount }
})
</script>

<style scoped>
.edhr-template-guide {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.edhr-template-guide__alert {
  margin-bottom: 0;
}

.edhr-template-guide__summary {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.edhr-template-guide__sheet-wrap {
  overflow: auto;
  border: 1px solid #dbe3ef;
  border-radius: 6px;
  background: #ffffff;
}

.edhr-template-guide__sheet {
  width: 100%;
  min-width: 960px;
  border-collapse: collapse;
  table-layout: fixed;
}

.edhr-template-guide__cell {
  border: 1px solid #dbe3ef;
  padding: 8px 6px;
  vertical-align: middle;
  text-align: center;
  white-space: pre-wrap;
  word-break: break-word;
}

.edhr-template-guide__cell.is-static {
  background: #f7f9fc;
  color: #263247;
  font-weight: 600;
}

.edhr-template-guide__cell.is-fillable {
  background: #fafcff;
  color: #1677ff;
  font-weight: 700;
}

.edhr-template-guide__cell.is-signature {
  background: #eefcf9;
  color: #0f766e;
  font-weight: 700;
}

.edhr-template-guide__cell.has-attachment {
  box-shadow: inset 0 0 0 1px rgba(245, 158, 11, 0.35);
}

.edhr-template-guide__cell.is-empty {
  color: #9ca3af;
}

.edhr-template-guide__text {
  display: block;
  min-height: 18px;
}

.edhr-template-guide__hint {
  display: block;
  margin-top: 4px;
  color: #4b5563;
  font-size: 12px;
  line-height: 1.45;
}
</style>
