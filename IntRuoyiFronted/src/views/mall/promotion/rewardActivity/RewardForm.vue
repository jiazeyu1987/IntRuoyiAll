<template>
  <Dialog v-model="dialogVisible" :title="dialogTitle" width="65%">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="80px"
    >
      <el-form-item label="活动名称" prop="name">
        <el-input v-model="formData.name" placeholder="请输入活动名称" />
      </el-form-item>

      <el-form-item label="活动时间" prop="startAndEndTime">
        <el-date-picker
          v-model="formData.startAndEndTime"
          :end-placeholder="t('common.endTimeText')"
          :start-placeholder="t('common.startTimeText')"
          range-separator="-"
          type="datetimerange"
        />
      </el-form-item>

      <el-form-item label="条件类型" prop="conditionType">
        <el-radio-group v-model="formData.conditionType">
          <el-radio
            v-for="dict in getIntDictOptions(DICT_TYPE.PROMOTION_CONDITION_TYPE)"
            :key="dict.value"
            :label="dict.value"
          >
            {{ dict.label }}
          </el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="优惠设置">
        <RewardRule ref="rewardRuleRef" v-model="formData" />
      </el-form-item>

      <el-form-item label="活动范围" prop="productScope">
        <el-radio-group v-model="formData.productScope">
          <el-radio
            v-for="dict in getIntDictOptions(DICT_TYPE.PROMOTION_PRODUCT_SCOPE)"
            :key="dict.value"
            :label="dict.value"
          >
            {{ dict.label }}
          </el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item
        v-if="formData.productScope === PromotionProductScopeEnum.SPU.scope"
        prop="productSpuIds"
      >
        <SpuShowcase v-model="formData.productSpuIds" />
      </el-form-item>

      <el-form-item
        v-if="formData.productScope === PromotionProductScopeEnum.CATEGORY.scope"
        label="分类"
        prop="productCategoryIds"
      >
        <ProductCategorySelect v-model="formData.productCategoryIds" :multiple="true" />
      </el-form-item>

      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" placeholder="请输入备注" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">确认</el-button>
      <el-button @click="dialogVisible = false">取消</el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import { cloneDeep } from 'lodash-es'
import RewardRule from './components/RewardRule.vue'
import SpuShowcase from '@/views/mall/product/spu/components/SpuShowcase.vue'
import ProductCategorySelect from '@/views/mall/product/category/components/ProductCategorySelect.vue'
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import * as RewardActivityApi from '@/api/mall/promotion/reward/rewardActivity'
import { PromotionConditionTypeEnum, PromotionProductScopeEnum } from '@/utils/constants'
import { fenToYuan, yuanToFen } from '@/utils'

defineOptions({ name: 'ProductBrandForm' })

const { t } = useI18n()
const message = useMessage()

type RewardFormData = RewardActivityApi.RewardActivityVO & {
  productSpuIds: number[]
  productCategoryIds: number[]
  startAndEndTime?: [Date, Date]
}

const createDefaultFormData = (): RewardFormData =>
  ({
    conditionType: PromotionConditionTypeEnum.PRICE.type,
    productScope: PromotionProductScopeEnum.ALL.scope,
    productSpuIds: [],
    productCategoryIds: [],
    rules: [],
    startAndEndTime: undefined
  }) as RewardFormData

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formLoading = ref(false)
const formType = ref('')
const formData = ref<RewardFormData>(createDefaultFormData())
const formRules = reactive({
  name: [{ required: true, message: '活动名称不能为空', trigger: 'blur' }],
  startAndEndTime: [{ required: true, message: '活动时间不能为空', trigger: 'blur' }],
  conditionType: [{ required: true, message: '条件类型不能为空', trigger: 'change' }],
  productScope: [{ required: true, message: '商品范围不能为空', trigger: 'blur' }],
  productSpuIds: [{ required: true, message: '商品不能为空', trigger: 'blur' }],
  productCategoryIds: [{ required: true, message: '商品分类不能为空', trigger: 'blur' }]
})
const formRef = ref()
const rewardRuleRef = ref<InstanceType<typeof RewardRule>>()

const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()

  if (id) {
    formLoading.value = true
    try {
      const data = await RewardActivityApi.getReward(id)
      if (data.startTime && data.endTime) {
        data.startAndEndTime = [data.startTime, data.endTime]
      }
      data.rules?.forEach((item: any) => {
        item.discountPrice = fenToYuan(item.discountPrice || 0)
        if (data.conditionType === PromotionConditionTypeEnum.PRICE.type) {
          item.limit = fenToYuan(item.limit || 0)
        }
      })
      formData.value = {
        ...createDefaultFormData(),
        ...data,
        productSpuIds: data.productSpuIds ?? [],
        productCategoryIds: data.productCategoryIds ?? []
      }
      await getProductScope()
    } finally {
      formLoading.value = false
    }
  }
}
defineExpose({ open })

const emit = defineEmits(['success'])
const submitForm = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate()
  if (!valid) return

  formLoading.value = true
  try {
    rewardRuleRef.value?.setRuleCoupon()
    const data = cloneDeep(formData.value)
    data.startTime = data.startAndEndTime?.[0]
    data.endTime = data.startAndEndTime?.[1]
    delete data.startAndEndTime

    data.rules.forEach((item) => {
      item.discountPrice = yuanToFen(item.discountPrice || 0)
      if (data.conditionType === PromotionConditionTypeEnum.PRICE.type) {
        item.limit = yuanToFen(item.limit || 0)
      }
    })

    setProductScopeValues(data)

    if (formType.value === 'create') {
      await RewardActivityApi.createRewardActivity(data)
      message.success(t('common.createSuccess'))
    } else {
      await RewardActivityApi.updateRewardActivity(data)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    emit('success')
  } finally {
    formLoading.value = false
  }
}

const resetForm = () => {
  formData.value = createDefaultFormData()
}

const getProductScope = async () => {
  switch (formData.value.productScope) {
    case PromotionProductScopeEnum.SPU.scope:
      formData.value.productSpuIds = formData.value.productScopeValues ?? []
      break
    case PromotionProductScopeEnum.CATEGORY.scope: {
      await nextTick()
      const values = formData.value.productScopeValues
      if (Array.isArray(values)) {
        formData.value.productCategoryIds = values
      } else {
        formData.value.productCategoryIds = []
      }
      break
    }
    default:
      break
  }
}

function setProductScopeValues(data: RewardFormData) {
  switch (formData.value.productScope) {
    case PromotionProductScopeEnum.SPU.scope:
      data.productScopeValues = formData.value.productSpuIds
      break
    case PromotionProductScopeEnum.CATEGORY.scope:
      data.productScopeValues = formData.value.productCategoryIds
      break
    default:
      break
  }
}
</script>
