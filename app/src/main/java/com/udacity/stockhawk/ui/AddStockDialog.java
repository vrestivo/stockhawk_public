package com.udacity.stockhawk.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import com.udacity.stockhawk.R;

import butterknife.BindView;
import butterknife.ButterKnife;


public class AddStockDialog extends android.support.v4.app.DialogFragment {


    @BindView(R.id.dialog_stock)
    EditText stock;

    private MainFragment mainFragment;


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {


        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());


        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View custom = inflater.inflate(R.layout.add_stock_dialog, null);

        ButterKnife.bind(this, custom);


        InputFilter lengthLimit = new InputFilter.LengthFilter(5);


        /**
         * allows only caps, and filters out all other characters.
         * stock tickers are usually limited to 5 all caps characters
         * based on http://stackoverflow.com/questions/3349121/how-do-i-use-inputfilter-to-limit-characters-in-an-edittext-in-android
         */
        InputFilter capsOnly = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence charSequence, int i, int i1, Spanned spanned, int i2, int i3) {
                for (int c = i; c < i1; c++) {
                    if (!Character.isUpperCase(charSequence.charAt(c))) {
                        return "";
                    }
                }
                return null;
            }
        };

        InputFilter[] filters = {capsOnly, lengthLimit};

        stock.setFilters(filters);

        stock.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                addStock();
                return true;
            }
        });
        builder.setView(custom);

        builder.setMessage(getString(R.string.dialog_title));
        builder.setPositiveButton(getString(R.string.dialog_add),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        addStock();
                    }
                });
        builder.setNegativeButton(getString(R.string.dialog_cancel), null);

        Dialog dialog = builder.create();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }

        return dialog;
    }


    private void addStock() {
        mainFragment = (MainFragment) getActivity().getSupportFragmentManager().findFragmentByTag(MainActivity.MAIN_FRAGMENT_TAG);
        mainFragment.addStock(stock.getText().toString());

        dismissAllowingStateLoss();
    }


}
