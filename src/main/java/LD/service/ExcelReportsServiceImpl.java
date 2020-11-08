package LD.service;

import LD.model.Scenario.Scenario;
import LD.repository.ScenarioRepository;
import LD.service.excel.report.ReportDepositsForCalculationWriter;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.stereotype.Service;

@Service
public class ExcelReportsServiceImpl implements ExcelReportsService {

    @Autowired
    private ScenarioRepository scenarioRepository;

    @Override
    public XSSFWorkbook getExcelReportForDepositsAndEntries(Long scenarioFromId,
                                                            Long scenarioToId) {

        Scenario scenarioFrom = scenarioRepository.findById(scenarioFromId).get();
        Scenario scenarioTo = scenarioRepository.findById(scenarioToId).get();

        XSSFWorkbook workbook = new XSSFWorkbook();
        ReportDepositsForCalculationWriter reportDepositsForCalculationWriter =
                getReportDepositsForCalculationWriter(workbook, scenarioFrom, scenarioTo);
        reportDepositsForCalculationWriter.writeReport();

        return workbook;
    }

    @Lookup
    ReportDepositsForCalculationWriter getReportDepositsForCalculationWriter(XSSFWorkbook workbook, Scenario scenarioFrom, Scenario scenarioTo) {
        return null;
    }
}