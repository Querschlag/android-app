/**
 * Android to Poche
 * A simple app to make the full save bookmark to Poche
 * web page available via the Share menu on Android tablets
 * @author GAULUPEAU Jonathan
 * August 2013
 */

package fr.gaulupeau.apps.Poche;

import static fr.gaulupeau.apps.Poche.ArticlesSQLiteOpenHelper.ARCHIVE;
import static fr.gaulupeau.apps.Poche.ArticlesSQLiteOpenHelper.ARTICLE_CONTENT;
import static fr.gaulupeau.apps.Poche.ArticlesSQLiteOpenHelper.ARTICLE_DATE;
import static fr.gaulupeau.apps.Poche.ArticlesSQLiteOpenHelper.ARTICLE_SYNC;
import static fr.gaulupeau.apps.Poche.ArticlesSQLiteOpenHelper.ARTICLE_TABLE;
import static fr.gaulupeau.apps.Poche.ArticlesSQLiteOpenHelper.ARTICLE_TITLE;
import static fr.gaulupeau.apps.Poche.ArticlesSQLiteOpenHelper.ARTICLE_URL;
import static fr.gaulupeau.apps.Poche.Helpers.PREFS_NAME;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import fr.gaulupeau.apps.InThePoche.R;



/**
 * Main activity class
 */
@TargetApi(Build.VERSION_CODES.FROYO) public class Poche extends Activity {
	private static SQLiteDatabase database;
	Button btnDone;
	Button btnGetPost;
	Button btnSync;
	EditText editPocheUrl;
	SharedPreferences settings;
	static String apiUsername;
	static String apiPassword;
	static String apiUserID;
	static String apiToken;
	static String pocheUrl;
	String action;
	private BasicCookieStore cookieStore;
	private BasicHttpContext httpContext;
	  
	
    /** Called when the activity is first created. 
     * Will act differently depending on whether sharing or
     * displaying information page. */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        this.cookieStore = new BasicCookieStore();
        this.httpContext = new BasicHttpContext();
       
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        action = intent.getAction();

        getSettings();
        // Find out if Sharing or if app has been launched from icon
        if (action.equals(Intent.ACTION_SEND) && pocheUrl != "http://") {
        	setContentView(R.layout.main);
        	findViewById(R.id.btnSync).setVisibility(View.GONE);
        	findViewById(R.id.btnGetPost).setVisibility(View.GONE);
        	findViewById(R.id.progressBar1).setVisibility(View.VISIBLE);
        	final String pageUrl = extras.getString("android.intent.extra.TEXT");
        	// Vérification de la connectivité Internet
			final ConnectivityManager conMgr =  (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();
			if (activeNetwork != null && activeNetwork.isConnected()) {
				new Thread(new Runnable() {
					public void run() {
						performServerLogin(pocheUrl);
						addPageToPoche(pageUrl);
					}
				}).start();
				this.finish();
			 } else {
				 // Afficher alerte connectivité
				 showToast(getString(R.string.txtNetOffline));
			 }
        }
        else {
        	setContentView(R.layout.main);

            btnSync = (Button)findViewById(R.id.btnSync);
            btnSync.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					 // Vérification de la connectivité Internet
					 final ConnectivityManager conMgr =  (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
					 final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();
					 if (pocheUrl == "http://") {
						 showToast(getString(R.string.txtConfigNotSet));
					 } else if (activeNetwork != null && activeNetwork.isConnected()) {
						 // Exécution de la synchro en arrière-plan
						 new Thread(new Runnable() {
							 public void run() {
								 //pushRead();
								 parseRSS();
							 }
						 }).start();
					 } else {
						 // Afficher alerte connectivité
						 showToast(getString(R.string.txtNetOffline));
					 }
					
				}
			});
            
            btnGetPost = (Button)findViewById(R.id.btnGetPost);
            //updateUnread();
            
			btnGetPost.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					startActivity(new Intent(getBaseContext(), ListArticles.class));
				}
			});
			
        }
    }
    
    private void getSettings(){
        settings = getSharedPreferences(PREFS_NAME, 0);
        pocheUrl = settings.getString("pocheUrl", "http://");
        apiUsername = settings.getString("APIUsername", "");
        apiPassword = settings.getString("APIPassword", "");
        apiUserID = settings.getString("APIUserID", "");
        apiToken = settings.getString("APIToken", "");
    }
    
    @Override
    protected void onResume() {
    	super.onResume();
    	getSettings();
    	if (! action.equals(Intent.ACTION_SEND)){
    		updateUnread();
    	}
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.option, menu);
    	return true;
    } 

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    	case R.id.menuSettings:
    		startActivity(new Intent(getBaseContext(), Settings.class));
    	default:
    		return super.onOptionsItemSelected(item);
    	}
    }
    
    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	if (database != null) {
    		database.close();
		}
    }
    
    private void performServerLogin(final String serverUrl) {
    	if (serverUrl.startsWith("https") ) {
    		HostnameVerifier hostnameVerifier = org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;

    		// Set verifier     
    		HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier);
    	}
    	
		DefaultHttpClient httpclient = new DefaultHttpClient();
		HttpPost loginPost = new HttpPost(serverUrl + "?login");
		
		try {
		    // Add login credentials
		    List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
		    nameValuePairs.add(new BasicNameValuePair("login", apiUsername));
		    nameValuePairs.add(new BasicNameValuePair("password", apiPassword));
		    loginPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
		    
		    // Execute HTTP Post Request
		    HttpResponse response = httpclient.execute(loginPost);
		    int statusCode = response.getStatusLine().getStatusCode();
		    
		    if (statusCode == HttpStatus.SC_NOT_MODIFIED ||
		    	statusCode == HttpStatus.SC_OK) {
		    	System.out.println("login successful");
		    }
		    
		    HttpEntity entity = response.getEntity();
		    
		    if (entity != null) {
		    	entity.consumeContent();
		    }
		    
		    List<Cookie> cookies = httpclient.getCookieStore().getCookies();
		    
		    if (cookies.isEmpty()) {
		    	System.out.println("No cookies found in response!");
		    } else {
		    	for (int i = 0; i < cookies.size(); i++) {
		    		Cookie cookie = cookies.get(i);
		    		System.out.println(cookie.getName());
		    		if (cookie.getName().equals("PHPSESSID")) {
		    			// save retrieved cookie
//		    			System.out.println("Saving login cookie");
		    			this.cookieStore.getCookies().clear();
		    			this.cookieStore.addCookie(cookie);
		    		}
		    		System.out.println("- " + cookies.get(i).toString());
                }
		    }
		    
		} catch (ClientProtocolException e) {
		    httpclient.getConnectionManager().shutdown();
		    e.printStackTrace();
		} catch (IOException e) {
			httpclient.getConnectionManager().shutdown();
		    e.printStackTrace();
		} finally {
			httpclient.getConnectionManager().shutdown();
		}
    }
    
    private void addPageToPoche(final String pageUrl) {
		// Start to build the poche URL
		Uri.Builder pocheSaveUrl = Uri.parse(pocheUrl+"/").buildUpon();
		// Add the parameters from the call
		pocheSaveUrl.appendQueryParameter("action", "add");
		byte[] data = null;
		try {
			data = pageUrl.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		String base64 = Base64.encodeToString(data, Base64.DEFAULT);
		pocheSaveUrl.appendQueryParameter("url", base64);
//		System.out.println("request : " + pocheSaveUrl.build().toString());
		
		DefaultHttpClient httpclient = new DefaultHttpClient();
		
		if (pocheUrl.startsWith("https") ) {
			System.out.println("Trust everyone");
			trustEveryone();
		}
		
		if (this.cookieStore != null && this.cookieStore.getCookies().size() > 0) {
			System.out.println("Adding cookie to request! cookies: " + this.cookieStore.getCookies().size());
			for (Cookie cookie : this.cookieStore.getCookies()) {
//				System.out.println("- " + cookie.toString());
				httpclient.getCookieStore().addCookie(cookie);
			}
			this.httpContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
		} else {
			System.out.println("No cookie available to add to request!");
		}
		
		HttpGet getRequest = new HttpGet(pocheSaveUrl.build().toString());
		
		try {
			HttpResponse response = httpclient.execute(getRequest);
		    int statusCode = response.getStatusLine().getStatusCode();
		    
		    if (statusCode == HttpStatus.SC_NOT_MODIFIED ||
		    	statusCode == HttpStatus.SC_OK) {
		    	showToast(getString(R.string.txtAddPageDone));
		    } else {
		    	showToast(getString(R.string.txtAddPageFailed));
		    }
		        
		} catch (ClientProtocolException e) {
		    httpclient.getConnectionManager().shutdown();
		    e.printStackTrace();
		    showToast(getString(R.string.txtAddPageFailed));
		} catch (IOException e) {
			httpclient.getConnectionManager().shutdown();
		    e.printStackTrace();
		    showToast(getString(R.string.txtAddPageFailed));
		} finally {
			httpclient.getConnectionManager().shutdown();
		}
}
    
    private void updateUnread(){
    	runOnUiThread(new Runnable() {
    		public void run()
    		{
    			ArticlesSQLiteOpenHelper helper = new ArticlesSQLiteOpenHelper(getApplicationContext());
    			database = helper.getReadableDatabase();
    			int news = database.query(ARTICLE_TABLE, null, ARCHIVE + "=0", null, null, null, null).getCount();
    			btnGetPost.setText(getString(R.string.btnGetPost) + " - " + news + " unread");
    		}
    	});
    }
    
    public void showToast(final String toast)
    {
    	runOnUiThread(new Runnable() {
    		public void run()
    		{
    			Toast.makeText(Poche.this, toast, Toast.LENGTH_SHORT).show();
    		}
    	});
    }
    
    
//    public void pocheIt(String url){
//    	String id ="req-001";
//    	JSONRPC2Request reqOut = null;
//    	try{
//    		reqOut = JSONRPC2Request.parse("{\"jsonrpc\":\"2.0\",\"method\":\"item.add\",\"id\":\"" + id + "\",\"params\":[{\"username\":\""+ apiUsername + "\",\"api_token\":\""+ apiToken +"\"}, \"" + url + "\", true]}");
//    		System.err.println(reqOut.toString());
//    		JSONRPC2Response response = sendRequest(reqOut);
//    		if (response.indicatesSuccess()) {
//    			showToast(getString(R.string.txtSyncDone));
//    		}
//    	} catch (JSONRPC2ParseException e2) {
//    		e2.printStackTrace();
//    		showToast(getString(R.string.txtSyncFailed));
//    	}
//    	finish();
//    }
    
    
//    public void pushRead(){
//    	JSONRPC2Request reqOut = null;
//    	String filter = ARCHIVE + "=1 AND " + ARTICLE_SYNC + "=0";
//    	String[] getStrColumns = new String[] {ARTICLE_ID};
//		Cursor ac = database.query(
//				ARTICLE_TABLE,
//				getStrColumns,
//				filter, null, null, null, null);
//		ac.moveToFirst();
//		if(!ac.isAfterLast()) {
//			do {
//				String article_id = ac.getString(0);
//				String id ="req-001";
//				try{
//		    		reqOut = JSONRPC2Request.parse("{\"jsonrpc\":\"2.0\",\"method\":\"item.mark_as_read\",\"id\":\"" + id + "\",\"params\":[{\"username\":\""+ apiUsername + "\",\"api_token\":\""+ apiToken +"\"}, " + article_id + "]}");
//		    		System.err.println(reqOut.toString());
//		    		JSONRPC2Response response = sendRequest(reqOut);
//		    		if (response.indicatesSuccess()) {
//		    			ContentValues values = new ContentValues();
//		    			values.put(ARTICLE_SYNC, 1);
//						database.update(ARTICLE_TABLE, values, ARTICLE_ID + "=" + article_id, null);
//					}
//		    	} catch (JSONRPC2ParseException e2) {
//		    		e2.printStackTrace();
//		    	}
//			} while (ac.moveToNext());
//		}
//		ac.close();
//    	
//    }

    
    public String cleanString(String s){
    	
    	s = s.replace("&Atilde;&copy;", "&eacute;");
    	s = s.replace("&Atilde;&uml;", "&egrave;");
    	s = s.replace("&Atilde;&ordf;", "&ecirc;");
    	s = s.replace("&Atilde;&laquo;", "&euml;");
    	s = s.replace("&Atilde;&nbsp;", "&agrave;");
    	s = s.replace("&Atilde;&curren;", "&auml;");
    	s = s.replace("&Atilde;&cent;", "&acirc;");
    	s = s.replace("&Atilde;&sup1;", "&ugrave;");
    	s = s.replace("&Atilde;&raquo;", "&ucirc;");
    	s = s.replace("&Atilde;&frac14;", "&uuml;");
    	s = s.replace("&Atilde;&acute;", "&ocirc;");
    	s = s.replace("&Atilde;&para;", "&ouml;");
    	s = s.replace("&Atilde;&reg;", "&icirc;");
    	s = s.replace("&Atilde;&macr;", "&iuml;");
    	s = s.replace("&Atilde;&sect;", "&ccedil;");
    	s = s.replace("&amp;", "&amp;");	
    	return s;
    }
    
    
    private void trustEveryone() {
    	try {
    		HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier(){
        			public boolean verify(String hostname, SSLSession session) {
        				return true;
        			}});
    		SSLContext context = SSLContext.getInstance("TLS");
    		context.init(null, new X509TrustManager[]{new X509TrustManager(){
    			public void checkClientTrusted(X509Certificate[] chain,
    					String authType) throws CertificateException {}
    			public void checkServerTrusted(X509Certificate[] chain,
    					String authType) throws CertificateException {}
    			public X509Certificate[] getAcceptedIssuers() {
    				return new X509Certificate[0];
    			}}}, new SecureRandom());
    		HttpsURLConnection.setDefaultSSLSocketFactory(
    				context.getSocketFactory());
    	} catch (Exception e) { // should never happen
    		e.printStackTrace();
    	}
    }
    
    
    public void parseRSS(){

    	URL url;
    	try
    	{
    		// Set the url (you will need to change this to your RSS URL
    		url = new URL(pocheUrl + "/?feed&type=home&user_id=" + apiUserID + "&token=" + apiToken );
    		// Setup the connection
    		HttpsURLConnection conn_s = null;
    		HttpURLConnection conn = null;
    		if (pocheUrl.startsWith("https") ) {
    			trustEveryone();
    			conn_s = (HttpsURLConnection) url.openConnection();
    		}else{
    			conn = (HttpURLConnection) url.openConnection();
    		}
    		
    		if (
    				((conn != null) && (conn.getResponseCode() == HttpURLConnection.HTTP_OK)) 
    			|| ((conn_s != null) && (conn_s.getResponseCode() == HttpURLConnection.HTTP_OK))
    			)
    		{

    			// Retreive the XML from the URL
    			DocumentBuilderFactory dbf = DocumentBuilderFactory
    					.newInstance();
    			DocumentBuilder db = dbf.newDocumentBuilder();
    			Document doc;
//    			doc = db.parse(url.openStream());
    			InputSource is = new InputSource(
				        new InputStreamReader(
				                url.openStream()));
    			doc = db.parse(is);
//    			doc = db.parse(
//    				    new InputSource(
//    				        new InputStreamReader(
//    				                url.openStream(),
//    				                "latin-1")));
    			doc.getDocumentElement().normalize();
    			
    			// This is the root node of each section you want to parse
    			NodeList itemLst = doc.getElementsByTagName("item");

    			// This sets up some arrays to hold the data parsed
    			arrays.PodcastTitle = new String[itemLst.getLength()];
    			arrays.PodcastURL = new String[itemLst.getLength()];
    			arrays.PodcastContent = new String[itemLst.getLength()];
    			arrays.PodcastMedia = new String[itemLst.getLength()];
    			arrays.PodcastDate = new String[itemLst.getLength()];

    			// Loop through the XML passing the data to the arrays
    			for (int i = 0; i < itemLst.getLength(); i++)
    			{

    				Node item = itemLst.item(i);
    				if (item.getNodeType() == Node.ELEMENT_NODE)
    				{
    					Element ielem = (Element) item;

    					// This section gets the elements from the XML
    					// that we want to use you will need to add
    					// and remove elements that you want / don't want
    					NodeList title = ielem.getElementsByTagName("title");
    					NodeList link = ielem.getElementsByTagName("link");
    					NodeList date = ielem.getElementsByTagName("pubDate");
    					NodeList content = ielem
    							.getElementsByTagName("description");
    					//NodeList media = ielem
    					//		.getElementsByTagName("media:content");

    					// This is an attribute of an element so I create
    					// a string to make it easier to use
    					//String mediaurl = media.item(0).getAttributes()
    					//		.getNamedItem("url").getNodeValue();

    					// This section adds an entry to the arrays with the
    					// data retrieved from above. I have surrounded each
    					// with try/catch just incase the element does not
    					// exist
    					try
    					{
    						arrays.PodcastTitle[i] = cleanString(title.item(0).getChildNodes().item(0).getNodeValue());
    					} catch (NullPointerException e)
    					{
    						e.printStackTrace();
    						arrays.PodcastTitle[i] = "Echec";
    					}
    					try {
							arrays.PodcastDate[i] = date.item(0).getChildNodes().item(0).getNodeValue();
						} catch (NullPointerException e) {
							e.printStackTrace();
    						arrays.PodcastDate[i] = null;
						}
    					try
    					{
    						arrays.PodcastURL[i] = link.item(0).getChildNodes()
    								.item(0).getNodeValue();
    					} catch (NullPointerException e)
    					{
    						e.printStackTrace();
    						arrays.PodcastURL[i] = "Echec";
    					}
    					try
    					{
    						arrays.PodcastContent[i] = content.item(0)
    								.getChildNodes().item(0).getNodeValue();
    					} catch (NullPointerException e)
    					{
    						e.printStackTrace();
    						arrays.PodcastContent[i] = "Echec";
    					}
    					
    					ContentValues values = new ContentValues();
    					values.put(ARTICLE_TITLE, Html.fromHtml(arrays.PodcastTitle[i]).toString());
        				values.put(ARTICLE_CONTENT, Html.fromHtml(arrays.PodcastContent[i]).toString());
        				//values.put(ARTICLE_ID, Html.fromHtml(article.getString("id")).toString());
        				values.put(ARTICLE_URL, Html.fromHtml(arrays.PodcastURL[i]).toString());
        				values.put(ARTICLE_DATE, arrays.PodcastDate[i]);
        				values.put(ARCHIVE, 0);
        				values.put(ARTICLE_SYNC, 0);
        				try {
        					database.insertOrThrow(ARTICLE_TABLE, null, values);
        				} catch (SQLiteConstraintException e) {
        					continue;
        				} catch (SQLiteException e) {
        				database.execSQL("ALTER TABLE " + ARTICLE_TABLE + " ADD COLUMN " + ARTICLE_DATE + " datetime;");
        				database.insertOrThrow(ARTICLE_TABLE, null, values);
    					}
    				}
    			}
    			
    		}
			showToast(getString(R.string.txtSyncDone));
    		updateUnread();
    	} catch (MalformedURLException e)
    	{
    		e.printStackTrace();
    	} catch (DOMException e)
    	{
    		e.printStackTrace();
    	} catch (IOException e)
    	{
    		e.printStackTrace();
    	} catch (ParserConfigurationException e)
    	{
    		e.printStackTrace();
    	} catch (SAXException e)
    	{
    		e.printStackTrace();
    	} catch (Exception e) {
			e.printStackTrace();
		}
    	

    }
    
//    public void fetchUnread(){
//    	String id = "req-001";
//    	JSONRPC2Request reqOut = null;
//    	try {
//    		// POCHE A LINK
//    		//reqOut = JSONRPC2Request.parse("{\"jsonrpc\":\"2.0\",\"method\":\"item.add\",\"id\":\"req-001\",\"params\":[{\"username\":\"poche\",\"api_token\":\"cPG2urVgA+ToMXY\"},\"http://cdetc.fr\",true]}");
//    		// GET A LINK
//    		//reqOut = JSONRPC2Request.parse("{\"jsonrpc\":\"2.0\",\"method\":\"item.info\",\"id\":\"" + id + "\",\"params\":[{\"username\":\""+ apiUsername + "\",\"api_token\":\""+ apiToken +"\"}, 1]}");
//    		// GET ALL UNREAD
//    		reqOut = JSONRPC2Request.parse("{\"jsonrpc\":\"2.0\",\"method\":\"item.list_unread\",\"id\":\"" + id + "\",\"params\":[{\"username\":\""+ apiUsername + "\",\"api_token\":\""+ apiToken +"\"}, null, null]}");
//    		System.err.println(reqOut.toString());
//    	} catch (JSONRPC2ParseException e2) {
//    		e2.printStackTrace();
//    	}
//    	System.out.println(reqOut.toString());
//    	URL url = null;
//    	try {
//    		final String rpcuser ="api_user";
//    		final String rpcpassword = globalToken;
//
//    		Authenticator.setDefault(new Authenticator() {
//    			protected PasswordAuthentication getPasswordAuthentication() {
//    				return new PasswordAuthentication (rpcuser, rpcpassword.toCharArray());
//    			}});
//    		url = new URL(pocheUrl + "/jsonrpc.php");
//    	} catch (MalformedURLException e1) {
//    		e1.printStackTrace();
//    	}
//    	JSONRPC2Session session = new JSONRPC2Session(url);
//    	JSONRPC2Response response = null;
//    	try{
//    		response = session.send(reqOut);
//    	} catch (JSONRPC2SessionException e) {
//
//    		System.err.println(e.getMessage());
//    	}
//    	if (response.indicatesSuccess()){
//    		JSONObject article = null;
//    		ContentValues values = new ContentValues();
//    		try {
//    			JSONArray ret = new JSONArray(response.getResult().toString());
//    			for (int i = 0; i < ret.length(); i++) {
//    				article = ret.getJSONObject(i);
//    				values.put(ARTICLE_TITLE, Html.fromHtml(article.getString("title")).toString());
//    				values.put(ARTICLE_CONTENT, Html.fromHtml(article.getString("content")).toString());
//    				values.put(ARTICLE_ID, Html.fromHtml(article.getString("id")).toString());
//    				values.put(ARTICLE_URL, Html.fromHtml(article.getString("url")).toString());
//    				values.put(ARCHIVE, 0);
//    				values.put(ARTICLE_SYNC, 0);
//    				try {
//    					database.insertOrThrow(ARTICLE_TABLE, null, values);
//    				} catch (SQLiteConstraintException e) {
//    					continue;
//    				}
//    			}
//    		} catch (JSONException e) {
//    			e.printStackTrace();
//    			showToast(getString(R.string.txtSyncFailed));
//    		}
//
//    		showToast(getString(R.string.txtSyncDone));
//    		updateUnread();
//    	}else{
//    		System.out.println(response.getError().getMessage( ));
//    		showToast(getString(R.string.txtSyncFailed));
//    	}
//    }
//    
//    public JSONRPC2Response sendRequest(JSONRPC2Request reqOut){
//    	URL url = null;
//    	try {
//    		final String rpcuser ="api_user";
//    		final String rpcpassword = globalToken;
//
//    		Authenticator.setDefault(new Authenticator() {
//    			protected PasswordAuthentication getPasswordAuthentication() {
//    				return new PasswordAuthentication (rpcuser, rpcpassword.toCharArray());
//    			}});
//    		url = new URL(pocheUrl + "/jsonrpc.php");
//    	} catch (MalformedURLException e1) {
//    		e1.printStackTrace();
//    	}
//    	JSONRPC2Session session = new JSONRPC2Session(url);
//    	JSONRPC2Response response = null;
//    	try{
//    		response = session.send(reqOut);
//    	} catch (JSONRPC2SessionException e) {
//
//    		System.err.println(e.getMessage());
//    	}
//    	return response;
//    }

}

