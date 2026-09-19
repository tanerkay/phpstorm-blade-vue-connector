import { createApp, defineAsyncComponent } from 'vue'
import vSelect from 'vue-select'
import FormTextInput from './components/FormTextInput.vue'
import MultiFileUploader from './components/MultiFileUploader.vue'
import AlertBanner from './components/AlertBanner.vue'
import AtAlias from '@/components/AtAlias.vue'
import UserCard from './components/UserCard.vue'

const ActivityFeed = defineAsyncComponent(() => import('./components/ActivityFeed.vue'))

const app = createApp({
    components: {
        ActivityFeed,
        SearchBox: defineAsyncComponent(() => import('./components/SearchPanel.vue')),
        AtAlias,
    },
})

app.component('TextInput', FormTextInput)
app.component('file-upload-v2', MultiFileUploader)
app.component('StatusMessage', AlertBanner)
app.component('v-select', vSelect)
app.component('Step2Details', require('./components/Step2Details.vue').default)
app.component(`user-card`, UserCard)

app.mount('#app')
