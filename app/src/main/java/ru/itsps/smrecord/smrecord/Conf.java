package ru.itsps.smrecord.smrecord;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by penart on 01.11.2018.
 */

public class Conf{
    @SerializedName("isrec")
    @Expose
    private int isRec;
    @SerializedName("isrecmic")
    @Expose
    private int isRecMIC;
    @SerializedName("isreccam")
    @Expose
    private int isRecCAM;
    @SerializedName("hourfrom")
    @Expose
    private int hourFrom;
    @SerializedName("hourto")
    @Expose
    private int hourTo;

    public int getIsRec() { return isRec; }

    public void setIsRec(int isRec) {
        this.isRec = isRec;
    }

    public int getIsRecMIC() { return isRecMIC; }

    public void setIsRecMIC(int isRecMIC) { this.isRecMIC = isRecMIC; }

    public int getIsRecCAM() { return isRecCAM; }

    public void setIsRecCAM(int isRecCAM) {
        this.isRecCAM = isRecCAM;
    }

    public int getHourFrom() {
        return hourFrom;
    }

    public void setHourFrom(int hourFrom) {
        this.hourFrom = hourFrom;
    }

    public int getHourTo() {
        return hourTo;
    }

    public void setHourTo(int hourTo) {
        this.hourTo = hourTo;
    }

}