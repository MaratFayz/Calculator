package LD.service.excel.report;

import LD.model.Scenario.Scenario;
import LD.service.EntryService;
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
final public class ReportRegLd3ForCalculationWriter extends AbstractReportWriter {

    @Autowired
    EntryService entryService;

    ReportRegLd3ForCalculationWriter(XSSFWorkbook workbook, Scenario scenarioFrom, Scenario scenarioTo) {
        super(workbook, scenarioFrom, scenarioTo);
    }

    @Override
    protected String getSheetName() {
        return "Reg.LD.3";
    }

    @Override
    protected String getTitle() {
        return "Отчет о расчетах формы Reg.LD.3";
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
                entryService.getAllLDEntries_RegLD3(this.scenarioTo.getId()));

        return reportRowsToWrite;
    }
}