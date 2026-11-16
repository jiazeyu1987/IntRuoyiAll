import { ref } from 'vue'

/** Every page/query uses one instance. Superseded requests finish without publishing stale rows. */
export const useLatestListRequest = () => {
  const loading = ref(false)
  let generation = 0
  const run = async <T>(load: () => Promise<T>, commit: (result: T) => void): Promise<boolean> => {
    const current = ++generation
    loading.value = true
    try {
      const result = await load()
      if (current !== generation) return false
      commit(result)
      return true
    } finally {
      if (current === generation) loading.value = false
    }
  }
  const invalidate = () => {
    generation++
    loading.value = false
  }
  return { loading, run, invalidate }
}
