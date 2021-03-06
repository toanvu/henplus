/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: SQLSession.java,v 1.33 2005-03-24 13:57:46 hzeller Exp $
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus;

import henplus.property.BooleanPropertyHolder;
import henplus.property.EnumeratedPropertyHolder;
import henplus.sqlmodel.Table;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.SortedSet;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;

/**
 * a SQL session.
 */
public class SQLSession implements Interruptable {
	private long _connectTime;
	private long _statementCount;
	private String _url;
	private String _username;
	private String _password;
	private String _databaseInfo;
	private Connection _conn;
	private SQLMetaData _metaData;

	private final PropertyRegistry _propertyRegistry;
	private volatile boolean _interrupted;
	private HenPlus _henplus;
//	private DataSourceFactory dsFactory;
	private BundleContext context;


	/**
	 * creates a new SQL session. Open the database connection, initializes the
	 * readline library
	 */
	public SQLSession(String url, String user, String password, HenPlus _henplus, BundleContext context)
			throws IllegalArgumentException, ClassNotFoundException,
			SQLException, IOException {
		this._henplus = _henplus;
//		this.dsFactory = dsFactory;
		this.context = context;
		_statementCount = 0;
		_conn = null;
		_url = url;
		_username = user;
		_password = password;
		_propertyRegistry = new PropertyRegistry();

		Driver driver = null;
		// HenPlus.msg().println("connect to '" + url + "'");
//		driver = dsFactory.getDriver();

		_henplus.msg().println("HenPlus II starting... ");
//		_henplus.msg().println(
//				" driver version " + driver.getMajorVersion() + "."
//						+ driver.getMinorVersion());
		connect();

		int currentIsolation = Connection.TRANSACTION_NONE;
		DatabaseMetaData meta = _conn.getMetaData();
		_databaseInfo = (meta.getDatabaseProductName() + " - " + meta
				.getDatabaseProductVersion());
		_henplus.msg().println(" " + _databaseInfo);
		try {
			if (meta.supportsTransactions()) {
				currentIsolation = _conn.getTransactionIsolation();
			} else {
				_henplus.msg().println("no transactions.");
			}
			_conn.setAutoCommit(false);
		} catch (SQLException ignore_me) {
		}

		printTransactionIsolation(meta, Connection.TRANSACTION_NONE,
				"No Transaction", currentIsolation);
		printTransactionIsolation(meta,
				Connection.TRANSACTION_READ_UNCOMMITTED, "read uncommitted",
				currentIsolation);
		printTransactionIsolation(meta, Connection.TRANSACTION_READ_COMMITTED,
				"read committed", currentIsolation);
		printTransactionIsolation(meta, Connection.TRANSACTION_REPEATABLE_READ,
				"repeatable read", currentIsolation);
		printTransactionIsolation(meta, Connection.TRANSACTION_SERIALIZABLE,
				"serializable", currentIsolation);

		Map availableIsolations = new HashMap();
		addAvailableIsolation(availableIsolations, meta,
				Connection.TRANSACTION_NONE, "none");
		addAvailableIsolation(availableIsolations, meta,
				Connection.TRANSACTION_READ_UNCOMMITTED, "read-uncommitted");
		addAvailableIsolation(availableIsolations, meta,
				Connection.TRANSACTION_READ_COMMITTED, "read-committed");
		addAvailableIsolation(availableIsolations, meta,
				Connection.TRANSACTION_REPEATABLE_READ, "repeatable-read");
		addAvailableIsolation(availableIsolations, meta,
				Connection.TRANSACTION_SERIALIZABLE, "serializable");

		_propertyRegistry.registerProperty("auto-commit",
				new AutoCommitProperty());
		_propertyRegistry.registerProperty("read-only", new ReadOnlyProperty());
		_propertyRegistry.registerProperty("isolation-level",
				new IsolationLevelProperty(availableIsolations,
						currentIsolation));
	}

	private void printTransactionIsolation(DatabaseMetaData meta, int iLevel,
			String descript, int current) throws SQLException {
		if (meta.supportsTransactionIsolationLevel(iLevel)) {
			_henplus.msg().println(
					" " + descript + ((current == iLevel) ? " *" : " "));
		}
	}

	private void addAvailableIsolation(Map result, DatabaseMetaData meta,
			int iLevel, String key) throws SQLException {
		if (meta.supportsTransactionIsolationLevel(iLevel)) {
			result.put(key, new Integer(iLevel));
		}
	}

	public PropertyRegistry getPropertyRegistry() {
		return _propertyRegistry;
	}

	public String getDatabaseInfo() {
		return _databaseInfo;
	}

	public String getURL() {
		return _url;
	}

	public SQLMetaData getMetaData(SortedSet/* <String> */tableNames) {
		if (_metaData == null) {
			_metaData = new SQLMetaDataBuilder().getMetaData(this, tableNames,_henplus);
		}
		return _metaData;
	}

	public Table getTable(String tableName) {
		return new SQLMetaDataBuilder().getTable(this, tableName,_henplus);
	}

	public boolean printMessages() {
		return !(_henplus.getDispatcher().isInBatch());
	}

	public void print(String msg) {
		if (printMessages())
			_henplus.msg().print(msg);
	}

	public void println(String msg) {
		if (printMessages())
			_henplus.msg().println(msg);
	}

	public void connect() throws SQLException, IOException {
		/*
		 * close old connection ..
		 */
		if (_conn != null) {
			try {
				_conn.close();
			} catch (Throwable t) { /* ignore */
			}
			_conn = null;
		}

		Properties props = new Properties();
		
		/*
		 * FIXME make generic plugin for specific database drivers that handle
		 * the specific stuff. For now this is a quick hack.
		 */
		if (_url.startsWith("jdbc:oracle:")) {
			/*
			 * this is needed to make comment in oracle show up in the remarks
			 * http://forums.oracle.com/forums/thread.jsp?forum=99&thread=225790
			 */
			props.setProperty("remarksReporting", "true");
		}

		/*
		 * try to connect directly with the url. Several JDBC-Drivers allow to
		 * embed the username and password directly in the URL.
		 */
		if (_username == null || _password == null) {
			try {				
				_conn = HenPlus.getDataSource(_url, context).getConnection();
//				_conn = dsFactory.getDriver().connect(_url, props);
//				_conn = DriverManager.getConnection(_url, props);
			} catch (SQLException e) {
				_henplus.msg().println(e.getMessage());
				// only query terminals.
				if (_henplus.msg().isTerminal()) {
					promptUserPassword();
				}
			} catch (InvalidSyntaxException e) {
				_henplus.msg().print("can not connect to datasource " + e.getMessage());
			}
		}

//		if (_conn == null) {
//			Properties pros = new Properties();
//			pros.setProperty("user", _username);
//			pros.setProperty("password", _password);
//			_conn = dsFactory.getDriver().connect(_url, pros);
//		}

		if (_conn != null && _username == null) {
			try {
				DatabaseMetaData meta = _conn.getMetaData();
				if (meta != null) {
					_username = meta.getUserName();
				}
			} catch (Exception e) {
				/* ok .. at least I tried */
			}
		}
		_connectTime = System.currentTimeMillis();
	}

	private void promptUserPassword() throws IOException {
		_henplus.msg().println("============ authorization required ===");
		BufferedReader input = new BufferedReader(new InputStreamReader(
				System.in));
		_interrupted = false;
		try {
			SigIntHandler.getInstance().pushInterruptable(this);
//			HenPlus.getInstance();
			_henplus.msg().print("Username: ");
			_username = input.readLine();
			if (_interrupted) {
				throw new IOException("connect interrupted ..");
			}
			_password = promptPassword("Password: ");
			if (_interrupted) {
				throw new IOException("connect interrupted ..");
			}
		} finally {
			SigIntHandler.getInstance().popInterruptable();
		}
	}

	/**
	 * This is after a hack found in
	 * http://java.sun.com/features/2002/09/pword_mask.html
	 */
	private String promptPassword(String prompt) throws IOException {
		String password = "";
		PasswordEraserThread maskingthread = new PasswordEraserThread(prompt);
		try {
			byte lineBuffer[] = new byte[64];
			maskingthread.start();
			for (;;) {
				if (_interrupted) {
					break;
				}

				maskingthread.goOn();
				int byteCount = System.in.read(lineBuffer);
				/*
				 * hold on as soon as the system call returnes. Usually, this is
				 * because we read the newline.
				 */
				maskingthread.holdOn();

				for (int i = 0; i < byteCount; ++i) {
					char c = (char) lineBuffer[i];
					if (c == '\r') {
						c = (char) lineBuffer[++i];
						if (c == '\n') {
							return password;
						} else {
							continue;
						}
					} else if (c == '\n') {
						return password;
					} else {
						password += c;
					}
				}
			}
		} finally {
			maskingthread.done();
		}

		return password;
	}

	// -- Interruptable interface
	public void interrupt() {
		_interrupted = true;
		_henplus.msg().attributeBold();
		_henplus.msg().println(" interrupted; press [RETURN]");
		_henplus.msg().attributeReset();
	}

	/**
	 * return username, if known.
	 */
	public String getUsername() {
		return _username;
	}

	public long getUptime() {
		return System.currentTimeMillis() - _connectTime;
	}

	public long getStatementCount() {
		return _statementCount;
	}

	public void close() {
		try {
			getConnection().close();
			_conn = null;
		} catch (Exception e) {
			_henplus.msg().println(e.toString()); // don't care
		}
	}

	/**
	 * returns the current connection of this session.
	 */
	public Connection getConnection() {
		return _conn;
	}

	public Statement createStatement() {
		Statement result = null;
		int retries = 2;
		try {
			if (_conn.isClosed()) {
				_henplus.msg().println("connection is closed; reconnect.");
				connect();
				--retries;
			}
		} catch (Exception e) { /* ign */
		}

		while (retries > 0) {
			try {
				result = _conn.createStatement();
				++_statementCount;
				break;
			} catch (Throwable t) {
				_henplus.msg().println("connection failure. Try to reconnect.");
				try {
					connect();
				} catch (Exception e) { /* ign */
				}
			}
			--retries;
		}
		return result;
	}

	/* ------- Session Properties ----------------------------------- */

	private class ReadOnlyProperty extends BooleanPropertyHolder {

		ReadOnlyProperty() {
			super(false);
			_propertyValue = "off"; // 'off' sounds better in this context.
		}

		public void booleanPropertyChanged(boolean switchOn) throws Exception {
			/*
			 * readonly requires a closed transaction.
			 */
			if (!switchOn) {
				getConnection().rollback(); // save choice.
			} else {
				/*
				 * if we switched off and the user has not closed the current
				 * transaction, setting readonly will throw an exception and
				 * will notify the user about what to do..
				 */
			}
			getConnection().setReadOnly(switchOn);
			if (getConnection().isReadOnly() != switchOn) {
				throw new Exception(
						"JDBC-Driver ignores request; transaction closed before ?");
			}
		}

		public String getDefaultValue() {
			return "off";
		}

		public String getShortDescription() {
			return "Switches on read only mode for optimizations.";
		}
	}

	private class AutoCommitProperty extends BooleanPropertyHolder {

		AutoCommitProperty() {
			super(false);
			_propertyValue = "off"; // 'off' sounds better in this context.
		}

		public void booleanPropertyChanged(boolean switchOn) throws Exception {
			/*
			 * due to a bug in Sybase, we have to close the transaction first
			 * before setting autcommit. This is probably a save choice to do,
			 * since the user asks for autocommit..
			 */
			if (switchOn) {
				getConnection().commit();
			}
			getConnection().setAutoCommit(switchOn);
			if (getConnection().getAutoCommit() != switchOn) {
				throw new Exception("JDBC-Driver ignores request");
			}
		}

		public String getDefaultValue() {
			return "off";
		}

		public String getShortDescription() {
			return "Switches auto commit";
		}
	}

	private class IsolationLevelProperty extends EnumeratedPropertyHolder {
		private final Map _availableValues;
		private final String _initialValue;

		IsolationLevelProperty(Map availableValues, int currentValue) {
			super(availableValues.keySet());
			_availableValues = availableValues;

			// sequential search .. doesn't matter, not much do do
			String initValue = null;
			Iterator it = availableValues.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry entry = (Map.Entry) it.next();
				Integer isolationLevel = (Integer) entry.getValue();
				if (isolationLevel.intValue() == currentValue) {
					initValue = (String) entry.getKey();
					break;
				}
			}
			_propertyValue = _initialValue = initValue;
		}

		public String getDefaultValue() {
			return _initialValue;
		}

		protected void enumeratedPropertyChanged(int index, String value)
				throws Exception {
			Integer isolationLevel = (Integer) _availableValues.get(value);
			if (isolationLevel == null) {
				throw new IllegalArgumentException("invalid value");
			}
			int isolation = isolationLevel.intValue();
			getConnection().setTransactionIsolation(isolation);
			if (getConnection().getTransactionIsolation() != isolation) {
				throw new Exception("JDBC-Driver ignores request");
			}
		}

		public String getShortDescription() {
			return "sets the transaction isolation level";
		}
	}
}

/*
 * Local variables: c-basic-offset: 4 compile-command:
 * "ant -emacs -find build.xml" End:
 */

