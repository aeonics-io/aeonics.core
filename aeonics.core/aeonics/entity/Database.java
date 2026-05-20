package aeonics.entity;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import aeonics.data.Data;
import aeonics.manager.Logger;
import aeonics.manager.Manager;
import aeonics.manager.Monitor;
import aeonics.template.Item;
import aeonics.template.Parameter;
import aeonics.template.Template;
import aeonics.util.Functions.Consumer;
import aeonics.util.Internal;
import aeonics.util.StringUtils;

/**
 * This entity represents a JDBC connection to a database.
 * It wraps the query operations with a connection pool and returns the result as a {@link Data} structure.
 * 
 * <p>There are two recommended ways to create your own item inline (without creating a full class).
 * The first method allows to provide the data connection establishment function and registers automatically the template in
 * the factory and the instance in the registry:</p>
 * <pre>
 * Database.Type item = new Database() { } // &lt;-- note the '{ }' to create a new anonymous class
 *     
 *     .template() // &lt;-- create the template and register it in the factory
 *     
 *     // add all your template documentation
 *     .summary("My favourite database")
 *     
 *     .create() // &lt;-- create an instance of the entity and register it in the registry
 *     
 *     // set the processing function
 *     .connection(() -&gt; { return new Database.PooledConnection(item, null); }); // &lt;-- the connection establishment function
 * </pre>
 * 
 * <p>If you need more control over the behavior such as private member variables or multiple
 * methods, then you need to declare a custom entity end register it <b>before</b> calling the template method:</p>
 * <pre>
 * public static class MyEntity extends Database.Type {
 *     protected Database.PooledDatabase connection() { return null; }
 * }
 * 
 * Database.Type action = new Database() { } // &lt;-- note the '{ }' to create a new anonymous class
 *     
 *     // register the custom entity before calling the template
 *     .target(MyEntity.class)
 *     .creator(MyEntity::new)
 *     
 *     .template() // &lt;-- create the template and register it in the factory
 *     
 *     // add all your template documentation
 *     .summary("My database")
 *     
 *     .create(); // &lt;-- create an instance of the entity and register it in the registry
 * </pre>
 */
public class Database extends Item<Database.Type>
{
	/** 
	 * JDBC 4.3 Specification (JSR 221)
	 * 
	 * Table B.4) Mapping from Java Object Types to JDBC Types
	 * +--------------------------+-------------------------------------------------------------+
	 * | Java Object Type         | JDBC Type                                                   |
	 * +--------------------------+-------------------------------------------------------------+
	 * | String                   | CHAR, VARCHAR, LONGVARCHAR, NCHAR, NVARCHAR or LONGNVARCHAR |
	 * | java.math.BigDecimal     | NUMERIC                                                     |
	 * | Boolean                  | BIT or BOOLEAN                                              |
	 * | Byte                     | TINYINT                                                     |
	 * | Short                    | SMALLINT                                                    |
	 * | Integer                  | INTEGER                                                     |
	 * | Long                     | BIGINT                                                      |
	 * | Float                    | REAL                                                        |
	 * | Double                   | DOUBLE                                                      |
	 * | byte[]                   | BINARY, VARBINARY, or LONGVARBINARY                         |
	 * | java.math.BigInteger     | BIGINT                                                      |
	 * | java.sql.Date            | DATE                                                        |
	 * | java.sql.Time            | TIME                                                        |
	 * | java.sql.Timestamp       | TIMESTAMP                                                   |
	 * | java.sql.Clob            | CLOB                                                        |
	 * | java.sql.Blob            | BLOB                                                        |
	 * | java.sql.Array           | ARRAY                                                       |
	 * | java.sql.Struct          | STRUCT                                                      |
	 * | java.sql.Ref             | REF                                                         |
	 * | java.net.URL             | DATALINK                                                    |
	 * | java.sql.RowId           | ROWID                                                       |
	 * | java.sql.NClob           | NCLOB                                                       |
	 * | java.sql.SQLXML          | SQLXML                                                      |
	 * | java.util.Calendar       | TIMESTAMP                                                   |
	 * | java.util.Date           | TIMESTAMP                                                   |
	 * | java.time.LocalDate      | DATE                                                        |
	 * | java.time.LocalTime      | TIME                                                        |
	 * | java.time.LocalDateTime  | TIMESTAMP                                                   |
	 * | java.time.OffsetTime     | TIME_WITH_TIMEZONE                                          |
	 * | java.time.OffsetDatetime | TIMESTAMP_WITH_TIMEZONE                                     |
	 * +--------------------------+-------------------------------------------------------------+
	 * 
	 * Table B.5) Conversions Performed by setObject and setNull Between Java Object Types and Target JDBC Types
	 * +--------------------------+-------------------------------------------+
	 * | Java Object Type         | Supported JDBC Type                       |
	 * +--------------------------+-------------------------------------------+
	 * | String                   | TINYINT, SMALLINT, INTEGER, BIGINT, REAL, |
	 * |                          | FLOAT, DOUBLE, DECIMAL, NUMERIC, BIT,     |
	 * |                          | BOOLEAN, CHAR, VARCHAR, LONGVARCHAR,      |
	 * |                          | BINARY, VARBINARY, LONVARBINARY, DATE,    |
	 * |                          | TIME, TIMESTAMP, NCHAR, NVARCHAR,         |
	 * |                          | LONGNVARCHAR                              |
	 * +--------------------------+-------------------------------------------+
	 * | java.math.BigDecimal     | TINYINT, SMALLINT, INTEGER, BIGINT, REAL, |
	 * |                          | FLOAT, DOUBLE, DECIMAL, NUMERIC, BIT,     |
	 * |                          | BOOLEAN, CHAR, VARCHAR, LONGVARCHAR       |
	 * +--------------------------+-------------------------------------------+
	 * | Boolean                  | TINYINT, SMALLINT, INTEGER, BIGINT, REAL, |
	 * |                          | FLOAT, DOUBLE, DECIMAL, NUMERIC, BIT,     |
	 * |                          | BOOLEAN, CHAR, VARCHAR, LONGVARCHAR       |
	 * +--------------------------+-------------------------------------------+
	 * | Byte                     | TINYINT, SMALLINT, INTEGER, BIGINT, REAL, |
	 * |                          | FLOAT, DOUBLE, DECIMAL, NUMERIC, BIT,     |
	 * |                          | BOOLEAN, CHAR, VARCHAR, LONGVARCHAR       |
	 * +--------------------------+-------------------------------------------+
	 * | Short                    | TINYINT, SMALLINT, INTEGER, BIGINT, REAL, |
	 * |                          | FLOAT, DOUBLE, DECIMAL, NUMERIC, BIT,     |
	 * |                          | BOOLEAN, CHAR, VARCHAR, LONGVARCHAR       |
	 * +--------------------------+-------------------------------------------+
	 * | Integer                  | TINYINT, SMALLINT, INTEGER, BIGINT, REAL, |
	 * |                          | FLOAT, DOUBLE, DECIMAL, NUMERIC, BIT,     |
	 * |                          | BOOLEAN, CHAR, VARCHAR, LONGVARCHAR       |
	 * +--------------------------+-------------------------------------------+
	 * | Long                     | TINYINT, SMALLINT, INTEGER, BIGINT, REAL, |
	 * |                          | FLOAT, DOUBLE, DECIMAL, NUMERIC, BIT,     |
	 * |                          | BOOLEAN, CHAR, VARCHAR, LONGVARCHAR       |
	 * +--------------------------+-------------------------------------------+
	 * | Float                    | TINYINT, SMALLINT, INTEGER, BIGINT, REAL, |
	 * |                          | FLOAT, DOUBLE, DECIMAL, NUMERIC, BIT,     |
	 * |                          | BOOLEAN, CHAR, VARCHAR, LONGVARCHAR       |
	 * +--------------------------+-------------------------------------------+
	 * | Double                   | TINYINT, SMALLINT, INTEGER, BIGINT, REAL, |
	 * |                          | FLOAT, DOUBLE, DECIMAL, NUMERIC, BIT,     |
	 * |                          | BOOLEAN, CHAR, VARCHAR, LONGVARCHAR       |
	 * +--------------------------+-------------------------------------------+
	 * | byte[]                   | BINARY, VARBINARY, or LONGVARBINARY       |
	 * +--------------------------+-------------------------------------------+
	 * | java.math.BigInteger     | BIGINT, CHAR, VARCHAR, LONGVARCHAR        |
	 * +--------------------------+-------------------------------------------+
	 * | java.sql.Date            | CHAR, VARCHAR, LONGVARCHAR, DATE,         |
	 * |                          | TIMESTAMP                                 |
	 * +--------------------------+-------------------------------------------+
	 * | java.sql.Time            | CHAR, VARCHAR, LONGVARCHAR, TIME,         |
	 * |                          | TIMESTAMP                                 |
	 * +--------------------------+-------------------------------------------+
	 * | java.sql.Timestamp       | CHAR, VARCHAR, LONGVARCHAR, DATE, TIME,   |
	 * |                          | TIMESTAMP                                 |
	 * +--------------------------+-------------------------------------------+
	 * | java.sql.Array           | ARRAY                                     |
	 * +--------------------------+-------------------------------------------+
	 * | java.sql.Blob            | BLOB                                      |
	 * +--------------------------+-------------------------------------------+
	 * | java.sql.Clob            | CLOB                                      |
	 * +--------------------------+-------------------------------------------+
	 * | java.sql.Struct          | STRUCT                                    |
	 * +--------------------------+-------------------------------------------+
	 * | java.sql.Ref             | REF                                       |
	 * +--------------------------+-------------------------------------------+
	 * | java.net.URL             | DATALINK                                  |
	 * +--------------------------+-------------------------------------------+
	 * | Java class               | JAVA_OBJECT                               |
	 * +--------------------------+-------------------------------------------+
	 * | java.sql.RowId           | ROWID                                     |
	 * +--------------------------+-------------------------------------------+
	 * | java.sql.NClob           | NCLOB                                     |
	 * +--------------------------+-------------------------------------------+
	 * | java.sql.SQLXML          | SQLXML                                    |
	 * +--------------------------+-------------------------------------------+
	 * | java.util.Calendar       | CHAR, VARCHAR, LONGVARCHAR, DATE, TIME,   |
	 * |                          | TIMESTAMP, ARRAY                          |
	 * +--------------------------+-------------------------------------------+
	 * | java.util.Date           | CHAR, VARCHAR, LONGVARCHAR, DATE, TIME,   |
	 * |                          | TIMESTAMP, ARRAY                          |
	 * +--------------------------+-------------------------------------------+
	 * | java.time.LocalDate      | CHAR, VARCHAR, LONGVARCHAR, DATE          |
	 * +--------------------------+-------------------------------------------+
	 * | java.time.LocalTime      | CHAR, VARCHAR, LONGVARCHAR, TIME          |
	 * +--------------------------+-------------------------------------------+
	 * | java.time.LocalDateTime  | CHAR, VARCHAR, LONGVARCHAR, DATE, TIME,   |
	 * |                          | TIMESTAMP                                 |
	 * +--------------------------+-------------------------------------------+
	 * | java.time.OffsetTime     | CHAR, VARCHAR, LONGVARCHAR,               |
	 * |                          | TIME_WITH_TIMEZONE                        |
	 * +--------------------------+-------------------------------------------+
	 * | java.time.OffsetDatetime | CHAR, VARCHAR, LONGVARCHAR,               |
	 * |                          | TIME_WITH_TIMEZONE,                       |
	 * |                          | TIMESTAMP_WITH_TIMEZONE                   |
	 * +--------------------------+-------------------------------------------+
	 */
	
	/**
	 * This class is a wrapper around a java.sql.Connection class to be used with the
	 * pooled {@link Database.Type}.
	 */
	@SuppressWarnings("exports")
	public static class PooledConnection implements AutoCloseable
	{
		/**
		 * The connection pool
		 */
		private Database.Type pool;
		
		/**
		 * The underlying connection
		 */
		private Connection connection;
		
		/**
		 * Creates a new connection attached to the specified pool
		 * @param pool the connection pool
		 * @param connection the underlying jdbc connection 
		 */
		public PooledConnection(Database.Type pool, Connection connection)
		{
			Objects.requireNonNull(pool);
			Objects.requireNonNull(connection);
			this.pool = pool;
			this.connection = connection;
		}
		
		/**
		 * Closes the underlying database connection.
		 * Do not confuse this method with the {@link #close()} method that simply returns this object to the connection pool.
		 */
		public void destroy()
		{
			pool = null;
			if( connection != null )
			{
				try { connection.close(); } catch (SQLException e) { /* silent */ }
				connection = null;
			}
		}
		
		/**
		 * Returns true if the underlying database connection is ready to perform a query
		 * @return true if the underlying database connection is ready to perform a query
		 */
		public boolean isValid()
		{
			try { return connection != null && connection.isValid(1); }
			catch(SQLException e) { return false; }
		}
		
		/**
		 * Performs a query.
		 * 
		 * <p>CAUTION : use in a <code>try...with</code> statement or do not forget to {@link #close()} after use to return to the pool
		 * The returned column names should ALWAYS be lower case.</p>
		 * 
		 * <p>The parameters provided for substitution will be provided to the underlying connection using 
		 * {@link java.sql.PreparedStatement#setObject(int, Object)} so that the compatibility with String values is maximized.
		 * (see <i>JDBC 4.3 Specification (JSR 221) Table B.5</i> for Conversions Performed by setObject and setNull Between 
		 * Java Object Types and Target JDBC Types)</p>
		 * 
		 * @param sql the parameterized query to perform
		 * @param params the parameters to substitute.
		 * @return the resulset (Data list), the inserted primary key (Data object), or the number of affected rows depending on the case
		 * @throws SQLException if an error occurs at the database level
		 */
		public Data query(String sql, Object... params) throws SQLException
		{
			if( connection == null )
			{
				pool.deadConnection(this);
				throw new SQLException("Underlying database connection is not available");
			}
			
			try( PreparedStatement p = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS) )
			{
				for( int i = 0; i < params.length; i++ )
				{
					if( params[i] instanceof Data )
						p.setObject(i+1, ((Data)params[i]).asString());
					else
						p.setObject(i+1, params[i]);
				}
				if( p.execute() )
				{
					// select
					try( ResultSet r = p.getResultSet() )
					{
						Data rows = Data.list();
						while( r.next() )
						{
							ResultSetMetaData metadata = r.getMetaData();
							Data cols = Data.map();
							for( int i = 1; i <= metadata.getColumnCount(); i++ )
								cols.put(metadata.getColumnLabel(i).toLowerCase(), r.getObject(i));
							rows.add(cols);
						}
						return rows;
					}
				}
				else
				{
					// insert
					try( ResultSet r = p.getGeneratedKeys() )
					{
						if( r.next() )
						{
							ResultSetMetaData metadata = r.getMetaData();
							int columns = metadata.getColumnCount();
							
							Data cols = Data.map();
							for( int i = 1; i <= columns; i++ )
								cols.put(metadata.getColumnLabel(i).toLowerCase(), r.getObject(i));
							return cols;
						}
					}
					
					// update
					return Data.of(p.getUpdateCount());
				}
			}
			catch(SQLException e)
			{
				if( !connection.isValid(1) )
				{
					this.pool.deadConnection(this);
				}
				Manager.of(Logger.class).finer(Database.class, sql);
				Manager.of(Logger.class).finer(Database.class, e);
				throw e;
			}
		}
		
		/**
		 * Return this connection to the pool after a query operation is complete.
		 * <p>You should not need to call this method manually if used in a <code>try...with</code> statement.</p>
		 */
		@Internal
		public void close()
		{
			if( pool != null )
				pool.returnConnection(this);
		}
		
		/**
		 * Returns the list of tables that can be accessed from this connection.
		 * 
		 * <p>The returned data is a list of table descriptions that include:</p>
		 * <ul>
		 * <li><b>name</b>: the table name</li>
		 * <li><b>schema</b>: the table schema</li>
		 * <li><b>database</b>: the table database (or catalog in JDBC terms)</li>
		 * </ul>
		 * 
		 * @see java.sql.DatabaseMetaData#getTables(String, String, String, String[])
		 * @return the list of tables
		 * @throws SQLException if an error happens
		 */
		public Data tables() throws SQLException
		{
			try
			{
				ResultSet r = connection.getMetaData().getTables(connection.getCatalog(), connection.getSchema(), "%", null);
				Data tables = Data.list();
				while( r.next() )
				{
					tables.add(Data.map()
						.put("name", r.getString("TABLE_NAME"))
						.put("schema", r.getString("TABLE_SCHEM"))
						.put("database", r.getString("TABLE_CAT"))
					);
				}
				return tables;
			}
			catch(SQLException e)
			{
				pool.deadConnection(this);
				throw e;
			}
		}
		
		/**
		 * Returns the list of columns in the specified table.
		 * 
		 * <p>The returned data is a list of column descriptions that include:</p>
		 * <ul>
		 * <li><b>name</b>: the column name</li>
		 * <li><b>size</b>: the column data type size</li>
		 * <li><b>auto</b>: whether or not the column has a default or auto-increment value</li>
		 * <li><b>null</b>: whether or not the column accepts null values</li>
		 * <li><b>type</b>: the JDBC data type as a string</li>
		 * <li><b>primary</b>: whether or not the column is part of a primary key</li>
		 * </ul>
		 * The information returned by this method is not exhaustive and may lack details for a complete automatic structure discovery.
		 * However it should be sufficient for most basic applications.
		 * 
		 * @see java.sql.DatabaseMetaData#getColumns(String, String, String, String)
		 * @see java.sql.JDBCType
		 * @param table the name of the table
		 * @return the list of columns
		 * @throws SQLException if an error happens
		 */
		public Data columns(String table) throws SQLException
		{
			try
			{
				ResultSet r = connection.getMetaData().getColumns(connection.getCatalog(), connection.getSchema(), table, null);
				if( !r.next() )
				{
					// maybe the table name is not an exact match
					Data tables = tables();
					for( Data t : tables )
					{
						if( table.equalsIgnoreCase(t.asString("name")) )
						{
							table = t.asString("name");
							break;
						}
					}
					
					r = connection.getMetaData().getColumns(connection.getCatalog(), connection.getSchema(), table, null);
					if( !r.next() ) return Data.list();
				}
				
				ResultSet rpk = connection.getMetaData().getPrimaryKeys(connection.getCatalog(), connection.getSchema(), table);
				List<String> primary = new LinkedList<>();
				while( rpk.next() )
					primary.add(rpk.getString("COLUMN_NAME"));
				
				Data columns = Data.list();
				do
				{
					columns.add(Data.map()
						.put("name", r.getString("COLUMN_NAME"))
						.put("size", r.getString("COLUMN_SIZE"))
						.put("auto", r.getString("IS_AUTOINCREMENT").equals("YES") || r.getString("COLUMN_DEF") != null)
						.put("null", r.getString("IS_NULLABLE").equals("YES"))
						.put("type", JDBCType.valueOf(r.getInt("DATA_TYPE")).getName())
						.put("primary", primary.contains(r.getString("COLUMN_NAME")))
					);
				} while( r.next() );
				
				return columns;
			}
			catch(SQLException e)
			{
				pool.deadConnection(this);
				throw e;
			}
		}
	}
	
	/**
	 * Superclass for all database entities.
	 */
	public static class Type extends Entity
	{
		/**
		 * The inline connection supplier
		 */
		private Supplier<PooledConnection> connection = () ->
		{
			try
			{
				Class.forName(valueOf("driver").asString());
				String u = valueOf("username").asString();
				String p = valueOf("password").asString();
				
				if( !u.isBlank() && !p.isBlank() )
				{
					return new PooledConnection(this, DriverManager.getConnection(
						valueOf("jdbc").asString(), 
						valueOf("username").asString(), 
						valueOf("password").asString()
					));
				}
				else
				{
					return new PooledConnection(this, DriverManager.getConnection(valueOf("jdbc").asString()));
				}
			}
			catch(Exception e)
			{
				Manager.of(Logger.class).info(Database.class, e);
			}
			return null;
		};
		
		/**
		 * Sets the process function as an alternative to {@link #connection()}.
		 * @param <T> this
		 * @param connector the connection supplier
		 * @return this
		 */
		@SuppressWarnings("unchecked")
		public <T extends Database.Type> T connection(Supplier<PooledConnection> connector) { this.connection = connector; return (T) this; }
		
		/**
		 * Returns a new connection to the database.
		 * This method will be called on-demand as part of the connection pool.
		 * The connection is marked as auto-commit.
		 * @return a new connection to the database
		 */
		protected PooledConnection connection()
		{
			if( connection != null )
			{
				PooledConnection c = connection.get();
				try { c.connection.setAutoCommit(true); }
				catch(Exception e)
				{
					c.destroy();
					return null;
				}
				return c;
			}
			else return null;
		}
		
		// =========================================
		//
		// CONNECTION POOL IMPLEMENTATION
		//
		// =========================================
		
		/**
		 * A Semaphore that you can reduce
		 */
		private static class ResizableSemaphore extends Semaphore
		{
			private int size;
			public ResizableSemaphore(int size) { super(size); this.size = size; }
			public synchronized void reduceBy(int value)
			{
				if( value <= 0 ) return;
				if( value >= size ) value = size - 1;
				super.reducePermits(value);
			}
			public int size() { return size; }
		}
		
		/**
		 * Tracker of current connections
		 */
		private ResizableSemaphore permits = new ResizableSemaphore(1);
		
		/**
		 * List of idle connections
		 */
		private LinkedBlockingQueue<PooledConnection> idle = new LinkedBlockingQueue<PooledConnection>();
		
		/**
		 * List of active connections
		 */
		private LinkedBlockingQueue<PooledConnection> active = new LinkedBlockingQueue<PooledConnection>();
		
		/**
		 * Returns the current connection pool size
		 * @return the current connection pool size
		 */
		public int size() { return valueOf("size").asInt(); }
		
		/**
		 * Update the connection pool size based on the current {@link #size()}
		 * @hidden
		 */
		@Internal
		public void refreshPoolSize()
		{
			int value = size();
			int size = permits.size();
			
			if( value == size ) // equal : do nothing
				return;
			else if( value > size ) // greater : increase limit
			{
				permits.size = value;
				permits.release(value-size);
			}
			else // smaller : reduce and cleanup
			{
				permits.size = value;
				permits.reduceBy(size-value);
				
				// we need to purge some connections
				int clean = size-value;
				// first start with idle connections
				while( clean > 0 && !idle.isEmpty() )
				{
					PooledConnection c = idle.poll();
					if( c == null ) break;
					try { c.destroy(); } catch(Exception e) { /* silent */ }
					clean--;
				}
				// if we need to purge more, continue active connections
				while( clean > 0 && !active.isEmpty() )
				{
					PooledConnection c = active.poll();
					if( c == null ) break;
					clean--;
				}
				// maybe we did not clear enough because we did not have enough in the first
				// place, or because of race condition. Anyway, those would become dormant which 
				// will not consume much
			}
		}
		
		/**
		 * Returns the next available connection from the pool waiting at most <code>timeout</code> milliseconds.
		 * If a timeout occurs, an SQLException is thrown.
		 * 
		 * <p>It is always preferable to use the {@link #query(String, Object...)} method unless you need to group a set of queries together
		 * as a transaction. In this case, you should use {@link #transaction(Consumer)}</p>
		 * 
		 * @param timeout the maximum number of milliseconds to wait for a connection to be available. A negative value means wait forever.
		 * @return the next available connection
		 * @throws SQLException if an underlying exception is raised, or if a timeout happens
		 * @throws InterruptedException if the operation is interrupted while waiting
		 */
		public PooledConnection next(long timeout) throws SQLException, InterruptedException
		{
			if( timeout > 0 )
			{
				if( !permits.tryAcquire(timeout, TimeUnit.MILLISECONDS) )
					throw new SQLException("No database connection available within allowed timeframe");
			}
			else
				permits.acquire();
			
			PooledConnection c;
			try
			{
				// past this point means that we are allowed to get a connection.
				// if none left, create one
				c = idle.poll();
				if( c == null )
					c = connection();
				if( c == null )
					throw new SQLException("No connection available");
			}
			catch(Exception e)
			{
				permits.release();
				throw e;
			}
			
			active.offer(c);
			
			try
			{
				if( c.isValid() )
					return c;
				else
				{
					deadConnection(c);
					return next(timeout); // get a new connection
				}
			}
			catch(Exception e)
			{
				deadConnection(c);
				throw e;
			}
		}
		
		/**
		 * Return a connection to the pool after a query operation.
		 * This method should only be called from the {@link PooledConnection#close()} method.
		 * @param connection the connection to return
		 * @hidden
		 */
		@Internal
		void returnConnection(PooledConnection connection)
		{
			if( active.remove(connection) )
			{
				if( connection.isValid() )
					idle.offer(connection);
				else
				{
					try { connection.destroy(); } catch(Exception e) { /* silent */ }
				}
				
				permits.release();
			}
			else
			{
				try { connection.destroy(); } catch(Exception e) { /* silent */ }
			}
		}
		
		/**
		 * Signals that a connection from this pool is dead and should be removed.
		 * The connection will also be {@link PooledConnection#destroy()}.
		 * This method should only be called from the {@link PooledConnection#query(String, Object...)} method when the underlying database link is broken.
		 * @param connection the connection to remove
		 * @hidden
		 */
		@Internal
		void deadConnection(PooledConnection connection)
		{
			if( active.remove(connection) )
				permits.release();
			try { connection.destroy(); } catch(Exception e) { /* silent */ }
		}
		
		/**
		 * Performs a query on this database, waiting indefinitely for a connection to be available from the pool.
		 * 
		 * <p>The parameters provided for substitution will be provided to the underlying connection using 
		 * {@link java.sql.PreparedStatement#setObject(int, Object)} so that the compatibility with String values is maximized.
		 * (see <i>JDBC 4.3 Specification (JSR 221) Table B.5</i> for Conversions Performed by setObject and setNull Between 
		 * Java Object Types and Target JDBC Types)</p>
		 * 
		 * @param sql the sql parameterized query
		 * @param params the parameters of the query
		 * @return the result as a list of map with column names in lower case
		 * @throws SQLException if an error happens
		 */
		public Data query(String sql, Collection<Object> params) throws SQLException
		{
			return query(-1, sql, params.toArray());
		}
		
		/**
		 * Performs a query on this database, waiting indefinitely for a connection to be available from the pool.
		 * 
		 * <p>The parameters provided for substitution will be provided to the underlying connection using 
		 * {@link java.sql.PreparedStatement#setObject(int, Object)} so that the compatibility with String values is maximized.
		 * (see <i>JDBC 4.3 Specification (JSR 221) Table B.5</i> for Conversions Performed by setObject and setNull Between 
		 * Java Object Types and Target JDBC Types)</p>
		 * 
		 * @param sql the sql parameterized query
		 * @param params the parameters of the query
		 * @return the result as a list of map with column names in lower case
		 * @throws SQLException if an error happens
		 */
		public Data query(String sql, Object ... params) throws SQLException
		{
			return query(-1, sql, params);
		}
		
		/**
		 * Performs a query on this database.
		 * 
		 * <p>The parameters provided for substitution will be provided to the underlying connection using 
		 * {@link java.sql.PreparedStatement#setObject(int, Object)} so that the compatibility with String values is maximized.
		 * (see <i>JDBC 4.3 Specification (JSR 221) Table B.5</i> for Conversions Performed by setObject and setNull Between 
		 * Java Object Types and Target JDBC Types)</p>
		 * 
		 * @param timeout the maximum time to wait to get a connection from the pool. A negative value means wait forever.
		 * @param sql the sql parameterized query
		 * @param params the parameters of the query
		 * @return the result as a list of map with column names in lower case
		 * @throws SQLException if an error happens
		 */
		public Data query(long timeout, String sql, Object ... params) throws SQLException
		{
			long start = System.nanoTime();
			try( PooledConnection c = next(timeout) )
			{
				return c.query(sql, params);
			}
			catch(SQLException e)
			{
				throw e;
			}
			catch(Exception e)
			{
				throw new SQLException(e);
			}
			finally
			{
				Manager.of(Monitor.class).count(this, "time", System.nanoTime() - start);
			}
		}
		
		/**
		 * Group a series of operations in a SQL transaction.
		 * On successful completion, the transaction is committed automatically.
		 * On failure (throw), the transaction is rolled back automatically.
		 * <p><b>You MUST use the passed connection to perform the queries, not the database object itself.</b></p>
		 * 
		 * <pre>
		 * Database db = ...;
		 * db.transaction(tx -> {
		 *     tx.query("...");
		 *     tx.query("...");
		 *     tx.query("...");
		 * });
		 * </pre>
		 * 
		 * @param operations the function to run operations, it accepts the single connection to query against.
		 * @throws SQLException if an error happens
		 * @throws InterruptedException if the transation was interrupted or could not start in due time
		 */
		public void transaction(Consumer<PooledConnection> operations) throws SQLException, InterruptedException
		{
			long start = System.nanoTime();
			PooledConnection c = next(-1);
			
			try { c.connection.setAutoCommit(false); }
			catch(Exception x) { c.destroy(); }
			
			try
			{
				operations.accept(c);
				c.connection.commit();
			}
			catch(SQLException e)
			{
				c.connection.rollback();
				throw e;
			}
			catch(Exception e)
			{
				c.connection.rollback();
				throw new SQLException(e);
			}
			finally
			{
				try { c.connection.setAutoCommit(true); }
				catch(Exception x) { c.destroy(); }
				
				c.close(); // return the connection to the pool
				Manager.of(Monitor.class).count(this, "time", System.nanoTime() - start);
			}
		}
		
		/**
		 * Returns the list of tables that can be accessed from this connection.
		 * 
		 * <p>The returned data is a list of table descriptions that include:</p>
		 * <ul>
		 * <li><b>name</b>: the table name</li>
		 * <li><b>schema</b>: the table schema</li>
		 * <li><b>database</b>: the table database (or catalog in JDBC terms)</li>
		 * </ul>
		 * 
		 * @see java.sql.DatabaseMetaData#getTables(String, String, String, String[])
		 * @return the list of tables
		 * @throws SQLException if an error happens
		 * @throws InterruptedException if the operation is interrupted
		 */
		public Data tables() throws SQLException, InterruptedException
		{
			try( PooledConnection c = next(-1) )
			{
				return c.tables();
			}
		}
		
		/**
		 * Returns the list of columns in the specified table.
		 * 
		 * <p>The returned data is a list of column descriptions that include:</p>
		 * <ul>
		 * <li><b>name</b>: the column name</li>
		 * <li><b>size</b>: the column data type size</li>
		 * <li><b>auto</b>: whether or not the column has a default or auto-increment value</li>
		 * <li><b>null</b>: whether or not the column accepts null values</li>
		 * <li><b>type</b>: the JDBC data type as a string</li>
		 * <li><b>primary</b>: whether or not the column is part of a primary key</li>
		 * </ul>
		 * The information returned by this method is not exhaustive and may lack details for a complete automatic structure discovery.
		 * However it should be sufficient for most basic applications.
		 * 
		 * @see java.sql.DatabaseMetaData#getColumns(String, String, String, String)
		 * @see java.sql.JDBCType
		 * @param table the name of the table
		 * @return the list of columns
		 * @throws SQLException if an error happens
		 * @throws InterruptedException if the operation is interrupted
		 */
		public Data columns(String table) throws SQLException, InterruptedException
		{
			try( PooledConnection c = next(-1) )
			{
				return c.columns(table);
			}
		}
		
		/**
		 * Returns a full database schema for this connection.
		 * The returned value is the same as {@link #tables()} with an additional value 'columns' that is equal to {@link #columns(String)}.
		 * 
		 * @return a full database schema
		 * @throws SQLException if an error happens
		 * @throws InterruptedException if the operation is interrupted
		 */
		public Data schema() throws SQLException, InterruptedException
		{
			try( PooledConnection c = next(-1) )
			{
				Data tables = c.tables();
				for( Data t : tables )
				{
					t.put("columns", c.columns(t.asString("name")));
				}
				return tables;
			}
		}
		
		public Data export()
		{
			// hide the password from the export
			Data x = super.export();
			x.get("parameters").remove("password");
			return x;
		}
		
		/**
		 * Hardcoded category to the {@link Database} class
		 */
		@Override
		public final String category() { return StringUtils.toLowerCase(Database.class); }
	}
	
	protected Class<? extends Database.Type> defaultTarget() { return Database.Type.class; }
	protected Supplier<? extends Database.Type> defaultCreator() { return Database.Type::new; }
	protected Class<? extends Database> category() { return Database.class; }

	@Override
	public Template<? extends Database.Type> template()
	{
		return super.template()
			.cast()
			.summary("SQL Database")
			.description("This entity type provides database connectivity through standard JDBC connections using an internal connection pool.")
			.add(new Parameter("size")
				.summary("Maximum number of connections")
				.description("The maximum number of simultaneous connections to the database. Connections will only be established on-demand up to this limit.")
				.rule(Parameter.Rule.DIGIT)
				.format(Parameter.Format.NUMBER)
				.optional(true)
				.defaultValue(1))
			.add(new Parameter("jdbc")
				.summary("JDBC connection string")
				.description("The JDBC connection string to the database.")
				.format(Parameter.Format.TEXT)
				.optional(false))
			.add(new Parameter("driver")
				.summary("Driver")
				.description("The full class name of the database driver.")
				.format(Parameter.Format.TEXT)
				.optional(false))
			.add(new Parameter("username")
				.summary("Username")
				.description("The username to connect to the database.")
				.format(Parameter.Format.TEXT)
				.optional(true))
			.add(new Parameter("password")
				.summary("Password")
				.description("The password to connect to the database.")
				.format(Parameter.Format.PASSWORD)
				.optional(true))
			.onCreate((data, instance) ->
			{
				if( data.get("parameters").containsKey("size") )
					((Database.Type)instance).refreshPoolSize();
			})
			.onUpdate((data, instance) ->
			{
				if( data.get("parameters").containsKey("size") )
					((Database.Type)instance).refreshPoolSize();
			})
			.cast();
	}
}
