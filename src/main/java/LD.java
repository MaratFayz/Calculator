import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.LinkedList;

public class LD
{
	public String id;
	public String id_scenario;
	public String id_entity;
	public int id_counterparty;
	public int id_currency;
	public Date start_date;
	public double deposit_sum_not_disc;
	public String to_deleted;
	public HashMap<String, Date> hmLD;

	public void calculate()
	{
	}

	public double getNominalValueAtStartDay()
	{
		return this.deposit_sum_not_disc;
	}

	public double getDiscountedValueAtStartDay()
	{
		return 0.00;
	}

	/*		BigDecimal percent = BigDecimal.valueOf(15.0);
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



}

