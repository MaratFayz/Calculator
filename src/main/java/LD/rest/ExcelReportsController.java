package LD.rest;

import LD.service.ExcelReportsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;

@Api(value = "Контроллер для получения отчетов в формате Excel")
@RestController
@RequestMapping("/excelReports")
@Log4j2
public class ExcelReportsController {

    @Autowired
    private ExcelReportsService excelReportsService;

    @GetMapping("/ld_regld1_2_3")
    @ApiOperation(value = "Получение отчета в формате Excel из 4 вкладок: депозиты в расчете, расчеты")
    @PreAuthorize("hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).ENTRY_READER) and " +
            "hasAuthority(T(LD.config.Security.model.Authority.ALL_AUTHORITIES).LEASING_DEPOSIT_READER)")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Отчет возвращается в ответе."),
            @ApiResponse(code = 403, message = "Доступ запрещён")
    })
    public void getExcelReportForDepositsAndEntries(@RequestParam @NonNull Long scenarioFromId,
                                                    @RequestParam @NonNull Long scenarioToId,
                                                    HttpServletResponse response) throws IOException, IllegalAccessException {

        XSSFWorkbook report = excelReportsService.getExcelReportForDepositsAndEntries(scenarioFromId, scenarioToId);

        response.setHeader("Content-Disposition", "inline;filename=\"" + URLEncoder.encode("Report.xlsx", "UTF-8") + "\"");
        response.setContentType("application/xlsx");

        OutputStream outputStream = response.getOutputStream();

        report.write(outputStream);

        outputStream.flush();
        outputStream.close();
        report.close();
    }
}