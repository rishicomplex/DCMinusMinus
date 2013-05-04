package nehru.apps;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class SearchActivity extends Activity {
	private static final int REQUEST_PICK_FILE = 1;
	String server,fina;
	FTPClient ftp;
	Button go;
	int counter=0;
	EditText searchbar;	
	String[] resultArray = {}, namesArray = {};
	TextView downloadPercentTv, downloadSpeedTv;
	ProgressBar progressbar;
	double fileSize, sizeCompleted = 0; // in kB
	Handler progressHandler;
	File downloadFile;
	Dialog searchdialog, progressdialog;
	AlertDialog alertdialog;
	DownloadThread dt;
	boolean downloading = false;
	String str,currdir="",posdialog;
	String downloadDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/DCMMDownloads/";
	int mainflag=1,maincounter=0;
	String[] prevDir={};
	ListView lv;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list);
		counter++;
		if(counter<2){
		File downloadDirFile = new File(downloadDir);
		downloadDirFile.mkdirs();
		
		Bundle b = getIntent().getExtras();
		server = b.getString("server");
		}
		
		lv = (ListView) findViewById(R.id.list);
		lv.setTextFilterEnabled(true);
		lv.setAdapter(new ArrayAdapter<String>(this, R.layout.list_item, resultArray));
		
		lv.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if(!namesArray[0].contentEquals("No search results found")){
					if(mainflag==2){
				fileSize = Double.parseDouble(namesArray[position].substring(namesArray[position].lastIndexOf('-') + 2, namesArray[position].lastIndexOf('B') - 1));
				char c = namesArray[position].charAt(namesArray[position].lastIndexOf('B') - 1);
				if(c == 'M') fileSize *= 1024;
				else if(c != 'K') fileSize /= 1024;
				posdialog=resultArray[position];
				showDialog(4);		
				
				}
					else if(mainflag==1){						
						if(!resultArray[position].contentEquals("Nothing Here")){
						currdir=resultArray[position];						
						directoryview("viewer.php",currdir,position);}											
					}
				}
			}
		});
		lv.setOnItemLongClickListener(new OnItemLongClickListener(){			
			public boolean onItemLongClick(AdapterView<?> parent, View view,int position, long id) {
				// TODO Auto-generated method stub
				if(mainflag==2){
					currdir=resultArray[position]+"/";
					mainflag=1;
				if(currdir!="/"){
					try{
				currdir=currdir.substring(0,currdir.lastIndexOf("/"));			
				currdir=currdir.substring(0,currdir.lastIndexOf("/"));}
				catch(Exception e){currdir="";}
				currdir=currdir+"/";			
				directoryview("viewer.php",currdir,1);
				}}				
				return true;
			}
			
		});
		
		
		
		progressHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
            	super.handleMessage(msg);
            	
            	if(downloadFile.exists()) {
                	double temp = 0;
                	try {
                		temp = downloadFile.length();
                		temp /= 1024;
                	}
                	catch(Exception e) {
                		Toast.makeText(getBaseContext(), "exception thrown while getting file length", Toast.LENGTH_SHORT ).show();
                	}
                	double speed = (temp - sizeCompleted) / 1;
                	sizeCompleted = temp;
                	double fraction = sizeCompleted / fileSize;
                	if(fraction >= 1) {
                		
                		downloading = false;
                	}
                	else {
                		String percenttext = Math.round(fraction * 100) + "%     " + Math.round(sizeCompleted) + "/" + Math.round(fileSize) + "kB";
                		String speedtext = Math.round(speed) + "kB/s";
                		int progress = (int)(fraction * 100);
                		
                		progressbar.setProgress(progress);
                		downloadPercentTv.setText(percenttext);
                		downloadSpeedTv.setText(speedtext);
                		
                		progressHandler.sendEmptyMessageDelayed(0,  1000);
                		
                	}
                }
                else {
                	progressHandler.sendEmptyMessageDelayed(0, 100);
                }
            }
        };
        if(counter<2){
		ftp = new FTPClient();
		ftp.addProtocolCommandListener(new PrintCommandListener(
				new PrintWriter(System.out), true));

		try {
			int reply;
			ftp.connect(server, 21);
			Toast.makeText(getBaseContext(), "Connected to " + server + " on 21", Toast.LENGTH_SHORT ).show();

			// After connection attempt, you should check the reply code to
			// verify
			// success.
			reply = ftp.getReplyCode();

			if (!FTPReply.isPositiveCompletion(reply)) {
				ftp.disconnect();
				Toast.makeText(getBaseContext(), "ftp server refused connection", Toast.LENGTH_SHORT) .show();
				finish();
			}
		} catch (IOException e) {
			if (ftp.isConnected()) {
				try {
					ftp.disconnect();
				} catch (IOException f) {
					// do nothing
				}
			}
			Toast.makeText(getBaseContext(), "Could not connect to server",
					Toast.LENGTH_SHORT).show();
			finish();
		}
		try {
			if (!ftp.login("music", "91214")) {
				Toast.makeText(getBaseContext(), "Couldn't login apparently",
						Toast.LENGTH_SHORT).show();
				ftp.logout();
				finish();
			} else {
				Toast.makeText(getBaseContext(), "Logged in",
						Toast.LENGTH_SHORT).show();
				HttpClient client = new DefaultHttpClient();
				HttpGet request = new HttpGet("http://" + server
						+ ":7050/ads.php");
				HttpResponse response = client.execute(request);

				InputStream in = response.getEntity().getContent();
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(in));
				StringBuilder str = new StringBuilder();
				String line = null;
				while ((line = reader.readLine()) != null) {
					str.append(line);
				}
				in.close();
				String finals="",html = str.toString();
				char htmls[]=html.toCharArray();				
				for(int counter=0;counter<htmls.length;counter++){
					if(htmls[counter]=='\5') htmls[counter]='\n';
					finals=finals+htmls[counter];}
					fina=new String(finals);
					if(!fina.contains("aadirishsree")){
					showDialog(3);}
					currdir="/";
					directoryview("viewer.php",currdir,1);
				}				
				
			
		} catch (Exception e) {
			Toast.makeText(getBaseContext(), "login problem",
					Toast.LENGTH_SHORT).show();
		}

		try {
			ftp.setFileType(FTP.BINARY_FILE_TYPE);
			ftp.enterLocalPassiveMode();
		} catch (Exception e) {
		} finally {
		}}

	}
	
	public void directoryview(String loc,String filePath,int pos){
		String html;
		HttpClient httpclient = new DefaultHttpClient();
	    HttpPost httppost = new HttpPost("http://"+server+":7050/"+loc);

	    try {
	        // Add your data
	        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
	        nameValuePairs.add(new BasicNameValuePair("code", "91214"));
	        nameValuePairs.add(new BasicNameValuePair("key", filePath));
	        httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
	        

	        // Execute HTTP Post Request
	        HttpResponse response = httpclient.execute(httppost);
	        InputStream in = response.getEntity().getContent();
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(in));
			StringBuilder str = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				str.append(line);
			}
			in.close();
			html = str.toString();
			if(html.contains("lotteryaadirishsree")){
				currdir=currdir.substring(0,currdir.lastIndexOf("/"));			
				currdir=currdir.substring(0,currdir.lastIndexOf("/"));//wtf is this?
				currdir=currdir+"/";
				String size=namesArray[pos].substring(namesArray[pos].indexOf("\t")+1);
				if(size.contains("MB")){
					fileSize=1024*Double.valueOf(size.substring(0,size.indexOf("M")));					
				}
				else if(size.contains("KB")){
					fileSize=Double.valueOf(size.substring(0,size.indexOf("K")));
				}
				else{
					fileSize=Double.valueOf(size.substring(0,size.indexOf("B")))/1024;
				}
				String path=resultArray[pos].substring(0,resultArray[pos].lastIndexOf("/"));
				posdialog="."+path;
				showDialog(4);				
			}			
			else {
			resultArray = null;
			resultArray = html.split("<br/>");
			namesArray = new String[resultArray.length];
			boolean noresultflag = false;
			String[] temp1 = new String[2];
			for(int i = 0; i < namesArray.length; i++) {
				temp1 = resultArray[i].split("<size>");
				if (temp1 == null || temp1.length != 2) {
					noresultflag = true;
					continue;
				}				
				resultArray[i] = temp1[0];
				if(temp1[1].contentEquals("-1KB")) temp1[1]="";
				resultArray[i]=resultArray[i].substring(0,resultArray[i].length()-1);
				namesArray[i] = nameFromPath(resultArray[i]) + "  \t" + temp1[1];
				resultArray[i]=resultArray[i]+"/";				
			}
			if(noresultflag) {
				namesArray = new String[1];
				namesArray[0] = "Nothing Here";
			}
			fillList();}
	    } catch (ClientProtocolException e) {
	        // 
	    } catch (IOException e) {
	        // TODO Auto-generated catch block
	    }
	}
	
	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub		
		if(mainflag==1){							
			if(currdir!="/"){
				try{
			currdir=currdir.substring(0,currdir.lastIndexOf("/"));			
			currdir=currdir.substring(0,currdir.lastIndexOf("/"));}
			catch(Exception e){currdir="";}
			currdir=currdir+"/";			
			directoryview("viewer.php",currdir,1);}										
		}
		else{
			mainflag=1;
			directoryview("viewer.php",currdir,1);						
		}
	}



	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch(id) {
		case 1 : 
			searchdialog = new Dialog(this);

			searchdialog.setContentView(R.layout.search);
			searchdialog.setTitle("Search");

			searchbar = (EditText) searchdialog.findViewById(R.id.search);
			go = (Button) searchdialog.findViewById(R.id.button1);
			
			go.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					String searchtext = String.valueOf(searchbar.getText());
					dismissDialog(1);
					getSearchResults(searchtext);
					mainflag=2;
					fillList();
				}
			});
			
			return searchdialog;
			
		case 2 :
            progressdialog = new Dialog(this);
            
            progressdialog.setContentView(R.layout.progress);
            progressdialog.setTitle("Downloading "+fina);            
            progressbar = (ProgressBar) progressdialog.findViewById(R.id.progressBar1);
            downloadPercentTv = (TextView) progressdialog.findViewById(R.id.textView1);
            downloadSpeedTv = (TextView) progressdialog.findViewById(R.id.textView2);
            
            progressbar.setMax(100);
            
            return progressdialog;
            
		case 3:						
            return new AlertDialog.Builder(this).setMessage(fina)                            
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    	dismissDialog(3);
                        /* User clicked OK so do some stuff */                    	
                    }
                }).create();		
		case 4:			
			return new AlertDialog.Builder(this).setMessage("Are You Sure?")                            
	                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
	                    public void onClick(DialogInterface dialog, int whichButton) {	                    	
	                    	dismissDialog(4);
	                        /* User clicked OK so do some stuff */	                  
	                    	downloadFile(posdialog);
	                    }
	                }).create();
		default : return null;
		}
		
	}
	
	
	
	 @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		if(resultCode == RESULT_OK) {
            switch(requestCode) {
            case REQUEST_PICK_FILE:
                if(data.hasExtra(FilePickerActivity.EXTRA_FILE_PATH)) {
                    // Get the file path
                    File selectedFile = new File(data.getStringExtra(FilePickerActivity.EXTRA_FILE_PATH));
                    // Set the file path text view
                    String mFilePath=selectedFile.getAbsolutePath();  
                    //Now you have your selected file, You can do your additional requirement with file.
                    String name;
                    name=mFilePath.substring(mFilePath.lastIndexOf("/")+1);
                    Toast.makeText(getBaseContext(), "Upload Started",
        					Toast.LENGTH_SHORT).show();                      
                    uploading(selectedFile,name);
                }
            }
        }
    }
	 
	 void uploading(File filer,String name){
		 UploadThread ut=new UploadThread(filer,name);
		 ut.start();
	 }
	
	 public void postData(String filePath,String loc) {
		    // Create a new HttpClient and Post Header
		    HttpClient httpclient = new DefaultHttpClient();
		    HttpPost httppost = new HttpPost("http://"+server+":7050/"+loc);

		    try {
		        // Add your data
		        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
		        nameValuePairs.add(new BasicNameValuePair("code", "91214"));
		        nameValuePairs.add(new BasicNameValuePair("key", filePath));
		        httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

		        // Execute HTTP Post Request
		        HttpResponse response = httpclient.execute(httppost);
		        
		    } catch (ClientProtocolException e) {
		        // 
		    } catch (IOException e) {
		        // TODO Auto-generated catch block
		    }
		}

     

	 
	@Override
	    public boolean onCreateOptionsMenu(Menu menu) {
	        super.onCreateOptionsMenu(menu);
	        menu.add(0, 1, 0, "Upload File");
	        menu.add(0, 0, 0, "Search");
	        menu.add(0,2, 0, "Exit");
	        return true;
	    }
	    
	    @Override
	    public boolean onOptionsItemSelected(MenuItem item) {
	        switch (item.getItemId()) {
	            case 0:
	                showDialog(1);
	                return true;
	            case 1:
	            	Intent intent = new Intent(this, FilePickerActivity.class);
	            	startActivityForResult(intent, REQUEST_PICK_FILE);
	                return true;
	            case 2:
				try {
					ftp.logout();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	            	finish();
	        }
	        return super.onOptionsItemSelected(item);
	    }


	void downloadFile(String path) { // path is full path
		String downloadLoc = downloadDir + nameFromPath(path);
		downloadFile = new File(downloadLoc);
		downloading = true;
		fina=nameFromPath(path);
		showDialog(2);
		startHandlingProgress();
		
		dt = new DownloadThread(path, downloadLoc);
		dt.start();
		
		
		

	}

	String nameFromPath(String path) {
		return path.substring(path.lastIndexOf("/") + 1, path.length());
	}

	void getSearchResults(String searchtext) {
		String[] arr = searchtext.split(" ");
		int i;
		searchtext = "";
		for (i = 0; i < arr.length; i++) {
			searchtext += arr[i];
			if (!(i == arr.length - 1))
				searchtext += "+";
		}
		String html = "";
		try {
			HttpClient client = new DefaultHttpClient();
			HttpGet request = new HttpGet("http://" + server
					+ ":7050/finder.php?key=" + searchtext);
			HttpResponse response = client.execute(request);

			InputStream in = response.getEntity().getContent();
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(in));
			StringBuilder str = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				str.append(line);
			}
			in.close();
			html = str.toString();
			Toast.makeText(getBaseContext(), "html succeeded",
					Toast.LENGTH_SHORT).show();
		} catch (Exception e) {
			Toast.makeText(getBaseContext(), "could not get http response",
					Toast.LENGTH_SHORT).show();
			finish();
		}
		resultArray = null;
		resultArray = html.split("<br/>");
		namesArray = new String[resultArray.length];
		boolean noresultflag = false;
		String[] temp1 = new String[2];
		for(i = 0; i < namesArray.length; i++) {
			temp1 = resultArray[i].split("<size>");
			if (temp1 == null || temp1.length != 2) {
				noresultflag = true;
				continue;
			}
			resultArray[i] = temp1[0];
			namesArray[i] = nameFromPath(resultArray[i]) + " - " + temp1[1];
			
		}
		if(noresultflag) {
			namesArray = new String[1];
			namesArray[0] = "No search results found";
		}
	}

	void fillList() {		
		lv.setAdapter(new ArrayAdapter<String>(this, R.layout.list_item, namesArray));
	}
	
	void startHandlingProgress() {
		
		progressHandler.sendEmptyMessageDelayed(0, 100);
		
	}
	
	
	private class UploadThread extends Thread{
		
		File filer;
		String name;
		UploadThread(File filer2,String path2){
			filer=filer2;
			name=path2;
		}
		@Override
		public void run() {
			// TODO Auto-generated method stub
			try
			 {		     		     			 
			         ftp.enterLocalPassiveMode(); // important!		         
			         FileInputStream in = new FileInputStream(filer);			         
			         boolean result = ftp.storeFile("./Uploads/"+name, in);		         
			         in.close();
			         if (result){ toast("upload succeeded");
			        	 postData(name,"uploads.php");
			         }
			 }
			 catch (Exception e)
			 {
			     e.printStackTrace();
			     toast("upload failed");
			 }
		
		}
		public void toast(String s) {
        	str = s;
        	SearchActivity.this.runOnUiThread(new Runnable() {
        	    public void run() {
        	        Toast.makeText(SearchActivity.this, str, Toast.LENGTH_SHORT).show();
        	    }
        	});
        }
			
	}
	
	private class DownloadThread extends Thread {
		
        String downloadLoc, path;
        
        DownloadThread(String p, String d) {
            downloadLoc = d;
            path = p;
        }
        
        // Override the run() method that will be invoked automatically when 
        // the Thread starts.  Do the work required to update the progress bar on this
        // thread but send a message to the Handler on the main UI thread to actually
        // change the visual representation of the progress. In this example we count
        // the index total down to zero, so the horizontal progress bar will start full and
        // count down.
        
        @Override
        public void run() {
        	
        	try {
        		toast("download started");
    			FileOutputStream output;
    			output = new FileOutputStream(downloadLoc);
    			boolean status = ftp.retrieveFile(path, output);
                downloading = false;
    			if (status){
    				toast("download succeeded");
    			postData(path,"downloads.php");
    			}
    			else {
    				toast("download failed status = false");
    			}
    			dismissDialog(2);
        		sizeCompleted = 0;
    			output.close();
    		} catch (Exception e) {
    			toast("download failed exception thrown");
    			e.printStackTrace();
    			dismissDialog(2);
    			downloading = false;
    		}
        }
        
        public void toast(String s) {
        	str = s;
        	SearchActivity.this.runOnUiThread(new Runnable() {
        	    public void run() {
        	        Toast.makeText(SearchActivity.this, str, Toast.LENGTH_SHORT).show();
        	    }
        	});
        }
        
    }
}
