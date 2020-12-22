<template>
  <div>
    <v-row>
      <v-col class="d-flex" cols="12" sm="5">
        <v-menu
          ref="menu"
          v-model="menu"
          :close-on-content-click="false"
          :return-value.sync="this.monthBeforeToClose"
          transition="scale-transition"
          offset-y
          max-width="290px"
          min-width="290px"
        >
          <template v-slot:activator="{ on, attrs }">
            <v-text-field
              v-model="monthBeforeToClose"
              label="Дата периода до которого необходимо закрыть периоды:"
              prepend-icon="mdi-calendar"
              readonly
              v-bind="attrs"
              v-on="on"
              clearable
            ></v-text-field>
          </template>
          <v-date-picker
            v-model="monthBeforeToClose"
            type="month"
            no-title
            scrollable
          >
            <v-spacer></v-spacer>
            <v-btn
              text
              color="primary"
              @click="$refs.menu.save(monthBeforeToClose)"
            >
              Сохранение
            </v-btn>
          </v-date-picker>
        </v-menu>
      </v-col>
    </v-row>

    <v-row align="center">
      <v-col class="d-flex" cols="12" sm="4">
        <v-select
          :items="this.allScenariosNames"
          filled
          label="Сценарий, в котором произойдет закрытие периодов:"
          dense
          @input="selectScenario"
        ></v-select>
      </v-col>
    </v-row>

    <v-row
      v-if="this.selectedScenarioName !== '' && this.monthBeforeToClose !== ''"
    >
      <v-btn
        color="success"
        @click="sendDataToDB(monthBeforeToClose, selectedScenarioName, `PUT`)"
      >
        Закрыть периоды
      </v-btn>
    </v-row>

    <v-row
      v-else-if="
        this.selectedScenarioName === '' && this.monthBeforeToClose === ''
      "
    >
      <v-alert type="error">
        Выберите период, до которого будут закрыты периоды, а также сценарий!
      </v-alert>
    </v-row>
    <v-row
      v-else-if="
        this.selectedScenarioName !== '' && this.monthBeforeToClose === ''
      "
    >
      <v-alert type="error">
        Выберите период, до которого будут закрыты периоды!
      </v-alert>
    </v-row>
    <v-row
      v-else-if="
        this.selectedScenarioName === '' && this.monthBeforeToClose !== ''
      "
    >
      <v-alert type="error">
        Выберите сценарий, в котором будут закрыты периоды!
      </v-alert>
    </v-row>
  </div>
</template>

<script>
import { urlWithAutoClosingPeriods } from "../../../generalData";
import { determineScenarioId } from "../../../functions/determineIdSprav";

export default {
  name: "tabAutoclosingPeriods",
  data: function () {
    return {
      selectedScenarioName: "",
      monthBeforeToClose: "",
      urlWithAutoClosingPeriods2: urlWithAutoClosingPeriods,
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
    sendDataToDB: async function (monthBeforeToClose, scenario, method) {
      let finalurl = this.urlWithAutoClosingPeriods2;
      console.log("finalurl = " + finalurl);
      var scenario_id = determineScenarioId(scenario);
      console.log("scenario_id = " + scenario_id);

      finalurl =
        finalurl +
        "?monthBeforeToClose=" +
        monthBeforeToClose +
        "&scenario_id=" +
        scenario_id;

      let response = await fetch(finalurl, {
        method: method,
        headers: {
          "Content-Type": "application/json;charset=utf-8",
        },
      });

      console.log(finalurl);
      console.log(response);
    },
  },
};
</script>

<style scoped>
</style>