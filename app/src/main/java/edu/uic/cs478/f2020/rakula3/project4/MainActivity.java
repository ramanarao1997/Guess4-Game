package edu.uic.cs478.f2020.rakula3.project4;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    TextView tvPlayer1Secret, tvPlayer2Secret;
    TextView tvGameStatus, tvGameResult;
    TextView tvPlayer1Round, tvPlayer2Round;
    Button btnStartGame;

    // secrets of players
    int[] player1Secret, player2Secret;

    // to make intelligent guesses based on feedback
    Integer[] allDigits = new Integer[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    List<Integer> p1GuessHelper = new ArrayList<>();
    List<Integer> p2GuessHelper = new ArrayList<>();

    // keep track of different permutations when all digits are correct
    HashSet<ArrayList<Integer>> p1CorrectDigitsPermutations = new HashSet<>();
    HashSet<ArrayList<Integer>> p2CorrectDigitsPermutations = new HashSet<>();

    // store correct digits of players guesses
    ArrayList<Integer> p1CorrectGuessedDigits = new ArrayList<>();
    ArrayList<Integer> p2CorrectGuessedDigits = new ArrayList<>();

    // Threads and handlers
    Thread p1Thread, p2Thread;
    Handler mainHandler, p1Handler, p2Handler;

    private Random random;
    androidx.fragment.app.FragmentManager fragmentManager;

    Player1Fragment player1Fragment;
    Player2Fragment player2Fragment;

    // constants
    private static int num_of_rounds = 20;
    private static int gameResult = -1;

    private static final int START_GAME = 1;
    private static final int MAKE_NEXT_GUESS = 2;
    private static final int HANDLE_OPPONENT_GUESS = 3;
    private static final int HANDLE_OPPONENT_RESPONSE = 4;
    private static final int UPDATE_UI_FOR_P1 = 5;
    private static final int UPDATE_UI_FOR_P2 = 6;
    private static final int GENERATE_SECRET_NUMBER = 7;
    private static final int GET_SECRET_FROM_P1 = 8;
    private static final int GET_SECRET_FROM_P2 = 9;
    private static final int DECIDE_SECRET_NUMBER = 10;

    private static final int NO_WRONG_DIGITS_TO_DISPLAY = -7;
    private static boolean NO_WRONG_DIGITS_BY_P1 = false;
    private static boolean NO_WRONG_DIGITS_BY_P2 = false;

    // keys for bundle
    private static final String GUESS_KEY = "GUESS_KEY";
    private static final String GUESS_RESPONSE_KEY = "GUESS_RESPONSE_KEY";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvPlayer1Secret = findViewById(R.id.tv_player1_secret);
        tvPlayer2Secret = findViewById(R.id.tv_player2_secret);

        tvPlayer1Round = findViewById(R.id.tv_player1_round);
        tvPlayer2Round = findViewById(R.id.tv_player2_round);

        tvGameResult = findViewById(R.id.tv_game_result);
        tvGameStatus = findViewById(R.id.tv_game_status);

        btnStartGame = findViewById(R.id.b_start_game);

        tvGameStatus.setText("Click start to start game");
        
        tvPlayer1Secret.setText(getString(R.string.secret_template, ""));
        tvPlayer2Secret.setText(getString(R.string.secret_template, ""));

        for(Integer digit : allDigits) {
            p1GuessHelper.add(digit);
            p2GuessHelper.add(digit);
        }

        random = new Random();

        player1Fragment = new Player1Fragment();
        player2Fragment = new Player2Fragment();

        fragmentManager = getSupportFragmentManager();
        androidx.fragment.app.FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fl_player1_guesses, player1Fragment);
        fragmentTransaction.replace(R.id.fl_player2_guesses, player2Fragment);
        fragmentTransaction.commit();
        fragmentManager.executePendingTransactions();

        mainHandler = new Handler(Looper.myLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                Message msgToSend;

                switch (msg.what) {
                    case START_GAME: {
                        Log.i("Main thread", "game started");

                        tvGameStatus.setText("Game has started");
                        tvGameResult.setBackgroundColor(getColor(R.color.gray));
                        tvGameResult.setText("No winner yet");

                        // p1 plays first turn
                        msgToSend = p1Handler.obtainMessage(MAKE_NEXT_GUESS);
                        p1Handler.sendMessage(msgToSend);
                        break;
                    }

                    case DECIDE_SECRET_NUMBER: {
                        msgToSend = p1Handler.obtainMessage(GENERATE_SECRET_NUMBER);
                        p1Handler.sendMessage(msgToSend);
                        break;
                    }

                    case GET_SECRET_FROM_P1: {
                        tvPlayer1Secret.setText(getString(R.string.secret_template, getIntArrayAsString(player1Secret)));

                        msgToSend = p2Handler.obtainMessage(GENERATE_SECRET_NUMBER);
                        p2Handler.sendMessage(msgToSend);
                        break;
                    }

                    case GET_SECRET_FROM_P2: {
                        tvPlayer2Secret.setText(getString(R.string.secret_template , getIntArrayAsString(player2Secret)));

                        // p1 plays first turn
                        msgToSend = mainHandler.obtainMessage(START_GAME);
                        mainHandler.sendMessage(msgToSend);
                        break;
                    }

                    case UPDATE_UI_FOR_P1: {
                        Log.i("Main thread", "updating ui for p1");

                        // update UI with data from P1
                        int[] guess = msg.getData().getIntArray(GUESS_KEY);
                        int[] response = msg.getData().getIntArray(GUESS_RESPONSE_KEY);

                        String [] result = new String[4];

                        result[0] = getIntArrayAsString(guess);
                        for(int i = 0; i < response.length; i++) {
                            if(i == 2 && response[2] == NO_WRONG_DIGITS_TO_DISPLAY)
                                result[i + 1] = "X";
                            else
                                result[i + 1] = String.valueOf(response[i]);
                        }

                        Player1Fragment.player1GuessResults.add(result);
                        Player1Fragment.adapterP1.notifyDataSetChanged();

                        tvPlayer1Round.setText(getString(R.string.round_template, 20 - num_of_rounds + 1));

                        // check for p1 win
                        if(gameResult == 1) {
                            tvGameStatus.setText("Game has finished!");
                            tvGameResult.setBackgroundColor(getColor(R.color.blue));
                            tvGameResult.setText("P1 wins!");
                            resetAllThreads();
                            break;
                        }

                        // Make player 2 think for 2 seconds before making next guess
                        p2Handler.post(() -> {
                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        });

                        // inform p2 to make next guess
                        msgToSend = p2Handler.obtainMessage(MAKE_NEXT_GUESS);
                        p2Handler.sendMessage(msgToSend);
                        break;
                    }

                    case UPDATE_UI_FOR_P2: {
                        Log.i("Main thread", "updating ui for p2");

                        // update UI with data from P2
                        int[] guess = msg.getData().getIntArray(GUESS_KEY);
                        int[] response = msg.getData().getIntArray(GUESS_RESPONSE_KEY);

                        String [] result = new String[4];

                        result[0] = getIntArrayAsString(guess);
                        for(int i = 0; i < response.length; i++) {
                            if(i == 2 && response[2] == NO_WRONG_DIGITS_TO_DISPLAY)
                                result[i + 1] = "X";
                            else
                                result[i + 1] = String.valueOf(response[i]);
                        }

                        Player2Fragment.player2GuessResults.add(result);
                        Player2Fragment.adapterP2.notifyDataSetChanged();

                        tvPlayer2Round.setText(getString(R.string.round_template, 20 - num_of_rounds + 1));

                        // check for p2 win
                        if(gameResult == 2) {
                            tvGameStatus.setText("Game has finished!");
                            tvGameResult.setBackgroundColor(getColor(R.color.red));
                            tvGameResult.setText("P2 wins!");
                            resetAllThreads();
                            break;
                        }

                        // completion of one round of the game (as p1 started first)
                        num_of_rounds--;

                        // check for draw after p2's last turn
                        if(num_of_rounds == 0) {
                            tvGameStatus.setText("Game has finished!");
                            tvGameResult.setBackgroundColor(getColor(R.color.green));
                            tvGameResult.setText("It's a draw!");
                            resetAllThreads();
                            break;
                        }

                        // Make player 1 think for 2 seconds before making next guess
                        p1Handler.post(() -> {
                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        });

                        // inform p1 to make next guess
                        msgToSend = p1Handler.obtainMessage(MAKE_NEXT_GUESS);
                        p1Handler.sendMessage(msgToSend);
                        break;
                    }

                    default: super.handleMessage(msg);
                }
            }
        };

        btnStartGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(p1Thread != null && p2Thread != null)
                    resetAllThreads();

                resetGameData();

                // Player 1
                p1Thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Log.i("see me", "p1 thread created");

                        Looper.prepare();
                        p1Handler = new Handler(Looper.myLooper()) {

                            @Override
                            public void handleMessage(@NonNull Message msg) {
                                Message messageToSend;
                                int [] nextGuess;

                                switch (msg.what) {
                                    case GENERATE_SECRET_NUMBER:{
                                        player1Secret = getRandomSecret();

                                        messageToSend = mainHandler.obtainMessage(GET_SECRET_FROM_P1);
                                        mainHandler.sendMessage(messageToSend);
                                        break;
                                    }

                                    case MAKE_NEXT_GUESS: {
                                        Log.i("Thread 1", "p1 making guess");

                                        // make an intelligent guess and send it to p2
                                        nextGuess = makeNextGuessForP1();

                                        messageToSend = p2Handler.obtainMessage(HANDLE_OPPONENT_GUESS);

                                        Bundle bundle = new Bundle();
                                        bundle.putIntArray(GUESS_KEY, nextGuess);
                                        messageToSend.setData(bundle);

                                        p2Handler.sendMessage(messageToSend);
                                        break;
                                    }

                                    case HANDLE_OPPONENT_GUESS: {
                                        Log.i("Thread 1", "got guess from p2");

                                        int[] p2Guess = msg.getData().getIntArray(GUESS_KEY);

                                        // generate feedback for p2 and send it back to p2
                                        int[] response = generateP1Response(p2Guess);

                                        messageToSend = p2Handler.obtainMessage(HANDLE_OPPONENT_RESPONSE);

                                        Bundle bundle = new Bundle();
                                        bundle.putIntArray(GUESS_KEY, p2Guess);
                                        bundle.putIntArray(GUESS_RESPONSE_KEY, response);
                                        messageToSend.setData(bundle);

                                        p2Handler.sendMessage(messageToSend);
                                        break;
                                    }

                                    case HANDLE_OPPONENT_RESPONSE: {
                                        Log.i("Thread 1", "got response from p2");

                                        // get response from p2
                                        int[] myGuess = msg.getData().getIntArray(GUESS_KEY);
                                        int[] responseFromP2 = msg.getData().getIntArray(GUESS_RESPONSE_KEY);

                                        // update p1GuessHelper to make better guess next time
                                        p1GuessHelper.remove(new Integer(responseFromP2[2]));

                                        // send guess & response to ui thread
                                        messageToSend = mainHandler.obtainMessage(UPDATE_UI_FOR_P1);

                                        Bundle bundle = new Bundle();
                                        bundle.putIntArray(GUESS_KEY, myGuess);
                                        bundle.putIntArray(GUESS_RESPONSE_KEY, responseFromP2);
                                        messageToSend.setData(bundle);

                                        mainHandler.sendMessage(messageToSend);
                                        break;
                                    }

                                    default: super.handleMessage(msg);
                                }
                            }
                        };

                        // start game by informing ui thread
                        Message msgForUIThread = mainHandler.obtainMessage(DECIDE_SECRET_NUMBER);
                        mainHandler.sendMessage(msgForUIThread);

                        Looper.loop();
                    }
                });

                // Player 2
                p2Thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Log.i("Thread 2", "p2 thread created");

                        Looper.prepare();
                        p2Handler = new Handler(Looper.myLooper()) {
                            @Override
                            public void handleMessage(@NonNull Message msg) {
                                Message messageToSend;
                                int [] nextGuess;

                                switch (msg.what) {
                                    case GENERATE_SECRET_NUMBER: {
                                        player2Secret = getRandomSecret();

                                        messageToSend = mainHandler.obtainMessage(GET_SECRET_FROM_P2);
                                        mainHandler.sendMessage(messageToSend);
                                        break;
                                    }

                                    case MAKE_NEXT_GUESS: {
                                        Log.i("Thread 2", "p2 making guess");

                                        // make an intelligent guess and send to p1
                                        nextGuess = makeNextGuessForP2();

                                        messageToSend = p1Handler.obtainMessage(HANDLE_OPPONENT_GUESS);

                                        Bundle bundle = new Bundle();
                                        bundle.putIntArray(GUESS_KEY, nextGuess);
                                        messageToSend.setData(bundle);

                                        p1Handler.sendMessage(messageToSend);
                                        break;
                                    }

                                    case HANDLE_OPPONENT_GUESS: {
                                        Log.i("Thread 2", "got guess from p1");

                                        int[] p1Guess = msg.getData().getIntArray(GUESS_KEY);

                                        // generate feedback for p1 and send to p1
                                        int[] response = generateP2Response(p1Guess);

                                        messageToSend = p1Handler.obtainMessage(HANDLE_OPPONENT_RESPONSE);

                                        Bundle bundle = new Bundle();
                                        bundle.putIntArray(GUESS_KEY, p1Guess);
                                        bundle.putIntArray(GUESS_RESPONSE_KEY, response);
                                        messageToSend.setData(bundle);

                                        p1Handler.sendMessage(messageToSend);
                                        break;
                                    }

                                    case HANDLE_OPPONENT_RESPONSE: {
                                        Log.i("Thread 2", "got response from p1");

                                        // get response from p1
                                        int[] myGuess = msg.getData().getIntArray(GUESS_KEY);
                                        int[] responseFromP1 = msg.getData().getIntArray(GUESS_RESPONSE_KEY);

                                        // update p2GuessHelper to make better guess next time
                                        p2GuessHelper.remove(new Integer(responseFromP1[2]));

                                        // send guess & response to ui thread
                                        messageToSend = mainHandler.obtainMessage(UPDATE_UI_FOR_P2);

                                        Bundle bundle = new Bundle();
                                        bundle.putIntArray(GUESS_KEY, myGuess);
                                        bundle.putIntArray(GUESS_RESPONSE_KEY, responseFromP1);
                                        messageToSend.setData(bundle);

                                        mainHandler.sendMessage(messageToSend);
                                        break;
                                    }

                                    default: super.handleMessage(msg);
                                }
                            }
                        };
                        Looper.loop();
                    }
                });

                // start the 2 player threads
                p1Thread.start();
                p2Thread.start();
            }
        });
    }

    // generates random secret
    private int[] getRandomSecret() {
        int[] randomNoRepeat = new int[4];
        HashSet<Integer> flag = new HashSet<>();

        int idx = 0;
        while(flag.size() != 4) {
            int randomDigit = random.nextInt(10);

            if(flag.add(randomDigit))
                randomNoRepeat[idx++] = randomDigit;
        }

        return randomNoRepeat;
    }

    // makes next guess for p1
    private int[] makeNextGuessForP1() {
        int[] guess = new int[4];
        int idx = 0;

        // at least one wrong digit was guessed before
        if(!NO_WRONG_DIGITS_BY_P1) {
            Iterator<Integer> iter = p1GuessHelper.iterator();

            while (p1GuessHelper.size() >= 4 && iter.hasNext() && idx < 4) {
                if(!iter.hasNext())
                    iter = p1GuessHelper.iterator();

                guess[idx++] = iter.next();
            }
        }
        // no wrong digits were guessed before
        else {
            while (p1CorrectDigitsPermutations.contains(p1CorrectGuessedDigits)) {
                Collections.shuffle(p1CorrectGuessedDigits);
            }

            p1CorrectDigitsPermutations.add(p1CorrectGuessedDigits);

            for(idx = 0; idx < p1CorrectGuessedDigits.size(); idx++)
                guess[idx] = p1CorrectGuessedDigits.get(idx);
        }

        return guess;
    }

    // makes next guess for p2
    private int[] makeNextGuessForP2() {
        int[] guess = new int[4];
        int idx = 0;

        // at least one wrong digit was guessed before
        if(!NO_WRONG_DIGITS_BY_P2) {
            Collections.shuffle(p2GuessHelper);

            while(idx < 4 && p2GuessHelper.size() >= 4) {
                guess[idx] = p2GuessHelper.get(idx);
                idx++;
            }
        }
        // no wrong digits were guessed before
        else {
            while (p2CorrectDigitsPermutations.contains(p2CorrectGuessedDigits)) {
                Collections.shuffle(p2CorrectGuessedDigits);
            }

            p2CorrectDigitsPermutations.add(p2CorrectGuessedDigits);

            for(idx = 0; idx < p2CorrectGuessedDigits.size(); idx++)
                guess[idx] = p2CorrectGuessedDigits.get(idx);
        }

        return guess;
    }

    // generates p1's feedback for p2
    private int[] generateP1Response(int[] p2Guess) {
        int correctPosCount = 0;
        int wrongPosCount = 0;
        int wrongDigit = NO_WRONG_DIGITS_TO_DISPLAY;

        int totalCorrectDigits = 0;

        HashSet<Integer> p1SecretSet = new HashSet<>();
        for(int digit : player1Secret)
            p1SecretSet.add(digit);

        HashSet<Integer> correctDigits = new HashSet<>();
        HashSet<Integer> wrongDigits = new HashSet<>();

        for(int i = 0; i < p2Guess.length; i++) {
            // # of correctly guessed digits in correct positions
            if(p2Guess[i] == player1Secret[i])
                correctPosCount++;

            if(p1SecretSet.contains(p2Guess[i])) {
                totalCorrectDigits++;
                correctDigits.add(p2Guess[i]);
            } else {
                wrongDigits.add(p2Guess[i]);
            }
        }

        // # of correctly guessed digits in wrong positions
        wrongPosCount = totalCorrectDigits - correctPosCount;

        if(!NO_WRONG_DIGITS_BY_P2 && wrongDigits.isEmpty()) {
            for(int digit : p2Guess)
                p2CorrectGuessedDigits.add(digit);

            p2CorrectDigitsPermutations.add(p2CorrectGuessedDigits);
            NO_WRONG_DIGITS_BY_P2 = true;
        }

        // declare p2 as winner
        if(correctPosCount == 4)
            gameResult = 2;

        // get random wrongly guessed digit
        Iterator<Integer> iter = wrongDigits.iterator();
        if(iter.hasNext())
            wrongDigit = (int) iter.next();

        return new int[] {correctPosCount, wrongPosCount, wrongDigit};
    }

    // generates p2's feedback for p1
    private int[] generateP2Response(int[] p1Guess) {
        int correctPosCount = 0;
        int wrongPosCount = 0;
        int wrongDigit = NO_WRONG_DIGITS_TO_DISPLAY;

        int totalCorrectDigits = 0;

        HashSet<Integer> p2SecretSet = new HashSet<>();
        for(int digit : player2Secret)
            p2SecretSet.add(digit);

        HashSet<Integer> correctDigits = new HashSet<>();
        HashSet<Integer> wrongDigits = new HashSet<>();

        for(int i = 0; i < p1Guess.length; i++) {
            // # of correctly guessed digits in correct positions
            if(p1Guess[i] == player2Secret[i])
                correctPosCount++;

            if(p2SecretSet.contains(p1Guess[i])) {
                totalCorrectDigits++;
                correctDigits.add(p1Guess[i]);
            } else {
                wrongDigits.add(p1Guess[i]);
            }
        }

        // # of correctly guessed digits in wrong positions
        wrongPosCount = totalCorrectDigits - correctPosCount;

        // when p1's guess only differs by positions
        if(!NO_WRONG_DIGITS_BY_P1 && wrongDigits.isEmpty()) {
            for(int digit : p1Guess)
                p1CorrectGuessedDigits.add(digit);

            p1CorrectDigitsPermutations.add(p1CorrectGuessedDigits);
            NO_WRONG_DIGITS_BY_P1 = true;
        }

        // declare p1 as winner
        if(correctPosCount == 4)
            gameResult = 1;

        // get random wrongly guessed digit
        Iterator<Integer> iter = wrongDigits.iterator();
        if(iter.hasNext())
            wrongDigit = (int) iter.next();

        return new int[] {correctPosCount, wrongPosCount, wrongDigit};
    }

    // utility function for int[] to String
    private String getIntArrayAsString(int[] inputArr) {
        StringBuilder res = new StringBuilder();

        for(int digit : inputArr)
            res.append(digit);

        return res.toString();
    }

    // resets all Threads
    private void resetAllThreads() {
        p1Thread.interrupt();
        p2Thread.interrupt();

        mainHandler.removeCallbacksAndMessages(null);
        p1Handler.removeCallbacksAndMessages(null);
        p2Handler.removeCallbacksAndMessages(null);

        p1Handler.getLooper().quitSafely();
        p2Handler.getLooper().quitSafely();
    }

    // resets all game data
    private void resetGameData() {
        Player1Fragment.player1GuessResults.clear();
        Player1Fragment.adapterP1.notifyDataSetChanged();

        Player2Fragment.player2GuessResults.clear();
        Player2Fragment.adapterP2.notifyDataSetChanged();

        p1CorrectGuessedDigits.clear();
        p2CorrectGuessedDigits.clear();

        p1GuessHelper.clear();
        p2GuessHelper.clear();

        for(Integer digit : allDigits) {
            p1GuessHelper.add(digit);
            p2GuessHelper.add(digit);
        }

        NO_WRONG_DIGITS_BY_P1 = false;
        NO_WRONG_DIGITS_BY_P2 = false;

        num_of_rounds = 20;
        gameResult = -1;

        tvGameResult.setBackgroundColor(getColor(R.color.gray));
        tvGameStatus.setText("");
        tvGameResult.setText("");

        tvPlayer1Round.setText("");
        tvPlayer2Round.setText("");
    }
}