import Vue from 'vue';
import Vuex from 'vuex';
import getFirstOpenDateInScenario from '../functions/getFirstOpenDateInScenario'

Vue.use(Vuex);

export const store = new Vuex.Store({
    state: {
        isShowNavigationPanel: false,
        chosenPageOnMainPage: "",
        allScenarios: [],
        allScenariosNames: [],
        firstOpenPeriod_from: "",
        firstOpenPeriod_to: "",
        selectedScenarioFrom: {},
        selectedScenarioTo: {}
    },
    mutations: {
        invertShowingNavigationPanel(state) {
            state.isShowNavigationPanel = !state.isShowNavigationPanel;
        },
        negateShowingNavigationPanel(state) {
            state.isShowNavigationPanel = false;
        },
        choosePageOnMainPage(state, payload) {
            state.chosenPageOnMainPage = payload;
        },
        saveScenarios(state, scenarios) {
            state.allScenarios = scenarios;
        },
        saveScenariosNames(state, scenarios) {
            state.allScenariosNames = scenarios;
        },
        savefirstOpenPeriod_from(state, firstOpenPeriod) {
            state.firstOpenPeriod_from = firstOpenPeriod;
        },
        savefirstOpenPeriod_to(state, firstOpenPeriod) {
            state.firstOpenPeriod_to = firstOpenPeriod;
        },
        saveScenarioFrom(state, scenarioFromName) {
            console.log(state.allScenarios)

            let scenario;
            for (let index = 0; index < state.allScenarios.length; ++index) {

                if (scenarioFromName === state.allScenarios[index].name) {
                    scenario = state.allScenarios[index];
                }
            }

            state.selectedScenarioFrom = scenario;
        },
        saveScenarioTo(state, scenarioToName) {
            console.log(state.allScenarios)

            let scenario;
            for (let index = 0; index < state.allScenarios.length; ++index) {

                if (scenarioToName === state.allScenarios[index].name) {
                    scenario = state.allScenarios[index];
                }
            }

            state.selectedScenarioTo = scenario;
        }
    },
    getters: {
        showNavigationPanel: state => {
            return state.isShowNavigationPanel;
        },
        getPageOnMainPage: state => {
            return state.chosenPageOnMainPage;
        },
        getScenarioNames: state => {
            return state.allScenariosNames;
        },
        getScenarios: state => {
            return state.allScenarios;
        },
        getFirstOpenPeriod_from: state => {
            return state.firstOpenPeriod_from;
        },
        getFirstOpenPeriod_to: state => {
            return state.firstOpenPeriod_to;
        },
        getScenarioFrom: state => {
            return state.selectedScenarioFrom;
        },
        getScenarioTo: state => {
            return state.selectedScenarioTo;
        }
    },
    actions: {
        setInvertedShowNavigationPanel: state => {
            state.commit('invertShowingNavigationPanel');
        },
        setChosenPageOnMainPage(state, payload) {
            state.commit('choosePageOnMainPage', payload);
        },
        setFalseShowNavigationPanel: state => {
            state.commit('negateShowingNavigationPanel');
        },
        saveAllScenarios: ({ commit }, inputData) => {
            commit('saveScenarios', inputData.allScenarios);
            commit('saveScenariosNames', inputData.allScenariosNames);
        },
        getFirstOpenDateInScenario_From({ commit }, containerWithScenario) {
            try {
                let firstOpenPeriodPromise = getFirstOpenDateInScenario(containerWithScenario.scenarioName);

                firstOpenPeriodPromise.then((fop) => {
                    commit('savefirstOpenPeriod_from', fop);
                });
            } catch (err) {
                console.log(err);
                commit('savefirstOpenPeriod_from', "");
            }
        },
        getFirstOpenDateInScenario_To({ commit }, containerWithScenario) {
            try {
                let firstOpenPeriodPromise = getFirstOpenDateInScenario(containerWithScenario.scenarioName);

                firstOpenPeriodPromise.then((fop) => {
                    commit('savefirstOpenPeriod_to', fop);
                });
            } catch (err) {
                console.log(err);
                commit('savefirstOpenPeriod_to', "");
            }
        },
        setScenarioFrom({ commit }, containerWithScenarioName) {
            commit('saveScenarioFrom', containerWithScenarioName.scenarioName);
        },
        setScenarioTo({ commit }, containerWithScenarioName) {
            console.log("setScenarioTo => " + containerWithScenarioName.scenarioName);

            commit('saveScenarioTo', containerWithScenarioName.scenarioName);
        }
    }
})