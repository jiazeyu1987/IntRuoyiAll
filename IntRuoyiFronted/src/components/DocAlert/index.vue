<script lang="ts">
import { defineComponent } from 'vue'
import { propTypes } from '@/utils/propTypes'

export default defineComponent({
  name: 'DocAlert',
  props: {
    title: propTypes.string,
    url: propTypes.string
  },
  setup() {
    // This project intentionally hides all shared documentation banners.
    return () => null
  }
})
</script>
