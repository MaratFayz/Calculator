package LD.service.ExchangeRate;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.FileInputStream;

@Component
@Scope("prototype")
@Log4j2
public class GetPropertiesForLdProjectFromFile {

    private String fileDirectory;

    @Getter
    private PropertiesForLdProject propertiesForLdProject;

    public GetPropertiesForLdProjectFromFile(String fileDirectory) {
        this.fileDirectory = fileDirectory;
    }

    @SneakyThrows
    @PostConstruct
    private void loadDataFromFile() {
        XmlMapper xmlMapper = new XmlMapper();
        propertiesForLdProject =
                xmlMapper.readValue(new FileInputStream(fileDirectory), PropertiesForLdProject.class);
    }
}