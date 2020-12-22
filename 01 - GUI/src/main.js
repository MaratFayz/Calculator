import Vue from 'vue'
import 'es6-promise/auto'
import App from './App.vue'
import vuetify from './plugins/vuetify'
import { store } from './store'
import { router } from './router'

Vue.config.productionTip = false

new Vue({
  el: '#app',
  vuetify,
  store,
  router,
  render: h => h(App)
})