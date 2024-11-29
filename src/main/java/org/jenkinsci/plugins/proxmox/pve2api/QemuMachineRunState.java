package org.jenkinsci.plugins.proxmox.pve2api;

/**
 * State of Qemu machine.
 * (https://www.qemu.org/docs/master/interop/qemu-qmp-ref.html#qapidoc-80)
 * If need to represent all states appears, may be
 * reworked to enum.
 */
public class QemuMachineRunState {
    private final String stateString;

    public QemuMachineRunState(String stateString) {
        this.stateString = stateString;
    }

    public String getStateString() {
        return this.stateString;
    }

    public boolean isRunning() {
        return "running".equalsIgnoreCase(stateString);
    }

    public boolean isError() {
        return "internal-error".equalsIgnoreCase(stateString)
            || "io-error".equalsIgnoreCase(stateString);
    }
}
