package aeonics.manager;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.net.ssl.ExtendedSSLSession;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509TrustManager;

import aeonics.data.Data;
import aeonics.util.Callback;
import aeonics.util.Internal;
import aeonics.util.Tuples.Tuple;

/**
 * Creates and manages network connections
 */
public abstract class Network extends Manager.Type
{
	/**
	 * Hardcoded manager type
	 */
	public final Class<? extends Manager.Type> manager() { return Network.class; }
	
	/**
	 * Returns the current active instance of this manager type.
	 * @return the current active instance of this manager type
	 */
	public static Network get() { return Manager.of(Network.class); }
	
	/**
	 * This class represents a set of security options that can be applied to secure a {@link Connection}.
	 * Some methods are only meaningful in case of a client or server connection, they are named accordingly.
	 * If some options are not specified, defaults will be used.
	 * 
	 * <p>A server certificate is mandatory in case of a server connection.</p>
	 * <p>A client certificate is optional in case of a client connection.</p>
	 * <p>A server or client certificate that does not match the connection type will not be used.</p>
	 */
	public static class SecurityOptions
	{
		/**
		 * Parses and returns a certificate from the given PEM-encoded certificate, or a valid 'storage://' URL, or a valid local path
		 * @param certificate the PEM-encoded certificate, or a valid 'storage://' URL, or a valid local path
		 * @return the actual certificate (not the chain)
		 * @throws Exception if the provided certificate cannot be converted to valid X509Certificate
		 */
		public static X509Certificate certificate(String certificate) throws Exception
		{
			X509Certificate[] certs = certificates(certificate);
			if( certs.length > 0 ) return certs[0];
			else throw new IllegalArgumentException("Empty certificate");
		}
		
		/**
		 * Parses and returns all certificates from the given PEM-encoded certificate, or a valid 'storage://' URL, or a valid local path
		 * @param certificates the PEM-encoded certificates, or a valid 'storage://' URL, or a valid local path
		 * @return the actual certificate chain
		 * @throws Exception if the provided certificates cannot be converted to valid X509Certificate
		 */
		public static X509Certificate[] certificates(String certificates) throws Exception
		{
			if( certificates == null || certificates.isBlank() ) throw new IllegalArgumentException("Empty certificate");
			
			InputStream certData = null;
			if( certificates.startsWith("storage://") )
				certData = URI.create(certificates).toURL().openConnection().getInputStream();
			else if( certificates.startsWith("-----BEGIN ") )
				certData = new ByteArrayInputStream(certificates.getBytes(StandardCharsets.ISO_8859_1));
			else if( Files.isRegularFile(Paths.get(certificates)) )
				certData = new ByteArrayInputStream(Files.readAllBytes(Paths.get(certificates)));
			else
				throw new IllegalArgumentException("Invalid certificate");
			
			CertificateFactory factory = CertificateFactory.getInstance("X.509");
			Object[] os = factory.generateCertificates(certData).toArray();
			if( os.length == 0 ) throw new IllegalArgumentException("Invalid certificate");
			X509Certificate[] certs = new X509Certificate[os.length];
			for( int i = 0; i < os.length; i++ )
				certs[i] = (X509Certificate) os[i];
			
			return certs;
		}
		
		/**
		 * Parses and returns a private key from the given PEM-encoded key, or a valid 'storage://' URL, or a valid local path.
		 * <p><b>Caution, the key should be in PKCS#8 format.</b> If the key starts with <code>-----BEGIN PRIVATE KEY-----</code>
		 * you are probably good. If it starts with <code>-----BEGIN RSA PRIVATE KEY-----</code> then it will most probably fail.</p>
		 * @param cert the matching certificate (see {@link #certificate(String)})
		 * @param key the PEM-encoded private key, or a valid 'storage://' URL, or a valid local path
		 * @return the private key
		 * @throws Exception if the provided private key cannot be converted to valid PrivateKey
		 */
		public static PrivateKey privateKey(X509Certificate cert, String key) throws Exception
		{
			String keyData = null;
			if( key.startsWith("storage://") )
				keyData = new String(URI.create(key).toURL().openConnection().getInputStream().readAllBytes(), StandardCharsets.ISO_8859_1);
			else if( key.startsWith("-----BEGIN ") )
				keyData = key;
			else if( Files.isRegularFile(Paths.get(key)) )
				keyData = new String(Files.readAllBytes(Paths.get(key)), StandardCharsets.ISO_8859_1);
			else
				throw new IllegalArgumentException("Invalid key");
			
			keyData = keyData.replaceAll("\\s", "").replaceFirst(".*?-+[A-Z ]+-+", "").replaceFirst("-+[A-Z ]+-+.*$", "");
			byte[] pk = Base64.getDecoder().decode(keyData);
			PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(pk);
			KeyFactory factory2 = KeyFactory.getInstance(cert.getPublicKey().getAlgorithm());
			PrivateKey key2 = factory2.generatePrivate(spec);
			
			return key2;
		}
		
		/**
		 * Forces the client connection to authenticate with the provided certificate against the server.
		 * @param certificate the PEM-encoded client certificate, or a valid 'storage://' URL, or a valid local path
		 * @param key the matching PEM-encoded private key, or a valid 'storage://' URL, or a valid local path
		 * @return this
		 * @throws Exception if the provided arguments cannot be converted to valid X509Certificate and PrivateKey
		 */
		public SecurityOptions withClientCertificate(String certificate, String key) throws Exception
		{
			if( certificate == null || key == null ) { serverCertificate = null; return this; }
			
			X509Certificate cert = certificate(certificate);
			PrivateKey key2 = privateKey(cert, key);
			
			return withClientCertificate(cert, key2);
		}
		
		/**
		 * Forces the client connection to authenticate with the provided certificate against the server.
		 * @param certificate the client certificate
		 * @param key the matching private key
		 * @return this
		 */
		public SecurityOptions withClientCertificate(X509Certificate certificate, PrivateKey key)
		{
			if( certificate == null || key == null ) clientCertificate = null;
			else clientCertificate = new Tuple<X509Certificate, PrivateKey>(certificate, key);
			return this; 
		}
		
		/**
		 * The client certificate and key
		 */
		private Tuple<X509Certificate, PrivateKey> clientCertificate = null;
		
		/**
		 * Returns the client certificate and key
		 * @return the client certificate and key
		 * @hidden
		 */
		@Internal
		public Tuple<X509Certificate, PrivateKey> clientCertificate() { return clientCertificate; }
		
		/**
		 * Exposes the server connection with the provided certificate to all clients.
		 * @param certificate the server certificate
		 * @param key the matching private key
		 * @param chain the complete certificate chain. If null, or if the chain does not start with the original certificate, it will be prepended to the chain.
		 * @return this
		 */
		public SecurityOptions withServerCertificate(X509Certificate certificate, PrivateKey key, X509Certificate[] chain)
		{
			if( certificate == null || key == null ) serverCertificate = null;
			else serverCertificate = new Tuple<X509Certificate, PrivateKey>(certificate, key);
			
			if( chain == null || chain.length == 0 ) serverCertificateChain = new X509Certificate[] { certificate };
			else if( !chain[0].equals(certificate) )
			{
				serverCertificateChain = new X509Certificate[chain.length + 1];
				serverCertificateChain[0] = certificate;
				System.arraycopy(chain, 0, serverCertificateChain, 1, chain.length);
			}
			else
				serverCertificateChain = chain;
			
			return this; 
		}
		
		/**
		 * Exposes the server connection with the provided PEM-encoded certificate to all clients.
		 * @param certificate the PEM-encoded server certificate, or a valid 'storage://' URL, or a valid local path
		 * @param key the matching PEM-encoded private key, or a valid 'storage://' URL, or a valid local path
		 * @param chain the PEM-encoded server certificate, or a valid 'storage://' URL, or a valid local path. If null, or if the chain does not start with the original certificate, it will be prepended to the chain. 
		 * @return this
		 * @throws Exception if the provided arguments cannot be converted to valid X509Certificate and PrivateKey
		 */
		public SecurityOptions withServerCertificate(String certificate, String key, String chain) throws Exception
		{
			if( certificate == null || key == null ) { serverCertificate = null; return this; }
			
			// =============
			// CERTIFICATE
			// =============
			
			InputStream certData = null;
			if( certificate.startsWith("storage://") )
				certData = URI.create(certificate).toURL().openConnection().getInputStream();
			else if( certificate.startsWith("-----BEGIN ") )
				certData = new ByteArrayInputStream(certificate.getBytes(StandardCharsets.ISO_8859_1));
			else if( Files.isRegularFile(Paths.get(certificate)) )
				certData = new ByteArrayInputStream(Files.readAllBytes(Paths.get(certificate)));
			else
				throw new IllegalArgumentException("Invalid certificate");
			
			CertificateFactory factory = CertificateFactory.getInstance("X.509");
			Object[] os = factory.generateCertificates(certData).toArray();
			if( os.length == 0 ) throw new IllegalArgumentException("Invalid certificate");
			X509Certificate[] certs = new X509Certificate[os.length];
			for( int i = 0; i < os.length; i++ )
				certs[i] = (X509Certificate) os[i];
			X509Certificate cert = certs[0];
			
			// =============
			// CHAIN
			// =============
			
			X509Certificate[] chainCerts = certs;
		
			if( chain != null )
			{
				InputStream chainData = null;
				if( chain.startsWith("storage://") )
					chainData = URI.create(chain).toURL().openConnection().getInputStream();
				else if( chain.startsWith("-----BEGIN ") )
					chainData = new ByteArrayInputStream(chain.getBytes(StandardCharsets.ISO_8859_1));
				else if( Files.isRegularFile(Paths.get(chain)) )
					chainData = new ByteArrayInputStream(Files.readAllBytes(Paths.get(chain)));
				else
					throw new IllegalArgumentException("Invalid certificate");
				
				os = factory.generateCertificates(chainData).toArray();
				
				if( os.length > 0 )
				{
					int offset = 0;
					if( !cert.equals((X509Certificate) os[0]) )
					{
						chainCerts = new X509Certificate[os.length + 1];
						offset = 1;
						chainCerts[0] = cert;
					}
					else
						chainCerts = new X509Certificate[os.length];
					
					for( int i = 0; i < os.length; i++ )
						chainCerts[i + offset] = (X509Certificate) os[i];
				}
			}
			
			// =============
			// PRIVATE KEY
			// =============
			
			PrivateKey key2 = privateKey(cert, key);
			
			return withServerCertificate(cert, key2, chainCerts);
		}
		
		/**
		 * The server certificate and key
		 */
		private Tuple<X509Certificate, PrivateKey> serverCertificate = null;
		
		/**
		 * Returns the server certificate and key
		 * @return the server certificate and key
		 * @hidden
		 */
		@Internal
		public Tuple<X509Certificate, PrivateKey> serverCertificate() { return serverCertificate; }
		
		/**
		 * The server certificate chain (including the main server certificate as first element)
		 */
		private X509Certificate[] serverCertificateChain = null;
		
		/**
		 * Returns the server certificate chain with the main server certificate as first element
		 * @return the server certificate chain
		 * @hidden
		 */
		@Internal
		public X509Certificate[] serverCertificateChain() { return serverCertificateChain; }
		
		/**
		 * Sets a certificate selection function for the server based on the Server Name Indication (SNI) sent by the client.
		 * The SNI may be null if it was not provided by the client.
		 * @param selector the selection function that accepts the SNI and returns a tuple with the server certificate chain and its key
		 * @return this
		 */
		public SecurityOptions withServerCertificate(Function<String, Tuple<X509Certificate[], PrivateKey>> selector)
		{
			serverCertificateSelector = selector;
			return this;
		}
		
		/**
		 * The server certificate selector
		 */
		private Function<String, Tuple<X509Certificate[], PrivateKey>> serverCertificateSelector = null;
		
		/**
		 * Returns the server certificate selector
		 * @return the server certificate selector
		 * @hidden
		 */
		@Internal
		public Function<String, Tuple<X509Certificate[], PrivateKey>> serverCertificateSelector() { return serverCertificateSelector; }
		
		/**
		 * Sets a certificate verifier that can be used by the server to validate client certificates.
		 * The client certificate may be null if the client did not authenticate.
		 * If the client certificate is not admissible, the verifier must throw an exception.
		 * @param verifier the verifier function that accepts the client certificate (or null) 
		 * @return this
		 */
		public SecurityOptions withClientVerifier(Consumer<X509Certificate> verifier)
		{
			clientCertificateVerifier = verifier;
			return this;
		}
		
		/**
		 * The client certificate verifier
		 */
		private Consumer<X509Certificate> clientCertificateVerifier = null;
		
		/**
		 * Returns the client certificate verifier
		 * @return the client certificate verifier
		 * @hidden
		 */
		@Internal
		public Consumer<X509Certificate> clientCertificateVerifier() { return clientCertificateVerifier; }
		
		/**
		 * Sets a certificate verifier that can be used by the client to validate the server certificate.
		 * If the server certificate is not admissible, the verifier must throw an exception.
		 * @param verifier the verifier function that accepts the server certificate
		 * @return this
		 */
		public SecurityOptions withServerVerifier(Consumer<X509Certificate> verifier)
		{
			serverCertificateVerifier = verifier;
			return this;
		}
		
		/**
		 * The server certificate verifier
		 */
		private Consumer<X509Certificate> serverCertificateVerifier = null;
		
		/**
		 * Returns the server certificate verifier
		 * @return the server certificate verifier
		 * @hidden
		 */
		@Internal
		public Consumer<X509Certificate> serverCertificateVerifier() { return serverCertificateVerifier; }
		
		/**
		 * Specifies the list of accepted cryptographic ciphers.
		 * The list will be matched with {@link SSLEngine#getSupportedCipherSuites()} and only common entries will be exposed.
		 * If the list of retained elements is empty, then the defaults are used instead.
		 * @param accepted the list of accepted ciphers
		 * @return this
		 */
		public SecurityOptions withCiphers(List<String> accepted)
		{
			this.ciphers = accepted;
			return this;
		}
		
		/**
		 * The list of ciphers
		 */
		private List<String> ciphers = null;
		
		/**
		 * Returns the list of ciphers
		 * @return the list of ciphers
		 * @hidden
		 */
		@Internal
		public List<String> ciphers() { return ciphers; }
		
		/**
		 * Specifies the list of accepted cryptographic protocols.
		 * The list will be matched with {@link SSLEngine#getSupportedProtocols()} and only common entries will be exposed.
		 * If the list of retained elements is empty, then the defaults are used instead.
		 * @param accepted the list of accepted protocols
		 * @return this
		 */
		public SecurityOptions withProtocols(List<String> accepted)
		{
			this.protocols = accepted;
			return this;
		}
		
		/**
		 * The list of protocols
		 */
		private List<String> protocols = null;
		
		/**
		 * Returns the list of protocols
		 * @return the list of protocols
		 * @hidden
		 */
		@Internal
		public List<String> protocols() { return protocols; }
		
		/**
		 * Specified the list of accepted Application-Layer Protocol Names (ALPN)
		 * @param alpn the list of alpn
		 * @return this
		 */
		public SecurityOptions withAlpn(List<String> alpn)
		{
			this.alpn = alpn;
			return this;
		}
		
		/**
		 * The list of ALPN
		 */
		private List<String> alpn = null;
		
		/**
		 * Returns the list of ALPN
		 * @return the list of ALPN
		 * @hidden
		 */
		@Internal
		public List<String> alpn() { return alpn; }
	}
	
	/**
	 * Returns a key manager that matches the provided security options.
	 * This is useful to initialize SSL connections.
	 * @param options the security options (may be null)
	 * @param clientMode whether or not the role is client (or otherwise server)
	 * @return a matching key manager
	 */
	public static KeyManager keyManager(SecurityOptions options, boolean clientMode)
	{
		return new X509ExtendedKeyManager()
		{
			@Override
			public String chooseEngineServerAlias(String keyType, Principal[] issuers, SSLEngine engine)
			{
				if( engine.getHandshakeSession() instanceof ExtendedSSLSession )
				{
					List<SNIServerName> sni = ((ExtendedSSLSession)engine.getHandshakeSession()).getRequestedServerNames();
					if( sni == null || sni.isEmpty() )
						return alias;
					
					for( SNIServerName s : sni )
					{
						if( !(s instanceof SNIHostName) ) continue;
						return ((SNIHostName)s).getAsciiName();
					}
				}
				return alias;
			}
			
			public X509Certificate[] getCertificateChain(String alias)
			{
				if( options != null && clientMode )
				{
					Tuple<X509Certificate, PrivateKey> key = options.clientCertificate();
					if( key != null ) return new X509Certificate[] { key.a };
				}
				if( options != null && !clientMode )
				{
					X509Certificate[] chain = options.serverCertificateChain();
					if( chain != null ) return chain;
					Function<String, Tuple<X509Certificate[], PrivateKey>> sni = options.serverCertificateSelector();
					if( sni != null )
					{
						Tuple<X509Certificate[], PrivateKey> key = sni.apply(alias == null || alias.equals(this.alias) ? null : alias);
						if( key != null ) return key.a;
					}
				}
				return null;
			}
			
			public PrivateKey getPrivateKey(String alias)
			{
				if( options != null && clientMode )
				{
					Tuple<X509Certificate, PrivateKey> key = options.clientCertificate();
					if( key != null ) return key.b;
				}
				if( options != null && !clientMode )
				{
					Tuple<X509Certificate, PrivateKey> key = options.serverCertificate();
					if( key != null ) return key.b;
					Function<String, Tuple<X509Certificate[], PrivateKey>> sni = options.serverCertificateSelector();
					if( sni != null )
					{
						Tuple<X509Certificate[], PrivateKey> key2 = sni.apply(alias == null || alias.equals(this.alias) ? null : alias);
						if( key2 != null ) return key2.b;
					}
				}
				return null;
			}
			
			private final String alias = "default";
			private final String[] aliases = new String[] { alias };
			
			public String[] getServerAliases(String keyType, Principal[] issuers) { return aliases; }
			public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) { return alias; }
			
			public String[] getClientAliases(String keyType, Principal[] issuers) { return aliases; }
			public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) { return alias; }
		};
	}
	
	/**
	 * Returns a trust manager that matches the provided security options.
	 * This is useful to initialize SSL connections.
	 * @param options the security options (may be null)
	 * @return a matching trust manager
	 */
	public static TrustManager trustManager(SecurityOptions options)
	{
		return new X509TrustManager()
		{
			public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) throws CertificateException
			{
				if( certs != null && certs.length > 0 && options != null && options.clientCertificateVerifier() != null )
				{
					// we only give the actual cert, not the chain
					try { options.clientCertificateVerifier().accept(certs[0]); }
					catch(Exception e) { throw new CertificateException("Client certificate not trusted"); }
				}
			}
			public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) throws CertificateException
			{
				if( certs != null && certs.length > 0 && options != null && options.serverCertificateVerifier() != null )
				{
					// we only give the actual cert, not the chain
					try { options.serverCertificateVerifier().accept(certs[0]); }
					catch(Exception e) { throw new CertificateException("Server certificate not trusted"); }
				}
			}
			private final X509Certificate[] empty = new java.security.cert.X509Certificate[0];
			public java.security.cert.X509Certificate[] getAcceptedIssuers() { return empty; }
		};
	}
	
	/**
	 * Returns an ssl engine that matches the provided security options
	 * @param options the security options (may be null)
	 * @param clientMode whether or not the role is client (or otherwise server)
	 * @return a matching ssl engine
	 */
	public static SSLEngine sslEngine(SecurityOptions options, boolean clientMode)
	{
		try
		{
			SSLEngine ssl = sslContext(options, clientMode).createSSLEngine();
			
			if( options != null )
			{
				SSLParameters params = ssl.getSSLParameters();
				
				List<String> protocols = options.protocols();
				if( protocols == null || protocols.isEmpty() )
				{
					if( Manager.of(Config.class).contains(Network.class, "tls.default.protocols") )
					{
						Data defaults = Manager.of(Config.class).get(Network.class, "tls.default.protocols");
						if( defaults.isList() && !defaults.isEmpty() )
						{
							protocols = new ArrayList<String>(defaults.size());
							for( Data p : defaults ) protocols.add(p.asString());
						}
						else
							protocols = null;
					}
				}
				
				if( protocols != null && !protocols.isEmpty() )
				{
					List<String> supported = new LinkedList<String>();
					Collections.addAll(supported, ssl.getSupportedProtocols()); 
					supported.retainAll(protocols);
					if( !supported.isEmpty() )
						params.setProtocols(supported.toArray(new String[supported.size()]));
				}
				
				List<String> ciphers = options.ciphers();
				if( ciphers == null || ciphers.isEmpty() )
				{
					if( Manager.of(Config.class).contains(Network.class, "tls.default.ciphers") )
					{
						Data defaults = Manager.of(Config.class).get(Network.class, "tls.default.ciphers");
						if( defaults.isList() && !defaults.isEmpty() )
						{
							ciphers = new ArrayList<String>(defaults.size());
							for( Data c : defaults ) ciphers.add(c.asString());
						}
						else
							ciphers = null;
					}
				}
				
				if( ciphers != null && !ciphers.isEmpty() )
				{
					List<String> supported = new LinkedList<String>();
					Collections.addAll(supported, ssl.getSupportedCipherSuites());
					supported.retainAll(ciphers);
					if( !supported.isEmpty() )
						params.setCipherSuites(supported.toArray(new String[supported.size()]));
				}
				
				if( options.alpn() != null )
				{
					List<String> supported = options.alpn();
					params.setApplicationProtocols(supported.toArray(new String[supported.size()]));
				}
				
				if( !clientMode && options.clientCertificateVerifier() != null )
				{
					ssl.setNeedClientAuth(true);
				}
				
				ssl.setSSLParameters(params);
			}
			
			ssl.setUseClientMode(clientMode);
			return ssl;
		}
		catch(Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Returns an ssl context that matches the provided security options
	 * @param options the security options (may be null)
	 * @param clientMode whether or not the role is client (or otherwise server)
	 * @return a matching ssl context
	 */
	public static SSLContext sslContext(SecurityOptions options, boolean clientMode)
	{
		try
		{
			KeyManager[] km = new KeyManager[] { keyManager(options, clientMode) };
			TrustManager[] tm = new TrustManager[] { trustManager(options) };
			SSLContext tls = SSLContext.getInstance("TLS");
			tls.init(km, tm, SecureRandom.getInstanceStrong());
			return tls;
		}
		catch(Exception e)
		{
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Transforms an unsecure connection into a secure one using the provided security options.
	 * If the connection is already {@link Connection#isSecure()} then it is returned as is.
	 * If the security options argument is null, the connection is returned as is.
	 * <p><b>Caution</b> that if some existing {@link Connection#onReady()} or {@link Connection#onClose()} handlers were set
	 * on the original connection, you should adjust them on the returned secured connection instead</p>
	 * @see SecurityOptions
	 * @param unsecure the connection to secure
	 * @param options the security options for this connection, may be null
	 * @return the secured connection
	 */
	public abstract Connection securize(Connection unsecure, SecurityOptions options);
	
	/**
	 * This class represents an established network connection ready read and write data.
	 * <p>The {@link Connection#onReady()} callback will be called every time some data is available. You should then call {@link Connection#next()} to fetch the data.</p>
	 * <p>Once and if the connection is closed, the {@link Connection#onClose()} callback will be called.</p>
	 * <p>You may call the {@link Connection#close()} method to close the connection yourself.</p>
	 */
	public static interface Connection extends Closeable, Iterable<byte[]>, Iterator<byte[]>
	{
		/**
		 * Returns whether or not this connection is secure (typically using TLS or some other encryption mechanism)
		 * @return whether or not this connection is secure
		 */
		public boolean isSecure();
		
		/**
		 * Returns whether or not this connection is a client connection connected to a remote server
		 * @return whether or not this connection is a client connection connected to a remote server
		 */
		public boolean isClientMode();
		
		/**
		 * Returns whether or not this connection is a server connection connected to a remote client
		 * @return whether or not this connection is a server connection connected to a remote client
		 */
		public default boolean isServerMode() { return !isClientMode(); }
		
		/**
		 * Gets the callback object that will be called when data has been read from the network and is available.
		 * Since data read on the network should preserve its ordering, the callback signals that data is available and
		 * you should then call {@link Connection#next()} to fetch it.
		 * @return the onReady callback
		 */
		public Callback<Void, Connection> onReady();
		
		/**
		 * This is a non-blocking method to fetch the next batch of data that has been read from the network.
		 * There might be more than one batch of data, so use this method in a loop.
		 * If no further data is available, this method returns null.
		 * <p><b>This method is not thread safe</b>, you should handle the fact that {@link #onReady()} might be called
		 * at any time and the callback may be activated to use this method in parallel.</p>
		 * <p>Consider the following construct:</p>
		 * <pre><code>
		 * AtomicBoolean busy = new AtomicBoolean(false);
		 * connection.onReady().then((c) -&gt; {
		 *     while( c.hasNext() ) {
		 *         if (!busy.compareAndSet(false, true)) return;
		 *         
		 *         try {
		 *             for( byte[] data = c.next(); data != null; data = c.next() ) {
		 *                 // ... process data
		 *             } 
		 *         } finally { 
		 *             busy.set(false);
		 *         }
		 *     }
		 * });
		 * </code></pre> 
		 * @return the next batch of data or null if no data is available
		 */
		public byte[] next();
		
		/**
		 * Returns whether or not some data is available at the time of the call.
		 * This method does not ensure that a call to {@link #next()} will return a value because of thread concurrency.
		 * @return whether or not some data is available
		 */
		public boolean hasNext();
		
		/**
		 * Returns an iterator over the data read by this connection.
		 * It may return a null element in case of thread concurrency.
		 * @see #next()
		 * @see #hasNext()
		 * @return an iterator over read data
		 */
		public default Iterator<byte[]> iterator() { return this; }
		
		/**
		 * Writes the specified data on the network
		 * @param data the data to write
		 * @throws IOException if a underlying I/O error happens
		 */
		public default void write(byte[] data) throws IOException { write(ByteBuffer.wrap(data)); }
		
		/**
		 * Writes the specified data on the network
		 * @param data the data to write
		 * @throws IOException if a underlying I/O error happens
		 */
		public default void write(String data) throws IOException { write(ByteBuffer.wrap(data.getBytes(StandardCharsets.ISO_8859_1))); }
		
		/**
		 * Writes the specified data on the network
		 * @param data the data to write
		 * @throws IOException if a underlying I/O error happens
		 */
		public default void write(Data data) throws IOException { write(ByteBuffer.wrap(data.toString().getBytes(StandardCharsets.ISO_8859_1))); }
		
		/**
		 * Writes the specified data on the network.
		 * This method is synchronous, so if you want an asynchronous behavior, you can wrap the call
		 * in an {@link Executor} method.
		 * @param data the data to write
		 * @see Executor#io(aeonics.util.Functions.Runnable)
		 */
		public void write(ByteBuffer data);
		
		/**
		 * Gets the callback object that will be called once the connection is closed.
		 * @return the onClose callback
		 */
		public Callback<Void, Connection> onClose();
		
		/**
		 * Sets the timeout on this connection. If no network activity is detected in the specified interval, the connection shall be closed.
		 * @param ms the timeout delay in milliseconds
		 */
		public void timeout(long ms);
		
		/**
		 * Returns the client IP address
		 * @return the client IP address
		 */
		public String clientIp();
		
		/**
		 * Returns the server IP address
		 * @return the server IP address
		 */
		public String serverIp();
		
		/**
		 * Returns the Application-Layer Protocol Negotiation (ALPN) protocol once the TLS connection is established
		 * @return the ALPN or an empty string if no protocol was negotiated or if ALPN is not available
		 */
		public String alpn();
		
		/**
		 * Returns the Server Name Indication (SNI) once the TLS connection is established
		 * @return the SNI or an empty string if none was specified in the TLS handshake
		 */
		public String sni();
		
		/**
		 * Returns true if this connection has not been closed
		 * @return true if this connection has not been closed
		 */
		public boolean active();
	}
	
	/**
	 * This class represents a listening server connection ready to accept clients.
	 * <p>The {@link Server#onAccept()} callback will be called for each new accepted connection.</p>
	 * <p>Once and if the listening connection is closed, the {@link Server#onClose()} callback will be called.</p>
	 * <p>You may call the {@link Server#close()} method to close the connection yourself.</p>
	 */
	public static interface Server extends Closeable
	{
		/**
		 * Gets the callback object that will be called with newly established connections
		 * @return the onAccept callback
		 */
		public Callback<Connection, Server> onAccept();
		
		/**
		 * Gets the callback object that will be called once the connection is closed.
		 * @return the onClose callback
		 */
		public Callback<Void, Server> onClose();
		
		/**
		 * Returns whether or not future accepted connections will be secure (typically using TLS or some other encryption mechanism)
		 * @return whether or not future accepted connections will be secure
		 */
		public boolean isSecure();
		
		/**
		 * Returns the {@link SecurityOptions} associated with this server, or null if not secure. 
		 * The returned object is live, modifications to it will affect
		 * all future accepted connections without requiring a server restart.
		 * @return the security options, or null for plain connections
		 */
		public default SecurityOptions security() { return null; }
	}
	
	/**
	 * Establishes a client connection to the specified remote network endpoint.
	 * This is equivalent to calling {@link #client(String, int, SecurityOptions)} without security options.
	 * @param remoteAddress the remote ip address
	 * @param remotePort the remote port number
	 * @return the connected network connection
	 * @throws IOException if a underlying I/O error happens
	 */
	public Connection client(String remoteAddress, int remotePort) throws IOException
	{
		return client(remoteAddress, remotePort, null);
	}
	
	/**
	 * Establishes a secure client connection to the specified remote network endpoint.
	 * @param remoteAddress the remote ip address
	 * @param remotePort the remote port number
	 * @param options the client security options for this connection, may be null to specify "without security options"
	 * @return the connected network connection
	 * @throws IOException if a underlying I/O error happens
	 */
	public abstract Connection client(String remoteAddress, int remotePort, SecurityOptions options) throws IOException;
	
	/**
	 * Opens a server that listens for incoming network connections.
	 * This is equivalent to calling {@link #server(String, int, SecurityOptions)} without security options.
	 * @param localAddress the local ip address
	 * @param localPort the local port number
	 * @return the connected network channel
	 * @throws IOException if a underlying I/O error happens
	 */
	public Server server(String localAddress, int localPort) throws IOException
	{
		return server(localAddress, localPort, null);
	}
	
	/**
	 * Opens a server that listens for incoming network connections
	 * @param localAddress the local ip address
	 * @param localPort the local port number
	 * @param options the server security options to apply to all clients, may be null to specify "without security options"
	 * @return the connected network channel
	 * @throws IOException if a underlying I/O error happens
	 */
	public abstract Server server(String localAddress, int localPort, SecurityOptions options) throws IOException;
	
	/**
	 * This method forces the network manager to refresh its internal state if necessary.
	 */
	public abstract void refresh();
}
