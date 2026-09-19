import { createApp } from 'vue'
import LoginDialog from './components/LoginDialog.vue'
import { registerLibrary } from './library-global'

const app = createApp({
    components: {
        LoginDialog,
    },
})

registerLibrary(app)
app.mount('#app')
