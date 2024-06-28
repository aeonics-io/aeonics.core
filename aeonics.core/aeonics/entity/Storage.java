package aeonics.entity;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Stream;

import aeonics.data.Data;
import aeonics.template.Item;
import aeonics.template.Parameter;
import aeonics.template.Template;
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
	 * @param root the root path to resolve against
	 * @param path the requested path
	 * @return the normalized resolved path
	 */
	public static String resolve(String root, String path)
	{
		return normalize(root + "/" + path);
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
		 */
		public void put(String path, Data content) { put(path, content.toString()); }
		
		/**
		 * Store some content using the specified path name.
		 * By default, this method calls {@link #put(String, byte[])} with the <code>getBytes(StandardCharsets.UTF_8)</code> representation of the content.
		 * @param path the full path
		 * @param content the content to store
		 */
		public void put(String path, String content) { put(path, content.getBytes(StandardCharsets.UTF_8)); }
		
		/**
		 * Store some content using the specified path name.
		 * @param path the full path
		 * @param content the content to store
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
		public String getString(String path) { return new String(get(path), StandardCharsets.UTF_8); }
		
		/**
		 * Retrieve the content of the specified final entry parsed from JSON
		 * @param path the full path
		 * @return the content or null if not found
		 */
		public Data getData(String path) { return Json.decode(getString(path)); }
		
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
		 * <p>The returned entries are relative to the provided path, so if the path "/foo/bar" is provided and a child
		 * is "/foo/bar/beef/dead", then "beef/dead" is returned.</p>
		 * 
		 * @param path the path entry
		 * @return the list of matching entries
		 */
		public abstract Collection<String> list(String path);
		
		/**
		 * Lists all direct sub-entries of the provided path.
		 * If the priovided path is a final entry (a file), an empty list is returned.
		 * Entries that are not final entries (directories) will be suffixed with a <code>File.separator</code>
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
					.defaultValue(Data.of("")));
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
				.description("This storage is non-persistent as it stores content directly in the heap memory of the application. However, it is the fastest to read and write data.");
		}
	}
}
