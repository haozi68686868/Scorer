package com.bigkoo.pickerview;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Think on 2017/10/13.
 */

public class BridgePickerData {
    public static final class BGOp {
        public static final ArrayList<String> directions = new ArrayList<>();
        public static final ArrayList<String> suits = new ArrayList<>();
        public static final ArrayList<ArrayList<String>> results = new ArrayList<>();
        public static final ArrayList<String> doubles = new ArrayList<>();
        public static final ArrayList<String> levels = new ArrayList<>();

        public BGOp() {
            init();
        }

        public static void init() {
            if(!directions.isEmpty())return;
            directions.add("N");
            directions.add("S");
            directions.add("E");
            directions.add("W");
            suits.add("NT");
            suits.add("♠");
            suits.add("♥");
            suits.add("♦");
            suits.add("♣");
            int i, j;
            ArrayList<String> temp;
            for (i = 7; i > 0; i--) {
                levels.add(String.valueOf(i));
                temp = new ArrayList<>();
                for (j = 7 - i; j > -7 - i; j--) {
                    temp.add(j == 0 ? "=" : j < 0 ? String.valueOf(j) : "+" + String.valueOf(j));
                }
                results.add(temp);
            }
            doubles.add("");
            doubles.add("X");
            doubles.add("XX");
        }
    }
}
