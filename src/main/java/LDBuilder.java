import java.sql.Date;
import java.util.HashMap;

public class LDBuilder
{
	LD ld;

	public LDBuilder()
	{
		this.ld = new LD();
	}

	public LD build()
	{
		return this.ld;
	}

	public void saveID(String id)
	{
		ld.id = id;
	}

	public void saveEN(String id_entity)
	{
		ld.id_entity = id_entity;
	}

	public void saveCP(int id_counterparty)
	{
		ld.id_counterparty = id_counterparty;
	}

	public void saveCU(int id_currency)
	{
		ld.id_currency = id_currency;
	}

	public void saveSD(Date start_date)
	{
		ld.start_date = start_date;
	}

	public void saveDE(double deposit_sum_not_disc)
	{
		ld.deposit_sum_not_disc = deposit_sum_not_disc;
	}

	public void saveTD(String to_deleted)
	{
		ld.to_deleted = to_deleted;
	}

	public void saveSC(String id_scenario)
	{
		ld.id_scenario = id_scenario;
	}

	public void addEndDate(HashMap<String, Date> hmLD)
	{
		ld.hmLD = hmLD;
	}
}
