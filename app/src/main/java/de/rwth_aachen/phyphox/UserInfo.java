package de.rwth_aachen.phyphox;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class UserInfo extends AppCompatActivity {
    private EditText text1; // Name
    private EditText text2; // Age
    private EditText text3; // Gender
    private EditText text4; //Height
    private EditText text5; // Weight
    private EditText text6; // Nationality
    private EditText text7; // Job
    SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = this.getSharedPreferences("isFirstUse",MODE_PRIVATE);
        boolean isFirst = preferences.getBoolean("isFirstUse", true);
        Log.i("UserInfo", "get sharedpreference info");
        if (isFirst == true) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("isFirstUse", false);
            editor.commit();
        }
        else {
            startActivity(new Intent(this, ExperimentList.class));
            finish();
        }
        setContentView(R.layout.activity_user_info);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        text1 = (EditText)findViewById(R.id.editText);
        text2 = (EditText)findViewById(R.id.editText2);
        text3 = (EditText)findViewById(R.id.editText3);
        text4 = (EditText)findViewById(R.id.editText4);
        text5 = (EditText)findViewById(R.id.editText5);
        text6 = (EditText)findViewById(R.id.editText6);
        text7 = (EditText)findViewById(R.id.editText7);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        Button button = (Button)findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                String Name = text1.getText().toString();
                String Age = text2.getText().toString();
                String Gender = text3.getText().toString();
                String Height = text4.getText().toString();
                String Weight = text5.getText().toString();
                String Nationality = text6.getText().toString();
                String Job = text7.getText().toString();

                intent.putExtra("Name", Name);
                intent.putExtra("Age", Age);
                intent.putExtra("Gender", Gender);
                intent.putExtra("Height", Height);
                intent.putExtra("Weight", Weight);
                intent.putExtra("Nationality", Nationality);
                intent.putExtra("Job", Job);

                intent.setClass(UserInfo.this, ExperimentList.class);
                startActivity(intent);
            }
        });
    }


}
