package LD.service;

import LD.model.Scenario.Scenario;
import LD.repository.ScenarioRepository;
import LD.service.excel.report.ReportDepositsForCalculationWriter;
import LD.service.excel.report.ReportRegLd1ForCalculationWriter;
import LD.service.excel.report.ReportRegLd2ForCalculationWriter;
import LD.service.excel.report.ReportRegLd3ForCalculationWriter;
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

        ReportRegLd1ForCalculationWriter reportRegLd1ForCalculationWriter =
                getReportRegLd1ForCalculationWriter(workbook, scenarioFrom, scenarioTo);
        reportRegLd1ForCalculationWriter.writeReport();

        ReportRegLd2ForCalculationWriter reportRegLd2ForCalculationWriter =
                getReportRegLd2ForCalculationWriter(workbook, scenarioFrom, scenarioTo);
        reportRegLd2ForCalculationWriter.writeReport();

        ReportRegLd3ForCalculationWriter reportRegLd3ForCalculationWriter =
                getReportRegLd3ForCalculationWriter(workbook, scenarioFrom, scenarioTo);
        reportRegLd3ForCalculationWriter.writeReport();

        return workbook;
    }

    @Lookup
    ReportDepositsForCalculationWriter getReportDepositsForCalculationWriter(XSSFWorkbook workbook, Scenario scenarioFrom, Scenario scenarioTo) {
        return null;
    }

    @Lookup
    ReportRegLd1ForCalculationWriter getReportRegLd1ForCalculationWriter(XSSFWorkbook workbook, Scenario scenarioFrom, Scenario scenarioTo) {
        return null;
    }

    @Lookup
    ReportRegLd2ForCalculationWriter getReportRegLd2ForCalculationWriter(XSSFWorkbook workbook, Scenario scenarioFrom, Scenario scenarioTo) {
        return null;
    }

    @Lookup
    ReportRegLd3ForCalculationWriter getReportRegLd3ForCalculationWriter(XSSFWorkbook workbook, Scenario scenarioFrom, Scenario scenarioTo) {
        return null;
    }
}