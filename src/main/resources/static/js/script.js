var GD_tabs_1Level = [{"id" : "generalData", "name" : "Справочники"},
                      {"id" : "Entries", "name" : "Проводки по депозитам"}]

var GD_tabs_2Level = [{"id" : "leasingdeposits", "name" : "Перечень лизинговых депозитов"},
                      {"id" : "calculate", "name" : "Расчет проводок за период"},
                      {"id" : "reg_ld_1", "name" : "Форма Reg.LD.1"},
                      {"id" : "reg_ld_2", "name" : "Форма Reg.LD.2"},
                      {"id" : "reg_ld_3", "name" : "Форма Reg.LD.3"},
                      {"id" : "entriesifrs", "name" : "Журнал МСФО"}]

var GD_spravochniki = [{"id" : "/currencies", "name" : "Валюты"},
                       {"id" : "/companies", "name" : "Компании"},
                       {"id" : "/counterpartners", "name" : "Контрагенты"},
                       {"id" : "/depositRates", "name" : "Ставки депозитов"},
                       {"id" : "/durations", "name" : "Длительности депозитов"},
                       {"id" : "/endDates", "name" : "Даты завершения депозитов"},
                       //{"id" : "/entries", "name" : "Проводки"},
                       //{"id" : "/entriesIFRS", "name" : "Проводки на счетах МСФО"},
                       {"id" : "/exchangeRates", "name" : "Курсы валют"},
                       {"id" : "/ifrsAccounts", "name" : "Счета МСФО"},
                       {"id" : "/leasingDeposits", "name" : "Лизинговые депозиты"},
                       {"id" : "/periods", "name" : "Периоды"},
                       {"id" : "/periodsClosed", "name" : "Закрытие периодов"},
                       {"id" : "/scenarios", "name" : "Сценарии"}]

async function getallScenarios()
{
    var url = '/scenarios';
    var commits;
    let response = await fetch(url);

    if (response.ok)
    { // если HTTP-статус в диапазоне 200-299
        // получаем тело ответа (см. про этот метод ниже)
      commits = await response.json(); // читаем ответ в формате json
      console.log("getallScenarios() => Запрос со статусом ОК: commits: " + commits);
    }
    else
    {
      console.log("Ошибка HTTP: " + response.status);
    }

    return commits;
}

async function getFirstOpenDateInScenario(selectedScenario, allScenarios)
{
  console.log("getFirstOpenDateInScenario => selectedScenario = " + JSON.stringify(selectedScenario));
  console.log("getFirstOpenDateInScenario => allScenarios = " + JSON.stringify(allScenarios));
  var selectedScenario_id;

  if(selectedScenario != null)
  {
    var index;
    for (index = 0; index < allScenarios.length; ++index) {
       console.log("scenario.id = " + allScenarios[index].id);
       console.log("scenario.name = " + allScenarios[index].name);

        if(selectedScenario == allScenarios[index].name)
            selectedScenario_id = allScenarios[index].id;
    }

    console.log("'/periodsClosed' + selectedScenario_id = " + '/periodsClosed/' + selectedScenario_id);

    let url = '/periodsClosed/' + selectedScenario_id;

    var response = await fetch(url);
    var commits = "";

    if (response.ok)
    { // если HTTP-статус в диапазоне 200-299
          // получаем тело ответа (см. про этот метод ниже)
        commits = await response.text(); // читаем ответ в формате text
    }
    else
    {
        console.log("Ошибка HTTP: " + response.status);
    }

    var closedPeriod = commits;
    console.log("closedPeriod = " + closedPeriod);

    return closedPeriod;
  }
}

Vue.component('InputFormForSpravochniki',
{
    props: ['showingKeys', 'showForm', 'spravochnik_name', 'url', 'method', 'actionName', 'dataInForm'],
   data: function() {
           return  {
                answer: {}
           }
   },
   watch: {
           showingKeys: function(val, oldVal) {
                this.refreshDataInForm(val, this.dataInForm);
           },
           dataInForm: function(val, oldVal) {
                this.refreshDataInForm(this.showingKeys, val);
           }
    },
    template:
        '<div v-if="showForm == true" class="inputForm_div" >' +
            '<div class="inputForm_form">' +
                '<h2> {{ actionName }} значения справочника "{{ spravochnik_name }}" </h2>' +
                '<div v-for="key in showingKeys" v-if="key != `id`">' +
                    '<label> {{ key }} </label>' +
                    '<input type="text" :name="key" v-model="answer[key]">' +
                '</div>' +

                '<hr>' +
                '<button id="saveDB" @click="sendDataToDB(url, answer, method)"> {{ actionName }} </button>'+
                '<button id="closeForm" @click="$emit(`hideForm`)" > Скрыть форму </button>' +
            '</div>' +
        '</div>',
    methods: {
        sendDataToDB: async function(url, answer, method)
        {
            let finalurl = url;
            if(method.toLowerCase() == "put") finalurl = finalurl + "/" + answer.id;

          let response = await fetch(finalurl, {
            method: method,
            headers: {
              'Content-Type': 'application/json;charset=utf-8'
            },
            body: JSON.stringify(answer)
          });

          let result = await response.json();
          console.log(JSON.stringify(answer));
          console.log(finalurl);
          console.log(result);
          this.$emit("hideForm");
          this.$emit("refreshDataToView", url);
        },
        refreshDataInForm: function(keys, data)
        {
            let A = new Object();

            for (let k of keys) {
                console.log("InputFormForSpravochniki:refreshDataInForm k = " + k);

                try
                {
                    console.log("watch: keys => A: " + JSON.stringify(A));
                    A[k] = data[k];
                }
                catch
                {
                    console.log("watch: keys => A: " + JSON.stringify(A));
                    A[k] = "";
                }
            }

            console.log("watch: keys => A: " + JSON.stringify(A));
            this.answer = A;
            console.log("watch: keys => this.answer = " + JSON.stringify(this.answer));
        }
    }
});

Vue.component('DataTableForDataRight', {
    props: ['showingData', 'showingKeys', 'urlToDetermineName'],
    data: function() {
        return {
            showFormToAdd: false,
            showFormToEdit: false,
            showingDataInForm: {},
            spravochnik_name: ""
        }
    },
    computed: {
                currentTabComponent: function() {
                      return "tab-" + this.currentTab.toLowerCase();
                    }
            },
    watch:    {
                selectedScenario: 'getFirstOpenDateInScenario'
            },
    template:
                '<div>' +
                    '<div>' +
                        '<InputFormForSpravochniki :showingKeys = "showingKeys" ' +
                                                   ':showForm = "showFormToAdd" ' +
                                                   ':spravochnik_name = "spravochnik_name" ' +
                                                   ':url = "urlToDetermineName.url" ' +
                                                   ':method = "`POST`" ' +
                                                   ':actionName = "`Добавление`" ' +
                                                   ':dataInForm = {} ' +
                                                   'v-on:hideForm="hideForm()" ' +
                                                   'v-on:refreshDataToView="refreshDataRight($event)" />' +
                    '</div>' +
                    '<div>' +
                        '<InputFormForSpravochniki :showingKeys = "showingKeys" ' +
                                                   ':showForm = "showFormToEdit" ' +
                                                   ':spravochnik_name = "spravochnik_name" ' +
                                                   ':url = "urlToDetermineName.url" ' +
                                                   ':method = "`PUT`" ' +
                                                   ':actionName = "`Изменение`" ' +
                                                   ':dataInForm = "showingDataInForm" ' +
                                                   'v-on:hideForm="hideForm()" ' +
                                                   'v-on:refreshDataToView="refreshDataRight($event)" />' +
                    '</div>' +
                    '<div>' +
                        '<button v-if="showingData.length > 0" @click=showAddForm(urlToDetermineName)> Добавить значение </button>' +
                    '</div>' +
                    '<div>' +
                        '<table border="1" v-if="showingData.length > 0" align="left" valign="top">' +
                           '<tr align="left" valign="top">' +
                                '<th v-for="key in showingKeys">' +
                                      '{{ key }}' +
                                '</th>' +
                           '</tr>' +
                           '<tr v-for="data in showingData" align="left" valign="top">' +
                               '<td v-for="key1 in showingKeys">' +
                                     '{{ data[key1] }}' +
                               '</td>' +
                                '<td>' +
                                     '<button v-bind:value="data.id" class="editButton" @click="changeData(data, showingKeys, urlToDetermineName)" >' +
                                         'E' +
                                     '</button>' +
                                '</td>' +
                                '<td>' +
                                      '<button v-bind:value="data.id" class="deleteButton" @click="deleteData(data.id)">' +
                                          'X' +
                                      '</button>' +
                                '</td>' +
                           '</tr>' +
                       '</table>' +
                    '</div>' +
                '</div>',
    methods: {
        refreshDataRight: function(url) {
            this.$emit("refreshDataToView", url);
        },
        determineNameSprav: function(urlToDetermineName) {
            var index;
            for (index = 0; index < GD_spravochniki.length; ++index)
            {
               console.log("GD_spravochniki.id = " + GD_spravochniki[index].id);
               console.log("GD_spravochniki.name = " + GD_spravochniki[index].name);

               console.log("urlToDetermineName.url = " + urlToDetermineName.url);

                if(urlToDetermineName.url == GD_spravochniki[index].id)
                    this.spravochnik_name = GD_spravochniki[index].name;
            }
        },
        showAddForm: function(urlToDetermineName) {
            console.log("В методе showAddForm, this.showFormToAdd до присвоения значения = " + this.showFormToAdd);

            this.showFormToAdd = true;

            console.log("В методе showAddForm, this.showFormToAdd после присвоения значения = " + this.showFormToAdd);

            this.determineNameSprav(urlToDetermineName);
        },
        hideForm: function() {
            this.showFormToAdd = false;
            this.showFormToEdit = false;
            this.showingDataInForm = {};
        },
        changeData: function(data, showingKeys, urlToDetermineName) {
           console.log("В методе changeData data = " + JSON.stringify(data));
           console.log("В методе changeData showingKeys = " + showingKeys);

           console.log("В методе changeData, this.showingDataInForm до присвоения значения = " + JSON.stringify(this.showingDataInForm));
           this.showingDataInForm = data;
           console.log("В методе changeData, this.showingDataInForm после присвоения значения = " + JSON.stringify(this.showingDataInForm));

           this.determineNameSprav(urlToDetermineName);

           console.log("В методе changeData, this.showFormToEdit до присвоения значения = " + JSON.stringify(this.showFormToEdit));
           this.showFormToEdit = true;
           console.log("В методе changeData, this.showFormToEdit после присвоения значения = " + JSON.stringify(this.showFormToEdit));
        },
        deleteData: async function(id_element) {
           let finalurl = this.urlToDetermineName.url + "/" + id_element;
           console.log("В методе deleteData finalurl = " + finalurl);

           let response = await fetch(finalurl, {
                method: "DELETE",
                headers: {
                  'Content-Type': 'application/json;charset=utf-8'
                },
                body: JSON.stringify()
           });

//           let result = await response.json();
//           console.log("В методе deleteData result = " + result);

           this.$emit("refreshDataToView", this.urlToDetermineName.url);
        }
    }
});

Vue.component('ButtonTableLeft', {
    props: ['array_buttons_to_show', 'throwingEvent'],
    data: function() {
        return {
            currentSpravTab: ""
        }
    },
    template: '<table align="left" valign="top">' +
                  '<tr v-for="button_to_show in array_buttons_to_show" align="left" valign="top">' +
                        '<td>' +
                          '<button ' +
                                'v-bind:value="button_to_show.id"' +
                                'class="spravochnikButton"' +
                                'v-bind:class="{active: currentSpravTab === button_to_show.id}"' +
                                '@click="clickToPerformButtonTable(throwingEvent, button_to_show.id)">' +
                                        '{{ button_to_show.name }}' +
                          '</button>' +
                        '</td>' +
                  '</tr>' +
              '</table>',
     methods: {
        clickToPerformButtonTable: function(throwingEventForPerform, data) {
            console.log("В методе clickToPerformButtonTable, this.currentSpravTab до присвоения значения = ", this.currentSpravTab);
            this.currentSpravTab = data;
            console.log("В методе clickToPerformButtonTable, this.currentSpravTab после присвоения значения = ", this.currentSpravTab);

            console.log("В методе clickToPerformButtonTable, url = ", data);
            this.$emit(throwingEventForPerform, data);
            console.log("В методе clickToPerformButtonTable 2, url = ", data);
        }
     }
});

//----------------tabs--------------------
Vue.component("tab-generaldata", {
    data: function() {
        return {
            spravochniki: GD_spravochniki,
            cache_spravochniki: {},
            URLIK : {},
            showingData : [],
            showingKeys : [],
            eventA: "refreshDataToView"
        }
    },
    template:
               '<table align="left" valign="top">' +
                    '<caption>Значения справочников</caption>' +
                    '<tr align="left" valign="top">' +
                          '<td>' +
                            '<ButtonTableLeft :array_buttons_to_show = "spravochniki" :throwingEvent = "eventA" v-on:refreshDataToView=getValuesFromServer($event) />' +
                          '</td>' +
                          '<td>' +
                            '<DataTableForDataRight :showingData = "showingData" ' +
                                ':showingKeys = "showingKeys" ' +
                                ':urlToDetermineName = "URLIK" ' +
                                'v-on:refreshDataToView=getValuesFromServer($event) />' +
                          '</td>' +
                    '</tr>' +
              '</table>',
    methods: {
                getValuesFromServer: async function(url) {
                      this.URLIK.url = url;
                      console.log("this.URLIK.url = " + this.URLIK.url);

//                        if(this.cache_spravochniki[url] == null)
//                        {
                            let response = await fetch(url);
                             console.log("url = " + url);

                              if (response.ok)
                              { // если HTTP-статус в диапазоне 200-299
                                    // получаем тело ответа (см. про этот метод ниже)
                                  let commits = await response.json(); // читаем ответ в формате json
                                  this.cache_spravochniki[url] = commits;
                                  console.log("this.cache_spravochniki[url] = " + this.cache_spravochniki[url]);

                                  this.showingData = this.cache_spravochniki[url];
                                  console.log("В методе getValuesFromServer this.showingData = ", this.showingData);

                                  var parsedobj = JSON.parse(JSON.stringify(this.showingData));
                                  console.log("В методе getValuesFromServer parsedobj = ", parsedobj);

                                  var parsedobj2 = parsedobj[0];
                                  console.log("В методе getValuesFromServer parsedobj2 = ", parsedobj2);

                                  this.showingKeys = Object.keys(parsedobj2);
                                  console.log("В методе getValuesFromServer showingKeys = ", showingKeys);

                                  console.log("В методе getValuesFromServer this.cache_spravochniki = ", this.cache_spravochniki);

                              }
                              else
                              {
                                  console.log("Ошибка HTTP: " + response.status);

                                    let commits = ""; // читаем ответ в формате json
                                    this.cache_spravochniki[url] = commits;
                                    console.log("this.cache_spravochniki[url] = " + JSON.stringify(this.cache_spravochniki[url]));
                              }

                            console.log("В методе getValuesFromServer this.cache_spravochniki = ", JSON.stringify(this.cache_spravochniki));
//                        }
//                        else
//                        {
//                          this.showingData = this.cache_spravochniki[url];
//                          console.log("В методе getValuesFromServer берём из кэша this.showingData = ", this.showingData);
//                        }
              }
}});

Vue.component("tab-leasingdeposits", {
    template: "<div>Posts1 component</div>"
});

Vue.component("tab-reg_ld_1", {
    template: "<div>Posts2 component</div>"
});

Vue.component("tab-reg_ld_2", {
    template: "<div>Archive3 component</div>"
});

Vue.component("tab-reg_ld_3", {
    template: "<div>Archive4 component</div>"
});

Vue.component("tab-entriesifrs", {
    template: "<div>Archive5 component</div>"
});

Vue.component("tab-calculate", {
    template: "<div>Archive6 component</div>"
});

Vue.component("tab-entries", {
    data: function() {
        return {
            tabs2: GD_tabs_2Level,
            currentTab2: GD_tabs_2Level[0].id,
            scenario_receiver: null,
            currentTabComponent2Level: "",
            eventA: "showTabForEntries",
            allScenarios: [],
            closedPeriod: ""
        }
    },
    template:
               '<table align="left" valign="top">' +
                    '<tr align="left" valign="top">' +
                          '<td>' +
                                '<p>Выбранный сценарий-получатель: </p>' +
                                '<select v-model="scenario_receiver">' +
                                    '<option v-for="scenario in this.allScenarios"> {{ scenario.name }} </option>' +
                                '</select>' +
                          '</td>' +
                    '</tr>' +
                    '<tr align="left" valign="top">' +
                          '<td>' +
                                '<p>Период расчета для сценария-источника: </p>' +
                                '<select disabled>' +
                                    '<option> {{ this.closedPeriod }} </option>' +
                                '</select>' +
                          '</td>' +
                    '</tr>' +
                    '<tr align="left" valign="top">' +
                          '<td>' +
                            '<ButtonTableLeft :array_buttons_to_show = "tabs2" :throwingEvent = "eventA" v-on:showTabForEntries=showTabForEntry($event) />' +
                          '</td>' +
                          '<td>' +
                            '<component v-bind:is="currentTabComponent2Level" class="tab2Level"></component>' +
                          '</td>' +
                    '</tr>' +
              '</table>',
    methods: {
        showTabForEntry: function(tab)
        {
            this.currentTabComponent2Level = "tab-" + tab.toLowerCase();
        }
    },
    watch: {
        scenario_receiver: function (val, oldVal) {
           let promise = getFirstOpenDateInScenario(val, this.allScenarios);
           promise.then(date => this.closedPeriod = date);
        }
    },
    created: function()
    {
       console.log("В методе created tab-entries: => до присвоения значения this.allScenarios = ", this.allScenarios);

        var promise = getallScenarios();
        promise.then(scenario => console.log(scenario));
        promise.then(scenario => this.allScenarios = scenario);

       console.log("В методе created tab-entries: => после присвоения значения this.allScenarios = ", this.allScenarios);
    }
});
//----------------tabs--------------------

var app = new Vue({
  el: '#ld',
    data: {
              currentTab: GD_tabs_1Level[0].id,
              tabs: GD_tabs_1Level,
              selectedScenario: null,
              allScenarios: [],
              closedPeriod: ""
          },
  computed: {
                currentTabComponent: function() {
                      return "tab-" + this.currentTab.toLowerCase();
                    }
            },
  watch:    {
                selectedScenario: function (val, oldVal) {
                   let promise = getFirstOpenDateInScenario(val, this.allScenarios);
                   promise.then(date => this.closedPeriod = date);
                }
            },
  created: function()
  {
     console.log("В методе created: => до присвоения значения this.allScenarios = ", this.allScenarios);

     var promise = getallScenarios();
     promise.then(scenario => console.log(scenario));
     promise.then(scenario => this.allScenarios = scenario);

     console.log("В методе created: => после присвоения значения this.allScenarios = ", this.allScenarios);
  }

});




