<template>
  <div>
    <DataTable
      :showButtonsForEditAndDelete="`false`"
      :showingData="data_to_show"
      :showingKeys="data_keys_to_show"
      :urlToDetermineName="``"
      :stringForNoValues="stringForNoValues"
    />
  </div>
</template>

<script>
import DataTable from "../../../Commons/DataTable";
import getValuesFromServer from "../../../../../functions/getValuesFromServer";

export default {
  name: "ShowDataForTwoScenarios",

  components: {
    DataTable,
  },

  props: ["stringForNoValues", "url"],
  data: function () {
    return {
      data_to_show: [],
      data_keys_to_show: [],
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
  mounted: function () {
    var url = this.url;
    console.log("mounted => url = ", url);
    console.log(
      "mounted => selectedScenario_from = ",
      this.selectedScenario_from
    );
    var selectedScenario_from_id = this.selectedScenario_from.id;
    console.log(
      "mounted => selectedScenario_from_id = ",
      selectedScenario_from_id
    );
    console.log("mounted => selectedScenario_to = ", this.selectedScenario_to);
    var selectedScenario_to_id = this.selectedScenario_to.id;

    console.log("mounted => selectedScenario_to_id = ", selectedScenario_to_id);

    var params = {};
    console.log("mounted => params = ", JSON.stringify(params));
    console.log(
      "mounted => params.scenarioFromId = ",
      JSON.stringify(params.scenarioFromId)
    );
    console.log(
      "mounted => params.scenarioToId = ",
      JSON.stringify(params.scenarioToId)
    );

    if (selectedScenario_from_id != undefined)
      params.scenarioFromId = selectedScenario_from_id;
    if (selectedScenario_to_id != undefined)
      params.scenarioToId = selectedScenario_to_id;

    console.log("mounted => params = ", JSON.stringify(params));

    var promise = getValuesFromServer(url, params);

    promise.then((response) => {
      console.log(response);
      this.data_to_show = response;

      if (this.data_to_show.length > 0) {
        this.data_keys_to_show = Object.keys(this.data_to_show[0]);
      } else {
        this.data_keys_to_show = [];
      }
    });
  },
};
</script>

<style scoped>
</style>