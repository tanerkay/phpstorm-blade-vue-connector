import { createApp } from 'vue'
import ApiKeyList from './components/Settings/APIKeyList.vue'
import SettingsDialog from '@/components/Profile/ProfileSettingsDialog.vue'

const options = {
    components: {
        ApiKeyList,
        AccountSettingsModal: SettingsDialog,
    },
}

createApp(options).mount('#app')
