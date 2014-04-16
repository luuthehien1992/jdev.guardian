/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jdev.guardian.service;

import com.google.gson.*;

/**
 *
 * @author Jeremy
 */
public class Result {

    private ResultStatus resultStatus = ResultStatus.Unknow;
    private MACInfo macInfo = null;
    private double distance = 0;

    public Result(ResultStatus resultStatus, MACInfo macInfo, double distance) {
        this.resultStatus = resultStatus;
        this.macInfo = macInfo;
        this.distance = distance;
    }

    public ResultStatus getResultStatus() {
        return resultStatus;
    }

    public void setResultStatus(ResultStatus resultStatus) {
        this.resultStatus = resultStatus;
    }

    public MACInfo getmACInfo() {
        return macInfo;
    }

    public void setmACInfo(MACInfo mACInfo) {
        this.macInfo = mACInfo;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public String toJson() {
        Gson gson = new Gson();

        return gson.toJson(this);
    }
}
