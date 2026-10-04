<template>
  <div class="dcc-project-attributes">
    <el-alert v-if="historicalMissing" title="历史未记录" type="info" :closable="false" />
    <template v-else>
      <el-form-item label="目标市场" required>
        <el-checkbox-group :model-value="modelValue.targetMarkets" :disabled="readonly" @update:model-value="marketsChanged">
          <el-checkbox v-for="item in markets" :key="item.value" :value="item.value">{{ item.label }}</el-checkbox>
        </el-checkbox-group>
      </el-form-item>
      <el-form-item v-if="modelValue.targetMarkets.includes('OTHER')" label="其他市场说明" required>
        <el-input :model-value="modelValue.otherMarket" :disabled="readonly" maxlength="512" @update:model-value="setField('otherMarket', $event)" />
      </el-form-item>
      <el-form-item v-for="field in identities" :key="field.value" :label="field.label" required>
        <el-radio-group :model-value="modelValue[field.value]" :disabled="readonly" @update:model-value="setField(field.value, String($event))">
          <el-radio value="Y">是</el-radio><el-radio value="N">否</el-radio><el-radio value="NA">不适用</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="文件转移" required>
        <el-radio-group :model-value="modelValue.documentTransfer" :disabled="readonly" @update:model-value="transferChanged">
          <el-radio value="Y">是</el-radio><el-radio value="N">否</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="modelValue.documentTransfer === 'Y'" label="转移至" required>
        <el-input :model-value="modelValue.transferTo" :disabled="readonly" maxlength="512" @update:model-value="setField('transferTo', $event)" />
      </el-form-item>
    </template>
  </div>
</template>
<script setup lang="ts">
import { changeMarkets, changeTransfer, type ProjectAttributes } from './state'
const props = defineProps<{ modelValue: ProjectAttributes; readonly?: boolean; historicalMissing?: boolean }>()
const emit = defineEmits<{ 'update:modelValue': [value: ProjectAttributes] }>()
const markets = [
  { value: 'NMPA', label: 'NMPA 国内' }, { value: 'CE', label: 'CE 欧盟' },
  { value: 'FDA', label: 'FDA' }, { value: 'MADSAP', label: 'MADSAP' },
  { value: 'OTHER', label: '其他' }, { value: 'NA', label: '不适用' }
]
const identities = [
  { value: 'licenseHolder' as const, label: '是否为注册人' },
  { value: 'actualManufacturer' as const, label: '是否为生产方' }
]
const setField = (key: keyof ProjectAttributes, value: string) => {
  if (props.readonly || props.historicalMissing) return
  emit('update:modelValue', { ...props.modelValue, [key]: value })
}
const marketsChanged = (value: (string | number | boolean)[]) => {
  if (props.readonly || props.historicalMissing) return
  emit('update:modelValue', changeMarkets(props.modelValue, value.map(String)))
}
const transferChanged = (value: string | number | boolean | undefined) => {
  if (props.readonly || props.historicalMissing) return
  emit('update:modelValue', changeTransfer(props.modelValue, String(value)))
}
</script>
