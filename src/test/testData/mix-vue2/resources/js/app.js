import PortalVue from 'portal-vue'
import SummaryTable from './components/SummaryTable.vue'

window.Vue = require('vue').default

Vue.use(PortalVue)
Vue.use(require('vue-moment'))

Vue.component('contact-form', require('./components/ContactForm.vue').default)
Vue.component('data-table', require('./components/DataTable').default)
Vue.component('flatPickr', require('vue-flatpickr-component'))

const app = new Vue({
    el: '#app',
    components: {
        SummaryTable,
        'payment-form': require('./components/PaymentForm.vue').default,
    },
})
