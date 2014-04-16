/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jdev.guardian.service;

/**
 *
 * @author Jeremy
 */
public class MACInfo {

    private String MAC;
    private boolean Enable;
    private String Name;

    public MACInfo() {
    }

    public MACInfo(String MAC, String Name, boolean Enable) {
        this.MAC = MAC;
        this.Enable = Enable;
        this.Name = Name;
    }

    public String getMAC() {
        return MAC;
    }

    public void setMAC(String MAC) {
        this.MAC = MAC;
    }

    public boolean isEnable() {
        return Enable;
    }

    public void setEnable(boolean Enable) {
        this.Enable = Enable;
    }

    public String getName() {
        return Name;
    }

    public void setName(String Name) {
        this.Name = Name;
    }
}
