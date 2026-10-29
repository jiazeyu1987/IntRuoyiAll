<template>
  <el-dialog
    v-model="visible"
    title="编辑参数"
    width="620px"
    destroy-on-close
    :close-on-click-modal="false"
  >
    <el-form label-position="top" @submit.prevent>
      <div class="parameter-grid">
        <el-form-item label="参数名称"
          ><el-input v-model="draft.name" aria-label="参数名称"
        /></el-form-item>
        <el-form-item label="显示名称"
          ><el-input v-model="draft.ui!.displayName" aria-label="参数显示名称"
        /></el-form-item>
        <el-form-item label="填写类型">
          <el-select v-model="draft.ui!.control" aria-label="填写类型" @change="changeControl">
            <el-option label="数字" value="number" /><el-option
              label="下拉选择"
              value="select"
            /><el-option label="文字" value="text" />
          </el-select>
        </el-form-item>
        <el-form-item label="单位"
          ><el-input v-model="draft.ui!.unit" aria-label="参数单位"
        /></el-form-item>
        <template v-if="draft.ui!.control === 'number'">
          <el-form-item label="下限"
            ><el-input v-model="minimum" aria-label="参数下限" clearable
          /></el-form-item>
          <el-form-item label="上限"
            ><el-input v-model="maximum" aria-label="参数上限" clearable
          /></el-form-item>
          <el-form-item label="步长"
            ><el-input v-model="step" aria-label="参数步长" clearable
          /></el-form-item>
        </template>
        <el-form-item label="默认值"
          ><el-input v-model="defaultValue" aria-label="参数默认值" clearable
        /></el-form-item>
      </div>
      <el-form-item v-if="draft.ui!.control === 'select'" label="下拉选项（每行一项）">
        <el-input v-model="options" type="textarea" :rows="4" aria-label="下拉选项" />
      </el-form-item>
      <el-form-item label="原文参考值"
        ><el-input v-model="draft.referenceValue" aria-label="原文参考值"
      /></el-form-item>
      <el-form-item label="参数归属">
        <el-select v-model="target" aria-label="参数归属">
          <el-option
            v-for="item in targets"
            :key="item.value"
            :value="item.value"
            :label="item.label"
          />
        </el-select>
      </el-form-item>
      <el-alert
        title="此处修改模板设置；试填数据不会写入模板。数字上下限修改后会同步更新参考值。"
        type="info"
        :closable="false"
      />
      <el-alert v-if="error" :title="error" type="error" :closable="false" class="mt-3" />
    </el-form>
    <template #footer
      ><el-button @click="visible = false">取消</el-button
      ><el-button type="primary" @click="save">保存参数</el-button></template
    >
  </el-dialog>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import type { BatchRecordTotalRecognitionParameter as Parameter } from '../productionRecognition'
import { checkParameter, clone } from '../simpleRecognition'
defineProps<{ targets: { value: string; label: string }[] }>()
const emit = defineEmits<{ save: [parameter: Parameter, target: string] }>()
const visible = ref(false)
const draft = ref<Parameter>({ ui: {} })
const original = ref<Parameter>()
const minimum = ref(''),
  maximum = ref(''),
  step = ref(''),
  defaultValue = ref(''),
  options = ref(''),
  target = ref(''),
  error = ref('')
const asText = (value: unknown) => (value == null ? '' : String(value))
function open(parameter: Parameter, location: string) {
  draft.value = clone(parameter)
  draft.value.ui ??= {}
  original.value = clone(parameter)
  minimum.value = asText(parameter.ui?.min)
  maximum.value = asText(parameter.ui?.max)
  step.value = asText(parameter.ui?.step)
  defaultValue.value = asText(parameter.ui?.defaultValue)
  options.value = parameter.ui?.options?.map(asText).join('\n') ?? ''
  target.value = location
  error.value = ''
  visible.value = true
}
function changeControl() {
  minimum.value = maximum.value = step.value = defaultValue.value = options.value = ''
  delete draft.value.ui!.min
  delete draft.value.ui!.max
  delete draft.value.ui!.step
  delete draft.value.ui!.options
}
function save() {
  const result = clone(draft.value)
  result.name = result.name?.trim()
  const ui = result.ui!
  const numeric = (value: string) => (value.trim() === '' ? undefined : Number(value))
  ui.defaultValue = ui.control === 'number' ? numeric(defaultValue.value) : defaultValue.value
  if (ui.control === 'number') {
    ui.min = numeric(minimum.value)
    ui.max = numeric(maximum.value)
    ui.step = numeric(step.value)
    if (
      asText(ui.min) !== asText(original.value?.ui?.min) ||
      asText(ui.max) !== asText(original.value?.ui?.max) ||
      ui.unit !== original.value?.ui?.unit
    ) {
      result.referenceValue =
        [ui.min == null ? '' : `≥ ${ui.min}`, ui.max == null ? '' : `≤ ${ui.max}`]
          .filter(Boolean)
          .join('，') + (ui.unit ? ` ${ui.unit}` : '')
    }
  }
  if (ui.control === 'select') {
    // Preserve numeric option types when editing an existing numeric option.
    ui.options = options.value
      .split('\n')
      .map((v) => v.trim())
      .filter(Boolean)
      .map((v) => {
        const existing = original.value?.ui?.options?.find((item) => String(item) === v)
        return existing === undefined ? v : existing
      })
    const selected = ui.options.find((v) => String(v) === defaultValue.value)
    if (selected !== undefined) ui.defaultValue = selected
  }
  const errors = checkParameter(result)
  if (errors.length) {
    error.value = errors.join('；')
    return
  }
  if (!target.value) {
    error.value = '请选择参数归属'
    return
  }
  emit('save', result, target.value)
  visible.value = false
}
defineExpose({ open })
</script>
<style scoped>
.parameter-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 18px;
}
.el-select {
  width: 100%;
}
</style>
