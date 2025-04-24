package aeonics.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import aeonics.data.Data;
import aeonics.manager.Network;

/**
 * Simple http request fetcher.
 * The result is returned as data. If the response mime type was <code>application/json</code>, then it is automatically decoded.
 */
public class Http
{
	/**
	 * Returns an SSLContext to validate a mutual TLS authentication with the server.
	 * @param clientCertificate the client certificate as PEM format
	 * @param clientKey the client private key as PEM format
	 * @param serverCertificate the server certificate (or signing authority) to authenticate the server
	 * @return a mTLS SSLContext
	 * @throws Exception in case of invalid parameters
	 */
	public static SSLContext mTLS(String clientCertificate, String clientKey, String serverCertificate) throws Exception
	{
		final X509Certificate trusted = Network.SecurityOptions.certificate(serverCertificate);
		
		return Network.sslContext(new Network.SecurityOptions()
			.withClientCertificate(clientCertificate, clientKey)
			.withServerVerifier(cert ->
			{
				try
				{
					cert.checkValidity();
					if( cert.equals(trusted) ) return;
					if( trusted.getBasicConstraints() >= 0 ) // trusted is a CA that can issue the server certificate
					{
						boolean[] keyUsage = trusted.getKeyUsage();
			            boolean hasKeyCertSign = keyUsage == null || (keyUsage.length > 5 && keyUsage[5]);
						
						if( hasKeyCertSign )
						{
							cert.verify(trusted.getPublicKey());
							return;
						}
					}
					throw new RuntimeException();
				}
				catch(GeneralSecurityException e)
				{
					throw new RuntimeException();
				}
			}), true);
	}
	
	private static SSLSocketFactory __bugfix(final SSLSocketFactory f, final String host)
	{
		// there is an old bug that has regression in which the https connection
		// does not send SNI in some cases (i.e. if the hostnameVerifier is set).
		// the solution is that the socket factory should not allow to create unconnected sockets
		
		// see bug 6771432 and 8144566
		
		final SSLParameters sni = new SSLParameters();
		sni.setServerNames(List.of(new SNIHostName(host)));
		
		return new SSLSocketFactory() {
			private Socket setSNI(Socket s) { ((SSLSocket) s).setSSLParameters(sni); return s; }
			public String[] getDefaultCipherSuites() { return f.getDefaultCipherSuites(); }
			public String[] getSupportedCipherSuites() { return f.getSupportedCipherSuites(); }
			public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
				return setSNI(f.createSocket(s, host, port, autoClose)); }
			public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
				return setSNI(f.createSocket(host, port)); }
			public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException, UnknownHostException {
				return setSNI(f.createSocket(host, port, localHost, localPort)); }
			public Socket createSocket(InetAddress host, int port) throws IOException {
				return setSNI(f.createSocket(host, port)); }
			public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
				return setSNI(f.createSocket(address, port, localAddress, localPort)); }
		};
	}
	
	/**
	 * The maximum number of milliseconds to wait before the connection is established.
	 * This does not affect the response time once the connection is established.
	 */
	public static int CONNECT_TIMEOUT = 1500;
	
	private Http() { /* no instances */ }
	
	/**
	 * Represents an HTTP Error code with possibly a body response.
	 */
	public static class Error extends IllegalStateException
	{
		/**
		 * The response code
		 */
		public int code = 500;
		
		/**
		 * The response body (may be null)
		 */
		public String body = null;
		
		/**
		 * Creates a new error
		 * @param code the code
		 * @param body the body
		 */
		public Error(int code, String body) { this.code = code; this.body = body; }
		
		public String toString()
		{
			return "Http error " + code + ": " + body;
		}
	}
	
	/**
	 * Fetches the specified resource using POST
	 * @param url the url
	 * @return the response
	 * @throws Http.Error if the remote endpoint returns an error (http code 400+)
	 */
	public static Data post(String url) { return post(url, null, null, "POST"); }
	
	/**
	 * Fetches the specified resource using POST
	 * @param url the url
	 * @param body the content to send in key/value pairs
	 * @return the response
	 * @throws Http.Error if the remote endpoint returns an error (http code 400+)
	 */
	public static Data post(String url, Data body) { return post(url, body, null, "POST"); }
	
	/**
	 * Fetches the specified resource using POST
	 * @param url the url
	 * @param body the content to send in key/value pairs
	 * @param headers the http request headers
	 * @return the response
	 * @throws Http.Error if the remote endpoint returns an error (http code 400+)
	 */
	public static Data post(String url, Data body, Data headers) { return post(url, body, headers, "POST"); }
	
	/**
	 * Fetches the specified resource using the provided method and include the parameters in the body of the request
	 * @param url the url
	 * @param body the content to send in key/value pairs
	 * @param headers the http request headers
	 * @param method the http method
	 * @return the response
	 * @throws Http.Error if the remote endpoint returns an error (http code 400+)
	 */
	public static Data post(String url, Data body, Data headers, String method) { return post(url, body, headers, method, 0); }
	
	/**
	 * Fetches the specified resource using the provided method and include the parameters in the body of the request
	 * @param url the url
	 * @param body the content to send in key/value pairs
	 * @param headers the http request headers
	 * @param method the http method
	 * @param timeout the request timeout in milliseconds. 0 means infinite.
	 * @return the response
	 * @throws Http.Error if the remote endpoint returns an error (http code 400+)
	 */
	public static Data post(String url, Data body, Data headers, String method, int timeout) { return post(url, body, headers, method, timeout, null); }
	
	/**
	 * Fetches the specified resource using the provided method and include the parameters in the body of the request
	 * @param url the url
	 * @param body the content to send in key/value pairs
	 * @param headers the http request headers
	 * @param method the http method
	 * @param timeout the request timeout in milliseconds. 0 means infinite.
	 * @param context the SSL context to be used (can be null for permissive defaults)
	 * @return the response
	 * @throws Http.Error if the remote endpoint returns an error (http code 400+)
	 */
	public static Data post(String url, Data body, Data headers, String method, int timeout, SSLContext context)
	{
		HttpURLConnection connection = null;
		
		try
		{
			if( url == null || url.isBlank() || (!url.startsWith("http://") && !url.startsWith("https://")) )
				throw new IllegalArgumentException("Invalid url");
			if( headers == null ) headers = Data.map(); 
			if( !headers.isMap() )
				throw new IllegalArgumentException("Invalid headers");
			if( body == null ) body = Data.empty();
			
			HttpURLConnection.setFollowRedirects(true);
			URL u = URI.create(url).toURL();
			connection = (HttpURLConnection)u.openConnection();
			connection.setRequestMethod(method);
			connection.setDoOutput(true);
			connection.setConnectTimeout(CONNECT_TIMEOUT);
			connection.setInstanceFollowRedirects(true);
			
			if( connection instanceof HttpsURLConnection )
			{
				if( context != null )
					((HttpsURLConnection)connection).setSSLSocketFactory(__bugfix(context.getSocketFactory(), u.getHost()));
				else
					((HttpsURLConnection)connection).setSSLSocketFactory(__bugfix(Network.sslContext(null, true).getSocketFactory(), u.getHost()));
				((HttpsURLConnection)connection).setHostnameVerifier((h, s) -> true);
			}
			
			for( Map.Entry<String, Data> h : headers.entrySet() )
				connection.setRequestProperty(h.getKey(), h.getValue().asString());
			connection.setRequestProperty("Connection", "close");
			
			ByteArrayOutputStream content = new ByteArrayOutputStream();
			switch(headers.asString("Content-Type") )
			{
				case "": // no content type specified
					connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
					// then fall through
				case "application/x-www-form-urlencoded":
					if( body.isMap() )
					{
						for( Map.Entry<String, Data> p : body.entrySet() )
						{
							content.write(URLEncoder.encode(p.getKey(), StandardCharsets.UTF_8).replace("+", "%20").getBytes(StandardCharsets.ISO_8859_1)); 
							content.write('='); 
							content.write(URLEncoder.encode(p.getValue().asString(), StandardCharsets.UTF_8).replace("+", "%20").getBytes(StandardCharsets.ISO_8859_1));
							content.write('&');
						}
					}
					else
						content.write(body.asString().getBytes(StandardCharsets.ISO_8859_1));
					break;
				case "multipart/form-data":
					connection.setRequestProperty("Content-Type", headers.asString("Content-Type") + ";boundary=\"a-e-o-n-i-c-s\"");
					if( body.isMap() )
					{
						for( Map.Entry<String, Data> p : body.entrySet() )
						{
							Data value = p.getValue();
							if( value.isMap() && value.containsKey("name") && value.containsKey("mime") && value.containsKey("content") )
							{
								// file upload
								content.write("--a-e-o-n-i-c-s\r\nContent-Disposition: form-data; name=\"".getBytes(StandardCharsets.ISO_8859_1));
								content.write(p.getKey().replaceAll("[\"\\r\\n\\0]", "").getBytes(StandardCharsets.ISO_8859_1));
								content.write("\"; filename=\"".getBytes(StandardCharsets.ISO_8859_1));
								content.write(value.asString("name").replaceAll("[\"\\r\\n\\0]", "").getBytes(StandardCharsets.ISO_8859_1));
								content.write("\"\r\nContent-Type: ".getBytes(StandardCharsets.ISO_8859_1));
								content.write(value.asString("mime").replaceAll("[\"\\r\\n\\0]", "").getBytes(StandardCharsets.ISO_8859_1));
								content.write("\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1));
								content.write(value.asString("content").getBytes(StandardCharsets.ISO_8859_1)); 
								content.write("\r\n".getBytes(StandardCharsets.ISO_8859_1));
							}
							else
							{
								// form data
								content.write("--a-e-o-n-i-c-s\r\nContent-Disposition: form-data; name=\"".getBytes(StandardCharsets.ISO_8859_1));
								content.write(p.getKey().replaceAll("[\"\\r\\n\\0]", "").getBytes(StandardCharsets.ISO_8859_1));
								content.write("\"\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1));
								content.write(value.asString().getBytes(StandardCharsets.ISO_8859_1)); 
								content.write("\r\n".getBytes(StandardCharsets.ISO_8859_1));
							}
						}
						content.write("--a-e-o-n-i-c-s--\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1));
					}
					else
						throw new IllegalArgumentException("Invalid multipart boundary");
					break;
				default:
					content.write(body.asString().getBytes(StandardCharsets.ISO_8859_1));
					break;
			}
			connection.setRequestProperty("Content-Length", "" + (content == null ? 0 : content.size()));
			
			try( OutputStream output = connection.getOutputStream() )
			{
				output.write(content.toByteArray());
			}
			
			int code = connection.getResponseCode();
			if( code < 0 || code >= 400 )
			{
				try( InputStream response = connection.getErrorStream() )
				{
					throw new Http.Error(code, (response == null ? null : new String(response.readAllBytes(), StandardCharsets.ISO_8859_1)));
				}
			}
			else
			{
				try( InputStream response = connection.getInputStream() )
				{
					if( Objects.requireNonNullElse(connection.getContentType(), "").startsWith("application/json") )
						return Json.decode(new String(response.readAllBytes(), StandardCharsets.ISO_8859_1));
					else
						return Data.of(new String(response.readAllBytes(), StandardCharsets.ISO_8859_1));
				}
			}
		}
		catch(Http.Error he) { throw he; }
		catch(Exception e)
		{
			throw new RuntimeException(e);
		}
		finally
		{
			if( connection != null )
				connection.disconnect();
		}
	}
	
	/**
	 * Fetches the specified resource using GET
	 * @param url the url
	 * @return the response
	 * @throws Http.Error if the remote endpoint returns an error (http code 400+)
	 */
	public static Data get(String url) { return get(url, null, null, "GET"); }
	
	/**
	 * Fetches the specified resource using GET
	 * @param url the url
	 * @param queryString the content to append to the url in key/value pairs
	 * @return the response
	 * @throws Http.Error if the remote endpoint returns an error (http code 400+)
	 */
	public static Data get(String url, Data queryString) { return get(url, queryString, null, "GET"); }
	
	/**
	 * Fetches the specified resource using GET
	 * @param url the url
	 * @param queryString the content to append to the url in key/value pairs
	 * @param headers the http request headers
	 * @return the response
	 * @throws Http.Error if the remote endpoint returns an error (http code 400+)
	 */
	public static Data get(String url, Data queryString, Data headers) { return get(url, queryString, headers, "GET"); }
	
	/**
	 * Fetches the specified resource using the provided method and include the parameters in the query string of the request
	 * @param url the url
	 * @param queryString the content to append to the url in key/value pairs
	 * @param headers the http request headers
	 * @param method the http method
	 * @return the response
	 * @throws Http.Error if the remote endpoint returns an error (http code 400+)
	 */
	public static Data get(String url, Data queryString, Data headers, String method) { return get(url, queryString, headers, method, 0); }
	
	/**
	 * Fetches the specified resource using the provided method and include the parameters in the query string of the request
	 * @param url the url
	 * @param queryString the content to append to the url in key/value pairs
	 * @param headers the http request headers
	 * @param method the http method
	 * @param timeout the request timeout in milliseconds. 0 means infinite.
	 * @return the response
	 * @throws Http.Error if the remote endpoint returns an error (http code 400+)
	 */
	public static Data get(String url, Data queryString, Data headers, String method, int timeout) { return get(url, queryString, headers, method, timeout, null); }
	
	/**
	 * Fetches the specified resource using the provided method and include the parameters in the query string of the request
	 * @param url the url
	 * @param queryString the content to append to the url in key/value pairs
	 * @param headers the http request headers
	 * @param method the http method
	 * @param timeout the request timeout in milliseconds. 0 means infinite.
	 * @param context the SSL context to be used (can be null for permissive defaults)
	 * @return the response
	 * @throws Http.Error if the remote endpoint returns an error (http code 400+)
	 */
	public static Data get(String url, Data queryString, Data headers, String method, int timeout, SSLContext context)
	{
		HttpURLConnection connection = null;
		
		try
		{
			if( url == null || url.isBlank() || (!url.startsWith("http://") && !url.startsWith("https://")) )
				throw new IllegalArgumentException("Invalid url");
			if( headers == null ) headers = Data.map(); 
			if( !headers.isMap() )
				throw new IllegalArgumentException("Invalid headers");
			if( queryString == null ) queryString = Data.empty();
			
			StringBuilder query = null;
			if( queryString.isMap() )
			{
				query = new StringBuilder();
				for( Map.Entry<String, Data> p : queryString.entrySet() )
				{
					query.append(
						URLEncoder.encode(p.getKey(), StandardCharsets.UTF_8).replace("+", "%20") + 
						"=" + 
						URLEncoder.encode(p.getValue().asString(), StandardCharsets.UTF_8).replace("+", "%20") + 
						"&");
				}
			}
			else
				query = new StringBuilder(queryString.asString());
			
			HttpURLConnection.setFollowRedirects(true);
			URL u = URI.create(url + (url.indexOf('?') > 0 ? "&" : "?") + query.toString()).toURL();

			connection = (HttpURLConnection)u.openConnection();
			connection.setRequestMethod(method);
			connection.setConnectTimeout(Math.max(timeout, CONNECT_TIMEOUT));
			if( timeout > 0 ) connection.setReadTimeout(timeout);
			connection.setInstanceFollowRedirects(true);
			
			if( connection instanceof HttpsURLConnection )
			{
				if( context != null )
					((HttpsURLConnection)connection).setSSLSocketFactory(__bugfix(context.getSocketFactory(), u.getHost()));
				else
					((HttpsURLConnection)connection).setSSLSocketFactory(__bugfix(Network.sslContext(null, true).getSocketFactory(), u.getHost()));
				((HttpsURLConnection)connection).setHostnameVerifier((h, s) -> true);
			}
			
			for( Map.Entry<String, Data> h : headers.entrySet() )
				connection.setRequestProperty(h.getKey(), h.getValue().asString());
			connection.setRequestProperty("Connection", "close");
			
			int code = connection.getResponseCode();
			if( code < 0 || code >= 400 )
			{
				try( InputStream response = connection.getErrorStream() )
				{
					throw new Http.Error(code, (response == null ? null : new String(response.readAllBytes(), StandardCharsets.ISO_8859_1)));
				}
			}
			else
			{
				try( InputStream response = connection.getInputStream() )
				{
					if( Objects.requireNonNullElse(connection.getContentType(), "").startsWith("application/json") )
						return Json.decode(new String(response.readAllBytes(), StandardCharsets.ISO_8859_1));
					else
						return Data.of(new String(response.readAllBytes(), StandardCharsets.ISO_8859_1));
				}
			}
		}
		catch(Http.Error he) { throw he; }
		catch(Exception e)
		{
			throw new RuntimeException(e);
		}
		finally
		{
			if( connection != null )
				connection.disconnect();
		}
	}
}
