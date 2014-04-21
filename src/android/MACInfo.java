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

    private String mac;
    private boolean enable;
    private String name;

    public MACInfo() {
    }

    public MACInfo(String MAC, String Name, boolean Enable) {
        this.mac = MAC;
        this.enable = Enable;
        this.name = Name;
    }

    public String getMAC() {
        return mac;
    }

    public void setMAC(String MAC) {
        this.mac = MAC;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean Enable) {
        this.enable = Enable;
    }

    public String getName() {
        return name;
    }

    public void setName(String Name) {
        this.name = Name;
    }
}
