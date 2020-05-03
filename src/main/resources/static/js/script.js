var GD_tabs = [{"id" : "generalData", "name" : "Справочники"},
               {"id" : "leasingDeposits", "name" : "Лизинговые депозиты"},
               {"id" : "reg_LD_1", "name" : "Reg.LD.1"},
               {"id" : "reg_LD_2", "name" : "Reg.LD.2"},
               {"id" : "reg_LD_3", "name" : "Reg.LD.3"},
               {"id" : "entriesIFRS", "name" : "Проводки на МСФО счетах"},
               {"id" : "Calculate", "name" : "Создать проводки"}]

var GD_spravochniki = [{"id" : "/currencies", "name" : "Курсы валют"},
                       {"id" : "/companies", "name" : "Компании"}]

var whatToShowRightFromServer = []

Vue.component('DataTableForDataRight', {
    data: function() {
        return {
            whatToShowRightFromServer : whatToShowRightFromServer
        }
    },
    watch: {
        whatToShowRightFromServer: function(newValue, oldValue) {
                console.log('новое значение: %s, старое значение: %s', newValue, oldValue)
        }
    }
});

Vue.component('DataTableForGeneralDataLeft', {
    props: ['spravochniki_to_show'],
    template: '<table>' +
                  '<tr v-for="spravochnik in spravochniki_to_show">' +
                        '<td>' +
                          '<button v-bind:value="spravochnik.id" class="spravochnikButton" @click=clickedToChangeView(spravochnik.id)>' +
                              '{{ spravochnik.name }}' +
                          '</button>' +
                        '</td>' +
                  '</tr>' +
              '</table>',
     methods: {
        clickedToChangeView: function(url) {
            console.log("В методе clickedToChangeView, url = ", url);
            this.$emit('refreshDataToView', url);
            console.log("В методе clickedToChangeView 2, url = ", url);
        }
     }
});

Vue.component('GeneralDataTable', {
    props: ["spravochniki", "whatToShowRightFromServer"],
    template: '<table>' +
                  '<tr>' +
                        '<td>' +
                          '<DataTableForGeneralDataLeft :spravochniki_to_show = "spravochniki" v-on:refreshDataToView=refreshValue($event) />' +
                        '</td>' +
                        '<td>' +
                          'Hello' +
                        '</td>' +
                '</tr>' +
              '</table>',
    methods: {
        refreshValue: function(url) {
            console.log("В методе refreshValue, url = ", url);
            this.$emit('refreshDataToView', url);
            console.log("В методе refreshValue 2, url = ", url);
        }
    }
});

//----------------tabs--------------------
Vue.component("tab-generaldata", {
    data: function() {
        return {
            spravochniki: GD_spravochniki,
            whatToShowRightFromServer : whatToShowRightFromServer
        }
    },
    template: '<div><GeneralDataTable :spravochniki = spravochniki ' +
                                      ':whatToShowRightFromServer = whatToShowRightFromServer ' +
                                      'v-on:refreshDataToView=getValuesFromServer($event) /></div>',
    methods: {
                getValuesFromServer: function(url) {
                        var whatToShowRightFromServer = [];
                        console.log(url);
                        console.log(Vue.resource(url+'{/id}'));
                        console.log(Vue.resource(url+'{/id}').get());
                        Vue.resource(url+'{/id}').get().then(result => console.log(result.json()));

                        Vue.resource(url+'{/id}').get().then(result => result.json().then(data =>
                                                 data.forEach(d => {
                                                 console.log(d);
                                                 whatToShowRightFromServer.push(d)})
                                             ));

                        return whatToShowRightFromServer;

              }
}});

Vue.component("tab-leasingdeposits", {
    template: "<div>Posts component</div>"
});

Vue.component("tab-reg_ld_1", {
    template: "<div>Posts component</div>"
});

Vue.component("tab-reg_ld_2", {
    template: "<div>Archive component</div>"
});

Vue.component("tab-reg_ld_3", {
    template: "<div>Archive component</div>"
});

Vue.component("tab-entriesifrs", {
    template: "<div>Archive component</div>"
});

Vue.component("tab-calculate", {
    template: "<div>Archive component</div>"
});
//----------------tabs--------------------

var app = new Vue({
  el: '#ld',
  data: {
            currentTab: GD_tabs[0].id,
            tabs: GD_tabs
        },
  computed: {
            currentTabComponent: function() {
                  return "tab-" + this.currentTab.toLowerCase();
                }
          }
});




