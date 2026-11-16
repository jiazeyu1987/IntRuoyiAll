import { computed, ref, watch, type Ref } from 'vue'

/** 编辑内容的快照与草稿状态；试填数据不进入编辑快照。 */
export function useParserEditing<T>(
  read: () => T | undefined,
  write: (value: T) => void,
  published: Ref<boolean>
) {
  const history = ref<string[]>([])
  const baseline = ref<string>()
  const savedAt = ref('')
  let ignored: string | undefined
  const current = computed(() => JSON.stringify(read()))
  const dirty = computed(
    () => !!current.value && current.value !== baseline.value && !published.value
  )
  watch(current, (value, previous) => {
    const skip = value === ignored
    ignored = undefined
    if (skip) return
    if (published.value || !previous || value === previous) return
    history.value.push(previous)
    if (history.value.length > 40) history.value.shift()
  })
  function reset(saved = false, time = '') {
    history.value = []
    baseline.value = saved ? current.value : ''
    savedAt.value = time
    ignored = current.value
  }
  function markSaved(
    time = new Date().toLocaleString('zh-CN', { timeZone: 'Asia/Shanghai', hour12: false })
  ) {
    baseline.value = current.value
    savedAt.value = time
  }
  function undo() {
    if (published.value) return
    const previous = history.value.pop()
    if (!previous) return
    write(JSON.parse(previous))
    ignored = current.value
  }
  return { history, dirty, savedAt, reset, markSaved, undo }
}
