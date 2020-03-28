import lombok.*;

import javax.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@Setter
@Entity
@Table(name = "leasing_deposits")
@EqualsAndHashCode(exclude={"statuses", "transactions", "end_dates"})
@ToString(exclude = {"statuses", "transactions", "end_dates"})
@Builder(toBuilder = true)
@NoArgsConstructor(staticName = "createNewLD")
public class LD
{
/*
	public static LD createNewLD()
	{
		return new LD();
	}*/

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false, unique = true)
	private int id;

	@OneToMany(mappedBy = "ld")
	@Column(name = "ld_status_id", nullable = false)
	private Set<LD_STATUS> statuses;

	@ManyToOne
	@JoinColumn(name = "entity_id")
	private ENTITY entity;

	@ManyToOne
	@JoinColumn(name = "counterpartner_id")
	private COUNTERPARTNER counterpartner;

	@ManyToOne
	@JoinColumn(name = "currency_id")
	private CURRENCY currency;

	private Date start_date;
	private BigDecimal deposit_sum_not_disc;

	@Transient
	private BigDecimal percent;

	@OneToMany(mappedBy = "ld", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	private Set<Transaction> transactions;

	@OneToMany(mappedBy = "ld", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	private Set<END_DATES> end_dates;

	@Transient
	private BigDecimal deposit_sum_discounted_on_firstEndDate;

	public void calculate(String SCENARIO_LOAD)
	{
		Date firstEndDate = getFirstEndData(SCENARIO_LOAD);
		int LDduration = (int) Duration.between(start_date.toInstant(), firstEndDate.toInstant()).toDays();

		BigDecimal percentPerDay = BigDecimal.valueOf(StrictMath.pow(this.percent.divide(BigDecimal.valueOf(100)).add(BigDecimal.ONE).doubleValue(), (double) 1 / (double) 365)).setScale(32, RoundingMode.UP).subtract(BigDecimal.ONE);
		BigDecimal discSum = this.deposit_sum_not_disc.setScale(32).divide(BigDecimal.ONE.add(percentPerDay).pow(LDduration), RoundingMode.UP);


		System.out.println("d = ");
	}

	private Date getFirstEndData(String SCENARIO_LOAD)
	{
		Date returnfirstEndDate = Calendar.getInstance().getTime();

/*		//Найдем первую дату истечения депозита
		LocalDate sdLD = LocalDate.ofInstant(this.start_date.toInstant(), ZoneId.of("UTC"));
		LocalDate endDateOfMonth = sdLD.withDayOfMonth(sdLD.lengthOfMonth());
		Calendar c = Calendar.getInstance();
		c.set(endDateOfMonth.getYear(), endDateOfMonth.getMonthValue()-1, endDateOfMonth.getDayOfMonth(), 0, 0, 0);
		c.clear(Calendar.MILLISECOND);

		System.out.println(c.getTime().getTime());
		end_dates.stream().forEach(e -> System.out.println(e.getPeriod().getDate().getTime()));

		List<Date> ListEndDate = end_dates.stream().filter(element -> element.getScenario().getName().equals(SCENARIO_LOAD))
				.filter(element -> element.getPeriod().getDate().equals(c.getTime()))
				.map(end_date -> end_date.getEnd_Date())
				.collect(Collectors.toList());

		if(ListEndDate.size() == 1) returnfirstEndDate = ListEndDate.get(0);
		else returnfirstEndDate = this.start_date;*/

		return returnfirstEndDate;
	}

}


/*	public BigDecimal countDiscountedValue()
	{
	*//*		BigDecimal percent = BigDecimal.valueOf(15.0);
		percent = percent.divide(BigDecimal.valueOf(100.0)).add(BigDecimal.ONE);

		int periodsYears = 10;

		BigDecimal coefDY = BigDecimal.ONE.setScale(32).divide(percent, RoundingMode.UP);

		Locale x = Locale.getDefault();
		String[] y = Locale.getISOCountries();
		Locale[] z = Locale.getAvailableLocales();
		Locale t = Locale.forLanguageTag("ru").;
		DecimalFormat df = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.forLanguageTag("ru"));
		//df.setRoundingMode(RoundingMode.UP);
		//df.applyPattern("# ##0.0000000000#");

		for (int i=1; i <= periodsYears; i++) System.out.println(df.format(coefDY.pow(i)));


		LocalDate ld = LocalDate.now();
		LocalDate end = ld.withDayOfMonth(ld.lengthOfMonth()).withDayOfYear(300);


		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy");
		System.out.println(ld.format(dtf));
		System.out.println(end);
		System.out.println(end.getDayOfMonth() - ld.getDayOfMonth());

		System.out.println(Duration.between(ld.atStartOfDay(), end.atStartOfDay()).toDays());*//*
	}

	*//*		BigDecimal percent = BigDecimal.valueOf(15.0);
		percent = percent.divide(BigDecimal.valueOf(100.0)).add(BigDecimal.ONE);

		int periodsYears = 10;

		BigDecimal coefDY = BigDecimal.ONE.setScale(32).divide(percent, RoundingMode.UP);

		Locale x = Locale.getDefault();
		String[] y = Locale.getISOCountries();
		Locale[] z = Locale.getAvailableLocales();
		Locale t = Locale.forLanguageTag("ru").;
		DecimalFormat df = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.forLanguageTag("ru"));
		//df.setRoundingMode(RoundingMode.UP);
		//df.applyPattern("# ##0.0000000000#");

		for (int i=1; i <= periodsYears; i++) System.out.println(df.format(coefDY.pow(i)));


		LocalDate ld = LocalDate.now();
		LocalDate end = ld.withDayOfMonth(ld.lengthOfMonth()).withDayOfYear(300);


		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy");
		System.out.println(ld.format(dtf));
		System.out.println(end);
		System.out.println(end.getDayOfMonth() - ld.getDayOfMonth());

		System.out.println(Duration.between(ld.atStartOfDay(), end.atStartOfDay()).toDays());*/
