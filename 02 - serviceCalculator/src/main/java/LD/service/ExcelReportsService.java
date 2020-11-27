package LD.service;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;

public interface ExcelReportsService {

    XSSFWorkbook getExcelReportForDepositsAndEntries(Long scenarioFromId,
                                                     Long scenarioToId) throws IOException, IllegalAccessException;
}