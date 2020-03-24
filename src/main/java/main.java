import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class main
{

	public static void main(String[] args)
	{
		String url = "jdbc:mysql://localhost:3306/ld?serverTimezone=UTC";
		String user = "root";
		String password = "ZZZXXX5!#~a";

		try
		{
			Connection connection = DriverManager.getConnection(url, user, password);

			Statement statement = connection.createStatement();
			ResultSet rs = statement.executeQuery("SELECT * FROM currency"); //надо получить все ID депозитов, которые имеют меньше, чем должно быть проводок

			ResultSet LeasingDeposits = statement.executeQuery("SELECT * FROM leasing_deposits");


			while(rs.next())
			{
				//запрос по каждому ID детальных проводок
				System.out.println(rs.getString("idCURRENCY"));
				System.out.println(rs.getString("CURRENCYcol"));
				System.out.println("");
			}

			rs.close();
			statement.close();
			connection.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
