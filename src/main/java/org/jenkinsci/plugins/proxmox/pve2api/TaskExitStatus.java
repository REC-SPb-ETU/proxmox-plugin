package org.jenkinsci.plugins.proxmox.pve2api;

/**
 * Status with which task has stopped.
 */
public enum TaskExitStatus {
    // there is no definition in proxmox api doc,
    // we just know that it may be ok or not
    OK("ok"),
    ANOTHER("another");

    private final String statusString;

    private TaskExitStatus(String statusString) {
        this.statusString = statusString;
    }

    public static TaskExitStatus fromString(String string) {
        if (OK.statusString.equalsIgnoreCase(string.trim())) {
            return OK;
        }

        return ANOTHER;
    }
}
