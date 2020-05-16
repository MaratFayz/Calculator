var tabs = [{"id" : "users", "name" : "Управление пользователями"},
            {"id" : "exImport", "name" : "Импорт курсов валют"},
            {"id" : "autoCreatePeriods", "name" : "Автоматическое создание периодов"},
            {"id" : "autoClosingPeriods", "name" : "Автоматическое закрытие периодов"}]

var selectedScenario_from = "";
var selectedScenario_to = "";
var allScenarios = [];

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

new Vue('tab-users',
);

new Vue('tab-eximport',
);

new Vue('tab-autocreateperiods',
);

new Vue('tab-autoclosingperiods',
);

var app = new Vue({
  el: '#admin',
    data: {
              currentTab: tabs[0].id,
              tabs: tabs,
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