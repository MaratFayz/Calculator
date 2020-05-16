var urlWithScenarios = "/scenarios";
var urlWithLeasingDeposits = "/leasingDeposits";
var urlWithLeasingDepositsFor2Scenarios = urlWithLeasingDeposits + "/for2Scenarios";
var urlWithEntryRegLD1 = "/entries/regld1";
var urlWithEntryRegLD2 = "/entries/regld2";
var urlWithEntryRegLD3 = "/entries/regld3";
var urlWithEntryIFRSAcc = "/entriesIFRS";
var urlWithEntryIFRSAccFor2Scenarios = urlWithEntryIFRSAcc + "/forDate";
var urlWithEntryCalculator = "/entries/calculator"

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
                       {"id" : "/entries", "name" : "Проводки"},
                       {"id" : urlWithEntryIFRSAcc, "name" : "Проводки на счетах МСФО"},
                       {"id" : "/exchangeRates", "name" : "Курсы валют"},
                       {"id" : "/ifrsAccounts", "name" : "Счета МСФО"},
                       {"id" : urlWithLeasingDeposits, "name" : "Лизинговые депозиты"},
                       {"id" : "/periods", "name" : "Периоды"},
                       {"id" : "/periodsClosed", "name" : "Закрытие периодов"},
                       {"id" : urlWithScenarios, "name" : "Сценарии"}]

var selectedScenario_from = "";
var selectedScenario_to = "";
var allScenarios = [];

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

async function getFirstOpenDateInScenario(selectedScenario, allScenarios)
{
  console.log("getFirstOpenDateInScenario => selectedScenario = " + JSON.stringify(selectedScenario));
  console.log("getFirstOpenDateInScenario => allScenarios = " + JSON.stringify(allScenarios));
  var selectedScenario_id;

  if(selectedScenario != null)
  {
    selectedScenario_id = determineIdSprav(selectedScenario, allScenarios);

    console.log("'/periodsClosed' + selectedScenario_id = " + '/periodsClosed/' + selectedScenario_id);

    let url = '/periodsClosed/' + selectedScenario_id;

    var response = await fetch(url);
    var closedPeriod = "";

    if (response.ok)
    { // если HTTP-статус в диапазоне 200-299
          // получаем тело ответа (см. про этот метод ниже)
        closedPeriod = await response.text(); // читаем ответ в формате text
    }
    else
    {
        console.log("Ошибка HTTP: " + response.status);
    }

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
            showingKeys : []
        }
    },
    template:
               '<table align="left" valign="top">' +
                    '<caption>Значения справочников</caption>' +
                    '<tr align="left" valign="top">' +
                          '<td>' +
                            '<ButtonTableLeft :array_buttons_to_show = "spravochniki" ' +
                                ':throwingEvent = "`refreshDataToView`" ' +
                                'v-on:refreshDataToView=remakeDataToView($event) />' +
                          '</td>' +
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
}});

Vue.component("ShowDataForTwoScenarios", {
    props: ['stringForNoValues', 'url'],
    data: function() {
        return {
            "data_to_show": [],
            "data_keys_to_show": []
        }
    },
    template:
              '<div>' +
                    '<DataTable :showButtonsForEditAndDelete="`false`"' +
                                ':showingData="data_to_show"' +
                                ':showingKeys="data_keys_to_show"' +
                                ':urlToDetermineName="``"' +
                                ':stringForNoValues="stringForNoValues" />' +
              '</div>',
    mounted: function() {
        var url = this.url;
        console.log("mounted => url = ", url);
        console.log("mounted => allScenarios = ", allScenarios);
        console.log("mounted => selectedScenario_from = ", selectedScenario_from);
        var selectedScenario_from_id = determineIdSprav(selectedScenario_from, allScenarios);
        console.log("mounted => selectedScenario_from_id = ", selectedScenario_from_id);
        console.log("mounted => selectedScenario_to = ", selectedScenario_to);
        var selectedScenario_to_id = determineIdSprav(selectedScenario_to, allScenarios);
        console.log("mounted => selectedScenario_to_id = ", selectedScenario_to_id);

        var params = {};
        console.log("mounted => params = ", JSON.stringify(params));
        console.log("mounted => params.scenarioFromId = ", JSON.stringify(params.scenarioFromId));
        console.log("mounted => params.scenarioToId = ", JSON.stringify(params.scenarioToId));

        if(selectedScenario_from_id != undefined) params.scenarioFromId = selectedScenario_from_id;
        if(selectedScenario_to_id != undefined) params.scenarioToId = selectedScenario_to_id;
        if(params.scenarioFromId == undefined && params.scenarioToId != undefined) params.scenarioFromId = params.scenarioToId;
        if(params.scenarioFromId != undefined && params.scenarioToId == undefined) params.scenarioToId = params.scenarioFromId;

        console.log("mounted => params = ", JSON.stringify(params));

        var promise = getValuesFromServer(url, params);

        promise.then(response => {
            console.log(response);
            this.data_to_show = response;
            this.data_keys_to_show = Object.keys(this.data_to_show[0]);
        });
    }
});

Vue.component("tab-leasingdeposits", {
    data: function()
    {
      return {
        urlWithLeasingDepositsFor2Scenarios: urlWithLeasingDepositsFor2Scenarios
      }
    },
    template:
              '<div>' +
                    '<ShowDataForTwoScenarios :url="urlWithLeasingDepositsFor2Scenarios"' +
                                ':stringForNoValues="`Депозиты, по которым будут рассчитаны проводки, отсутствуют`" />' +
              '</div>'
});

Vue.component("tab-reg_ld_1", {
    data: function()
    {
      return {
        urlWithEntryRegLD1: urlWithEntryRegLD1
      }
    },
    template:
              '<div>' +
                    '<ShowDataForTwoScenarios :url="urlWithEntryRegLD1"' +
                                ':stringForNoValues="`Рассчитанные проводки отсутствуют`" />' +
              '</div>'
});

Vue.component("tab-reg_ld_2", {
    data: function()
    {
      return {
        urlWithEntryRegLD2: urlWithEntryRegLD2
      }
    },
    template:
              '<div>' +
                    '<ShowDataForTwoScenarios :url="urlWithEntryRegLD2"' +
                                ':stringForNoValues="`Рассчитанные проводки отсутствуют`" />' +
              '</div>'
});

Vue.component("tab-reg_ld_3", {
    data: function()
    {
      return {
        urlWithEntryRegLD3: urlWithEntryRegLD3
      }
    },
    template:
              '<div>' +
                    '<ShowDataForTwoScenarios :url="urlWithEntryRegLD3"' +
                                ':stringForNoValues="`Рассчитанные проводки отсутствуют`" />' +
              '</div>'
});

Vue.component("tab-entriesifrs", {
    data: function()
    {
      return {
        urlWithEntryIFRSAccFor2Scenarios: urlWithEntryIFRSAccFor2Scenarios
      }
    },
    template:
              '<div>' +
                    '<ShowDataForTwoScenarios :url="urlWithEntryIFRSAccFor2Scenarios"' +
                                ':stringForNoValues="`Рассчитанные проводки отсутствуют`" />' +
              '</div>'
});

Vue.component("tab-calculate", {
    computed: {
        selectedScenario_from_: function() {
            return selectedScenario_from;
        },
        selectedScenario_to_: function() {
           return selectedScenario_to;
        },
    },
    template: '<div>' +
                    '<template v-if="selectedScenario_from_ != `` && selectedScenario_to_ != `` "> ' +
                        '<button ' +
                              'v-bind:value="`calculate`" ' +
                              'class="spravochnikButton" ' +
                              '@click="calculate()"> ' +
                                      'Рассчитать проводки за период \n' +
                                      'со сценария-источника: ' +
                                      '{{ selectedScenario_from_ }} \n' +
                                      'на сценарий-получатель: ' +
                                      '{{ selectedScenario_to_ }}' +
                        '</button>' +
                    '</template> '+
                    '<template v-else> ' +
                        '<button ' +
                              'v-bind:value="calculate" ' +
                              'class="spravochnikButton" ' +
                              'v-bind:class="{active: true}" ' +
                              'disabled = true ' +
                              '> ' +
                                      'Выберите сценарий-источник и сценарий-получатель для расчета проводок' +
                        '</button>' +
                    '</template> '+
              '</div>',
    methods: {
        calculate: async function()
        {
            console.log("calculate => selectedScenario_from = ", selectedScenario_from);
            console.log("calculate => selectedScenario_to = ", selectedScenario_to);

            var selectedScenario_from_id = determineIdSprav(selectedScenario_from, allScenarios);
            console.log("calculate => selectedScenario_from_id = ", selectedScenario_from_id);
            console.log("calculate => selectedScenario_to = ", selectedScenario_to);
            var selectedScenario_to_id = determineIdSprav(selectedScenario_to, allScenarios);
            console.log("calculate => selectedScenario_to_id = ", selectedScenario_to_id);

            if(selectedScenario_from_id != undefined && selectedScenario_to_id != undefined)
            {
                let finalurl = urlWithEntryCalculator + "?scenario_from=" +
                               selectedScenario_from_id + "&scenario_to=" +
                               selectedScenario_to_id;

                let response = await fetch(finalurl, {
                    method: "POST",
                    headers: {
                      'Content-Type': 'application/json;charset=utf-8'
                    },
                    body: JSON.stringify()
                });

                let promise = await response.json();
                promise.then()
                console.log(JSON.stringify(answer));
                console.log(finalurl);
                console.log(result);
            }
            else
            {
                alert("Не выбран один из сценариев! Расчет выполнен не будет!");
            }
        }
    }
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
                                '<p>Период расчета для сценария-получателя: </p>' +
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
           selectedScenario_to = val;

           let promise = getFirstOpenDateInScenario(val, this.allScenarios);
           promise.then(date => this.closedPeriod = date);
        }
    },
    created: function()
    {
       console.log("В методе created tab-entries: => до присвоения значения this.allScenarios = ", this.allScenarios);

        var promise = getValuesFromServer(urlWithScenarios, null);
        promise.then(scenario => {
            console.log("В методе created tab-entries: => ", scenario);
            this.allScenarios = scenario;
            console.log("В методе created tab-entries: => после присвоения значения this.allScenarios = ", this.allScenarios);
        });
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
                selectedScenario: function (val, oldVal)
                {
                    selectedScenario_from = val;

                    let promise = getFirstOpenDateInScenario(val, this.allScenarios);
                    promise.then(date => this.closedPeriod = date);
                }
            },
  created: function()
  {
     console.log("В методе created: => до присвоения значения this.allScenarios = ", this.allScenarios);
     console.log("В методе created: => до присвоения значения allScenarios = ", allScenarios);

     var promise = getValuesFromServer(urlWithScenarios, null);
     promise.then(scenario => {
            console.log("В методе created: => scenarios после запроса = ", scenario);
            this.allScenarios = scenario;
            allScenarios = this.allScenarios;

             console.log("В методе created: => после присвоения значения this.allScenarios = ", this.allScenarios);
             console.log("В методе created: => после присвоения значения allScenarios = ", allScenarios);
        });
  }

});




