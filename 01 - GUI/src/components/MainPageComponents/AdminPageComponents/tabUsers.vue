<template>
  <div>
    <v-toolbar-title>Учетные записи пользователей</v-toolbar-title>

    <DataTableForDataRight
      :showingData="showingData"
      :showingKeys="showingKeys"
      :urlToDetermineName="URLIK"
      v-on:refreshDataToView="remakeDataToView($event)"
    />
  </div>
</template>

<script>
import DataTableForDataRight from "../Commons/DataTableForDataRight";
import getValuesFromServer from "../../../functions/getValuesFromServer";
import { urlWithUsers } from "../../../generalData";

export default {
  name: "tabUsers",

  components: {
    DataTableForDataRight,
  },
  data: function () {
    return {
      URLIK: {},
      showingData: [],
      showingKeys: [],
    };
  },
  methods: {
    remakeDataToView: function (url) {
      this.URLIK.url = url;
      console.log(this.URLIK.url);
      var promise = getValuesFromServer(url, null);
      promise.then((data) => {
        console.log(data);
        this.showingData = data;
        console.log(this.showingData);
        if (this.showingData.length > 0) {
          this.showingKeys = Object.keys(this.showingData[0]);
          console.log(this.showingKeys);
        } else {
          this.showingKeys = [];
        }
      });
    },
  },
  created: function () {
    this.remakeDataToView(urlWithUsers);
  },
};
</script>

<style scoped>
</style>