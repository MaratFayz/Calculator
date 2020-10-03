package LD.service.excel.report;

import LD.model.Scenario.Scenario;
import LD.repository.LeasingDepositRepository;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ReportDepositsForCalculationWriter extends AbstractReportWriter {

    @Autowired
    private LeasingDepositRepository leasingDepositRepository;

    public static void write(XSSFWorkbook workbook, Scenario scenarioFrom, Scenario scenarioTo) throws IllegalAccessException {
        XSSFSheet sheet = workbook.createSheet("ReportDepositsForCalculation");

        ReportDepositsForCalculationWriter writer = new ReportDepositsForCalculationWriter(sheet, scenarioFrom, scenarioTo);
        writer.writeReport();
    }

    private ReportDepositsForCalculationWriter(XSSFSheet sheet, Scenario scenarioFrom, Scenario scenarioTo) {
        super(sheet, scenarioFrom, scenarioTo);
    }

    @Override
    protected String getTitle() {
        return "Отчет о рассчитываемых депозитах";
    }

    @Override
    protected Map<String, String> createMappingForColumnNames() {
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