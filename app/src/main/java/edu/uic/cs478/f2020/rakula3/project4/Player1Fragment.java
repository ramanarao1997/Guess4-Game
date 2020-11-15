package edu.uic.cs478.f2020.rakula3.project4;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class Player1Fragment extends Fragment {
    ListView lvGuessResultsP1;

    // List of [guess, correctPosCount, wrongPosCount, wrongDigit]
    public static ArrayList<String[]> player1GuessResults = new ArrayList<>();
    public static CustomAdapter adapterP1;

    public Player1Fragment() {
       super();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.player1_board, container, false);
        lvGuessResultsP1 = view.findViewById(R.id.lv_guesses_list_p1);

        adapterP1 = new CustomAdapter(getContext(), player1GuessResults);
        lvGuessResultsP1.setAdapter(adapterP1);

        return view;
    }
}