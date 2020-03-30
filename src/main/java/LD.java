import lombok.*;

import javax.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Getter
@Setter
@Entity
@Table(name = "leasing_deposits")
@ToString(exclude = {"statuses", "transactions", "end_dates"})
@NoArgsConstructor()
public class LD
{
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false, unique = true)
	private int id;

	@OneToMany(mappedBy = "ld", fetch = FetchType.EAGER)
	@Column(name = "ld_status_id", nullable = false)
	private Set<LD_STATUS> statuses;

	@ManyToOne
	@JoinColumn(name = "entity_id", nullable = false)
	private ENTITY entity;

	@ManyToOne
	@JoinColumn(name = "counterpartner_id", nullable = false)
	private COUNTERPARTNER counterpartner;

	@ManyToOne
	@JoinColumn(name = "currency_id", nullable = false)
	private CURRENCY currency;

	@Column(name = "start_date", nullable = false, columnDefinition = "DATE")
	private ZonedDateTime start_date;

	@Column(name = "deposit_sum_not_disc", nullable = false)
	private BigDecimal deposit_sum_not_disc;

	@Transient
	private BigDecimal percent;

	@OneToMany(mappedBy = "ld", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	private Set<Transaction> transactions;

	@OneToMany(mappedBy = "ld", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	private Set<END_DATES> end_dates;

	@Transient
	private BigDecimal deposit_sum_discounted_on_firstEndDate;
	@Transient
	private ZonedDateTime firstEndDate;
	@Transient
	private int LDdurationDays;
	@Transient
	private int LDdurationMonths;

	public void calculate(List<DEPOSIT_RATES> LDRateL)
	{
		if(LDRateL.size() == 1)
		{
			this.percent = LDRateL.get(0).getRATE();
			BigDecimal percentPerDay = BigDecimal.valueOf(StrictMath.pow(this.percent.divide(BigDecimal.valueOf(100)).add(BigDecimal.ONE).doubleValue(), (double) 1 / (double) 365)).setScale(32, RoundingMode.UP).subtract(BigDecimal.ONE);
			deposit_sum_discounted_on_firstEndDate = this.deposit_sum_not_disc.setScale(32).divide(BigDecimal.ONE.add(percentPerDay).pow(LDdurationDays), RoundingMode.UP);
		}
		else
		{
			System.out.println("Обнаружено более 1 ставки. Ошибка. Расчет не будет произведен");
		}

		System.out.println("d = ");
	}

	public void countFirstEndDataAndDuration(String SCENARIO_LOAD)
	{
		//Найдем первую дату истечения депозита
		ZonedDateTime endDateOfMonth = this.start_date.withDayOfMonth(this.start_date.toLocalDate().lengthOfMonth());

		System.out.println(endDateOfMonth);
		end_dates.stream().forEach(e -> System.out.println(e));

		List<ZonedDateTime> ListEndDate = end_dates.stream().filter(element -> element.getScenario().getName().equals(SCENARIO_LOAD))
				.filter(element -> element.getPeriod().getDate().equals(endDateOfMonth))
				.map(end_date -> end_date.getEnd_Date())
				.collect(Collectors.toList());

		if(ListEndDate.size() == 1) this.firstEndDate = ListEndDate.get(0);
		else this.firstEndDate = this.start_date;

		LDdurationDays = (int) Duration.between(this.start_date, this.firstEndDate).toDays();
		LDdurationMonths = (int) Math.round(LDdurationDays / ((double) 365/ (double) 12));
	}

}