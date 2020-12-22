<template>
  <v-row>
    <v-tabs vertical @change="clickToPerformButtonTable">
      <v-tab
        v-for="button_to_show in array_buttons_to_show"
        :key="button_to_show.id"
      >
        {{ button_to_show.name }}
      </v-tab>
    </v-tabs>
  </v-row>
</template>

<script>
export default {
  name: "ButtonTableLeft",
  props: ["array_buttons_to_show"],
  data: function () {
    return {
      currentSpravTab: "",
    };
  },
  methods: {
    clickToPerformButtonTable: function (buttonNumber) {
      console.log(
        "В методе clickToPerformButtonTable, this.currentSpravTab до присвоения значения = ",
        this.currentSpravTab
      );
      this.currentSpravTab = this.array_buttons_to_show[buttonNumber].id;
      console.log(
        "В методе clickToPerformButtonTable, this.currentSpravTab после присвоения значения = ",
        this.currentSpravTab
      );

      console.log(
        "В методе clickToPerformButtonTable, this.array_buttons_to_show[buttonNumber].id = ",
        this.array_buttons_to_show[buttonNumber].id
      );

      this.$emit("refreshDataToView", this.currentSpravTab);
      console.log(
        "В методе clickToPerformButtonTable 2, url = ",
        this.currentSpravTab
      );
    },
  },
};
</script>

<style scoped>
</style>