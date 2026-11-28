<template>
  <span v-if="label">{{ label }}</span>
  <span v-else-if="!sourceValue">未记录</span>
  <el-popover v-else trigger="click" title="源系统技术详情" width="320">
    <p>该字段尚未配置可识别的中文含义，请联系管理员核对。</p>
    <p class="break-all">源系统原值：{{ sourceValue }}</p>
    <template #reference>
      <el-button link type="danger">未识别的{{ fieldName }}</el-button>
    </template>
  </el-popover>
</template>

<script setup lang="ts">
const props = defineProps<{
  value?: string | number | null
  field: 'documentStatus' | 'childMaterialType' | 'issueMethod' | 'transferDirect'
}>()

// 单据状态沿用 ActiveOrderReplenishmentList 中已有的金蝶状态定义。
// 发料方式 1 沿用生产用料清单已有定义；不推测其余源系统枚举。
const labels: Record<string, Record<string, string>> = {
  documentStatus: { A: '创建', B: '审核中', C: '已审核', D: '重新审核' },
  issueMethod: { '1': '直接领料' },
  childMaterialType: {},
  transferDirect: {}
}
const fieldNames = {
  documentStatus: '业务状态',
  childMaterialType: '子项类型',
  issueMethod: '发料方式',
  transferDirect: '调拨方向'
}
const sourceValue = computed(() => String(props.value ?? '').trim())
const fieldName = computed(() => fieldNames[props.field])
const label = computed(() => {
  const value = sourceValue.value
  if (Object.prototype.hasOwnProperty.call(labels[props.field], value)) {
    return labels[props.field][value]
  }
  // 接口本身也可能返回“标准件”“直接领料”等已翻译的业务文本。
  return /[\u4e00-\u9fff]/.test(value) ? value : ''
})
</script>
