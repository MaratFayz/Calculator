import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class LDArrayBuilder
{
	ResultSet LeasingDeposits;
	ResultSet LDWIthEndDatesAndPeriods;
	ResultSet LDtransactions;

	ArrayList<LD> deposits;

	LDArrayBuilder(ResultSet LeasingDeposits,
				   ResultSet LDWIthEndDatesAndPeriods,
				   ResultSet LDtransactions)
	{
		this.LeasingDeposits = LeasingDeposits;
		this.LDWIthEndDatesAndPeriods = LDWIthEndDatesAndPeriods;
		this.LDtransactions = LDtransactions;
		this.deposits = new ArrayList<>();
	}

	public ArrayList<LD> getLDArray(ResultSet commonParameter_currencyExchange, ResultSet commonParameter_depositRate, LocalDate endDate)
	{
		try
		{
			LDBuilder curLD;
			String ldID;

			while (this.LeasingDeposits.next())
			{
				curLD = new LDBuilder();
				ldID = this.LeasingDeposits.getString("ID");

				curLD.saveID(ldID);
				curLD.saveEN(this.LeasingDeposits.getString("ID_ENTITY"));
				curLD.saveCP(this.LeasingDeposits.getInt("ID_COUNTERPARTY"));
				curLD.saveCU(this.LeasingDeposits.getInt("ID_CURRENCY"));
				curLD.saveSD(this.LeasingDeposits.getDate("START_DATE"));
				curLD.saveDE(this.LeasingDeposits.getDouble("DEPOSIT_SUM_NOT_DISC"));
				curLD.saveTD(this.LeasingDeposits.getString("TO_DELETED"));
				curLD.saveSC(this.LeasingDeposits.getString("ID_SCENARIO"));

				HashMap<String, Date> hmEndDatesLD = new HashMap<>();
				while (this.LDWIthEndDatesAndPeriods.next())
				{
					if (this.LDWIthEndDatesAndPeriods.getString("ID_LD").equals(ldID))
						hmEndDatesLD.put(this.LDWIthEndDatesAndPeriods.getString("ID_PERIOD"), this.LDWIthEndDatesAndPeriods.getDate("END_DATE"));
				}
				curLD.addEndDate(hmEndDatesLD);
				this.LDWIthEndDatesAndPeriods.absolute(0);

				HashMap<String, Date> hmTRLD = new HashMap<>();
				while (this.LDtransactions.next())
				{
					if (this.LDtransactions.getString("ID_LD").equals(ldID))
						hmEndDatesLD.put(this.LDWIthEndDatesAndPeriods.getString("ID_PERIOD"), this.LDWIthEndDatesAndPeriods.getDate("END_DATE"));
				}
				curLD.addTransactions(hmTRLD);
				this.LDtransactions.absolute(0);

				this.deposits.add(curLD.build());
			}

			this.LeasingDeposits.close();
			this.LDWIthEndDatesAndPeriods.close();
			this.LDtransactions.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			this.deposits = null;
		}

		return this.deposits;
	}

}
