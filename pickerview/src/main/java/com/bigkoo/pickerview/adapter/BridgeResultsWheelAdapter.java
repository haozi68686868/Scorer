package com.bigkoo.pickerview.adapter;

/**
 * BridgeResults Wheel adapter.
 */

public class BridgeResultsWheelAdapter implements WheelAdapter {
    /** The default min value */
    public static final int DEFAULT_MAX_VALUE = 9;

    /** The default max value */
    private static final int DEFAULT_MIN_VALUE = 0;

    // Values
    private int minValue;
    private int maxValue;
    private int level;

    /**
     * Default constructor
     */
    public BridgeResultsWheelAdapter() {
        this(3);
    }

    /**
     * Constructor
     * @param level the wheel contract level
     */
    public BridgeResultsWheelAdapter(int level) {
        this.level = level;
    }

    @Override
    public Object getItem(int index) {
        if (index >= 0 && index < getItemsCount()) {
            int temp=13-index-level;
            String value;
            if(temp==0)
                value="=";
            else if(temp>0)
                value="+"+String.valueOf(temp);
            else
                value=String.valueOf(temp);
            return value;
        }
        return 0;
    }

    @Override
    public int getItemsCount() {
        //return maxValue - minValue + 1;
        return 14;
    }

    @Override
    public int indexOf(Object o){

        try {
            String s=(String)o;
            int temp;
            if(s.equals("="))temp=0;
            else if(s.startsWith("+"))
            {
                temp=Integer.valueOf(s.substring(1));
            }
            else
                temp=Integer.valueOf(s);
            temp=7-level-temp;
            return temp<=13?temp:-1;
        } catch (Exception e) {
            return -1;
        }

    }
}
