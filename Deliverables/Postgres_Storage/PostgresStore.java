import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class PostgresStore {
	private static final String connectionURL = "jdbc:postgresql://localhost:5432/kms";
	private static final String psqlUser = "postgres";
	private static final String tableName = "encrypteddata";

	public static void main(String[] args) {
		System.out.println("Java JDBC PostgreSQL Example");

		try (Connection connection = DriverManager.getConnection(connectionURL, psqlUser, "")) {
			// When this class first attempts to establish a connection, it automatically loads any JDBC 4.0 drivers found within
			// the class path. Note that your application must manually load any JDBC drivers prior to version 4.0.
			// Class.forName("org.postgresql.Driver");
			System.out.println("Connected to PostgreSQL database!");
			Statement statement = connection.createStatement();
			System.out.println("Created statement object.");

			// To update the table, you will need to build a PreparedStatement. The below shows how: put the SQL command (without
			// an ending semicolon) in connection.prepareStatement() method, then run executeUpdate on that PreparedStatement.
			PreparedStatement ps = connection.prepareStatement("INSERT INTO " + tableName + " (payload) VALUES ('1234567890000000')");
			ps.executeUpdate();
			System.out.println(">>> Updated table by inserting 1 row.");

			System.out.println("Reading stored information...");
			System.out.printf("%-30.30s  %-30.30s%n", "id", "payload"); // these are the table's column names
			// To query the table, you will need to build a ResultSet. This is a data structure that holds the result of the SELECT
			// statement below. resultSet functions very much like a C iterator: you must run "ResultSet.next()" to get the next item,
			// and by default resultSet does not point to the data immediately after statement.executeQuery (i.e. you **must** run
			// ResultSet.next() at least once in order to access the data, which in this case starts at the first row returned by the
			// given SELECT statement).
			ResultSet resultSet = statement.executeQuery("SELECT * FROM " + tableName);
			while (resultSet.next()) {
				System.out.printf("%-30.30s  %-30.30s%n", resultSet.getString("id"), resultSet.getString("payload"));
			}
			System.out.println("\033[1;32mDone.\033[0m"); // green bolded text
		/*} catch (ClassNotFoundException e) {
			System.out.println("PostgreSQL JDBC driver not found.");
			e.printStackTrace();/**/
		} catch (SQLException e) {
			System.out.println("Connection failure." + e);
			System.out.println("\033[1;31mError.\033[0m"); // red bolded text
		}

		System.out.println("Done. Exiting program...");
	}
}
