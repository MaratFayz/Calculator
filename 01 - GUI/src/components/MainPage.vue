<template>
  <v-container fluid>
    <CustomAppBar />

    <NavigationBar />

    <router-view />
  </v-container>
</template>

<script>
import NavigationBar from "./MainPageComponents/NavigationBar";
import CustomAppBar from "./MainPageComponents/CustomAppBar";
import getAllScenariosFromServer from "../functions/getAllScenariosFromServer.js";

export default {
  name: "MainPage",

  components: {
    NavigationBar,
    CustomAppBar,
  },
  created: function () {
    getAllScenariosFromServer();
  },
};
</script>

<style>
</style>