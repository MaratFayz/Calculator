<template>
  <div>
    <v-row>
      <v-col class="d-flex" cols="12" sm="5">
        <v-menu
          ref="menu"
          v-model="menu"
          :close-on-content-click="false"
          :return-value.sync="this.monthFrom"
          transition="scale-transition"
          offset-y
          max-width="290px"
          min-width="290px"
        >
          <template v-slot:activator="{ on, attrs }">
            <v-text-field
              v-model="monthFrom"
              label="Дата периода начала генерации"
              prepend-icon="mdi-calendar"
              readonly
              v-bind="attrs"
              v-on="on"
              clearable
            ></v-text-field>
          </template>
          <v-date-picker v-model="monthFrom" type="month" no-title scrollable>
            <v-spacer></v-spacer>
            <v-btn text color="primary" @click="$refs.menu.save(monthFrom)">
              Сохранение
            </v-btn>
          </v-date-picker>
        </v-menu>
      </v-col>
    </v-row>

    <v-row>
      <v-col class="d-flex" cols="12" sm="5">
        <v-menu
          ref="menu2"
          v-model="menu2"
          :close-on-content-click="false"
          :return-value.sync="this.monthTo"
          transition="scale-transition"
          offset-y
          max-width="290px"
          min-width="290px"
        >
          <template v-slot:activator="{ on, attrs }">
            <v-text-field
              v-model="monthTo"
              label="Дата периода конца генерации"
              prepend-icon="mdi-calendar"
              readonly
              v-bind="attrs"
              v-on="on"
              clearable
            ></v-text-field>
          </template>
          <v-date-picker v-model="monthTo" type="month" no-title scrollable>
            <v-spacer></v-spacer>
            <v-btn text color="primary" @click="$refs.menu2.save(monthTo)">
              Сохранение
            </v-btn>
          </v-date-picker>
        </v-menu>
      </v-col>
    </v-row>

    <v-row v-if="this.monthFrom !== '' && this.monthTo !== ''">
      <v-btn @click="sendDataToDB(`POST`)" color="success">
        Сгенерировать периоды
      </v-btn>
    </v-row>

    <v-row v-else>
      <v-alert type="error"> Выберите границы генерации периодов! </v-alert>
    </v-row>
  </div>
</template>

<script>
import { urlWithAutoCreatePeriods } from "../../../generalData";

export default {
  name: "tabAutoCreatePeriods",
  data: function () {
    return {
      monthFrom: "",
      monthTo: "",
      urlWithAutoCreatePeriods: urlWithAutoCreatePeriods,
    };
  },
  methods: {
    sendDataToDB: async function (method) {
      let finalurl = this.urlWithAutoCreatePeriods;
      finalurl =
        finalurl + "?dateFrom=" + this.monthFrom + "&dateTo=" + this.monthTo;

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