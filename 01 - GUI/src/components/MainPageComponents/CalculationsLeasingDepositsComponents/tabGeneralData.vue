<template>
  <v-simple-table align="left" valign="top">
    <tr align="left" valign="top">
      <td>
        <ButtonTableLeft
          :array_buttons_to_show="spravochniki"
          @refreshDataToView="remakeDataToView"
        />
      </td>
      <td>
        <DataTableForDataRight
          :showingData="showingData"
          :showingKeys="showingKeys"
          :urlToDetermineName="URLIK"
          @refreshDataToView="remakeDataToView"
        />
      </td>
    </tr>
  </v-simple-table>
</template>

<script>
import ButtonTableLeft from "../Commons/ButtonTableLeft";
import DataTableForDataRight from "../Commons/DataTableForDataRight";
import { GD_spravochniki } from "../../../generalData.js";
import getValuesFromServer from "../../../functions/getValuesFromServer.js";

export default {
  name: "tabGeneralData",

  components: {
    ButtonTableLeft,
    DataTableForDataRight,
  },

  data: function () {
    return {
      spravochniki: GD_spravochniki,
      URLIK: {},
      showingData: [],
    };
  },
  methods: {
    remakeDataToView: function (url) {
      console.log("remakeDataToView словил событие и параметр => " + url);
      this.URLIK = { url: url };
      var promise = getValuesFromServer(url, null);
      promise.then((data) => {
        console.log(data);
        this.showingData = data;
        console.log(this.showingData);
      });
    },
  },
  computed: {
    showingKeys: function () {
      let showingKeys = [];

      if (this.showingData.length > 0) {
        showingKeys = Object.keys(this.showingData[0]);
        console.log(showingKeys);
      }

      return showingKeys;
    },
  },
};
</script>

<style scoped>
</style>