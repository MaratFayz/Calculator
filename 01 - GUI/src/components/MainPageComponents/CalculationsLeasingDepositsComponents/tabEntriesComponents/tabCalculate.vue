<template>
  <div>
    <template
      v-if="
        Object.keys(this.selectedScenario_from).length != 0 &&
        Object.keys(this.selectedScenario_to).length != 0
      "
    >
      <v-row>
        <v-col class="d-flex" cols="12" sm="5">
          <v-menu
            ref="menu"
            v-model="menu"
            :close-on-content-click="false"
            :return-value.sync="this.dateCopyStart"
            transition="scale-transition"
            offset-y
            max-width="290px"
            min-width="290px"
          >
            <template v-slot:activator="{ on, attrs }">
              <v-text-field
                v-model="dateCopyStart"
                label="Дата начала копирования со сценария-источника:"
                prepend-icon="mdi-calendar"
                readonly
                v-bind="attrs"
                v-on="on"
                clearable
              ></v-text-field>
            </template>
            <v-date-picker
              v-model="dateCopyStart"
              type="month"
              no-title
              scrollable
            >
              <v-spacer></v-spacer>
              <v-btn
                text
                color="primary"
                @click="$refs.menu.save(dateCopyStart)"
              >
                Сохранение
              </v-btn>
            </v-date-picker>
          </v-menu>
        </v-col>
      </v-row>
      <v-col cols="12" sm="4">
        <v-row>
          <v-btn
            color="success"
            class="spravochnikButton"
            @click="calculate"
            width="350"
            height="100"
            left
          >
            Рассчитать проводки за период <br />
            со сценария-источника: {{ this.selectedScenario_from.name }} <br />
            на сценарий-получатель: {{ this.selectedScenario_to.name }}
          </v-btn>
        </v-row>
      </v-col>
    </template>
    <template v-else>
      <v-alert type="error">
        Выберите сценарий-источник и сценарий-получатель для расчета проводок
      </v-alert>
    </template>
  </div>
</template>

<script>
import { urlWithEntryCalculator } from "../../../../generalData.js";

export default {
  name: "tabCalculate",
  data: function () {
    return {
      urlWithEntryCalculator: urlWithEntryCalculator,
      dateCopyStart: "",
    };
  },
  computed: {
    selectedScenario_from: function () {
      return this.$store.getters.getScenarioFrom;
    },
    selectedScenario_to: function () {
      return this.$store.getters.getScenarioTo;
    },
  },
  methods: {
    calculate: async function () {
      console.log(
        "calculate => selectedScenario_from = ",
        this.selectedScenario_from
      );
      console.log(
        "calculate => this.selectedScenario_to = ",
        this.selectedScenario_to
      );

      var selectedScenario_from_id = this.selectedScenario_from.id;

      console.log(
        "calculate => selectedScenario_from_id = ",
        selectedScenario_from_id
      );

      var selectedScenario_to_id = this.selectedScenario_to.id;

      console.log(
        "calculate => selectedScenario_to_id = ",
        selectedScenario_to_id
      );
      console.log("calculate => dateCopyStart = ", this.dateCopyStart);

      if (
        selectedScenario_from_id != undefined &&
        selectedScenario_to_id != undefined
      ) {
        let dateCopy;
        if (this.dateCopyStart == null || this.dateCopyStart === '') {
          dateCopy = null;
        } else {
          dateCopy = this.dateCopyStart + "-01";
        }

        let calculateEntriesRequestDto = {
          scenarioFrom: selectedScenario_from_id,
          scenarioTo: selectedScenario_to_id,
          dateCopyStart: dateCopy,
        };

        let response = await fetch(urlWithEntryCalculator, {
          method: "POST",
          headers: {
            "Content-Type": "application/json;charset=utf-8",
          },
          body: JSON.stringify(calculateEntriesRequestDto),
        });

        let promise = await response.json();
        promise.then();
        console.log(urlWithEntryCalculator);
      } else {
        alert("Не выбран один из сценариев! Расчет выполнен не будет!");
      }
    },
  },
};
</script>

<style>
</style>