<template>
  <div>
    <v-toolbar-title>Панель администратора </v-toolbar-title>
    <v-row>
      <v-tabs background-color="primary" dark v-model="tab">
        <v-tab v-for="tab in adminTabs" :key="tab.id">
          {{ tab.name }}
        </v-tab>

        <v-tabs-items v-model="tab">
          <v-tab-item :key="'users'">
            <tabUsers />
          </v-tab-item>

          <v-tab-item :key="'roles'">
            <tabRoles />
          </v-tab-item>

          <v-tab-item :key="'exImport'">
            <tabExImport />
          </v-tab-item>

          <v-tab-item :key="'autoCreatePeriods'">
            <tabAutoCreatePeriods />
          </v-tab-item>

          <v-tab-item :key="'autoClosingPeriods'">
            <tabAutoclosingPeriods />
          </v-tab-item>
        </v-tabs-items>
      </v-tabs>
    </v-row>
  </div>
</template>

<script>
import { adminTabs } from "../../generalData";
import tabUsers from "./AdminPageComponents/tabUsers";
import tabRoles from "./AdminPageComponents/tabRoles";
import tabExImport from "./AdminPageComponents/tabExImport";
import tabAutoCreatePeriods from "./AdminPageComponents/tabAutoCreatePeriods";
import tabAutoclosingPeriods from "./AdminPageComponents/tabAutoclosingPeriods";

export default {
  name: "AdminPage",

  components: {
    tabUsers,
    tabRoles,
    tabExImport,
    tabAutoCreatePeriods,
    tabAutoclosingPeriods,
  },

  data: function () {
    return {
      adminTabs: adminTabs,
      tab: "",
    };
  },
};
</script>

<style scoped>
</style>