<template>
  <div>
    <v-toolbar-title>Импорт курсов только: </v-toolbar-title>

    <v-radio-group v-model="isAddOnlyNewestRates">
      <v-radio :label="`Новых курсов`" :value="true"></v-radio>
      <v-radio
        :label="`Всех курсов с затиранием имеющихся`"
        :value="false"
      ></v-radio>
    </v-radio-group>

    <v-row align="center">
      <v-col class="d-flex" cols="12" sm="4">
        <v-select
          :items="this.allScenariosNames"
          filled
          label="Сценарий, в который будут сохранены курсы:"
          dense
          @input="selectScenario"
        ></v-select>
      </v-col>
    </v-row>

    <v-row v-if="this.selectedScenarioName !== ''">
      <v-btn @click="sendDataToDB(`POST`)" color="success">
        Импорт курсов валют
      </v-btn>
    </v-row>
    <v-row v-else>
      <v-alert type="error"> Выберите границы генерации периодов! </v-alert>
    </v-row>
  </div>
</template>

<script>
import { urlWithImportExchangeCurrencies } from "../../../generalData";
import { determineScenarioId } from "../../../functions/determineIdSprav";

export default {
  name: "tabExImport",
  data: function () {
    return {
      urlWithImportExchangeCurrencies: urlWithImportExchangeCurrencies,
      selectedScenarioName: "",
      isAddOnlyNewestRates: true,
    };
  },
  computed: {
    allScenariosNames: function () {
      return this.$store.getters.getScenarioNames;
    },
  },
  methods: {
    selectScenario: function (scenarioName) {
      console.log(scenarioName);
      this.selectedScenarioName = scenarioName;
    },
    sendDataToDB: async function (method) {
      let finalurl = this.urlWithImportExchangeCurrencies;
      var scenario_id = determineScenarioId(this.selectedScenarioName);
      finalurl =
        finalurl +
        "?scenario_id=" +
        scenario_id +
        "&isAddOnlyNewestRates=" +
        this.isAddOnlyNewestRates;

      let response = await fetch(finalurl, {
        method: method,
        headers: {
          "Content-Type": "application/json;charset=utf-8",
        },
      });

      let result = await response.json();
      console.log(finalurl);
      console.log(result);
    },
  },
};
</script>