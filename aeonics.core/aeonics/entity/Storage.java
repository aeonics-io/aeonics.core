package aeonics.entity;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.JDBCType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import aeonics.data.Data;
import aeonics.manager.Logger;
import aeonics.manager.Manager;
import aeonics.template.Item;
import aeonics.template.Parameter;
import aeonics.template.Relationship;
import aeonics.template.Template;
import aeonics.util.Functions.BiConsumer;
import aeonics.util.Json;
import aeonics.util.StringUtils;

/**
 * The Storage entity can be used to persist some content using a typical directory structure.
 * The storage persistency may be temporary or long-lived, local or remote, file-based or any other implementation.
 * 
 * <p>The basic {@link Type#put(String, byte[])} and {@link Type#get(String)} focus only on the content itself, not any metadata.
 * If some metadata is necessary (file attributes or else) it is recommended to wrap the content and the matadata in a {@link Data}
 * structure instead.</p>
 * 
 * <p>The storage paths are <b>case sensitive</b> and use a <b>forward slash</b> separator <code>"/"</code> for path traversal.</p>
 * 
 * <p>Within a storage, absolute or relative paths are treated equally, this means that the path <code>"foo/bar"</code> or <code>"/foo/bar"</code> 
 * are considered equal. Implementations should always normalize the path and eventually resolve or relativize if needed.</p>
 * 
 * @see Storage#normalize(String)
 * @see Storage#resolve(String, String)
 * @see Storage#relativize(String, String)
 */
public abstract class Storage extends Item<Storage.Type>
{
	/**
	 * Normalizes the provided path.
	 * <ul>
	 * <li>leading and trailing '/' are removed</li>
	 * <li>contiguous '/' are flattened as one</li>
	 * <li>current directory '.' are removed</li>
	 * <li>parent directory '..' are honored</li>
	 * <li>backslashes '\\' are converted to '/'</li>
	 * <li>blank or null is converted to ""</li>
	 * </ul>
	 * @param path the requested path
	 * @return the normalized path
	 */
	public static String normalize(String path)
	{
		if( path == null || path.isBlank() ) path = "";
		
		StringBuilder normalized = new StringBuilder();
        int mark = 0;
        boolean separator = false;
        for (int i = 0; i <= path.length(); i++)
        {
        	if( i == path.length() || path.charAt(i) == '/' || path.charAt(i) == '\\' )
        	{
        		if( i - mark == 1 && path.charAt(mark) == '.' )
        		{
        			// case './'
        		}
        		else if( i - mark == 2 && path.charAt(mark) == '.' && path.charAt(mark + 1) == '.' )
        		{
        			// case '../'
        			int lastSlash = normalized.lastIndexOf("/");
                    if (lastSlash >= 0) normalized.delete(lastSlash, normalized.length());
        		}
        		else if( i != mark )
        		{
        			// case 'name/'
        			if( mark > 0 && !separator ) normalized.append('/');
        			normalized.append(path, mark, i);
        		}
        		mark = i + 1;
        		separator = true;
        	}
        	else separator = false;
        }
        
        if( normalized.length() > 0 && normalized.charAt(0) == '/' ) return normalized.substring(1);
        return normalized.toString();
	}
	
	/**
	 * Resolves the provided path against a root path
	 * @param root the root path to resolve against (will not be normalized)
	 * @param path the requested path (will be normalized)
	 * @return the resolved path
	 */
	public static String resolve(String root, String path)
	{
		return root + "/" + normalize(path);
	}
	
	/**
	 * Relativizes the provided path against a root path
	 * @param root the root path to relativize against
	 * @param path the requested path
	 * @return the normalized relativized path
	 */
	public static String relativize(String root, String path)
	{
		String r = normalize(root);
		String p = normalize(path);
		if( p.startsWith(r) ) return normalize(p.substring(r.length()));
		return p;
	}
	
	/**
	 * Superclass for all storage entities.
	 */
	public abstract static class Type extends Entity
	{
		/**
		 * Store some content using the specified path name.
		 * By default, this method calls {@link #put(String, String)} with the <code>toString()</code> representation of the content.
		 * @param path the full path
		 * @param content the content to store
		 * @throws IllegalArgumentException if the content or the path are incorrect or cannot be honored
		 * @throws RuntimeException if a technical error happens
		 */
		public void put(String path, Data content) { put(path, content.toString()); }
		
		/**
		 * Store some content using the specified path name.
		 * By default, this method calls {@link #put(String, byte[])} with the <code>getBytes(StandardCharsets.UTF_8)</code> representation of the content.
		 * @param path the full path
		 * @param content the content to store
		 * @throws IllegalArgumentException if the content or the path are incorrect or cannot be honored
		 * @throws RuntimeException if a technical error happens
		 */
		public void put(String path, String content) { put(path, content.getBytes(StandardCharsets.UTF_8)); }
		
		/**
		 * Store some content using the specified path name.
		 * @param path the full path
		 * @param content the content to store
		 * @throws IllegalArgumentException if the content or the path are incorrect or cannot be honored
		 * @throws RuntimeException if a technical error happens
		 */
		public abstract void put(String path, byte[] content);
		
		/**
		 * Retrieve the content of the specified final entry
		 * @param path the full path
		 * @return the content or null if not found
		 */
		public abstract byte[] get(String path);
		
		/**
		 * Retrieve the content of the specified final entry as an UTF-8 String
		 * @param path the full path
		 * @return the content or null if not found
		 */
		public String getString(String path) { byte[] b = get(path); return b == null ? null : new String(b, StandardCharsets.UTF_8); }
		
		/**
		 * Retrieve the content of the specified final entry parsed from JSON
		 * @param path the full path
		 * @return the content or null if not found
		 */
		public Data getData(String path) { String s = getString(path); return s == null ? null : Json.decode(getString(path)); }
		
		/**
		 * Returns whether or not the specified path is a final entry
		 * @param path the full path
		 * @return true if this path exists
		 */
		public abstract boolean containsEntry(String path);
		
		/**
		 * Returns whether or not the specified path exists and is not a final entry (aka: a directory)
		 * @param path the full path
		 * @return true if this path exists
		 */
		public abstract boolean containsPath(String path);
		
		/**
		 * Removes all entries that match the provided path. If the path targets a parent entry (a directory),
		 * all sub-entries are removed recursively
		 * @param path the path entry
		 */
		public abstract void remove(String path);
		
		/**
		 * Lists all final entries (files only) that start with the provided path.
		 * If the path targets a final entry, that entry is returned.
		 * If the path targets a parent entry (a directory), all final entries are returned recursively.
		 * 
		 * <p>The returned entries are the full path to the root.</p>
		 * 
		 * @param path the path entry
		 * @return the list of matching entries
		 */
		public abstract Collection<String> list(String path);
		
		/**
		 * Lists all direct sub-entries of the provided path.
		 * If the priovided path is a final entry (a file), an empty list is returned.
		 * Entries that are not final entries (directories) will be suffixed with a <code>/</code> character.
		 * 
		 * <p>The returned entries are relative to the provided path, so if the path "/foo/bar" is provided and a direct child
		 * is "/foo/bar/beef", then "beef" is returned.</p>
		 * 
		 * @param path the path entry
		 * @return the list of direct sub-entries
		 */
		public abstract Collection<String> tree(String path);
		
		/**
		 * Removes all entries in this store
		 */
		public abstract void clear();
		
		/**
		 * Copies all entries from this storage to another storage
		 * @param other the destination storage
		 */
		public void drainTo(Storage.Type other) { drainTo("", other); }
		
		/**
		 * Copies all entries recursively starting at the specified path entry,
		 * from this storage to the other storage
		 * @param path the starting point to copy
		 * @param other the destination storage
		 */
		public void drainTo(String path, Storage.Type other)
		{
			for( String p : list(path) )
				other.put(p, get(p));
		}
		
		/**
		 * Hardcoded category to the {@link Storage} class
		 */
		@Override
		public final String category() { return StringUtils.toLowerCase(Storage.class); }
	}
	
	protected Class<? extends Storage> category() { return Storage.class; }
	
	// =========================================
	//
	// FILESYSTEM IMPLEMENTATION
	//
	// =========================================
	
	public static class File extends Storage
	{
		public static class Type extends Storage.Type
		{
			private Path path(String path)
			{
				return Path.of(resolve(valueOf("root").asString(), path));
			}
			
			public void put(String path, byte[] content)
			{
				Path p = path(path);
				try
				{
					if( p.getParent() != null )
						Files.createDirectories(p.getParent());
					Files.write(p, content);
				}
				catch(Exception e) { throw new RuntimeException(e); }
			}

			public byte[] get(String path)
			{
				Path p = path(path);
				try
				{
					if( !Files.isRegularFile(p) ) return null;
					return Files.readAllBytes(p);
				}
				catch(Exception e) { throw new RuntimeException(e); }
			}

			public boolean containsEntry(String path)
			{
				Path p = path(path);
				return Files.isRegularFile(p);
			}
			
			public boolean containsPath(String path)
			{
				Path p = path(path);
				return Files.isDirectory(p);
			}

			public void remove(String path)
			{
				Path p = path(path);
				Path root = Path.of(valueOf("root").asString());
				if( !Files.exists(p) ) return;
				
				if( Files.isRegularFile(p) )
				{
					try { Files.delete(p); }
					catch(Exception e) { throw new RuntimeException(e); }
				}
				else
				{
					try( Stream<Path> files = Files.walk(p) )
					{
						files
							.filter((f) -> !f.equals(root))
							.map(Path::toFile)
					        .sorted(Comparator.reverseOrder())
					        .forEach(java.io.File::delete);
					}
					catch(Exception e) { throw new RuntimeException(e); }
				}
			}

			public Collection<String> list(String path) 
			{
				Path p = path(path);
				if( !Files.exists(p) ) return Collections.emptyList();
				
				String root = valueOf("root").asString();
				Collection<String> paths = new LinkedList<String>();
				if( Files.isRegularFile(p) )
				{
					paths.add(relativize(root, p.toString()));
				}
				else
				{
					try( Stream<Path> files = Files.walk(p) )
					{
						files
							.filter(Files::isRegularFile)
							.forEach((f) -> paths.add(relativize(root, f.toString())));
					}
					catch(Exception e) { throw new RuntimeException(e); }
				}
				return paths;
			}

			public Collection<String> tree(String path) 
			{
				Path p = path(path);
				if( !Files.exists(p) ) return Collections.emptyList();
				
				Collection<String> paths = new LinkedList<String>();
				if( Files.isRegularFile(p) ) return paths;
				else
				{
					try( Stream<Path> files = Files.list(p) )
					{
						files
							.forEach((f) -> {
								if( Files.isRegularFile(f) )
									paths.add(relativize(p.toString(), f.toString()));
								else
									paths.add(relativize(p.toString(), f.toString()) + "/");
							});
					}
					catch(Exception e) { throw new RuntimeException(e); }
				}
				return paths;
			}

			public void clear()
			{
				Path root = Path.of(valueOf("root").asString());
				try( Stream<Path> files = Files.walk(root) )
				{
					files
						.filter((p) -> !p.equals(root))
				        .map(Path::toFile)
				        .sorted(Comparator.reverseOrder())
				        .forEach(java.io.File::delete);
				}
				catch(Exception e) { throw new RuntimeException(e); }
			}
		}
		
		protected Class<? extends File.Type> defaultTarget() { return File.Type.class; }
		protected Supplier<? extends File.Type> defaultCreator() { return File.Type::new; }
		
		@SuppressWarnings("unchecked")
		@Override
		public Template<? extends File.Type> template()
		{
			return (Template<File.Type>) super.template()
				.summary("Local file storage")
				.description("This storage is a direct access to the hard drive and stores the content in files as provided by the underlying operating system.")
				.add(new Parameter("root")
					.summary("Root directory")
					.description("The root directory of this storage. All content will be stored as file or directory under this root path.")
					.format(Parameter.Format.TEXT)
					.optional(true)
					.defaultValue(""))
				;
		}
	}
	
	// =========================================
	//
	// MEMORY IMPLEMENTATION
	//
	// =========================================
	
	public static class Memory extends Storage
	{
		public static class Type extends Storage.Type
		{
			private ConcurrentHashMap<String, byte[]> store = new ConcurrentHashMap<>();
			
			public void put(String path, byte[] content)
			{
				store.put(normalize(path), content);
			}
			
			public byte[] get(String path)
			{
				return store.get(normalize(path));
			}
			
			public boolean containsEntry(String path)
			{
				return store.containsKey(normalize(path));
			}
			
			public boolean containsPath(String path)
			{
				path = normalize(path) + "/";
				for( String p : store.keySet() )
					if( p.startsWith(path) )
						return true;
				return false;
			}
			
			public Collection<String> list(String path)
			{
				path = normalize(path);
				Collection<String> paths = new LinkedList<String>();
				for( String p : store.keySet() )
				{
					if( p.startsWith(path) )
						paths.add(p);
				}
				return paths;
			}
			
			public Collection<String> tree(String path)
			{
				path = normalize(path);
				
				Collection<String> paths = new LinkedList<String>();
				
				if( store.containsKey(path) ) return paths;
				else
				{
					if( path.length() > 0 ) path += "/";
					for( String p : store.keySet() )
					{
						if( p.length() > path.length() && p.startsWith(path) )
						{
							p = p.substring(path.length());
							int s = p.indexOf('/'); // find next separator
							if( s > -1 ) p = p.substring(0, s+1); // keep trailing separator
							if( p.length() > 0 && !paths.contains(p) ) paths.add(p);
						}
					}
				}
				return paths;
			}
			
			public void remove(String path)
			{
				path = normalize(path);
				for( String p : store.keySet() )
				{
					if( p.startsWith(path) )
						store.remove(p);
				}
			}
			
			public void clear()
			{
				store.clear();
			}
		}
		
		protected Class<? extends Memory.Type> defaultTarget() { return Memory.Type.class; }
		protected Supplier<? extends Memory.Type> defaultCreator() { return Memory.Type::new; }
		
		@SuppressWarnings("unchecked")
		@Override
		public Template<? extends Memory.Type> template()
		{
			return (Template<Memory.Type>) super.template()
				.summary("Memory storage")
				.description("This storage is non-persistent as it stores content directly in the heap memory of the application. However, it is the fastest to read and write data.")
				;
		}
	}
	
	// =========================================
	//
	// DATABASE IMPLEMENTATION
	//
	// =========================================
	
	public static class Database extends Storage
	{
		// SECURITY NOTE : 
		// This class is sql injection prone if the table or column name contains forged values.
		//
		// Although, those are pulled from the database itself using Database.schema().
		// So we assume that the database implementation would not auto-inject itself.
		
		public static class Type extends Storage.Type
		{
			private BiConsumer<Data, Entity> updateHandler;
			private BiConsumer<Void, Entity> removeHandler;
			
			public Type()
			{
				updateHandler = (Data data, Entity entity) ->
				{
					this.schema.set(null);
				};
				
				removeHandler = (Void data, Entity entity) ->
				{
					this.schema.set(null);
					entity.onUpdate().remove(updateHandler);
					entity.onRemove().remove(removeHandler);
					this.db.set(null);
				};
			}
			
			private Path path(String path)
			{
				return Path.of(normalize(path));
			}
			
			private AtomicReference<Data> schema = new AtomicReference<>();
			private Data schema()
			{
				Data s = schema.get();
				if( s != null ) return s;
				
				synchronized(schema)
				{
					if( schema.get() == null )
					{
						try { schema.set(db().schema()); }
						catch(Exception e) { throw new RuntimeException("Database schema unavailable", e); }
					}
				}
				
				return schema.get();
			}
			
			private AtomicReference<aeonics.entity.Database.Type> db = new AtomicReference<>();
			private aeonics.entity.Database.Type db()
			{
				aeonics.entity.Database.Type db = firstRelation("database");
				aeonics.entity.Database.Type cache = this.db.get();
				if( db != cache )
				{
					if( cache != null )
					{
						cache.onRemove().remove(removeHandler);
						cache.onUpdate().remove(updateHandler);
					}
					if( db != null )
					{
						db.onRemove().then(removeHandler);
						db.onUpdate().then(updateHandler);
					}
					this.schema.set(null);
					this.db.set(db);
				}
				if( db == null ) throw new IllegalStateException("Underlying database is not ready");
				
				return db;
			}
			
			@Override
			public void put(String path, Data content)
			{
				if( path == null || path.isBlank() ) throw new IllegalArgumentException("Invalid path " + path);
				if( path.endsWith(".json") ) path = path.substring(0, path.length()-5);
				
				Path p = path(path);
				if( p.getNameCount() == 2 || (p.getNameCount() == 1 && path.endsWith("/")) )
				{
					try
					{
						// add or update row
						String table = p.getName(0).toString();
						
						// the path was ending with '/' so we append the id from the data itself
						if( p.getNameCount() == 1 )
						{
							String tmp = p.getName(0).toString();
							boolean fixed = false;
							for( Data t : schema() )
							{
								if( tmp.equalsIgnoreCase(t.asString("name")) )
								{
									for( Data c : t.get("columns") )
									{
										if( c.asBool("primary") )
										{
											for( Map.Entry<String, Data> entry : content.entrySet() )
											{
												if( entry.getKey().equalsIgnoreCase(c.asString("name")) )
												{
													path += entry.getValue().asString();
													p = path(path);
													fixed = true;
												}
											}
										}
									}
								}
							}
							if( !fixed ) throw new RuntimeException("Could not infer record primary key");
						}
						
						String id = p.getName(1).toString();
						
						for( Data t : schema() )
						{
							if( table.equalsIgnoreCase(t.asString("name")) )
							{
								if( containsEntry(path) )
								{
									// UPDATE
									String sql = "UPDATE " + t.asString("name") + " SET ";
									List<Object> params = new ArrayList<>();
									String where = " WHERE ";
									for( Data c : t.get("columns") )
									{
										if( c.asBool("primary") ) where += c.asString("name") + " = ?";
										for( Map.Entry<String, Data> entry : content.entrySet() )
										{
											if( entry.getKey().equalsIgnoreCase(c.asString("name")) )
											{
												sql += c.asString("name") + " = ?,";
												params.add(entry.getValue().asString());
											}
										}
									}
									if( params.size() == 0 ) return; // nothing to update
									sql = sql.substring(0, sql.length() - 1);
									params.add(id);
									
									db().query(sql + where, params);
									return;
								}
								else
								{
									// INSERT
									String sql = "INSERT INTO " + t.asString("name") + " ("; 
									String values = ") VALUES (";
									List<Object> params = new ArrayList<>();
									boolean primarySet = false;
									String primaryName = null;
									for( Data c : t.get("columns") )
									{
										if( c.asBool("primary") ) primaryName = c.asString("name");
										for( Map.Entry<String, Data> entry : content.entrySet() )
										{
											if( entry.getKey().equalsIgnoreCase(c.asString("name")) )
											{
												if( c.asBool("primary") ) primarySet = true;
												
												sql += c.asString("name") + ",";
												values += "?,";
												params.add(entry.getValue().asString());
											}
										}
									}
									if( params.size() == 0 && primarySet ) return; // nothing to insert
									if( !primarySet )
									{
										sql += primaryName + ",";
										values += "?,";
										params.add(id);
									}
									sql = sql.substring(0, sql.length() - 1);
									values = values.substring(0, values.length() - 1);
									
									db().query(sql + values + ")", params);
									return;
								}
							}
						}
					}
					catch(Exception e)
					{
						throw new RuntimeException(e); 
					}
				}
				else if( p.getNameCount() == 1 )
				{
					// add table
					String table = p.getName(0).toString();
					if( !StringUtils.isComposedOf(table, "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890_") ) throw new IllegalArgumentException("Illegal table name " + table);
					
					if( !content.isList("columns") ) throw new IllegalArgumentException("Missing or invalid 'columns' definition");
					
					String sql = "CREATE TABLE " + table + " (";
					String primary = "PRIMARY KEY (";
					for( Data c : content.get("columns") )
					{
						if( !c.containsKey("name") ) throw new IllegalArgumentException("Missing column name");
						if( !StringUtils.isComposedOf(c.asString("name"), "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890_") ) throw new IllegalArgumentException("Illegal column name " + c.asString("name"));
						
						sql += c.asString("name") + " ";
						
						JDBCType type = null;
						if( c.containsKey("type") )
							type = JDBCType.valueOf(c.asString("type"));
						else
							type = JDBCType.NVARCHAR;
						
						sql += type.getName();
						
						if( c.containsKey("size") )
							sql += "(" + c.asInt("size") + ")";
							
						if( c.containsKey("null") && !c.asBool("null") )
							sql += " NOT NULL";
						
						if( c.containsKey("primary") && c.asBool("primary") )
							primary += c.asString("name") + "))";
						
						sql += ", ";
					}
					
					try { db().query(sql + primary); }
					catch(Exception e) { throw new RuntimeException(e); }
				}
				else
					throw new IllegalArgumentException("Invalid path " + path);
			}
			
			@Override
			public void put(String path, String content) { put(path, Json.decode(content)); }
			
			public void put(String path, byte[] content) { put(path, Json.decode(new String(content, StandardCharsets.UTF_8))); }

			@Override
			public Data getData(String path)
			{
				if( path == null || path.isBlank() ) return null;
				if( path.endsWith(".json") ) path = path.substring(0, path.length()-5);
				
				Path p = path(path);
				if( p.getNameCount() > 2 ) return null;
				
				// root = schema
				if( p.getNameCount() == 0 || p.equals(Path.of("")) ) return schema().clone();
				
				// table = definition
				if( p.getNameCount() == 1 )
				{
					String table = p.getName(0).toString();
					for( Data t : schema() )
					{
						if( table.equalsIgnoreCase(t.asString("name")) )
						{
							return t.clone();
						}
					}
					return null;
				}
				
				try
				{
					String table = p.getName(0).toString();
					String id = p.getName(1).toString();
					
					for( Data t : schema() )
					{
						if( table.equalsIgnoreCase(t.asString("name")) )
						{
							for( Data c : t.get("columns") )
							{
								if( c.asBool("primary") )
								{
									Data rows = db().query("SELECT * FROM " + t.asString("name") + " WHERE " + c.asString("name") + " = ?", id);
									if( rows.isEmpty() ) return null;
									else return rows.get(0);
								}
							}
						}
					}
					
					return null;
				}
				catch(Exception e)
				{
					Manager.of(Logger.class).finer(Database.class, e);
					return null;
				}
			}
			
			@Override
			public String getString(String path)
			{
				Data data = getData(path);
				if( data == null ) return null;
				else return data.toString();
			}

			public byte[] get(String path)
			{
				Data data = getData(path);
				if( data == null ) return null;
				else return data.toString().getBytes(StandardCharsets.UTF_8);
			}

			public boolean containsEntry(String path)
			{
				if( path == null || path.isBlank() ) return false;
				if( path.endsWith(".json") ) path = path.substring(0, path.length()-5);
				
				Path p = path(path);
				if( p.getNameCount() != 2 ) return false;
				
				try
				{
					String table = p.getName(0).toString();
					String id = p.getName(1).toString();
					
					for( Data t : schema() )
					{
						if( table.equalsIgnoreCase(t.asString("name")) )
						{
							for( Data c : t.get("columns") )
							{
								if( c.asBool("primary") )
								{
									Data rows = db().query("SELECT " + c.asString("name") + " FROM " + t.asString("name") + " WHERE " + c.asString("name") + " = ?", id);
									return rows.size() == 1;
								}
							}
						}
					}
					
					return false;
				}
				catch(Exception e)
				{
					Manager.of(Logger.class).finer(Database.class, e);
					return false;
				}
			}

			public boolean containsPath(String path)
			{
				if( path == null ) return false;
				if( path.isBlank() ) return true;
				
				Path p = path(path);
				if( p.getNameCount() == 0 || p.equals(Path.of("")) ) return true;
				if( p.getNameCount() != 1 ) return false;
				
				try
				{
					String table = p.getName(0).toString();
					
					for( Data t : schema() )
					{
						if( table.equalsIgnoreCase(t.asString("name")) )
						{
							return true;
						}
					}
					
					return false;
				}
				catch(Exception e)
				{
					Manager.of(Logger.class).finer(Database.class, e);
					return false;
				}
			}

			public void remove(String path)
			{
				if( path == null ) return;
				if( path.endsWith(".json") ) path = path.substring(0, path.length()-5);
				
				if( path.isBlank() ) { clear(); return; }
				Path p = path(path);
				if( p.getNameCount() == 0 || p.equals(Path.of("")) ) { clear(); return; }
				if( p.getNameCount() > 2 ) return;
				
				try
				{
					String table = p.getName(0).toString();
					String id = p.getNameCount() == 2 ? p.getName(1).toString() : null;
					
					for( Data t : schema() )
					{
						if( table.equalsIgnoreCase(t.asString("name")) )
						{
							if( id == null )
							{
								db().query("TRUNCATE TABLE " + t.asString("name"));
								return;
							}
							
							for( Data c : t.get("columns") )
							{
								if( c.asBool("primary") )
								{
									db().query("DELETE FROM " + t.asString("name") + " WHERE " + c.asString("name") + " = ?", id);
									return;
								}
							}
						}
					}
				}
				catch(Exception e)
				{
					Manager.of(Logger.class).finer(Database.class, e);
				}
			}

			public Collection<String> list(String path)
			{
				if( path == null || path.isBlank() ) return Collections.emptyList();
				if( path.endsWith(".json") ) path = path.substring(0, path.length()-5);
				
				Path p = path(path);
				if( p.getNameCount() >= 2 ) return Collections.emptyList();
				else if( p.getNameCount() == 0 || p.equals(Path.of("")) )
				{
					int limit = valueOf("maxRecords").asInt();
					List<String> all = new ArrayList<>();
					for( Data t : schema() )
					{
						String table = t.asString("name") + "/";
						all.addAll(
							tree(table).stream().map(id -> table.concat(id)).collect(Collectors.toList())
						);
						if( all.size() >= limit ) return all;
					}
					return all;
				}
				else if( p.getNameCount() == 1 )
				{
					String table = p.getName(0).toString() + "/";
					return tree(path).stream().map(id -> table.concat(id)).collect(Collectors.toList());
				}
				else if( p.getNameCount() == 2 && containsEntry(path) )
					return Arrays.asList(path);
				
				return Collections.emptyList();
			}

			public Collection<String> tree(String path)
			{
				if( path == null || path.isBlank() ) return Collections.emptyList();
				if( path.endsWith(".json") ) path = path.substring(0, path.length()-5);
				
				Path p = path(path);
				if( p.getNameCount() >= 2 ) return Collections.emptyList();
		
				List<String> list = new ArrayList<>();
				if( p.getNameCount() == 0 || p.equals(Path.of("")) )
				{
					for( Data t : schema() )
						list.add(t.asString("name") + "/");
					return list;
				}
				else if( p.getNameCount() == 1 )
				{
					String table = p.getName(0).toString();
					for( Data t : schema() )
					{
						if( table.equalsIgnoreCase(t.asString("name")) )
						{
							String primary = null;
							for( Data c : t.get("columns") )
								if( c.asBool("primary") )
									primary = c.asString("name");
							if( primary == null )
								return Collections.emptyList();
							
							String sql = "SELECT " + primary + " FROM " + t.asString("name") + " ORDER BY " + primary + " ASC";
							
							int limit = valueOf("maxRecords").asInt();
							if( limit > 0 )
								sql += " OFFSET 0 ROWS FETCH FIRST " + limit + " ROWS ONLY";
							
							try
							{
								Data rows = db().query(sql); 
							
								for( Data row : rows )
									list.add(row.asString(primary));
								return list;
							}
							catch(Exception e) { throw new RuntimeException(e); }
						}
					}
				}
				
				return Collections.emptyList();
			}

			public void clear()
			{
				try
				{
					for( Data t : schema() )
						db().query("DROP TABLE " + t.asString("name"));
				}
				catch(Exception e)
				{
					Manager.of(Logger.class).finer(Database.class, e);
				}
			}
		}
		
		protected Class<? extends Database.Type> defaultTarget() { return Database.Type.class; }
		protected Supplier<? extends Database.Type> defaultCreator() { return Database.Type::new; }
		
		@SuppressWarnings("unchecked")
		@Override
		public Template<? extends Database.Type> template()
		{
			return (Template<Database.Type>) super.template()
				.summary("Database storage")
				.description("This storage uses a database connection to store data. Therefore, the path structure is limited and must always start with the table name and "
					+ "then the primary key of the record of interest. This also implies that this storage is only compatible with tables that define one simple primary key "
					+ "(no composite primary key is supported). In order to improve performance, this storage keeps a cache of the database schema. This cache is "
					+ "populated at first use and refreshed whenever the underlying database entity is updated.")
				.add(new Relationship("database")
					.category(aeonics.entity.Database.class)
					.summary("Database")
					.description("The target underlying database")
					.min(1).max(1))
				.add(new Parameter("maxRecords")
					.summary("Maximum record count")
					.description("When listing the content of the storage, a database may have a large number of rows. This parameter limits the number of returned "
						+ "elements. The elements are always sorted in ascending order. Setting this parameter to 0 or a negative value means unlimited.")
					.format(Parameter.Format.NUMBER)
					.rule(Parameter.Rule.INTEGER)
					.defaultValue(-1)
					.optional(true))
				;
		}
	}
}
