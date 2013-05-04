package nehru.apps;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class DCMinusMinusActivity extends Activity {
	
	EditText etIP;
	TextView tv;
	String server;
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        etIP = (EditText) findViewById(R.id.editText1);
        Button doneButton = (Button) findViewById(R.id.button1);
        
        
        doneButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				server = String.valueOf(etIP.getText());
				callSearchActivity();
				}
        });
    }
    
    void callSearchActivity() {

        Intent i = new Intent(this, SearchActivity.class);
		Bundle b = new Bundle();
		b.putString("server", server);
		i.putExtras(b);
		startActivity(i);
    }
}