<template>
  <v-simple-table align="left" valign="top">
    <tr align="left" valign="top">
      <td>
        <v-col class="d-flex" cols="12" sm="4">
          <v-select
            :items="this.allScenariosNames"
            filled
            dense
            label="Выбранный сценарий-получатель"
            @input="selectScenario"
          >
          </v-select>
        </v-col>
      </td>
    </tr>
    <tr align="left" valign="top">
      <td>
        <v-col class="d-flex" cols="12" sm="4">
          <v-text-field
            label="Период расчета для сценария-получателя:"
            v-model="this.firstOpenPeriod_to"
            disabled
          ></v-text-field>
        </v-col>
      </td>
    </tr>

    <template
      v-if="
        Object.keys(this.selectedScenario_from).length != 0 &&
        Object.keys(this.selectedScenario_to).length != 0
      "
    >
      <v-btn color="success" @click="downloadReport">
        <v-icon> mdi-microsoft-excel </v-icon>
        <v-spacer></v-spacer>
        Выгрузить отчет в формате Excel
      </v-btn>
    </template>

    <v-alert type="error" v-model="this.showAlerts">
      В базе данных не содержится первый открытый период! Обратитесь к
      администратору!
    </v-alert>

    <tr align="left" valign="top">
      <td>
        <ButtonTableLeft
          :array_buttons_to_show="tabs2"
          @refreshDataToView="showTabForEntry"
        />
      </td>
      <td>
        <tabLeasingdeposits
          v-if="this.currentTabComponent2Level === 'leasingdeposits'"
        />
      </td>
      <td>
        <tabCalculate v-if="this.currentTabComponent2Level === 'calculate'" />
      </td>
      <td>
        <tabRegLd1 v-if="this.currentTabComponent2Level === 'reg_ld_1'" />
      </td>
      <td>
        <tabRegLd2 v-if="this.currentTabComponent2Level === 'reg_ld_2'" />
      </td>
      <td>
        <tabRegLd3 v-if="this.currentTabComponent2Level === 'reg_ld_3'" />
      </td>
      <td>
        <tabEntriesIfrs
          v-if="this.currentTabComponent2Level === 'entriesifrs'"
        />
      </td>
    </tr>
  </v-simple-table>
</template>

<script>
import {
  GD_tabs_2Level,
  urlWithDownloadingReportForCalculation,
} from "../../../generalData.js";
import ButtonTableLeft from "../Commons/ButtonTableLeft";
import tabCalculate from "./tabEntriesComponents/tabCalculate";
import tabLeasingdeposits from "./tabEntriesComponents/tabLeasingdeposits";
import tabRegLd1 from "./tabEntriesComponents/tabRegLd1";
import tabRegLd2 from "./tabEntriesComponents/tabRegLd2";
import tabRegLd3 from "./tabEntriesComponents/tabRegLd3";
import tabEntriesIfrs from "./tabEntriesComponents/tabEntriesIfrs";
import getFileFromServer from "../../../functions/getFileFromServer";

export default {
  name: "tabEntries",
  components: {
    ButtonTableLeft,
    tabCalculate,
    tabLeasingdeposits,
    tabEntriesIfrs,
    tabRegLd1,
    tabRegLd2,
    tabRegLd3,
  },
  data: function () {
    return {
      tabs2: GD_tabs_2Level,
      currentTab2: GD_tabs_2Level[0].id,
      currentTabComponent2Level: "",
      selectedScenario_toName: "",
      urlWithDownloadingReportForCalculation: urlWithDownloadingReportForCalculation,
    };
  },
  computed: {
    allScenariosNames: function () {
      return this.$store.getters.getScenarioNames;
    },
    firstOpenPeriod_to: function () {
      return this.$store.getters.getFirstOpenPeriod_to;
    },
    showAlerts: function () {
      console.log("showAlerts");
      console.log(
        "this.selectedScenario_toName = " + this.selectedScenario_toName
      );
      console.log("this.firstOpenPeriod_to = " + this.firstOpenPeriod_to);

      return (
        this.selectedScenario_toName !== "" && this.firstOpenPeriod_to === ""
      );
    },
    selectedScenario_from: function () {
      return this.$store.getters.getScenarioFrom;
    },
    selectedScenario_to: function () {
      return this.$store.getters.getScenarioTo;
    },
  },
  methods: {
    showTabForEntry: function (buttonNumber) {
      console.log("Метод showTabForEntry словил событие");
      console.log("buttonNumber =>", buttonNumber);
      this.currentTabComponent2Level = buttonNumber;
      console.log(
        "this.currentTabComponent2Level => " + this.currentTabComponent2Level
      );
    },
    selectScenario: function (scenarioName) {
      console.log(scenarioName);
      this.selectedScenario_toName = scenarioName;
      this.$store.dispatch("getFirstOpenDateInScenario_To", { scenarioName });
      this.$store.dispatch("setScenarioTo", { scenarioName });
    },
    downloadReport: function () {
      let params = {
        scenarioFromId: this.selectedScenario_from.id,
        scenarioToId: this.selectedScenario_to.id,
      };

      getFileFromServer(this.urlWithDownloadingReportForCalculation, params);

      // promise.then(data => console.log(data));
    },
  },
};
</script>

<style scoped>
</style>