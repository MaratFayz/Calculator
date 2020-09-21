package LD.model.EntryIFRSAcc;

import LD.model.AbstractModelClass;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "EntryIFRSAcc")
@NoArgsConstructor
@Data
@Builder
@AllArgsConstructor
public class EntryIFRSAcc extends AbstractModelClass {

    @EmbeddedId
    private EntryIFRSAccID entryIFRSAccID;

    @Column(columnDefinition = "DECIMAL(30,10)", scale = 10, precision = 30, nullable = false)
    private BigDecimal sum;
}
