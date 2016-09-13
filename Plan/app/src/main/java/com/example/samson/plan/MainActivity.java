package com.example.samson.plan;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.view.ContextThemeWrapper;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.TableLayout;
import android.widget.TableRow;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends AppCompatActivity {

    // Variables //////////////////////////////////////////////////////////////////////////////////

    //dip to pixel value
    private int px;

    //initial buttonAdd (titleButtonAdd)
    Button buttonAdd = null;

    private AddPlanClickListener planEvent = null;
    private AddActivityClickListener activityEvent = null;

    //used for programmatically generated ids
    private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);

    //used for debugging
    private static final String TAG = "message";

    // Methods ////////////////////////////////////////////////////////////////////////////////////
    //Android Activity cycle methods
    private void eventSeed() {
        buttonAdd = (Button) findViewById(R.id.titleButtonAdd);
        planEvent = new AddPlanClickListener();

        if (null == buttonAdd) {
            throw new AssertionError();
        }

        buttonAdd.setOnClickListener(planEvent);
    }

    //generates ids for programmatically generated views
    public static int generateViewId() {
        for (; ; ) {
            final int result = sNextGeneratedId.get();
            // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
            int newValue = result + 1;
            if (newValue > 0x00FFFFFF) newValue = 1; // Roll over to 1, not 0.
            if (sNextGeneratedId.compareAndSet(result, newValue)) {
                return result;
            }
        }
    }


    // Classes /////////////////////////////////////////////////////////////////////////////////////
    abstract class AddTableRowElement implements OnClickListener {
        protected ArrayList<Integer> recNum = new ArrayList<>();
        protected ArrayList<String> rec = new ArrayList<>();
        protected TableLayout table;
        protected AtomicBoolean showDelButton = new AtomicBoolean();
        protected String fName;

        public AddTableRowElement(int tableId, String fileName, int buttonMinId) {

            table = (TableLayout) findViewById(tableId);
            showDelButton.set(false);
            fName = fileName;

            Button buttonMin = (Button) findViewById(buttonMinId);

            /*action handler for showing delete buttons*/
            if (buttonMin != null) {

                buttonMin.setOnClickListener(
                        new OnClickListener() {
                            public void onClick(View V) {
                                showDelButton.set(!showDelButton.get());
                                for (int index = 0; index < recNum.size(); index++) {
                                    Button toChange = ((Button) findViewById(recNum.get(index)));
                                    if (toChange != null) {
                                        if (showDelButton.get()) {
                                            toChange.setVisibility(View.VISIBLE);
                                        } else {
                                            toChange.setVisibility(View.GONE);
                                        }
                                    }
                                }

                            }
                        }

                );

            }

            readFileData();

            for (int index = 0; index < rec.size(); index++) {
                implementTableRow(rec.get(0));
                rec.remove(0);
            }


        }

        protected abstract void implementTableRow(String tName);

        private boolean fileExists() {
            File file = getApplicationContext().getFileStreamPath(fName);
            return !(file == null || !file.exists());
        }


        private void readFileData() {

            if (fileExists()) {
                try {
                    InputStream inputStream = openFileInput(fName);
                    Scanner input = new Scanner(inputStream);
                    input.useDelimiter("\0");
                    while (input.hasNext()) {
                        rec.add(input.next());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

        }

        protected void writeFileData(){
            FileOutputStream outputStream;
            int index;

            try {
                outputStream = openFileOutput(fName, Context.MODE_PRIVATE);
                for (index = 0; index < rec.size(); index++) {
                    outputStream.write(rec.get(index).getBytes());
                    outputStream.write("\0".getBytes());
                }
                outputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        protected void deleteTableRow(int recordId, int rowId) {

            int recordIndex = recNum.indexOf(recordId);

            if (fileExists()) {
                try {
                    File dFile = new File(rec.get(recordIndex));
                    dFile.delete();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

            table.removeView(table.findViewById(rowId));
            rec.remove(recordIndex);
            recNum.remove(recordIndex);

        }

    }

    public class AddPlanClickListener extends AddTableRowElement {
        private boolean criticalSect = false;

        public AddPlanClickListener() {
            super(R.id.titleTable, "PlanSys", R.id.titleButtonMin);
        }

        public void createPlan() {

            if(!criticalSect){
                criticalSect = true;
                }

            else{
                return;
                }

            //popup window components
            LayoutInflater layoutInflater
                    = (LayoutInflater)getBaseContext().getSystemService(LAYOUT_INFLATER_SERVICE);
            View popupView = layoutInflater.inflate(R.layout.activity_popup, null);
            final PopupWindow pw = new PopupWindow(popupView,
                                                    ActionBar.LayoutParams.WRAP_CONTENT,
                                                    ActionBar.LayoutParams.WRAP_CONTENT);

            final EditText pET = (EditText) popupView.findViewById(R.id.pPlanName);
            Button pAccept = (Button)popupView.findViewById(R.id.pAccept);
            Button pDecline = (Button)popupView.findViewById(R.id.pDecline);

            //initialize popup window
            InputFilter filter = new InputFilter() {
                public CharSequence filter(CharSequence source, int start, int end,
                                           Spanned dest, int dStart, int dEnd) {
                    if (source.length() < 1) return null;
                    char last = source.charAt(source.length() - 1);
                    String reservedChars = "?:\"*|/\\<> ";
                    if (reservedChars.indexOf(last) > -1)
                        return source.subSequence(0, source.length() - 1);
                    return null;
                }
            };

            pET.setFilters(new InputFilter[]{filter});

            pAccept.setOnClickListener(
                    new OnClickListener() {
                        public void onClick(View V) {
                            if (rec.indexOf(pET.getText().toString()) == -1) {
                                implementTableRow(pET.getText().toString());
                                pw.dismiss();
                                criticalSect = false;
                                }
                            else{
                                pET.setText("");
                                pET.setHint("Plan name is already used");
                                }
                        }

                    }
            );

            pDecline.setOnClickListener(
                    new OnClickListener() {
                        public void onClick(View v) {
                            pw.dismiss();
                            criticalSect = false;
                        }
                    }
            );
            pw.setFocusable(true);
            pw.showAsDropDown(buttonAdd,0,0);

        }

        public void implementTableRow(String tName) {
            TableRow tr = new TableRow(getApplicationContext());

            Button activityLink = new Button(getApplicationContext());
            Button db = new Button(new ContextThemeWrapper(getApplicationContext(), R.style.redBox),
                                   null, 0);

            tr.setId(generateViewId());
            db.setId(generateViewId());

            final int delVal = db.getId();
            final int val = tr.getId();

            //store important data in lists
            recNum.add(delVal);
            rec.add(tName);

            //EditText element specifications and onClick event handler
            activityLink.setTextColor(Color.BLACK);
            activityLink.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);

            activityLink.setText(tName);

            activityLink.setOnClickListener(
                    new OnClickListener() {
                        public void onClick(View V) {
                            //change view
                            setContentView(R.layout.activity_main);

                            //start construct plan method//
                            Button mainButtonAdd = (Button) findViewById(R.id.buttonAdd);
                            activityEvent = new AddActivityClickListener(
                                    (((Button) V).getText().toString()));

                            if (null == mainButtonAdd) {
                                throw new AssertionError();
                            }

                            mainButtonAdd.setOnClickListener(activityEvent);
                            //end construct plan method//

                        }

                    }

            );

            //DeleteButton, boolean, and event handler element specs
            db.setText("X");

            if (showDelButton.get()) {
                db.setVisibility(View.VISIBLE);
            } else {
                db.setVisibility(View.GONE);
            }

            //delete field action handler
            db.setOnClickListener(
                    new OnClickListener() {
                        public void onClick(View V) {
                            deleteTableRow(delVal, val);
                        }

                    }
            );

            //add elements to table row
            tr.addView(activityLink, px * 12, px * 4);
            tr.addView(db);

            //add TableRow to table
            table.addView(tr);

        }

        public void onClick(View V) {
            createPlan();
        }
    }

    public class AddActivityClickListener extends AddTableRowElement {

        public AddActivityClickListener(String fileName) {
            super(R.id.activityTable, fileName+"User", R.id.buttonMin);
        }

        public void implementTableRow(String str) {
            //View element declaration and initialization
            TableRow tr = new TableRow(getApplicationContext());

            EditText et = new EditText(getApplicationContext());
            CheckBox cb = new CheckBox(getApplicationContext());
            Button db = new Button(new ContextThemeWrapper(getApplicationContext(),
                    R.style.redBox), null, 0);

            tr.setId(generateViewId());
            db.setId(generateViewId());

            final int delVal = db.getId();
            final int val = tr.getId();

            //store important data in lists
            recNum.add(db.getId());
            rec.add(str);

            //EditText element specifications and FocusChange event handler
            et.setTextColor(Color.BLACK);
            et.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);

            if (str.isEmpty()) {
                et.setHint(R.string.EnterHint);
            } else {
                et.setText(str);
            }

            et.setOnFocusChangeListener(
                    new OnFocusChangeListener() {
                        public void onFocusChange(View V, boolean bVal) {
                            rec.set(recNum.indexOf(delVal), ((EditText) V).getText().toString());
                        }

                    }
            );

            //CheckBox element specifications
            cb.setBackgroundColor(Color.BLUE);

            //DeleteButton, boolean, and event handler element specs
            db.setText("X");

            if (showDelButton.get()) {
                db.setVisibility(View.VISIBLE);
            } else {
                db.setVisibility(View.GONE);
            }

            //delete field action handler
            db.setOnClickListener(
                    new OnClickListener() {

                        public void onClick(View V) {

                            table.removeView(table.findViewById(val));
                            rec.remove(recNum.indexOf(delVal));
                            recNum.remove(recNum.indexOf(delVal));
                        }

                    }
            );

            //add elements to table row
            tr.addView(et, px * 12, px * 4);
            tr.addView(cb);
            tr.addView(db);

            //add TableRow to table
            table.addView(tr);
        }


        public void onClick(View V) {
            implementTableRow("");
        }

    }


    //Activity Cycle methods ///////////////////////////////////////////////////////////////////////
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //initial calls
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_title);
        Log.i(TAG, getString(R.string.onCreate));

        //variables
        Resources r = getResources();

        //determine dip to pixel ratio
        px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20,
                r.getDisplayMetrics());

        //seed program logic and event handling
        eventSeed();

    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i(TAG, "onStart");


    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
    }


    @Override
    public void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.i(TAG, "onStop");

        if(planEvent != null){
            planEvent.writeFileData();
            }

        if(activityEvent != null){
            activityEvent.writeFileData();
            }

    }
}
