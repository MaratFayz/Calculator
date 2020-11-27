package LD.service.excel.report;

import LD.model.Scenario.Scenario;
import LD.service.EntryIFRSAccService;
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
final public class ReportRegIfrsEntriesForCalculationWriter extends AbstractReportWriter {

    @Autowired
    EntryIFRSAccService entryIFRSAccService;

    ReportRegIfrsEntriesForCalculationWriter(XSSFWorkbook workbook, Scenario scenarioFrom, Scenario scenarioTo) {
        super(workbook, scenarioFrom, scenarioTo);
    }

    @Override
    protected String getSheetName() {
        return "Проводки";
    }

    @Override
    protected String getTitle() {
        return "Отчет о проводках на счетах МСФО";
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
                entryIFRSAccService.getAllEntriesIFRSAcc_for2Scenarios(this.scenarioTo.getId()));

        return reportRowsToWrite;
    }
}