package LD.service.excel.report;

import LD.model.Scenario.Scenario;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.lang.reflect.Field;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Log4j2
public abstract class AbstractReportWriter {

    private final String titleRowName = "Наименование отчета:";
    private final String dateTimeRowName = "Дата и время создания отчета:";
    private final String scenarioFromRowName = "Сценарий с:";
    private final String scenarioToRowName = "Сценарий на:";

    private XSSFWorkbook workbook;
    private XSSFSheet sheet;
    private XSSFRow row;
    private XSSFCell cell;
    private int writingRowIndex;
    private int firstTableRow;
    protected Scenario scenarioFrom;
    protected Scenario scenarioTo;
    private List<Object> reportRowsToWrite;
    private List<Field> fields;
    private Map<String, String> mappingForColumnNames;
    private final int startCellIndexToWrite = 0;
    private CellStyle headerStyle;

    protected AbstractReportWriter(XSSFWorkbook workbook, Scenario scenarioFrom, Scenario scenarioTo) {
        this.workbook = workbook;

        String sheetName = getSheetName();
        this.sheet = workbook.createSheet(sheetName);

        this.scenarioFrom = scenarioFrom;
        this.scenarioTo = scenarioTo;
        this.mappingForColumnNames = getMappingForColumnNames();
    }

    protected abstract String getSheetName();

    protected abstract Map<String, String> getMappingForColumnNames();

    final public void writeReport() {
        this.reportRowsToWrite = getDataForWriting();

        log.info("reportRowsToWrite => {}", reportRowsToWrite);

        createStyleForTableFirstRow();
        writeHeader();
        createEmptyRow();
        saveRowIndexOfTableFirstRow();
        writeBody();
        setAutoSizeInColumn(0);
        setAutoFilterForTable();
    }

    protected abstract List<Object> getDataForWriting();

    private void createStyleForTableFirstRow() {
        headerStyle = sheet.getWorkbook().createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setWrapText(true);
        headerStyle.setBorderBottom(BorderStyle.MEDIUM);
        headerStyle.setBorderTop(BorderStyle.MEDIUM);
        headerStyle.setBorderLeft(BorderStyle.MEDIUM);
        headerStyle.setBorderRight(BorderStyle.MEDIUM);
    }

    private void writeHeader() {
        writeTitle();
        writeCreationDateTime();
        writeScenarioFrom();
        writeScenarioTo();
    }

    private void writeTitle() {
        writeTitleRowName();
        writeTitleValue();

        incrementWritingRowIndex();
    }

    private void writeTitleRowName() {
        createNextRow();
        createNewCell(0);
        cell.setCellValue(titleRowName);
    }

    private void createNextRow() {
        row = sheet.createRow(writingRowIndex);
    }

    private void createNewCell(int columnIndex) {
        cell = row.createCell(columnIndex);
    }

    private void writeTitleValue() {
        createNewCell(1);
        String title = getTitle();
        cell.setCellValue(title);
    }

    protected abstract String getTitle();

    private void incrementWritingRowIndex() {
        writingRowIndex++;
    }

    private void writeCreationDateTime() {
        createNextRow();
        writeDateTimeRowName();
        writeDateTimeRowValue();

        incrementWritingRowIndex();
    }

    private void writeDateTimeRowName() {
        createNewCell(0);
        cell.setCellValue(dateTimeRowName);
    }

    private void writeDateTimeRowValue() {
        createNewCell(1);
        String dateTime = ZonedDateTime.now().toString();
        cell.setCellValue(dateTime);
    }

    private void writeScenarioFrom() {
        createNextRow();

        writeScenarioFromRowName();
        writeScenarioFromValue();

        incrementWritingRowIndex();
    }

    private void writeScenarioFromRowName() {
        createNewCell(0);
        cell.setCellValue(scenarioFromRowName);
    }

    private void writeScenarioFromValue() {
        createNewCell(1);
        cell.setCellValue(scenarioFrom.getName());
    }

    private void writeScenarioTo() {
        createNextRow();

        writeScenarioToRowName();
        writeScenarioNameToValue();

        incrementWritingRowIndex();
    }

    private void writeScenarioToRowName() {
        createNewCell(0);
        cell.setCellValue(scenarioToRowName);
    }

    private void writeScenarioNameToValue() {
        createNewCell(1);
        cell.setCellValue(scenarioTo.getName());
    }

    private void createEmptyRow() {
        incrementWritingRowIndex();
    }

    private void saveRowIndexOfTableFirstRow() {
        this.firstTableRow = writingRowIndex;
    }

    private void writeBody() {
        getColumnsOfFutureTable();
        writeColumnsOfTable();
        writeDataIntoTable();
    }

    private void getColumnsOfFutureTable() {
        Object object = reportRowsToWrite.get(0);

        Class<?> dataClass = object.getClass();
        fields = new LinkedList<>(Arrays.asList(dataClass.getDeclaredFields()));

        log.info("fields => {}", fields);
    }

    private void writeColumnsOfTable() {
        createNextRow();

        int columnIndex = startCellIndexToWrite;
        for (Field field : fields) {
            createNewCell(columnIndex);

            String fieldName = getColumnName(field);
            cell.setCellValue(fieldName);
            cell.setCellStyle(headerStyle);

            columnIndex++;
        }

        incrementWritingRowIndex();
    }

    private String getColumnName(Field field) {
        String mappedValueForColumnName = getMappedValueForColumnName(field);

        if (isNull(mappedValueForColumnName)) {
            return field.getName();
        } else {
            return mappedValueForColumnName;
        }
    }

    private String getMappedValueForColumnName(Field field) {
        return mappingForColumnNames.get(field.getName());
    }

    @SneakyThrows
    private void writeDataIntoTable() {
        for (Object reportRow : reportRowsToWrite) {
            log.info("reportRow => {}", reportRow);

            createNextRow();
            int columnIndex = startCellIndexToWrite;

            for (Field field : fields) {
                log.info("field => {}", field);

                createNewCell(columnIndex);

                field.setAccessible(true);

                Object value = field.get(reportRow);

                if (nonNull(value)) {
                    String fieldValue = value.toString();
                    cell.setCellValue(fieldValue);
                }

                columnIndex++;
            }

            incrementWritingRowIndex();
        }
    }

    private void setAutoSizeInColumn(int columnIndex) {
        sheet.autoSizeColumn(columnIndex);
    }

    private void setAutoFilterForTable() {
        sheet.setAutoFilter(new CellRangeAddress(firstTableRow, writingRowIndex, startCellIndexToWrite, fields.size() - 1));
    }
}