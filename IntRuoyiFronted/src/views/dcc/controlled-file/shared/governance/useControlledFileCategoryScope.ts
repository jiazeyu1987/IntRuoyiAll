import type { ControlledFileCategoryVO } from '@/api/dcc/controlledFile/fileCategories'
import { getFileCategoryList } from '@/api/dcc/controlledFile/fileCategories'

export const useControlledFileCategoryScope = () => {
  const categories = ref<ControlledFileCategoryVO[]>([])
  const queryParams = reactive<{
    categoryId?: number
  }>({
    categoryId: undefined
  })

  const currentCategory = computed(() =>
    categories.value.find((item) => item.id === queryParams.categoryId)
  )

  const activeCategoryOptions = computed(() =>
    categories.value.filter(
      (item): item is ControlledFileCategoryVO & { id: number } =>
        item.active && item.id !== undefined
    )
  )

  const loadCategories = async () => {
    categories.value = await getFileCategoryList()
  }

  const resetCategoryScope = () => {
    queryParams.categoryId = undefined
  }

  return {
    categories,
    queryParams,
    currentCategory,
    activeCategoryOptions,
    loadCategories,
    resetCategoryScope
  }
}
