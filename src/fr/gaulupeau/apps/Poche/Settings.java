package fr.gaulupeau.apps.Poche;

import static fr.gaulupeau.apps.Poche.Helpers.PREFS_NAME;
import fr.gaulupeau.apps.InThePoche.R;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class Settings extends Activity {
	Button btnDone;
	EditText editPocheUrl;
	EditText editAPIUsername;
	EditText editAPIPassword;
	EditText editAPIToken;
	EditText editGlobalToken;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings);
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String pocheUrl = settings.getString("pocheUrl", "http://");
        String apiUsername = settings.getString("APIUsername", "");
        String apiPassword = settings.getString("APIPassword", "");
        String apiToken = settings.getString("APIToken", "");
    	editPocheUrl = (EditText)findViewById(R.id.pocheUrl);
    	editPocheUrl.setText(pocheUrl);
    	editAPIUsername = (EditText)findViewById(R.id.APIUsername);
    	editAPIUsername.setText(apiUsername);
    	editAPIPassword = (EditText)findViewById(R.id.APIPassword);
    	editAPIPassword.setText(apiPassword);
    	editAPIToken = (EditText)findViewById(R.id.APIToken);
    	editAPIToken.setText(apiToken);
        btnDone = (Button)findViewById(R.id.btnDone);
        btnDone.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
	        	SharedPreferences.Editor editor = settings.edit();
	        	editor.putString("pocheUrl", editPocheUrl.getText().toString());
	        	editor.putString("APIUsername", editAPIUsername.getText().toString());
	        	editor.putString("APIPassword", editAPIPassword.getText().toString());
	        	editor.putString("APIToken", editAPIToken.getText().toString());
				editor.commit();
				finish();
			}
        });
	}
}
