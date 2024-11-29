package org.jenkinsci.plugins.proxmox.pve2api;

/**
 * Status with which task has stopped.
 * If need to represent all status appears, may be
 * reworked to enum.
 */
public class TaskExitStatus {
    private final String statusString;

    public TaskExitStatus(String statusString) {
        this.statusString = statusString;
    }

    public String getStatusString() {
        return this.statusString;
    }

    public boolean isOk() {
        return "ok".equalsIgnoreCase(this.statusString);
    }
}
