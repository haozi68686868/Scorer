package com.blacktec.think.scorer;

import android.widget.Toast;

import java.util.Random;
import java.util.UUID;

/**
 * Created by Think on 2017/10/3.
 */

public class BridgeContract {

    private int mBoardNum;
    private Vulnerable mVul;
    private String mDeclarer;
    private int mResult;
    private int mLevel;
    private Suit mSuit;

    private DoubleStatus mDouble;
    public String mFlag;

    public BridgeContract()
    {
        this(1);
    }
    public BridgeContract(int boardNum)
    {
        mFlag="None";
        mDeclarer="";
        mBoardNum=boardNum;
        switch (boardNum%16)
        {
            case 0: mVul = Vulnerable.EW; break;
            case 1: mVul = Vulnerable.None; break;
            case 2: mVul = Vulnerable.NS; break;
            case 3: mVul = Vulnerable.EW; break;
            case 4: mVul = Vulnerable.Both; break;
            case 5: mVul = Vulnerable.NS; break;
            case 6: mVul = Vulnerable.EW; break;
            case 7: mVul = Vulnerable.Both; break;
            case 8: mVul = Vulnerable.None; break;
            case 9: mVul = Vulnerable.EW; break;
            case 10: mVul = Vulnerable.Both; break;
            case 11: mVul = Vulnerable.None; break;
            case 12: mVul = Vulnerable.NS; break;
            case 13: mVul = Vulnerable.Both; break;
            case 14: mVul = Vulnerable.None; break;
            case 15: mVul = Vulnerable.NS; break;
            default: mVul = Vulnerable.None; break;
        }
    }
    public int getBoardNum()
    {
        return mBoardNum;
    }

    public void setBoardNum(int boardNum) {
        mBoardNum = boardNum;
    }
    public Suit getSuit() {
        return mSuit;
    }
    public int getResult() {
        return mResult;
    }
    public int getLevel() {
        return mLevel;
    }
    public DoubleStatus getDouble() {
        return mDouble;
    }
    public String getDeclarer() {
        return mDeclarer;
    }
    public String getContract()
    {
        if(mFlag.equals("None")|mFlag.equals("Format Error"))return "";
        if(mSuit==mSuit.AP)return"All Pass";
        String s;
        s=String.valueOf(mLevel)+getSuitString(mSuit)+getDoubleString(mDouble);
        return s;
    }
    public String getResultText()//
    {
        if(mFlag.equals("None")|mFlag.equals("Format Error"))return "";
        if(mSuit==mSuit.AP)return"All Pass";
        return getDeclarer()+getContract()+getResultString();
    }
    public String getResultShown()//区别在于是否转化可逆，有空格(不符合输入格式，但美观)，无空格(符合输入格式)
    {
        if(mFlag.equals("None")|mFlag.equals("Format Error"))return "";
        if(mSuit==mSuit.AP)return"All Pass";
        return getDeclarer()+" "+getContract()+getResultString();
    }
    private String getSuitString(Suit suit)
    {
        switch (suit)
        {
            case Notrump:
                return "NT";
            case Spade:
                return "♠";
            case Heart:
                return "♥";
            case Diamond:
                return "♦";
            case Club:
                return "♣";
            case AP:
                return "AP";
            default:
                return null;
        }
    }
    private String getDoubleString(DoubleStatus d)
    {
        switch (d)
        {
            case None:
                return "";
            case Doubled:
                return "X";
            case Redoubled:
                return "XX";
            default:
                return null;
        }
    }
    public String getResultString()
    {
        return getResultString(mResult);
    }
    public String getResultString(int result)
    {
        return result==0?"=":result<0?String.valueOf(result):"+"+String.valueOf(result);
    }
    public void setContract(String s)
    {
        String temp;
        int tempIndex;
        if(s.equals("AP")||s.equals("All Pass"))
        {
            mSuit=Suit.AP;
            mFlag="Done";
            return;
        }
        if(s.length()<4)
        {
            mFlag="None";
            return;
        }
        try
        {
            s=s.replace('♠','S')
            .replace('♥','H')
            .replace('♦','D')
            .replace('♣','C');
            mDeclarer=s.substring(0,1);
            if("NSEW".indexOf(mDeclarer)==-1)
            {
                mFlag="Format Error";
                return;
            }
            mLevel=Integer.valueOf(s.substring(1,2));
            if(mLevel>7|mLevel==0)
                throw new Exception();
            temp=s.substring(2,3);
            tempIndex=3;
            switch (temp)
            {
                case "S":
                    mSuit=Suit.Spade;
                    break;
                case "H":
                    mSuit=Suit.Heart;
                    break;
                case "D":
                    mSuit=Suit.Diamond;
                    break;
                case "C":
                    mSuit=Suit.Club;
                    break;
                case "N":
                    if(s.substring(3,4).equals("T"))
                    {
                        tempIndex++;
                    }
                    mSuit=Suit.Notrump;
                    break;
                default:
                    throw new Exception();
            }
            temp=s.substring(tempIndex);
            if(temp.startsWith("XX"))
            {
                tempIndex+=2;
                mDouble=DoubleStatus.Redoubled;
            }
            else if(temp.startsWith("X"))
            {
                tempIndex+=1;
                mDouble=DoubleStatus.Doubled;
            }
            else
            {
                mDouble=DoubleStatus.None;
            }
            temp=s.substring(tempIndex);
            if(temp.equals("="))
                mResult=0;
            else if(temp.startsWith("+"))
            {
                mResult=Integer.valueOf(temp.substring(1));
                if(mResult+mLevel>7)
                    throw new Exception();
            }
            else if(temp.startsWith("-"))
            {
                mResult=Integer.valueOf(temp);
                if(mResult+mLevel<-6)
                    throw new Exception();
            }
            else
            {
                throw new Exception();
            }
        }
        catch(Exception e)
        {
            mFlag="Format Error";
            return;
        }
        mFlag="Done";
    }

    public boolean isVul()
    {
        switch (mVul)
        {
            case Both:
                return true;
            case None:
                return false;
            case NS:
                return mDeclarer.equals("N")|mDeclarer.equals("S");
            case EW:
                return mDeclarer.equals("E")|mDeclarer.equals("W");
            default:
                return false;
        }
    }
    public int getScore()
    {
        if(mSuit==Suit.AP)return 0;
        if(mDeclarer.equals("N")|mDeclarer.equals("S"))
            return getContractScore();
        else
            return -getContractScore();
    }

    private int getContractScore() {
        int basePoint=0;
        int bonusPoint=0;
        int trickPoint=0;
        int temp;
        if(mResult<0)
        {
            int x=1;
            switch (mDouble)
            {
                case None:
                    temp = isVul() ? 100 : 50;
                    return mResult * temp;
                case Redoubled:
                    x = 2;
                case Doubled:
                    temp = (isVul() ? 1 : 0) - mResult;
                    switch (temp)
                    {
                        case 1:
                            return -100 * x;
                        case 2:
                            return isVul()?-200 * x:-300 * x;
                        default:
                            return (400 - 300 * temp) * x;
                    }
            }
        }
        else
        {
            switch (mSuit)
            {
                case AP:
                    return 0;
                case Notrump:
                    basePoint=30*mLevel+10;
                    break;
                case Spade:
                case Heart:
                    basePoint=30*mLevel;
                    break;
                case Diamond:
                case Club:
                    basePoint=20*mLevel;
                    break;
            }
            switch (mDouble)
            {
                case Doubled:
                    basePoint*=2;
                    break;
                case Redoubled:
                    basePoint*=4;
                    break;
            }
            switch (mLevel)
            {
                case 7:
                    bonusPoint=isVul()?2000:1300;
                    break;
                case 6:
                    bonusPoint=isVul()?1250:800;
                    break;
                default:
                    if(basePoint>=100)
                        bonusPoint=isVul()?500:300;
                    else
                        bonusPoint=50;
            }
            switch (mDouble)
            {
                case None:
                    switch (mSuit)
                    {
                        case Notrump:
                            trickPoint=30*mResult;
                            break;
                        case Spade:
                        case Heart:
                            trickPoint=30*mResult;
                            break;
                        case Diamond:
                        case Club:
                            trickPoint=20*mResult;
                            break;
                    }
                    break;
                case Doubled:
                    trickPoint=(isVul()?2:1)*100*mResult+50;
                    break;
                case Redoubled:
                    trickPoint=(isVul()?2:1)*200*mResult+100;
                    break;
            }
        }
        return bonusPoint+basePoint+trickPoint;
    }
    public void Clear()
    {
        mSuit=null;
        mDeclarer=null;
        mResult=0;
        mFlag="None";
    }

}
