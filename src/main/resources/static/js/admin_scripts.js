var tabs = [{"id" : "users", "name" : "Управление пользователями"},
            {"id" : "roles", "name" : "Управление ролями"},
            {"id" : "exImport", "name" : "Импорт курсов валют"},
            {"id" : "autoCreatePeriods", "name" : "Автоматическое создание периодов"},
            {"id" : "autoClosingPeriods", "name" : "Автоматическое закрытие периодов"}]

var selectedScenario_from = "";
var selectedScenario_to = "";
var allScenarios = [];
var allCurrencies = [];

var urlWithScenarios = "/scenarios";
var urlWithCurrencies = "/currencies";
var urlWithUsers = "/users";
var urlWithRoles = "/roles";
var urlWithAutoCreatePeriods = "/periods/autoCreatePeriods";
var urlWithAutoClosingPeriods = "/periodsClosed/autoClosingPeriods";
var urlWithImportExchangeCurrencies = "/exchangeRates/importERFromCBR";

var GD_spravochniki = [{"id" : "/user", "name" : "Пользователи"},
                       {"id" : "/roles", "name" : "Роли"}]

function determineIdSprav(value, allValueForSprav)
{
    var value_id;
    var index;
    for (index = 0; index < allValueForSprav.length; ++index)
    {
       console.log("value.id = " + allValueForSprav[index].id);
       console.log("value.name = " + allValueForSprav[index].name);

        if(value == allValueForSprav[index].name)
            value_id = allValueForSprav[index].id;
    }

    return value_id;
}

function determineNameSprav(urlToDetermineName) {
    var index;
    var SpravName;
    for (index = 0; index < GD_spravochniki.length; ++index)
    {
       console.log("GD_spravochniki.id = " + GD_spravochniki[index].id);
       console.log("GD_spravochniki.name = " + GD_spravochniki[index].name);

       console.log("urlToDetermineName.url = " + urlToDetermineName.url);

        if(urlToDetermineName.url == GD_spravochniki[index].id)
            SpravName = GD_spravochniki[index].name;
    }

    return SpravName;
}

async function getValuesFromServer(url, params)
{
      var finalUrl = url;
      console.log("getValuesFromServer => params = " + JSON.stringify(params));

      if(params != null)
      {
        finalUrl = finalUrl + "?";
        Object.keys(params).forEach(function(keyInParam) {
            console.log("getValuesFromServer => keyInParam = " + JSON.stringify(keyInParam));
            console.log("getValuesFromServer => params[keyInParam] = " + JSON.stringify(params[keyInParam]));
            finalUrl = finalUrl + keyInParam + "=" + params[keyInParam] + "&";
        });

        finalUrl = finalUrl.substring(0, finalUrl.length-1);
      }

      let response = await fetch(finalUrl);
      console.log("getValuesFromServer => finalUrl = " + finalUrl);

      var commits;

      if (response.ok)
      {
          // если HTTP-статус в диапазоне 200-299
          // получаем тело ответа
          commits = await response.json(); // читаем ответ в формате json

          console.log("Статус ОК: В методе getValuesFromServer commits = ", commits);
      }
      else
      {
          console.log("Ошибка HTTP: " + response.status);

          commits = [];

          console.log("В методе getValuesFromServer commits = ", commits);
      }

      return commits;
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
            if(method.toLowerCase() == "put")
                if(answer.id != null) finalurl = finalurl + "/" + answer.id;

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

Vue.component('DataTable', {
    props: ['showButtonsForEditAndDelete',
            'showingData',
            'showingKeys',
            'urlToDetermineName',
            'stringForNoValues'],
    template:
              '<div>' +
                  '<table border="1" v-if="showingKeys.length > 0" align="left" valign="top">' +
                     '<tr align="left" valign="top">' +
                          '<th v-for="key in showingKeys">' +
                                '{{ key }}' +
                          '</th>' +
                     '</tr>' +
                     '<tr v-for="data in showingData" align="left" valign="top">' +
                         '<td v-for="key1 in showingKeys">' +
                               '{{ data[key1] }}' +
                         '</td>' +
                          '<td v-if="showButtonsForEditAndDelete == `true`">' +
                               '<button v-bind:value="data.id"' +
                                        'class="editButton"' +
                                        '@click="changeData(data, showingKeys, urlToDetermineName)" >' +
                                   'E' +
                               '</button>' +
                          '</td>' +
                          '<td v-if="showButtonsForEditAndDelete == `true`">' +
                                '<button v-bind:value="data.id"' +
                                         'class="deleteButton"' +
                                         '@click="deleteData(data)">' +
                                    'X' +
                                '</button>' +
                          '</td>' +
                     '</tr>' +
                 '</table>' +
                 '<table v-else> {{ stringForNoValues }} </table>' +
              '</div>',
    methods: {
        changeData: function(data, showingKeys, urlToDetermineName) {
           console.log("В методе changeData data = " + JSON.stringify(data));
           console.log("В методе changeData showingKeys = " + showingKeys);
           console.log("В методе changeData urlToDetermineName = " + urlToDetermineName);

           var dataForUpdating = {};
           dataForUpdating.data = data;
           dataForUpdating.urlToDetermineName = urlToDetermineName;
           dataForUpdating.showingKeys = showingKeys;

           console.log("В методе changeData был сформирован dataForUpdating = " + JSON.stringify(dataForUpdating));

           this.$emit("updateFormToShow", dataForUpdating);
        },
        deleteData: async function(data) {
           var finalurl = this.urlToDetermineName.url;
           if(data.id != null) finalurl = finalurl + "/" + data.id;
           console.log("В методе deleteData finalurl = " + finalurl);

           let response = await fetch(finalurl, {
                method: "DELETE",
                headers: {
                  'Content-Type': 'application/json;charset=utf-8'
                },
                body: JSON.stringify(data)
           });

           this.$emit("refreshDataToView", this.urlToDetermineName.url);
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
                        '<DataTable :showButtonsForEditAndDelete="`true`"' +
                                    ':showingData="showingData"' +
                                    ':showingKeys="showingKeys"' +
                                    ':urlToDetermineName="urlToDetermineName"' +
                                    ':stringForNoValues="`Для отображения значений выберите справочник. \n ' +
                                    'Если справочник выбран, то значения в справочнике отсутствуют`"' +
                                    'v-on:updateFormToShow="changeData($event)"/>' +
                    '</div>' +
                '</div>',
    methods: {
        refreshDataRight: function(url) {
            this.$emit("refreshDataToView", url);
        },
        showAddForm: function(urlToDetermineName) {
            console.log("В методе showAddForm, this.showFormToAdd до присвоения значения = " + this.showFormToAdd);

            this.showFormToAdd = true;

            console.log("В методе showAddForm, this.showFormToAdd после присвоения значения = " + this.showFormToAdd);

            this.spravochnik_name = determineNameSprav(urlToDetermineName);
        },
        hideForm: function() {
            this.showFormToAdd = false;
            this.showFormToEdit = false;
            this.showingDataInForm = {};
        },
        changeData: function(dataForUpdating) {
           console.log("В методе changeData data = " + JSON.stringify(dataForUpdating.data));
           console.log("В методе changeData showingKeys = " + dataForUpdating.showingKeys);

           console.log("В методе changeData, this.showingDataInForm до присвоения значения = " + JSON.stringify(this.showingDataInForm));
           this.showingDataInForm = dataForUpdating.data;
           console.log("В методе changeData, this.showingDataInForm после присвоения значения = " + JSON.stringify(this.showingDataInForm));

           this.spravochnik_name = determineNameSprav(dataForUpdating.urlToDetermineName);

           console.log("В методе changeData, this.showFormToEdit до присвоения значения = " + JSON.stringify(this.showFormToEdit));
           this.showFormToEdit = true;
           console.log("В методе changeData, this.showFormToEdit после присвоения значения = " + JSON.stringify(this.showFormToEdit));
        }
    }
});

Vue.component('tab-users',
{
    data: function() {
        return {
            URLIK : {},
            showingData : [],
            showingKeys : []
        }
    },
    template:
               '<table align="left" valign="top">' +
                    '<caption> <h1> Учетные записи пользователей </h1> </caption>' +
                    '<tr align="left" valign="top">' +
                          '<td>' +
                            '<DataTableForDataRight :showingData = "showingData" ' +
                                ':showingKeys = "showingKeys" ' +
                                ':urlToDetermineName = "URLIK" ' +
                                'v-on:refreshDataToView=remakeDataToView($event) />' +
                          '</td>' +
                    '</tr>' +
              '</table>',
    methods: {
               remakeDataToView: function(url)
               {
                    this.URLIK.url = url;
                    console.log(this.URLIK.url);
                    var promise = getValuesFromServer(url, null);
                    promise.then(data => {
                          console.log(data);
                          this.showingData = data;
                          console.log(this.showingData);
                          if (this.showingData.length > 0)
                          {
                              this.showingKeys = Object.keys(this.showingData[0]);
                              console.log(this.showingKeys);
                          }
                          else
                          {
                              this.showingKeys = [];
                          }
                    });
               }
             },
    created: function()
    {
        this.remakeDataToView(urlWithUsers);
    }
});

Vue.component('tab-roles',
{
    data: function() {
        return {
            URLIK : {},
            showingData : [],
            showingKeys : []
        }
    },
    template:
               '<table align="left" valign="top">' +
                    '<caption> <h1> Роли пользователей </h1> </caption>' +
                    '<tr align="left" valign="top">' +
                          '<td>' +
                            '<DataTableForDataRight :showingData = "showingData" ' +
                                ':showingKeys = "showingKeys" ' +
                                ':urlToDetermineName = "URLIK" ' +
                                'v-on:refreshDataToView=remakeDataToView($event) />' +
                          '</td>' +
                    '</tr>' +
              '</table>',
    methods: {
               remakeDataToView: function(url)
               {
                    this.URLIK.url = url;
                    console.log(this.URLIK.url);
                    var promise = getValuesFromServer(url, null);
                    promise.then(data => {
                          console.log(data);
                          this.showingData = data;
                          console.log(this.showingData);
                          if (this.showingData.length > 0)
                          {
                              this.showingKeys = Object.keys(this.showingData[0]);
                              console.log(this.showingKeys);
                          }
                          else
                          {
                              this.showingKeys = [];
                          }
                    });
               }
             },
    created: function()
    {
        this.remakeDataToView(urlWithRoles);
    }
});

Vue.component('tab-autocreateperiods', {
    data: function() {
        return {
            dateFrom: {},
            dateTo: {},
            urlWithAutoCreatePeriods: urlWithAutoCreatePeriods
        }
    },
    template: '<div>' +
                    '<label> Дата периода начала генерации </label>' +
                    '<input type="text" v-model="dateFrom.date">' +

                    '<label> Дата периода конца генерации </label>' +
                    '<input type="text" v-model="dateTo.date">' +

                    '<button @click="sendDataToDB(this.urlWithAutoCreatePeriods, dateFrom, dateTo, `POST`)">' +
                        'Сгенерировать периоды </button>' +
              '</div>',
    methods: {
        sendDataToDB: async function(url, dateFrom, dateTo, method)
        {
            let finalurl = url;
            finalurl = finalurl + "?dateFrom=" + dateFrom.date + "&dateTo=" + dateTo.date;

          let response = await fetch(finalurl, {
            method: method,
            headers: {
              'Content-Type': 'application/json;charset=utf-8'
            }
          });

          let result = await response.json();
          console.log(finalurl);
          console.log(result);
        },
    }
});

Vue.component('tab-eximport', {
    data: function() {
        return {
            urlWithImportExchangeCurrencies: urlWithImportExchangeCurrencies,
            selectedScenario: {},
            allScenarios: allScenarios,
            isAddOnlyNewestRates: {}
        }
    },
    template: '<div>' +
                    '<label> Импорт курсов только: </label>' +
                    '<input type="radio" id="X" value="1" v-model="isAddOnlyNewestRates.value">' +
                    '<label for="X">Новых курсов</label>' +
                    '<input type="radio" id="Y" value="0" v-model="isAddOnlyNewestRates.value">' +
                    '<label for="Y">Всех курсов с затиранием имеющихся</label>' +

                    '<p>Сценарий, в который будут сохранены курсы:</p>' +
                    '<select v-model="selectedScenario">' +
                        '<option v-for="scenario in this.allScenarios"> {{ scenario.name }} </option>' +
                    '</select>' +

                    '<button @click="sendDataToDB(this.urlWithImportExchangeCurrencies, ' +
                                    'selectedScenario, isAddOnlyNewestRates, `POST`)">' +
                        'Импорт курсов валют </button>' +
              '</div>',
    methods: {
        sendDataToDB: async function(url, scenario, isAddOnlyNewestRates, method)
        {
            let finalurl = url;
            var scenario_id = determineIdSprav(scenario, allScenarios);
            finalurl = finalurl + "?scenario_id=" + scenario_id + "&isAddOnlyNewestRates=" + isAddOnlyNewestRates.value;

          let response = await fetch(finalurl, {
            method: method,
            headers: {
              'Content-Type': 'application/json;charset=utf-8'
            }
          });

          let result = await response.json();
          console.log(finalurl);
          console.log(result);
        },
    }
});

Vue.component('tab-autoclosingperiods', {
    data: function() {
        return {
            dateBeforeToClose: {},
            urlWithAutoClosingPeriods: urlWithAutoClosingPeriods,
            selectedScenario: {},
            allScenarios: allScenarios
        }
    },
    template: '<div>' +
                    '<label> Дата периода до которого необходимо закрыть периоды </label>' +
                    '<input type="text" v-model="dateBeforeToClose.date">' +

                    '<p>Сценарий, в котором произойдет закрытие периодов:</p>' +
                    '<select v-model="selectedScenario">' +
                        '<option v-for="scenario in this.allScenarios"> {{ scenario.name }} </option>' +
                    '</select>' +

                    '<button @click="sendDataToDB(this.urlWithAutoClosingPeriods, dateBeforeToClose, selectedScenario, `PUT`)">' +
                        'Закрыть периоды </button>' +
              '</div>',
    methods: {
        sendDataToDB: async function(url, dateBeforeToClose, scenario, method)
        {
            let finalurl = url;
            var scenario_id = determineIdSprav(scenario, allScenarios);
            finalurl = finalurl + "?dateBeforeToClose=" + dateBeforeToClose.date + "&scenario_id=" + scenario_id;

          let response = await fetch(finalurl, {
            method: method,
            headers: {
              'Content-Type': 'application/json;charset=utf-8'
            }
          });

          let result = await response.json();
          console.log(finalurl);
          console.log(result);
        },
    }
});

var app = new Vue({
  el: '#admin',
    data: {
              currentTab: tabs[0].id,
              tabs: tabs,
              closedPeriod: ""
          },
  computed: {
                currentTabComponent: function() {
                      return "tab-" + this.currentTab.toLowerCase();
                    }
            },
  created: function()
  {
     console.log("В методе created: => до присвоения значения this.allScenarios = ", this.allScenarios);
     console.log("В методе created: => до присвоения значения allScenarios = ", allScenarios);

     var promise = getValuesFromServer(urlWithScenarios, null);
     promise.then(scenario => {
            console.log("В методе created: => scenarios после запроса = ", scenario);
            allScenarios = scenario;

             console.log("В методе created: => после присвоения значения allScenarios = ", allScenarios);
        });

     var promise = getValuesFromServer(urlWithCurrencies, null);
     promise.then(currencies => {
            console.log("В методе created: => currencies после запроса = ", currencies);
            allCurrencies = currencies;

            console.log("В методе created: => после присвоения значения allCurrencies = ", allCurrencies);
        });
  }

});