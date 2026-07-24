<template>
  <el-form-item
    :rules="[{ required: true, message: '请选择数据类型', trigger: 'change' }]"
    label="数据类型"
    prop="property.dataType"
  >
    <el-select v-model="property.dataType" placeholder="请选择数据类型" @change="handleChange">
      <el-option
        v-for="option in getDataTypeOptions2"
        :key="option.value"
        :label="`${option.value}(${option.label})`"
        :value="option.value"
      />
    </el-select>
  </el-form-item>

  <ThingModelNumberDataSpecs v-if="isNumberDataType" v-model="propertyDataSpecs" />

  <ThingModelEnumDataSpecs
    v-if="property.dataType === IoTDataSpecsDataTypeEnum.ENUM"
    v-model="propertyDataSpecsList"
  />

  <el-form-item v-if="property.dataType === IoTDataSpecsDataTypeEnum.BOOL" label="布尔值">
    <template v-for="(item, index) in propertyDataSpecsList" :key="item.value">
      <div class="mb-5px flex w-1/1 items-center justify-start">
        <span>{{ item.value }}</span>
        <span class="mx-2">-</span>
        <el-form-item
          :prop="`property.dataSpecsList[${index}].name`"
          :rules="[
            { required: true, message: '枚举描述不能为空' },
            { validator: validateBoolName, trigger: 'blur' }
          ]"
          class="mb-0 flex-1"
        >
          <el-input
            v-model="item.name"
            :placeholder="`如：${item.value === 0 ? '关' : '开'}`"
            class="w-255px!"
          />
        </el-form-item>
      </div>
    </template>
  </el-form-item>

  <el-form-item
    v-if="property.dataType === IoTDataSpecsDataTypeEnum.TEXT"
    label="数据长度"
    prop="property.dataSpecs.length"
  >
    <el-input v-model="propertyDataSpecs.length" class="w-255px!" placeholder="请输入文本字节长度">
      <template #append>字节</template>
    </el-input>
  </el-form-item>

  <el-form-item
    v-if="property.dataType === IoTDataSpecsDataTypeEnum.DATE"
    label="时间格式"
    prop="date"
  >
    <el-input class="w-255px!" disabled placeholder="String 类型的 UTC 时间戳（毫秒）" />
  </el-form-item>

  <ThingModelArrayDataSpecs
    v-if="property.dataType === IoTDataSpecsDataTypeEnum.ARRAY"
    v-model="propertyDataSpecs"
  />

  <ThingModelStructDataSpecs
    v-if="property.dataType === IoTDataSpecsDataTypeEnum.STRUCT"
    v-model="propertyDataSpecsList"
  />

  <el-form-item v-if="!isStructDataSpecs && !isParams" label="读写类型" prop="property.accessMode">
    <el-radio-group v-model="property.accessMode">
      <el-radio
        v-for="accessMode in Object.values(IoTThingModelAccessModeEnum)"
        :key="accessMode.value"
        :label="accessMode.value"
      >
        {{ accessMode.label }}
      </el-radio>
    </el-radio-group>
  </el-form-item>
</template>

<script lang="ts" setup>
import { useVModel } from '@vueuse/core'
import {
  ThingModelArrayDataSpecs,
  ThingModelEnumDataSpecs,
  ThingModelNumberDataSpecs,
  ThingModelStructDataSpecs
} from './dataSpecs'
import { ThingModelProperty, validateBoolName } from '@/api/iot/thingmodel'
import { isEmpty } from '@/utils/is'
import {
  getDataTypeOptions,
  IoTDataSpecsDataTypeEnum,
  IoTThingModelAccessModeEnum
} from '@/views/iot/utils/constants'

defineOptions({ name: 'ThingModelProperty' })

type ThingModelDataType =
  (typeof IoTDataSpecsDataTypeEnum)[keyof typeof IoTDataSpecsDataTypeEnum]

type ThingModelPropertyForm = Partial<Omit<ThingModelProperty, 'dataSpecs' | 'dataSpecsList'>> & {
  dataSpecs?: Record<string, any>
  dataSpecsList?: Array<Record<string, any>>
}

const numberDataTypes = [
  IoTDataSpecsDataTypeEnum.INT,
  IoTDataSpecsDataTypeEnum.DOUBLE,
  IoTDataSpecsDataTypeEnum.FLOAT
] as const
const dataSpecsListOnlyTypes: ThingModelDataType[] = [
  IoTDataSpecsDataTypeEnum.ENUM,
  IoTDataSpecsDataTypeEnum.BOOL,
  IoTDataSpecsDataTypeEnum.STRUCT
]

const props = defineProps<{
  modelValue: ThingModelPropertyForm
  isStructDataSpecs?: boolean
  isParams?: boolean
}>()
const emits = defineEmits(['update:modelValue'])
const property = useVModel(props, 'modelValue', emits) as Ref<ThingModelPropertyForm>
const propertyDataSpecs = computed<Record<string, any>>({
  get() {
    return property.value.dataSpecs ?? (property.value.dataSpecs = {})
  },
  set(value) {
    property.value.dataSpecs = value
  }
})
const propertyDataSpecsList = computed<Array<Record<string, any>>>({
  get() {
    return property.value.dataSpecsList ?? (property.value.dataSpecsList = [])
  },
  set(value) {
    property.value.dataSpecsList = value
  }
})
const isNumberDataType = computed(() =>
  numberDataTypes.includes(property.value.dataType as (typeof numberDataTypes)[number])
)
const getDataTypeOptions2 = computed(() => {
  if (!props.isStructDataSpecs) {
    return getDataTypeOptions()
  }
  const excludedTypes = [IoTDataSpecsDataTypeEnum.STRUCT, IoTDataSpecsDataTypeEnum.ARRAY]
  return getDataTypeOptions().filter(
    (item) => !excludedTypes.includes(item.value as typeof excludedTypes[number])
  )
})

const handleChange = (dataType: ThingModelDataType) => {
  property.value.dataSpecs = {}
  property.value.dataSpecsList = []
  if (!dataSpecsListOnlyTypes.includes(dataType)) {
    propertyDataSpecs.value.dataType = dataType
  }
  switch (dataType) {
    case IoTDataSpecsDataTypeEnum.ENUM:
      propertyDataSpecsList.value.push({
        dataType: IoTDataSpecsDataTypeEnum.ENUM,
        name: '',
        value: undefined
      })
      break
    case IoTDataSpecsDataTypeEnum.BOOL:
      for (let i = 0; i < 2; i++) {
        propertyDataSpecsList.value.push({
          dataType: IoTDataSpecsDataTypeEnum.BOOL,
          name: '',
          value: i
        })
      }
      break
  }
}

watch(
  () => property.value.accessMode,
  (val: string | undefined) => {
    if (props.isStructDataSpecs || props.isParams) {
      return
    }
    if (isEmpty(val)) {
      property.value.accessMode = IoTThingModelAccessModeEnum.READ_WRITE.value
    }
  },
  { immediate: true }
)
</script>

<style lang="scss" scoped>
:deep(.el-form-item) {
  .el-form-item {
    margin-bottom: 0;
  }
}
</style>
