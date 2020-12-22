<template>
  <div>
    <ShowDataForTwoScenarios
      :url="urlWithLeasingDepositsFor2Scenarios"
      :stringForNoValues="'Депозиты, по которым будут рассчитаны проводки, отсутствуют'"
    />
  </div>
</template>

<script>
import { urlWithLeasingDepositsFor2Scenarios } from "../../../../generalData.js";
import ShowDataForTwoScenarios from "./Common/ShowDataForTwoScenarios";

export default {
  name: "tabLeasingdeposits",

  components: {
    ShowDataForTwoScenarios
  },

  data: function () {
    return {
      urlWithLeasingDepositsFor2Scenarios: urlWithLeasingDepositsFor2Scenarios,
    };
  },
};
</script>

<style scoped>
</style>