<template>
  <v-card>
    <v-app-bar app color="primary" dark>
      <v-app-bar-nav-icon @click="openNavigationBar"></v-app-bar-nav-icon>
      <v-toolbar-title>Калькулятор</v-toolbar-title>
      <v-spacer></v-spacer>
      <v-avatar color="indigo">
        <v-icon dark> mdi-account-circle </v-icon>
      </v-avatar>
    </v-app-bar>
  </v-card>
</template>

<script>
export default {
  name: "CustomAppBar",
  methods: {
    openNavigationBar: function () {
      this.$store.dispatch("setInvertedShowNavigationPanel");
    },
  },
};
</script>

<style scoped>
</style>