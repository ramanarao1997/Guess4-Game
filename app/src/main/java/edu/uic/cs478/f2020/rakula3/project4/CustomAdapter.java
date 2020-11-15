package edu.uic.cs478.f2020.rakula3.project4;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class CustomAdapter extends BaseAdapter {
    ArrayList<String[]> myData;
    Context context;
    LayoutInflater inflater;

    public CustomAdapter(Context applicationContext, ArrayList<String[]> data) {
        context = applicationContext;
        inflater = LayoutInflater.from(applicationContext);

        myData = data;
    }

    @Override
    public int getCount() {
        try {
            int size = myData.size();
            return size;
        } catch(NullPointerException ex) {
            return 0;
        }
    }

    @Override
    public Object getItem(int i) {
        return myData.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        view = inflater.inflate(R.layout.guess_result_item, null);

        TextView tvGuessSequence = view.findViewById(R.id.tv_guessed_sequence);
        TextView tvCorrectCount = view.findViewById(R.id.tv_correct_pos_count);
        TextView tvIncorrectCount = view.findViewById(R.id.tv_incorrect_pos_count);
        TextView tvWrongDigit = view.findViewById(R.id.tv_wrongly_guessed_digit);

        tvGuessSequence.setText(myData.get(i)[0]);
        tvCorrectCount.setText(myData.get(i)[1]);
        tvIncorrectCount.setText(myData.get(i)[2]);
        tvWrongDigit.setText(myData.get(i)[3]);

        view.setClickable(false);

        return view;
    }
}
