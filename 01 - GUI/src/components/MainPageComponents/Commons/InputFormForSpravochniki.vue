<template>
  <v-dialog v-model="this.showForm" persistent max-width="600px">
    <v-card>
      <v-card-title>
        <span class="headline"
          >{{ actionName }} значения справочника '{{ spravochnik_name }}'</span
        >
      </v-card-title>
      <v-card-text>
        <v-container>
          <v-row v-for="key in showingKeys" :key="key">
            <v-col cols="4">
              <v-subheader> {{ key }}</v-subheader>
            </v-col>
            <v-col cols="8">
              <v-text-field v-model="answer[key]" clearable></v-text-field>
            </v-col>
          </v-row>
        </v-container>
      </v-card-text>
      <v-card-actions>
        <v-spacer></v-spacer>
        <v-btn
          color="blue darken-1"
          text
          @click="sendDataToDB(url, answer, method)"
        >
          {{ actionName }}
        </v-btn>
        <v-btn color="blue darken-1" text @click="$emit(`hideForm`)">
          Скрыть форму
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script>
import { mainUrl } from "../../../generalData";

export default {
  name: "InputFormForSpravochniki",
  props: [
    "showingKeys",
    "showForm",
    "spravochnik_name",
    "url",
    "method",
    "actionName",
    "dataInForm",
  ],
  data: function () {
    return {
      answer: {},
    };
  },
  watch: {
    showingKeys: function (val, oldVal) {
      console.log("showingKeys => oldVal=" + oldVal);
      this.refreshDataInForm(val, this.dataInForm);
    },
    dataInForm: function (val, oldVal) {
      console.log("showingKeys => oldVal=" + oldVal);
      this.refreshDataInForm(this.showingKeys, val);
    },
  },
  methods: {
    sendDataToDB: async function (url, answer, method) {
      this.dialog = false;

      let finalurl = mainUrl + url;
      if (method.toLowerCase() == "put")
        if (answer.id != null) finalurl = finalurl + "/" + answer.id;

      let response = await fetch(finalurl, {
        method: method,
        headers: {
          "Content-Type": "application/json;charset=utf-8",
        },
        body: JSON.stringify(answer),
      });

      let result = await response.json();
      console.log(JSON.stringify(answer));
      console.log(finalurl);
      console.log(result);
      this.$emit("hideForm");
      this.$emit("refreshDataToView", url);
    },
    refreshDataInForm: function (keys, data) {
      let A = new Object();

      for (let k of keys) {
        console.log("InputFormForSpravochniki:refreshDataInForm k = " + k);

        try {
          // console.log("watch: keys => A: " + JSON.stringify(A));
          A[k] = data[k];
        } catch {
          // console.log("watch: keys => A: " + JSON.stringify(A));
          A[k] = "";
        }
      }

      // console.log("watch: keys => A: " + JSON.stringify(A));
      this.answer = A;
      // console.log(
      //   "watch: keys => this.answer = " + JSON.stringify(this.answer)
      // );
    },
  },
};
</script>

<style scoped>
</style>