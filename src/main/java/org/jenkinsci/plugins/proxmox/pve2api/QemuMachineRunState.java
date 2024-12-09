package org.jenkinsci.plugins.proxmox.pve2api;

/**
 * State of Qemu machine.
 * (https://www.qemu.org/docs/master/interop/qemu-qmp-ref.html#qapidoc-80)
 */
public enum QemuMachineRunState {
    DEBUG("debug"),
    FINISH_MIGRATE("finish-migrate"),
    INTERNAL_ERROR("internal-error"),
    IO_ERROR("io-error"),
    PAUSED("paused"),
    POSTMIGRATE("postmigrate"),
    PRELAUNCH("prelaunch"),
    RESTORE_VM("restore-vm"),
    RUNNING("running"),
    SAVE_VM("save-vm"),
    SHUTDOWN("shutdown"),
    SUSPENDED("suspended"),
    WATCHDOG("watchdog"),
    GUEST_PANICKED("guest-panicked"),
    COLO("colo");

    private final String stateString;

    private QemuMachineRunState(String stateString) {
        this.stateString = stateString;
    }

    public static QemuMachineRunState fromString(String string) {
        for (QemuMachineRunState value: QemuMachineRunState.values()) {
            if (value.stateString.equalsIgnoreCase(string.trim())) {
                return value;
            }
        }

        throw new IllegalArgumentException(String.format("No qemu state '%s'", string));
    }

    public boolean isError() {
        return this == INTERNAL_ERROR || this == IO_ERROR;
    }
}
