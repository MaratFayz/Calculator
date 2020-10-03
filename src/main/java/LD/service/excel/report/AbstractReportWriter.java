package LD.service.excel.report;

import LD.model.Scenario.Scenario;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;

import java.lang.reflect.Field;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static java.util.Objects.isNull;

public abstract class AbstractReportWriter {

    private final String titleRowName = "Наименование отчета:";
    private final String dateTimeRowName = "Дата и время создания отчета:";
    private final String scenarioFromRowName = "Сценарий с:";
    private final String scenarioToRowName = "Сценарий на:";

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

    protected AbstractReportWriter(XSSFSheet sheet, Scenario scenarioFrom, Scenario scenarioTo) {
        this.sheet = sheet;
        this.scenarioFrom = scenarioFrom;
        this.scenarioTo = scenarioTo;
        this.reportRowsToWrite = getDataForWriting();
        this.mappingForColumnNames = createMappingForColumnNames();
    }

    protected abstract Map<String, String> createMappingForColumnNames();

    protected abstract List<Object> getDataForWriting();

    final public void writeReport() throws IllegalAccessException {
        createStyleForTableFirstRow();
        writeHeader();
        createEmptyRow();
        saveRowIndexOfTableFirstRow();
        writeBody();
        setAutoSizeInColumn(0);
        setAutoFilterForTable();
    }

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

    private void

    createEmptyRow() {
        incrementWritingRowIndex();
    }

    private void saveRowIndexOfTableFirstRow() {
        this.firstTableRow = writingRowIndex;
    }

    private void writeBody() throws IllegalAccessException {
        getColumnsOfFutureTable();
        writeColumnsOfTable();
        writeDataIntoTable();
    }

    private void getColumnsOfFutureTable() {
        Object object = reportRowsToWrite.get(0);

        Class<?> dataClass = object.getClass();
        fields = new LinkedList<>(Arrays.asList(dataClass.getDeclaredFields()));
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

    private void writeDataIntoTable() throws IllegalAccessException {
        for (Object reportRow : reportRowsToWrite) {
            createNextRow();
            int columnIndex = startCellIndexToWrite;

            for (Field field : fields) {
                createNewCell(columnIndex);

                field.setAccessible(true);
                String fieldValue = field.get(reportRow).toString();
                cell.setCellValue(fieldValue);

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