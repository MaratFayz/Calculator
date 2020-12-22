<template>
  <v-container fluid>
    <v-toolbar-title>Лизинговые депозиты</v-toolbar-title>

    <v-row align="center">
      <v-col class="d-flex" cols="12" sm="4">
        <v-select
          :items="this.allScenariosNames"
          filled
          label="Выбранный сценарий-источник"
          dense
          @input="selectScenario"
        ></v-select>
      </v-col>
    </v-row>

    <v-row align="center">
      <v-col class="d-flex" cols="12" sm="4">
        <v-text-field
          label="Период расчета для сценария-источника:"
          v-model="this.firstOpenPeriod_from"
          disabled
        ></v-text-field>
      </v-col>
    </v-row>

    <v-alert type="error" v-model="this.showAlerts">
      В базе данных не содержится первый открытый период! Обратитесь к
      администратору!
    </v-alert>

    <v-row>
      <v-tabs background-color="primary" dark v-model="tabs">
        <v-tab :key="1"> Значения справочников </v-tab>
        <v-tab :key="2"> Проводки по депозитам </v-tab>
        <v-tabs-items v-model="tabs">
          <v-tab-item :key="1">
            <tabGeneralData />
          </v-tab-item>

          <v-tab-item :key="2">
            <tabEntries />
          </v-tab-item>
        </v-tabs-items>
      </v-tabs>
    </v-row>
  </v-container>
</template>

<script>
import tabGeneralData from "./CalculationsLeasingDepositsComponents/tabGeneralData.vue";
import tabEntries from "./CalculationsLeasingDepositsComponents/tabEntries.vue";

export default {
  name: "CalculationsLeasingDeposits",
  components: {
    tabGeneralData,
    tabEntries,
  },
  data: function () {
    return {
      tabs: {},
      selectedScenario_fromName: "",
    };
  },
  methods: {
    selectScenario: function (scenarioName) {
      console.log(scenarioName);
      this.selectedScenario_fromName = scenarioName;
      this.$store.dispatch("getFirstOpenDateInScenario_From", { scenarioName });
      this.$store.dispatch("setScenarioFrom", { scenarioName });
    },
  },
  computed: {
    allScenariosNames: function () {
      return this.$store.getters.getScenarioNames;
    },
    firstOpenPeriod_from: function () {
      return this.$store.getters.getFirstOpenPeriod_from;
    },
    showAlerts: function () {
      console.log("showAlerts");
      console.log(
        "this.selectedScenario_fromName = " + this.selectedScenario_fromName
      );
      console.log("this.firstOpenPeriod_from = " + this.firstOpenPeriod_from);

      return (
        this.selectedScenario_fromName !== "" &&
        this.firstOpenPeriod_from === ""
      );
    },
  },
};
</script>

    <style scoped>
</style>