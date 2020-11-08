package LD.service.excel.report;

import LD.model.Scenario.Scenario;
import LD.repository.LeasingDepositRepository;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Scope(scopeName = "prototype")
final public class ReportDepositsForCalculationWriter extends AbstractReportWriter {

    @Autowired
    private LeasingDepositRepository leasingDepositRepository;

    ReportDepositsForCalculationWriter(XSSFWorkbook workbook, Scenario scenarioFrom, Scenario scenarioTo) {
        super(workbook, scenarioFrom, scenarioTo);
    }

    @Override
    protected String getSheetName() {
        return "ReportDepositsForCalculation";
    }

    @Override
    protected String getTitle() {
        return "Отчет о рассчитываемых депозитах";
    }

    @Override
    protected Map<String, String> getMappingForColumnNames() {
        Map<String, String> mappingForColumnNames = new HashMap<>();

        return mappingForColumnNames;
    }

    @Override
    protected List<Object> getDataForWriting() {
        List<Object> reportRowsToWrite = new ArrayList<>();

        reportRowsToWrite.addAll(
                leasingDepositRepository
                        .getActualDepositsWithEndDatesForScenarios(this.scenarioFrom.getId(), this.scenarioTo.getId()));

        return reportRowsToWrite;
    }
}