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

public class Player2Fragment extends Fragment {
    ListView lvGuessResultsP2;

    // List of [guess, correctPosCount, wrongPosCount, wrongDigit]
    public static ArrayList<String[]> player2GuessResults = new ArrayList<>();
    public static CustomAdapter adapterP2;

    public Player2Fragment() {
       super();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.player2_board, container, false);
        lvGuessResultsP2 = view.findViewById(R.id.lv_guesses_list_p2);

        adapterP2 = new CustomAdapter(getContext(), player2GuessResults);
        lvGuessResultsP2.setAdapter(adapterP2);

        return view;
    }
}